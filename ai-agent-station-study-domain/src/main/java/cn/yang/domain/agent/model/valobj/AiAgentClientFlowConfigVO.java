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
public class AiAgentClientFlowConfigVO {
    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 客户端名称
     */
    private String clientName;

    /**
     * 客户端枚举
     */
    private String clientType;

    /**
     * 序列号(执行顺序)
     */
    private Integer sequence;

}
