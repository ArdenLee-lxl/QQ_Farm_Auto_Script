package org.game;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Point;
import org.core.ImageMatcher;
import org.core.MouseController;
import org.core.StatsLog;
import org.game.actions.BuySeedAction;
import org.game.actions.FieldCheckAction;
import org.game.actions.GoHomeAction;
import org.game.actions.HarvestAction;
import org.game.actions.LoginCheckAction;
import org.game.actions.SowSeedAction;
import org.game.actions.VisitFriendAction;
import org.game.actions.WelcomeDialogAction;
import org.game.config.GameConfig;

import java.io.IOException;

/**
 * QQ农场主控制器
 * 负责协调收获、购买种子、播种的自动化流程
 */
@Slf4j
public class QQFarmController {

    private final MouseController mouse;
    private final ImageMatcher imageMatcher;
    private final HarvestAction harvestAction;
    private final BuySeedAction buySeedAction;
    private final SowSeedAction sowSeedAction;
    private final LoginCheckAction loginCheckAction;
    private final FieldCheckAction fieldCheckAction;
    private final WelcomeDialogAction welcomeDialogAction;
    private final GoHomeAction goHomeAction;
    private final VisitFriendAction visitFriendAction;

    public QQFarmController() throws Exception {
        this.mouse = new MouseController();
        this.imageMatcher = new ImageMatcher();
        this.harvestAction = new HarvestAction(mouse, imageMatcher);
        this.buySeedAction = new BuySeedAction(mouse, imageMatcher);
        this.sowSeedAction = new SowSeedAction(mouse, imageMatcher);
        this.loginCheckAction = new LoginCheckAction(mouse, imageMatcher);
        this.fieldCheckAction = new FieldCheckAction(mouse, imageMatcher);
        this.welcomeDialogAction = new WelcomeDialogAction(mouse, imageMatcher);
        this.goHomeAction = new GoHomeAction(mouse, imageMatcher);
        this.visitFriendAction = new VisitFriendAction(mouse, imageMatcher);
    }

