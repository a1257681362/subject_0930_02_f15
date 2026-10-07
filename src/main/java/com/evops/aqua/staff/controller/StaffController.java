package com.evops.aqua.staff.controller;

import com.evops.aqua.staff.dto.CertificateIssueRequest;
import com.evops.aqua.staff.dto.CertRevokeRequest;
import com.evops.aqua.staff.dto.FreezeRequest;
import com.evops.aqua.staff.dto.OpenTaskRequest;
import com.evops.aqua.staff.dto.PostChangeRequest;
import com.evops.aqua.staff.dto.PostScopeUpdateRequest;
import com.evops.aqua.staff.dto.PersonnelRegisterRequest;
import com.evops.aqua.staff.dto.StaffActionRequest;
import com.evops.aqua.staff.dto.TempGrantRequest;
import com.evops.aqua.staff.entity.StaffCertificate;
import com.evops.aqua.staff.entity.StaffFreezeBatch;
import com.evops.aqua.staff.entity.StaffFreezeImpact;
import com.evops.aqua.staff.entity.StaffOpenTask;
import com.evops.aqua.staff.entity.StaffPersonnel;
import com.evops.aqua.staff.entity.StaffPositionGrant;
import com.evops.aqua.staff.entity.StaffPostScope;
import com.evops.aqua.staff.entity.StaffTempGrant;
import com.evops.aqua.staff.service.StaffService;
import com.evops.common.ApiResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 养殖人员资质与岗位授权写操作：
 * 建档、岗位范围配置、证书登记/撤销、岗位变更、临时授权（必带审批单与结束时间）、
 * 在办任务登记销项、批量冻结（先列影响后冻）、解冻、离场。
 */
@RestController
@RequestMapping("/api/aqua/staff")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @PostMapping("/personnel")
    public ApiResponse<StaffPersonnel> register(@Validated @RequestBody PersonnelRegisterRequest request) {
        return ApiResponse.ok(staffService.register(request));
    }

    @PostMapping("/post-scopes")
    public ApiResponse<StaffPostScope> updateScope(@Validated @RequestBody PostScopeUpdateRequest request) {
        return ApiResponse.ok(staffService.updatePostScope(request));
    }

    @PostMapping("/certificates")
    public ApiResponse<StaffCertificate> issueCert(@Validated @RequestBody CertificateIssueRequest request) {
        return ApiResponse.ok(staffService.issueCertificate(request));
    }

    @PostMapping("/certificates/{certId}/revoke")
    public ApiResponse<StaffCertificate> revokeCert(@PathVariable Long certId,
                                                    @Validated @RequestBody CertRevokeRequest request) {
        return ApiResponse.ok(staffService.revokeCertificate(certId, request));
    }

    @PostMapping("/post-changes")
    public ApiResponse<StaffPositionGrant> changePost(@Validated @RequestBody PostChangeRequest request) {
        return ApiResponse.ok(staffService.changePost(request));
    }

    @PostMapping("/temp-grants")
    public ApiResponse<StaffTempGrant> tempGrant(@Validated @RequestBody TempGrantRequest request) {
        return ApiResponse.ok(staffService.createTempGrant(request));
    }

    @PostMapping("/temp-grants/{tempId}/revoke")
    public ApiResponse<StaffTempGrant> revokeTemp(@PathVariable Long tempId,
                                                  @Validated @RequestBody StaffActionRequest request) {
        return ApiResponse.ok(staffService.revokeTempGrant(tempId, request));
    }

    @PostMapping("/open-tasks")
    public ApiResponse<StaffOpenTask> registerOpenTask(@Validated @RequestBody OpenTaskRequest request) {
        return ApiResponse.ok(staffService.registerOpenTask(request));
    }

    @PostMapping("/open-tasks/{taskId}/finish")
    public ApiResponse<StaffOpenTask> finishOpenTask(@PathVariable Long taskId,
                                                     @Validated @RequestBody StaffActionRequest request) {
        return ApiResponse.ok(staffService.finishOpenTask(taskId, request));
    }

    /** 冻结影响预览：先列出受影响工单与未完成任务（不落库）。 */
    @PostMapping("/freeze/preview")
    public ApiResponse<Map<String, Object>> previewFreeze(@Validated @RequestBody FreezeRequest request) {
        return ApiResponse.ok(staffService.previewFreeze(request));
    }

    /** 确认批量冻结：按同一查询落影响清单并收回权限。 */
    @PostMapping("/freeze")
    public ApiResponse<StaffFreezeBatch> freeze(@Validated @RequestBody FreezeRequest request) {
        return ApiResponse.ok(staffService.freeze(request));
    }

    @GetMapping("/freeze/{batchId}/impacts")
    public ApiResponse<List<StaffFreezeImpact>> freezeImpacts(@PathVariable Long batchId) {
        return ApiResponse.ok(staffService.listFreezeImpacts(batchId));
    }

    @PostMapping("/personnel/{staffNo}/unfreeze")
    public ApiResponse<StaffPersonnel> unfreeze(@PathVariable String staffNo,
                                                @Validated @RequestBody StaffActionRequest request) {
        return ApiResponse.ok(staffService.unfreeze(staffNo, request));
    }

    @PostMapping("/personnel/{staffNo}/leave")
    public ApiResponse<StaffPersonnel> leave(@PathVariable String staffNo,
                                             @Validated @RequestBody StaffActionRequest request) {
        return ApiResponse.ok(staffService.leave(staffNo, request));
    }

    /** 手工触发"到期提醒+过期/到期收权"扫描（验收与运维核查用）。 */
    @PostMapping("/scan/now")
    public ApiResponse<Map<String, Object>> scanNow(@RequestParam(required = false) String operator) {
        return ApiResponse.ok(staffService.runScheduledScan());
    }
}
