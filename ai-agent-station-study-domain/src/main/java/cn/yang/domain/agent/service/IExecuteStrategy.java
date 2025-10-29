package cn.yang.domain.agent.service;

import cn.yang.domain.agent.model.entity.ExecuteCommandEntity;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

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
    void execute(ExecuteCommandEntity executeCommandEntity,ResponseBodyEmitter emitter) throws Exception;
}
