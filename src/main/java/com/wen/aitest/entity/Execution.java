package com.wen.aitest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** t_execution 批次表影子：一次"点执行" = 一行 */
@Data
@TableName("t_execution")
public class Execution {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** RUNNING / FINISHED */
    private String status;

    /** 执行器类型：API（Java 内置引擎）/ UI（ProcessBuilder 桥 pytest） */
    private String execType;

    /** 执行对象快照：用例名（接口）或套件名（UI）。快照不随用例改名/删除变化 */
    private String targetName;

    private Integer totalCount;

    private Integer passedCount;

    private Integer failedCount;

    private String createBy;

    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
