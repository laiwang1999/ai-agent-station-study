package cn.yang.domain.agent.service;

import cn.yang.domain.agent.model.valobj.AiAgentTaskScheduleVO;

import java.util.List;

/**
 * @version v1.0
 * @ClassName: cn.yang.domain.agent.service
 * @Description: TODO
 * @Author: YJ
 */
public interface ITaskService {
    List<AiAgentTaskScheduleVO> queryAllValidTaskSchedule();

    List<Long> queryAllInvalidTaskScheduleIds();
}
