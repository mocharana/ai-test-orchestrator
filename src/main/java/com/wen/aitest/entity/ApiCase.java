package com.wen.aitest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * t_api_case 表实体：一条用例 = 一行数据。
 * headers/params/body/asserts 为 JSON 字符串（TEXT 列），执行引擎解析后使用。
 */
@Data
@TableName("t_api_case")
public class ApiCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String caseName;

    private String module;

    /** 0=P0 1=P1 2=P2 */
    private Integer priority;

    private String method;

    private String url;

    private String headers;

    private String params;

    private String body;

    /** 字段名避开 Java 关键字 assert */
    private String asserts;

    private String remark;

    private Integer status;

    /** 创建人（当前登录用户） */
    private String createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
