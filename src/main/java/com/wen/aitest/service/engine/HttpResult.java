package com.wen.aitest.service.engine;

/**
 * HTTP 执行结果快照。
 * status = HTTP 状态码；0 = 网络级失败（连接拒绝/超时），由断言引擎自然判失败。
 */
public record HttpResult(int status, String body) {
}
