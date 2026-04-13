package cn.lysoy.jingu3.events.config;

import cn.lysoy.jingu3.config.Jingu3Properties;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 启动时若索引不存在则根据 classpath 模板创建（无 IK，便于本地空集群）。
 */
@Slf4j
@Component
@ConditionalOnBean(ElasticsearchClient.class)
public class EventIndexInitializer {

    private final ElasticsearchClient client;

    private final Jingu3Properties properties;

    public EventIndexInitializer(ElasticsearchClient client, Jingu3Properties properties) {
        this.client = client;
        this.properties = properties;
    }

    @PostConstruct
    public void ensureIndex() {
        String name = properties.getElasticsearch().getIndexEvents();
        try {
            boolean exists = client.indices().exists(e -> e.index(name)).value();
            if (exists) {
                return;
            }
            ClassPathResource resource = new ClassPathResource("elasticsearch/jingu3-events-index.json");
            try (InputStream in = resource.getInputStream()) {
                client.indices().create(c -> c.index(name).withJson(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
            log.info("Elasticsearch index created: {}", name);
        } catch (Exception ex) {
            log.error("Elasticsearch 索引初始化失败 index={}（可稍后手工 PUT 或检查集群）", name, ex);
        }
    }
}
