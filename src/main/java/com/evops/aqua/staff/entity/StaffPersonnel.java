package com.evops.aqua.staff.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.evops.common.BaseEntity;

import java.time.LocalDateTime;

/**
 * 养殖人员档案：一人一档。状态 ACTIVE 在职 / FROZEN 冻结 / LEFT 离场。
 * 冻结、离场、调岗都不删人员行与任何历史操作，只改状态并追加任职/收权记录。
 */
@TableName("t_staff_personnel")
public class StaffPersonnel extends BaseEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FROZEN = "FROZEN";
    public static final String STATUS_LEFT = "LEFT";

    private String staffNo;
    private String staffName;
    private String phone;
    private String status;
    private String currentPostCode;
    private String currentPostName;
    private LocalDateTime frozenTime;
    private String frozenReason;
    private LocalDateTime leftTime;
    private String remark;

    public String getStaffNo() { return staffNo; }
    public void setStaffNo(String staffNo) { this.staffNo = staffNo; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentPostCode() { return currentPostCode; }
    public void setCurrentPostCode(String currentPostCode) { this.currentPostCode = currentPostCode; }
    public String getCurrentPostName() { return currentPostName; }
    public void setCurrentPostName(String currentPostName) { this.currentPostName = currentPostName; }
    public LocalDateTime getFrozenTime() { return frozenTime; }
    public void setFrozenTime(LocalDateTime frozenTime) { this.frozenTime = frozenTime; }
    public String getFrozenReason() { return frozenReason; }
    public void setFrozenReason(String frozenReason) { this.frozenReason = frozenReason; }
    public LocalDateTime getLeftTime() { return leftTime; }
    public void setLeftTime(LocalDateTime leftTime) { this.leftTime = leftTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
