package cn.yang.domain.agent.service.execute.auto.step;

import cn.bugstack.wrench.design.framework.tree.AbstractMultiThreadStrategyRouter;
import cn.yang.domain.agent.adapter.repository.IAgentRepository;
import cn.yang.domain.agent.model.entity.AutoAgentExecuteResultEntity;
import cn.yang.domain.agent.model.entity.ExecuteCommandEntity;
import cn.yang.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.yang.domain.agent.service.execute.auto.step.factory.DefaultAutoAgentExecuteStrategyFactory;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * @version v1.0
 * @ClassName: cn.yang.domain.agent.service.execute.auto.step
 * @Description: TODO
 * @Author: YJ
 */
@Slf4j
public abstract class AbstractExecuteSupport extends AbstractMultiThreadStrategyRouter<ExecuteCommandEntity,
        DefaultAutoAgentExecuteStrategyFactory.DynamicContext,
        String> {
    @Resource
    protected ApplicationContext applicationContext;
    @Resource
    protected IAgentRepository repository;

    public static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    public static final String CHAT_MEMORY_RETRIEVE_SIZE_KEY = "chat_memory_retrieve_size";


    @Override
    protected void multiThread(ExecuteCommandEntity executeCommandEntity, DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {

    }

    protected ChatClient getChatClientByClientId(String clientId) {
        return getBean(AiAgentEnumVO.AI_CLIENT.getBeanName(clientId));
    }
    protected <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }

    public void sendSseResult(DefaultAutoAgentExecuteStrategyFactory.DynamicContext dynamicContext,
                              AutoAgentExecuteResultEntity autoAgentExecuteResultEntity)  {
        try{
            ResponseBodyEmitter emitter = dynamicContext.getValue("emitter");
            if(emitter!=null) {
                // 发送sse格式的数据
                String sseData = "data: " + JSON.toJSONString(autoAgentExecuteResultEntity) + "\n\n";
                emitter.send(sseData);
                log.info("sseData:{}",sseData);
            }
        }catch (IOException e){
        }
    }
}
