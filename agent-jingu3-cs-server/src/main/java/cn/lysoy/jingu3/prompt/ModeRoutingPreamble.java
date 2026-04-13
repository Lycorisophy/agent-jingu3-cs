package cn.lysoy.jingu3.prompt;

import cn.lysoy.jingu3.common.constant.PromptTemplates;
import cn.lysoy.jingu3.engine.ActionMode;
import cn.lysoy.jingu3.engine.ExecutionContext;

/**
 * 将「本轮路由来源 + 当前模式自然语言说明 + 各模式含义」拼入送入 LLM 的提示词前缀，
 * 使模型能结合上下文善意提示用户切换到更合适模式（见 {@link PromptTemplates#MODE_CATALOG_FOR_LLM}）。
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
