package com.kfpanda.logengine.entity.log;


public class Level {
    public static int OFF = 0;
    
    public static int TRACE = 1;
    
    public static int DEBUG = 2;
    
    public static int INFO = 3;
    
    public static int WARN = 4;
    
    public static int ERROR = 5;
    
    public static int ALL = 6;
    
    public static int getType(String type){
        if(type.equalsIgnoreCase("TRACE")){
            return TRACE;
        }else if(type.equalsIgnoreCase("DEBUG")){
            return DEBUG;
        }else if(type.equalsIgnoreCase("INFO")){
            return INFO;
        }else if(type.equalsIgnoreCase("WARN")){
            return WARN;
        }else if(type.equalsIgnoreCase("ERROR")){
            return ERROR;
        }else{
            return OFF;
        }
    }
}
