package cn.yang.infrastructure.persistent.adapter.repository;

import cn.yang.domain.agent.adapter.repository.IAgentRepository;

/**
 * AI代理仓储实现类
 * <p>
 * 职责说明：
 * 1. 实现领域层定义的仓储接口，完成AI代理相关数据的持久化操作
 * 2. 协调多个DAO组件完成复杂的数据聚合查询
 * 3. 实现数据库PO对象与领域VO对象之间的转换
 * 4. 处理配置数据的级联查询和状态过滤逻辑
 * </p>
 * @author xiaofuge bugstack.cn @小傅哥
 * @since 2025/6/28
 */
import cn.yang.domain.agent.model.valobj.*;
import cn.yang.infrastructure.persistent.dao.*;
import cn.yang.infrastructure.persistent.po.*;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static cn.yang.domain.agent.model.valobj.AiAgentEnumVO.AI_CLIENT;
import static cn.yang.domain.agent.model.valobj.AiAgentEnumVO.AI_CLIENT_MODEL;

/**
 * AiAgent 仓储服务
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/6/28 18:09
 */
@Slf4j
@Repository
public class AgentRepository implements IAgentRepository {

    // region DAO组件注入

    /** 代理主体DAO，负责ai_agent表的基础CRUD操作 */
    @Resource
    private IAiAgentDao aiAgentDao;

    /** 代理流程配置DAO，管理代理工作流的节点配置 */
    @Resource
    private IAiAgentFlowConfigDao aiAgentFlowConfigDao;

    /** 代理任务调度DAO，处理定时任务相关的持久化操作 */
    @Resource
    private IAiAgentTaskScheduleDao aiAgentTaskScheduleDao;

    /** 客户端顾问配置DAO，维护advisor类型的配置信息 */
    @Resource
    private IAiClientAdvisorDao aiClientAdvisorDao;

    /** API配置DAO，管理第三方API的接入凭证和端点信息 */
    @Resource
    private IAiClientApiDao aiClientApiDao;

    /** 客户端配置关系DAO，处理client与其他实体的关联关系 */
    @Resource
    private IAiClientConfigDao aiClientConfigDao;

    @Resource
    private IAiClientDao aiClientDao;

    @Resource
    private IAiClientModelDao aiClientModelDao;

    @Resource
    private IAiClientRagOrderDao aiClientRagOrderDao;

    @Resource
    private IAiClientSystemPromptDao aiClientSystemPromptDao;

    @Resource
    private IAiClientToolMcpDao aiClientToolMcpDao;

