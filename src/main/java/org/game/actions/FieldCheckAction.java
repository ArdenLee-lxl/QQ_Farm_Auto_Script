package org.game.actions;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Point;
import org.config.CoordinateConfig;
import org.core.ImageMatcher;
import org.core.MouseController;
import org.game.config.GameConfig;

/**
 * 田地状态检测操作
 * 点击第一块田地，检测是否有小铲子图标，判断田地是否已播种
 */
@Slf4j
public class FieldCheckAction {

    private final MouseController mouse;
    private final ImageMatcher imageMatcher;

    public FieldCheckAction(MouseController mouse, ImageMatcher imageMatcher) {
        this.mouse = mouse;
        this.imageMatcher = imageMatcher;
    }

    /**
     * 检测田地是否已播种
     * 点击第一块田地，然后检测是否有小铲子图标
     * @return true 如果田地已播种（有小铲子图标）
     */
    public boolean isFieldSeeded() {
        try {
            // 1. 点击第一块田地
            int[] firstField = CoordinateConfig.getPoint("qqFarm.first.field.location");
            mouse.moveToAndClick(firstField[0], firstField[1], MouseController.ClickType.LEFT);
            mouse.waitFor(1);

            // 2. 检测是否有小铲子图标
            Point shovel = imageMatcher.findButton(GameConfig.IMG_SHOVEL);

            // 3. 点击指定坐标退出田地状态检测
            int[] exitPos = CoordinateConfig.getPoint("qqFarm.field.check.exit");
            mouse.moveToAndClick(exitPos[0], exitPos[1], MouseController.ClickType.LEFT);
            mouse.waitFor(0.5);

            if (shovel != null) {
                log.info("检测到小铲子图标，田地已播种");
                return true;
            } else {
                log.info("未检测到小铲子图标，田地空闲");
                return false;
            }
        } catch (Exception e) {
            log.debug("检测田地状态异常: {}", e.getMessage());
            return false;
        }
    }
}
