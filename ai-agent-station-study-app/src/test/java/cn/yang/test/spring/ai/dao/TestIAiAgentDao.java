package cn.yang.test.spring.ai.dao;

import cn.yang.infrastructure.persistent.dao.IAiAgentDao;
import cn.yang.infrastructure.persistent.po.AiAgent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TestIAiAgentDao {

    @Autowired
    private IAiAgentDao aiAgentDao;

    @Test
    public void testInsert() {
        AiAgent aiAgent = new AiAgent();
        aiAgent.setAgentId("agent_test_001");
        aiAgent.setAgentName("测试智能体");
        aiAgent.setDescription("这是一个测试智能体");
        aiAgent.setChannel("agent");
        aiAgent.setStatus(1);

        int result = aiAgentDao.insert(aiAgent);
        assertEquals(1, result);
    }

    @Test
    public void testUpdateById() {
        // 先插入一条数据
        AiAgent aiAgent = new AiAgent();
        aiAgent.setAgentId("agent_test_002");
        aiAgent.setAgentName("测试智能体_更新前");
        aiAgent.setDescription("这是更新前的智能体");
        aiAgent.setChannel("agent");
        aiAgent.setStatus(1);
        aiAgentDao.insert(aiAgent);

        // 查询插入的数据
        List<AiAgent> allAgents = aiAgentDao.queryAll();
        AiAgent insertedAgent = allAgents.stream()
                .filter(a -> "test_agent_id_for_update".equals(a.getAgentId()))
                .findFirst()
                .orElseThrow();

        // 更新数据
        insertedAgent.setAgentName("测试智能体_更新后");
        int result = aiAgentDao.updateById(insertedAgent);
        assertEquals(1, result);

        // 验证更新结果
        AiAgent updatedAgent = aiAgentDao.queryById(insertedAgent.getId());
        assertEquals("测试智能体_更新后", updatedAgent.getAgentName());
    }

    @Test
    public void testUpdateByAgentId() {
        AiAgent aiAgent = new AiAgent();
        aiAgent.setAgentId("agent_test_003");
        aiAgent.setAgentName("测试智能体_更新前");
        aiAgent.setDescription("这是更新前的智能体");
        aiAgent.setChannel("agent");
        aiAgent.setStatus(1);
        aiAgentDao.insert(aiAgent);

        // 更新数据
        aiAgent.setAgentName("测试智能体_更新后");
        int result = aiAgentDao.updateByAgentId(aiAgent);
        assertEquals(1, result);

        // 验证更新结果
        AiAgent updatedAgent = aiAgentDao.queryByAgentId("test_agent_id_for_update_by_agent_id");
        assertEquals("测试智能体_更新后", updatedAgent.getAgentName());
    }

    @Test
    public void testDeleteById() {
        // 先插入一条数据
        AiAgent aiAgent = new AiAgent();
        aiAgent.setAgentId("agent_test_004");
        aiAgent.setAgentName("测试智能体_删除");
        aiAgent.setDescription("这是要删除的智能体");
        aiAgent.setChannel("agent");
        aiAgent.setStatus(1);
        aiAgentDao.insert(aiAgent);

        // 查询插入的数据
        List<AiAgent> allAgents = aiAgentDao.queryAll();
        AiAgent insertedAgent = allAgents.stream()
                .filter(a -> "test_agent_id_for_delete".equals(a.getAgentId()))
                .findFirst()
                .orElseThrow();

        // 删除数据
        int result = aiAgentDao.deleteById(insertedAgent.getId());
        assertEquals(1, result);

        // 验证删除结果
        AiAgent deletedAgent = aiAgentDao.queryById(insertedAgent.getId());
        assertNull(deletedAgent);
    }

    @Test
    public void testDeleteByAgentId() {
        // 先插入一条数据
        AiAgent aiAgent = new AiAgent();
        aiAgent.setAgentId("agent_test_005");
        aiAgent.setAgentName("测试智能体_按agentId删除");
        aiAgent.setDescription("这是按agentId删除的智能体");
        aiAgent.setChannel("agent");
        aiAgent.setStatus(1);
        aiAgentDao.insert(aiAgent);

        // 删除数据
        int result = aiAgentDao.deleteByAgentId("test_agent_id_for_delete_by_agent_id");
        assertEquals(1, result);

        // 验证删除结果
        AiAgent deletedAgent = aiAgentDao.queryByAgentId("test_agent_id_for_delete_by_agent_id");
        assertNull(deletedAgent);
    }

    @Test
    public void testQueryById() {
        // 先插入一条数据
        AiAgent aiAgent = new AiAgent();
        aiAgent.setAgentId("agent_test_006");
        aiAgent.setAgentName("测试智能体_查询");
        aiAgent.setDescription("这是要查询的智能体");
        aiAgent.setChannel("agent");
        aiAgent.setStatus(1);
        aiAgentDao.insert(aiAgent);

        // 查询插入的数据
        List<AiAgent> allAgents = aiAgentDao.queryAll();
        AiAgent insertedAgent = allAgents.stream()
                .filter(a -> "test_agent_id_for_query".equals(a.getAgentId()))
                .findFirst()
                .orElseThrow();

        // 测试查询
        AiAgent queriedAgent = aiAgentDao.queryById(insertedAgent.getId());
        assertNotNull(queriedAgent);
        assertEquals("test_agent_id_for_query", queriedAgent.getAgentId());
    }

    @Test
    public void testQueryByAgentId() {
        // 先插入一条数据
        AiAgent aiAgent = new AiAgent();
        aiAgent.setAgentId("agent_test_007");
        aiAgent.setAgentName("测试智能体_按agentId查询");
        aiAgent.setDescription("这是按agentId查询的智能体");
        aiAgent.setChannel("agent");
        aiAgent.setStatus(1);
        aiAgentDao.insert(aiAgent);

        // 测试查询
        AiAgent queriedAgent = aiAgentDao.queryByAgentId("test_agent_id_for_query_by_agent_id");
        assertNotNull(queriedAgent);
        assertEquals("test_agent_id_for_query_by_agent_id", queriedAgent.getAgentId());
    }

    @Test
    public void testQueryEnabledAgents() {
        // 先插入启用和禁用的数据
        AiAgent enabledAgent = new AiAgent();
        enabledAgent.setAgentId("agent_test_008");
        enabledAgent.setAgentName("启用的智能体");
        enabledAgent.setDescription("这是启用的智能体");
        enabledAgent.setChannel("agent");
        enabledAgent.setStatus(1);
        aiAgentDao.insert(enabledAgent);

        AiAgent disabledAgent = new AiAgent();
        disabledAgent.setAgentId("agent_test_009");
        disabledAgent.setAgentName("禁用的智能体");
        disabledAgent.setDescription("这是禁用的智能体");
        disabledAgent.setChannel("agent");
        disabledAgent.setStatus(0);
        aiAgentDao.insert(disabledAgent);

        // 测试查询
        List<AiAgent> enabledAgents = aiAgentDao.queryEnabledAgents();
        assertTrue(enabledAgents.stream().anyMatch(a -> "enabled_agent_id".equals(a.getAgentId())));
        assertFalse(enabledAgents.stream().anyMatch(a -> "disabled_agent_id".equals(a.getAgentId())));
    }

    @Test
    public void testQueryByChannel() {
        // 先插入不同渠道的数据
        AiAgent channel1Agent = new AiAgent();
        channel1Agent.setAgentId("agent_test_010");
        channel1Agent.setAgentName("测试智能体_按渠道查询");
        channel1Agent.setDescription("这是按渠道查询的智能体");
        channel1Agent.setChannel("test_channel");
        channel1Agent.setStatus(1);
        aiAgentDao.insert(channel1Agent);

        AiAgent channel2Agent = new AiAgent();
        channel2Agent.setAgentId("agent_test_011");
        channel2Agent.setAgentName("测试智能体_按渠道查询");
        channel2Agent.setDescription("这是按渠道查询的智能体");
        channel2Agent.setChannel("test_channel");
        channel2Agent.setStatus(1);
        aiAgentDao.insert(channel2Agent);

        // 测试查询
        List<AiAgent> channel1Agents = aiAgentDao.queryByChannel("channel1");
        assertTrue(channel1Agents.stream().anyMatch(a -> "channel1_agent_id".equals(a.getAgentId())));
        assertFalse(channel1Agents.stream().anyMatch(a -> "channel2_agent_id".equals(a.getAgentId())));
    }

    @Test
    public void testQueryAll() {
        // 先清空数据（如果需要）
        // 插入测试数据
        AiAgent agent1 = new AiAgent();
        agent1.setAgentId("agent_test_012");
        agent1.setAgentName("所有智能体1");
        agent1.setDescription("这是所有智能体1");
        agent1.setChannel("agent");
        agent1.setStatus(1);
        aiAgentDao.insert(agent1);

        AiAgent agent2 = new AiAgent();
        agent2.setAgentId("agent_test_013");
        agent2.setAgentName("所有智能体2");
        agent2.setDescription("这是所有智能体2");
        agent2.setChannel("agent");
        agent2.setStatus(1);
        aiAgentDao.insert(agent2);

        // 测试查询
        List<AiAgent> allAgents = aiAgentDao.queryAll();
        assertTrue(allAgents.size() >= 2);
        assertTrue(allAgents.stream().anyMatch(a -> "all_agent_id_1".equals(a.getAgentId())));
        assertTrue(allAgents.stream().anyMatch(a -> "all_agent_id_2".equals(a.getAgentId())));
    }
}