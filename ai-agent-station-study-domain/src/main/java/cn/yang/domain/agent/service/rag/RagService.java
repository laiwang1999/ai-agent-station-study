package cn.yang.domain.agent.service.rag;

import cn.yang.domain.agent.adapter.repository.IAgentRepository;
import cn.yang.domain.agent.model.valobj.AiRagOrderVO;
import cn.yang.domain.agent.service.IRagService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识库服务
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/10/4 09:12
 */
@Slf4j
@Service
public class RagService implements IRagService {

    @Resource
    private TokenTextSplitter tokenTextSplitter;

    @Resource
    private PgVectorStore vectorStore;

    @Resource
    private IAgentRepository repository;

    @Override
    public void storeRagFile(String name, String tag, List<MultipartFile> files) {
        for (MultipartFile file : files) {
            // file.getResource()：将 MultipartFile 转换为 Spring 的 Resource 对象（表示文件资源）。
            // TikaDocumentReader：一个封装了 Apache Tika 的工具类，用于从各种格式（如 PDF、Word、PPT 等）中提取纯文本内容。
            TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
            List<Document> documentList = tokenTextSplitter.apply(documentReader.get());

            // 为每个文档添加知识库标签
            documentList.forEach(doc -> doc.getMetadata().put("knowledge", tag));

            // 存储知识库文件
            vectorStore.accept(documentList);

            // 存储到数据库
            AiRagOrderVO aiRagOrderVO = new AiRagOrderVO();
            aiRagOrderVO.setRagName(name);
            aiRagOrderVO.setKnowledgeTag(tag);
            repository.createTagOrder(aiRagOrderVO);
        }
    }

}
