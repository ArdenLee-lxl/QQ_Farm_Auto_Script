package org.test;

import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.config.CoordinateConfig;
import org.core.ImageMatcher;
import org.core.MouseController;
import org.game.config.GameConfig;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 田地扫描-播种 demo（主流程合并前的验证版本，勿在主流程引用）
 *
 * 验证目标：
 * 1. 4角+4x6网格 双线性算出24个田块中心
 * 2. 免模板颜色无关判空：灰度归一化纹理匹配（空田模版）+ 绿色植被占比 双特征
 * 3. 点击空田 → 动态识别种子栏（角标 / "没有种子"文字 / 铲子误判）
 * 4. 一键种植循环直到无空田；无种子时点文字进商店购买后继续
 *
 * 运行方式（IDEA 直接右键运行）：
 *   FieldPlantDemo      → 完整循环（会点鼠标）
 *   FieldPlantScanDemo  → 只扫描不点击（先验证识别效果）
 */
public class FieldPlantDemo {

    // ========== 新增模板（demo 内自持，合并时迁入 GameConfig）==========
    private static final String EMPTY_PLOT_TEMPLATE = "images/个人田地页面/空田模版.png";
    private static final String NO_SEED_TEXT = "images/个人田地页面/点击田地无种子截图.png";

    // ========== 判定阈值 ==========
    /** 绿色植被占比 ≥ 此值 → 有作物 */
    private static final double GREEN_MIN_RATIO = 0.05;
    /** 空田纹理匹配分 ≥ 此值 → 空田 */
    private static final double EMPTY_MIN_SCORE = 0.80;
    /** 白色像素占比 ≥ 此值 → 认定屏幕上有手势提示（配合"最高白格M → 上方T判空"规则） */
    private static final double WHITE_MIN_RATIO = 0.08;
    /** 种子角标动态搜索阈值（数字角标变化，宽松匹配） */
    private static final double BADGE_SEARCH_THRESHOLD = 0.6;
    /** 空田模板多尺度搜索范围（模板裁剪时与当前画面缩放不一致时自适应） */
    private static final double[] TPL_SCALES = {0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3};
    /** 完整模式最大轮次（防死循环） */
    private static final int MAX_ROUNDS = 10;

    private static final String OUT_DIR = "target/field_debug";

    private final ImageMatcher imageMatcher;
    private final MouseController mouse;
    private final Robot robot;
    private BufferedImage emptyTemplate;

    public FieldPlantDemo() throws Exception {
        this.imageMatcher = new ImageMatcher();
        this.mouse = new MouseController();
        this.robot = new Robot();
        this.emptyTemplate = loadResource(EMPTY_PLOT_TEMPLATE);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== 田地扫描-播种 demo（完整循环，会点击鼠标）===");
        System.out.println("等待 5 秒，请确保投屏画面已显示、停留在自己农场...");

        Thread.sleep(5000);
        FieldPlantDemo demo = new FieldPlantDemo();
        demo.runFull();
    }

    /** 只扫描不点击（供 FieldPlantScanDemo 入口调用） */
    public void runScanOnly() throws Exception {
        printTemplateSanity();
        scanOnce(true);
        System.out.println("scan 结束（未点击任何位置）");
    }

    /** 打印模版自检信息（模版里若混入绿色会影响判定） */
    private void printTemplateSanity() {
        int w = emptyTemplate.getWidth();
        int h = emptyTemplate.getHeight();
        double green = greenRatio(emptyTemplate, 0, 0, w, h);
        System.out.printf("空田模版: %dx%d, 模版内绿占比=%.1f%%（应接近0）%n", w, h, green * 100);
    }

    // ==================== 完整循环 ====================

    private void runFull() throws Exception {
        printTemplateSanity();
        for (int round = 1; round <= MAX_ROUNDS; round++) {
            System.out.println();
            System.out.println("==== 第 " + round + " 轮扫描 ====");
            List<Point> empties = scanOnce(true);
            if (empties.isEmpty()) {
                System.out.println("★ 没有空田了，全部种满，demo 完成");
                return;
            }

            Point target = empties.get(0);
            System.out.println("点击空田候选: (" + target.x() + ", " + target.y()
                    + ")，本轮共 " + empties.size() + " 块空田");
            mouse.moveToAndClick(target.x(), target.y(), MouseController.ClickType.LEFT);
            mouse.waitFor(2);

            if (!handleSeedBar()) {
                System.out.println("种子栏未处理成功，重扫下一轮...");
            }
        }
        System.out.println("到达最大轮次 " + MAX_ROUNDS + "，demo 结束（看上方日志排查）");
    }

