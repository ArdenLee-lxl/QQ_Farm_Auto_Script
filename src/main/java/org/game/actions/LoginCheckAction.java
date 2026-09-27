package org.game.actions;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Point;
import org.core.ImageMatcher;
import org.core.MouseController;
import org.game.config.GameConfig;

/**
 * 登录检测操作
 * 检测是否有"平台登录失败"弹窗，如果有则点击"重新登录"
 */
@Slf4j
public class LoginCheckAction {

    private final MouseController mouse;
    private final ImageMatcher imageMatcher;

    public LoginCheckAction(MouseController mouse, ImageMatcher imageMatcher) {
        this.mouse = mouse;
        this.imageMatcher = imageMatcher;
    }

    /**
     * 检测是否有登录失败弹窗（只检测不操作）
     * @return true 如果检测到登录失败弹窗
     */
    public boolean hasLoginDialog() {
        try {
            Point reLoginButton = imageMatcher.findButton(GameConfig.IMG_RELOGIN_BUTTON);
            return reLoginButton != null;
        } catch (Exception e) {
            log.debug("检测登录弹窗异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 执行重新登录操作
     * @return true 如果找到了登录弹窗并点击了重新登录
     */
    public boolean execute() {
        try {
            Point reLoginButton = imageMatcher.findButton(GameConfig.IMG_RELOGIN_BUTTON);
            if (reLoginButton != null) {
                log.info("检测到登录失败弹窗，点击重新登录按钮");
                mouse.moveToAndClick(reLoginButton.x(), reLoginButton.y(), MouseController.ClickType.LEFT);
                mouse.waitFor(3);
                return true;
            } else {
                log.debug("未检测到登录失败弹窗");
                return false;
            }
        } catch (Exception e) {
            log.warn("重新登录操作异常: {}", e.getMessage());
            return false;
        }
    }
}
