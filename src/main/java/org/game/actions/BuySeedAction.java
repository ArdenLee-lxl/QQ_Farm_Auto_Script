package org.game.actions;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Point;
import org.config.CoordinateConfig;
import org.core.ImageMatcher;
import org.core.MouseController;
import org.core.StatsLog;
import org.game.config.GameConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 购买种子操作
 * 打开商店 → 找最贵种子 → 购买 → 关闭商店
 */
@Slf4j
public class BuySeedAction {

    private final MouseController mouse;
    private final ImageMatcher imageMatcher;

    public BuySeedAction(MouseController mouse, ImageMatcher imageMatcher) {
        this.mouse = mouse;
        this.imageMatcher = imageMatcher;
    }

    /**
     * 执行购买种子操作
     */
    public void execute() throws IOException {
        log.info("开始购买种子...");

        // 1. 点击商店按钮
        int[] storeLocation = CoordinateConfig.getPoint("qqFarm.store.location");
        mouse.moveToAndClick(storeLocation[0], storeLocation[1], MouseController.ClickType.LEFT);
        mouse.waitFor(2);

        // 2. 找到最贵的种子（没有锁的）
        Point bestSeed = findBestSeed();
        if (bestSeed == null) {
            log.warn("未找到可购买的种子");
            return;
        }
        log.info("找到最贵的种子，位置: ({}, {})", bestSeed.x(), bestSeed.y());

        // 3. 点击最贵的种子
        mouse.moveToAndClick(bestSeed.x(), bestSeed.y(), MouseController.ClickType.LEFT);
        mouse.waitFor(1);

        // 4. 点击确认购买
        Point confirmButton = imageMatcher.findButton(GameConfig.IMG_BUY_CONFIRM);
        if (confirmButton != null) {
            log.info("点击确认购买");
            mouse.moveToAndClick(confirmButton.x(), confirmButton.y(), MouseController.ClickType.LEFT);
            mouse.waitFor(1);
            StatsLog.event("BUY_SEED");
        } else {
            log.warn("未找到确认按钮");
        }

        // 5. 关闭商店
        Point closeButton = imageMatcher.findButton(GameConfig.IMG_SHOP_CLOSE);
        if (closeButton != null) {
            log.info("关闭商店");
            mouse.moveToAndClick(closeButton.x(), closeButton.y(), MouseController.ClickType.LEFT);
            mouse.waitFor(2);
        } else {
            log.warn("未找到关闭按钮");
        }

        log.info("购买种子完成");
    }

    /**
     * 找到最贵的种子（没有锁的）
     * 从右下角开始扫描，右下角是最贵的种子
     */
    private Point findBestSeed() {
        int gridWidth = GameConfig.GRID_RIGHT - GameConfig.GRID_LEFT;
        int gridHeight = GameConfig.GRID_BOTTOM - GameConfig.GRID_TOP;
        int cellWidth = gridWidth / GameConfig.GRID_COLS;
        int cellHeight = gridHeight / GameConfig.GRID_ROWS;

        // 找到所有锁的位置
        List<Point> locks = findAllLocks();
        log.debug("找到 {} 个锁", locks.size());

        // 从右下角开始扫描（最贵的在右下角）
        for (int row = GameConfig.GRID_ROWS - 1; row >= 0; row--) {
            for (int col = GameConfig.GRID_COLS - 1; col >= 0; col--) {
                int cellX = GameConfig.GRID_LEFT + col * cellWidth;
                int cellY = GameConfig.GRID_TOP + row * cellHeight;
                int centerX = cellX + cellWidth / 2;
                int centerY = cellY + cellHeight / 2;

                // 检查这个格子是否有锁
                if (!cellHasLock(cellX, cellY, cellWidth, cellHeight, locks)) {
                    log.info("找到最贵种子: 第{}行 第{}列 ({},{})", row + 1, col + 1, centerX, centerY);
                    return new Point(centerX, centerY);
                }
            }
        }

        return null;
    }

    /**
     * 找到屏幕上所有的锁位置
     */
    private List<Point> findAllLocks() {
        List<Point> locks = new ArrayList<>();
        try {
            locks = imageMatcher.findAllButtons(GameConfig.IMG_SHOP_LOCK);
        } catch (Exception e) {
            log.debug("查找锁时出错: {}", e.getMessage());
        }
        return locks;
    }

    /**
     * 检查格子是否包含锁
     */
    private boolean cellHasLock(int cellX, int cellY, int cellW, int cellH, List<Point> locks) {
        for (Point lock : locks) {
            if (lock.x() >= cellX && lock.x() <= cellX + cellW &&
                lock.y() >= cellY && lock.y() <= cellY + cellH) {
                return true;
            }
        }
        return false;
    }
}
