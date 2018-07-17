package com.github.jbox.http;

import com.google.common.base.Strings;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpProcessor;
import org.apache.http.util.EntityUtils;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-04 10:53:00.
 */
@Data
@Slf4j
public abstract class AbstractHttpClient {

    /**
     * for request
     */
    private String protocol = "http";

    private String host;

    private int port = 80;

    private List<Cookie> reqCookies;

    /**
     * for response
     */
    private List<Header> respHeaders;

    private List<Cookie> respCookies;

    private String respString;

    /**
     * 通用
     */
    // 从连接池获取连接超时时间, 默认[10s]
    private int connectionRequestTimeout = 10 * 1000;

    // 建立连接超时时间, 默认[2s]
    private int connectTimeout = 2 * 1000;

    // 响应超时时间, 默认[5s]
    private int socketTimeout = 5 * 1000;

    private HttpProcessor httpProcessor;

    private CookieStore cookieStore = new BasicCookieStore();

    public AbstractHttpClient(String host) {
        this.host = host;
    }

    public AbstractHttpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public AbstractHttpClient(String protocol, String host, int port) {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    public List<Cookie> getRespCookies() {
        return Optional.ofNullable(respCookies).orElseGet(Collections::emptyList);
    }

    public void setReqCookie(Cookie reqCookie) {
        if (reqCookie != null) {
            this.setReqCookies(Collections.singletonList(reqCookie));
        }
    }

    public String execute() {
        return execute("");
    }

    public String execute(String path) {
        return execute(path, null);
    }

    public String execute(String path, Map<String, String> params) {
        return execute(path, params, RespProcessor.identity);
    }

    public abstract <T> T execute(String path, Map<String, String> params, RespProcessor<T> processor);


    /**
     * helpers
     */

    protected CloseableHttpClient buildHttpClient() {
        HttpClientBuilder builder = HttpClients.custom();
        builder.setDefaultRequestConfig(generateRequestConfig());

        if (getCookieStore() != null) {
            builder.setDefaultCookieStore(getCookieStore());
        }

        if (getHttpProcessor() != null) {
            builder.setHttpProcessor(getHttpProcessor());
        }

        // cookies
        addReqCookies(getReqCookies());

        return builder.build();
    }

    private RequestConfig generateRequestConfig() {
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setConnectionRequestTimeout(connectionRequestTimeout);
        builder.setConnectTimeout(connectTimeout);
        builder.setSocketTimeout(socketTimeout);

        return builder.build();
    }

    private void addReqCookies(List<Cookie> cookies) {
        if (CollectionUtils.isEmpty(cookies)) {
            return;
        }

        if (getCookieStore() == null) {
            log.warn("CookieStore == null");
            return;
        }

        cookies.forEach(cookie -> {
            if (cookie == null) {
                return;
            }

            getCookieStore().addCookie(cookie);
        });
    }

    protected String buildRequestUrl(String path) {
        return String.format("%s://%s:%d%s", getProtocol(), getHost(), getPort(), path);
    }

    protected String processResp(CloseableHttpResponse response) throws IOException {
        if (response == null) {
            throw new HttpClientException("http response == null");
        }

        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            throw new HttpClientException("http response status code : " + response.getStatusLine().getStatusCode());
        }

        HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw new HttpClientException("http response entity == null");
        }

        String respString = EntityUtils.toString(entity, StandardCharsets.UTF_8);
        if (Strings.isNullOrEmpty(respString)) {
            // todo +开关
            log.warn("http response string is empty");
        }


        setRespString(respString);
        setRespCookies(Optional.ofNullable(getCookieStore()).map(CookieStore::getCookies).orElse(Collections.emptyList()));
        Header[] headers = response.getAllHeaders();
        if (headers == null) {
            setRespHeaders(Collections.emptyList());
        } else {
            setRespHeaders(Arrays.asList(headers));
        }

        return respString;
    }

    protected RespContext newContext(String resp, String path, Map<String, String> params) {
        RespContext context = new RespContext();
        context.setResp(resp);
        context.setClient(this);
        context.setPath(path);
        context.setParams(params);

        return context;
    }

    public static class HttpClientException extends RuntimeException {

        private static final long serialVersionUID = -2449653478069192157L;

        public HttpClientException() {
            super();
        }

        public HttpClientException(String message) {
            super(message);
        }

        public HttpClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public HttpClientException(Throwable cause) {
            super(cause);
        }

        protected HttpClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

}
