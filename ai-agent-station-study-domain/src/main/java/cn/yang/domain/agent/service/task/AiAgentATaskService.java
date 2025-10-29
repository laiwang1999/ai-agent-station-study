package cn.yang.domain.agent.service.task;

import cn.yang.domain.agent.adapter.repository.IAgentRepository;
import cn.yang.domain.agent.model.valobj.AiAgentTaskScheduleVO;
import cn.yang.domain.agent.service.ITaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * @version v1.0
 * @ClassName: cn.yang.domain.agent.service.task
 * @Description: TODO
 * @Author: YJ
 */
@Service
@Slf4j
public class AiAgentATaskService implements ITaskService {
    @Resource
    private IAgentRepository repository;


    @Override
    public List<AiAgentTaskScheduleVO> queryAllValidTaskSchedule() {
        return repository.queryAllValidTaskSchedule();
    }

    @Override
    public List<Long> queryAllInvalidTaskScheduleIds() {
        return repository.queryAllInvalidTaskScheduleIds();
    }
}
