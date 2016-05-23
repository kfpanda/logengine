package com.kfpanda.logengine.filter;


import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Random;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.kfpanda.logengine.entity.log.LogContent;
import com.kfpanda.logengine.util.LogUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogFilter implements Filter {
    private static final Logger logger = LogManager.getLogger(LogFilter.class);

    private SimpleDateFormat idFormat = new SimpleDateFormat("yyyMMddHHmmss");
    private SimpleDateFormat logFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SS");

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("log filter init.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest req = (HttpServletRequest) request;

        String uri = req.getRequestURI();
        String requestURL = req.getRequestURL().toString();
        String contextPath = req.getContextPath();

        // 如果开头是savelogin的要记录
        if (uri.endsWith(".css") || uri.endsWith(".png") || uri.endsWith(".js") || uri.endsWith(".jpg") || uri.endsWith(".gif")
                || uri.endsWith("index.htm") || uri.startsWith("/log")) {// 去掉pre
            chain.doFilter(request, response);
            return;
        }

        LogContent content = LogUtil.getLog();
        String requestId = idFormat.format(new Date()) + RandomStringUtils.random(8);
//        Map<String,String> paramMap = new HashMap<String, String>();
        String paramJson = "{";
        Enumeration<String> params = req.getParameterNames();
        while(params.hasMoreElements()){
            String key = params.nextElement();
            String value = req.getParameter(key);
            paramJson += key + ":" + value + ",";
//            paramMap.put(key,value );
        }
        paramJson = "}";
        content.setRequestId(requestId);
        content.setLogTime(logFormat.format(new Date()));
        content.setParam(paramJson);
        content.setRequestUrl(uri);
        
        chain.doFilter(request, response);
        
    }

    @Override
    public void destroy() {
        logger.debug("log filter destroy.");
    }
}
