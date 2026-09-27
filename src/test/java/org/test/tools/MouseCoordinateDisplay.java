package org.test.tools;

import javax.swing.*;
import java.awt.*;

/**
 * 鼠标坐标实时显示工具
 * 左上角悬浮窗实时显示鼠标当前的 X、Y 坐标（分两行显示）
 * 按 ESC 退出
 */
public class MouseCoordinateDisplay {

    private JFrame frame;
    private JLabel xLabel;
    private JLabel yLabel;
    private JLabel hintLabel;
    private Timer timer;

    public MouseCoordinateDisplay() {
        // 创建窗口
        frame = new JFrame("坐标工具");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 180));

        // 创建面板
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        panel.setOpaque(false);

        // X坐标标签
        xLabel = new JLabel("X: 0000");
        xLabel.setForeground(Color.GREEN);
        xLabel.setFont(new Font("Consolas", Font.BOLD, 18));

        // Y坐标标签
        yLabel = new JLabel("Y: 0000");
        yLabel.setForeground(Color.GREEN);
        yLabel.setFont(new Font("Consolas", Font.BOLD, 18));

        // 提示标签
        hintLabel = new JLabel("按 ESC 退出");
        hintLabel.setForeground(Color.WHITE);
        hintLabel.setFont(new Font("微软雅黑", Font.PLAIN, 11));

        panel.add(xLabel);
        panel.add(yLabel);
        panel.add(Box.createVerticalStrut(4));
        panel.add(hintLabel);

        frame.add(panel);
        frame.pack();

        // 放在左上角
        frame.setLocation(10, 10);
        frame.setVisible(true);

        // 注册 ESC 键监听
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            if (e.getID() == java.awt.event.KeyEvent.KEY_PRESSED && e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                stop();
            }
            return false;
        });

        // 定时刷新鼠标坐标（每 50ms）
        timer = new Timer(50, e -> updateCoordinate());
        timer.start();
    }

    private void updateCoordinate() {
        Point mousePos = MouseInfo.getPointerInfo().getLocation();
        xLabel.setText(String.format("X: %d", mousePos.x));
        yLabel.setText(String.format("Y: %d", mousePos.y));
    }

    private void stop() {
        if (timer != null) {
            timer.stop();
        }
        frame.dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            System.out.println("坐标工具已启动，鼠标移动查看坐标，按 ESC 退出");
            new MouseCoordinateDisplay();
        });
    }
}
