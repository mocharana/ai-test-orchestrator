package com.wen.aitest.service.engine;

/**
 * 一次 HTTP 执行的结果快照。
 *
 * status = HTTP 状态码；特殊值 0 = 网络级失败（连不上/超时）——
 * 它不是 HTTP 状态，但断言引擎对它自然判失败且 message 可说明原因，
 * 执行器永远不抛"网络异常"，只如实记录（测试工具的设计灵魂）。
 *
 * record = 只读数据载体：自动生成构造器/访问器/equals——Java 17+ 数据类首选。
 */
public record HttpResult(int status, String body) {
}