    /**
     * 处理弹出的种子栏，返回是否推进了进度（种植或购买）
     * 三种界面：种子角标 / "没有种子"文字 / 小铲子（误判在长田）
     */
    private boolean handleSeedBar() {
        for (int attempt = 1; attempt <= 2; attempt++) {
            // 1. 种子角标 → 点种子一键种植
            Point seed = findSeedCenter();
            if (seed != null) {
                System.out.println("检测到种子角标，点击种子一键种植: (" + seed.x() + ", " + seed.y() + ")");
                mouse.moveToAndClick(seed.x(), seed.y(), MouseController.ClickType.LEFT);
                mouse.waitFor(2);
                System.out.println("已点击种子（一键种植只种空田）");
                return true;
            }

            // 2. 小铲子 → 这格其实在长，扫描误判
            if (findQuiet(GameConfig.IMG_SHOVEL) != null) {
                System.out.println("检测到小铲子 → 该格误判为在长（会被重扫跳过），不算失败");
                return false;
            }

            // 3. "没有种子"文字 → 点击直接进商店
            Point noSeed = findQuiet(NO_SEED_TEXT);
            if (noSeed != null) {
                System.out.println("检测到「没有种子」提示，点击进入商店: (" + noSeed.x() + ", " + noSeed.y() + ")");
                mouse.moveToAndClick(noSeed.x(), noSeed.y(), MouseController.ClickType.LEFT);
                mouse.waitFor(2);
                buyInShop();
                return true;
            }

            System.out.println("种子栏还没出现（第 " + attempt + " 次），等 1.5 秒重试...");
            mouse.waitFor(1.5);
        }
        System.out.println("WARN: 两次都没识别到种子栏内容");
        return false;
    }

    /**
     * 动态寻找种子角标（全投屏搜索，位置随点击的田块变化），
     * 返回"角标+偏移"的种子中心；找不到返回 null
     */
    private Point findSeedCenter() {
        try {
            int[] b = CoordinateConfig.getScreenBounds();
            List<Point> badges = imageMatcher.findButtonsInRegion(
                    GameConfig.IMG_SEED_BADGE, b[0], b[1], b[4], b[5], BADGE_SEARCH_THRESHOLD);
            if (badges.isEmpty()) {
                return null;
            }
            Point leftmost = badges.get(0);
            for (Point p : badges) {
                if (p.x() < leftmost.x()) {
                    leftmost = p;
                }
            }
            return new Point(
                    leftmost.x() + GameConfig.BADGE_TO_SEED_OFFSET_X,
                    leftmost.y() + GameConfig.BADGE_TO_SEED_OFFSET_Y);
        } catch (Exception e) {
            System.out.println("搜种子角标异常: " + e.getMessage());
            return null;
        }
    }

    // ==================== 商店购买（照抄 BuySeedAction 的店内流程） ====================

    private void buyInShop() {
        try {
            Point seed = findBestSeedInShop();
            if (seed == null) {
                System.out.println("WARN: 商店里没找到可购买的种子");
                return;
            }
            System.out.println("购买最靠右下的无锁种子: (" + seed.x() + ", " + seed.y() + ")");
            mouse.moveToAndClick(seed.x(), seed.y(), MouseController.ClickType.LEFT);
            mouse.waitFor(1);

            Point confirm = findQuiet(GameConfig.IMG_BUY_CONFIRM);
            if (confirm != null) {
                System.out.println("点击确认购买");
                mouse.moveToAndClick(confirm.x(), confirm.y(), MouseController.ClickType.LEFT);
                mouse.waitFor(1);
            } else {
                System.out.println("WARN: 未找到确认按钮");
            }

            Point close = findQuiet(GameConfig.IMG_SHOP_CLOSE);
            if (close != null) {
                System.out.println("关闭商店");
                mouse.moveToAndClick(close.x(), close.y(), MouseController.ClickType.LEFT);
                mouse.waitFor(2);
            } else {
                System.out.println("WARN: 未找到商店关闭按钮");
            }
            System.out.println("购买流程结束");
        } catch (Exception e) {
            System.out.println("购买流程异常: " + e.getMessage());
        }
    }

