package com.kfpanda.mongodb;

import java.net.InetSocketAddress;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.kfpanda.util.PropertiesUtil;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.WriteResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
* 类名： MongoDBDaoImpl
* 作者： kfpanda
* 时间： 2014-8-30 下午04:21:11
* 描述： 
*/
public class MongoDBDaoImpl implements MongoDBDao{
	private static final Logger logger = LogManager.getLogger(MongoDBDaoImpl.class);

	/**
	 * MongoClient的实例代表数据库连接池，是线程安全的，可以被多线程共享，客户端在多线程条件下仅维持一个实例即可
	 * Mongo是非线程安全的，目前mongodb API中已经建议用MongoClient替代Mongo
	 */
	private MongoClient mongoClient = null;
	private	static final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	/**
	 * 私有的构造函数
	 */
	private MongoDBDaoImpl(){
		if(mongoClient == null){
			MongoClientOptions.Builder build = new MongoClientOptions.Builder();	
			PropertiesUtil.init();
			build.connectionsPerHost(PropertiesUtil.getInstance().getIntValue("log.mongo.connectionsPerHost"));	//与目标数据库能够建立的最大connection数量为50
			build.autoConnectRetry(PropertiesUtil.getInstance().getBooleanValue("log.mongo.autoConnectRetry"));	//自动重连数据库启动
			build.threadsAllowedToBlockForConnectionMultiplier(PropertiesUtil.getInstance().getIntValue("log.mongo.threadsAllowedToBlockForConnectionMultiplier"));	//如果当前所有的connection都在使用中，则每个connection上可以有50个线程排队等待
			/*
			 * 一个线程访问数据库的时候，在成功获取到一个可用数据库连接之前的最长等待时间为2分钟
			 * 这里比较危险，如果超过maxWaitTime都没有获取到这个连接的话，该线程就会抛出Exception
			 * 故这里设置的maxWaitTime应该足够大，以免由于排队线程过多造成的数据库访问失败
			 */
			build.maxWaitTime(PropertiesUtil.getInstance().getIntValue("log.mongo.maxWaitTime"));
			build.connectTimeout(PropertiesUtil.getInstance().getIntValue("log.mongo.connectTimeout"));	//与数据库建立连接的timeout设置为1分钟
			build.socketKeepAlive(PropertiesUtil.getInstance().getBooleanValue("log.mongo.socketKeepAlive"));
			build.socketTimeout(PropertiesUtil.getInstance().getIntValue("log.mongo.socketTimeout"));
			
			MongoClientOptions myOptions = build.build();
			ServerAddress address = new ServerAddress(new InetSocketAddress(PropertiesUtil.getInstance().getValue("log.mongo.host"), 
					PropertiesUtil.getInstance().getIntValue("log.mongo.port")));
			
			try {
				//数据库连接实例
				
				if((PropertiesUtil.getInstance().getValue("log.mongo.host")==null)||(PropertiesUtil.getInstance().getValue("log.mongo.host").equals(""))){
					//配置了用户名 密码
					List<MongoCredential> mongoCredentialList = new ArrayList<MongoCredential>();
					mongoCredentialList.add(MongoCredential.createMongoCRCredential(PropertiesUtil.getInstance().getValue("log.mongo.username"), 
							PropertiesUtil.getInstance().getValue("log.mongo.dbname"), PropertiesUtil.getInstance().getValue("log.mongo.password").toCharArray()));
					mongoClient = new MongoClient(address, mongoCredentialList, myOptions);
				}else{
					mongoClient = new MongoClient(address, myOptions);
				}
			} catch (MongoException e){
				e.printStackTrace();
			}
			
		}
	}
	//类初始化时，自行实例化，饿汉式单例模式
	private static final MongoDBDaoImpl mongoDBDaoImpl = new MongoDBDaoImpl();
	/**
	 * 
	 * 方法名：getMongoDBDaoImplInstance
	 * 创建时间：2014-8-30 下午04:29:26
	 * 描述：单例的静态工厂方法
	 * @return
	 */
	public static MongoDBDaoImpl getMongoDBDaoImplInstance(){
		return mongoDBDaoImpl;
	}
	
	@Override
	public boolean delete(String dbName, String collectionName, String[] keys,
			Object[] values) {
		DB db = null;
		DBCollection dbCollection = null;
		if(keys!=null && values!=null){
			if(keys.length != values.length){	//如果keys和values不对等，直接返回false
				return false;
			}else{
				try {
				    db = getDb(dbName);	//获取指定的数据库
					dbCollection = db.getCollection(collectionName);	//获取指定的collectionName集合
					
					BasicDBObject doc = new BasicDBObject();	//构建删除条件
					WriteResult result = null;	//删除返回结果
					String resultString = null;
					
					for(int i=0; i<keys.length; i++){
						doc.put(keys[i], values[i]);	//添加删除的条件
					}
					result = dbCollection.remove(doc);	//执行删除操作
					
					resultString = result.getError();
					
					if(null != db){
						try {
							db.requestDone();	//请求结束后关闭db
							db = null;
						} catch (Exception e) {
							throw new Exception(e.toString());
						}
						
					}
					
					return (resultString!=null) ? false : true;	//根据删除执行结果进行判断后返回结果
				} catch (Exception e) {
					e.printStackTrace();
				} finally{
					if(null != db){
						db.requestDone();	//关闭db
						db = null;
					}
				}
				
			}
		}
		return false;
	}

