package org.test;

/**
 * 田地扫描 demo —— 只扫描不点击的入口
 * IDEA 里直接右键 Run 即可，用于先验证24格识别效果（判定表 + 标注截图）
 */
public class FieldPlantScanDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("=== 田地扫描 demo（只扫描，不点击任何位置）===");
        System.out.println("等待 5 秒，请确保投屏画面已显示、停留在自己农场...");

        Thread.sleep(5000);
        new FieldPlantDemo().runScanOnly();
    }
}
