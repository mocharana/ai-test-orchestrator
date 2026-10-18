package com.wen.aitest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** t_ui_suite 表实体：一个 UI 套件 = 指向一个 pytest 用例路径的配置 */
@Data
@TableName("t_ui_suite")
public class UiSuite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String suiteName;

    /** pytest 目标路径（目录或单个用例文件，相对 work-dir） */
    private String pyPath;

    private String description;

    private String createBy;

    private LocalDateTime createTime;
}