	@Override
	public Map<String, Object> find(String dbName, String collectionName,
			String[] keys, Object[] values,  Integer curPage, Integer pageSize ) throws Exception {
		ArrayList<DBObject> resultList = new ArrayList<DBObject>();	//创建返回的结果集
		Map<String, Object > ret = new HashMap<String, Object>();
		DB db = null;
		DBCollection dbCollection = null;
		DBCursor cursor = null;
		int count=0;
		
		if(keys!=null && values!=null){
			if(keys.length != values.length){
				return null;	//如果传来的查询参数对不对，直接返回空的结果集
			}else{
				try {
                    db = getDb(dbName); //获取指定的数据库
					dbCollection = db.getCollection(collectionName);	//获取数据库中指定的collection集合
					
					BasicDBObject queryObj = new BasicDBObject();	//构建查询条件
					BasicDBObject timeQb = new BasicDBObject();
					for(int i=0; i<keys.length; i++){
						if((PropertiesUtil.getInstance().getValue("log.fuzzy.search")).indexOf(keys[i])!= -1){ //模糊查询
							StringBuffer content = new StringBuffer();
							content.append("^.*");
							content.append(values[i]);
							content.append(".*$");
							Pattern pattern = Pattern.compile(content.toString(), Pattern.CASE_INSENSITIVE);
							queryObj.put(keys[i],pattern);
						}else if((PropertiesUtil.getInstance().getValue("log.search.start.time")).equals(keys[i])){
							timeQb.put("$gt",dataToLong(values[i].toString()));
							queryObj.put((PropertiesUtil.getInstance().getValue("log.search.creat.time")),timeQb);
						}else if((PropertiesUtil.getInstance().getValue("log.search.end.time")).equals(keys[i])){
							timeQb.put("$lt",dataToLong(values[i].toString()));
							queryObj.put((PropertiesUtil.getInstance().getValue("log.search.creat.time")),timeQb);
						}
						else{
							queryObj.put(keys[i], values[i]);
						}
					}	
					cursor = dbCollection.find(queryObj).skip((curPage-1)*pageSize).limit(pageSize);	//分页查询获取数据
					count=dbCollection.find(queryObj).count(); //数据总数
					while(cursor.hasNext()){
						resultList.add(cursor.next());
					}
				} catch (Exception e) {
					 throw new Exception(e.toString());
				} finally{				
					if(null != cursor){
						cursor.close();
					}
					if(null != db){
						db.requestDone();	//关闭数据库请求
					}
				}
			}
		}
		ret.put("resultList", resultList);
		ret.put("count", count);
		ret.put("pageSize", pageSize);
		ret.put("curPage", curPage);
		return ret;
	}

	@Override
	public Map<String, Object> find(String dbName, String collectionName,
			String[] keys, Object[] values ) throws Exception {
		ArrayList<DBObject> resultList = new ArrayList<DBObject>();	//创建返回的结果集
		Map<String, Object > ret = new HashMap<String, Object>();
		DB db = null;
		DBCollection dbCollection = null;
		DBCursor cursor = null;
		if(keys!=null && values!=null){
			if(keys.length != values.length){
				return null;	//如果传来的查询参数对不对，直接返回空的结果集
			}else{
				try {
					db = getDb(dbName); //获取指定的数据库
					dbCollection = db.getCollection(collectionName);	//获取数据库中指定的collection集合
					BasicDBObject timeQb = new BasicDBObject();
					BasicDBObject queryObj = new BasicDBObject();	//构建查询条件
					for(int i=0; i<keys.length; i++){
						if((PropertiesUtil.getInstance().getValue("log.fuzzy.search")).indexOf(keys[i])!= -1){ //模糊查询
							StringBuffer content = new StringBuffer();
							content.append("^.*");
							content.append(values[i]);
							content.append(".*$");
							Pattern pattern = Pattern.compile(content.toString(), Pattern.CASE_INSENSITIVE);
							queryObj.put(keys[i],pattern);
						}else if((PropertiesUtil.getInstance().getValue("log.search.start.time")).equals(keys[i])){
							timeQb.put("$gt",dataToLong(values[i].toString()));
							queryObj.put((PropertiesUtil.getInstance().getValue("log.search.creat.time")),timeQb);
						}else if((PropertiesUtil.getInstance().getValue("log.search.end.time")).equals(keys[i])){
							timeQb.put("$lt",dataToLong(values[i].toString()));
							queryObj.put((PropertiesUtil.getInstance().getValue("log.search.creat.time")),timeQb);
						}
						else{
							queryObj.put(keys[i], values[i]);
						}
					}
					cursor = dbCollection.find(queryObj);	//分页查询获取数据
					while(cursor.hasNext()){
						resultList.add(cursor.next());
					}
				} catch (Exception e) {
					 throw new Exception(e.toString());
				} finally{				
					if(null != cursor){
						cursor.close();
					}
					if(null != db){
						db.requestDone();	//关闭数据库请求
					}
				}
			}
		}
		ret.put("resultList", resultList);
		return ret;
	}

	
	
