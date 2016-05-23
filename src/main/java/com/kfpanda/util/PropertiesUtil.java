package com.kfpanda.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.URL;
import java.util.Properties;

/**
 * 单例模式 读取配置文件的相关信息 
 * 该配置文件有3中读取方式 :
 * 		1.具体的文件路径
 * 		2.相对类路径的文件路径 
 * 		3.远程URL路径
 * @author kfpanda 2015-7-14 上午10:55:45
 */
public class PropertiesUtil {
	private static Logger logger = LogManager.getLogger(PropertiesUtil.class);

	private static PropertiesUtil instance = new PropertiesUtil();
	private static Properties config = null;

	static {
		init();
	}

	/**
	 * 初始化载入classpath下的properties配置文件及/properties/目录下配置文件。
	 */
	public static void init() {
		logger.info(">>> loading the properties .....");
		//加载所有class目录下的所有properties文件。
		loadProps("/");
		//加载所有class目录下properties目录下的所有properties文件。
		loadProps("/properties/");
		logger.info(">>> loaded the properties ... ... ... ... ... ... ... ... OK!");
	}

	/**
	 * 扫描class路径下指定的目录的properties文件。
	 *
	 * @param path 为class目录下的目录路径。 path=/ 扫描class目录所有properties文件。
	 */
	private static void loadProps(String path) {
		@SuppressWarnings("ConstantConditions")
		String filePath = PropertiesUtil.class.getClassLoader().getResource("").getPath() + path;
		File fileDir = new File(filePath);
		File[] propsFile = fileDir.listFiles(new FilenameFilter() {
			private String extension = ".properties";

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(extension);
			}
		});

		if (config == null) {
			config = new Properties();
		}
		for (File file : propsFile) {
			FileInputStream fis;
			try {
				fis = new FileInputStream(file);
				config.load(fis);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				logger.debug("classpath 路径下, " + filePath + " 文件无法找到.");
			} catch (IOException e) {
				e.printStackTrace();
				logger.debug("classpath 路径下, " + filePath + " 文件读取失败.");
			}
		}
	}

	public static PropertiesUtil addProps(String configPath) {
		loadProperties(configPath);
		return getInstance();
	}
	
	public static PropertiesUtil getInstance() {
		return instance;
	}

	public static Properties getConfig() {
        return config;
    }

	public static String getValue(String name, String defaultValue) {
		return config.getProperty(name, defaultValue);
	}

	public static String getValue(String name) {
		return getValue(name, null);
	}

	public static Integer getIntValue(String name) {
		return Integer.parseInt(getValue(name));
	}
	public static Integer getIntValue(String name, Integer defaultValue) {
		String value = getValue(name);
		return value == null ? defaultValue : Integer.parseInt(value);
	}
	
	public static Boolean getBooleanValue(String name) {
		return Boolean.parseBoolean(getValue(name));
	}

	/**
	 * 载入configPath路径的配置文件。
	 * @param configPath 参数configpath可以 是远程URL 也可以是相对classpath的路径 还可以是绝对路径
     */
	private static void loadProperties(String configPath) {
		if (configPath == null) {
			logger.error("文件的路径不能为空");
			return;
		}

		InputStream ins = null;
		
		try {
			ins = new FileInputStream(configPath);
		} catch (FileNotFoundException e) {
			logger.debug("绝对路径:" + configPath + " 找不到该文件");
		}

		if (ins == null) {
			try {
				//noinspection ConstantConditions
				ins = new FileInputStream(PropertiesUtil.class.getClassLoader().getResource("").getPath() + configPath);
			} catch (FileNotFoundException e) {
				logger.debug("classpath 路径下, " + configPath + " 文件无法找到.");
			}
		}

		if (ins == null) {
			logger.debug("类路径下:" + configPath + " 找不到该文件");
			try {
				ins = (new URL(configPath)).openStream();
			} catch (Exception e) {
				logger.debug("远程路径:" + configPath + " 取不到该文件");
			}
		}

		try {
			if (ins != null) {
				config.load(ins);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		} finally {
			if (ins != null) {
				try {
					ins.close();
				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		
		if(ins == null){
			logger.error("加载属性文件失败");
		}
	}
}
