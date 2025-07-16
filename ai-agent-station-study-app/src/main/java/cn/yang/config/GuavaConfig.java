package cn.yang.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Guava缓存配置类
 * 提供基于内存的本地缓存服务
 */
@Configuration
public class GuavaConfig {

    /**
     * 创建Guava缓存实例
     * 缓存过期时间：3秒
     * 键类型：String，值类型：String
     * @return Cache<String, String> 缓存实例
     */
    @Bean(name = "cache")
    public Cache<String, String> cache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .build();
    }

}