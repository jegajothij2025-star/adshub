package com.ads.adshub.ehcache;

import java.time.Duration;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.jsr107.Eh107Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class EHConfiguration {

    @Bean
    public CacheManager getCacheManager() {
        CachingProvider provider = Caching.getCachingProvider();
        CacheManager cacheManager = provider.getCacheManager();

        // Create cache
        createCache(cacheManager, "gendersCache", String.class, Object.class, 100, 10, 10);
        createCache(cacheManager, "districtsCache", String.class, Object.class, 100, 10, 10);
        createCache(cacheManager, "statesCache", String.class, Object.class, 100, 10, 10);
        createCache(cacheManager, "countriesCache", String.class, Object.class, 100, 10, 10);
        createCache(cacheManager, "deviceosCache", String.class, Object.class, 100, 10, 10);
        createCache(cacheManager, "eduqualificationCache", String.class, Object.class, 100, 10, 10);
        createCache(cacheManager, "languageCache", String.class, Object.class, 100, 10, 10);
        createCache(cacheManager, "currencyCache", String.class, Object.class, 100, 10, 10);
        createCache(cacheManager, "professionCache", String.class, Object.class, 100, 10, 10);

        return cacheManager;
    }

    private <K, V> void createCache(CacheManager cacheManager, String cacheName, 
                                    Class<K> keyClass, Class<V> valueClass,
                                    long heapEntries, long offheapMB, long ttlMinutes) {

        // Build Ehcache configuration
        CacheConfiguration<K, V> ehcacheConfig = CacheConfigurationBuilder
                .newCacheConfigurationBuilder(keyClass, valueClass,
                        ResourcePoolsBuilder.newResourcePoolsBuilder()
                            .heap(heapEntries, org.ehcache.config.units.EntryUnit.ENTRIES)
                            .offheap(offheapMB, MemoryUnit.MB))
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMinutes(ttlMinutes)))
                .build();

        // Convert to JSR-107 configuration
        javax.cache.configuration.Configuration<K, V> jcacheConfig =
                Eh107Configuration.fromEhcacheCacheConfiguration(ehcacheConfig);

        cacheManager.createCache(cacheName, jcacheConfig);
    }
}

