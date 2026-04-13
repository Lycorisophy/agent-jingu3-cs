package cn.lysoy.jingu3.events.config;

import cn.lysoy.jingu3.config.Jingu3Properties;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * v0.6-C：Elasticsearch Java API Client；{@code jingu3.elasticsearch.enabled=true} 时生效。
 * <p>仅暴露 {@link ElasticsearchClient}，关闭时级联释放底层 {@link RestClient}。</p>
 */
@Configuration
@ConditionalOnProperty(prefix = "jingu3.elasticsearch", name = "enabled", havingValue = "true")
public class ElasticsearchClientConfiguration {

    @Bean(destroyMethod = "close")
    public ElasticsearchClient elasticsearchClient(Jingu3Properties properties) {
        RestClient restClient = buildRestClient(properties);
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    private static RestClient buildRestClient(Jingu3Properties properties) {
        Jingu3Properties.Elasticsearch e = properties.getElasticsearch();
        HttpHost httpHost = new HttpHost(e.getHost(), e.getPort(), e.getScheme());
        RestClientBuilder builder = RestClient.builder(httpHost);
        if (e.getUsername() != null && !e.getUsername().isBlank()) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                    new AuthScope(e.getHost(), e.getPort()),
                    new UsernamePasswordCredentials(
                            e.getUsername(), e.getPassword() == null ? "" : e.getPassword()));
            builder.setHttpClientConfigCallback(hc -> hc.setDefaultCredentialsProvider(credentialsProvider));
        }
        return builder.build();
    }
}
