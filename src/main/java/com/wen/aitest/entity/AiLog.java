package com.wen.aitest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** t_ai_log 影子：每次 AI 调用留痕（成本可观测） */
@Data
@TableName("t_ai_log")
public class AiLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务类型：GENERATE=生成用例 ANALYZE=失败归因 */
    private String bizType;

    /** 业务对象 ID：ANALYZE 时 = t_api_result.id（按明细回查历史归因）；GENERATE 为空 */
    private Long bizId;

    private String model;

    private String prompt;

    private String response;

    /** 1=成功 0=失败 */
    private Integer success;

    private String errorMsg;

    private Long costMs;

    private Integer totalTokens;

    private String createBy;

    private LocalDateTime createTime;
}
