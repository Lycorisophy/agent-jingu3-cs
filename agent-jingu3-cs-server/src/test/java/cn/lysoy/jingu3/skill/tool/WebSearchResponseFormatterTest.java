package cn.lysoy.jingu3.skill.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSearchResponseFormatterTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void formatDuckDuckGo_withAbstractAndRelated() throws Exception {
        String json = """
                {
                  "Heading": "Example",
                  "AbstractText": "Hello world",
                  "AbstractURL": "https://example.com/a",
                  "RelatedTopics": [
                    {"Text": "Topic one", "FirstURL": "https://example.com/1"}
                  ]
                }
                """;
        String out = WebSearchResponseFormatter.formatDuckDuckGo(mapper.readTree(json), 5);
        assertTrue(out.contains("Example"));
        assertTrue(out.contains("Hello world"));
        assertTrue(out.contains("https://example.com/a"));
        assertTrue(out.contains("Topic one"));
    }

    @Test
    void formatDuckDuckGo_empty() throws Exception {
        String json = "{}";
        String out = WebSearchResponseFormatter.formatDuckDuckGo(mapper.readTree(json), 3);
        assertTrue(out.contains("未检索"));
    }

    @Test
    void formatTavily_results() throws Exception {
        String json = """
                {
                  "results": [
                    {"title": "T", "url": "https://u", "content": "C"}
                  ]
                }
                """;
        String out = WebSearchResponseFormatter.formatTavily(mapper.readTree(json), 3);
        assertTrue(out.contains("T"));
        assertTrue(out.contains("https://u"));
        assertTrue(out.contains("C"));
    }

    @Test
    void formatTavily_empty() throws Exception {
        String json = "{\"results\":[]}";
        String out = WebSearchResponseFormatter.formatTavily(mapper.readTree(json), 3);
        assertTrue(out.contains("未检索"));
    }
}
