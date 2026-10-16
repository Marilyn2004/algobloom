package com.ziyi.leetcodereviewsystem.coach;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
@Component
class CoachRequestLimit extends OncePerRequestFilter {
    private record Window(long minute,int count){}
    private final ConcurrentHashMap<String,Window> windows=new ConcurrentHashMap<>();
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        if(req.getRequestURI().startsWith("/api/v2/") && req.getContentLengthLong()>65536){res.sendError(413);return;}
        if(req.getRequestURI().startsWith("/api/v2/auth/") && req.getMethod().equals("POST")){
            long minute=System.currentTimeMillis()/60000;
            if(windows.size()>10000)windows.entrySet().removeIf(e->e.getValue().minute()<minute);
            var window=windows.compute(req.getRemoteAddr(),(k,v)->new Window(minute,v==null||v.minute()!=minute?1:v.count()+1));
            if(window.count()>20){res.setStatus(429);res.setContentType("application/json");res.getWriter().write("{\"code\":\"AUTH_RATE_LIMIT\"}");return;}
        }
        if(req.getRequestURI().startsWith("/api/v2/")){
            var wrapped=new HttpServletRequestWrapper(req){
                private ServletInputStream limited;
                @Override public ServletInputStream getInputStream() throws IOException {
                    if(limited!=null)return limited;var original=super.getInputStream();
                    limited=new ServletInputStream(){private int bytes;
                        @Override public int read() throws IOException {int value=original.read();if(value>=0&&++bytes>65536)throw new IOException("Request too large");return value;}
                        @Override public boolean isFinished(){return original.isFinished();}
                        @Override public boolean isReady(){return original.isReady();}
                        @Override public void setReadListener(ReadListener listener){original.setReadListener(listener);}
                    };return limited;
                }
                @Override public java.io.BufferedReader getReader() throws IOException {return new java.io.BufferedReader(new java.io.InputStreamReader(getInputStream(),java.nio.charset.StandardCharsets.UTF_8));}
            };chain.doFilter(wrapped,res);
        }else chain.doFilter(req,res);
    }
}
