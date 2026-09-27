package org.game.actions;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Point;
import org.core.ImageMatcher;
import org.core.MouseController;
import org.core.StatsLog;
import org.game.config.GameConfig;

/**
 * 求助页回家操作
 * 检测"回家"按钮，如果还停留在好友农场页则点击返回自己的农场，
 * 点击后复查，直到按钮消失为止（彻底回家）
 */
@Slf4j
public class GoHomeAction {

    /** 最多尝试点击次数，防止按钮一直存在时死循环 */
    private static final int MAX_ATTEMPTS = 3;
    /** 每次点击后等待页面跳转的时间（秒） */
    private static final double CLICK_WAIT_SECONDS = 1.5;

    private final MouseController mouse;
    private final ImageMatcher imageMatcher;

    public GoHomeAction(MouseController mouse, ImageMatcher imageMatcher) {
        this.mouse = mouse;
        this.imageMatcher = imageMatcher;
    }

    /**
     * 确保回到自己的农场
     * 1. 检测"回家"按钮，不存在说明已在家，直接返回
     * 2. 存在则点击，等待页面跳转后复查
     * 3. 仍在家则继续点击，最多 MAX_ATTEMPTS 次
     */
    public void ensureHome() {
        try {
            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                Point goHomeButton = imageMatcher.findButton(GameConfig.IMG_GO_HOME);

                if (goHomeButton == null) {
                    if (attempt == 1) {
                        log.info("未检测到回家按钮，当前已在自己的农场");
                    } else {
                        log.info("回家成功，已回到自己的农场");
                    }
                    return;
                }

                log.info("检测到回家按钮（第 {}/{} 次），点击返回自己的农场", attempt, MAX_ATTEMPTS);
                mouse.moveToAndClick(goHomeButton.x(), goHomeButton.y(), MouseController.ClickType.LEFT);
                mouse.waitFor(CLICK_WAIT_SECONDS);
                StatsLog.event("GO_HOME", "attempt=" + attempt);
            }

            log.warn("已点击 {} 次仍未离开求助页，请检查回家按钮模板或页面状态", MAX_ATTEMPTS);
        } catch (Exception e) {
            log.warn("回家检查异常: {}", e.getMessage());
        }
    }
}
