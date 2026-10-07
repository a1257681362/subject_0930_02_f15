package com.evops.aqua.staff.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 临时授权请求：审批单号与结束时间缺一不可，无审批单或无结束时间一律拒绝。
 */
public class TempGrantRequest {

    @NotNull(message = "人员不能为空")
    private Long staffId;
    @NotBlank(message = "临时岗位编码不能为空")
    private String postCode;
    @NotBlank(message = "审批单号不能为空：临时授权必须带审批单")
    private String approvalNo;
    @NotBlank(message = "审批人不能为空")
    private String approvedBy;
    @NotNull(message = "开始时间不能为空")
    private LocalDateTime startTime;
    @NotNull(message = "结束时间不能为空：临时授权必须带结束时间")
    private LocalDateTime endTime;
    @NotBlank(message = "授权事由不能为空")
    private String reason;
    @NotBlank(message = "请求幂等号不能为空")
    private String requestId;

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getApprovalNo() { return approvalNo; }
    public void setApprovalNo(String approvalNo) { this.approvalNo = approvalNo; }
    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
