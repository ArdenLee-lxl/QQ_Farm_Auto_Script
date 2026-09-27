package org.game.actions;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Point;
import org.core.ImageMatcher;
import org.core.MouseController;
import org.core.StatsLog;
import org.game.config.GameConfig;

import java.io.IOException;

/**
 * 收获操作
 * 检测并点击一键收获按钮
 */
@Slf4j
public class HarvestAction {

    private final MouseController mouse;
    private final ImageMatcher imageMatcher;

    public HarvestAction(MouseController mouse, ImageMatcher imageMatcher) {
        this.mouse = mouse;
        this.imageMatcher = imageMatcher;
    }

    /**
     * 检测是否有成熟作物（只检测不执行收获）
     * @return true 如果检测到一键收获按钮
     */
    public boolean hasReadyCrops() {
        try {
            Point harvestButton = imageMatcher.findButton(GameConfig.IMG_HARVEST_BUTTON);
            return harvestButton != null;
        } catch (Exception e) {
            log.debug("检测成熟作物异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 执行收获操作
     * @return true 如果找到了收获按钮并点击了
     */
    public boolean execute() throws IOException {
        log.info("检测一键收获按钮...");

        Point harvestButton = imageMatcher.findButton(GameConfig.IMG_HARVEST_BUTTON);

        if (harvestButton != null) {
            log.info("找到一键收获按钮！位置: ({}, {})", harvestButton.x(), harvestButton.y());
            mouse.moveToAndClick(harvestButton.x(), harvestButton.y(), MouseController.ClickType.LEFT);
            mouse.waitFor(2);
            StatsLog.event("HARVEST");
            return true;
        } else {
            log.debug("未找到一键收获按钮");
            return false;
        }
    }
}
