package org.test;

import org.bytedeco.opencv.opencv_core.Point;
import org.core.ImageMatcher;
import org.core.MouseController;
import org.core.StatsLog;
import org.game.actions.GoHomeAction;
import org.game.config.GameConfig;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 好友偷菜角标 + 拜访按钮匹配 demo（会点击鼠标，仅用于验证识别）
 *
 * 流程：
 * 1. 在主页找到并点击「好友」按钮，进入好友页
 * 2. 识别页面上的「偷菜角标」（戴墨镜的小手），聚类得到每个可偷好友的位置
 * 3. 识别页面上所有「拜访」按钮，聚类
 * 4. 将每个角标与同排（Y 接近）、且在角标右侧最近的拜访按钮配对
 * 5. 点击第一个配对到的拜访按钮进入好友农场
 * 6. 检测「回家」按钮，确认已成功进入好友农场
 * 7. 识别「一键偷菜」按钮：找到则点击偷菜，没有则直接回家
 * 8. 最后点「回家」返回自己的农场（复用 GoHomeAction，带复查）
 *
 * 说明：
 * - ImageMatcher.findAllButtons 会对每个命中像素返回一个点，所以这里做了邻近聚类，
 *   把距离 25px 以内的点合并成一个目标。
 * - 本 demo 不负责关闭好友页/返回主页，验证完请手动关掉（或按回家按钮）。
 *
 * 运行方式：
 *   mvn test-compile exec:java -Dexec.mainClass="org.test.FriendStealBadgeDemo" -Dexec.classpathScope=test
 *   （运行前请将游戏停留在农场主页）
 */
public class FriendStealBadgeDemo {

    private static final String OUT_DIR = "target/friend_demo";
    private static final long START_DELAY_MS = 5000;   // 等投屏画面就绪
    private static final double PAGE_WAIT_SECONDS = 2; // 等页面打开/跳转

    /** 聚类半径（px），距离小于此值的匹配点算同一个目标 */
    private static final int CLUSTER_RADIUS = 25;
    /** 同排行差（px）：角标与拜访按钮的 Y 坐标差在此范围内视为同一行 */
    private static final int ROW_TOLERANCE = 60;

    private final ImageMatcher imageMatcher;
    private final MouseController mouse;
    private final Robot robot;
    private final GoHomeAction goHomeAction;

    public FriendStealBadgeDemo() throws AWTException {
        this.imageMatcher = new ImageMatcher();
        this.mouse = new MouseController();
        this.robot = new Robot();
        this.goHomeAction = new GoHomeAction(mouse, imageMatcher);
    }

