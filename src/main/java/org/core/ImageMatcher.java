package org.core;

import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
public class ImageMatcher {

    private final Robot robot;
    private final double threshold;
    private static final double[] SCALE_RANGE = {0.5, 0.6, 0.7, 0.8, 0.9, 1.0, 1.1, 1.2, 1.3, 1.5};

    public ImageMatcher() throws AWTException {
        this.robot = new Robot();
        this.threshold = 0.8;
    }

    public Point findButton(String imagePath) throws IOException {
        // 截取屏幕
        BufferedImage screenImage = robot.createScreenCapture(
                new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

        // 加载模板图片
        InputStream imgStream = getClass().getClassLoader().getResourceAsStream(imagePath);
        if (imgStream == null) {
            log.error("未找到图片 {}", imagePath);
            return null;
        }
        BufferedImage templateBuffered = ImageIO.read(imgStream);

        // 转 OpenCV Mat
        Mat screen = bufferedImageToMat(screenImage);
        Mat template = bufferedImageToMat(templateBuffered);

        int screenW = screen.cols();
        int screenH = screen.rows();
        int tplW = templateBuffered.getWidth();
        int tplH = templateBuffered.getHeight();

        // 转灰度，匹配更稳定
        Mat screenGray = new Mat();
        opencv_imgproc.cvtColor(screen, screenGray, opencv_imgproc.COLOR_BGR2GRAY);

        double bestScore = -1;
        int bestX = 0, bestY = 0;

        for (double scale : SCALE_RANGE) {
            int w = (int) (tplW * scale);
            int h = (int) (tplH * scale);
            if (w < 10 || h < 10 || w > screenW || h > screenH) continue;

            // 缩放模板
            Mat templateGray = new Mat();
            opencv_imgproc.cvtColor(template, templateGray, opencv_imgproc.COLOR_BGR2GRAY);
            Mat scaled = new Mat();
            opencv_imgproc.resize(templateGray, scaled, new org.bytedeco.opencv.opencv_core.Size(w, h));

            // 模板匹配
            Mat result = new Mat();
            opencv_imgproc.matchTemplate(screenGray, scaled, result, opencv_imgproc.TM_CCOEFF_NORMED);

            DoublePointer maxVal = new DoublePointer(1);
            Point maxLoc = new Point();
            opencv_core.minMaxLoc(result, null, maxVal, null, maxLoc, null);

            double score = maxVal.get();
            log.debug("缩放 {}x → 匹配度 {}%",
                    String.format("%.1f", scale),
                    String.format("%.2f", score * 100));

            if (score > bestScore) {
                bestScore = score;
                bestX = maxLoc.x();
                bestY = maxLoc.y();
            }

            scaled.release();
            templateGray.release();
            result.release();
        }

        screenGray.release();
        screen.release();
        template.release();

        log.info("图片匹配度：{}%", String.format("%.2f", bestScore * 100));

        if (bestScore >= threshold) {
            int centerX = bestX + tplW / 2;
            int centerY = bestY + tplH / 2;
            log.info("匹配成功，位置：({}, {})", centerX, centerY);
            return new Point(centerX, centerY);
        } else {
            log.warn("匹配度不足：{}% < {}%",
                    String.format("%.2f", bestScore * 100),
                    (int)(threshold * 100));
            return null;
        }
    }

    /**
     * 查找所有匹配的位置
     * @param imagePath 模板图片路径
     * @return 所有匹配位置的列表
     */
    public java.util.List<Point> findAllButtons(String imagePath) throws IOException {
        java.util.List<Point> results = new java.util.ArrayList<>();

        // 截取屏幕
        BufferedImage screenImage = robot.createScreenCapture(
                new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

        // 加载模板图片
        InputStream imgStream = getClass().getClassLoader().getResourceAsStream(imagePath);
        if (imgStream == null) {
            log.error("未找到图片 {}", imagePath);
            return results;
        }
        BufferedImage templateBuffered = ImageIO.read(imgStream);

        // 转 OpenCV Mat
        Mat screen = bufferedImageToMat(screenImage);
        Mat template = bufferedImageToMat(templateBuffered);

        int screenW = screen.cols();
        int screenH = screen.rows();
        int tplW = templateBuffered.getWidth();
        int tplH = templateBuffered.getHeight();

        // 转灰度，匹配更稳定
        Mat screenGray = new Mat();
        opencv_imgproc.cvtColor(screen, screenGray, opencv_imgproc.COLOR_BGR2GRAY);

        // 使用最佳缩放比例进行匹配
        double bestScale = 1.0;
        double bestScore = -1;

        for (double scale : SCALE_RANGE) {
            int w = (int) (tplW * scale);
            int h = (int) (tplH * scale);
            if (w < 10 || h < 10 || w > screenW || h > screenH) continue;

            Mat templateGray = new Mat();
            opencv_imgproc.cvtColor(template, templateGray, opencv_imgproc.COLOR_BGR2GRAY);
            Mat scaled = new Mat();
            opencv_imgproc.resize(templateGray, scaled, new org.bytedeco.opencv.opencv_core.Size(w, h));

            Mat result = new Mat();
            opencv_imgproc.matchTemplate(screenGray, scaled, result, opencv_imgproc.TM_CCOEFF_NORMED);

            DoublePointer maxVal = new DoublePointer(1);
            Point maxLoc = new Point();
            opencv_core.minMaxLoc(result, null, maxVal, null, maxLoc, null);

            double score = maxVal.get();
            if (score > bestScore) {
                bestScore = score;
                bestScale = scale;
            }

            scaled.release();
            templateGray.release();
            result.release();
        }

        // 使用最佳缩放比例，找出所有超过阈值的位置
        int w = (int) (tplW * bestScale);
        int h = (int) (tplH * bestScale);

        Mat templateGray = new Mat();
        opencv_imgproc.cvtColor(template, templateGray, opencv_imgproc.COLOR_BGR2GRAY);
        Mat scaled = new Mat();
        opencv_imgproc.resize(templateGray, scaled, new org.bytedeco.opencv.opencv_core.Size(w, h));

        Mat result = new Mat();
        opencv_imgproc.matchTemplate(screenGray, scaled, result, opencv_imgproc.TM_CCOEFF_NORMED);

        // 遍历结果，找出所有超过阈值的位置
        int resultW = result.cols();
        int resultH = result.rows();

        // 使用 FloatPointer 读取结果矩阵的数据
        org.bytedeco.javacpp.FloatPointer fp = new org.bytedeco.javacpp.FloatPointer(result.data());

        for (int y = 0; y < resultH; y++) {
            for (int x = 0; x < resultW; x++) {
                int idx = y * resultW + x;
                float score = fp.get(idx);

                if (score >= threshold) {
                    int centerX = x + w / 2;
                    int centerY = y + h / 2;
                    results.add(new Point(centerX, centerY));
                }
            }
        }

        fp.close();

        // 释放资源
        screenGray.release();
        screen.release();
        template.release();
        templateGray.release();
        scaled.release();
        result.release();

        log.info("找到 {} 个匹配位置", results.size());
        return results;
    }

    private Mat bufferedImageToMat(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        int[] pixels = new int[width * height];
        img.getRGB(0, 0, width, height, pixels, 0, width);

        Mat mat = new Mat(height, width, opencv_core.CV_8UC3);
        byte[] data = new byte[width * height * 3];

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            data[i * 3] = (byte) ((pixel >> 0) & 0xFF);   // B
            data[i * 3 + 1] = (byte) ((pixel >> 8) & 0xFF);  // G
            data[i * 3 + 2] = (byte) ((pixel >> 16) & 0xFF); // R
        }

        mat.data().put(data);
        return mat;
    }

    /**
     * 在指定矩形区域内查找所有匹配的位置
     * @param imagePath 模板图片路径
     * @param regionX 区域左上角X坐标
     * @param regionY 区域左上角Y坐标
     * @param regionW 区域宽度
     * @param regionH 区域高度
     * @return 区域内所有匹配位置的列表
     */
    public java.util.List<Point> findButtonsInRegion(String imagePath, int regionX, int regionY, int regionW, int regionH) throws IOException {
        java.util.List<Point> results = new java.util.ArrayList<>();

        // 截取指定区域的屏幕
        BufferedImage screenImage = robot.createScreenCapture(
                new Rectangle(regionX, regionY, regionW, regionH));

        // 加载模板图片
        InputStream imgStream = getClass().getClassLoader().getResourceAsStream(imagePath);
        if (imgStream == null) {
            log.error("未找到图片 {}", imagePath);
            return results;
        }
        BufferedImage templateBuffered = ImageIO.read(imgStream);

        // 转 OpenCV Mat
        Mat screen = bufferedImageToMat(screenImage);
        Mat template = bufferedImageToMat(templateBuffered);

        int screenW = screen.cols();
        int screenH = screen.rows();
        int tplW = templateBuffered.getWidth();
        int tplH = templateBuffered.getHeight();

        // 转灰度，匹配更稳定
        Mat screenGray = new Mat();
        opencv_imgproc.cvtColor(screen, screenGray, opencv_imgproc.COLOR_BGR2GRAY);

        // 使用最佳缩放比例进行匹配
        double bestScale = 1.0;
        double bestScore = -1;

        for (double scale : SCALE_RANGE) {
            int w = (int) (tplW * scale);
            int h = (int) (tplH * scale);
            if (w < 10 || h < 10 || w > screenW || h > screenH) continue;

            Mat templateGray = new Mat();
            opencv_imgproc.cvtColor(template, templateGray, opencv_imgproc.COLOR_BGR2GRAY);
            Mat scaled = new Mat();
            opencv_imgproc.resize(templateGray, scaled, new org.bytedeco.opencv.opencv_core.Size(w, h));

            Mat result = new Mat();
            opencv_imgproc.matchTemplate(screenGray, scaled, result, opencv_imgproc.TM_CCOEFF_NORMED);

            DoublePointer maxVal = new DoublePointer(1);
            Point maxLoc = new Point();
            opencv_core.minMaxLoc(result, null, maxVal, null, maxLoc, null);

            double score = maxVal.get();
            if (score > bestScore) {
                bestScore = score;
                bestScale = scale;
            }

            scaled.release();
            templateGray.release();
            result.release();
        }

        // 使用最佳缩放比例，找出所有超过阈值的位置
        int w = (int) (tplW * bestScale);
        int h = (int) (tplH * bestScale);

        Mat templateGray = new Mat();
        opencv_imgproc.cvtColor(template, templateGray, opencv_imgproc.COLOR_BGR2GRAY);
        Mat scaled = new Mat();
        opencv_imgproc.resize(templateGray, scaled, new org.bytedeco.opencv.opencv_core.Size(w, h));

        Mat result = new Mat();
        opencv_imgproc.matchTemplate(screenGray, scaled, result, opencv_imgproc.TM_CCOEFF_NORMED);

        // 遍历结果，找出所有超过阈值的位置
        int resultW = result.cols();
        int resultH = result.rows();

        org.bytedeco.javacpp.FloatPointer fp = new org.bytedeco.javacpp.FloatPointer(result.data());

        for (int y = 0; y < resultH; y++) {
            for (int x = 0; x < resultW; x++) {
                int idx = y * resultW + x;
                float score = fp.get(idx);

                if (score >= threshold) {
                    // 转换为屏幕绝对坐标
                    int centerX = regionX + x + w / 2;
                    int centerY = regionY + y + h / 2;
                    results.add(new Point(centerX, centerY));
                }
            }
        }

        fp.close();

        // 释放资源
        screenGray.release();
        screen.release();
        template.release();
        templateGray.release();
        scaled.release();
        result.release();

        log.info("在区域内找到 {} 个匹配位置", results.size());
        return results;
    }

    /**
     * 在指定矩形区域内查找所有匹配的位置（支持自定义阈值）
     * @param imagePath 模板图片路径
     * @param regionX 区域左上角X坐标
     * @param regionY 区域左上角Y坐标
     * @param regionW 区域宽度
     * @param regionH 区域高度
     * @param customThreshold 自定义匹配阈值（0.0-1.0），值越低越宽松
     * @return 区域内所有匹配位置的列表
     */
    public java.util.List<Point> findButtonsInRegion(String imagePath, int regionX, int regionY, int regionW, int regionH, double customThreshold) throws IOException {
        java.util.List<Point> results = new java.util.ArrayList<>();

        // 截取指定区域的屏幕
        BufferedImage screenImage = robot.createScreenCapture(
                new Rectangle(regionX, regionY, regionW, regionH));

        // 加载模板图片
        InputStream imgStream = getClass().getClassLoader().getResourceAsStream(imagePath);
        if (imgStream == null) {
            log.error("未找到图片 {}", imagePath);
            return results;
        }
        BufferedImage templateBuffered = ImageIO.read(imgStream);

        // 转 OpenCV Mat
        Mat screen = bufferedImageToMat(screenImage);
        Mat template = bufferedImageToMat(templateBuffered);

        int screenW = screen.cols();
        int screenH = screen.rows();
        int tplW = templateBuffered.getWidth();
        int tplH = templateBuffered.getHeight();

        // 转灰度，匹配更稳定
        Mat screenGray = new Mat();
        opencv_imgproc.cvtColor(screen, screenGray, opencv_imgproc.COLOR_BGR2GRAY);

        // 使用最佳缩放比例进行匹配
        double bestScale = 1.0;
        double bestScore = -1;

        for (double scale : SCALE_RANGE) {
            int w = (int) (tplW * scale);
            int h = (int) (tplH * scale);
            if (w < 10 || h < 10 || w > screenW || h > screenH) continue;

            Mat templateGray = new Mat();
            opencv_imgproc.cvtColor(template, templateGray, opencv_imgproc.COLOR_BGR2GRAY);
            Mat scaled = new Mat();
            opencv_imgproc.resize(templateGray, scaled, new org.bytedeco.opencv.opencv_core.Size(w, h));

            Mat result = new Mat();
            opencv_imgproc.matchTemplate(screenGray, scaled, result, opencv_imgproc.TM_CCOEFF_NORMED);

            DoublePointer maxVal = new DoublePointer(1);
            Point maxLoc = new Point();
            opencv_core.minMaxLoc(result, null, maxVal, null, maxLoc, null);

            double score = maxVal.get();
            if (score > bestScore) {
                bestScore = score;
                bestScale = scale;
            }

            scaled.release();
            templateGray.release();
            result.release();
        }

        // 使用最佳缩放比例，找出所有超过阈值的位置
        int w = (int) (tplW * bestScale);
        int h = (int) (tplH * bestScale);

        Mat templateGray = new Mat();
        opencv_imgproc.cvtColor(template, templateGray, opencv_imgproc.COLOR_BGR2GRAY);
        Mat scaled = new Mat();
        opencv_imgproc.resize(templateGray, scaled, new org.bytedeco.opencv.opencv_core.Size(w, h));

        Mat result = new Mat();
        opencv_imgproc.matchTemplate(screenGray, scaled, result, opencv_imgproc.TM_CCOEFF_NORMED);

        // 遍历结果，找出所有超过阈值的位置
        int resultW = result.cols();
        int resultH = result.rows();

        org.bytedeco.javacpp.FloatPointer fp = new org.bytedeco.javacpp.FloatPointer(result.data());

        for (int y = 0; y < resultH; y++) {
            for (int x = 0; x < resultW; x++) {
                int idx = y * resultW + x;
                float score = fp.get(idx);

                if (score >= customThreshold) {
                    // 转换为屏幕绝对坐标
                    int centerX = regionX + x + w / 2;
                    int centerY = regionY + y + h / 2;
                    results.add(new Point(centerX, centerY));
                }
            }
        }

        fp.close();

        // 释放资源
        screenGray.release();
        screen.release();
        template.release();
        templateGray.release();
        scaled.release();
        result.release();

        log.info("在区域内找到 {} 个匹配位置（阈值: {}）", results.size(), customThreshold);
        return results;
    }
}
