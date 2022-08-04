package com.study.onsmq.spring;

import com.study.onsmq.ConnectionConfig;
import lombok.Data;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SpringMqProperties
 *
 * @author lry
 */
@Data
@ToString
@ConfigurationProperties(prefix = "rocket-mq")
public class SpringMqProperties implements Serializable {

    /**
     * The mq enable switch
     */
    private boolean enable = false;
    /**
     * The connection list
     */
    private Map<String, ConnectionConfig> connections = new LinkedHashMap<>();

    public ConnectionConfig select(String key) {
        for (Map.Entry<String, ConnectionConfig> entry : connections.entrySet()) {
            if (entry.getKey().equals(key)) {
                return entry.getValue();
            }
        }

        throw new RuntimeException("Not found connection: " + key);
    }

}
