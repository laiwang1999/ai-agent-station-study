package cn.yang;

import cn.yang.dto.AiAgentResponseDTO;
import cn.yang.dto.ArmoryAgentRequestDTO;
import cn.yang.dto.ArmoryApiRequestDTO;
import cn.yang.dto.AutoAgentRequestDTO;
import cn.yang.response.Response;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import java.util.List;

/**
 * Ai Agent 服务接口
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/8/7 17:52
 */
public interface IAiAgentService {

    ResponseBodyEmitter autoAgent(AutoAgentRequestDTO request, HttpServletResponse response);

    @RequestMapping(value = "armory_agent", method = RequestMethod.POST)
    Response<Boolean> armoryAgent(@RequestBody ArmoryAgentRequestDTO request);

    @RequestMapping(value = "query_available_agents", method = RequestMethod.GET)
    Response<List<AiAgentResponseDTO>> queryAvailableAgents();

    @RequestMapping(value = "armory_api", method = RequestMethod.POST)
    Response<Boolean> armoryApi(@RequestBody ArmoryApiRequestDTO request);
}
