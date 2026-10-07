package com.evops.aqua.staff.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量冻结请求：先调预览（dry-run）列出受影响工单与未完成任务，再确认冻结。
 */
public class FreezeRequest {

    @NotEmpty(message = "至少选择一名冻结人员")
    private List<String> staffNos;
    @NotBlank(message = "冻结原因不能为空")
    private String reason;
    @NotBlank(message = "操作人不能为空")
    private String operator;
    @NotBlank(message = "请求幂等号不能为空")
    private String requestId;

    public List<String> getStaffNos() { return staffNos; }
    public void setStaffNos(List<String> staffNos) { this.staffNos = staffNos; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
