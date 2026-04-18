package cn.lysoy.jingu3.service.context.stream;

import cn.lysoy.jingu3.service.context.chat.ConversationStmBuffer;

/**
 * 在流式正常结束时把本轮「用户原文 + 拼接后的助手可见文本」写入 STM。
 */
public final class StmRecordingStreamEventSink implements StreamEventSink {

    private final StreamEventSink delegate;
    private final ConversationStmBuffer stmBuffer;
    private final boolean stmEnabled;
    private final String conversationId;
    private final String userRawForStm;
    private final StringBuilder assistantAccum = new StringBuilder();

    public StmRecordingStreamEventSink(
            StreamEventSink delegate,
            ConversationStmBuffer stmBuffer,
            boolean stmEnabled,
            String conversationId,
            String userRawForStm) {
        this.delegate = delegate;
        this.stmBuffer = stmBuffer;
        this.stmEnabled = stmEnabled;
        this.conversationId = conversationId;
        this.userRawForStm = userRawForStm == null ? "" : userRawForStm;
    }

    @Override
    public void meta(String actionMode, String routingSource, String userId, String username) {
        delegate.meta(actionMode, routingSource, userId, username);
    }

    @Override
    public void token(String delta) {
        if (delta != null && !delta.isEmpty()) {
            assistantAccum.append(delta);
        }
        delegate.token(delta);
    }

    @Override
    public void stepBegin(int step, String label) {
        delegate.stepBegin(step, label);
    }

    @Override
    public void stepEnd(int step) {
        delegate.stepEnd(step);
    }

    @Override
    public void block(String text) {
        if (text != null && !text.isEmpty()) {
            assistantAccum.append(text);
        }
        delegate.block(text);
    }

    @Override
    public void toolResult(String toolId, String toolOutput) {
        delegate.toolResult(toolId, toolOutput);
    }

    @Override
    public void done() {
        if (stmEnabled) {
            stmBuffer.recordTurn(conversationId, userRawForStm, assistantAccum.toString());
        }
        delegate.done();
    }

    @Override
    public void error(String message) {
        delegate.error(message);
    }
}
