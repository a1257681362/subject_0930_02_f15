package com.evops.aqua.staff.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 岗位资质证书登记请求：一证一岗，到期日必填（到期前提醒、过期收权都据此计算）。
 * requestId 幂等：重复提交不产生第二张证。
 */
public class CertificateIssueRequest {

    @NotNull(message = "人员不能为空")
    private Long staffId;
    @NotBlank(message = "证书编号不能为空")
    private String certNo;
    @NotBlank(message = "证书名称不能为空")
    private String certName;
    @NotBlank(message = "岗位编码不能为空（VET/SAMPLER/FISHER/EQUIPMENT）")
    private String postCode;
    private String certAuthor;
    @NotNull(message = "发证日期不能为空")
    private LocalDate issueDate;
    @NotNull(message = "到期日期不能为空")
    private LocalDate expiryDate;
    private Integer remindBeforeDays;
    @NotBlank(message = "请求幂等号不能为空")
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
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