    private Point findBestSeedInShop() {
        try {
            int left = CoordinateConfig.getInt("qqFarm.seedGrid.leftTop.x");
            int top = CoordinateConfig.getInt("qqFarm.seedGrid.leftTop.y");
            int right = CoordinateConfig.getInt("qqFarm.seedGrid.rightTop.x");
            int bottom = CoordinateConfig.getInt("qqFarm.seedGrid.leftBottom.y");
            int cols = CoordinateConfig.getInt("qqFarm.seedGrid.cols");
            int rows = CoordinateConfig.getInt("qqFarm.seedGrid.rows");

            int cellW = (right - left) / cols;
            int cellH = (bottom - top) / rows;

            List<Point> locks = imageMatcher.findAllButtons(GameConfig.IMG_SHOP_LOCK);

            for (int row = rows - 1; row >= 0; row--) {
                for (int col = cols - 1; col >= 0; col--) {
                    int cellX = left + col * cellW;
                    int cellY = top + row * cellH;
                    boolean locked = false;
                    for (Point lock : locks) {
                        if (lock.x() >= cellX && lock.x() <= cellX + cellW
                                && lock.y() >= cellY && lock.y() <= cellY + cellH) {
                            locked = true;
                            break;
                        }
                    }
                    if (!locked) {
                        return new Point(cellX + cellW / 2, cellY + cellH / 2);
                    }
                }
            }
            return null;
        } catch (Exception e) {
            System.out.println("扫描商店异常: " + e.getMessage());
            return null;
        }
    }

    // ==================== 核心：24格扫描 ====================

    /**
     * 扫描全田区，打印每格特征并（可选）保存标注截图
     * @return 空田中心坐标列表（按行扫描顺序）
     */
    private List<Point> scanOnce(boolean saveShot) throws Exception {
        // 1. 读4角与行列数
        int[] lt = CoordinateConfig.getPoint("qqFarm.fieldArea.leftTop");
        int[] rt = CoordinateConfig.getPoint("qqFarm.fieldArea.rightTop");
        int[] lb = CoordinateConfig.getPoint("qqFarm.fieldArea.leftBottom");
        int[] rb = CoordinateConfig.getPoint("qqFarm.fieldArea.rightBottom");
        int cols = CoordinateConfig.getInt("qqFarm.fieldArea.cols");
        int rows = CoordinateConfig.getInt("qqFarm.fieldArea.rows");

        // 2. 格子尺寸（取样窗 = 整格，给模板多尺度放大留空间）
        double cellW = dist(lt, rt) / cols;
        double cellH = dist(lt, lb) / rows;
        int tplW = emptyTemplate.getWidth();
        int tplH = emptyTemplate.getHeight();
        int winW = Math.max(tplW, (int) Math.ceil(cellW));
        int winH = Math.max(tplH, (int) Math.ceil(cellH));
        System.out.printf("田区格子≈%.1fx%.1f px, 取样窗=%dx%d, 模版=%dx%d%n",
                cellW, cellH, winW, winH, tplW, tplH);
        if (cellW < tplW || cellH < tplH) {
            System.out.println("WARN: 格子比模版还小，请检查 fieldArea 四角坐标！");
        }

        // 3. 一次性截全屏 + 预生成多尺度灰度模板
        BufferedImage screen = robot.createScreenCapture(
                new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
        List<ScaledTpl> tpls = buildScaledTemplates(winW, winH);
        StringBuilder scaleInfo = new StringBuilder();
        for (ScaledTpl t : tpls) {
            scaleInfo.append(String.format("%.1fx ", t.scale()));
        }
        System.out.println("多尺度模板档位: " + scaleInfo);
        if (tpls.isEmpty()) {
            System.out.println("ERROR: 没有任何尺度的模板能放进取样窗，检查四角坐标/模版尺寸");
        }

        // ===== 第一遍：逐格计算特征 =====
        int total = rows * cols;
        double[] greens = new double[total];
        double[] whites = new double[total];
        double[] scoresArr = new double[total];
        double[] scaleArr = new double[total];
        Point[] centers = new Point[total];
        int[] x0s = new int[total];
        int[] y0s = new int[total];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int i = r * cols + c;
                double u = (c + 0.5) / cols;
                double v = (r + 0.5) / rows;
                Point center = bilinear(lt, rt, lb, rb, u, v);
                int x0 = Math.max(0, Math.min(center.x() - winW / 2, screen.getWidth() - winW));
                int y0 = Math.max(0, Math.min(center.y() - winH / 2, screen.getHeight() - winH));
                BufferedImage crop = screen.getSubimage(x0, y0, winW, winH);
                Tm tm = tmScore(crop, tpls);
                greens[i] = greenRatio(crop, 0, 0, winW, winH);
                whites[i] = whiteRatio(crop, 0, 0, winW, winH);
                scoresArr[i] = tm.score();
                scaleArr[i] = tm.scale();
                centers[i] = center;
                x0s[i] = x0;
                y0s[i] = y0;
            }
        }

