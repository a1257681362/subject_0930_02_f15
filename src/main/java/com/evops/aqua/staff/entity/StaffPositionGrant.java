package com.evops.aqua.staff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

import java.time.LocalDateTime;

/**
 * 岗位任职历史：一人可历任多岗，只追加。
 * 岗位变更把旧任职置 SUPERSEDED 保留，新增 ACTIVE 行；历史操作/单据/证据不因调岗删除。
 * source: CERT 凭资质首聘 / POST_CHANGE 组织调岗 / TEMP 临时授权。
 * 状态 ACTIVE 在任 / SUPERSEDED 被新任职取代 / EXPIRED 临时到期 / REVOKED 收回。
 */
@TableName("t_staff_position_grant")
public class StaffPositionGrant extends BaseEntity {

    public static final String SOURCE_CERT = "CERT";
    public static final String SOURCE_POST_CHANGE = "POST_CHANGE";
    public static final String SOURCE_TEMP = "TEMP";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SUPERSEDED = "SUPERSEDED";
    public static final String STATUS_EXPIRED = "EXPIRED";
    public static final String STATUS_REVOKED = "REVOKED";

    private Long staffId;
    private String postCode;
    private String postName;
    private String source;
    private Long refId;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String status;
    private String grantedBy;
    private LocalDateTime grantedTime;
    private LocalDateTime endedTime;
    private String endReason;
    private String menuSnapshot;
    private String apiSnapshot;
    private String requestId;
    private String remark;

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public String getPostCode() { return postCode; }
    public void setPostCode(String postCode) { this.postCode = postCode; }
    public String getPostName() { return postName; }
    public void setPostName(String postName) { this.postName = postName; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Long getRefId() { return refId; }
    public void setRefId(Long refId) { this.refId = refId; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getGrantedBy() { return grantedBy; }
    public void setGrantedBy(String grantedBy) { this.grantedBy = grantedBy; }
    public LocalDateTime getGrantedTime() { return grantedTime; }
    public void setGrantedTime(LocalDateTime grantedTime) { this.grantedTime = grantedTime; }
    public LocalDateTime getEndedTime() { return endedTime; }
    public void setEndedTime(LocalDateTime endedTime) { this.endedTime = endedTime; }
    public String getEndReason() { return endReason; }
    public void setEndReason(String endReason) { this.endReason = endReason; }
    public String getMenuSnapshot() { return menuSnapshot; }
    public void setMenuSnapshot(String menuSnapshot) { this.menuSnapshot = menuSnapshot; }
    public String getApiSnapshot() { return apiSnapshot; }
    public void setApiSnapshot(String apiSnapshot) { this.apiSnapshot = apiSnapshot; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
