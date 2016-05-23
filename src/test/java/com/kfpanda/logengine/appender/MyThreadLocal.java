package com.kfpanda.logengine.appender;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

public class MyThreadLocal {
    
    /**
     * 线程变量-全局唯一识别 
     */
    public static ThreadLocal<String> threadUUID = new ThreadLocal<String>() {
        /*public String initialValue() {
            return StringUtil.getToken();  
        }*/
    };
    
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
            threadUUID.set("LOG_" + RandomStringUtils.randomAlphanumeric(4) + "-");
            return threadUUID.get();
        }
        String strs = StringUtils.EMPTY;
        for(String str : strings) {
            strs+=str;
        }
        
        return threadUUID.get();
    }

}
