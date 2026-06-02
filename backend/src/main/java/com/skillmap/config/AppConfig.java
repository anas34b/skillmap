package com.skillmap.config;

import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executors;

// ─────────────────────────────────────────
//  THREAD CONFIG — Virtual Threads Java 21
// ─────────────────────────────────────────
@Configuration
class ThreadConfig {

    /**
     * Active les Virtual Threads (Project Loom) pour Tomcat.
     * Chaque requête HTTP s'exécute dans un Virtual Thread léger.
     * Gère des milliers de requêtes concurrentes sans saturer le pool.
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> virtualThreadsCustomizer() {
        return protocolHandler ->
            protocolHandler.setExecutor(
                Executors.newVirtualThreadPerTaskExecutor()
            );
    }

    /**
     * Scheduler avec Virtual Threads pour les collecteurs planifiés
     */
    @Bean
    public SimpleAsyncTaskScheduler taskScheduler() {
        var scheduler = new SimpleAsyncTaskScheduler();
        scheduler.setVirtualThreads(true); // Java 21 — un Virtual Thread par tâche planifiée
        scheduler.setConcurrencyLimit(5);
        scheduler.setThreadNamePrefix("skillmap-scheduler-");
        return scheduler;
    }

    /**
     * RestClient bean partagé pour les appels HTTP externes
     */
    @Bean
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}

// ─────────────────────────────────────────
//  REDIS CONFIG — Cache + Rate Limiting
// ─────────────────────────────────────────
@Configuration
@EnableCaching
class RedisConfig {

    @Bean
    public RedisTemplate<String, Integer> redisTemplate(RedisConnectionFactory factory) {
        var template = new RedisTemplate<String, Integer>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        // Config par défaut : TTL 10 minutes
        var defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Configs spécifiques par cache
        var cacheConfigs = Map.of(
            "insights",      defaultConfig.entryTtl(Duration.ofHours(1)),   // Insights IA : 1h
            "topSkills",     defaultConfig.entryTtl(Duration.ofMinutes(30)), // Top skills : 30min
            "cityStats",     defaultConfig.entryTtl(Duration.ofMinutes(30)), // Villes : 30min
            "globalStats",   defaultConfig.entryTtl(Duration.ofMinutes(15))  // Stats : 15min
        );

        return RedisCacheManager.builder(factory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build();
    }
}
