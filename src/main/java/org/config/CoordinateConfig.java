package org.config;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 坐标配置读取类
 */
@Slf4j
public class CoordinateConfig {

    private static final Properties props = new Properties();

    // 静态代码块，类加载时自动读取配置文件
    static {
        try (InputStream input = CoordinateConfig.class
                .getClassLoader()
                .getResourceAsStream("QQFarmCoordinates.properties")) {

            if (input == null) {
                log.error("找不到配置文件 QQFarmCoordinates.properties");
            }

            // 加载配置文件
            props.load(input);
            log.info("坐标配置文件加载成功");

        } catch (IOException e) {
            log.error("读取配置文件失败", e);
        }
    }

    /**
     * 获取整数坐标
     */
    public static int getInt(String key) {
        String value = props.getProperty(key);
        if (value == null) {
            log.error("配置项 {} 不存在", key);
            return 0;
        }
        return Integer.parseInt(value);
    }

    /**
     * 获取字符串配置
     */
    public static String getString(String key) {
        return props.getProperty(key);
    }

    /**
     * 获取坐标点（返回数组）
     */
    public static int[] getPoint(String prefix) {
        int x = getInt(prefix + ".x");
        int y = getInt(prefix + ".y");
        return new int[]{x, y};
    }

    /**
     * 获取投屏区域的边界
     * 返回 {leftX, topY, rightX, bottomY, width, height}
     */
    public static int[] getScreenBounds() {
        int[] leftTop = getPoint("qqFarm.screen.leftTop");
        int[] rightTop = getPoint("qqFarm.screen.rightTop");
        int[] leftBottom = getPoint("qqFarm.screen.leftBottom");
        int[] rightBottom = getPoint("qqFarm.screen.rightBottom");

        int leftX = Math.min(leftTop[0], leftBottom[0]);
        int topY = Math.min(leftTop[1], rightTop[1]);
        int rightX = Math.max(rightTop[0], rightBottom[0]);
        int bottomY = Math.max(leftBottom[1], rightBottom[1]);

        int width = rightX - leftX;
        int height = bottomY - topY;

        return new int[]{leftX, topY, rightX, bottomY, width, height};
    }

    /**
     * 将相对坐标（百分比）转换为绝对坐标
     * @param relativeX 相对X坐标（0-1）
     * @param relativeY 相对Y坐标（0-1）
     * @return 绝对坐标 {x, y}
     */
    public static int[] toAbsolutePosition(double relativeX, double relativeY) {
        int[] bounds = getScreenBounds();
        int leftX = bounds[0];
        int topY = bounds[1];
        int width = bounds[4];
        int height = bounds[5];

        int absX = (int)(leftX + width * relativeX);
        int absY = (int)(topY + height * relativeY);

        return new int[]{absX, absY};
    }

    /**
     * 获取种子网格区域的边界
     * 返回 {leftX, topY, rightX, bottomY, width, height}
     */
    public static int[] getSeedGridBounds() {
        int[] leftTop = getPoint("qqFarm.seedGrid.leftTop");
        int[] rightTop = getPoint("qqFarm.seedGrid.rightTop");
        int[] leftBottom = getPoint("qqFarm.seedGrid.leftBottom");
        int[] rightBottom = getPoint("qqFarm.seedGrid.rightBottom");

        int leftX = Math.min(leftTop[0], leftBottom[0]);
        int topY = Math.min(leftTop[1], rightTop[1]);
        int rightX = Math.max(rightTop[0], rightBottom[0]);
        int bottomY = Math.max(leftBottom[1], rightBottom[1]);

        int width = rightX - leftX;
        int height = bottomY - topY;

        return new int[]{leftX, topY, rightX, bottomY, width, height};
    }
}