package com.study.mq;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ConnectionConfig
 *
 * @author lry
 */
@Data
@ToString
public class ConnectionConfig implements Serializable {

    public static final String DEFAULT_CONNECT_GROUP = "default";
    public static final String LOCAL_CONNECT_GROUP = "local";
    public static final String TENANT_RULE = "%s_%s";

    public static final String TRACE_KEY = "Trace-Id";
    public static final String TITLE_KEY = "Title";
    public static final String TRACE_MSG_KEY = "Trace";
    public static final String TENANT_ID_KEY = "Tenant-Id";
    public static final String TENANT_NAME_KEY = "Tenant-Name";

    public static final String DELIVER_TIME_KEY = "Deliver-Time";

    public static final String ONS_TYPE = "ons";
    public static final String ONS_ORDERLY_TYPE = "ons-orderly";
    public static final String ROCKET_TYPE = "rocket";

    /**
     * The mq enable switch
     */
    private boolean enable = true;
    /**
     * Mq type({@link ConnectionConfig#ONS_TYPE} or {@link ConnectionConfig#ROCKET_TYPE})
     */
    private String type = ONS_TYPE;
    /**
     * The mq filter switch
     */
    private boolean filter = false;
    /**
     * The mq message log
     */
    private boolean debug = false;
    /**
     * Mq group id
     */
    private String group;
    /**
     * mq connect address
     */
    private String address;
    /**
     * access key
     */
    private String accessKey;
    /**
     * secret key
     */
    private String secretKey;
    /**
     * security token
     */
    private String securityToken;
    /**
     * Consumer tenant
     */
    private String tenant;
    /**
     * CLUSTERING,BROADCASTING
     */
    private String model = "CLUSTERING";
    /**
     * Max  reconsume times, 0 means no retry, -1 means unlimited retry,other means max retry times
     */
    private Integer maxReconsumeTimes = 0;
    /**
     * Maximum allowed message size in bytes.(10M)
     */
    private int maxMessageSize = 1024 * 1024 * 10;
    /**
     * Consumer listener package
     */
    private List<String> listenerPackages = new ArrayList<>();
    /**
     * commons others parameters
     */
    private Map<String, Object> parameters = new LinkedHashMap<>();

}