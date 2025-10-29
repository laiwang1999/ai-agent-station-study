package cn.yang.trigger.job;

import cn.yang.domain.agent.model.entity.ExecuteCommandEntity;
import cn.yang.domain.agent.model.valobj.AiAgentTaskScheduleVO;
import cn.yang.domain.agent.service.IAgentDispatchService;
import cn.yang.domain.agent.service.ITaskService;
import cn.yang.types.job.model.TaskScheduleVO;
import cn.yang.types.job.provider.ITaskDataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @version v1.0
 * @ClassName: cn.yang.trigger.job
 * @Description: TODO
 * @Author: YJ
 */
@Slf4j
@Service
public class AgentTaskJob implements ITaskDataProvider {
    @Resource
    private ITaskService taskService;
    @Resource
    private IAgentDispatchService agentDispatchService;


    @Override
    public List<TaskScheduleVO> queryAllValidTaskSchedule() {
        List<AiAgentTaskScheduleVO> aiAgentTaskScheduleVOS = taskService.queryAllValidTaskSchedule();

        List<TaskScheduleVO> result = new ArrayList<>();

        for (AiAgentTaskScheduleVO aiAgentTaskScheduleVO : aiAgentTaskScheduleVOS) {
            TaskScheduleVO taskScheduleVO = TaskScheduleVO.builder()
                    .id(aiAgentTaskScheduleVO.getId())
                    .description(aiAgentTaskScheduleVO.getDescription())
                    .cronExpression(aiAgentTaskScheduleVO.getCronExpression())
                    .taskParam(aiAgentTaskScheduleVO.getTaskParam())
                    .build();
            taskScheduleVO.setTaskLogic(
                    () -> {
                        try {
                            agentDispatchService.dispatch(ExecuteCommandEntity.builder()
                                            .aiAgentId(aiAgentTaskScheduleVO.getAgentId())
                                            .sessionId(String.valueOf(System.nanoTime()))
                                            .maxStep(1)
                                            .build()
                                    , new ResponseBodyEmitter());
                        } catch (Exception e) {
                            log.error("xxxx", e);
                        }
                    }
            );
            result.add(taskScheduleVO);
        }
        return result;
    }

    @Override
    public List<Long> queryAllInvalidTaskScheduleIds() {
        return taskService.queryAllInvalidTaskScheduleIds();
    }
}
