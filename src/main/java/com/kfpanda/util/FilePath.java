package com.kfpanda.util;

import java.io.UnsupportedEncodingException;

/**
 * 获取文件路径
 * @author kfpanda
 * @date 2015年1月7日 上午10:41:12
 */
public class FilePath {
	
	/**
	 * 获取项目classpath目录的绝对路径
	 * @return classes目录的绝对路径<br/>
	 * 	file:/F:/tomcat/webapps/J2EEUtil/WEB-INF/classes/
	 */
	public static String getAbsolutePathWithClass() {
		String path = FilePath.class.getResource("/").getPath();
		try {
			//获得的路径是getResource方法使用了utf-8对路径信息进行了编码。
			//当路径中存在中文和空格时，他会对这些字符进行转换。
			path = java.net.URLDecoder.decode(path,"utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if(path.length() > 5 && "file:".equals(path.substring(0,5))){
			//windows 路径格式 file:/F:/home/myweb/cms/cms/
			//linux 路径 格式 file:/home/myweb/cms/cms/
			path = path.substring(5);
		}
		return path;
	}
	
	/**
	 * 获得web项目的根路径
	 * @return
	 */
	public static String getWEBRoot(){
		String path = FilePath.getAbsolutePathWithClass().toString();
		if(path.indexOf("WEB-INF") > -1){
			path = path.substring(0,path.indexOf("WEB-INF"));
		}
		return path;
	}

	/**
	 * 获取项目classPath目录下的指定目录的绝对路径
	 * @param path
	 * 			classes目录下的指定目录.比如:/com/
	 * @return file:/F:/tomcat/webapps/J2EEUtil/WEB-INF/classes/com/
	 */
	public static String getAbsolutePathWithClass(String path) {
		return FilePath.class.getResource(path).toString();
	}
	 
	/**
	 * 获取指定类文件的所在目录的绝对路径
	 * @param clazz
	 * @return 类文件的绝对路径.例如:<br/> 包com.Aries.Util.Web下的Main.java类.<br/>
	 *  路径为:file:/
	 *         F:/tomcat/webapps/J2EEUtil/WEB-INF/classes/com/Aries/Util/Web/
	 */
	public static String getAbsolutePathWithClass(Class<?> clazz) {
		String path = clazz.getResource("").toString();
		if(path.length() > 5 && "file:".equals(path.substring(0,5))){
			//windows 路径格式 file:/F:/home/myweb/cms/cms/
			//linux 路径 格式 file:/home/myweb/cms/cms/
			path = path.substring(5);
		}
		return path;
	}
}
