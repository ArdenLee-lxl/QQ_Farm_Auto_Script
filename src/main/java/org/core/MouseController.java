package org.core;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * 鼠标控制器 - 负责所有鼠标操作
 * 高内聚 - 所有的鼠标相关操作都在这里
 * 低耦合 - 不依赖具体逻辑，只负责业务
 */
@Slf4j
public class MouseController {

    private final Robot robot;
    @Getter
    private final Dimension screenSize;

    // 点击类型枚举
    public enum ClickType {
        LEFT, RIGHT, Double
    }

    public MouseController() throws AWTException {
        this.robot = new Robot();
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    }

    /**
     * 将鼠标移动到指定坐标位置
     * @param x 目标横坐标
     * @param y 目标纵坐标
     * @return 返回当前对象实例，支持链式调用
     */
    public MouseController moveTo(int x, int y) {
        robot.mouseMove(x, y);
        log.debug("移动鼠标到：({}, {})", x, y);
        robot.delay(500);
        return this;
    }

    /**
     * 平滑移动到目标位置
     * 通过多个步进点模拟真实的鼠标移动轨迹，使移动更加自然流畅
     *
     * @param x 目标横坐标
     * @param y 目标纵坐标
     * @param steps 移动步数，值越大移动越平滑
     * @return 返回当前对象实例，支持链式调用
     */
    public MouseController fluentMoveTo(int x, int y, int steps) {
        // 获取当前位置
        Point currentPos = getCurrentPosition();
        int startX = currentPos.x;
        int startY = currentPos.y;

        // 计算每步的移动距离
        int stepX = (x - startX) / steps;
        int stepY = (y - startY) / steps;

        // 逐步移动到目标位置
        for (int i = 1; i <= steps; i++) {
            int newX = startX + stepX * i;
            int newY = startY + stepY * i;
            robot.mouseMove(newX, newY);
            robot.delay(50); // 每步延迟 50ms
        }

        // 确保到达精确的目标位置
        robot.mouseMove(x, y);

        log.debug("平滑移动到：({}, {}), 步数：{}", x, y, steps);
        return this;
    }

    /**
     * 将鼠标移动到指定坐标并执行点击操作
     * 先移动鼠标到目标位置，然后根据指定的点击类型执行相应的点击动作
     *
     * @param x 目标横坐标
     * @param y 目标纵坐标
     * @param type 点击类型（左键、右键或双击）
     */
    public MouseController moveToAndClick(int x, int y, ClickType type) {
        moveTo(x, y);
        switch (type) {
            case LEFT:
                leftClick();
                break;
            case RIGHT:
                rightClick();
                break;
            case Double:
                doubleClick();
                break;
        }
        log.info("{}点击鼠标: ({}, {})", type, x, y);
        waitFor(2);
        return this;
    }

