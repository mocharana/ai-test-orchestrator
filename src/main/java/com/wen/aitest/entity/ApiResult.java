package com.wen.aitest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** t_api_result 明细表影子：批次内每条用例的结果一行 */
@Data
@TableName("t_api_result")
public class ApiResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long executionId;

    private Long caseId;

    private String caseName;

    /** 1=通过 0=失败 */
    private Integer passed;

    private Integer httpStatus;

    private String assertSummary;

    private String responseBody;

    private Long costMs;

    private LocalDateTime createTime;
}
