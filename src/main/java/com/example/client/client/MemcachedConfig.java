package com.example.client.client;

import net.spy.memcached.MemcachedClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

@Configuration
public class MemcachedConfig {

    @Value("${memcached.servers}")
    private String servers;

    @Bean
    public MemcachedClient memcachedClient() throws IOException {
        List<InetSocketAddress> addresses = Arrays.stream(servers.split(","))
                .map(server -> {
                    String[] parts = server.split(":");
                    String host = parts[0];
                    int port = Integer.parseInt(parts[1]);
                    return new InetSocketAddress(host, port);
                })
                .toList();
        return new MemcachedClient(addresses);
    }
}
