package cn.yang.domain.agent.service;

import cn.yang.domain.agent.model.valobj.AiAgentVO;

import java.util.List;

/**
 * @version v1.0
 * @ClassName: cn.yang.domain.agent.service
 * @Description: TODO
 * @Author: YJ
 */
public interface IArmoryService {
    List<AiAgentVO> acceptArmoryAllAvailableAgents();

    void acceptArmoryAgent(String agentId);

    List<AiAgentVO> queryAvailableAgents();

    void acceptArmoryAgentClientModelApi(String apiId);
}
