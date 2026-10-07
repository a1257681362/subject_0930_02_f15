package com.evops.aqua.staff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 人员岗位资质证书：一证一岗一行，绑定岗位与到期日。
 * 状态 VALID 有效 / EXPIRED 已过期（扫描置位，不删行）/ REVOKED 被撤销。
 * 换证只追加新证，旧证保留可追溯。
 */
@TableName("t_staff_certificate")
public class StaffCertificate extends BaseEntity {

    public static final String STATUS_VALID = "VALID";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_REVOKED = "REVOKED";

    private Long staffId;
    private String certNo;
    private String certName;
    private String postCode;
    private String certAuthor;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private Integer remindBeforeDays;
    private String status;
    private String revokedBy;
    private LocalDateTime revokedTime;
    private String revokeReason;
    private String requestId;
    private String remark;

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public String getCertNo() { return certNo; }
    public void setCertNo(String certNo) { this.certNo = certNo; }
    public String getCertName() { return certName; }
    public void setCertName(String certName) { this.certName = certName; }
    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getCertAuthor() { return certAuthor; }
    public void setCertAuthor(String certAuthor) { this.certAuthor = certAuthor; }
    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Integer getRemindBeforeDays() { return remindBeforeDays; }
    public void setRemindBeforeDays(Integer remindBeforeDays) { this.remindBeforeDays = remindBeforeDays; }
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
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
