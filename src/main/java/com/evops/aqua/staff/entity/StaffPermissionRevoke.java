package com.evops.aqua.staff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

import java.time.LocalDateTime;

/**
 * 权限收回台账：证书过期/临时授权到期/岗位变更/冻结/手工撤销每发生一次收权追加一行。
 * 收回的菜单与接口范围取当时岗位配置快照，只追加，供变更日志交叉核对。
 */
@TableName("t_staff_permission_revoke")
public class StaffPermissionRevoke extends BaseEntity {

    public static final String REASON_CERT_EXPIRED = "CERT_EXPIRED";
    public static final String REASON_TEMP_EXPIRED = "TEMP_EXPIRED";
    public static final String REASON_POST_CHANGE = "POST_CHANGE";
    public static final String REASON_FREEZE = "FREEZE";
    public static final String REASON_LEAVE = "LEAVE";
    public static final String REASON_MANUAL_REVOKE = "MANUAL_REVOKE";

    private Long staffId;
    private String staffNo;
    private String postCode;
    private String reason;
    private String refType;
    private Long refId;
    private String revokedBy;
    private LocalDateTime revokedTime;
    private String affectedMenus;
    private String affectedApis;
    private String detail;
    private String requestId;

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public String getStaffNo() { return staffNo; }
    public void setStaffNo(String staffNo) { this.staffNo = staffNo; }
    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRefType() { return refType; }
    public void setRefType(String refType) { this.refType = refType; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public String getRevokedBy() { return revokedBy; }
    public void setRevokedBy(String revokedBy) { this.revokedBy = revokedBy; }
    public LocalDateTime getRevokedTime() { return revokedTime; }
    public void setRevokedTime(LocalDateTime revokedTime) { this.revokedTime = revokedTime; }
    public String getAffectedMenus() { return affectedMenus; }
    public void setAffectedMenus(String affectedMenus) { this.affectedMenus = affectedMenus; }
    public String getAffectedApis() { return affectedApis; }
    public void setAffectedApis(String affectedApis) { this.affectedApis = affectedApis; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
