# Status Window Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在Step2_FindBestSeed测试中添加浮动状态窗口，显示当前步骤和进度，完成时提示用户。

**Architecture:** 创建StatusWindow类，使用JFrame实现半透明浮动窗口，显示在左上角。集成到Step2_FindBestSeed中，在每个步骤更新状态。

**Tech Stack:** Java Swing, AWT

## Global Constraints

- Java 25+
- 无额外依赖（使用Java标准库）
- 窗口显示在左上角
- 半透明背景
- 显示当前步骤名称、进度百分比、完成状态

---

### Task 1: 创建StatusWindow类

**Covers:** 状态窗口创建

**Files:**
- Create: `src/main/java/org/core/StatusWindow.java`

**Interfaces:**
- Consumes: 无
- Produces: StatusWindow类，提供updateStatus和showCompleted方法

- [ ] **Step 1: 创建StatusWindow类框架**

```java
package org.core;

import javax.swing.*;
import java.awt.*;

public class StatusWindow {
    private JFrame frame;
    private JLabel statusLabel;
    private JLabel progressLabel;
    private JLabel completedLabel;
    
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
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("状态: " + status);
            progressLabel.setText("进度: " + progress + "%");
            completedLabel.setText("未完成");
            completedLabel.setForeground(Color.YELLOW);
        });
    }
    
    public void showCompleted(String message) {
        SwingUtilities.invokeLater(() -> {
            completedLabel.setText(message);
            completedLabel.setForeground(Color.GREEN);
        });
    }
    
    public void dispose() {
        SwingUtilities.invokeLater(() -> {
            frame.dispose();
        });
    }
}
```

- [ ] **Step 2: 编译测试**

Run: `mvn compile -q`
Expected: 编译成功

- [ ] **Step 3: 提交**

```bash
git add src/main/java/org/core/StatusWindow.java
git commit -m "feat: add StatusWindow class for floating status display"
```

### Task 2: 集成StatusWindow到Step2_FindBestSeed

**Covers:** 状态窗口集成

**Files:**
- Modify: `src/test/java/org/test/demo/Step2_FindBestSeed.java`

**Interfaces:**
- Consumes: StatusWindow类
- Produces: 更新后的Step2_FindBestSeed，使用StatusWindow显示状态

- [ ] **Step 1: 修改Step2_FindBestSeed类**

```java
// 在类中添加成员变量
private StatusWindow statusWindow;

// 修改构造函数
public Step2_FindBestSeed() throws AWTException {
    this.robot = new Robot();
    this.mouse = new MouseController();
    this.imageMatcher = new ImageMatcher();
    this.statusWindow = new StatusWindow();
}

// 修改run方法，在每个步骤更新状态
public void run() throws IOException, InterruptedException {
    // ... 现有代码 ...
    
    // 步骤1：点击第一块土地
    statusWindow.updateStatus("步骤1：点击第一块土地", 0);
    // ... 现有代码 ...
    statusWindow.updateStatus("步骤1：点击第一块土地", 20);
    
    // 步骤2：识别"去商店看看"提示
    statusWindow.updateStatus("步骤2：识别商店入口", 40);
    // ... 现有代码 ...
    statusWindow.updateStatus("步骤2：识别商店入口", 60);
    
    // 步骤3：点击进入商店
    statusWindow.updateStatus("步骤3：进入商店", 80);
    // ... 现有代码 ...
    statusWindow.updateStatus("步骤3：进入商店", 90);
    
    // 步骤4：找到种子网格并检测锁
    statusWindow.updateStatus("步骤4：查找最贵种子", 95);
    // ... 现有代码 ...
    
    // 完成
    statusWindow.showCompleted("任务完成！");
    // ... 现有代码 ...
}
```

- [ ] **Step 2: 编译测试**

Run: `mvn compile -q`
Expected: 编译成功

- [ ] **Step 3: 运行测试**

Run: `mvn test -Dtest=Step2_FindBestSeed -q`
Expected: 测试通过，窗口显示正常

- [ ] **Step 4: 提交**

```bash
git add src/test/java/org/test/demo/Step2_FindBestSeed.java
git commit -m "feat: integrate StatusWindow into Step2_FindBestSeed"
```

### Task 3: 验证功能

**Covers:** 功能验证

**Files:**
- 无新文件

**Interfaces:**
- Consumes: 更新后的Step2_FindBestSeed
- Produces: 验证结果

- [ ] **Step 1: 运行完整测试**

Run: `mvn test -Dtest=Step2_FindBestSeed`
Expected: 测试通过，窗口显示在左上角

- [ ] **Step 2: 检查窗口显示**

手动验证：
1. 窗口显示在左上角
2. 窗口半透明
3. 显示当前步骤名称
4. 显示进度百分比
5. 完成时显示完成消息并保持显示

- [ ] **Step 3: 提交验证结果**

```bash
git add .
git commit -m "test: verify StatusWindow functionality"
```