	@Override
	public DBCollection getCollection(String dbName, String collectionName) {
		return getDb(dbName).getCollection(collectionName);
	}

	@Override
	public DB getDb(String dbName) {
	    if(dbName==null||dbName.trim().length()==0){
            dbName = PropertiesUtil.getInstance().getValue("log.mongo.dbname");
        }
		return mongoClient.getDB(dbName);
	}
	 
	@Override
	public boolean inSert(String dbName, String collectionName, String[] keys,
			Object[] values) {
		DB db = null;
		DBCollection dbCollection = null;
		WriteResult result = null;
		String resultString = null;
		if(keys!=null && values!=null){
			if(keys.length != values.length){
				return false;
			}else{
				db = getDb(dbName);	//获取数据库实例
				dbCollection = db.getCollection(collectionName);	//获取数据库中指定的collection集合
				BasicDBObject insertObj = new BasicDBObject();
				for(int i=0; i<keys.length; i++){	//构建添加条件
					insertObj.put(keys[i], values[i]);
				}
				
				try {
					result = dbCollection.insert(insertObj);
					resultString = result.getError();
				} catch (Exception e) {
					e.printStackTrace();
				}finally{
					if(null != db){
						db.requestDone();	//请求结束后关闭db
					}
				}				
				return (resultString != null) ? false : true;
			}
		}
		return false;
	}

	@Override
	public boolean isExit(String dbName, String collectionName, String key,
			Object value) {
		DB db = null;
		DBCollection dbCollection = null;
		if(key!=null && value!=null){
			try {
				db = getDb(dbName);	//获取数据库实例
				dbCollection = db.getCollection(collectionName);	//获取数据库中指定的collection集合
				BasicDBObject obj = new BasicDBObject();	//构建查询条件
				obj.put(key, value);
				
				if(dbCollection.count(obj) > 0) {
					return true;
				}else{
					return false;
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally{
				if(null != db){
					db.requestDone();	//关闭db
					db = null;
				}
			}
			
		}
		return false;
	}

	@Override
	public boolean update(String dbName, String collectionName,
			DBObject oldValue, DBObject newValue) {
		DB db = null;
		DBCollection dbCollection = null;
		WriteResult result = null;
		String resultString = null;
		
		if(oldValue.equals(newValue)){
			return true;
		}else{
			try {
				db = getDb(dbName);	//获取数据库实例
				dbCollection = db.getCollection(collectionName);	//获取数据库中指定的collection集合
				
				result = dbCollection.update(oldValue, newValue);
				resultString = result.getError();
				
				return (resultString!=null) ? false : true;
			} catch (Exception e) {
				e.printStackTrace();
			} finally{
				if(null != db){
					db.requestDone();	//关闭db
					db = null;
				}
			}
			
		}
		
		return false;
	}
	@Override
	public Map<String, Object> findLog(Map<String, String> logInfo,String dbName, String collectionName,Integer curPage, Integer pageSize) throws Exception {
		int size = logInfo.size();
		String[] keys = new String[size]; 
		String[] values = new String[size];
		int i=0;
		  for (Map.Entry<String, String> m :logInfo.entrySet())  {  
	            keys[i] =m.getKey();
	            values[i]=m.getValue();
	            i++;
	        }   
		return  MongoDBDaoImpl.getMongoDBDaoImplInstance().find(dbName, collectionName, keys, values,curPage,pageSize);
	}
	
	@Override
	public Map<String, Object> findLog(Map<String, String> logInfo,String dbName, String collectionName) throws Exception {
		int size = logInfo.size();
		String[] keys = new String[size]; 
		String[] values = new String[size];
		int i=0;
		  for (Map.Entry<String, String> m :logInfo.entrySet())  {  
	            keys[i] =m.getKey();
	            values[i]=m.getValue();
	            i++;
	        }   
		return  MongoDBDaoImpl.getMongoDBDaoImplInstance().find(dbName, collectionName, keys, values);
	}
	public String dataToLong(String time) throws ParseException {
		return df.parse(time).getTime()+"";
	}
	
	
}
