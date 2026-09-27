package org.game.actions;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.opencv.opencv_core.Point;
import org.core.ImageMatcher;
import org.core.MouseController;
import org.core.StatsLog;
import org.game.config.GameConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 等待期访问好友农场（偷菜 / 帮忙务农）
 * 主页 → 好友列表 → 检测偷菜角标与务农角标 → 各自配对拜访按钮 →
 * 进入好友农场 → 点一键偷菜 / 一键务农 → 回家
 * 同一轮内两类角标都会处理：先偷菜，后务农（务农轮会重新打开好友列表并重新检测）
 */
@Slf4j
public class VisitFriendAction {

    /** 打开好友页/进入农场后的等待时间（秒） */
    private static final double PAGE_WAIT_SECONDS = 2;
    /** 聚类半径（px），距离小于此值的匹配点算同一个目标 */
    private static final int CLUSTER_RADIUS = 25;
    /** 角标与拜访按钮的同排行差（px） */
    private static final int ROW_TOLERANCE = 60;

    private final MouseController mouse;
    private final ImageMatcher imageMatcher;
    private final GoHomeAction goHomeAction;

    public VisitFriendAction(MouseController mouse, ImageMatcher imageMatcher) {
        this.mouse = mouse;
        this.imageMatcher = imageMatcher;
        this.goHomeAction = new GoHomeAction(mouse, imageMatcher);
    }

    /**
     * 执行一轮好友访问（偷菜 + 务农）
     * @return true 如果至少成功完成了一次拜访
     */
    public boolean execute() {
        boolean listOpen = false;
        boolean acted = false;
        try {
            // 1. 打开好友列表
            listOpen = openFriendList();
            if (!listOpen) {
                log.info("打开好友列表失败，跳过本轮好友访问");
                return false;
            }

            // 2. 两类角标一起检测
            List<Point> stealBadges = cluster(imageMatcher.findAllButtons(GameConfig.IMG_STEAL_BADGE));
            List<Point> farmBadges = cluster(imageMatcher.findAllButtons(GameConfig.IMG_FARM_BADGE));
            log.info("角标检测: 偷菜 {} 个, 务农 {} 个", stealBadges.size(), farmBadges.size());

            if (stealBadges.isEmpty() && farmBadges.isEmpty()) {
                log.warn("好友页未识别到任何角标，本轮中止");
                StatsLog.event("VISIT_ABORT", "no_badge");
                closeFriendList();
                return false;
            }

            // 3. 偷菜轮（优先）
            if (!stealBadges.isEmpty()) {
                acted = visitAndAct(stealBadges, true);
                listOpen = false;  // visitAndAct 结束后列表必已关闭
            }

            // 4. 务农轮
            if (farmBadges.isEmpty()) {
                // 什么都没剩下，收尾
                if (listOpen) {
                    closeFriendList();
                    listOpen = false;
                }
            } else {
                if (!listOpen) {
                    // 偷菜轮已经离开过列表，重开并重新检测务农角标
                    if (!openFriendList()) {
                        log.warn("重开好友列表失败，跳过务农轮");
                        return acted;
                    }
                    listOpen = true;
                    farmBadges = cluster(imageMatcher.findAllButtons(GameConfig.IMG_FARM_BADGE));
                    if (farmBadges.isEmpty()) {
                        log.info("重新检测后未见务农角标，跳过务农轮");
                        closeFriendList();
                        return acted;
                    }
                }
                acted |= visitAndAct(farmBadges, false);
                listOpen = false;
            }

            if (listOpen) {
                closeFriendList();
            }
            return acted;
        } catch (Exception e) {
            log.warn("好友访问流程异常: {}", e.getMessage());
            StatsLog.event("VISIT_ABORT", "exception");
            try {
                closeFriendList();
                goHomeAction.ensureHome();
            } catch (Exception ignored) {
                // 尽力恢复，失败已在各方法内记录
            }
            return acted;
        }
    }

    /**
     * 对一批同类角标完成：配对拜访 → 进入 → 页内动作 → 回家
     * 前置条件：好友列表当前处于打开状态
     * 后置条件：无论成败，好友列表都已关闭（或已回到自己农场）
     *
     * @param steal true=偷菜动作，false=务农动作
     * @return true 如果成功进入好友农场
     */
    private boolean visitAndAct(List<Point> badges, boolean steal) throws IOException {
        String kind = steal ? "偷菜" : "务农";

        // 1. 识别拜访按钮并配对
        List<Point> visitButtons = cluster(imageMatcher.findAllButtons(GameConfig.IMG_VISIT_BUTTON));
        if (visitButtons.isEmpty()) {
            log.warn("好友页未识别到拜访按钮，{}轮中止", kind);
            StatsLog.event("VISIT_ABORT", steal ? "no_visit_button_steal" : "no_visit_button_farm");
            closeFriendList();
            return false;
        }

        Point target = findNearestVisitButton(badges.get(0), visitButtons);
        if (target == null) {
            log.warn("{}角标与拜访按钮配对失败，本轮中止", kind);
            StatsLog.event("VISIT_ABORT", steal ? "pair_failed_steal" : "pair_failed_farm");
            closeFriendList();
            return false;
        }

        // 2. 点击拜访进入好友农场
        log.info("点击拜访（{}轮）: ({}, {})", kind, target.x(), target.y());
        mouse.moveToAndClick(target.x(), target.y(), MouseController.ClickType.LEFT);
        StatsLog.event("VISIT");
        mouse.waitFor(PAGE_WAIT_SECONDS);

        // 3. 确认进入（检测回家按钮）
        if (imageMatcher.findButton(GameConfig.IMG_GO_HOME) == null) {
            log.warn("未检测到回家按钮，可能未进入好友农场（{}轮）", kind);
            StatsLog.event("VISIT_ABORT", steal ? "not_entered_steal" : "not_entered_farm");
            closeFriendList();
            goHomeAction.ensureHome();
            return false;
        }

        // 4. 页内动作
        if (steal) {
            doSteal();
        } else {
            doFarm();
        }

        // 5. 回家
        goHomeAction.ensureHome();
        return true;
    }

