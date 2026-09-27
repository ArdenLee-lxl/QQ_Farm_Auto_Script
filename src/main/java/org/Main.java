package org;

import lombok.extern.slf4j.Slf4j;
import org.game.QQFarmController;

@Slf4j
public class Main {

    /**
     * QQ农场自动化脚本入口
     *
     * 使用方法：
     * 1. 请先通过新手教程
     * 2. 请将页面停留在游戏主界面
     * 3. 确保投屏软件已连接并显示在电脑上
     * 4. 运行程序
     */
    public static void main(String[] args) {
        log.info("=== QQ农场自动化脚本启动 ===");
        log.info("等待 5 秒，请确保投屏画面已显示...");

        try {
            Thread.sleep(5000);

            QQFarmController controller = new QQFarmController();
            controller.run();
        } catch (Exception e) {
            log.error("程序启动失败: {}", e.getMessage(), e);
        }
    }
}
