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
 * 行动模式到 {@link ActionModeHandler} 实例的注册表（Spring 注入各模式 Bean，启动时装配）。
 * <p>新增模式时需：增加 {@link ActionMode}、实现 handler、在此注册、并同步 {@link cn.lysoy.jingu3.engine.ActionModePolicy} 等策略。</p>
 */
@Component
public class ModeRegistry {

    private final Map<ActionMode, ActionModeHandler> handlers = new EnumMap<>(ActionMode.class);

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
