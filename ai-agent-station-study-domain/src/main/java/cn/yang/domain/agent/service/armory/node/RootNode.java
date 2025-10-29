package cn.yang.domain.agent.service.armory.node;

import cn.bugstack.wrench.design.framework.tree.StrategyHandler;
import cn.yang.domain.agent.model.entity.ArmoryCommandEntity;
import cn.yang.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.yang.domain.agent.service.armory.business.data.ILoadDataStrategy;
import cn.yang.domain.agent.service.armory.node.factory.DefaultArmoryStrategyFactory;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 根节点，数据加载
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/6/27 16:47
 */
@Slf4j
@Service
public class RootNode extends AbstractArmorySupport {

    private final Map<String, ILoadDataStrategy> loadDataStrategyMap;

    @Resource
    private AiClientApiNode aiClientApiNode;

    /**
     * 构造函数，注入数据加载策略
     * @param loadDataStrategyMap 数据加载策略
     */
    public RootNode(Map<String, ILoadDataStrategy> loadDataStrategyMap) {
        this.loadDataStrategyMap = loadDataStrategyMap;
    }

    /**
     * 多线程执行数据加载策略
     * @param requestParameter 请求数据
     * @param dynamicContext 动态上下文
     * @throws ExecutionException 异常
     * @throws InterruptedException 异常
     * @throws TimeoutException 异常
     */
    @Override
    protected void multiThread(ArmoryCommandEntity requestParameter,
                               DefaultArmoryStrategyFactory.DynamicContext dynamicContext)
            throws ExecutionException, InterruptedException, TimeoutException {
        // 获取命令；不同的命令类型，对应不同的数据加载策略
        String commandType = requestParameter.getCommandType();

        // 获取策略
        AiAgentEnumVO aiAgentEnumVO = AiAgentEnumVO.getByCode(commandType);
        String loadDataStrategyKey = aiAgentEnumVO.getLoadDataStrategy();

        // 加载数据
        ILoadDataStrategy loadDataStrategy = loadDataStrategyMap.get(loadDataStrategyKey);
        loadDataStrategy.loadData(requestParameter, dynamicContext);
    }

    @Override
    protected String doApply(ArmoryCommandEntity requestParameter, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("Ai Agent 构建，数据加载节点 {}", JSON.toJSONString(requestParameter));
        return router(requestParameter, dynamicContext);
    }

    /**
     * 获取下一个节点，进入客户端节点
     * @param armoryCommandEntity 入参
     * @param dynamicContext   上下文
     * @return
     * @throws Exception
     */
    @Override
    public StrategyHandler<ArmoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext, String> get(ArmoryCommandEntity armoryCommandEntity, DefaultArmoryStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return aiClientApiNode;
    }

}
