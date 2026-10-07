package com.evops.aqua.staff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

import java.time.LocalDate;

/**
 * 证书到期提醒：进入提醒窗口的有效证书每证每日至多一条，扫描幂等不多发。
 */
@TableName("t_staff_cert_reminder")
public class StaffCertReminder extends BaseEntity {

    public static final String STATUS_REMINDED = "REMINDED";

    private Long certificateId;
    private Long staffId;
    private LocalDate remindDate;
    private LocalDate expiryDate;
    private Integer daysToExpiry;
    private String status;
    private String requestId;

    public Long getCertificateId() { return certificateId; }
    public void setCertificateId(Long certificateId) { this.certificateId = certificateId; }
    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public LocalDate getRemindDate() { return remindDate; }
    public void setRemindDate(LocalDate remindDate) { this.remindDate = remindDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
    public Integer getDaysToExpiry() { return daysToExpiry; }
    public void setDaysToExpiry(Integer daysToExpiry) { this.daysToExpiry = daysToExpiry; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
