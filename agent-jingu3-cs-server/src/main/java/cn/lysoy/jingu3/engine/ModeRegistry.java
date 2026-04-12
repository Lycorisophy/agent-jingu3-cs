package cn.lysoy.jingu3.engine;

import cn.lysoy.jingu3.engine.mode.AskModeHandler;
import cn.lysoy.jingu3.engine.mode.ReActModeHandler;
import cn.lysoy.jingu3.engine.mode.StubActionModeHandler;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * 行动模式 → 处理器注册表。
 */
@Component
public class ModeRegistry {

    private final Map<ActionMode, ActionModeHandler> handlers = new EnumMap<>(ActionMode.class);

    public ModeRegistry(AskModeHandler ask, ReActModeHandler react, StubActionModeHandler stub) {
        handlers.put(ActionMode.ASK, ask);
        handlers.put(ActionMode.REACT, react);
        for (ActionMode m : ActionMode.values()) {
            handlers.putIfAbsent(m, stub);
        }
    }

    public ActionModeHandler get(ActionMode mode) {
        return handlers.get(mode);
    }
}
