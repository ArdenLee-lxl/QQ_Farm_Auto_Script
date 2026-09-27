package org.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 关键事件统计日志（独立于 logback 运行日志）
 * 每个关键动作追加一行到 logs/stats.log，格式：
 *   2026-07-05 14:30:01 | SOW
 *   2026-07-05 14:30:01 | FARM_CLICK | source=help
 * 用于按时间段统计收获/播种/偷菜等次数。
 * 写入失败静默忽略，绝不影响主流程。
 */
public final class StatsLog {

    private static final Path LOG_FILE = Path.of("logs", "stats.log");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private StatsLog() {
    }

    public static void event(String type) {
        event(type, null);
    }

    public static synchronized void event(String type, String detail) {
        try {
            Path parent = LOG_FILE.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            StringBuilder line = new StringBuilder()
                    .append(LocalDateTime.now().format(TIME_FMT))
                    .append(" | ").append(type);
            if (detail != null && !detail.isEmpty()) {
                line.append(" | ").append(detail);
            }
            line.append(System.lineSeparator());
            Files.writeString(LOG_FILE, line.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // 统计日志写入失败不影响主流程
        }
    }
}
