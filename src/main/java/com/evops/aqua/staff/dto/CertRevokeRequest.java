package com.evops.aqua.staff.dto;

import javax.validation.constraints.NotBlank;

/**
 * 证书撤销/人员离场等手工操作的通用请求。
 */
public class CertRevokeRequest {

    @NotBlank(message = "操作人不能为空")
    private String operator;
    @NotBlank(message = "撤销原因不能为空")
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
