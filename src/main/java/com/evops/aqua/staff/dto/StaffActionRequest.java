package com.evops.aqua.staff.dto;

import javax.validation.constraints.NotBlank;

/**
 * 通用操作请求：解冻、离场、在办任务销项、临时授权提前撤销等带操作人与幂等号的动作。
 * reason 对离场/提前撤销必填，由服务层按动作校验。
 */
public class StaffActionRequest {

    private String operator;
    private String reason;
    @NotBlank(message = "请求幂等号不能为空")
    private String requestId;

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
