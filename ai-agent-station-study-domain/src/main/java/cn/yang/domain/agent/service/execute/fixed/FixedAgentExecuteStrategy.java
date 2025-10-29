package cn.yang.domain.agent.service.execute.fixed;

import cn.yang.domain.agent.adapter.repository.IAgentRepository;
import cn.yang.domain.agent.model.entity.ExecuteCommandEntity;
import cn.yang.domain.agent.model.valobj.AiAgentClientFlowConfigVO;
import cn.yang.domain.agent.model.valobj.enums.AiAgentEnumVO;
import cn.yang.domain.agent.service.IExecuteStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.List;

/**
 * @version v1.0
 * @ClassName: cn.yang.domain.agent.service.execute.fixed
 * @Description: TODO
 * @Author: YJ
 */
@Slf4j
@Service("fixedAgentExecuteStrategy")
public class FixedAgentExecuteStrategy implements IExecuteStrategy {
    @Resource
    private IAgentRepository repository;

    @Resource
    private ApplicationContext applicationContext;

    public static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    public static final String CHAT_MEMORY_RETRIEVE_SIZE_KEY = "chat_memory_retrieve_size";

    @Override
    public void execute(ExecuteCommandEntity requestParameter, ResponseBodyEmitter emitter) throws Exception {
        List<AiAgentClientFlowConfigVO> aiAgentClientList = repository.queryAIAgentClientByAgentId(requestParameter.getAiAgentId());

        String content = "";
        for (AiAgentClientFlowConfigVO configVO : aiAgentClientList) {
            ChatClient chatClient = getChatClientByClientId(configVO.getClientId());

            content = chatClient.prompt(requestParameter.getMessage() + ",")
                    .system(s -> s.param("current_data", LocalDate.now().toString()))
                    .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, requestParameter.getSessionId())
                            .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, requestParameter.getMessage()))
                    .call()
                    .content();
        }
    }

    private ChatClient getChatClientByClientId(String clientId) {
        return getBean(AiAgentEnumVO.AI_CLIENT.getBeanName(clientId));
    }

    private <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }
}
