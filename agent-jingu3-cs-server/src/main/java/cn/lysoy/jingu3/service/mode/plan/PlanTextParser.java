package cn.lysoy.jingu3.service.mode.plan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <strong>计划文本 → 子任务列表</strong>（Plan-and-Execute 驾驭链路的「弱结构化」环节）：从 Planner 输出的<strong>自然语言</strong>
 * 中抽取编号行作为 Executor 逐步输入。业界更稳妥做法是强制 JSON Schema；本仓库用正则降低模型遵循成本，
 * 若未匹配任何编号行则<strong>整段计划视为单步子任务</strong>，避免返回空列表导致无执行。
 */
public final class PlanTextParser {

    /** 匹配行首 {@code 1.} / {@code 1)} / {@code 1、} 等形式，捕获标题后正文 */
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
