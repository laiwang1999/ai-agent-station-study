package cn.yang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @version v1.0
 * @ClassName: cn.yang.config
 * @Description: TODO
 * @Author: YJ
 */
@Data
@ConfigurationProperties(prefix = "spring.ai.agent.auto-config")
public class AiAgentAutoConfigProperties {

    private boolean enabled = false;
    private List<String> clientIds;
}
