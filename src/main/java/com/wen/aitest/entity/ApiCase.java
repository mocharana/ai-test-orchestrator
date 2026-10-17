package com.wen.aitest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * t_api_case 表的影子——"用例即数据"的 Java 形态。
 *
 * headers/params/body/asserts 存的是 JSON 字符串（TEXT 列），
 * M3 执行引擎再解析成对象使用——存储时一坨字符串，执行时才拆开。
 *
 * 字段名 asserts（不是 assert）：assert 是 Java 关键字（断言语句），要避开。
 */
@Data
@TableName("t_api_case")
public class ApiCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String caseName;

    private String module;

    /** 0=P0 1=P1 2=P2（冒烟跑 P0，回归跑 P0+P1） */
    private Integer priority;

    private String method;

    private String url;

    private String headers;

    private String params;

    private String body;

    private String asserts;

    private String remark;

    private Integer status;

    /** 创建人——值来自 M1 拦截器 request.setAttribute("currentUser")，伏笔回收 */
    private String createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
