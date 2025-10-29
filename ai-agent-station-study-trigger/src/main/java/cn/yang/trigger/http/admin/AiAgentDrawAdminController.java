package cn.yang.trigger.http.admin;

import cn.yang.IAiAgentDrawAdminService;
import cn.yang.dto.AiAgentDrawConfigRequestDTO;
import cn.yang.dto.AiAgentDrawConfigResponseDTO;
import cn.yang.response.Response;
import cn.yang.infrastructure.persistent.dao.*;
import cn.yang.infrastructure.persistent.po.AiAgent;
import cn.yang.infrastructure.persistent.po.AiAgentDrawConfig;
import cn.yang.infrastructure.persistent.po.AiAgentFlowConfig;
import cn.yang.infrastructure.persistent.po.AiClientConfig;
import cn.yang.trigger.http.admin.util.DrawConfigParser;
import cn.yang.types.enums.ResponseCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 拖拉拽
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/9/28 07:35
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/ai-agent-draw")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
public class AiAgentDrawAdminController implements IAiAgentDrawAdminService {

    @Resource
    private IAiAgentDrawConfigDao aiAgentDrawConfigDao;
    @Resource
    private IAiClientConfigDao aiClientConfigDao;
    @Resource
    private IAiAgentDao aiAgentDao;
    @Resource
    private IAiAgentFlowConfigDao aiAgentFlowConfigDao;

