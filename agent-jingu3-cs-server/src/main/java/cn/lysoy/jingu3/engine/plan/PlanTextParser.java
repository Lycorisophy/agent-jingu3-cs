package cn.lysoy.jingu3.engine.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 Planner 输出的<strong>自然语言</strong>计划中抽取编号子任务（指南 §5：Executor 前的结构化步骤）。
 * 业界更稳妥的做法是要求模型输出 JSON Schema / 结构化字段；本仓库用正则解析 {@code 1. / 1) / 1、} 行以降低门槛，
 * 若解析不到编号行则退化为「整段文本算一步」。
 */
public final class PlanTextParser {

    private static final Pattern NUMBERED_LINE = Pattern.compile(
            "^\\s*\\d+[\\).、]\\s*(.+)$",
            Pattern.MULTILINE);

    private PlanTextParser() {
    }

    /**
     * 解析形如 {@code 1. xxx} / {@code 1) xxx} 的行；若无匹配则返回单元素列表（全文作为一步）。
     */
    public static List<String> parseSubtasks(String planText) {
        if (planText == null || planText.isBlank()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        Matcher m = NUMBERED_LINE.matcher(planText);
        while (m.find()) {
            String line = m.group(1).trim();
            if (!line.isEmpty()) {
                out.add(line);
            }
        }
        if (out.isEmpty()) {
            out.add(planText.trim());
        }
        return out;
    }
}
