package cn.yang.domain.agent.model.valobj;

/**
 * @version v1.0
 * @ClassName: cn.yang.domain.agent.model.valobj
 * @Description: TODO
 * @Author: YJ
 */

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI智能体配置值对象
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/9/2 07:05
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiAgentVO {

    /**
     * 智能体ID
     */
    private String agentId;

    /**
     * 智能体名称
     */
    private String agentName;

    /**
     * 描述
     */
    private String description;

    /**
     * 渠道类型(agent，chat_stream)
     */
    private String channel;

    /**
     * 执行策略(auto、flow)
     */
    private String strategy;

    /**
     * 状态(0:禁用,1:启用)
     */
    private Integer status;

}