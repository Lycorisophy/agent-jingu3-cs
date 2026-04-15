package cn.lysoy.jingu3.engine;

import cn.lysoy.jingu3.engine.mode.AgentTeamModeHandler;
import cn.lysoy.jingu3.engine.mode.AskModeHandler;
import cn.lysoy.jingu3.engine.mode.CronModeHandler;
import cn.lysoy.jingu3.engine.mode.HumanInLoopModeHandler;
import cn.lysoy.jingu3.engine.mode.PlanAndExecuteModeHandler;
import cn.lysoy.jingu3.engine.mode.ReActModeHandler;
import cn.lysoy.jingu3.engine.mode.StateTrackingModeHandler;
import cn.lysoy.jingu3.engine.mode.WorkflowModeHandler;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * <strong>八大行动模式注册表</strong>（驾驭工程）：{@link ActionMode} → {@link ActionModeHandler} 的编译期固定映射，
 * Spring 构造时注入八个具体 Handler，供 {@link cn.lysoy.jingu3.service.chat.ChatService} 与 {@link cn.lysoy.jingu3.service.chat.ChatStreamService}
 * 在路由完成后 O(1) 分派。
 * <p>新增对话可选模式时需同步：枚举 {@link ActionMode}、本表、{@link cn.lysoy.jingu3.engine.ActionModePolicy}、
 * {@link cn.lysoy.jingu3.engine.routing.IntentRouter} / 分类器白名单及客户端契约。</p>
 */
@Component
public class ModeRegistry {

    /** 八种模式各唯一实现；未在构造器中 put 的模式在运行时应视为配置错误 */
    private final Map<ActionMode, ActionModeHandler> handlers = new EnumMap<>(ActionMode.class);

    /**
     * 由 Spring 注入全部 {@link ActionModeHandler} 实现并装入 EnumMap（顺序与枚举定义无关，以 put 键为准）。
     */
    public ModeRegistry(
            AskModeHandler ask,
            ReActModeHandler react,
            PlanAndExecuteModeHandler planAndExecute,
            WorkflowModeHandler workflow,
            AgentTeamModeHandler agentTeam,
            CronModeHandler cron,
            StateTrackingModeHandler stateTracking,
            HumanInLoopModeHandler humanInLoop) {
        handlers.put(ActionMode.ASK, ask);
        handlers.put(ActionMode.REACT, react);
        handlers.put(ActionMode.PLAN_AND_EXECUTE, planAndExecute);
        handlers.put(ActionMode.WORKFLOW, workflow);
        handlers.put(ActionMode.AGENT_TEAM, agentTeam);
        handlers.put(ActionMode.CRON, cron);
        handlers.put(ActionMode.STATE_TRACKING, stateTracking);
        handlers.put(ActionMode.HUMAN_IN_LOOP, humanInLoop);
    }

    /**
     * 按模式取得同步执行器；不存在时返回 {@code null}（正常启动下八个模式均应有实现）。
     *
     * @param mode 路由结果中的目标模式
     * @return 该模式对应的 handler，未注册则为 {@code null}
     */
    public ActionModeHandler get(ActionMode mode) {
        return handlers.get(mode);
    }
}
