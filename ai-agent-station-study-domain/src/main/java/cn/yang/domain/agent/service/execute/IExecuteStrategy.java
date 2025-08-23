package cn.yang.domain.agent.service.execute;

import cn.yang.domain.agent.model.entity.ExecuteCommandEntity;

/**
 * @version v1.0
 * @ClassName: cn.yang.domain.agent.service.execute
 * @Description: TODO
 * @Author: YJ
 */
public interface IExecuteStrategy {
    /**
     * 执行策略
     */
    void execute(ExecuteCommandEntity executeCommandEntity) throws Exception;
}
