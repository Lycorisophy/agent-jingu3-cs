package cn.lysoy.jingu3.prompt;

import cn.lysoy.jingu3.common.constant.PromptTemplates;
import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.engine.ExecutionContext;

/**
 * <strong>意图与模式对 LLM 可见</strong>（提示词工程子模块）：在每条送模提示的<strong>最前部</strong>插入固定结构文案，
 * 说明本轮 {@link cn.lysoy.jingu3.engine.routing.RoutingSource}、当前 {@link cn.lysoy.jingu3.engine.ActionMode} 以及
 * 各模式职责速查（{@link PromptTemplates#MODE_CATALOG_FOR_LLM}），使模型在回答中能<strong>善意引导</strong>用户切换到
 * 更契合任务的模式，并与《JinGu3 AI Agent 客户端 UI 设计规范》中「模式含义对用户/模型可见」的要求对齐。
 */
public final class ModeRoutingPreamble {

    private ModeRoutingPreamble() {
    }

    /**
     * 置于系统/角色段之前，与具体模式模板（如 ASK_SYSTEM）之间用空行分隔。
     */
    public static String build(ExecutionContext ctx) {
        if (ctx == null) {
            return "";
        }
        // 首行：人类可读的中文路由说明 + 内部枚举名，便于排障与模型对齐「谁决定了模式」
        String headLine = switch (ctx.getRoutingSource()) {
            case CLIENT_EXPLICIT ->
                    "【模式路由】用户显式选择了「" + modeDisplayZh(ctx.getSelectedMode()) + "」模式（内部标识 "
                            + ctx.getSelectedMode().name() + "）。";
            case RULE ->
                    "【模式路由】系统按规则/关键词将本轮对话定为「" + modeDisplayZh(ctx.getSelectedMode()) + "」模式（"
                            + ctx.getSelectedMode().name() + "）。";
            case MODEL ->
                    "【模式路由】系统经意图场景识别（模型分类）将本轮对话定为「" + modeDisplayZh(ctx.getSelectedMode())
                            + "」模式（" + ctx.getSelectedMode().name() + "）。";
            case FALLBACK ->
                    "【模式路由】模式解析失败已降级，当前使用「" + modeDisplayZh(ctx.getSelectedMode()) + "」模式（"
                            + ctx.getSelectedMode().name() + "）。";
            case EXPLICIT_GUARD ->
                    "【模式路由】用户曾显式选择较重模式，经意图识别后已自动切换为「"
                            + modeDisplayZh(ctx.getSelectedMode()) + "」模式（"
                            + ctx.getSelectedMode().name()
                            + "）；回复开头已说明切换原因。";
        };
        return headLine
                + "\n若用户所需任务明显更适合其他模式，可礼貌说明原因并建议其切换到对应模式（勿编造不存在的功能）。\n"
                + "\n【各行动模式含义速查】\n"
                + PromptTemplates.MODE_CATALOG_FOR_LLM
                + "\n";
    }

    static String modeDisplayZh(ActionMode mode) {
        if (mode == null) {
            return "未知";
        }
        return switch (mode) {
            case ASK -> "单轮问答";
            case REACT -> "推理与行动（ReAct）";
            case PLAN_AND_EXECUTE -> "计划后执行";
            case WORKFLOW -> "工作流编排";
            case AGENT_TEAM -> "多智能体协同";
            case CRON -> "定时任务意图";
            case STATE_TRACKING -> "状态追踪";
            case HUMAN_IN_LOOP -> "人在环审批";
        };
    }
}
