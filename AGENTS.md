# AGENTS.md - QQ农场自动化脚本

## 项目概述
这是一个QQ农场自动化脚本，使用Java和Maven构建。通过图像识别和鼠标控制实现自动化操作，包括收获、购买种子和播种。

## 构建和运行
```bash
# 编译项目
mvn clean compile

# 运行主程序
mvn exec:java -Dexec.mainClass="org.Main"

# 或者使用Maven直接运行
mvn compile exec:java
```

## 架构
- **Main.java**: 程序入口点，执行自动化任务循环
- **qqFarmRunCases.java**: 具体业务逻辑（收获、购买种子、播种）
- **ImageMatcher.java**: 图像识别，使用OpenCV进行模板匹配
- **MouseController.java**: 鼠标控制，支持平滑移动、点击、拖拽等操作
- **CoordinateConfig.java**: 坐标配置读取，从properties文件加载坐标

## 配置文件
- `src/main/resources/QQFarmCoordinates.properties`: 存储所有屏幕坐标配置
- `src/main/resources/images/`: 存储图像模板（收获按钮、种子等）

## 关键配置
1. **屏幕坐标配置**: 必须根据实际投屏窗口位置调整坐标
2. **图像模板**: 需要为不同分辨率/缩放比例准备对应的图像模板
3. **Java版本**: 需要Java 25+
4. **依赖**: OpenCV、Lombok、SLF4J/Logback

## 开发注意事项
1. **坐标调整**: 修改坐标前，使用`MouseController`获取当前鼠标位置
2. **图像匹配**: 添加新图像模板时，确保分辨率与实际屏幕匹配
3. **异常处理**: 主要异常为AWTException、IOException、InterruptedException
4. **日志**: 使用SLF4J记录操作日志，便于调试

## 常见问题
1. **图像匹配失败**: 检查图像模板是否与当前屏幕分辨率匹配
2. **坐标偏移**: 确保投屏窗口位置与配置文件中的坐标一致
3. **鼠标操作失败**: 确保程序有足够权限模拟鼠标操作

## 测试
```bash
# 运行测试
mvn test

# 单独运行测试类
mvn test -Dtest=Step1_EnterShop
mvn test -Dtest=Step2_FindBestSeed
```

## 依赖管理
- 使用Maven管理依赖
- 主要依赖：OpenCV 4.13.0、Lombok 1.18.42、SLF4J 2.0.17
