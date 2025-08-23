package cn.yang.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @version v1.0
 * @ClassName: cn.yang.domain.agent.model.entity
 * @Description: 执行命令实体
 * @Author: YJ
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ExecuteCommandEntity {
    private String aiAgentId;
    private String message;
    private String sessionId;
    private Integer maxStep;
}
