package com.kfpanda.logengine.appender;

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
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Serializable;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.Date;
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
    /**
     * 序列号
     */
    private static final long serialVersionUID = -1249019394975267818L;
    private static Logger logger = LogManager.getLogger(RedisAppender.class);
    private static final String KEY_PREFIX = "LOG_";
    
    /**
     * 时间格式
     */
    private static SimpleDateFormat idFormat = new SimpleDateFormat("yyyMMddHHmmss");
    
    /**
     * 异常次数记录
     */
    private static int eCount = 0;
    private static final Integer ERROR_NUM = 5;

    private static String getRandomId(){
        return KEY_PREFIX + idFormat.format(new Date()) + RandomStringUtils.randomAlphanumeric(4);
    }
    
    /**
     * 日志id
     */
    public static ThreadLocal<String> threadUUID = new ThreadLocal<String>() {
        public String initialValue() {
            return getRandomId();
        }
    };
    
    /**
     * 插入
     * @date 2016-5-9 下午4:38:14
     */
    public static void setUUID(double urlKey){
        String reqId = getRandomId();
        threadUUID.set(reqId);
        Jedis jedis = null;
        if(0 != urlKey) {
            try {
                jedis = pool.getResource();
                //设置reqId这个key的过期时间。
                jedis.expire(reqId, expireTime);
                jedis.zadd(KEY_PREFIX + "LIST", urlKey, reqId);
                eCount = 0;
            } catch (Exception e1) {
                logger.error("eCount={},redisAppender Exception:{}", eCount , e1);
                eCount++;
            }finally {
               if(null != jedis) {
                   jedis.close();
               }
            }
        }
    }

    private final String host;
    private final int port;
    private final int database;
    private static JedisPool pool;
    private final int maxIdle;
    private final int maxTotal;
    private final Long maxWaitMillis;
    private final boolean testOnBorrow;
    private final int timeout;
    private static int expireTime = 3600;
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock;

    /**
     * 构建redisAppender
     * @param name
     * @param filter
     * @param layout
     */
    protected RedisAppender(String name, Filter filter, Layout<? extends Serializable> layout,
                            String host, int port, int database, int maxIdle, int maxTotal,
                            Long maxWaitMillis, boolean testOnBorrow, int timeout, int expireTime) {
        super(name, filter, layout, true);
        this.readLock = this.rwLock.readLock();
        this.host = host;
        this.port = port;
        this.database = database;
        this.maxIdle = maxIdle;
        this.maxTotal = maxTotal;
        this.maxWaitMillis = maxWaitMillis;
        this.testOnBorrow = testOnBorrow;
        this.timeout = timeout;
        this.expireTime = expireTime;
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxIdle(maxIdle);
            poolConfig.setMaxTotal(maxTotal);
            poolConfig.setMaxWaitMillis(maxWaitMillis);
            poolConfig.setTestOnBorrow(testOnBorrow);
            pool = new JedisPool(poolConfig, host, port);
        } catch (Exception e) {
            logger.error("获取jedis异常, eCount={}, Exception:{}", eCount , e);
            eCount++;
        }
    }

    
    /**
     * 执行数据传输
     */
    @Override
    public void append(final LogEvent event) {
        if(eCount > ERROR_NUM) {
            logger.error("RedisAppender 异常超过3次");
            return;
        }
        this.readLock.lock();
        String traceId = threadUUID.get();

        final byte[] bytes = getLayout().toByteArray(event);
        Jedis jedis = null;
        try {
            jedis = pool.getResource();
            jedis.lpush(traceId.getBytes(), bytes);
            //将错误计数清零
            eCount = 0;
        } catch (Exception e1) {
            logger.error("eCount={},redisAppender Exception:{}", eCount , e1);
            eCount++;
        }finally {
            if(null != jedis) {
                jedis.close();
            }
            this.readLock.unlock();
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
            @PluginAttribute(value = "port", defaultInt = 6379) final int port,
            @PluginAttribute(value = "database", defaultInt = 0) final int database,
            @PluginAttribute(value = "maxIdle", defaultInt = 20) final int maxIdle,
            @PluginAttribute(value = "maxTotal", defaultInt = 50) final int maxTotal,
            @PluginAttribute(value = "maxWaitMillis", defaultLong = 10000) final Long maxWaitMillis,
            @PluginAttribute(value = "testOnBorrow", defaultBoolean = true) final boolean testOnBorrow,
            @PluginAttribute(value = "timeout", defaultInt = 300000) final int timeout,
            @PluginAttribute(value = "expireTime", defaultInt = 3600) final int expireTime,

            @PluginAttribute("name") final String name,
            @PluginAttribute(value = "ignoreExceptions", defaultBoolean = true) final boolean ignoreExceptions,
            @PluginElement("Layout") Layout<? extends Serializable> layout,
            @PluginElement("Filter") final Filter filter,
            @PluginAttribute(value = "advertise", defaultBoolean = false) final boolean advertise,
            @PluginConfiguration final Configuration config,
            @PluginAttribute("otherAttribute") String otherAttribute){

        if (name == null) {
            LOGGER.error("No name provided for RedisAppender");
            return null;
        }
        if (host == null) {
            LOGGER.error("No host provided for RedisAppender");
            return null;
        }
        if (layout == null) {
            layout = PatternLayout.createDefaultLayout();
        }
        RedisAppender appender = new RedisAppender(name, filter, layout, host, port, 
                database, maxIdle, maxTotal, maxWaitMillis, testOnBorrow, timeout, expireTime);
        return appender;
    }
}
