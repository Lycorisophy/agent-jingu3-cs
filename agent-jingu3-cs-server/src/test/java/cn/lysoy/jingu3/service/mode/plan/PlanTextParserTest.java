package cn.lysoy.jingu3.service.mode.plan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanTextParserTest {

    @Test
    void parsesNumberedLines() {
        String plan = """
                计划如下：
                1. 第一步
                2) 第二步
                3、第三步
                """;
        List<String> s = PlanTextParser.parseSubtasks(plan);
        assertEquals(3, s.size());
        assertEquals("第一步", s.get(0));
        assertEquals("第二步", s.get(1));
        assertEquals("第三步", s.get(2));
    }

    @Test
    void fallbackSingleStepWhenNoNumbers() {
        List<String> s = PlanTextParser.parseSubtasks("无编号的整段计划");
        assertEquals(1, s.size());
        assertEquals("无编号的整段计划", s.get(0));
    }

    @Test
    void emptyReturnsEmpty() {
        assertEquals(0, PlanTextParser.parseSubtasks("").size());
    }
}
