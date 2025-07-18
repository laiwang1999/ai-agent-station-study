package cn.yang.domain.agent.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 装配命令实体 - 用于管理AI代理的武器化指令配置
 * <p>
 * 该实体包含指令类型和关联的客户端/模型ID列表，用于在武器化过程中组合不同AI能力
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * @since 2025/6/27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ArmoryCommandEntity {

    /**
     * 命令类型 - 定义武器化指令的操作类型
     * <p>
     * 示例值：
     * - DEPLOY: 部署指令
     * - CONFIG: 配置指令
     * - EXECUTE: 执行指令
     */
    private String commandType;

    /**
     * 命令索引列表 - 存储关联的客户端/模型ID集合
     * <p>
     * 格式要求：
     * - 每个ID必须符合UUIDv4规范
     * - 列表最大长度不超过20个元素
     * - 空列表表示未绑定具体目标
     */
    private List<String> commandIdList;

}