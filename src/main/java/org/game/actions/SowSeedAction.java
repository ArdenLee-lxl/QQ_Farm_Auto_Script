package org.game.actions;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Point;
import org.config.CoordinateConfig;
import org.core.ImageMatcher;
import org.core.MouseController;
import org.core.StatsLog;
import org.game.config.GameConfig;

import java.io.IOException;
import java.util.List;

/**
 * 播种操作
 * 通过角标识别定位种子，点击种子一键种植到所有田地
 */
@Slf4j
public class SowSeedAction {

    private final MouseController mouse;
    private final ImageMatcher imageMatcher;

    public SowSeedAction(MouseController mouse, ImageMatcher imageMatcher) {
        this.mouse = mouse;
        this.imageMatcher = imageMatcher;
    }

    /**
     * 执行播种操作
     * @return true 如果播种成功
     */
    public boolean execute() throws IOException {
        log.info("开始播种...");

        // 1. 点击第一个田地，触发种子栏弹出
        log.info("步骤1：点击第一个田地");
        int[] firstField = CoordinateConfig.getPoint("qqFarm.first.field.location");
        mouse.moveToAndClick(firstField[0], firstField[1], MouseController.ClickType.LEFT);
        mouse.waitFor(2);  // 等待种子栏弹出

        // 2. 识别角标，找到最左边的种子
        log.info("步骤2：识别角标，找到种子");
        Point seedPosition = findSeedPosition();
        if (seedPosition == null) {
            log.warn("未找到种子，无法播种（可能背包里没有种子）");
            exitFieldState();
            return false;
        }

        log.info("种子位置: ({}, {})", seedPosition.x(), seedPosition.y());

        // 3. 点击种子，一键种植到所有田地
        log.info("步骤3：点击种子，一键种植");
        mouse.moveToAndClick(seedPosition.x(), seedPosition.y(), MouseController.ClickType.LEFT);
        mouse.waitFor(2);

        log.info("播种完成！");
        StatsLog.event("SOW");
        return true;
    }

    /**
     * 通过角标识别找到种子位置
     * 在种子栏区域识别所有角标，找到最左边的角标，然后计算种子中心
     *
     * 使用低阈值（0.6）匹配，因为角标中的数字会变化（24、48等）
     */
    private Point findSeedPosition() throws IOException {
        // 在种子栏区域识别角标（使用低阈值，因为数字会变化）
        double badgeThreshold = 0.6;  // 角标匹配阈值（数字变化，需要宽松匹配）
        List<Point> badges = imageMatcher.findButtonsInRegion(
                GameConfig.IMG_SEED_BADGE,
                GameConfig.SEED_BAR_LEFT, GameConfig.SEED_BAR_TOP,
                GameConfig.SEED_BAR_WIDTH, GameConfig.SEED_BAR_HEIGHT,
                badgeThreshold
        );

        log.info("在种子栏找到 {} 个角标", badges.size());

        if (badges.isEmpty()) {
            return null;
        }

        // 按X坐标排序，找到最左边的角标
        badges.sort((a, b) -> Integer.compare(a.x(), b.x()));
        Point leftmostBadge = badges.get(0);

        // 角标右下方就是种子中心
        int seedX = leftmostBadge.x() + GameConfig.BADGE_TO_SEED_OFFSET_X;
        int seedY = leftmostBadge.y() + GameConfig.BADGE_TO_SEED_OFFSET_Y;

        log.info("最左边角标: ({}, {}), 种子中心: ({}, {})",
                leftmostBadge.x(), leftmostBadge.y(), seedX, seedY);

        return new Point(seedX, seedY);
    }

    /**
     * 退出田地选中状态（点击空白处）
     * 播种失败时调用，避免弹出的种子栏挡住后续商店操作
     */
    private void exitFieldState() {
        try {
            int[] exitPos = CoordinateConfig.getPoint("qqFarm.field.check.exit");
            mouse.moveToAndClick(exitPos[0], exitPos[1], MouseController.ClickType.LEFT);
            mouse.waitFor(1);
        } catch (Exception e) {
            log.debug("退出田地状态异常: {}", e.getMessage());
        }
    }
}
