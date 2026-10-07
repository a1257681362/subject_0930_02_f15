package com.evops.aqua.staff.dto;

import javax.validation.constraints.NotBlank;

/**
 * 养殖人员建档请求：一人一档，只建档不授权，上岗需凭岗位资质证书。
 */
public class PersonnelRegisterRequest {

    @NotBlank(message = "人员工号不能为空")
    private String staffNo;
    @NotBlank(message = "人员姓名不能为空")
    private String staffName;
    private String phone;
    private String remark;

    public String getStaffNo() { return staffNo; }
    public void setStaffNo(String staffNo) { this.staffNo = staffNo; }
    public String getStaffName() { return staffName; }
    public void setStaffName(String staffName) { this.staffName = staffName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
