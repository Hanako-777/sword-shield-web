package com.swordshield.admin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统日志与审计查询服务。
 *
 * <p>聚合三类可观测与审计数据供管理后台实时检索：
 * <ol>
 *   <li>9 个微服务落盘的 Logback 运行日志与错误日志（含全链路 MDC trace_id 解析与跨服务追踪）；</li>
 *   <li>MySQL {@code ai_chat_log} 表的 AI 智能问答审计记录；</li>
 *   <li>MySQL {@code payment_callback_log} 关联 {@code payment_order} 的支付回调流水日志。</li>
 * </ol>
 */
@Service
public class AdminLogService {

    private static final int MAX_TAIL_BYTES = 512 * 1024;
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 匹配 logback-spring.xml 标准日志行，兼容 trace_id 字段尾部的冗余花括号。
     */
    private static final Pattern LOG_LINE_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+\\[([^\\]]*)\\]\\s+" +
            "(TRACE|DEBUG|INFO|WARN|ERROR)\\s+(\\S+)\\s+\\[trace_id:([^\\]}]*)\\}?\\](?:\\s*-\\s*(.*))?$"
    );

    private static final LinkedHashMap<String, String> SERVICE_CATALOG = new LinkedHashMap<>();

    static {
        SERVICE_CATALOG.put("sword-shield-gateway", "网关服务");
        SERVICE_CATALOG.put("sword-shield-user", "用户服务");
        SERVICE_CATALOG.put("sword-shield-activity", "活动服务");
        SERVICE_CATALOG.put("sword-shield-content", "内容服务");
        SERVICE_CATALOG.put("sword-shield-interaction", "互动服务");
        SERVICE_CATALOG.put("sword-shield-file", "文件服务");
        SERVICE_CATALOG.put("sword-shield-admin", "管理后台");
        SERVICE_CATALOG.put("sword-shield-ai", "AI 助手");
        SERVICE_CATALOG.put("sword-shield-payment", "在线支付");
    }

    private final JdbcTemplate jdbcTemplate;
    private final String configuredLogDir;

    public AdminLogService(JdbcTemplate jdbcTemplate,
                           @Value("${app.log.dir:./logs}") String configuredLogDir) {
        this.jdbcTemplate = jdbcTemplate;
        this.configuredLogDir = configuredLogDir;
    }

    public Map<String, Object> getSummary() {
        long aiChatTotal = safeQueryLong("SELECT COUNT(*) FROM ai_chat_log");
        double aiAvgLatencyMs = safeQueryDouble("SELECT COALESCE(ROUND(AVG(latency_ms), 1), 0) FROM ai_chat_log");
        long paymentCallbackTotal = safeQueryLong("SELECT COUNT(*) FROM payment_callback_log");
        long paymentCallbackSuccess = safeQueryLong("SELECT COUNT(*) FROM payment_callback_log WHERE success = 1");
        long paymentOrderTotal = safeQueryLong("SELECT COUNT(*) FROM payment_order");
        long paymentOrderPaid = safeQueryLong("SELECT COUNT(*) FROM payment_order WHERE status = 'PAID'");

        Path logDir = resolveLogDir();
        int activeLogServices = 0;
        long errorLogCount = 0;
        List<Map<String, Object>> services = new ArrayList<>();

        for (Map.Entry<String, String> entry : SERVICE_CATALOG.entrySet()) {
            String svcName = entry.getKey();
            String label = entry.getValue();
            Path mainFile = logDir.resolve(svcName + ".log");
            Path errFile = logDir.resolve(svcName + "-error.log");

            boolean exists = Files.isRegularFile(mainFile);
            long sizeBytes = exists ? safeFileSize(mainFile) : 0L;
            long errSizeBytes = Files.isRegularFile(errFile) ? safeFileSize(errFile) : 0L;
            if (exists && sizeBytes > 0) {
                activeLogServices++;
            }
            long svcErrorLines = errSizeBytes > 0 ? countLogEntries(errFile) : 0L;
            errorLogCount += svcErrorLines;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", svcName);
            item.put("label", label);
            item.put("fileExists", exists);
            item.put("fileSizeKb", Math.round(sizeBytes / 102.4) / 10.0);
            item.put("errorFileSizeKb", Math.round(errSizeBytes / 102.4) / 10.0);
            item.put("errorCount", svcErrorLines);
            item.put("lastModified", exists ? safeLastModified(mainFile) : null);
            services.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("aiChatTotal", aiChatTotal);
        result.put("aiAvgLatencyMs", aiAvgLatencyMs);
        result.put("paymentCallbackTotal", paymentCallbackTotal);
        result.put("paymentCallbackSuccess", paymentCallbackSuccess);
        result.put("paymentOrderTotal", paymentOrderTotal);
        result.put("paymentOrderPaid", paymentOrderPaid);
        result.put("activeLogServices", activeLogServices);
        result.put("totalServices", SERVICE_CATALOG.size());
        result.put("errorLogCount", errorLogCount);
        result.put("logDir", logDir.toAbsolutePath().normalize().toString());
        result.put("services", services);
        return result;
    }

    public Map<String, Object> queryRuntimeLogs(String service,
                                                String level,
                                                String keyword,
                                                String traceId,
                                                boolean errorOnly,
                                                int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        String normalizedService = service == null ? "" : service.trim();
        String normalizedLevel = level == null ? "" : level.trim().toUpperCase(Locale.ROOT);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedTraceId = traceId == null ? "" : traceId.trim().toLowerCase(Locale.ROOT);

        List<String> targetServices = new ArrayList<>();
        if (normalizedService.isEmpty() || "ALL".equalsIgnoreCase(normalizedService)) {
            targetServices.addAll(SERVICE_CATALOG.keySet());
        } else {
            if (!SERVICE_CATALOG.containsKey(normalizedService)) {
                throw new IllegalArgumentException("未知的服务日志名称: " + normalizedService);
            }
            targetServices.add(normalizedService);
        }

        Path logDir = resolveLogDir();
        List<Map<String, Object>> matched = new ArrayList<>();

        for (String svc : targetServices) {
            Path file = logDir.resolve(errorOnly ? (svc + "-error.log") : (svc + ".log"));
            if (!Files.isRegularFile(file)) {
                continue;
            }
            List<Map<String, Object>> entries = parseLogFileTail(file, svc, SERVICE_CATALOG.get(svc));
            for (Map<String, Object> entry : entries) {
                if (!normalizedLevel.isEmpty() && !"ALL".equals(normalizedLevel)) {
                    String entryLevel = String.valueOf(entry.get("level"));
                    if (!normalizedLevel.equalsIgnoreCase(entryLevel)) {
                        continue;
                    }
                }
                if (!normalizedTraceId.isEmpty()) {
                    String entryTrace = String.valueOf(entry.get("traceId")).toLowerCase(Locale.ROOT);
                    if (!entryTrace.contains(normalizedTraceId)) {
                        continue;
                    }
                }
                if (!normalizedKeyword.isEmpty()) {
                    String haystack = (
                            entry.get("service") + " " +
                            entry.get("serviceLabel") + " " +
                            entry.get("logger") + " " +
                            entry.get("traceId") + " " +
                            entry.get("message")
                    ).toLowerCase(Locale.ROOT);
                    if (!haystack.contains(normalizedKeyword)) {
                        continue;
                    }
                }
                matched.add(entry);
            }
        }

        matched.sort((a, b) -> String.valueOf(b.get("timestamp")).compareTo(String.valueOf(a.get("timestamp"))));

        int totalMatched = matched.size();
        List<Map<String, Object>> items = totalMatched > safeLimit
                ? new ArrayList<>(matched.subList(0, safeLimit))
                : matched;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("totalMatched", totalMatched);
        result.put("limit", safeLimit);
        result.put("logDir", logDir.toAbsolutePath().normalize().toString());
        return result;
    }

    public Map<String, Object> queryAiChatLogs(String keyword, String model, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = safePage * safeSize;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String like = "%" + keyword.trim() + "%";
            where.append(" AND (a.question LIKE ? OR a.answer LIKE ? OR a.session_id LIKE ? OR u.username LIKE ?) ");
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (model != null && !model.trim().isEmpty() && !"ALL".equalsIgnoreCase(model.trim())) {
            where.append(" AND a.model = ? ");
            params.add(model.trim());
        }

        String countSql = "SELECT COUNT(*) FROM ai_chat_log a LEFT JOIN `user` u ON a.user_id = u.id " + where;
        Long totalObj = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        long total = totalObj == null ? 0L : totalObj;

        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(safeSize);
        dataParams.add(offset);

        String dataSql = "SELECT a.id, a.user_id, COALESCE(u.username, '游客') AS username, " +
                "a.session_id, a.question, a.answer, a.model, a.latency_ms, a.created_at " +
                "FROM ai_chat_log a LEFT JOIN `user` u ON a.user_id = u.id " +
                where + " ORDER BY a.created_at DESC, a.id DESC LIMIT ? OFFSET ?";

        List<Map<String, Object>> items = jdbcTemplate.query(dataSql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            long uid = rs.getLong("user_id");
            row.put("userId", rs.wasNull() ? null : uid);
            row.put("username", rs.getString("username"));
            row.put("sessionId", rs.getString("session_id"));
            row.put("question", rs.getString("question"));
            row.put("answer", rs.getString("answer"));
            row.put("model", rs.getString("model"));
            long latency = rs.getLong("latency_ms");
            row.put("latencyMs", rs.wasNull() ? 0L : latency);
            row.put("createdAt", formatTimestamp(rs.getTimestamp("created_at")));
            return row;
        }, dataParams.toArray());

        List<String> models = safeQueryStringList("SELECT DISTINCT model FROM ai_chat_log WHERE model IS NOT NULL ORDER BY model");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("models", models);
        return result;
    }

    public Map<String, Object> queryPaymentCallbackLogs(String orderNo, String channel, Integer success, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        int offset = safePage * safeSize;

        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (orderNo != null && !orderNo.trim().isEmpty()) {
            String like = "%" + orderNo.trim() + "%";
            where.append(" AND (c.order_no LIKE ? OR c.trade_no LIKE ? OR o.product_name LIKE ?) ");
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (channel != null && !channel.trim().isEmpty() && !"ALL".equalsIgnoreCase(channel.trim())) {
            where.append(" AND c.channel = ? ");
            params.add(channel.trim());
        }
        if (success != null && (success == 0 || success == 1)) {
            where.append(" AND c.success = ? ");
            params.add(success);
        }

        String countSql = "SELECT COUNT(*) FROM payment_callback_log c " +
                "LEFT JOIN payment_order o ON c.order_no = o.order_no " + where;
        Long totalObj = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        long total = totalObj == null ? 0L : totalObj;

        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(safeSize);
        dataParams.add(offset);

        String dataSql = "SELECT c.id, c.order_no, c.channel, c.trade_no, c.payload, c.success, c.created_at, " +
                "o.product_name, o.amount, o.status AS order_status, o.user_id, " +
                "COALESCE(u.username, CASE WHEN o.user_id IS NOT NULL THEN CONCAT('用户#', o.user_id) ELSE '-' END) AS username " +
                "FROM payment_callback_log c " +
                "LEFT JOIN payment_order o ON c.order_no = o.order_no " +
                "LEFT JOIN `user` u ON o.user_id = u.id " +
                where + " ORDER BY c.created_at DESC, c.id DESC LIMIT ? OFFSET ?";

        List<Map<String, Object>> items = jdbcTemplate.query(dataSql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("orderNo", rs.getString("order_no"));
            row.put("channel", rs.getString("channel"));
            row.put("tradeNo", rs.getString("trade_no"));
            row.put("payload", rs.getString("payload"));
            row.put("success", rs.getBoolean("success"));
            row.put("createdAt", formatTimestamp(rs.getTimestamp("created_at")));
            row.put("productName", rs.getString("product_name"));
            row.put("amount", rs.getBigDecimal("amount"));
            row.put("orderStatus", rs.getString("order_status"));
            long uid = rs.getLong("user_id");
            row.put("userId", rs.wasNull() ? null : uid);
            row.put("username", rs.getString("username"));
            return row;
        }, dataParams.toArray());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total);
        result.put("page", safePage);
        result.put("size", safeSize);
        return result;
    }

    private Path resolveLogDir() {
        Path primary = Paths.get(configuredLogDir);
        if (Files.isDirectory(primary)) {
            return primary;
        }
        Path parentFallback = Paths.get("..", "logs");
        if (Files.isDirectory(parentFallback)) {
            return parentFallback;
        }
        return primary;
    }

    private List<Map<String, Object>> parseLogFileTail(Path file, String serviceName, String serviceLabel) {
        String content = readTailUtf8(file, MAX_TAIL_BYTES);
        if (content.isEmpty()) {
            return Collections.emptyList();
        }
        String[] lines = content.split("\\r?\\n");
        List<Map<String, Object>> entries = new ArrayList<>();
        Map<String, Object> current = null;

        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                continue;
            }
            Matcher m = LOG_LINE_PATTERN.matcher(line);
            if (m.matches()) {
                String timestamp = m.group(1);
                String thread = m.group(2);
                String lvl = m.group(3);
                String logger = m.group(4);
                String traceId = m.group(5) == null ? "" : m.group(5).trim();
                String message = m.group(6) == null ? "" : m.group(6);

                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("service", serviceName);
                entry.put("serviceLabel", serviceLabel);
                entry.put("timestamp", timestamp);
                entry.put("thread", thread);
                entry.put("level", lvl);
                entry.put("logger", logger);
                entry.put("shortLogger", shortenLogger(logger));
                entry.put("traceId", traceId);
                entry.put("message", message);
                entries.add(entry);
                current = entry;
            } else if (current != null) {
                String existing = String.valueOf(current.get("message"));
                if (existing.length() < MAX_MESSAGE_LENGTH) {
                    String appended = existing + "\n" + line;
                    if (appended.length() > MAX_MESSAGE_LENGTH) {
                        appended = appended.substring(0, MAX_MESSAGE_LENGTH) + "...(truncated)";
                    }
                    current.put("message", appended);
                }
            }
        }
        return entries;
    }

    private long countLogEntries(Path file) {
        String content = readTailUtf8(file, MAX_TAIL_BYTES);
        if (content.isEmpty()) {
            return 0L;
        }
        long count = 0;
        for (String line : content.split("\\r?\\n")) {
            if (LOG_LINE_PATTERN.matcher(line).matches()) {
                count++;
            }
        }
        return count;
    }

    private String readTailUtf8(Path file, int maxBytes) {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long fileLen = raf.length();
            if (fileLen <= 0) {
                return "";
            }
            long start = Math.max(0L, fileLen - maxBytes);
            raf.seek(start);
            int toRead = (int) (fileLen - start);
            byte[] buf = new byte[toRead];
            raf.readFully(buf);
            String text = new String(buf, StandardCharsets.UTF_8);
            if (start > 0) {
                int nl = text.indexOf('\n');
                if (nl >= 0 && nl + 1 < text.length()) {
                    text = text.substring(nl + 1);
                }
            }
            return text;
        } catch (IOException e) {
            return "";
        }
    }

    private String shortenLogger(String logger) {
        if (logger == null) {
            return "";
        }
        int lastDot = logger.lastIndexOf('.');
        return lastDot >= 0 && lastDot + 1 < logger.length() ? logger.substring(lastDot + 1) : logger;
    }

    private long safeFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return 0L;
        }
    }

    private String safeLastModified(Path path) {
        try {
            Instant instant = Files.getLastModifiedTime(path).toInstant();
            return LocalDateTime.ofInstant(instant, ZoneId.systemDefault()).format(TS_FMT);
        } catch (IOException e) {
            return null;
        }
    }

    private long safeQueryLong(String sql) {
        try {
            Long val = jdbcTemplate.queryForObject(sql, Long.class);
            return val == null ? 0L : val;
        } catch (Exception e) {
            return 0L;
        }
    }

    private double safeQueryDouble(String sql) {
        try {
            Double val = jdbcTemplate.queryForObject(sql, Double.class);
            return val == null ? 0.0 : val;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private List<String> safeQueryStringList(String sql) {
        try {
            return jdbcTemplate.queryForList(sql, String.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private String formatTimestamp(Timestamp ts) {
        if (ts == null) {
            return null;
        }
        return ts.toLocalDateTime().format(TS_FMT);
    }
}
