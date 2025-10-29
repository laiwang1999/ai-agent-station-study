package cn.yang.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @version v1.0
 * @ClassName: cn.yang.domain.agent.model.valobj
 * @Description: TODO
 * @Author: YJ
 */
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class AiAgentTaskScheduleVO {
    private Long id;
    private String agentId;
    private String description;
    /**
     * 定时表达式
     */
    private String cronExpression;
    /**
     * 任务入参配置
     */
    private String taskParam;
}