    /**
     * 执行一次完整 demo：点好友 → 识别角标 → 匹配拜访按钮 → 点击进入 → 确认
     */
    public void run() throws Exception {
        // 1. 截一张进入前的图，便于排查
        saveScreenshot("01_before_click");

        // 2. 找到主页「好友」按钮并点击
        Point friendButton = imageMatcher.findButton(GameConfig.IMG_HOME_FRIEND);
        if (friendButton == null) {
            System.out.println("结论: 未检测到主页好友按钮，请确认当前在农场主页，且模板/分辨率匹配");
            return;
        }
        System.out.println("找到主页好友按钮: (" + friendButton.x() + ", " + friendButton.y() + ")，点击进入...");
        mouse.moveToAndClick(friendButton.x(), friendButton.y(), MouseController.ClickType.LEFT);
        mouse.waitFor(PAGE_WAIT_SECONDS);

        // 3. 截一张好友页的图
        saveScreenshot("02_friend_page");

        // 4. 识别偷菜角标（聚类后每个点代表一个可偷好友）
        List<Point> badges = cluster(imageMatcher.findAllButtons(GameConfig.IMG_STEAL_BADGE));
        System.out.println("---- 角标检测 ----");
        System.out.println("偷菜角标: " + badges.size() + " 个");
        for (int i = 0; i < badges.size(); i++) {
            Point p = badges.get(i);
            System.out.println("  角标 " + (i + 1) + ": (" + p.x() + ", " + p.y() + ")");
        }
        if (badges.isEmpty()) {
            System.out.println("结论: 未识别到偷菜角标，流程中止");
            return;
        }

        // 5. 识别所有拜访按钮（聚类）
        List<Point> visitButtons = cluster(imageMatcher.findAllButtons(GameConfig.IMG_VISIT_BUTTON));
        System.out.println("---- 拜访按钮检测 ----");
        System.out.println("拜访按钮: " + visitButtons.size() + " 个");
        for (int i = 0; i < visitButtons.size(); i++) {
            Point p = visitButtons.get(i);
            System.out.println("  按钮 " + (i + 1) + ": (" + p.x() + ", " + p.y() + ")");
        }
        if (visitButtons.isEmpty()) {
            System.out.println("结论: 未识别到拜访按钮，流程中止");
            return;
        }

        // 6. 角标 ↔ 同排最近的拜访按钮 配对
        System.out.println("---- 角标与拜访按钮配对 ----");
        List<Point> matchedButtons = new ArrayList<>();
        for (int i = 0; i < badges.size(); i++) {
            Point badge = badges.get(i);
            Point button = findNearestVisitButton(badge, visitButtons);
            if (button != null) {
                matchedButtons.add(button);
                System.out.println("  角标 " + (i + 1) + " (" + badge.x() + ", " + badge.y() + ")"
                        + " → 拜访 (" + button.x() + ", " + button.y() + ")");
            } else {
                System.out.println("  角标 " + (i + 1) + " (" + badge.x() + ", " + badge.y() + ")"
                        + " → 未找到同行拜访按钮");
            }
        }
        if (matchedButtons.isEmpty()) {
            System.out.println("结论: 角标与拜访按钮配对失败，流程中止");
            return;
        }

        // 7. 点击第一个配对到的拜访按钮
        Point target = matchedButtons.get(0);
        System.out.println("点击拜访按钮: (" + target.x() + ", " + target.y() + ")...");
        mouse.moveToAndClick(target.x(), target.y(), MouseController.ClickType.LEFT);
        StatsLog.event("VISIT");
        mouse.waitFor(PAGE_WAIT_SECONDS);

        // 8. 截图 + 检测「回家」按钮，确认进入好友农场
        saveScreenshot("03_after_visit_click");
        boolean entered = false;
        try {
            entered = imageMatcher.findButton(GameConfig.IMG_GO_HOME) != null;
        } catch (Exception e) {
            System.out.println("回家按钮检测异常: " + e.getMessage());
        }
        if (!entered) {
            System.out.println("---- 检测结果 ----");
            System.out.println("结论: 点击后未检测到回家按钮（没进去？跳转慢？看 03 截图），流程中止");
            return;
        }
        System.out.println("成功进入好友农场（检测到回家按钮）");

        // 9. 识别「一键偷菜」按钮：找到就点，没有就跳过
        boolean stealClicked = false;
        try {
            Point stealButton = imageMatcher.findButton(GameConfig.IMG_STEAL_ALL_BUTTON);
            if (stealButton != null) {
                System.out.println("检测到一键偷菜按钮: (" + stealButton.x() + ", " + stealButton.y() + ")，点击偷菜...");
                mouse.moveToAndClick(stealButton.x(), stealButton.y(), MouseController.ClickType.LEFT);
                StatsLog.event("STEAL");
                mouse.waitFor(PAGE_WAIT_SECONDS);
                stealClicked = true;
                saveScreenshot("04_after_steal");
            } else {
                System.out.println("未检测到一键偷菜按钮（好友无可偷作物？看 04 截图）");
                saveScreenshot("04_no_steal_button");
            }
        } catch (Exception e) {
            System.out.println("一键偷菜检测异常: " + e.getMessage());
        }

        // 10. 回家（偷没偷到都回自己的农场，复用 GoHomeAction 的复查逻辑）
        System.out.println("执行回家...");
        goHomeAction.ensureHome();

        // 11. 汇总
        System.out.println("---- 检测结果 ----");
        System.out.println("进入好友农场: ✔");
        System.out.println("一键偷菜: " + (stealClicked ? "✔ 已点击" : "✘ 未检测到/未点击"));
        System.out.println("回家: 见上方日志（回家成功 / 已在家 / 多次点击未成功）");
    }

