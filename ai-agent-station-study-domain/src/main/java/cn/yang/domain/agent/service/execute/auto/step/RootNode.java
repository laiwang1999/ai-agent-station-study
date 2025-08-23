package cn.yang.domain.agent.service.execute.auto.step;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.yang.domain.agent.model.entity.ExecuteCommandEntity;
import cn.yang.domain.agent.model.valobj.AiAgentClientFlowConfigVO;
import cn.yang.domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

/**
 * @version v1.0
 * @ClassName: cn.yang.domain.agent.service.execute.auto.step
 * @Description: TODO
 * @Author: YJ
 */
@Slf4j
@Service("executeRootNode")
public class RootNode extends AbstractExecuteSupport {
    @Resource
    private Step1AnalyzerNode step1AnalyzerNode;



    @Override
    protected String doApply(ExecuteCommandEntity requestParameter, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("===动态多轮执行测试开始===");
        log.info("用户输入：{}", requestParameter.getMessage());
        log.info("最大执行步数：{}", requestParameter.getMaxStep());
        log.info("会话ID:{}", requestParameter.getSessionId());

        Map<String, AiAgentClientFlowConfigVO> aiAgentClientFlowConfigVOMap =
                repository.queryAiAgentClientFlowConfig(requestParameter.getAiAgentId());

        //客户端对话组
        dynamicContext.setAiAgentClientFlowConfigVOMap(aiAgentClientFlowConfigVOMap);
        //上下文信息
        dynamicContext.setExecutionHistory(new StringBuilder());
        //当前任务信息
        dynamicContext.setCurrentTask(requestParameter.getMessage());
        //最大任务步骤
        dynamicContext.setMaxStep(requestParameter.getMaxStep());

        return router(requestParameter, dynamicContext);
    }

    @Override
    public StrategyHandler<ExecuteCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext, String> get(ExecuteCommandEntity executeCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return step1AnalyzerNode;
    }
}