        // ===== 白手规则：M=白占比最高（手盖住/路过，不可信）→ T=M同一行的前一格（左侧，手加成判空）=====
        // TODO: 边界优化——当 T 是最后一列（右缘）时，手 M 落在田区右侧边界之外，
        //       24 个格窗都采不到白像素，该行手加成失效（只剩纹理兜底）。
        //       方案：在田区右缘外侧加一条虚拟采样带作为 M 候选，命中则 T=其左侧一格。
        int mIdx = -1;
        for (int i = 0; i < total; i++) {
            if (mIdx < 0 || whites[i] > whites[mIdx]) {
                mIdx = i;
            }
        }
        int tIdx = -1;
        String handInfo = "无手（最高白占比 "
                + String.format("%.1f%%", mIdx >= 0 ? whites[mIdx] * 100 : 0) + "）";
        if (mIdx >= 0 && whites[mIdx] >= WHITE_MIN_RATIO) {
            int mr = mIdx / cols;
            int mc = mIdx % cols;
            if (mc - 1 >= 0) {
                int cand = mIdx - 1;  // 同一行的前一格 = 左侧一格
                if (whites[mIdx] > whites[cand]) {
                    tIdx = cand;
                    handInfo = String.format("手在 R%dC%d(白=%.1f%%) → 同行左侧 T=R%dC%d 手加成判空",
                            mr + 1, mc + 1, whites[mIdx] * 100, mr + 1, mc);
                } else {
                    handInfo = String.format("手在 R%dC%d 但同行左侧白占比更高，本轮放弃手加成", mr + 1, mc + 1);
                }
            } else {
                handInfo = String.format("手在 R%dC%d（首列无左邻），本轮放弃手加成", mr + 1, mc + 1);
            }
        }

        // ===== 第二遍：判定 =====
        // 语义：M(手盖格)=未知；T(手目标)=空；绿=在长；纹理达标=空；纹理低分=在长
        boolean handActive = mIdx >= 0 && whites[mIdx] >= WHITE_MIN_RATIO;
        List<Point> empties = new ArrayList<>();
        List<Cell> cells = new ArrayList<>();
        int growingCount = 0;
        int unknownCount = 0;
        StringBuilder table = new StringBuilder();
        for (int i = 0; i < total; i++) {
            int r = i / cols;
            int c = i % cols;
            String verdict;
            if (handActive && i == mIdx) {
                // 白占比最高的手盖格：本轮不可信，唯一一个未知
                verdict = "UNKNOWN";
                unknownCount++;
            } else if (i == tIdx) {
                verdict = "EMPTY";
                empties.add(centers[i]);
            } else if (greens[i] >= GREEN_MIN_RATIO) {
                verdict = "GROWING";
                growingCount++;
            } else if (scoresArr[i] >= EMPTY_MIN_SCORE) {
                verdict = "EMPTY";
                empties.add(centers[i]);
            } else {
                // 纹理不像空田 = 上面有东西 = 正在长（成熟作物不一定绿，不能只看绿）
                verdict = "GROWING";
                growingCount++;
            }
            cells.add(new Cell(centers[i], verdict, x0s[i], y0s[i], winW, winH));

            table.append(String.format("R%dC%d (%3d,%3d) 绿=%4.1f%% 白=%4.1f%% 匹配=%.3f@%.1fx → %s%n",
                    r + 1, c + 1, centers[i].x(), centers[i].y(),
                    greens[i] * 100, whites[i] * 100, scoresArr[i], scaleArr[i], verdict));
        }

        System.out.println("---- 24格扫描明细 ----");
        System.out.print(table);
        System.out.println("手规则: " + handInfo);
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        double sum = 0;
        for (double s : scoresArr) {
            min = Math.min(min, s);
            max = Math.max(max, s);
            sum += s;
        }
        System.out.printf("匹配分分布: 最低=%.3f 最高=%.3f 平均=%.3f（阈值=%.2f）%n",
                min, max, sum / total, EMPTY_MIN_SCORE);
        System.out.println("空田 " + empties.size() + " 块, 在长 " + growingCount
                + " 块, 未知 " + unknownCount + " 块");
        if (handActive) {
            System.out.println("（未知 = M=R" + (mIdx / cols + 1) + "C" + (mIdx % cols + 1)
                    + "，白占比最高的手盖格，本轮不可信）");
        }

