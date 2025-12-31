package com.ads.adshub.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;

import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import javax.net.ssl.SSLContext;

@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String elasticUrl;

    @Value("${spring.elasticsearch.username}")
    private String username;

    @Value("${spring.elasticsearch.password}")
    private String password;

    @Bean
    public ElasticsearchClient elasticsearchClient() throws Exception {

        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(null, (x, y) -> true)
                .build();

        BasicCredentialsProvider creds = new BasicCredentialsProvider();
        creds.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

        RestClient restClient = RestClient.builder(HttpHost.create(elasticUrl))
                .setHttpClientConfigCallback(hc -> hc
                        .setSSLContext(sslContext)
                        .setSSLHostnameVerifier((hostname, session) -> true)   // <—
                        .setDefaultCredentialsProvider(creds))
                .build();

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        ElasticsearchTransport transport =
                new RestClientTransport(restClient, new JacksonJsonpMapper(mapper));

        return new ElasticsearchClient(transport);
    }

}

