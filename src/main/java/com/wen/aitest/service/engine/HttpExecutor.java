package com.wen.aitest.service.engine;

import com.wen.aitest.common.BizException;
import com.wen.aitest.entity.ApiCase;
import com.wen.aitest.util.VariableResolver;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 执行器：把用例数据变成真实 HTTP 请求。
 * 用 exchange 而非 retrieve：4xx/5xx 是合法测试结果，必须如实返回而不是抛异常。
 */
@Component
public class HttpExecutor {

    private final ObjectMapper objectMapper;

    /** 执行专用 RestClient：5 秒超时，防止被测接口挂死拖垮执行线程 */
    private final RestClient restClient;

    public HttpExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(5_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 执行一条用例。
     *
     * @param c         用例数据
     * @param variables 变量表（如 {"token": "..."}），用于替换 {{key}} 占位
     */
    public HttpResult execute(ApiCase c, Map<String, String> variables) {
        String url;
        try {
            // ① 变量替换
            String resolvedUrl = VariableResolver.resolve(c.getUrl(), variables);
            String headersJson = VariableResolver.resolve(c.getHeaders(), variables);
            String paramsJson = VariableResolver.resolve(c.getParams(), variables);
            String body = VariableResolver.resolve(c.getBody(), variables);

            // ② params JSON 拼进 URL 查询串
            url = appendParams(resolvedUrl, paramsJson);

            // ③ 解析请求头（形态宽容：数组 [{key,value}] 或对象 {k:v} 都能吃——AI/手写两种习惯都兼容）；
            //    POST 带 body 时默认补 Content-Type
            Map<String, String> headerMap = new HashMap<>();
            if (StringUtils.hasText(headersJson)) {
                JsonNode h = objectMapper.readTree(headersJson);
                if (h.isArray()) {
                    h.forEach(n -> {
                        String k = n.path("key").asString("");
                        if (!k.isEmpty()) {
                            headerMap.put(k, n.path("value").asString(""));
                        }
                    });
                } else if (h.isObject()) {
                    h.properties().forEach(e -> headerMap.put(e.getKey(), e.getValue().asString("")));
                }
            }
            boolean hasBody = StringUtils.hasText(body) && !"GET".equalsIgnoreCase(c.getMethod());
            if (hasBody && !headerMap.containsKey("Content-Type")) {
                headerMap.put("Content-Type", "application/json");
            }

            // ④ 发送（exchange：4xx/5xx 不抛异常，如实返回）
            RestClient.RequestBodySpec spec = restClient
                    .method(HttpMethod.valueOf(c.getMethod().toUpperCase()))
                    .uri(URI.create(url))
                    .headers(h -> headerMap.forEach(h::set));

            RestClient.RequestBodySpec withBody = hasBody ? spec.body(body) : spec;

            return withBody.exchange((req, res) -> new HttpResult(
                    res.getStatusCode().value(),
                    res.bodyTo(String.class)));

        } catch (BizException e) {
            throw e; // 用例配置错误：上抛给 Runner 逐条记录（不该被误标成"网络异常"）
        } catch (JacksonException e) {
            // 用例数据不是合法 JSON——配置错误，快速失败
            throw new BizException(3001, "用例数据 JSON 格式错误：" + e.getMessage());
        } catch (Exception e) {
            // 网络级失败（连接拒绝/超时）→ status=0；带上异常类型，避免 NPE 之类被误标成网络问题
            return new HttpResult(0, "请求失败[" + e.getClass().getSimpleName() + "]：" + e.getMessage());
        }
    }

    /** params JSON 对象拼成 URL 查询串 */
    private String appendParams(String url, String paramsJson) {
        if (!StringUtils.hasText(paramsJson)) {
            return url;
        }
        JsonNode p = objectMapper.readTree(paramsJson);
        UriComponentsBuilder ub = UriComponentsBuilder.fromUriString(url);
        p.properties().forEach(e -> ub.queryParam(e.getKey(), e.getValue().asString()));
        return ub.encode().build().toUriString();
    }
}
