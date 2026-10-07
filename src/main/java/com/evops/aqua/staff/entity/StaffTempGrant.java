package com.evops.aqua.staff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

import java.time.LocalDateTime;

/**
 * 临时授权：必须带审批单号与结束时间，在 [startTime, endTime) 内有效。
 * 到期扫描置 EXPIRED 并收权；可提前撤销 REVOKED。审批单号全局唯一，杜绝无单临时授权。
 */
@TableName("t_staff_temp_grant")
public class StaffTempGrant extends BaseEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_REVOKED = "REVOKED";

    private Long staffId;
    private String postCode;
    private String postName;
    private String approvalNo;
    private String approvedBy;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private String scopeMenuSnapshot;
    private String scopeApiSnapshot;
    private String status;
    private String revokedBy;
    private LocalDateTime revokedTime;
    private String revokeReason;
    private String requestId;

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getPostName() { return postName; }
    public void setPostName(String postName) { this.postName = postName; }
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
    public String getScopeMenuSnapshot() { return scopeMenuSnapshot; }
    public void setScopeMenuSnapshot(String scopeMenuSnapshot) { this.scopeMenuSnapshot = scopeMenuSnapshot; }
    public String getScopeApiSnapshot() { return scopeApiSnapshot; }
    public void setScopeApiSnapshot(String scopeApiSnapshot) { this.scopeApiSnapshot = scopeApiSnapshot; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }
    public LocalDateTime getRevokedTime() { return revokedTime; }
    public void setRevokedTime(LocalDateTime revokedTime) { this.revokedTime = revokedTime; }
    public String getRevokeReason() { return revokeReason; }
    public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
