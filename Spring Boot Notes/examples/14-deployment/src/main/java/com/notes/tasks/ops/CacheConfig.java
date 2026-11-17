package com.notes.tasks.ops;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/** Turns on @Cacheable / @CachePut / @CacheEvict. Boot then auto-configures a CacheManager. */
@Configuration
@EnableCaching
public class CacheConfig {
}
