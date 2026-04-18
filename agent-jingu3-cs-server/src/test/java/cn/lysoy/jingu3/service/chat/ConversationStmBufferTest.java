package cn.lysoy.jingu3.service.context.chat;

import cn.lysoy.jingu3.config.Jingu3Properties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationStmBufferTest {

    @Test
    void respectsMaxPairsAndDropLastTurn() {
        Jingu3Properties p = new Jingu3Properties();
        p.getChat().setStmEnabled(true);
        p.getChat().setStmMaxPairs(2);
        ConversationStmBuffer buf = new ConversationStmBuffer(p);
        String cid = "conv-test-1";
        buf.recordTurn(cid, "u1", "a1");
        buf.recordTurn(cid, "u2", "a2");
        buf.recordTurn(cid, "u3", "a3");
        List<String> lines = buf.snapshotLines(cid);
        assertThat(lines).hasSize(4);
        assertThat(lines.get(0)).startsWith("用户：u2");
        assertThat(lines.get(2)).startsWith("用户：u3");

        buf.dropLastTurn(cid);
        List<String> afterDrop = buf.snapshotLines(cid);
        assertThat(afterDrop).hasSize(2);
        assertThat(afterDrop.get(0)).startsWith("用户：u2");
    }

    @Test
    void disabledIsNoOp() {
        Jingu3Properties p = new Jingu3Properties();
        p.getChat().setStmEnabled(false);
        ConversationStmBuffer buf = new ConversationStmBuffer(p);
        buf.recordTurn("x", "u", "a");
        assertThat(buf.snapshotLines("x")).isEmpty();
    }
}
