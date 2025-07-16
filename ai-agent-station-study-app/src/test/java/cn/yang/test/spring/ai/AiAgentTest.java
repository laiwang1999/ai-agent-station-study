package cn.yang.test.spring.ai;

import cn.yang.test.spring.ai.advisors.RagAnswerAdvisor;
import com.alibaba.fastjson.JSON;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;

/**
 * AI智能体测试类，用于测试基于Spring AI的智能代理功能
 * 包括模型初始化、对话交互、工具调用和RAG增强等核心功能测试
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AiAgentTest {

    /**
     * OpenAI聊天模型实例
     */
    private ChatModel chatModel;

    /**
     * 聊天客户端实例，用于构建和发送对话请求
     */
    private ChatClient chatClient;

    /**
     * PGVector向量存储，用于RAG相关功能测试
     */
    @Resource
    private PgVectorStore vectorStore;

    /**
     * 聊天记忆对话ID参数键
     */
    public static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";
    /**
     * 聊天记忆响应大小参数键
     */
    public static final String CHAT_MEMORY_RETRIEVE_SIZE_KEY = "chat_memory_response_size";

    /**
     * 测试初始化方法，在每个测试方法执行前运行
     * 初始化OpenAI API客户端、聊天模型和聊天客户端
     */
    @Before
    public void init() {

        // 创建OpenAI API客户端，配置自定义API地址和密钥
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl("https://ai.nengyongai.cn")
                .apiKey("sk-yrwkgKbEjqfKOw85E0KAoSO9hqCVsY8cvX5U554Ii3SShEPH")
                .completionsPath("v1/chat/completions")
                .embeddingsPath("v1/embeddings")
                .build();

        // 构建OpenAI聊天模型，配置默认参数和工具回调
        chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4.1-mini")
                        .toolCallbacks(new SyncMcpToolCallbackProvider(stdioMcpClient(), sseMcpClient01(), sseMcpClient02()).getToolCallbacks())
                        .build())
                .build();

        // 构建聊天客户端，配置系统提示、记忆管理和RAG增强
        chatClient = ChatClient.builder(chatModel)
                .defaultSystem("""
                        	 你是一个 AI Agent 智能体，可以根据用户输入信息生成文章，并发送到 CSDN 平台以及完成微信公众号消息通知，今天是 {current_date}。
                        
                        	 你擅长使用Planning模式，帮助用户生成质量更高的文章。
                        
                        	 你的规划应该包括以下几个方面：
                        	 1. 分析用户输入的内容，生成技术文章。
                        	 2. 提取，文章标题（需要含带技术点）、文章内容、文章标签（多个用英文逗号隔开）、文章简述（100字）将以上内容发布文章到CSDN
                        	 3. 获取发送到 CSDN 文章的 URL 地址。
                        	 4. 微信公众号消息通知，平台：CSDN、主题：为文章标题、描述：为文章简述、跳转地址：从发布文章到CSDN获取 URL 地址
                        """)
                .defaultToolCallbacks(new SyncMcpToolCallbackProvider(stdioMcpClient(), sseMcpClient01(), sseMcpClient02()).getToolCallbacks())
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(
                                MessageWindowChatMemory.builder()
                                        .maxMessages(100)
                                        .build()
                        ).build(),
                        new RagAnswerAdvisor(vectorStore, SearchRequest.builder()
                                .topK(5)
                                .filterExpression("knowledge == 'article-prompt-words'")
                                .build()),
                        new SimpleLoggerAdvisor())
                .build();
    }

    /**
     * 测试聊天模型流式响应功能
     * 通过CountDownLatch等待异步流处理完成
     */
    @Test
    public void test_chat_model_stream_01() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Prompt prompt = Prompt.builder()
                .messages(new UserMessage(
                        """
                                有哪些工具可以使用
                                """
                ))
                .build();

        // 非流式，chatModel.call(prompt)
        Flux<ChatResponse> stream = chatModel.stream(prompt);

        stream.subscribe(
                chatResponse -> {
                    AssistantMessage output = chatResponse.getResult().getOutput();
                    log.info("测试结果: {}", JSON.toJSONString(output));
                },
                Throwable::printStackTrace,
                () -> {
                    countDownLatch.countDown();
                    System.out.println("Stream completed");
                }
        );

        countDownLatch.await();
    }

    /**
     * 测试聊天模型普通调用功能
     * 同步获取并记录模型响应结果
     */
    @Test
    public void test_chat_model_call() {
        Prompt prompt = Prompt.builder()
                .messages(new UserMessage(
                        """
                                有哪些工具可以使用
                                """
                ))
                .build();

        ChatResponse chatResponse = chatModel.call(prompt);

        log.info("测试结果(call):{}", JSON.toJSONString(chatResponse));
    }

    /**
     * 测试基本对话功能
     * 使用聊天客户端处理用户问题并输出结果
     */
    @Test
    public void test_02() {
        String userInput = "王大瓜今年几岁";
        System.out.println("\n>>> QUESTION: " + userInput);
        System.out.println("\n>>> ASSISTANT: " + chatClient
                .prompt(userInput)
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .call().content());
    }

    /**
     * 测试AI提示词优化和文章生成流程
     * 1. 使用提示词优化专家角色生成高质量文章提示
     * 2. 使用AI Agent智能体根据优化后的提示生成并发布文章
     */
    @Test
    public void test_client03() {
        // 创建提示词优化专家聊天客户端
        ChatClient chatClient01 = ChatClient.builder(chatModel)
                .defaultSystem("""
                        你是一个专业的AI提示词优化专家。请帮我优化以下prompt，并按照以下格式返回：
                        
                        # Role: [角色名称]
                        
                        ## Profile
                        - language: [语言]
                        - description: [详细的角色描述]
                        - background: [角色背景]
                        - personality: [性格特征]
                        - expertise: [专业领域]
                        - target_audience: [目标用户群]
                        
                        ## Skills
                        
                        1. [核心技能类别]
                           - [具体技能]: [简要说明]
                           - [具体技能]: [简要说明]
                           - [具体技能]: [简要说明]
                           - [具体技能]: [简要说明]
                        
                        2. [辅助技能类别]
                           - [具体技能]: [简要说明]
                           - [具体技能]: [简要说明]
                           - [具体技能]: [简要说明]
                           - [具体技能]: [简要说明]
                        
                        ## Rules
                        
                        1. [基本原则]：
                           - [具体规则]: [详细说明]
                           - [具体规则]: [详细说明]
                           - [具体规则]: [详细说明]
                           - [具体规则]: [详细说明]
                        
                        2. [行为准则]：
                           - [具体规则]: [详细说明]
                           - [具体规则]: [详细说明]
                           - [具体规则]: [详细说明]
                           - [具体规则]: [详细说明]
                        
                        3. [限制条件]：
                           - [具体限制]: [详细说明]
                           - [具体限制]: [详细说明]
                           - [具体限制]: [详细说明]
                           - [具体限制]: [详细说明]
                        
                        ## Workflows
                        
                        - 目标: [明确目标]
                        - 步骤 1: [详细说明]
                        - 步骤 2: [详细说明]
                        - 步骤 3: [详细说明]
                        - 预期结果: [说明]
                        
                        
                        ## Initialization
                        作为[角色名称]，你必须遵守上述Rules，按照Workflows执行任务。
                        
                        请基于以上模板，优化并扩展以下prompt，确保内容专业、完整且结构清晰，注意不要携带任何引导词或解释，不要使用代码块包围。
                        """)
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(
                                MessageWindowChatMemory.builder()
                                        .maxMessages(100)
                                        .build()
                        ).build(),
                        new RagAnswerAdvisor(vectorStore, SearchRequest.builder()
                                .topK(5)
                                .filterExpression("knowledge == 'article-prompt-words'")
                                .build())
                )
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4.1")
                        .build())
                .build();

        // 获取优化后的文章提示词
        String content = chatClient01
                .prompt("生成一篇文章")
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, "chatId-101")
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .call().content();

        System.out.println("\n>>> ASSISTANT: " + content);

        // 创建文章生成和发布AI Agent
        ChatClient chatClient02 = ChatClient.builder(chatModel)
                .defaultSystem("""
                        	 你是一个 AI Agent 智能体，可以根据用户输入信息生成文章，并发送到 CSDN 平台以及完成微信公众号消息通知，今天是 {current_date}。
                        
                        	 你擅长使用Planning模式，帮助用户生成质量更高的文章。
                        
                        	 你的规划应该包括以下几个方面：
                        	 1. 分析用户输入的内容，生成技术文章。
                        	 2. 提取，文章标题（需要含带技术点）、文章内容、文章标签（多个用英文逗号隔开）、文章简述（100字）将以上内容发布文章到CSDN
                        	 3. 获取发送到 CSDN 文章的 URL 地址。
                        	 4. 微信公众号消息通知，平台：CSDN、主题：为文章标题、描述：为文章简述、跳转地址：为发布文章到CSDN获取 URL地址 CSDN文章链接 https 开头的地址。
                        """)
