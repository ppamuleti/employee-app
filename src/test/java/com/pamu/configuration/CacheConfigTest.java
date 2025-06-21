package com.pamu.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import static org.junit.jupiter.api.Assertions.*;

class CacheConfigTest {

    @Test
    void testCaffeineConfigBean() {
        CacheConfig config = new CacheConfig();
        Caffeine<Object, Object> caffeine = config.caffeineConfig();
        assertNotNull(caffeine);
    }

    @Test
    void testCacheManagerBean() {
        CacheConfig config = new CacheConfig();
        Caffeine<Object, Object> caffeine = config.caffeineConfig();
        CacheManager cacheManager = config.cacheManager(caffeine);
        assertNotNull(cacheManager);
        assertTrue(cacheManager instanceof CaffeineCacheManager);
    }
}

