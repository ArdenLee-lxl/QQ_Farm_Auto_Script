package org.core;

import javax.swing.*;
import java.awt.*;

public class StatusWindow {
    private JFrame frame;
    private JLabel statusLabel;
    private JLabel progressLabel;
    private JLabel completedLabel;
    private volatile boolean isCompleted = false;
    private volatile boolean disposed = false;

    public StatusWindow() {
        // 创建窗口
        frame = new JFrame("运行状态");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setBackground(new Color(0, 0, 0, 128)); // 半透明背景

        // 创建面板
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setOpaque(false);

        // 创建标签
        statusLabel = new JLabel("状态: 初始化");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));

        progressLabel = new JLabel("进度: 0%");
        progressLabel.setForeground(Color.WHITE);
        progressLabel.setFont(new Font("微软雅黑", Font.PLAIN, 12));

        completedLabel = new JLabel("未完成");
        completedLabel.setForeground(Color.YELLOW);
        completedLabel.setFont(new Font("微软雅黑", Font.BOLD, 16));

        // 添加标签到面板
        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(progressLabel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(completedLabel);

        frame.add(panel);
        frame.pack();

        // 设置窗口位置（左上角）
        frame.setLocation(10, 10);

        // 显示窗口
        frame.setVisible(true);
    }

    public void updateStatus(String status, int progress) {
        if (isCompleted) {
            return; // 如果已完成，不更新状态
        }
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("状态: " + status);
            progressLabel.setText("进度: " + progress + "%");
            completedLabel.setText("未完成");
            completedLabel.setForeground(Color.YELLOW);
        });
    }

    public void showCompleted(String message) {
        isCompleted = true;
        SwingUtilities.invokeLater(() -> {
            completedLabel.setText(message);
            completedLabel.setForeground(Color.GREEN);
        });
    }

    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        SwingUtilities.invokeLater(() -> {
            frame.dispose();
        });
    }
}