    @Override
    /**
     * 根据客户端ID列表查询关联的API配置
     * 
     * @param clientIdList 客户端ID列表，支持批量查询
     * @return 有效的API配置值对象列表，包含：
     *         - API基础路径
     *         - 认证密钥
     *         - 补全接口路径
     *         - 嵌入接口路径
     * @implNote 实现逻辑：
     * 1. 状态过滤：只查询状态为启用的配置(status=1)
     * 2. 级联查询：client→model→api三级关联查询
     * 3. 去重处理：确保相同API配置只返回一次
     */
    public List<AiClientApiVO> queryAiClientApiVOListByClientIds(List<String> clientIdList) {
        if (clientIdList == null || clientIdList.isEmpty()) {
            return List.of();
        }

        List<AiClientApiVO> result = new ArrayList<>();

        for (String clientId : clientIdList) {
            // 1. 通过clientId查询关联的modelId
            List<AiClientConfig> configs = aiClientConfigDao.queryBySourceTypeAndId(AI_CLIENT.getCode(), clientId);

            for (AiClientConfig config : configs) {
                if (AI_CLIENT_MODEL.getCode().equals(config.getTargetType()) && config.getStatus() == 1) {
                    String modelId = config.getTargetId();

                    // 2. 通过modelId查询模型配置，获取apiId
                    AiClientModel model = aiClientModelDao.queryByModelId(modelId);
                    if (model != null && model.getStatus() == 1) {
                        String apiId = model.getApiId();

                        // 3. 通过apiId查询API配置信息
                        AiClientApi apiConfig = aiClientApiDao.queryByApiId(apiId);
                        if (apiConfig != null && apiConfig.getStatus() == 1) {
                            // 4. 转换为VO对象
                            AiClientApiVO apiVO = AiClientApiVO.builder()
                                    .apiId(apiConfig.getApiId())
                                    .baseUrl(apiConfig.getBaseUrl())
                                    .apiKey(apiConfig.getApiKey())
                                    .completionsPath(apiConfig.getCompletionsPath())
                                    .embeddingsPath(apiConfig.getEmbeddingsPath())
                                    .build();

                            // 避免重复添加相同的API配置
                            if (result.stream().noneMatch(vo -> vo.getApiId().equals(apiVO.getApiId()))) {
                                result.add(apiVO);
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    /**
     * 获取客户端关联的模型配置
     * 
     * @param clientIdList 客户端ID列表
     * @return 模型配置值对象列表，包含：
     *         - 模型ID
     *         - 关联的API配置ID
     *         - 模型类型
     * @implSpec 该方法会跳过以下情况：
     *         1. 无效的客户端ID
     *         2. 非模型类型的配置项
     *         3. 已禁用(status≠1)的配置
     */
    public List<AiClientModelVO> AiClientModelVOByClientIds(List<String> clientIdList) {
        if (clientIdList == null || clientIdList.isEmpty()) {
            return List.of();
        }

        List<AiClientModelVO> result = new ArrayList<>();

        for (String clientId : clientIdList) {
            // 1. 通过clientId查询关联的modelId
            List<AiClientConfig> configs = aiClientConfigDao.queryBySourceTypeAndId(AI_CLIENT.getCode(), clientId);

            for (AiClientConfig config : configs) {
                if (AI_CLIENT_MODEL.getCode().equals(config.getTargetType()) && config.getStatus() == 1) {
                    String modelId = config.getTargetId();

                    // 2. 通过modelId查询模型配置
                    AiClientModel model = aiClientModelDao.queryByModelId(modelId);
                    if (model != null && model.getStatus() == 1) {
                        // 3. 转换为VO对象
                        AiClientModelVO modelVO = AiClientModelVO.builder()
                                .modelId(model.getModelId())
                                .apiId(model.getApiId())
                                .modelName(model.getModelName())
                                .modelType(model.getModelType())
                                .build();

                        // 避免重复添加相同的模型配置
                        if (result.stream().noneMatch(vo -> vo.getModelId().equals(modelVO.getModelId()))) {
                            result.add(modelVO);
                        }
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<AiClientToolMcpVO> AiClientToolMcpVOByClientIds(List<String> clientIdList) {
        if (clientIdList == null || clientIdList.isEmpty()) {
            return List.of();
        }

        List<AiClientToolMcpVO> result = new ArrayList<>();
        Set<String> processedMcpIds = new HashSet<>();

        for (String clientId : clientIdList) {
            // 1. 通过clientId查询关联的tool_mcp配置
            List<AiClientConfig> configs = aiClientConfigDao.queryBySourceTypeAndId(AI_CLIENT.getCode(), clientId);

            for (AiClientConfig config : configs) {
                if ("tool_mcp".equals(config.getTargetType()) && config.getStatus() == 1) {
                    String mcpId = config.getTargetId();

                    // 避免重复处理相同的mcpId
                    if (processedMcpIds.contains(mcpId)) {
                        continue;
                    }
                    processedMcpIds.add(mcpId);

                    // 2. 通过mcpId查询ai_client_tool_mcp表获取MCP工具配置
                    AiClientToolMcp toolMcp = aiClientToolMcpDao.queryByMcpId(mcpId);
                    if (toolMcp != null && toolMcp.getStatus() == 1) {
                        // 3. 转换为VO对象
                        AiClientToolMcpVO mcpVO = AiClientToolMcpVO.builder()
                                .mcpId(toolMcp.getMcpId())
                                .mcpName(toolMcp.getMcpName())
                                .transportType(toolMcp.getTransportType())
                                .transportConfig(toolMcp.getTransportConfig())
                                .requestTimeout(toolMcp.getRequestTimeout())
                                .build();

                        result.add(mcpVO);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<AiClientSystemPromptVO> AiClientSystemPromptVOByClientIds(List<String> clientIdList) {
        if (clientIdList == null || clientIdList.isEmpty()) {
            return List.of();
        }

        List<AiClientSystemPromptVO> result = new ArrayList<>();
        Set<String> processedPromptIds = new HashSet<>();

        for (String clientId : clientIdList) {
            // 1. 通过clientId查询关联的prompt配置
            List<AiClientConfig> configs = aiClientConfigDao.queryBySourceTypeAndId(AI_CLIENT.getCode(), clientId);

            for (AiClientConfig config : configs) {
                if ("prompt".equals(config.getTargetType()) && config.getStatus() == 1) {
                    String promptId = config.getTargetId();

                    // 避免重复处理相同的promptId
                    if (processedPromptIds.contains(promptId)) {
                        continue;
                    }
                    processedPromptIds.add(promptId);

                    // 2. 通过promptId查询ai_client_system_prompt表获取系统提示词配置
                    AiClientSystemPrompt systemPrompt = aiClientSystemPromptDao.queryByPromptId(promptId);
                    if (systemPrompt != null && systemPrompt.getStatus() == 1) {
                        // 3. 转换为VO对象
                        AiClientSystemPromptVO promptVO = AiClientSystemPromptVO.builder()
                                .promptId(systemPrompt.getPromptId())
                                .promptName(systemPrompt.getPromptName())
                                .promptContent(systemPrompt.getPromptContent())
                                .description(systemPrompt.getDescription())
                                .build();

                        result.add(promptVO);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 根据客户端ID列表查询关联的智能体顾问配置
     *
     * @param clientIdList 客户端ID列表
     * @return 顾问配置值对象列表，包含：
     *         1. 对话记忆配置（ChatMemory）
     *         2. RAG回答配置（RagAnswer）
     *         3. 基础顾问信息
     */
    @Override
    public List<AiClientAdvisorVO> AiClientAdvisorVOByClientIds(List<String> clientIdList) {
        // 参数校验
        if (clientIdList == null || clientIdList.isEmpty()) {
            return List.of();
        }

        List<AiClientAdvisorVO> result = new ArrayList<>();
        Set<String> processedAdvisorIds = new HashSet<>(); // 已处理顾问ID集合，用于去重

        // 遍历客户端ID列表
        for (String clientId : clientIdList) {
            // 1. 查询客户端关联的配置项（配置类型为advisor）
            List<AiClientConfig> configs = aiClientConfigDao.queryBySourceTypeAndId("client", clientId);

            for (AiClientConfig config : configs) {
                // 过滤无效配置：状态非启用 或 配置类型不匹配
                if (config.getStatus() != 1 || !"advisor".equals(config.getTargetType())) {
                    continue;
                }

                String advisorId = config.getTargetId();
                // 跳过已处理的顾问配置
                if (processedAdvisorIds.contains(advisorId)) {
                    continue;
                }
                processedAdvisorIds.add(advisorId);

                // 2. 查询顾问主体信息
                AiClientAdvisor aiClientAdvisor = aiClientAdvisorDao.queryByAdvisorId(advisorId);
                // 过滤无效的顾问配置
                if (aiClientAdvisor == null || aiClientAdvisor.getStatus() != 1) {
                    continue;
                }

                /*
                 * 3. 解析扩展参数配置逻辑
                 * ----------------------------------------
                 * 根据不同的顾问类型解析对应的配置项：
                 * - ChatMemory: 对话记忆配置
                 * - RagAnswer: RAG回答配置
                 * 使用FastJSON进行JSON反序列化，异常时保持null值
                 */
                AiClientAdvisorVO.ChatMemory chatMemory = null;
                AiClientAdvisorVO.RagAnswer ragAnswer = null;

                String extParam = aiClientAdvisor.getExtParam();
                if (extParam != null && !extParam.trim().isEmpty()) {
                    try {
                        if ("ChatMemory".equals(aiClientAdvisor.getAdvisorType())) {
                            // 反序列化对话记忆配置
                            chatMemory = JSON.parseObject(extParam, AiClientAdvisorVO.ChatMemory.class);
                        } else if ("RagAnswer".equals(aiClientAdvisor.getAdvisorType())) {
                            // 反序列化RAG回答配置
                            ragAnswer = JSON.parseObject(extParam, AiClientAdvisorVO.RagAnswer.class);
                        }
                    } catch (Exception e) {
                        // JSON解析异常处理：记录日志并保持配置为null
                        log.error("顾问配置解析失败 advisorId:{} type:{}", advisorId, aiClientAdvisor.getAdvisorType(), e);
                    }
                }

                // 4. 构建值对象
                AiClientAdvisorVO advisorVO = AiClientAdvisorVO.builder()
                        .advisorId(aiClientAdvisor.getAdvisorId())
                        .advisorName(aiClientAdvisor.getAdvisorName()) // 顾问名称
                        .advisorType(aiClientAdvisor.getAdvisorType()) // 顾问类型
                        .orderNum(aiClientAdvisor.getOrderNum())        // 执行顺序
                        .chatMemory(chatMemory)                         // 对话记忆配置
                        .ragAnswer(ragAnswer)                           // RAG回答配置
                        .build();

                result.add(advisorVO);
            }
        }

        return result;
    }

    @Override
    public List<AiClientVO> AiClientVOByClientIds(List<String> clientIdList) {
        if (clientIdList == null || clientIdList.isEmpty()) {
            return List.of();
        }

        List<AiClientVO> result = new ArrayList<>();
        Set<String> processedClientIds = new HashSet<>();

        for (String clientId : clientIdList) {
            if (processedClientIds.contains(clientId)) {
                continue;
            }
            processedClientIds.add(clientId);

            // 1. 查询客户端基本信息
            AiClient aiClient = aiClientDao.queryByClientId(clientId);
            if (aiClient == null || aiClient.getStatus() != 1) {
                continue;
            }

            // 2. 查询客户端相关配置
            List<AiClientConfig> configs = aiClientConfigDao.queryBySourceTypeAndId("client", clientId);

            String modelId = null;
            List<String> promptIdList = new ArrayList<>();
            List<String> mcpIdList = new ArrayList<>();
            List<String> advisorIdList = new ArrayList<>();

            for (AiClientConfig config : configs) {
                if (config.getStatus() != 1) {
                    continue;
                }

                switch (config.getTargetType()) {
                    case "model":
                        modelId = config.getTargetId();
                        break;
                    case "prompt":
                        promptIdList.add(config.getTargetId());
                        break;
                    case "tool_mcp":
                        mcpIdList.add(config.getTargetId());
                        break;
                    case "advisor":
                        advisorIdList.add(config.getTargetId());
                        break;
                }
            }

            // 3. 构建AiClientVO对象
            AiClientVO aiClientVO = AiClientVO.builder()
                    .clientId(aiClient.getClientId())
                    .clientName(aiClient.getClientName())
                    .description(aiClient.getDescription())
                    .modelId(modelId)
                    .promptIdList(promptIdList)
                    .mcpIdList(mcpIdList)
                    .advisorIdList(advisorIdList)
                    .build();

            result.add(aiClientVO);
        }

        return result;
    }

    @Override
    public List<AiClientApiVO> queryAiClientApiVOListByModelIds(List<String> modelIdList) {
        if (modelIdList == null || modelIdList.isEmpty()) {
            return List.of();
        }

        List<AiClientApiVO> result = new ArrayList<>();

        for (String modelId : modelIdList) {
            // 1. 通过modelId查询模型配置，获取apiId
            AiClientModel model = aiClientModelDao.queryByModelId(modelId);
            if (model != null && model.getStatus() == 1) {
                String apiId = model.getApiId();

                // 2. 通过apiId查询API配置信息
                AiClientApi apiConfig = aiClientApiDao.queryByApiId(apiId);
                if (apiConfig != null && apiConfig.getStatus() == 1) {
                    // 3. 转换为VO对象
                    AiClientApiVO apiVO = AiClientApiVO.builder()
                            .apiId(apiConfig.getApiId())
                            .baseUrl(apiConfig.getBaseUrl())
                            .apiKey(apiConfig.getApiKey())
                            .completionsPath(apiConfig.getCompletionsPath())
                            .embeddingsPath(apiConfig.getEmbeddingsPath())
                            .build();

                    // 避免重复添加相同的API配置
                    if (result.stream().noneMatch(vo -> vo.getApiId().equals(apiVO.getApiId()))) {
                        result.add(apiVO);
                    }
                }
            }
        }

        return result;
    }

    @Override
    public List<AiClientModelVO> AiClientModelVOByModelIds(List<String> modelIdList) {
        if (modelIdList == null || modelIdList.isEmpty()) {
            return List.of();
        }

        List<AiClientModelVO> result = new ArrayList<>();

        for (String modelId : modelIdList) {
            // 通过modelId查询模型配置
            AiClientModel model = aiClientModelDao.queryByModelId(modelId);
            if (model != null && model.getStatus() == 1) {
                // 转换为VO对象
                AiClientModelVO modelVO = AiClientModelVO.builder()
                        .modelId(model.getModelId())
                        .apiId(model.getApiId())
                        .modelName(model.getModelName())
                        .modelType(model.getModelType())
                        .build();

                // 避免重复添加相同的模型配置
                if (result.stream().noneMatch(vo -> vo.getModelId().equals(modelVO.getModelId()))) {
                    result.add(modelVO);
                }
            }
        }

        return result;
    }

}