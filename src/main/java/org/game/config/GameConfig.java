package org.game.config;

import org.config.CoordinateConfig;

/**
 * 游戏配置常量
 * 所有屏幕坐标/区域/路线均从 QQFarmCoordinates.properties 读取（CoordinateConfig），
 * 修改坐标只需改配置文件，本类不写死任何像素值。
 */
public class GameConfig {

    // ========== 种子栏配置（properties: qqFarm.seedBar.*）==========
    // 种子栏搜索范围（左上角 + 宽高）
    public static final int SEED_BAR_LEFT = CoordinateConfig.getInt("qqFarm.seedBar.left");
    public static final int SEED_BAR_TOP = CoordinateConfig.getInt("qqFarm.seedBar.top");
    public static final int SEED_BAR_WIDTH = CoordinateConfig.getInt("qqFarm.seedBar.width");
    public static final int SEED_BAR_HEIGHT = CoordinateConfig.getInt("qqFarm.seedBar.height");

    // 角标到种子中心的偏移（角标在种子左上角，往右下偏移；properties: qqFarm.badge.offset.*）
    public static final int BADGE_TO_SEED_OFFSET_X = CoordinateConfig.getInt("qqFarm.badge.offset.x");
    public static final int BADGE_TO_SEED_OFFSET_Y = CoordinateConfig.getInt("qqFarm.badge.offset.y");

    // ========== 图片模板路径 ==========
    public static final String IMG_HARVEST_BUTTON = "images/一键收获.png";
    public static final String IMG_SEED_BADGE = "images/种子角标.png";
    public static final String IMG_SHOP_LOCK = "images/商店锁.png";
    public static final String IMG_BUY_CONFIRM = "images/购买种子确认.png";
    public static final String IMG_SHOP_CLOSE = "images/商店关闭.png";
    public static final String IMG_RELOGIN_BUTTON = "images/重新登录.png";
    public static final String IMG_SHOVEL = "images/小铲子.png";
    public static final String IMG_WELCOME_CLOSE = "images/欢迎弹窗关闭.png";
    public static final String IMG_FARM_BUTTON = "images/一键务农.png";
    public static final String IMG_GO_HOME = "images/求助页回家.png";
    public static final String IMG_HOME_FRIEND = "images/主页好友.png";
    public static final String IMG_STEAL_BADGE = "images/好友/偷菜角标.png";
    public static final String IMG_FARM_BADGE = "images/好友/务农角标.png";
    public static final String IMG_VISIT_BUTTON = "images/好友/拜访按钮.png";
    public static final String IMG_STEAL_ALL_BUTTON = "images/好友田地/一键偷菜按钮.png";
    public static final String IMG_FRIEND_FARM_BUTTON = "images/好友田地/一键务农按钮.png";
    public static final String IMG_FRIEND_CLOSE = "images/好友/关闭按钮.png";

    // ========== 时间配置（毫秒）==========
    // 收获后等待时间（可配置）
    public static final long HARVEST_WAIT_TIME = 5 * 60 * 1000;  // 5分钟
    // 等待作物成熟期间，进入好友农场偷菜的间隔
    public static final long FRIEND_VISIT_INTERVAL_MS = 30 * 1000;  // 30秒

    // ========== 商店配置（properties: qqFarm.seedGrid.*）==========
    // 种子网格坐标（商店界面），四角换算出的扫描边界
    public static final int GRID_LEFT = CoordinateConfig.getInt("qqFarm.seedGrid.leftTop.x");
    public static final int GRID_TOP = CoordinateConfig.getInt("qqFarm.seedGrid.leftTop.y");
    public static final int GRID_RIGHT = CoordinateConfig.getInt("qqFarm.seedGrid.rightTop.x");
    public static final int GRID_BOTTOM = CoordinateConfig.getInt("qqFarm.seedGrid.leftBottom.y");
    public static final int GRID_COLS = CoordinateConfig.getInt("qqFarm.seedGrid.cols");
    public static final int GRID_ROWS = CoordinateConfig.getInt("qqFarm.seedGrid.rows");
}
