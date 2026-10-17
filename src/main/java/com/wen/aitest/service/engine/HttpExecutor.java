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
 * HTTP 执行器：把一条"用例数据"变成一次真实的 HTTP 请求。
 *
 * 调用侧真实顺序（M3-3 的 ExecutionService 会这样用它）：
 *   ① 从变量表替换 {{token}} 等占位（M3-1 的收货时刻）
 *   ② params JSON 拼进 URL 查询串
 *   ③ headers JSON 解析并设置（POST 有 body 时默认补 Content-Type: application/json）
 *   ④ 发送 → HttpResult(status, body)
 *
 * ⚠️ 核心设计：用 exchange 而不是 retrieve！
 *   retrieve() 遇到 4xx/5xx 会抛异常——但执行引擎里 4xx/5xx 是【合法测试结果】
 *   （断言可能就期望 RuoYi 返回 500），必须如实记录而不是炸掉。
 *   普通业务代码"拒绝失败"，测试工具"如实记录失败"——本质区别。
 */
@Component
public class HttpExecutor {

    private final ObjectMapper objectMapper;

    /** 执行专用 RestClient：5 秒超时——被测接口挂死不能拖垮执行线程 */
    private final RestClient restClient;

    public HttpExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(5_000);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * @param c         用例（数据形态）
     * @param variables 变量表（如 {"token": "RuoYi 登录后拿到的 token"}）
     */
    public HttpResult execute(ApiCase c, Map<String, String> variables) {
        String url;
        try {
            // ① 变量替换：{{token}} 等 → 真实值
            String url1 = VariableResolver.resolve(c.getUrl(), variables);
            String headersJson = VariableResolver.resolve(c.getHeaders(), variables);
            String paramsJson = VariableResolver.resolve(c.getParams(), variables);
            String body = VariableResolver.resolve(c.getBody(), variables);

            // ② params（JSON 对象）→ URL 查询串（?a=1&b=2）
            url = appendParams(url1, paramsJson);

            // ③ headers：JSON 数组 [{key,value}] → Map；POST 有 body 时默认补 JSON 头
            Map<String, String> headerMap = new HashMap<>();
            if (StringUtils.hasText(headersJson)) {
                JsonNode arr = objectMapper.readTree(headersJson);
                arr.forEach(n -> headerMap.put(n.get("key").asString(), n.get("value").asString()));
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

            // bodyTo(String.class) 读响应体——不碰 InputStream，免去受检异常处理
            return withBody.exchange((req, res) -> new HttpResult(
                    res.getStatusCode().value(),
                    res.bodyTo(String.class)));

        } catch (JacksonException e) {
            // 用例数据本身不是合法 JSON——配置错误，快速失败给明确提示
            throw new BizException(3001, "用例数据 JSON 格式错误：" + e.getMessage());
        } catch (Exception e) {
            // 网络级失败（连接拒绝/超时）——也是测试结果：status=0，message 说明原因
            // catch Exception 而非 IOException：Spring 7 的 exchange 异常声明存在
            // 工具链解析差异（javap 实测签名仍 throws IOException，IDEA 误报"从未抛出"），
            // 宽泛兜底最稳——message 保留现场
            return new HttpResult(0, "网络异常：" + e.getMessage());
        }
    }

    /** params 是 JSON 对象 {"pageNum":1} → 拼成 ?pageNum=1（已有 ? 则用 & 续接） */
    private String appendParams(String url, String paramsJson) {
        if (!StringUtils.hasText(paramsJson)) {
            return url;
        }
        JsonNode p = objectMapper.readTree(paramsJson);
        // fromUriString：Spring 7 已移除弃用的 fromHttpUrl（6.1 起弃用、7 删除）——本模块第三个版本坑
        UriComponentsBuilder ub = UriComponentsBuilder.fromUriString(url);
        p.properties().forEach(e -> ub.queryParam(e.getKey(), e.getValue().asString()));
        return ub.encode().build().toUriString();
    }
}
