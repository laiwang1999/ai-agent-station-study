package cn.yang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 线程池配置属性类
 * 用于绑定application.yml中的线程池配置参数
 */
@Data
@ConfigurationProperties(prefix = "thread.pool.executor.config", ignoreInvalidFields = true)
public class ThreadPoolConfigProperties {

    /**
     * 核心线程数
     * 线程池维护的最小线程数量，即使没有任务执行也会保持这些线程
     * 默认值：20
     */
    private Integer corePoolSize = 20;
    /**
     * 最大线程数
     * 线程池允许创建的最大线程数量
     * 默认值：200
     */
    private Integer maxPoolSize = 200;
    /**
     * 空闲线程存活时间(秒)
     * 当线程数超过核心线程数时，多余的空闲线程的存活时间
     * 默认值：10L
     */
    private Long keepAliveTime = 10L;
    /**
     * 任务队列容量
     * 用于保存等待执行任务的阻塞队列容量
     * 默认值：5000
     */
    private Integer blockQueueSize = 5000;
    /**
     * 拒绝策略
     * 当线程池和队列都满时的任务处理策略
     * 可选值：AbortPolicy(默认)、DiscardPolicy、DiscardOldestPolicy、CallerRunsPolicy
     * <p>
     * AbortPolicy：丢弃任务并抛出RejectedExecutionException异常
     * DiscardPolicy：直接丢弃任务，不抛出异常
     * DiscardOldestPolicy：删除最早进入队列的任务，再尝试加入新任务
     * CallerRunsPolicy：由调用线程处理该任务
     */
    private String policy = "AbortPolicy";

}