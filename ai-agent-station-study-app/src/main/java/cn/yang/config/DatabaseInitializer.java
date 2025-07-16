package cn.yang.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DatabaseInitializer implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 测试表是否存在
            jdbcTemplate.queryForObject(
                    "SELECT 1 FROM public.vector_store LIMIT 1",
                    Integer.class
            );
        } catch (Exception e) {
            log.info("postgresql vector_store表不存在，正在创建...");
            // 表不存在，创建它
            initializeVectorStore();
        }
    }

    private void initializeVectorStore() {
        // 创建扩展
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");

        // 创建表
        jdbcTemplate.execute("""
CREATE TABLE public.vector_store (
                                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                            content TEXT NOT NULL,
                                            metadata JSONB,
                                            embedding VECTOR(1536)
);

        """);
    }
}