package com.wen.aitest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** t_ui_result 表实体：一次 UI 套件执行的结果 */
@Data
@TableName("t_ui_result")
public class UiResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long executionId;

    private Long suiteId;

    private String suiteName;

    /** 1=全部通过 0=存在失败 */
    private Integer passed;

    /** pytest 退出码：0=全过 1=有失败 其他=异常 */
    private Integer exitCode;

    private String summary;

    private String output;

    private Long costMs;

    private LocalDateTime createTime;
}
