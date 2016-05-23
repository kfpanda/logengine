package com.kfpanda.logengine.filter;


import com.kfpanda.logengine.appender.RedisAppender;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;


public class TraceLogFilter implements Filter {


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest req = (HttpServletRequest) request;

        String uri = req.getRequestURI();
        String requestURL = req.getRequestURL().toString();
        String contextPath = req.getContextPath();
        // 如果开头是savelogin的要记录
        if (uri.endsWith(".css") || uri.endsWith(".png") || uri.endsWith(".js")
                || uri.endsWith("index.htm") || uri.startsWith("/log")) {// 去掉pre
            chain.doFilter(request, response);
            return;
        }
       /* req.getParameter("client_mac");
        req.getParameter("mac");
        req.getParameter("user_mac");
        req.getParameter("userMac");
        req.getParameter("terMac");
        */
        RedisAppender.setUUID(requestURL.hashCode());
        chain.doFilter(request, response);
        
    }

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
        
    }
}