//                .defaultTools(new SyncMcpToolCallbackProvider(sseMcpClient01(), sseMcpClient02()))
                .defaultAdvisors(
                        PromptChatMemoryAdvisor.builder(
                                MessageWindowChatMemory.builder()
                                        .maxMessages(100)
                                        .build()
                        ).build(),
                        new SimpleLoggerAdvisor()
                )
                .defaultOptions(OpenAiChatOptions.builder()
                        .model("gpt-4.1")
                        .build())
                .build();

        // 使用优化后的提示词生成并发布文章
        String userInput = "生成一篇文章，要求如下 \r\n" + content;
        System.out.println("\n>>> QUESTION: " + userInput);
        System.out.println("\n>>> ASSISTANT: " + chatClient02
                .prompt(userInput)
                .system(s -> s.param("current_date", LocalDate.now().toString()))
                .advisors(a -> a
                        .param(CHAT_MEMORY_CONVERSATION_ID_KEY, "chatId-101")
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 100))
                .call().content());
    }

    /**
     * 创建基于标准输入输出的MCP客户端
     * 用于本地文件系统操作的工具调用
     *
     * @return McpSyncClient实例
     */
    public McpSyncClient stdioMcpClient() {

        // based on
        // https://github.com/modelcontextprotocol/servers/tree/main/src/filesystem
        var stdioParams = ServerParameters.builder("C:\\Program Files\\nodejs\\npx.cmd")
                .args("-y", "@modelcontextprotocol/server-filesystem", "C:\\Users\\void\\Desktop", "C:\\Users\\void\\Desktop")
                .build();

        var mcpClient = McpClient.sync(new StdioClientTransport(stdioParams))
                .requestTimeout(Duration.ofSeconds(10)).build();

        var init = mcpClient.initialize();

        System.out.println("Stdio MCP Initialized: " + init);

        return mcpClient;

    }

    /**
     * 创建基于SSE的MCP客户端（端口8102）
     * 用于远程工具调用和服务交互
     *
     * @return McpSyncClient实例
     */
    public McpSyncClient sseMcpClient01() {

        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport.builder("http://117.72.216.135:8102").build();

        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofMinutes(180)).build();

        var init = mcpSyncClient.initialize();
        System.out.println("SSE MCP Initialized: " + init);

        return mcpSyncClient;
    }

    /**
     * 创建基于SSE的MCP客户端（端口8101）
     * 用于远程工具调用和服务交互
     *
     * @return McpSyncClient实例
     */
    public McpSyncClient sseMcpClient02() {

        HttpClientSseClientTransport sseClientTransport = HttpClientSseClientTransport.builder("http://117.72.216.135:8101").build();

        McpSyncClient mcpSyncClient = McpClient.sync(sseClientTransport).requestTimeout(Duration.ofMinutes(180)).build();

        var init = mcpSyncClient.initialize();
        System.out.println("SSE MCP Initialized: " + init);

        return mcpSyncClient;
    }

}