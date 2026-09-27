package org.test;

import org.core.ImageMatcher;
import org.game.config.GameConfig;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 农作物检测小测试（只读，不点击鼠标）
 *
 * 通过模板匹配判断当前画面是否有农作物：
 * - 检测到「一键收获」 → 作物已成熟
 * - 检测到「一键务农」 → 作物在生长中
 * - 两者都没有        → 可能是空地 / 画面未就绪
 *
 * 每次检测会保存一张截图到 target/crop_detect/ 便于排查。
 *
 * 运行方式：
 *   mvn test-compile exec:java -Dexec.mainClass="org.test.CropDetectTest" -Dexec.classpathScope=test
 *   （可选参数：检测间隔秒数，>0 则循环检测，Ctrl+C 退出）
 *   例：... -Dexec.args="10"  每 10 秒检测一次
 */
public class CropDetectTest {

    private static final String OUT_DIR = "target/crop_detect";
    private static final long START_DELAY_MS = 5000; // 等投屏画面就绪

    private final ImageMatcher imageMatcher;
    private final Robot robot;

    public CropDetectTest() throws AWTException {
        this.imageMatcher = new ImageMatcher();
        this.robot = new Robot();
    }

    /**
     * 单次检测
     *
     * @return true 如果检测到作物迹象（成熟或生长中）
     */
    public boolean detectOnce() throws Exception {
        // 1. 保存本次截图，便于事后排查
        String screenshotPath = saveScreenshot();

        // 2. 检测「一键收获」（作物成熟）
        boolean hasReady = false;
        try {
            var harvestBtn = imageMatcher.findButton(GameConfig.IMG_HARVEST_BUTTON);
            hasReady = harvestBtn != null;
        } catch (Exception e) {
            System.out.println("[一键收获] 检测异常: " + e.getMessage());
        }

        // 3. 检测「一键务农」（作物生长中）
        boolean hasGrowing = false;
        try {
            var farmBtn = imageMatcher.findButton(GameConfig.IMG_FARM_BUTTON);
            hasGrowing = farmBtn != null;
        } catch (Exception e) {
            System.out.println("[一键务农] 检测异常: " + e.getMessage());
        }

        // 4. 输出结论
        System.out.println("---- 检测结果 ----");
        System.out.println("截图: " + screenshotPath);
        System.out.println("一键收获(成熟): " + (hasReady ? "✔ 检测到" : "✘ 未检测到"));
        System.out.println("一键务农(生长): " + (hasGrowing ? "✔ 检测到" : "✘ 未检测到"));
        if (hasReady) {
            System.out.println("结论: 有农作物，且已成熟可收获");
        } else if (hasGrowing) {
            System.out.println("结论: 有农作物，正在生长");
        } else {
            System.out.println("结论: 未检测到农作物迹象（空地？画面未就绪？模板/分辨率不匹配？）");
        }
        System.out.println();
        return hasReady || hasGrowing;
    }

    /**
     * 截取全屏并保存到 target/crop_detect/
     */
    private String saveScreenshot() throws Exception {
        BufferedImage screen = robot.createScreenCapture(
                new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
        File dir = new File(OUT_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String name = "crop_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd_HHmmss")) + ".png";
        File file = new File(dir, name);
        ImageIO.write(screen, "png", file);
        return file.getPath();
    }

    public static void main(String[] args) {
        int intervalSeconds = 0;
        if (args.length > 0) {
            try {
                intervalSeconds = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("参数无效，按单次检测运行。用法: [检测间隔秒数]");
            }
        }

        System.out.println("=== 农作物检测测试启动 ===");
        System.out.println("等待 " + START_DELAY_MS / 1000 + " 秒，请确保投屏画面已显示...");
        System.out.println("（运行前请将游戏停留在农场主界面）");

        try {
            Thread.sleep(START_DELAY_MS);

            CropDetectTest test = new CropDetectTest();
            test.detectOnce();

            if (intervalSeconds > 0) {
                System.out.println("进入循环检测，每 " + intervalSeconds + " 秒一次，Ctrl+C 退出");
                while (true) {
                    Thread.sleep(intervalSeconds * 1000L);
                    test.detectOnce();
                }
            }
        } catch (InterruptedException e) {
            System.out.println("已停止");
        } catch (Exception e) {
            System.out.println("测试运行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
