package com.github.jbox.http;

import com.google.common.base.Preconditions;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.util.CollectionUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-04 11:08:00.
 */
public class HttpGetClient extends AbstractHttpClient {

    public HttpGetClient(String host) {
        super(host);
    }

    public HttpGetClient(String host, int port) {
        super(host, port);
    }

    public HttpGetClient(String protocol, String host, int port) {
        super(protocol, host, port);
    }

    @Override
    public <T> T execute(String path, Map<String, String> params, RespProcessor<T> processor) {
        Preconditions.checkNotNull(processor);
        try (CloseableHttpClient client = buildHttpClient()) {

            // 组装入参
            URI uri = buildGetURI(buildRequestUrl(path), params);
            HttpGet get = new HttpGet(uri);

            if (!CollectionUtils.isEmpty(this.getReqHeaders())) {
                this.getReqHeaders().forEach(get::addHeader);
            }

            // 执行
            String resp = processResp(client.execute(get));
            return processor.process(newContext(resp, path, params));

        } catch (Throwable e) {
            throw new HttpClientException(e);
        }
    }

    private URI buildGetURI(String url, Map<String, String> params) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(url);
        if (!CollectionUtils.isEmpty(params)) {
            params.forEach(builder::setParameter);
        }

        return builder.build();
    }
}