    /**
     * 为一个角标找最匹配的拜访按钮：
     * 同行（|ΔY| <= ROW_TOLERANCE）且在角标右侧（按钮 X >= 角标 X）中，水平距离最近的；
     * 若右侧没有同行按钮，则退化为同行中距离最近的。
     */
    private Point findNearestVisitButton(Point badge, List<Point> buttons) {
        Point bestRight = null;
        int bestRightDx = Integer.MAX_VALUE;
        Point bestAny = null;
        int bestAnyDist = Integer.MAX_VALUE;

        for (Point btn : buttons) {
            int dy = Math.abs(btn.y() - badge.y());
            if (dy > ROW_TOLERANCE) continue;

            if (btn.x() >= badge.x()) {
                int dx = btn.x() - badge.x();
                if (dx < bestRightDx) {
                    bestRightDx = dx;
                    bestRight = btn;
                }
            }

            int dist = dx2(badge, btn);
            if (dist < bestAnyDist) {
                bestAnyDist = dist;
                bestAny = btn;
            }
        }
        return bestRight != null ? bestRight : bestAny;
    }

    private int dx2(Point a, Point b) {
        int dx = a.x() - b.x();
        int dy = a.y() - b.y();
        return dx * dx + dy * dy;
    }

    /**
     * 邻近点聚类：把距离 CLUSTER_RADIUS 以内的匹配点合并为一个目标（取均值位置）
     */
    private List<Point> cluster(List<Point> points) {
        List<Point> merged = new ArrayList<>();
        boolean[] used = new boolean[points.size()];

        for (int i = 0; i < points.size(); i++) {
            if (used[i]) continue;
            used[i] = true;

            List<Point> group = new ArrayList<>();
            group.add(points.get(i));

            for (int j = i + 1; j < points.size(); j++) {
                if (used[j]) continue;
                Point a = points.get(i);
                Point b = points.get(j);
                if (Math.abs(a.x() - b.x()) <= CLUSTER_RADIUS && Math.abs(a.y() - b.y()) <= CLUSTER_RADIUS) {
                    used[j] = true;
                    group.add(b);
                }
            }

            long sumX = 0, sumY = 0;
            for (Point p : group) {
                sumX += p.x();
                sumY += p.y();
            }
            merged.add(new Point((int) (sumX / group.size()), (int) (sumY / group.size())));
        }
        return merged;
    }

    /**
     * 截取全屏并保存到 target/friend_demo/
     */
    private void saveScreenshot(String name) {
        try {
            BufferedImage screen = robot.createScreenCapture(
                    new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            File dir = new File(OUT_DIR);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, name + "_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd_HHmmss")) + ".png");
            ImageIO.write(screen, "png", file);
            System.out.println("截图: " + file.getPath());
        } catch (Exception e) {
            System.out.println("截图失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 好友偷菜角标 + 拜访按钮匹配 demo ===");
        System.out.println("等待 " + START_DELAY_MS / 1000 + " 秒，请确保投屏画面已显示...");
        System.out.println("（运行前请将游戏停留在农场主页）");

        try {
            Thread.sleep(START_DELAY_MS);

            FriendStealBadgeDemo demo = new FriendStealBadgeDemo();
            demo.run();

            System.out.println();
            System.out.println("demo 结束（正常情况下已自动回家；如未回家请手动点回家按钮）");
        } catch (InterruptedException e) {
            System.out.println("已停止");
        } catch (Exception e) {
            System.out.println("demo 运行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