    /**
     * 执行鼠标左键点击操作
     * 模拟按下并释放鼠标左键的完整动作
     *
     * @return 返回当前对象实例，支持链式调用
     */
    private MouseController leftClick() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        return this;
    }

    /**
     * 执行鼠标右键点击操作
     * 模拟按下并释放鼠标右键的完整动作
     *
     * @return 返回当前对象实例，支持链式调用
     */
    private MouseController rightClick() {
        robot.mousePress(InputEvent.BUTTON3_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON3_DOWN_MASK);
        return this;
    }

    /**
     * 执行鼠标双击操作
     * 模拟按下并释放鼠标左键的完整动作
     *
     * @return 返回当前对象实例，支持链式调用
     */
    private MouseController doubleClick() {
        leftClick();
        delay(100);
        leftClick();
        return this;
    }

    /**
     * 执行鼠标拖拽操作
     * 从起始坐标平滑拖动到目标坐标，通过多个步进点模拟真实的拖拽轨迹
     *
     * @param startX 起始横坐标
     * @param startY 起始纵坐标
     * @param endX 目标横坐标
     * @param endY 目标纵坐标
     * @param steps 拖拽过程中的步进点数，值越大拖拽越平滑
     * @return 返回当前对象实例，支持链式调用
     */
    public MouseController drag(int startX, int startY, int endX, int endY, int steps) {
        moveTo(startX, startY);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        for (int i = 0; i < steps; i++) {
            int x = (int) (startX + (endX - startX) * i / steps);
            int y = (int) (startY + (endY - startY) * i / steps);
            moveTo(x, y);
            delay(10);
        }
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        log.info("拖拽鼠标：({}, {}) -> ({}, {})", startX, startY, endX, endY);
        return this;
    }

    // 鼠标左键长按
    public MouseController leftLongPress() {
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(200);
        return this;
    }

    // 鼠标左键释放
    public MouseController leftRelease() {
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(2000);
        return this;
    }

    /**
     * 滚动鼠标滚轮
     *
     * @param amount 滚动刻度数，负值表示向上滚动（远离用户），正值表示向下滚动（朝向用户）
     * @return 返回当前对象实例，支持链式调用
     */
    public MouseController scroll(int amount) {
        robot.mouseWheel(amount);
        log.info("鼠标滚轮滚动：{}", amount);
        return this;
    }

    /**
     * 延迟指定时间后继续执行
     *
     * @param millis 延迟的毫秒数
     * @return 返回当前对象实例，支持链式调用
     */
    public MouseController delay(int millis) {
        robot.delay(millis);
        log.info("等待：{}ms", millis);
        return this;
    }

    /**
     * 等待指定秒数后继续执行
     *
     * @param seconds 等待的秒数
     * @return 返回当前对象实例，支持链式调用
     */
    public MouseController waitFor(double seconds) {
        delay((int)(seconds * 1000));
        log.info("等待：{}s", seconds);
        return this;
    }

    /**
     * 获取当前鼠标位置
     *
     * @return 当前鼠标的屏幕坐标点
     */
    public Point getCurrentPosition() {
        return MouseInfo.getPointerInfo().getLocation();
    }

    /**
     * 判断给定坐标是否在屏幕有效范围内
     *
     * @param x 待检查的横坐标
     * @param y 待检查的纵坐标
     * @return 如果坐标在屏幕范围内返回 true，否则返回 false
     */
    public boolean isValidPosition(int x, int y) {
        return x >= 0 && x < screenSize.width && y >= 0 && y < screenSize.height;
    }

    /**
     * 执行捏合手势操作（双指缩放）
     * 通过按住 Ctrl 键并拖动鼠标，模拟触控板的双指捏合动作，用于缩小操作
     * 从起始点逐步向屏幕中心滑动，实现平滑的缩放效果
     *
     * @param startX        起始横坐标
     * @param startY        起始纵坐标
     * @param ScreenCenterX 屏幕中心横坐标
     * @param ScreenCenterY 屏幕中心纵坐标
     * @param steps         捏合过程的步数，值越大动作越平滑
     */
    public void pinch(int startX, int startY, int ScreenCenterX, int ScreenCenterY, int steps) {
        log.info("捏合：从 ({},{}) 向中心 ({},{}) 滑动，步数{}", startX, startY, ScreenCenterX, ScreenCenterY, steps);

        // 移动到起始点
        moveTo(startX, startY);
        robot.delay(1000);

        // === 强力按下Ctrl（多重保证）===
        for (int i = 0; i < 5; i++) {  // 连续按5次
            robot.keyPress(KeyEvent.VK_CONTROL);
            robot.delay(50);
        }
        robot.delay(500);

        // 按下鼠标左键
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(200);

        // 计算每次移动距离
        int stepX = (ScreenCenterX - startX) / steps;
        int stepY = (ScreenCenterY - startY) / steps;

        // 逐步向中心移动
        int currentX = startX;
        int currentY = startY;
        for (int i = 1; i <= steps; i++) {
            currentX = startX + stepX * i;
            currentY = startY + stepY * i;
            robot.mouseMove(currentX, currentY);
            robot.delay(10);  // 每步延迟 10ms
        }

        // 确保到达中心点
        robot.mouseMove(ScreenCenterX, ScreenCenterY);
        robot.delay(100);

        // 释放按键
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.keyRelease(KeyEvent.VK_CONTROL);

        log.info("捏合结束");
    }


}