    @Override
    @PostMapping("/save-config")
    @Transactional(rollbackFor = Exception.class)
    public Response<String> saveDrawConfig(@RequestBody AiAgentDrawConfigRequestDTO request) {
        try {
            log.info("保存流程图配置请求：{}", request);

            // 生成8位数字的唯一AgentId
            String agentId = String.format("%08d", System.currentTimeMillis() % 100000000L);
            request.setAgentId(agentId);

            // 参数校验
            if (!StringUtils.hasText(request.getConfigName())) {
                return Response.<String>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("配置名称不能为空")
                        .build();
            }

            if (!StringUtils.hasText(request.getConfigData())) {
                return Response.<String>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("配置数据不能为空")
                        .build();
            }

            // 解析JSON中的agent信息
            String[] agentInfo = parseAgentInfoFromJson(request.getConfigData());
            String agentName = agentInfo[0];
            String description = agentInfo[1];
            String channel = agentInfo[2];
            String strategy = agentInfo[3];

            aiAgentDao.insert(AiAgent.builder()
                    .agentId(request.getAgentId())
                    .agentName(agentName)
                    .channel(channel)
                    .strategy(strategy)
                    .status(1)
                    .description(description)
                    .build());

            // 生成配置ID（如果没有提供）
            String configId = request.getConfigId();
            if (!StringUtils.hasText(configId)) {
                configId = UUID.randomUUID().toString().replace("-", "");
            }

            // 检查配置是否已存在
            AiAgentDrawConfig existingConfig = aiAgentDrawConfigDao.queryByConfigId(configId);

            AiAgentDrawConfig drawConfig = new AiAgentDrawConfig();
            BeanUtils.copyProperties(request, drawConfig);
            drawConfig.setConfigId(configId);
            drawConfig.setVersion(1); // 默认版本号
            drawConfig.setStatus(1); // 默认启用状态

            int result;
            if (existingConfig != null) {
                // 更新现有配置
                drawConfig.setId(existingConfig.getId());
                drawConfig.setVersion(existingConfig.getVersion() + 1);
                drawConfig.setUpdateTime(LocalDateTime.now());
                result = aiAgentDrawConfigDao.updateByConfigId(drawConfig);
                log.info("更新流程图配置，configId: {}, result: {}", configId, result);
            } else {
                // 创建新配置
                drawConfig.setCreateTime(LocalDateTime.now());
                drawConfig.setUpdateTime(LocalDateTime.now());
                result = aiAgentDrawConfigDao.insert(drawConfig);
                log.info("创建流程图配置，configId: {}, result: {}", configId, result);
            }

            if (result > 0) {
                // 解析JSON配置数据，生成关系映射并存储到ai_client_config表
                try {
                    List<AiClientConfig> configRelations = DrawConfigParser.parseConfigData(request.getConfigData());
                    if (!configRelations.isEmpty()) {
                        // 先删除该配置相关的旧关系数据（如果是更新操作）
                        if (existingConfig != null) {
                            aiClientConfigDao.deleteBySourceId(configId);
                            log.info("删除配置{}的旧关系数据", configId);
                        }

                        // 批量插入新的关系数据
                        for (AiClientConfig config : configRelations) {
                            // 检查是否已经存在相同的记录
                            List<AiClientConfig> existingConfigs = aiClientConfigDao.queryByConditions(
                                config.getSourceType(), 
                                config.getSourceId(), 
                                config.getTargetType(), 
                                config.getTargetId()
                            );
                            
                            if (existingConfigs.isEmpty()) {
                                // 设置扩展参数，记录来源配置ID
                                config.setExtParam("{\"configId\":\"" + configId + "\"}");
                                aiClientConfigDao.insert(config);
                                log.debug("插入新的配置关系: sourceType={}, sourceId={}, targetType={}, targetId={}", 
                                    config.getSourceType(), config.getSourceId(), config.getTargetType(), config.getTargetId());
                            } else {
                                log.debug("配置关系已存在，跳过插入: sourceType={}, sourceId={}, targetType={}, targetId={}", 
                                    config.getSourceType(), config.getSourceId(), config.getTargetType(), config.getTargetId());
                            }
                        }
                        log.info("成功保存{}条配置关系数据", configRelations.size());
                    }
                } catch (Exception e) {
                    log.error("解析和保存配置关系数据失败，configId: {}", configId, e);
                    // 这里不影响主流程，只记录错误日志
                }

                // 解析JSON配置数据，提取client信息并保存agent-client关系
                try {
                    List<AiAgentFlowConfig> agentFlowConfigs = parseClientInfoFromJson(request.getConfigData(), agentId);
                    if (!agentFlowConfigs.isEmpty()) {
                        // 先删除该agentId相关的旧关系数据（如果是更新操作）
                        if (existingConfig != null) {
                            aiAgentFlowConfigDao.deleteByAgentId(agentId);
                            log.info("删除agentId{}的旧流程配置数据", agentId);
                        }

                        // 批量插入新的agent-client关系数据
                        for (AiAgentFlowConfig flowConfig : agentFlowConfigs) {
                            aiAgentFlowConfigDao.insert(flowConfig);
                        }
                        log.info("成功保存{}条agent-client关系数据", agentFlowConfigs.size());
                    }
                } catch (Exception e) {
                    log.error("解析和保存agent-client关系数据失败，agentId: {}", agentId, e);
                    // 这里不影响主流程，只记录错误日志
                }

                return Response.<String>builder()
                        .code(ResponseCode.SUCCESS.getCode())
                        .info(ResponseCode.SUCCESS.getInfo())
                        .data(configId)
                        .build();
            } else {
                return Response.<String>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("保存失败")
                        .build();
            }

        } catch (Exception e) {
            log.error("保存流程图配置失败", e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("保存失败：" + e.getMessage())
                    .build();
        }
    }

    /**
     * 解析JSON配置数据中的agent信息
     *
     * @param configData JSON配置数据
     * @return agent信息数组 [agentName, channel]
     */
    private String[] parseAgentInfoFromJson(String configData) {
        String[] agentInfo = new String[]{"", "", "", ""}; // 默认值

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(configData);
            JsonNode nodesArray = rootNode.get("nodes");

            if (nodesArray != null && nodesArray.isArray()) {
                for (JsonNode node : nodesArray) {
                    String nodeType = node.get("type").asText();

                    // 只处理type为"agent"的节点
                    if ("agent".equals(nodeType)) {
                        JsonNode dataNode = node.get("data");
                        if (dataNode != null) {
                            JsonNode inputsValuesNode = dataNode.get("inputsValues");
                            if (inputsValuesNode != null) {
                                log.debug("开始解析agent节点的inputsValues: {}", inputsValuesNode.toString());
                                
                                // 提取agent信息
                                String agentName = extractValueFromInputs(inputsValuesNode, "agentName");
                                String description = extractValueFromInputs(inputsValuesNode, "description");
                                String channel = extractValueFromInputs(inputsValuesNode, "channel");
                                String strategy = extractValueFromInputs(inputsValuesNode, "strategy");

                                agentInfo[0] = agentName != null ? agentName : "";
                                agentInfo[1] = description != null ? description : "";
                                agentInfo[2] = channel != null ? channel : "";
                                agentInfo[3] = strategy != null ? strategy : "";

                                log.info("解析到agent信息: agentName={}, description={}, channel={}, strategy={}", 
                                        agentName, description, channel, strategy);
                                break; // 找到第一个agent节点就退出
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析JSON配置数据中的agent信息失败", e);
            // 返回默认值，不抛出异常，避免影响整个保存流程
        }

        return agentInfo;
    }

    /**
     * 解析JSON配置数据中的client信息
     *
     * @param configData JSON配置数据
     * @param agentId    智能体ID
     * @return agent-client关系配置列表
     */
    private List<AiAgentFlowConfig> parseClientInfoFromJson(String configData, String agentId) {
        List<AiAgentFlowConfig> agentFlowConfigs = new ArrayList<>();

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(configData);
            JsonNode nodesArray = rootNode.get("nodes");

            if (nodesArray != null && nodesArray.isArray()) {
                for (JsonNode node : nodesArray) {
                    String nodeType = node.get("type").asText();

                    // 只处理type为"client"的节点
                    if ("client".equals(nodeType)) {
                        JsonNode dataNode = node.get("data");
                        if (dataNode != null) {
                            JsonNode inputsValuesNode = dataNode.get("inputsValues");
                            if (inputsValuesNode != null) {
                                // 提取client信息
                                String clientType = extractValueFromInputs(inputsValuesNode, "clientType");
                                String clientId = extractValueFromInputs(inputsValuesNode, "clientId");
                                String clientName = extractValueFromInputs(inputsValuesNode, "clientName");
                                Integer sequence = extractIntegerValueFromInputs(inputsValuesNode, "sequence");
                                String stepPrompt = extractValueFromInputs(inputsValuesNode, "stepPrompt");

                                // 创建AiAgentFlowConfig对象
                                AiAgentFlowConfig flowConfig = AiAgentFlowConfig.builder()
                                        .agentId(agentId)
                                        .clientId(clientId)
                                        .clientName(clientName)
                                        .clientType(clientType)
                                        .sequence(sequence)
                                        .stepPrompt(stepPrompt)
                                        .createTime(LocalDateTime.now())
                                        .build();

                                agentFlowConfigs.add(flowConfig);
                                log.info("解析到client信息: clientType={}, clientName={}, sequence={}",
                                        clientType, clientName, sequence);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析JSON配置数据失败", e);
            throw new RuntimeException("解析JSON配置数据失败", e);
        }

        return agentFlowConfigs;
    }

    /**
     * 从inputsValues中提取字符串值
     *
     * @param inputsValuesNode inputsValues节点
     * @param fieldName        字段名
     * @return 字段值
     */
    private String extractValueFromInputs(JsonNode inputsValuesNode, String fieldName) {
        JsonNode fieldNode = inputsValuesNode.get(fieldName);
        log.debug("提取字段 '{}': fieldNode={}", fieldName, fieldNode != null ? fieldNode.toString() : "null");
        
        if (fieldNode != null) {
            // 处理数组格式：[{"key": "xxx", "value": "yyy"}] 或 [{"key": "xxx", "value": {"content": "yyy"}}]
            if (fieldNode.isArray() && !fieldNode.isEmpty()) {
                JsonNode firstItem = fieldNode.get(0);
                if (firstItem != null) {
                    JsonNode valueNode = firstItem.get("value");
                    log.debug("字段 '{}' 数组格式，valueNode={}", fieldName, valueNode != null ? valueNode.toString() : "null");
                    
                    if (valueNode != null) {
                        // 如果value是对象，尝试获取content字段
                        if (valueNode.isObject()) {
                            JsonNode contentNode = valueNode.get("content");
                            if (contentNode != null) {
                                String result = contentNode.asText();
                                log.debug("字段 '{}' 从content获取值: {}", fieldName, result);
                                return result;
                            }
                        }
                        // 如果value是字符串，直接返回
                        else if (valueNode.isTextual()) {
                            String result = valueNode.asText();
                            log.debug("字段 '{}' 直接获取字符串值: {}", fieldName, result);
                            return result;
                        }
                        // 如果value是数字，转换为字符串
                        else if (valueNode.isNumber()) {
                            String result = valueNode.asText();
                            log.debug("字段 '{}' 数字转字符串值: {}", fieldName, result);
                            return result;
                        }
                    }
                }
            }
            // 处理直接字符串格式："fieldName": "value"
            else if (fieldNode.isTextual()) {
                String result = fieldNode.asText();
                log.debug("字段 '{}' 直接字符串格式值: {}", fieldName, result);
                return result;
            }
        }
        
        log.debug("字段 '{}' 未找到有效值", fieldName);
        return null;
    }

    /**
     * 从inputsValues中提取整数值
     *
     * @param inputsValuesNode inputsValues节点
     * @param fieldName        字段名
     * @return 字段值
     */
    private Integer extractIntegerValueFromInputs(JsonNode inputsValuesNode, String fieldName) {
        JsonNode fieldNode = inputsValuesNode.get(fieldName);
        if (fieldNode != null) {
            // 处理数组格式：[{"key": "xxx", "value": 123}] 或 [{"key": "xxx", "value": {"content": 123}}]
            if (fieldNode.isArray() && fieldNode.size() > 0) {
                JsonNode firstItem = fieldNode.get(0);
                if (firstItem != null) {
                    JsonNode valueNode = firstItem.get("value");
                    if (valueNode != null) {
                        // 如果value是对象，尝试获取content字段
                        if (valueNode.isObject()) {
                            JsonNode contentNode = valueNode.get("content");
                            if (contentNode != null && contentNode.isNumber()) {
                                return contentNode.asInt();
                            }
                        }
                        // 如果value是数字，直接返回
                        else if (valueNode.isNumber()) {
                            return valueNode.asInt();
                        }
                        // 如果value是字符串，尝试转换为数字
                        else if (valueNode.isTextual()) {
                            try {
                                return Integer.parseInt(valueNode.asText());
                            } catch (NumberFormatException e) {
                                log.warn("无法将字符串 '{}' 转换为整数", valueNode.asText());
                            }
                        }
                    }
                }
            }
            // 处理直接数值格式："fieldName": 123
            else if (fieldNode.isNumber()) {
                return fieldNode.asInt();
            }
        }
        return null;
    }

    @Override
    @GetMapping("/get-config/{configId}")
    public Response<AiAgentDrawConfigResponseDTO> getDrawConfig(@PathVariable String configId) {
        try {
            log.info("获取流程图配置请求，configId: {}", configId);

            if (!StringUtils.hasText(configId)) {
                return Response.<AiAgentDrawConfigResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("配置ID不能为空")
                        .build();
            }

            AiAgentDrawConfig drawConfig = aiAgentDrawConfigDao.queryByConfigId(configId);

            if (drawConfig == null) {
                return Response.<AiAgentDrawConfigResponseDTO>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("配置不存在")
                        .build();
            }

            AiAgentDrawConfigResponseDTO responseDTO = new AiAgentDrawConfigResponseDTO();
            BeanUtils.copyProperties(drawConfig, responseDTO);

            return Response.<AiAgentDrawConfigResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();

        } catch (Exception e) {
            log.error("获取流程图配置失败", e);
            return Response.<AiAgentDrawConfigResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("获取失败：" + e.getMessage())
                    .build();
        }
    }

    @Override
    @DeleteMapping("/delete-config/{configId}")
    public Response<String> deleteDrawConfig(@PathVariable String configId) {
        try {
            log.info("删除流程图配置请求，configId: {}", configId);

            if (!StringUtils.hasText(configId)) {
                return Response.<String>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("配置ID不能为空")
                        .build();
            }

            int result = aiAgentDrawConfigDao.deleteByConfigId(configId);

            if (result > 0) {
                return Response.<String>builder()
                        .code(ResponseCode.SUCCESS.getCode())
                        .info(ResponseCode.SUCCESS.getInfo())
                        .data("删除成功")
                        .build();
            } else {
                return Response.<String>builder()
                        .code(ResponseCode.UN_ERROR.getCode())
                        .info("删除失败，配置不存在")
                        .build();
            }

        } catch (Exception e) {
            log.error("删除流程图配置失败", e);
            return Response.<String>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("删除失败：" + e.getMessage())
                    .build();
        }
    }
}
