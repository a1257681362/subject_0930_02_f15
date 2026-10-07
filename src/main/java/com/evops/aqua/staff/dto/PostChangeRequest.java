package com.evops.aqua.staff.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 岗位上岗/变更请求：目标岗位必须持有效资质证书。
 * 岗位变更不删除历史任职与历史操作——旧任职置 SUPERSEDED 保留，只追加新任职行。
 */
public class PostChangeRequest {

    @NotNull(message = "人员不能为空")
    private Long staffId;
    @NotBlank(message = "目标岗位编码不能为空（VET/SAMPLER/FISHER/EQUIPMENT）")
    private String newPostCode;
    @NotBlank(message = "操作人不能为空")
    private String operator;
    @NotBlank(message = "调岗原因不能为空")
    private String reason;
    @NotBlank(message = "请求幂等号不能为空")
    private String requestId;

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public String getNewPostCode() { return newPostCode; }
    public void setNewPostCode(String newPostCode) { this.newPostCode = newPostCode; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
