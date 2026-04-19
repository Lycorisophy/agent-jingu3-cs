package cn.lysoy.jingu3.bpmn;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * BPMN ServiceTask：从流程变量读取 {@code prompt}，调用本地 LLM，结果写入 {@code llmOutput}。
 */
@Component("jingu3LlmDelegate")
public class Jingu3LlmJavaDelegate implements JavaDelegate {

    private final ChatLanguageModel chatLanguageModel;

    public Jingu3LlmJavaDelegate(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String prompt = Objects.toString(execution.getVariable("prompt"), "").trim();
        if (prompt.isEmpty()) {
            execution.setVariable("llmOutput", "");
            execution.setVariable("llmError", "prompt 变量为空");
            return;
        }
        try {
            String reply = chatLanguageModel.generate(
                    "你是工作流中的一个步骤，请简洁回答用户问题，不要寒暄。\n\n" + prompt);
            execution.setVariable("llmOutput", reply == null ? "" : reply);
            execution.setVariable("llmError", null);
        } catch (Exception ex) {
            execution.setVariable("llmOutput", "");
            execution.setVariable("llmError", ex.getMessage() != null ? ex.getMessage() : "llm_failed");
        }
    }
}
