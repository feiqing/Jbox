package com.github.jbox.http;

import com.github.jbox.utils.Collections3;
import com.google.common.base.Preconditions;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author jifang.zjf@alibaba-inc.com (FeiQing)
 * @version 1.0
 * @since 2018-05-04 10:21:00.
 */
public class HttpPostClient extends AbstractHttpClient {

    public HttpPostClient(String host) {
        super(host);
    }

    public HttpPostClient(String host, int port) {
        super(host, port);
    }

    public HttpPostClient(String protocol, String host, int port) {
        super(protocol, host, port);
    }

    @Override
    public <T> T execute(String path, Map<String, String> params, RespProcessor<T> processor) {
        Preconditions.checkNotNull(processor);

        try (CloseableHttpClient client = buildHttpClient()) {
            // 入参
            List<NameValuePair> pairs = new ArrayList<>();
            if (!CollectionUtils.isEmpty(params)) {
                params.forEach((key, value) -> pairs.add(new BasicNameValuePair(key, value)));
            }

            HttpPost post = new HttpPost(buildRequestUrl(path));
            post.setEntity(new UrlEncodedFormEntity(pairs));

            if (!CollectionUtils.isEmpty(this.getReqHeaders())) {
                this.getReqHeaders().forEach(post::addHeader);
            }

            // 执行
            String resp = processResp(client.execute(post));

            return processor.process(newContext(resp, path, params));
        } catch (Throwable e) {
            throw new HttpClientException(e);
        }
    }
}
