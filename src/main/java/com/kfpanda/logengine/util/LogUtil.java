package com.kfpanda.logengine.util;

import java.lang.reflect.Field;

import com.kfpanda.util.PropertiesUtil;
import com.kfpanda.logengine.entity.log.Level;
import com.kfpanda.logengine.entity.log.LogContent;
import com.kfpanda.mongodb.MongoDBDaoImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class LogUtil {
    private static final Logger logger = LogManager.getLogger(LogUtil.class);
    private static ThreadLocal<LogContent>logContent = new ThreadLocal<LogContent>();
    
    
    public LogUtil(){
    }

    public static LogContent getLog() {
        LogContent ci = logContent.get();
        if(ci==null){
            ci = new LogContent();
            logContent.set(ci);
        }
        return ci;
    }

    public static void setLog(LogContent content) {
        logContent.set(content);
    }
    
    private static void logToDB(String key,String collectionName,Object... obj){
        MongoDBDaoImpl dao = MongoDBDaoImpl.getMongoDBDaoImplInstance();
        LogContent content = logContent.get();
        //TODO 还缺将logcontent转化为键对值数组的方法步骤
        Field[] declaredFields = content.getClass().getDeclaredFields();
        String[] objkey = convertKey(obj);
        String [] keys = new String[declaredFields.length+objkey.length];
        Object [] objs = new Object[keys.length];
        int i = 0;
        for (Field field : declaredFields) {
            field.setAccessible(true);
            Object param;
            try {
                param = field.get(content);
                keys[i++] = field.getName();
                objs[i++] = param;
            } catch (IllegalArgumentException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        String[]srcKeys = convertKey(obj);
        System.arraycopy(srcKeys, 0, keys, declaredFields.length, srcKeys.length);
        System.arraycopy(obj, 0, objs, declaredFields.length, obj.length);
        dao.inSert(null, collectionName, keys, objs);
    }
    
    private static String[] convertKey(Object[] object) {
        String[] str = new String[object.length];
        for (int i = 0; i < object.length; i++) {
            str[i] = object[i].getClass().getName();
        }
        return str;
    } 
    
    private static int getLogType(){
        String type = PropertiesUtil.getInstance().getValue("log.type");
        return Level.getType(type);
    }

    public static void debug(String arg0, Object... obj){
        if(getLogType() <= Level.DEBUG){
            logToDB(null, "common_info", obj);
        }
        
    }
    public static void debug(String arg0, Throwable t){
        if(getLogType() <= Level.DEBUG){
            logToDB(null, "error_info", t.getMessage());
        }
    }
    
    public static void info(String arg0, Object... obj){
        if(getLogType() <= Level.INFO){
            logToDB(null, "common_info", obj);
        }
    }
    public static void info(String arg0, Throwable t){
        if(getLogType() <= Level.INFO){
            logToDB(null, "error_info", t.getMessage());
        }
    }
    
    public static void warn(String arg0, Object... obj){
        if(getLogType() <= Level.WARN){
            logToDB(null, "common_info", obj);
        }
    }
    public static void warn(String arg0, Throwable t){
        if(getLogType() <= Level.WARN){
            logToDB(null, "error_info", t.getMessage());
        }
    }
    
    public static void trace(String arg0, Object... obj){
        if(getLogType() <= Level.TRACE){
            logToDB(null, "common_info", obj);
        }
    }
    public static void trace(String arg0, Throwable t){
        if(getLogType() <= Level.TRACE){
            logToDB(null, "error_info", t.getMessage());
        }
    }
    
    public static void error(String arg0, Object... obj){
        if(getLogType() <= Level.ERROR){
            logToDB(null, "common_info", obj);
        }
    }
    public static void error(String arg0, Throwable t){
        if(getLogType() <= Level.ERROR){
            logToDB(null, "error_info", t.getMessage());
        }
    }



    public static void main(String[] args){
        logger.trace("sdddddddddd");
        long startTime = System.currentTimeMillis();
        for(int i = 0; i < 1; i++) {
            logger.info("sddddsssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssssddddddd");
        }
        long endTime = System.currentTimeMillis();
        System.out.println(endTime - startTime);
        Thread thread1 = new MyThread("th1");
        thread1.run();
        Thread thread2 = new MyThread("th2");
        thread2.run();
        Thread thread3 = new MyThread("th3");
        thread3.run();
    }

}

 class MyThread extends Thread{
    private static final Logger logger = LogManager.getLogger(LogUtil.class);
    private String name;
    public MyThread(String name) {
        this.name=name;
    }
    public void run() {
        try {
            logger.info(this.name + "1111");
            Thread.sleep(1000);
            logger.info(this.name +"2222");
            Thread.sleep(100);
            logger.info(this.name + "3333");
            Thread.sleep(1000);
            logger.info(this.name + "4444");
            Thread.sleep(100);
            logger.info(this.name + "5555");
        }catch (Exception e){

        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