    /**
     * 好友农场内点一键偷菜（没有则跳过）
     */
    private void doSteal() {
        try {
            Point stealButton = imageMatcher.findButton(GameConfig.IMG_STEAL_ALL_BUTTON);
            if (stealButton != null) {
                log.info("检测到一键偷菜: ({}, {})", stealButton.x(), stealButton.y());
                mouse.moveToAndClick(stealButton.x(), stealButton.y(), MouseController.ClickType.LEFT);
                StatsLog.event("STEAL");
                mouse.waitFor(PAGE_WAIT_SECONDS);
            } else {
                log.info("好友无可偷作物");
            }
        } catch (Exception e) {
            log.warn("一键偷菜检测异常: {}", e.getMessage());
        }
    }

    /**
     * 好友农场内点一键务农（没有则跳过）
     */
    private void doFarm() {
        try {
            Point farmButton = imageMatcher.findButton(GameConfig.IMG_FRIEND_FARM_BUTTON);
            if (farmButton != null) {
                log.info("检测到好友田地一键务农: ({}, {})", farmButton.x(), farmButton.y());
                mouse.moveToAndClick(farmButton.x(), farmButton.y(), MouseController.ClickType.LEFT);
                StatsLog.event("FARM_CLICK", "source=friend");
                mouse.waitFor(PAGE_WAIT_SECONDS);
            } else {
                log.info("好友田地未检测到一键务农按钮（无需务农？）");
            }
        } catch (Exception e) {
            log.warn("一键务农检测异常: {}", e.getMessage());
        }
    }

    /**
     * 打开好友列表
     * @return true 成功点到主页好友按钮
     */
    private boolean openFriendList() {
        try {
            Point friendButton = imageMatcher.findButton(GameConfig.IMG_HOME_FRIEND);
            if (friendButton == null) {
                return false;
            }
            log.info("进入好友列表: ({}, {})", friendButton.x(), friendButton.y());
            mouse.moveToAndClick(friendButton.x(), friendButton.y(), MouseController.ClickType.LEFT);
            mouse.waitFor(PAGE_WAIT_SECONDS);
            return true;
        } catch (Exception e) {
            log.warn("打开好友列表异常: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 关闭好友列表弹窗（点右上角 X）
     * 弹窗不存在时静默跳过
     */
    private void closeFriendList() {
        try {
            Point closeBtn = imageMatcher.findButton(GameConfig.IMG_FRIEND_CLOSE);
            if (closeBtn != null) {
                log.info("关闭好友列表弹窗: ({}, {})", closeBtn.x(), closeBtn.y());
                mouse.moveToAndClick(closeBtn.x(), closeBtn.y(), MouseController.ClickType.LEFT);
                mouse.waitFor(1);
            }
        } catch (Exception e) {
            log.debug("关闭好友列表异常: {}", e.getMessage());
        }
    }

    /**
     * 为一个角标找最匹配的拜访按钮：
     * 同行（|ΔY| <= ROW_TOLERANCE）且在角标右侧（按钮 X >= 角标 X）中，水平距离最近的；
     * 若右侧没有同行按钮，则退化为同行中距离最近的。
     */
    private Point findNearestVisitButton(Point badge, List<Point> buttons) {
        Point bestRight = null;
        int bestRightDx = Integer.MAX_VALUE;
        Point bestAny = null;
        int bestAnyDist = Integer.MAX_VALUE;

        for (Point btn : buttons) {
            int dy = Math.abs(btn.y() - badge.y());
            if (dy > ROW_TOLERANCE) continue;

            if (btn.x() >= badge.x()) {
                int dx = btn.x() - badge.x();
                if (dx < bestRightDx) {
                    bestRightDx = dx;
                    bestRight = btn;
                }
            }

            int dist = dx2(badge, btn);
            if (dist < bestAnyDist) {
                bestAnyDist = dist;
                bestAny = btn;
            }
        }
        return bestRight != null ? bestRight : bestAny;
    }

    private int dx2(Point a, Point b) {
        int dx = a.x() - b.x();
        int dy = a.y() - b.y();
        return dx * dx + dy * dy;
    }

    /**
     * 邻近点聚类：把距离 CLUSTER_RADIUS 以内的匹配点合并为一个目标（取均值位置）
     */
    private List<Point> cluster(List<Point> points) {
        List<Point> merged = new ArrayList<>();
        boolean[] used = new boolean[points.size()];

        for (int i = 0; i < points.size(); i++) {
            if (used[i]) continue;
            used[i] = true;

            List<Point> group = new ArrayList<>();
            group.add(points.get(i));

            for (int j = i + 1; j < points.size(); j++) {
                if (used[j]) continue;
                Point a = points.get(i);
                Point b = points.get(j);
                if (Math.abs(a.x() - b.x()) <= CLUSTER_RADIUS && Math.abs(a.y() - b.y()) <= CLUSTER_RADIUS) {
                    used[j] = true;
                    group.add(b);
                }
            }

            long sumX = 0, sumY = 0;
            for (Point p : group) {
                sumX += p.x();
                sumY += p.y();
            }
            merged.add(new Point((int) (sumX / group.size()), (int) (sumY / group.size())));
        }
        return merged;
    }
}
