package org.game.actions;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Point;
import org.core.ImageMatcher;
import org.core.MouseController;
import org.game.config.GameConfig;

/**
 * 欢迎弹窗检测操作
 * 检测是否有"欢迎回来"弹窗，如果有则点击关闭按钮
 */
@Slf4j
public class WelcomeDialogAction {

    private final MouseController mouse;
    private final ImageMatcher imageMatcher;

    public WelcomeDialogAction(MouseController mouse, ImageMatcher imageMatcher) {
        this.mouse = mouse;
        this.imageMatcher = imageMatcher;
    }

    /**
     * 检测是否有欢迎弹窗
     * @return true 如果检测到欢迎弹窗
     */
    public boolean hasWelcomeDialog() {
        try {
            Point closeButton = imageMatcher.findButton(GameConfig.IMG_WELCOME_CLOSE);
            return closeButton != null;
        } catch (Exception e) {
            log.debug("检测欢迎弹窗异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 关闭欢迎弹窗
     * 可能有多个弹窗，循环检测直到没有
     * @return 关闭的弹窗数量
     */
    public int closeAllWelcomeDialogs() {
        int closedCount = 0;
        int maxAttempts = 5; // 防止无限循环

        for (int i = 0; i < maxAttempts; i++) {
            try {
                Point closeButton = imageMatcher.findButton(GameConfig.IMG_WELCOME_CLOSE);
                if (closeButton != null) {
                    log.info("检测到欢迎弹窗，点击关闭按钮");
                    mouse.moveToAndClick(closeButton.x(), closeButton.y(), MouseController.ClickType.LEFT);
                    mouse.waitFor(1);
                    closedCount++;
                } else {
                    // 没有弹窗了，退出循环
                    break;
                }
            } catch (Exception e) {
                log.warn("关闭欢迎弹窗异常: {}", e.getMessage());
                break;
            }
        }

        if (closedCount > 0) {
            log.info("共关闭 {} 个欢迎弹窗", closedCount);
        }
        return closedCount;
    }
}
