package com.evops.aqua.staff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

import java.time.LocalDateTime;

/**
 * 批量冻结单：一次批量冻结一张，冻结人员编号留快照，逐人影响项见 t_staff_freeze_impact。
 */
@TableName("t_staff_freeze_batch")
public class StaffFreezeBatch extends BaseEntity {

    private String batchNo;
    private Integer staffCount;
    private String staffNosSnapshot;
    private String reason;
    private String operator;
    private LocalDateTime frozenTime;
    private String requestId;

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Integer getStaffCount() { return staffCount; }
    public void setStaffCount(Integer staffCount) { this.staffCount = staffCount; }
    public String getStaffNosSnapshot() { return staffNosSnapshot; }
    public void setStaffNosSnapshot(String staffNosSnapshot) { this.staffNosSnapshot = staffNosSnapshot; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public LocalDateTime getFrozenTime() { return frozenTime; }
    public void setFrozenTime(LocalDateTime frozenTime) { this.frozenTime = frozenTime; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
}
