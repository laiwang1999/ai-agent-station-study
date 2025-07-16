package cn.yang.test.spring.ai;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * +  * OpenAI模型功能测试类
 * +  * 测试包括文本生成、图像识别、流式响应、文档向量存储和RAG增强对话等功能
 * +
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class OpenAiTest {

    /**
     * 测试用图片资源
     */
    @Value("classpath:data/dog.png")
    private Resource imageResource;

    /**
     * 测试用文本资源
     */
    @Value("classpath:data/file.txt")
    private Resource textResource;

    /**
     * 文章提示词资源
     */
    @Value("classpath:data/article-prompt-words.txt")
    private Resource articlePromptWordsResource;

    /**
     * OpenAI聊天模型实例
     */
    @Autowired
    private OpenAiChatModel openAiChatModel;

    /**
     * PGVector向量存储实例
     */
    @Autowired
    private PgVectorStore pgVectorStore;

    /**
     * 文本分块器，用于文档处理
     */
    private final TokenTextSplitter tokenTextSplitter = new TokenTextSplitter();

    /**
     * +      * 测试基本文本生成功能
     * +      * 向GPT-4o模型发送简单计算请求并验证响应
     * +
     */
    @Test
    public void test_call() {
        ChatResponse response = openAiChatModel.call(new Prompt(
                "1+1",
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .build()));
        log.info("测试结果(call):{}", JSON.toJSONString(response));
    }

    /**
     * +      * 测试图像识别功能
     * +      * 向GPT-4o模型发送图片并请求描述内容和用途
     * +
     */
    @Test
    public void test_call_images() {
        UserMessage userMessage = UserMessage.builder()
                .text("请描述这张图片的主要内容，并说明图中物品的可能用途。")
                .media(org.springframework.ai.content.Media.builder()
                        .mimeType(MimeType.valueOf(MimeTypeUtils.IMAGE_PNG_VALUE))
                        .data(imageResource)
                        .build())
                .build();

        ChatResponse response = openAiChatModel.call(new Prompt(
                userMessage,
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .build()));

        log.info("测试结果(images):{}", JSON.toJSONString(response));
    }

    /**
     * +      * 测试流式响应功能
     * +      * 使用CountDownLatch等待异步流式响应完成
     * +
     */
    @Test
    public void test_stream() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        Flux<ChatResponse> stream = openAiChatModel.stream(new Prompt(
                "1+1",
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .build()));

        stream.subscribe(
                chatResponse -> {
                    AssistantMessage output = chatResponse.getResult().getOutput();
                    log.info("测试结果(stream): {}", JSON.toJSONString(output));
                },
                Throwable::printStackTrace,
                () -> {
                    countDownLatch.countDown();
                    log.info("测试结果(stream): done!");
                }
        );

        countDownLatch.await();
    }

    /**
     * +      * 测试文档上传到向量存储功能
     * +      * 将文本文件内容分割后存入PGVector向量数据库
     * +
     */
    @Test
    public void upload() {
        // textResource、articlePromptWordsResource
        TikaDocumentReader reader = new TikaDocumentReader(textResource);
        List<Document> documents = reader.get();
        List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

        documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", "知识库名称-v4"));

        pgVectorStore.accept(documentSplitterList);

        log.info("上传完成");
    }

    /**
     * +      * 测试RAG增强对话功能
     * +      * 结合向量存储中的文档上下文回答用户问题
     * +
     */
    @Test
    public void chat() {
        String message = "王大瓜今年几岁";

        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;

        SearchRequest request = SearchRequest.builder()
                .query(message)
                .topK(5)
                .filterExpression("knowledge == '知识库名称-v4'")
                .build();

        List<Document> documents = pgVectorStore.similaritySearch(request);

        String documentsCollectors = null == documents ? "" : documents.stream().map(Document::getText).collect(Collectors.joining());

        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentsCollectors));

        ArrayList<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);

        ChatResponse chatResponse = openAiChatModel.call(new Prompt(
                messages,
                OpenAiChatOptions.builder()
                        .model("gpt-4o")
                        .build()));

        log.info("测试结果:{}", JSON.toJSONString(chatResponse));
    }

}