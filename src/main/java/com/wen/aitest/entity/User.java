package com.wen.aitest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * t_user 表在 Java 里的"影子"：一个字段对应一个列。
 *
 * MyBatis-Plus 的列映射约定（启动时反射读取）：
 *   类名 ↔ 表名：靠 @TableName 显式指定
 *   字段 ↔ 列名：驼峰自动转下划线（createTime ↔ create_time，默认开启）
 *   主键：@TableId(type = IdType.AUTO) = 数据库自增，插入后 MP 自动把新 id 回填到对象
 */
@Data
@TableName("t_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 存的是 BCrypt 密文；永远不返回给前端（M1-2 注册时加密，拦截器管返回过滤） */
    private String password;

    private String nickname;

    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
