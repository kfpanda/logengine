package com.kfpanda.logengine.util;

import com.kfpanda.mongodb.MongoDBDaoImpl;
import com.kfpanda.redis.RedisUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.*;
import org.apache.logging.log4j.core.layout.PatternLayout;
import redis.clients.jedis.Jedis;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Created by kfpanda on 16-4-15.
 */
@Plugin(
        name = "redisAppender",
        category = "Core",
        elementType = "appender",
        printObject = true
)
public class RedisAppender extends AbstractAppender {
    private static Logger logger = LogManager.getLogger(RedisAppender.class);
    private ThreadLocal<String> uid = new ThreadLocal<String>();
    public static ThreadLocal<String> threadUUID = new ThreadLocal<String>() {
        public String initialValue() {
            return idFormat.format(new Date()) + RandomStringUtils.random(6);
        }
    };
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock;
    private final MongoDBDaoImpl mongoDBDao = MongoDBDaoImpl.getMongoDBDaoImplInstance();

    protected RedisAppender(String name, Filter filter, Layout<? extends Serializable> layout) {
        super(name, filter, layout, true);
        this.readLock = this.rwLock.readLock();
    }

    public ThreadLocal<String> getUid(){
        System.out.println("-----");
        String ci = uid.get();
        if(ci==null){
            ci = UUID.randomUUID().toString();
            System.out.println(ci);
            uid.set(ci);
        }
        return uid;
    }
    private static SimpleDateFormat idFormat = new SimpleDateFormat("yyyMMddHHmmss");

    public static void setUUID(String uid){
        threadUUID.set(uid);
    }
    /**
     * 获取uuid
     * @param strings 生成参数
     * @return
     * @author gmo_ye
     * @date 2016-5-9 上午9:23:57
     */
    public static String getThreadUUID(String...strings) {
//        MD5Util.check(f, md5)
        // TODO 生成uuid规则待定
        if(StringUtils.isNotBlank(threadUUID.get())) {
            return threadUUID.get();
        }
        if(null == strings || strings.length<1) {
            String reqId = idFormat.format(new Date()) + RandomStringUtils.random(8);
            threadUUID.set("BASIC_LOGID_" + reqId);
            return threadUUID.get();
        }
        String strs = StringUtils.EMPTY;
        for(String str : strings) {
            strs+=str;
        }

        return threadUUID.get();
    }

    private Jedis jedis = RedisUtil.getResource();
    /**
     * 执行数据传输
     */
    @Override
    public synchronized void append(final LogEvent event) {
        String traceId = threadUUID.get();
        byte[] uuId = traceId.getBytes();

        final byte[] bytes = getLayout().toByteArray(event);
        final byte[] data = new byte[uuId.length+bytes.length];
        for(int i=0;i<uuId.length;i++) {
            data[i] = uuId[i];
        }
        for(int i=0;i<bytes.length;i++) {
            data[uuId.length+i] = bytes[i];
        }
        try {
            jedis.lpush(traceId.getBytes(), data);
            jedis.expire(traceId, 60*60*24);
        } catch (Exception e1) {
            logger.error("redisAppender Exception:{}", e1);
        }
    }

    @Override
    public synchronized void stop() {
        super.stop();
    }

    /**
     * 创建appender
     * @param host redis地址
     * @param port 端口
     * @param database 库
     * @param maxIdle 最大闲置
     * @param maxTotal 最大连接
     * @param maxWaitMillis 最大等待时间
     * @param timeout 超时时间
     * @param name 日志名
     * @param ignoreExceptions 异常
     * @param layout 布局
     * @param filter 过滤器
     * @param advertise 通告
     * @param config 配置
     * @return redisappender
     * @date 2016-5-9 上午10:51:25
     */
    @PluginFactory
    public static RedisAppender createAppender(
            // @formatter:off
            @PluginAttribute("host") final String host,
            @PluginAttribute(value = "port", defaultInt = 0) final int port,
            @PluginAttribute(value = "database", defaultInt = 0) final int database,
            @PluginAttribute(value = "maxIdle", defaultInt = 0) final int maxIdle,
            @PluginAttribute(value = "maxTotal", defaultInt = 0) final int maxTotal,
            @PluginAttribute(value = "maxWaitMillis", defaultLong = 0) final Long maxWaitMillis,
            @PluginAttribute(value = "timeout", defaultInt = 0) final Long timeout,

            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute(value = "advertise", defaultBoolean = false) final boolean advertise,
            @PluginConfiguration final Configuration config,
            @PluginAttribute("otherAttribute") String otherAttribute){

        if (name == null) {
            LOGGER.error("No name provided for MyCustomAppenderImpl");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        RedisAppender appender = new RedisAppender(name, filter, layout);
        appender.getUid();
        return appender;
    }
}