    /**
     * 启动自动化循环
     *
     * 流程：
     * 1. 购买种子
     * 2. 播种
     * 3. 循环等待一键收获按钮出现
     * 4. 点击收获
     * 5. 等待几秒
     * 6. 重复
     */
    public void run() throws InterruptedException {
        log.info("=== QQ农场自动化启动 ===");
        log.info("按 Ctrl+C 停止程序");
        log.info("等待 5 秒，请确保投屏画面已显示...");
        StatsLog.event("RUN_START");
        Thread.sleep(5000);

        // 初始化检测0：若脚本启动时停留在好友农场页，先回家
        try {
            goHomeAction.ensureHome();
        } catch (Exception e) {
            log.warn("启动回家检查异常: {}", e.getMessage());
        }

        // 初始化检测1：关闭欢迎弹窗
        try {
            int closedCount = welcomeDialogAction.closeAllWelcomeDialogs();
            if (closedCount > 0) {
                log.info("等待1秒让界面稳定...");
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            log.warn("关闭欢迎弹窗异常: {}", e.getMessage());
        }

        // 初始化检测2：检查是否有登录失败弹窗
        if (loginCheckAction.hasLoginDialog()) {
            log.info("检测到登录失败弹窗，点击重新登录...");
            StatsLog.event("RELOGIN");
            try {
                loginCheckAction.execute();
                log.info("等待3秒让游戏重新加载...");
                Thread.sleep(3000);
            } catch (Exception e) {
                log.warn("重新登录异常: {}", e.getMessage());
            }
        } else {
            log.info("未检测到登录失败弹窗");
        }

        // 初始化检测3：检查是否有成熟作物
        if (harvestAction.hasReadyCrops()) {
            log.info("检测到成熟作物，先执行收获...");
            try {
                harvestAction.execute();
                log.info("收获完成，等待2秒后购买/播种...");
                Thread.sleep(2000);

                // 播种（优先使用已有种子，不够才去买）
                log.info("步骤1：播种");
                plantSeeds();
            } catch (Exception e) {
                log.warn("初始收获异常: {}", e.getMessage());
            }
        } else {
            // 没有成熟作物，检测田地状态
            log.info("未检测到成熟作物，检测田地状态...");

            try {
                if (fieldCheckAction.isFieldSeeded()) {
                    // 田地已播种，直接等待收获
                    log.info("田地已播种，直接进入等待收获...");
                } else {
                    // 田地空闲，播种（优先使用已有种子）
                    log.info("田地空闲，播种（优先使用已有种子）...");
                    plantSeeds();
                }
            } catch (Exception e) {
                log.warn("田地检测异常: {}", e.getMessage());
            }
        }

        int round = 1;
        int checkInterval = 10000;  // 每10秒检测一次
        int maxWaitTime = 30 * 60 * 1000;  // 复查间隔：等待30分钟未成熟则复查田地后继续等

        try {
            while (true) {
                log.info("=== 第 {} 轮开始 ===", round);
                StatsLog.event("ROUND_START", "round=" + round);

                // 等待作物成熟（单一循环检测所有按钮）
                log.info("等待作物成熟...");
                int waited = 0;
                long lastFriendVisit = 0;  // 置0：每轮等待开始就先偷一轮，之后每30秒一轮

                while (waited < maxWaitTime) {
                    // 1. 检测一键务农按钮
                    Point farmButton = imageMatcher.findButton(GameConfig.IMG_FARM_BUTTON);
                    if (farmButton != null) {
                        log.info("检测到一键务农按钮，点击加速生长");
                        StatsLog.event("FARM_CLICK", "source=wait");
                        mouse.moveToAndClick(farmButton.x(), farmButton.y(), MouseController.ClickType.LEFT);
                        mouse.waitFor(1);
                        waited = 0;
                        continue;
                    } else {
                        log.info("未检测到一键务农");
                    }

                    // 2. 检测一键收获按钮
                    Point harvestButton = imageMatcher.findButton(GameConfig.IMG_HARVEST_BUTTON);
                    if (harvestButton != null) {
                        log.info("检测到一键收获按钮！");
                        break;
                    } else {
                        log.info("未检测到一键收获，{}秒后再检测...", checkInterval / 1000);
                    }

                    // 3. 等待期每30秒去好友农场偷一轮菜（仅等待阶段，启动初始化段不含）
                    if (System.currentTimeMillis() - lastFriendVisit >= GameConfig.FRIEND_VISIT_INTERVAL_MS) {
                        log.info("等待期开始好友偷菜流程...");
                        visitFriendAction.execute();
                        lastFriendVisit = System.currentTimeMillis();
                    }

                    Thread.sleep(checkInterval);
                    waited += checkInterval;
                }

                if (waited >= maxWaitTime) {
                    log.warn("等待超时，未检测到一键收获按钮，执行田地复查...");
                    StatsLog.event("WAIT_TIMEOUT");
                    try {
                        recoverFieldAfterTimeout();
                    } catch (InterruptedException e) {
                        throw e;  // 保持中断语义，交由外层处理"用户中断"
                    } catch (Exception e) {
                        log.warn("超时复查异常: {}，进入下一轮等待", e.getMessage());
                    }
                    round++;
                    continue;  // 不退出，开新一轮30分钟等待
                }

                // 收获
                log.info("点击一键收获");
                harvestAction.execute();
                Thread.sleep(2000);

                // 收获后播种（优先使用已有种子，不够才去买）
                log.info("收获后播种");
                plantSeeds();
                Thread.sleep(1000);

                log.info("=== 第 {} 轮完成 ===", round);
                round++;
            }
        } catch (InterruptedException e) {
            log.info("程序被用户中断");
        } catch (IOException e) {
            log.error("IO异常: {}", e.getMessage());
        } catch (Exception e) {
            log.error("程序出错: {}", e.getMessage());
        }
    }

    /**
     * 等待超时后的田地复查（不退出程序）
     * 根据超时原因恢复：
     * 1. 有成熟作物（刚好卡在超时点成熟）→ 收获 → 买种 → 播种
     * 2. 田地已播种（作物生长变慢，正常情况）→ 不动，继续等待
     * 3. 田地空闲（播种失败/被清空等异常）→ 买种 → 播种补种
     */
    private void recoverFieldAfterTimeout() throws Exception {
        log.info("超时复查：检查作物与田地状态...");

        if (harvestAction.hasReadyCrops()) {
            log.info("超时复查：发现成熟作物，执行收获...");
            harvestAction.execute();
            Thread.sleep(2000);

            log.info("超时复查：播种（优先使用已有种子）");
            plantSeeds();
        } else if (fieldCheckAction.isFieldSeeded()) {
            log.info("超时复查：田地已播种，作物生长较慢，继续等待");
        } else {
            log.info("超时复查：田地空闲，播种（优先使用已有种子）...");
            plantSeeds();
        }

        log.info("超时复查完成，进入下一轮等待");
    }

    /**
     * 播种：优先使用已有的种子
     * 1. 先直接尝试一键播种（点第一块田 → 点种子）
     * 2. 播种失败（背包里没有种子）→ 去商店购买 → 再播一次
     * 无论哪步异常都不中断主流程
     */
    private void plantSeeds() {
        try {
            log.info("播种：先尝试使用已有种子");
            if (sowSeedAction.execute()) {
                return;
            }

            log.info("没有可用种子，去商店购买...");
            buySeedAction.execute();

            log.info("购买完成，再次播种");
            sowSeedAction.execute();
        } catch (Exception e) {
            log.warn("播种流程异常: {}", e.getMessage());
        }
    }
}
