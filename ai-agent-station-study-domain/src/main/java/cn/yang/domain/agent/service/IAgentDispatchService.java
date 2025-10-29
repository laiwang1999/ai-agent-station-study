package cn.yang.domain.agent.service;

import cn.yang.domain.agent.model.entity.ExecuteCommandEntity;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

/**
 * @version v1.0
 * @ClassName: cn.yang.domain.agent.service
 * @Description: TODO
 * @Author: YJ
 */
public interface IAgentDispatchService {
    void dispatch(ExecuteCommandEntity requestParameter, ResponseBodyEmitter emitter) throws Exception;
}
