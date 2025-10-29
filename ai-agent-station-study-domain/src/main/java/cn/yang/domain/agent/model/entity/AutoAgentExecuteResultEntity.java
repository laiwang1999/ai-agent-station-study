package cn.yang.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @version v1.0
 * @ClassName: cn.yang.domain.agent.model.entity
 * @Description: TODO
 * @Author: YJ
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AutoAgentExecuteResultEntity {
    private String type;
    private String subType;
    private Integer step;
    private String content;
    private Boolean completed;
    private Long timestamp;
    private String sessionId;


    /**
     * 创建分析阶段结果
     */
    public static AutoAgentExecuteResultEntity createAnalysisResult(Integer step, String content, String sessionId) {
        return AutoAgentExecuteResultEntity.builder()
                .type("analysis")
                .step(step)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建分析阶段细分结果
     */
    public static AutoAgentExecuteResultEntity createAnalysisSubResult(Integer step, String subType, String content, String sessionId) {
        return AutoAgentExecuteResultEntity.builder()
                .type("analysis")
                .subType(subType)
                .step(step)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建执行阶段结果
     */
    public static AutoAgentExecuteResultEntity createExecutionResult(Integer step, String content, String sessionId) {
        return AutoAgentExecuteResultEntity.builder()
                .type("execution")
                .step(step)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建执行阶段细分结果
     */
    public static AutoAgentExecuteResultEntity createExecutionSubResult(Integer step, String subType, String content, String sessionId) {
        return AutoAgentExecuteResultEntity.builder()
                .type("execution")
                .subType(subType)
                .step(step)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建监督阶段结果
     */
    public static AutoAgentExecuteResultEntity createSupervisionResult(Integer step, String content, String sessionId) {
        return AutoAgentExecuteResultEntity.builder()
                .type("supervision")
                .step(step)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建监督阶段细分结果
     */
    public static AutoAgentExecuteResultEntity createSupervisionSubResult(Integer step, String subType, String content, String sessionId) {
        return AutoAgentExecuteResultEntity.builder()
                .type("supervision")
                .subType(subType)
                .step(step)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建总结阶段细分的结果
     */
    public static AutoAgentExecuteResultEntity createSummarySubResult(String subType, String content, String sessionId) {
        return AutoAgentExecuteResultEntity.builder()
                .type("summary")
                .subType(subType)
                .step(4)
                .content(content)
                .completed(false)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建总结阶段结果
     */
    public static AutoAgentExecuteResultEntity createSummaryResult(String content, String sessionId) {
        return AutoAgentExecuteResultEntity.builder()
                .type("summary")
                .step(null)
                .content(content)
                .completed(true)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建错误结果
     */
    public static AutoAgentExecuteResultEntity createErrorResult(String content, String sessionId) {
        return AutoAgentExecuteResultEntity.builder()
                .type("error")
                .step(null)
                .content(content)
                .completed(true)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }

    /**
     * 创建完成标识
     */
    public static AutoAgentExecuteResultEntity createCompleteResult(String sessionId) {
        return AutoAgentExecuteResultEntity.builder()
                .type("complete")
                .step(null)
                .content("执行完成")
                .completed(true)
                .timestamp(System.currentTimeMillis())
                .sessionId(sessionId)
                .build();
    }
}