        if (saveShot) {
            String path = saveAnnotated(screen, lt, rt, lb, rb, cells, mIdx, tIdx, cols);
            System.out.println("标注截图: " + path);
        }
        for (ScaledTpl t : tpls) {
            t.gray().release();
        }
        return empties;
    }

    /** 双线性插值取田块中心（4角一般四边形，透视歪斜可兜） */
    private Point bilinear(int[] lt, int[] rt, int[] lb, int[] rb, double u, double v) {
        double topX = lt[0] + u * (rt[0] - lt[0]);
        double topY = lt[1] + u * (rt[1] - lt[1]);
        double botX = lb[0] + u * (rb[0] - lb[0]);
        double botY = lb[1] + u * (rb[1] - lb[1]);
        return new Point((int) Math.round(topX + v * (botX - topX)),
                (int) Math.round(topY + v * (botY - topY)));
    }

    private double dist(int[] a, int[] b) {
        return Math.hypot(a[0] - b[0], a[1] - b[1]);
    }

    /**
     * 绿色植被占比：g 明显高于 r/b 判为绿叶（土色紫/黄/红/黑都不会命中）
     */
    private double greenRatio(BufferedImage img, int x0, int y0, int w, int h) {
        int green = 0;
        int total = 0;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                if (g > 60 && g > r + 12 && g > b + 12) {
                    green++;
                }
                total++;
            }
        }
        return total == 0 ? 0 : (double) green / total;
    }

    /**
     * 白色像素占比（近白色：三通道都高且饱和度低）
     * 用于检测游戏在空田上显示的白色小手指引（会动，无法用模板）
     */
    private double whiteRatio(BufferedImage img, int x0, int y0, int w, int h) {
        int white = 0;
        int total = 0;
        for (int y = y0; y < y0 + h; y++) {
            for (int x = x0; x < x0 + w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                if (min > 185 && (max - min) < 45) {
                    white++;
                }
                total++;
            }
        }
        return total == 0 ? 0 : (double) white / total;
    }

    /**
     * 预生成多尺度灰度模板（只保留能放进取样窗的档位）
     */
    private List<ScaledTpl> buildScaledTemplates(int winW, int winH) {
        List<ScaledTpl> list = new ArrayList<>();
        int tplW = emptyTemplate.getWidth();
        int tplH = emptyTemplate.getHeight();
        for (double s : TPL_SCALES) {
            int w = (int) Math.round(tplW * s);
            int h = (int) Math.round(tplH * s);
            if (w < 8 || h < 8 || w > winW || h > winH) {
                continue;
            }
            list.add(new ScaledTpl(s, toGrayMat(scaleImage(emptyTemplate, w, h))));
        }
        return list;
    }

    private BufferedImage scaleImage(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    /**
     * 灰度归一化纹理匹配，多尺度取最高分
     * （TM_CCOEFF_NORMED = 置灰+减均值+归一化，只比纹理结构；多尺度吸收模板与画面的缩放差）
     */
    private Tm tmScore(BufferedImage crop, List<ScaledTpl> tpls) {
        if (tpls.isEmpty()) {
            return new Tm(0, 1.0);
        }
        Mat cropGray = toGrayMat(crop);
        double bestScore = -1;
        double bestScale = 1.0;
        for (ScaledTpl t : tpls) {
            Mat result = new Mat();
            opencv_imgproc.matchTemplate(cropGray, t.gray(), result, opencv_imgproc.TM_CCOEFF_NORMED);
            org.bytedeco.javacpp.DoublePointer maxVal = new org.bytedeco.javacpp.DoublePointer(1);
            org.bytedeco.javacpp.DoublePointer minVal = new org.bytedeco.javacpp.DoublePointer(1);
            Point maxLoc = new Point();
            Point minLoc = new Point();
            opencv_core.minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);
            double score = maxVal.get();
            maxVal.close();
            minVal.close();
            result.release();
            if (score > bestScore) {
                bestScore = score;
                bestScale = t.scale();
            }
        }
        cropGray.release();
        return new Tm(Math.max(bestScore, 0), bestScale);
    }

    private Mat toGrayMat(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        Mat mat = new Mat(height, width, opencv_core.CV_8UC3);
        byte[] data = new byte[width * height * 3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);
                int i = (y * width + x) * 3;
                data[i] = (byte) ((rgb) & 0xFF);         // B
                data[i + 1] = (byte) ((rgb >> 8) & 0xFF);  // G
                data[i + 2] = (byte) ((rgb >> 16) & 0xFF); // R
            }
        }
        mat.data().put(data);
        Mat gray = new Mat();
        opencv_imgproc.cvtColor(mat, gray, opencv_imgproc.COLOR_BGR2GRAY);
        mat.release();
        return gray;
    }

    // ==================== 标注截图 ====================

    private String saveAnnotated(BufferedImage screen, int[] lt, int[] rt, int[] lb, int[] rb,
                                 List<Cell> cells, int mIdx, int tIdx, int cols) {
        try {
            File dir = new File(OUT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            BufferedImage img = deepCopy(screen);
            Graphics2D g = img.createGraphics();
            g.setFont(new Font("SansSerif", Font.BOLD, 13));

            // 田区四角连线
            g.setColor(new Color(47, 133, 90));
            g.setStroke(new java.awt.BasicStroke(2f));
            g.drawLine(lt[0], lt[1], rt[0], rt[1]);
            g.drawLine(rt[0], rt[1], rb[0], rb[1]);
            g.drawLine(rb[0], rb[1], lb[0], lb[1]);
            g.drawLine(lb[0], lb[1], lt[0], lt[1]);

            for (Cell cell : cells) {
                if ("GROWING".equals(cell.verdict())) {
                    g.setColor(Color.RED);
                } else if ("EMPTY".equals(cell.verdict())) {
                    g.setColor(Color.GREEN);
                } else {
                    g.setColor(Color.YELLOW);
                }
                g.drawRect(cell.x0(), cell.y0(), cell.w(), cell.h());
                String label = cell.verdict().substring(0, 1);
                g.drawString(label, cell.center().x() - 3, cell.center().y() + 4);
            }

            // 手规则标注：M=蓝框（手盖住，不可信），T=紫框（手加成判空目标）
            g.setStroke(new java.awt.BasicStroke(3f));
            if (mIdx >= 0 && mIdx < cells.size()) {
                Cell m = cells.get(mIdx);
                g.setColor(Color.BLUE);
                g.drawRect(m.x0() - 2, m.y0() - 2, m.w() + 4, m.h() + 4);
                g.drawString("M手", m.center().x() + m.w() / 2 + 4, m.center().y() - m.h() / 2);
            }
            if (tIdx >= 0 && tIdx < cells.size()) {
                Cell t = cells.get(tIdx);
                g.setColor(new Color(200, 0, 200));
                g.drawRect(t.x0() - 2, t.y0() - 2, t.w() + 4, t.h() + 4);
                g.drawString("T手目标", t.center().x() + t.w() / 2 + 4, t.center().y() - t.h() / 2);
            }
            g.setStroke(new java.awt.BasicStroke(1f));

            g.setColor(Color.WHITE);
            g.drawString("E=EMPTY(空田)  G=GROWING(在长)  U=UNKNOWN(未知)  蓝=M(手)  紫=T(手加成目标)", 20, 30);
            g.dispose();

            File out = new File(dir, "scan_" + System.currentTimeMillis() + ".png");
            ImageIO.write(img, "png", out);
            return out.getPath();
        } catch (Exception e) {
            System.out.println("保存标注截图失败: " + e.getMessage());
            return "(失败)";
        }
    }

    private BufferedImage deepCopy(BufferedImage src) {
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), src.getType());
        Graphics2D g = copy.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return copy;
    }

    // ==================== 工具 ====================

    private Point findQuiet(String template) {
        try {
            return imageMatcher.findButton(template);
        } catch (Exception e) {
            return null;
        }
    }

    private BufferedImage loadResource(String path) throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("找不到图片 " + path);
            }
            return ImageIO.read(in);
        }
    }

    /** 单格扫描结果 */
    private record Cell(Point center, String verdict, int x0, int y0, int w, int h) {
    }

    /** 某一缩放档的灰度模板 */
    private record ScaledTpl(double scale, Mat gray) {
    }

    /** 多尺度匹配结果：最高分 + 对应缩放 */
    private record Tm(double score, double scale) {
    }
}
