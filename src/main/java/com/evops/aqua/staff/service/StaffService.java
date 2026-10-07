package com.evops.aqua.staff.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.evops.aqua.maint.entity.MaintWorker;
import com.evops.aqua.maint.entity.WorkOrder;
import com.evops.aqua.maint.entity.WorkOrderWorker;
import com.evops.aqua.maint.mapper.MaintWorkerMapper;
import com.evops.aqua.maint.mapper.WorkOrderMapper;
import com.evops.aqua.maint.mapper.WorkOrderWorkerMapper;
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
import com.evops.aqua.staff.entity.StaffChangeEvidence;
import com.evops.aqua.staff.entity.StaffFreezeBatch;
import com.evops.aqua.staff.entity.StaffFreezeImpact;
import com.evops.aqua.staff.entity.StaffOpenTask;
import com.evops.aqua.staff.entity.StaffPermissionRevoke;
import com.evops.aqua.staff.entity.StaffPersonnel;
import com.evops.aqua.staff.entity.StaffPositionGrant;
import com.evops.aqua.staff.entity.StaffPostScope;
import com.evops.aqua.staff.entity.StaffTempGrant;
import com.evops.aqua.staff.mapper.StaffCertificateMapper;
import com.evops.aqua.staff.mapper.StaffCertReminderMapper;
import com.evops.aqua.staff.mapper.StaffFreezeBatchMapper;
import com.evops.aqua.staff.mapper.StaffFreezeImpactMapper;
import com.evops.aqua.staff.mapper.StaffOpenTaskMapper;
import com.evops.aqua.staff.mapper.StaffPermissionRevokeMapper;
import com.evops.aqua.staff.mapper.StaffPersonnelMapper;
import com.evops.aqua.staff.mapper.StaffPositionGrantMapper;
import com.evops.aqua.staff.mapper.StaffPostScopeMapper;
import com.evops.aqua.staff.mapper.StaffTempGrantMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 养殖人员资质与岗位授权主服务。
 * 收权双通道：实时判定（StaffAccessService，证书过期/冻结立即拒绝）+ 定时扫描（置台账状态、写收权台账与证据）。
 * 所有历史只追加不删除：调岗旧任职 SUPERSEDED、证书/临时授权到期保留、冻结与离场不改写历史单据。
 */
@Service
public class StaffService {

    public static final String SCHEDULER_OPERATOR = "SCHEDULER";

    private final StaffPersonnelMapper personnelMapper;
    private final StaffPostScopeMapper postScopeMapper;
    private final StaffCertificateMapper certificateMapper;
    private final StaffCertReminderMapper reminderMapper;
    private final StaffPositionGrantMapper grantMapper;
    private final StaffTempGrantMapper tempGrantMapper;
    private final StaffPermissionRevokeMapper revokeMapper;
    private final StaffOpenTaskMapper openTaskMapper;
    private final StaffFreezeBatchMapper freezeBatchMapper;
    private final StaffFreezeImpactMapper freezeImpactMapper;
    private final StaffAccessService accessService;
    private final StaffEvidenceService evidence;
    private final MaintWorkerMapper maintWorkerMapper;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderWorkerMapper workOrderWorkerMapper;

    public StaffService(StaffPersonnelMapper personnelMapper,
                        StaffPostScopeMapper postScopeMapper,
                        StaffCertificateMapper certificateMapper,
                        StaffCertReminderMapper reminderMapper,
                        StaffPositionGrantMapper grantMapper,
                        StaffTempGrantMapper tempGrantMapper,
                        StaffPermissionRevokeMapper revokeMapper,
                        StaffOpenTaskMapper openTaskMapper,
                        StaffFreezeBatchMapper freezeBatchMapper,
                        StaffFreezeImpactMapper freezeImpactMapper,
                        StaffAccessService accessService,
                        StaffEvidenceService evidence,
                        MaintWorkerMapper maintWorkerMapper,
                        WorkOrderMapper workOrderMapper,
                        WorkOrderWorkerMapper workOrderWorkerMapper) {
        this.personnelMapper = personnelMapper;
        this.postScopeMapper = postScopeMapper;
        this.certificateMapper = certificateMapper;
        this.reminderMapper = reminderMapper;
        this.grantMapper = grantMapper;
        this.tempGrantMapper = tempGrantMapper;
        this.revokeMapper = revokeMapper;
        this.openTaskMapper = openTaskMapper;
        this.freezeBatchMapper = freezeBatchMapper;
        this.freezeImpactMapper = freezeImpactMapper;
        this.accessService = accessService;
        this.evidence = evidence;
        this.maintWorkerMapper = maintWorkerMapper;
        this.workOrderMapper = workOrderMapper;
        this.workOrderWorkerMapper = workOrderWorkerMapper;
    }

    // ============================ 人员建档 ============================

    @Transactional(rollbackFor = Exception.class)
    public StaffPersonnel register(PersonnelRegisterRequest request) {
        Long duplicated = personnelMapper.selectCount(
                new QueryWrapper<StaffPersonnel>().eq("staff_no", request.getStaffNo()));
        if (duplicated != null && duplicated > 0) {
            throw new IllegalArgumentException("人员工号已存在：" + request.getStaffNo());
        }
        StaffPersonnel staff = new StaffPersonnel();
        staff.setStaffNo(request.getStaffNo());
        staff.setStaffName(request.getStaffName());
        staff.setPhone(request.getPhone());
        staff.setRemark(request.getRemark());
        staff.setStatus(StaffPersonnel.STATUS_ACTIVE);
        personnelMapper.insert(staff);
        evidence.record(StaffChangeEvidence.ENTITY_PERSONNEL, staff.getId(),
                StaffChangeEvidence.ACTION_REGISTER, null, "bootstrap",
                null, staff, "养殖人员建档：" + staff.getStaffNo());
        return staff;
    }

    public StaffPersonnel getOrThrow(Long id) {
        StaffPersonnel staff = personnelMapper.selectById(id);
        if (staff == null) {
            throw new IllegalArgumentException("养殖人员不存在，id=" + id);
        }
        return staff;
    }

    // ============================ 岗位可操作范围配置 ============================

    @Transactional(rollbackFor = Exception.class)
    public StaffPostScope updatePostScope(PostScopeUpdateRequest request) {
        String postCode = StaffAccessService.normalizePost(request.getPostCode());
        StaffPostScope scope = accessService.getScopeOrThrow(postCode);
        String beforeMenus = scope.getMenuCodes();
        String beforeApis = scope.getApiScopes();
        scope.setMenuCodes(normalizeCsv(request.getMenuCodes(), "菜单编码"));
        scope.setApiScopes(normalizeCsv(request.getApiScopes(), "接口范围"));
        scope.setChangedReason(request.getChangedReason());
        scope.setOperator(request.getOperator());
        scope.setStatus(StaffPostScope.STATUS_ACTIVE);
        postScopeMapper.updateById(scope);
        evidence.record(StaffChangeEvidence.ENTITY_POST_SCOPE, scope.getId(),
                StaffChangeEvidence.ACTION_UPDATE_SCOPE, null, request.getOperator(),
                map("menus", beforeMenus, "apis", beforeApis),
                map("menus", scope.getMenuCodes(), "apis", scope.getApiScopes()),
                "岗位[" + scope.getPostName() + "]可操作范围调整：" + request.getChangedReason()
                        + "。只影响之后的授权/收权快照，历史授权快照不回改");
        return scope;
    }

    // ============================ 证书登记 / 撤销 ============================

    @Transactional(rollbackFor = Exception.class)
    public StaffCertificate issueCertificate(CertificateIssueRequest request) {
        StaffPersonnel staff = getOrThrow(request.getStaffId());
        if (StaffPersonnel.STATUS_LEFT.equals(staff.getStatus())) {
            throw new IllegalArgumentException("人员已离场，不能再登记上岗证书：" + staff.getStaffNo());
        }
        String postCode = StaffAccessService.normalizePost(request.getPostCode());
        accessService.getScopeOrThrow(postCode);
        if (request.getExpiryDate().isBefore(request.getIssueDate())) {
            throw new IllegalArgumentException("证书到期日期不能早于发证日期");
        }
        StaffCertificate cert = new StaffCertificate();
        cert.setStaffId(staff.getId());
        cert.setCertNo(request.getCertNo());
        cert.setCertName(request.getCertName());
        cert.setPostCode(postCode);
        cert.setCertAuthor(request.getCertAuthor());
        cert.setIssueDate(request.getIssueDate());
        cert.setExpiryDate(request.getExpiryDate());
        cert.setRemindBeforeDays(request.getRemindBeforeDays() == null ? 30 : request.getRemindBeforeDays());
        cert.setStatus(StaffCertificate.STATUS_VALID);
        cert.setRequestId(request.getRequestId());
        cert.setRemark(request.getRemark());
        certificateMapper.insert(cert);
        evidence.record(StaffChangeEvidence.ENTITY_CERTIFICATE, cert.getId(),
                StaffChangeEvidence.ACTION_ISSUE_CERT, request.getRequestId(), "bootstrap",
                null, cert, "登记[" + staff.getStaffNo() + "]岗位[" + postCode + "]资质证书，到期日"
                        + cert.getExpiryDate());

        // 该岗位尚无在任正式授权：凭有效证书首次上岗，冻结当时岗位范围快照
        Long activeFormal = grantMapper.selectCount(new QueryWrapper<StaffPositionGrant>()
                .eq("staff_id", staff.getId())
                .eq("post_code", postCode)
                .eq("status", StaffPositionGrant.STATUS_ACTIVE)
                .in("source", StaffPositionGrant.SOURCE_CERT, StaffPositionGrant.SOURCE_POST_CHANGE));
        if (activeFormal == null || activeFormal == 0) {
            createFormalGrant(staff, postCode, StaffPositionGrant.SOURCE_CERT, null,
                    LocalDateTime.now(), null, "凭资质证书[" + cert.getCertNo() + "]首次上岗",
                    request.getRequestId(), "bootstrap");
            refreshCurrentPost(staff);
        }
        return cert;
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffCertificate revokeCertificate(Long certId, CertRevokeRequest request) {
        StaffCertificate cert = certificateMapper.selectById(certId);
        if (cert == null) {
            throw new IllegalArgumentException("证书不存在，id=" + certId);
        }
        if (!StaffCertificate.STATUS_VALID.equals(cert.getStatus())) {
            throw new IllegalArgumentException("仅有效证书可撤销，当前状态=" + cert.getStatus());
        }
        String before = cert.getStatus();
        cert.setStatus(StaffCertificate.STATUS_REVOKED);
        cert.setRevokedBy(request.getOperator());
        cert.setRevokedTime(LocalDateTime.now());
        cert.setRevokeReason(request.getReason());
        certificateMapper.updateById(cert);
        evidence.record(StaffChangeEvidence.ENTITY_CERTIFICATE, cert.getId(),
                StaffChangeEvidence.ACTION_REVOKE_CERT, request.getRequestId(), request.getOperator(),
                before, cert.getStatus(), "撤销证书[" + cert.getCertNo() + "]：" + request.getReason());

        // 撤销后该岗位若无其他有效证书，立即收回对应正式岗位的菜单与接口权限
        StaffPersonnel staff = getOrThrow(cert.getStaffId());
        if (accessService.validCertificate(staff.getId(), cert.getPostCode(), LocalDate.now()) == null) {
            revokeFormalGrants(staff, cert.getPostCode(),
                    StaffPermissionRevoke.REASON_MANUAL_REVOKE, "CERTIFICATE", cert.getId(),
                    request.getOperator(), "证书[" + cert.getCertNo() + "]被撤销：" + request.getReason(),
                    request.getRequestId());
        }
        return cert;
    }

    // ============================ 岗位变更 ============================

    @Transactional(rollbackFor = Exception.class)
    public StaffPositionGrant changePost(PostChangeRequest request) {
        StaffPersonnel staff = getOrThrow(request.getStaffId());
        requireActive(staff);
        String newPost = StaffAccessService.normalizePost(request.getNewPostCode());
        StaffPostScope scope = accessService.getScopeOrThrow(newPost);
        StaffCertificate cert = accessService.validCertificate(staff.getId(), newPost, LocalDate.now());
        if (cert == null) {
            throw new IllegalArgumentException("调岗失败：人员[" + staff.getStaffNo() + "]无岗位[" + newPost
                    + "]的有效资质证书，不能上岗");
        }

        // 旧在任授权一律置 SUPERSEDED 保留（不删除），逐岗写收权台账；历史操作/单据/证据原样保留
        List<StaffPositionGrant> activeGrants = grantMapper.selectList(new QueryWrapper<StaffPositionGrant>()
                .eq("staff_id", staff.getId())
                .eq("status", StaffPositionGrant.STATUS_ACTIVE));
        for (StaffPositionGrant old : activeGrants) {
            if (newPost.equals(old.getPostCode())
                    && !StaffPositionGrant.SOURCE_TEMP.equals(old.getSource())) {
                continue; // 已在该正式岗：无需自我取代
            }
            String beforeStatus = old.getStatus();
            old.setStatus(StaffPositionGrant.STATUS_SUPERSEDED);
            old.setEndedTime(LocalDateTime.now());
            old.setEndReason("岗位变更为[" + newPost + "]：" + request.getReason());
            grantMapper.updateById(old);
            writeRevokeLedger(staff, old.getPostCode(), StaffPermissionRevoke.REASON_POST_CHANGE,
                    "POSITION_GRANT", old.getId(), request.getOperator(),
                    "调岗前收回岗位[" + old.getPostCode() + "]菜单与接口权限", old.getMenuSnapshot(),
                    old.getApiSnapshot(), request.getRequestId() + "#revoke-" + old.getId());
            evidence.record(StaffChangeEvidence.ENTITY_GRANT, old.getId(),
                    StaffChangeEvidence.ACTION_POST_CHANGE, request.getRequestId(), request.getOperator(),
                    beforeStatus, old.getStatus(),
                    "旧任职[" + old.getPostCode() + "/" + old.getSource() + "]因调岗置 SUPERSEDED，历史保留不删");
        }

        StaffPositionGrant grant = createFormalGrant(staff, newPost, StaffPositionGrant.SOURCE_POST_CHANGE,
                null, LocalDateTime.now(), null,
                "组织调岗，依据有效证书[" + cert.getCertNo() + "]：" + request.getReason(),
                request.getRequestId(), request.getOperator());
        staff.setCurrentPostCode(newPost);
        staff.setCurrentPostName(scope.getPostName());
        personnelMapper.updateById(staff);
        evidence.record(StaffChangeEvidence.ENTITY_PERSONNEL, staff.getId(),
                StaffChangeEvidence.ACTION_POST_CHANGE, request.getRequestId(), request.getOperator(),
                map("post", staff.getCurrentPostCode()), map("post", newPost),
                "人员[" + staff.getStaffNo() + "]岗位变更为[" + newPost + "]，历史操作不删除");
        return grant;
    }

    // ============================ 临时授权 ============================

    @Transactional(rollbackFor = Exception.class)
    public StaffTempGrant createTempGrant(TempGrantRequest request) {
        StaffPersonnel staff = getOrThrow(request.getStaffId());
        requireActive(staff);
        String postCode = StaffAccessService.normalizePost(request.getPostCode());
        StaffPostScope scope = accessService.getScopeOrThrow(postCode);
        // 临时授权必须带结束时间与审批单：注解兜底非空，此处再拦业务非法值
        if (request.getApprovalNo() == null || request.getApprovalNo().trim().isEmpty()) {
            throw new IllegalArgumentException("临时授权必须带审批单号");
        }
        if (request.getEndTime() == null) {
            throw new IllegalArgumentException("临时授权必须带结束时间");
        }
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new IllegalArgumentException("临时授权结束时间必须晚于开始时间");
        }
        Long approvalDup = tempGrantMapper.selectCount(
                new QueryWrapper<StaffTempGrant>().eq("approval_no", request.getApprovalNo()));
        if (approvalDup != null && approvalDup > 0) {
            throw new IllegalArgumentException("审批单号已被使用：" + request.getApprovalNo());
        }

        StaffTempGrant temp = new StaffTempGrant();
        temp.setStaffId(staff.getId());
        temp.setPostCode(postCode);
        temp.setPostName(scope.getPostName());
        temp.setApprovalNo(request.getApprovalNo());
        temp.setApprovedBy(request.getApprovedBy());
        temp.setStartTime(request.getStartTime());
        temp.setEndTime(request.getEndTime());
        temp.setReason(request.getReason());
        temp.setScopeMenuSnapshot(scope.getMenuCodes());
        temp.setScopeApiSnapshot(scope.getApiScopes());
        temp.setStatus(StaffTempGrant.STATUS_ACTIVE);
        temp.setRequestId(request.getRequestId());
        tempGrantMapper.insert(temp);

        StaffPositionGrant grant = new StaffPositionGrant();
        grant.setStaffId(staff.getId());
        grant.setPostCode(postCode);
        grant.setPostName(scope.getPostName());
        grant.setSource(StaffPositionGrant.SOURCE_TEMP);
        grant.setRefId(temp.getId());
        grant.setValidFrom(temp.getStartTime());
        grant.setValidTo(temp.getEndTime());
        grant.setStatus(StaffPositionGrant.STATUS_ACTIVE);
        grant.setGrantedBy(request.getApprovedBy());
        grant.setGrantedTime(LocalDateTime.now());
        grant.setMenuSnapshot(scope.getMenuCodes());
        grant.setApiSnapshot(scope.getApiScopes());
        grant.setRequestId(request.getRequestId() + "#grant");
        grant.setRemark("临时授权，审批单" + temp.getApprovalNo());
        grantMapper.insert(grant);

        evidence.record(StaffChangeEvidence.ENTITY_TEMP_GRANT, temp.getId(),
                StaffChangeEvidence.ACTION_TEMP_GRANT, request.getRequestId(), request.getApprovedBy(),
                null, temp, "临时授权[" + staff.getStaffNo() + "]岗位[" + postCode + "]，审批单"
                        + temp.getApprovalNo() + "，有效期[" + temp.getStartTime() + "," + temp.getEndTime()
                        + ")，到期自动收权");
        return temp;
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffTempGrant revokeTempGrant(Long tempId, StaffActionRequest request) {
        StaffTempGrant temp = tempGrantMapper.selectById(tempId);
        if (temp == null) {
            throw new IllegalArgumentException("临时授权不存在，id=" + tempId);
        }
        if (!StaffTempGrant.STATUS_ACTIVE.equals(temp.getStatus())) {
            throw new IllegalArgumentException("仅生效中的临时授权可提前撤销，当前=" + temp.getStatus());
        }
        if (request.getOperator() == null || request.getOperator().trim().isEmpty()) {
            throw new IllegalArgumentException("操作人不能为空");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("提前撤销必须登记原因");
        }
        LocalDateTime now = LocalDateTime.now();
        temp.setStatus(StaffTempGrant.STATUS_REVOKED);
        temp.setRevokedBy(request.getOperator());
        temp.setRevokedTime(now);
        temp.setRevokeReason(request.getReason());
        tempGrantMapper.updateById(temp);
        endTempGrants(temp, StaffPositionGrant.STATUS_REVOKED, now,
                "临时授权提前撤销：" + request.getReason(), request.getOperator(),
                StaffPermissionRevoke.REASON_MANUAL_REVOKE, request.getRequestId());
        evidence.record(StaffChangeEvidence.ENTITY_TEMP_GRANT, temp.getId(),
                StaffChangeEvidence.ACTION_TEMP_REVOKE, request.getRequestId(), request.getOperator(),
                StaffTempGrant.STATUS_ACTIVE, temp.getStatus(),
                "审批单[" + temp.getApprovalNo() + "]临时授权提前撤销，立即收回菜单与接口");
        return temp;
    }

    // ============================ 在办任务 ============================

    @Transactional(rollbackFor = Exception.class)
    public StaffOpenTask registerOpenTask(OpenTaskRequest request) {
        StaffPersonnel staff = accessService.getByStaffNoOrThrow(request.getStaffNo());
        String postCode = staff.getCurrentPostCode() == null ? "TEMP" : staff.getCurrentPostCode();
        StaffOpenTask task = new StaffOpenTask();
        task.setStaffId(staff.getId());
        task.setPostCode(postCode);
        task.setTaskType(request.getTaskType().trim().toUpperCase());
        task.setSourceBizNo(request.getSourceBizNo());
        task.setSourceBizId(request.getSourceBizId());
        task.setTaskSummary(request.getTaskSummary());
        task.setStatus(StaffOpenTask.STATUS_OPEN);
        task.setRegisteredBy(request.getRegisteredBy());
        task.setRegisteredTime(LocalDateTime.now());
        task.setRequestId(request.getRequestId());
        openTaskMapper.insert(task);
        evidence.record(StaffChangeEvidence.ENTITY_OPEN_TASK, task.getId(),
                StaffChangeEvidence.ACTION_OPEN_TASK_REGISTER, request.getRequestId(),
                request.getRegisteredBy(), null, task,
                "登记[" + staff.getStaffNo() + "]在办任务：" + task.getTaskType() + "/" + task.getSourceBizNo());
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffOpenTask finishOpenTask(Long taskId, StaffActionRequest request) {
        StaffOpenTask task = openTaskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("在办任务不存在，id=" + taskId);
        }
        if (!StaffOpenTask.STATUS_OPEN.equals(task.getStatus())) {
            throw new IllegalArgumentException("任务已销项，不能重复完成");
        }
        task.setStatus(StaffOpenTask.STATUS_FINISHED);
        task.setFinishedBy(request.getOperator() == null ? "bootstrap" : request.getOperator());
        task.setFinishedTime(LocalDateTime.now());
        openTaskMapper.updateById(task);
        evidence.record(StaffChangeEvidence.ENTITY_OPEN_TASK, task.getId(),
                StaffChangeEvidence.ACTION_OPEN_TASK_FINISH, request.getRequestId(),
                task.getFinishedBy(), StaffOpenTask.STATUS_OPEN, task.getStatus(),
                "在办任务[" + task.getSourceBizNo() + "]销项");
        return task;
    }

    // ============================ 批量冻结：先列后冻 ============================

    /**
     * 冻结影响预览：逐人列出受影响工单（设备在途工单实时反查保养工单表）与未完成任务。
     * 预览不落库，供确认前核对；确认冻结时按同一查询落影响清单。
     */
    public Map<String, Object> previewFreeze(FreezeRequest request) {
        return buildFreezeImpact(request.getStaffNos(), null);
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffFreezeBatch freeze(FreezeRequest request) {
        LocalDateTime now = LocalDateTime.now();
        List<StaffPersonnel> staffList = resolveStaff(request.getStaffNos());

        StaffFreezeBatch batch = new StaffFreezeBatch();
        batch.setBatchNo("FZ" + now.getYear() + pad2(now.getMonthValue()) + pad2(now.getDayOfMonth())
                + "-" + Math.abs(request.getRequestId().hashCode()));
        batch.setStaffCount(staffList.size());
        List<String> nos = new ArrayList<>();
        for (StaffPersonnel s : staffList) {
            nos.add(s.getStaffNo());
        }
        batch.setStaffNosSnapshot(String.join(",", nos));
        batch.setReason(request.getReason());
        batch.setOperator(request.getOperator());
        batch.setFrozenTime(now);
        batch.setRequestId(request.getRequestId());
        freezeBatchMapper.insert(batch);

        int impactSeq = 0;
        for (StaffPersonnel staff : staffList) {
            // 落影响清单：受影响工单 + 未完成任务
            Map<String, Object> impact = buildFreezeImpact(Arrays.asList(staff.getStaffNo()), batch.getId());
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = (List<Map<String, Object>>) impact.get("impacts");
            for (Map<String, Object> item : items) {
                StaffFreezeImpact row = new StaffFreezeImpact();
                row.setFreezeBatchId(batch.getId());
                row.setStaffId(staff.getId());
                row.setStaffNo(staff.getStaffNo());
                row.setStaffName(staff.getStaffName());
                row.setImpactType((String) item.get("impactType"));
                row.setSourceModule((String) item.get("sourceModule"));
                row.setBizRef((String) item.get("bizRef"));
                row.setBizStatus((String) item.get("bizStatus"));
                row.setDetail((String) item.get("detail"));
                freezeImpactMapper.insert(row);
                impactSeq++;
            }

            // 冻结：人员状态置 FROZEN，实时判定立即收回全部菜单与接口；不改动任职行/证书/历史单据
            String beforeStatus = staff.getStatus();
            staff.setStatus(StaffPersonnel.STATUS_FROZEN);
            staff.setFrozenTime(now);
            staff.setFrozenReason(request.getReason());
            personnelMapper.updateById(staff);

            // 在任授权范围写收权台账（授权行保留，解冻后仍按证书有效期恢复）
            List<StaffPositionGrant> activeGrants = grantMapper.selectList(new QueryWrapper<StaffPositionGrant>()
                    .eq("staff_id", staff.getId())
                    .eq("status", StaffPositionGrant.STATUS_ACTIVE));
            if (activeGrants.isEmpty()) {
                // 即便此前证书已过期等导致无在任授权行，冻结也必须留一条人员级收权记录，台账不缺人
                writeRevokeLedger(staff, staff.getCurrentPostCode(), StaffPermissionRevoke.REASON_FREEZE,
                        "PERSONNEL", staff.getId(), request.getOperator(),
                        "批量冻结[" + batch.getBatchNo() + "]，人员冻结（冻结前已无在任有效授权），全部菜单与接口收回",
                        null, null, request.getRequestId() + "#revoke-personnel-" + staff.getId());
            }
            for (StaffPositionGrant g : activeGrants) {
                writeRevokeLedger(staff, g.getPostCode(), StaffPermissionRevoke.REASON_FREEZE,
                        "POSITION_GRANT", g.getId(), request.getOperator(),
                        "批量冻结[" + batch.getBatchNo() + "]，在岗期间收回菜单与接口权限",
                        g.getMenuSnapshot(), g.getApiSnapshot(),
                        request.getRequestId() + "#revoke-" + g.getId());
            }
            evidence.record(StaffChangeEvidence.ENTITY_PERSONNEL, staff.getId(),
                    StaffChangeEvidence.ACTION_FREEZE, request.getRequestId(), request.getOperator(),
                    beforeStatus, staff.getStatus(), "人员[" + staff.getStaffNo() + "]冻结，受影响项 "
                            + countOf(items, "WORK_ORDER") + " 个工单、"
                            + countOf(items, "OPEN_TASK") + " 个未完成任务，见冻结单" + batch.getBatchNo());
        }
        evidence.record(StaffChangeEvidence.ENTITY_FREEZE, batch.getId(),
                StaffChangeEvidence.ACTION_FREEZE, request.getRequestId(), request.getOperator(),
                null, batch, "批量冻结单" + batch.getBatchNo() + "，冻结 " + staffList.size()
                        + " 人，影响清单 " + impactSeq + " 行");
        return batch;
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffPersonnel unfreeze(String staffNo, StaffActionRequest request) {
        StaffPersonnel staff = accessService.getByStaffNoOrThrow(staffNo);
        if (!StaffPersonnel.STATUS_FROZEN.equals(staff.getStatus())) {
            throw new IllegalArgumentException("仅冻结状态人员可解冻，当前=" + staff.getStatus());
        }
        String before = staff.getStatus();
        staff.setStatus(StaffPersonnel.STATUS_ACTIVE);
        staff.setFrozenTime(null);
        staff.setFrozenReason(null);
        personnelMapper.updateById(staff);
        evidence.record(StaffChangeEvidence.ENTITY_PERSONNEL, staff.getId(),
                StaffChangeEvidence.ACTION_UNFREEZE, request.getRequestId(),
                request.getOperator() == null ? "bootstrap" : request.getOperator(),
                before, staff.getStatus(),
                "人员[" + staff.getStaffNo() + "]解冻，按仍在有效期内的证书/临时授权恢复菜单与接口权限");
        return staff;
    }

    @Transactional(rollbackFor = Exception.class)
    public StaffPersonnel leave(String staffNo, StaffActionRequest request) {
        StaffPersonnel staff = accessService.getByStaffNoOrThrow(staffNo);
        if (StaffPersonnel.STATUS_LEFT.equals(staff.getStatus())) {
            throw new IllegalArgumentException("人员已离场，不能重复离场");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("离场必须登记原因");
        }
        LocalDateTime now = LocalDateTime.now();
        String before = staff.getStatus();
        staff.setStatus(StaffPersonnel.STATUS_LEFT);
        staff.setLeftTime(now);
        personnelMapper.updateById(staff);

        List<StaffPositionGrant> activeGrants = grantMapper.selectList(new QueryWrapper<StaffPositionGrant>()
                .eq("staff_id", staff.getId())
                .eq("status", StaffPositionGrant.STATUS_ACTIVE));
        String operator = request.getOperator() == null ? "bootstrap" : request.getOperator();
        if (activeGrants.isEmpty()) {
            // 离场前可能已因证书过期/冻结而无在任授权行：仍写一条人员级 LEAVE 收权记录，台账不缺人
            writeRevokeLedger(staff, staff.getCurrentPostCode(), StaffPermissionRevoke.REASON_LEAVE,
                    "PERSONNEL", staff.getId(), operator,
                    "人员离场收回全部菜单与接口权限（离场前已无在任有效授权，历史操作保留不删）",
                    null, null, request.getRequestId() + "#revoke-personnel-" + staff.getId());
        }
        for (StaffPositionGrant g : activeGrants) {
            g.setStatus(StaffPositionGrant.STATUS_REVOKED);
            g.setEndedTime(now);
            g.setEndReason("人员离场：" + request.getReason());
            grantMapper.updateById(g);
            writeRevokeLedger(staff, g.getPostCode(), StaffPermissionRevoke.REASON_LEAVE,
                    "POSITION_GRANT", g.getId(), operator,
                    "人员离场收回全部菜单与接口权限（历史操作保留不删）",
                    g.getMenuSnapshot(), g.getApiSnapshot(),
                    request.getRequestId() + "#revoke-" + g.getId());
        }
        evidence.record(StaffChangeEvidence.ENTITY_PERSONNEL, staff.getId(),
                StaffChangeEvidence.ACTION_LEAVE, request.getRequestId(), operator,
                before, staff.getStatus(), "人员[" + staff.getStaffNo() + "]离场，任职历史与历史操作全部保留");
        return staff;
    }

    public List<StaffFreezeImpact> listFreezeImpacts(Long freezeBatchId) {
        return freezeImpactMapper.selectList(new QueryWrapper<StaffFreezeImpact>()
                .eq("freeze_batch_id", freezeBatchId).orderByAsc("id"));
    }

    // ============================ 定时扫描：到期提醒 + 过期收权 ============================

    /** Quartz 入口：经代理调用，事务生效。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runScheduledScan() {
        LocalDateTime now = LocalDateTime.now();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scanAt", now);
        result.put("reminder", runReminderScan(now.toLocalDate()));
        result.put("certExpiry", runExpiryScan(now));
        result.put("tempExpiry", runTempExpiryScan(now));
        return result;
    }

    /**
     * 到期前提醒：进入 [expiry - remindBeforeDays, expiry] 的有效证书每证每日只生成一条提醒。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runReminderScan(LocalDate today) {
        List<StaffCertificate> certs = certificateMapper.selectList(new QueryWrapper<StaffCertificate>()
                .eq("status", StaffCertificate.STATUS_VALID)
                .le("expiry_date", today.plusDays(60L))
                .ge("expiry_date", today)
                .orderByAsc("expiry_date"));
        int reminded = 0;
        for (StaffCertificate cert : certs) {
            int window = cert.getRemindBeforeDays() == null ? 30 : cert.getRemindBeforeDays();
            int daysTo = (int) java.time.temporal.ChronoUnit.DAYS.between(today, cert.getExpiryDate());
            if (daysTo > window) {
                continue; // 尚未进入该证提醒窗口
            }
            Long dup = reminderMapper.selectCount(new QueryWrapper<com.evops.aqua.staff.entity.StaffCertReminder>()
                    .eq("certificate_id", cert.getId())
                    .eq("remind_date", today));
            if (dup != null && dup > 0) {
                continue; // 每证每日一条，幂等
            }
            com.evops.aqua.staff.entity.StaffCertReminder reminder =
                    new com.evops.aqua.staff.entity.StaffCertReminder();
            reminder.setCertificateId(cert.getId());
            reminder.setStaffId(cert.getStaffId());
            reminder.setRemindDate(today);
            reminder.setExpiryDate(cert.getExpiryDate());
            reminder.setDaysToExpiry(daysTo);
            reminder.setStatus(com.evops.aqua.staff.entity.StaffCertReminder.STATUS_REMINDED);
            reminder.setRequestId("REMIND#" + cert.getId() + "#" + today);
            reminderMapper.insert(reminder);
            reminded++;
            StaffPersonnel staff = getOrThrow(cert.getStaffId());
            evidence.record(StaffChangeEvidence.ENTITY_CERTIFICATE, cert.getId(),
                    StaffChangeEvidence.ACTION_CERT_REMIND, reminder.getRequestId(), SCHEDULER_OPERATOR,
                    null, reminder, "证书[" + cert.getCertNo() + "]到期提醒：人员[" + staff.getStaffNo()
                            + "]岗位[" + cert.getPostCode() + "]距到期还有 " + daysTo + " 天（"
                            + cert.getExpiryDate() + "），过期将立即收回菜单与接口权限");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scanDate", today);
        result.put("remindedCount", reminded);
        return result;
    }

    /**
     * 过期收权扫描：到期日早于今天的有效证书置 EXPIRED，立即收回对应正式岗位菜单与接口。
     * 实时判定（StaffAccessService）即使本扫描未跑也已拒绝；扫描负责台账状态、收权台账与证据。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runExpiryScan(LocalDateTime now) {
        LocalDate today = now.toLocalDate();
        List<StaffCertificate> expiredCerts = certificateMapper.selectList(new QueryWrapper<StaffCertificate>()
                .eq("status", StaffCertificate.STATUS_VALID)
                .lt("expiry_date", today)
                .orderByAsc("expiry_date"));
        int expired = 0;
        for (StaffCertificate cert : expiredCerts) {
            String before = cert.getStatus();
            cert.setStatus(StaffCertificate.STATUS_EXPIRED);
            certificateMapper.updateById(cert);
            StaffPersonnel staff = getOrThrow(cert.getStaffId());
            evidence.record(StaffChangeEvidence.ENTITY_CERTIFICATE, cert.getId(),
                    StaffChangeEvidence.ACTION_CERT_EXPIRE,
                    "EXPIRE#" + cert.getId() + "#" + today, SCHEDULER_OPERATOR,
                    before, cert.getStatus(), "证书[" + cert.getCertNo() + "]已于" + cert.getExpiryDate()
                            + "到期，系统立即收回岗位[" + cert.getPostCode() + "]菜单与接口权限");
            // 该岗位若无其他有效证书，正式任职收权
            if (accessService.validCertificate(staff.getId(), cert.getPostCode(), today) == null) {
                revokeFormalGrants(staff, cert.getPostCode(), StaffPermissionRevoke.REASON_CERT_EXPIRED,
                        "CERTIFICATE", cert.getId(), SCHEDULER_OPERATOR,
                        "证书[" + cert.getCertNo() + "]到期未续，立即收回岗位[" + cert.getPostCode()
                                + "]菜单与接口权限",
                        "EXPIRE#" + cert.getId() + "#" + today);
            }
            expired++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scanAt", now);
        result.put("expiredCount", expired);
        return result;
    }

    /** 临时授权到期扫描：endTime <= now 的 ACTIVE 临时授权置 EXPIRED 并收权。 */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runTempExpiryScan(LocalDateTime now) {
        List<StaffTempGrant> dues = tempGrantMapper.selectList(new QueryWrapper<StaffTempGrant>()
                .eq("status", StaffTempGrant.STATUS_ACTIVE)
                .le("end_time", now)
                .orderByAsc("end_time"));
        int expired = 0;
        for (StaffTempGrant temp : dues) {
            temp.setStatus(StaffTempGrant.STATUS_EXPIRED);
            tempGrantMapper.updateById(temp);
            endTempGrants(temp, StaffPositionGrant.STATUS_EXPIRED, now,
                    "临时授权到期（结束时间" + temp.getEndTime() + "），系统自动收权",
                    SCHEDULER_OPERATOR, StaffPermissionRevoke.REASON_TEMP_EXPIRED,
                    "TEMPEXPIRE#" + temp.getId() + "#" + now.toLocalDate());
            evidence.record(StaffChangeEvidence.ENTITY_TEMP_GRANT, temp.getId(),
                    StaffChangeEvidence.ACTION_TEMP_EXPIRE,
                    "TEMPEXPIRE#" + temp.getId() + "#" + now.toLocalDate(), SCHEDULER_OPERATOR,
                    StaffTempGrant.STATUS_ACTIVE, temp.getStatus(),
                    "审批单[" + temp.getApprovalNo() + "]临时授权到期，立即收回菜单与接口");
            expired++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scanAt", now);
        result.put("expiredTempCount", expired);
        return result;
    }

    // ============================ 内部辅助 ============================

    private StaffPositionGrant createFormalGrant(StaffPersonnel staff, String postCode, String source,
                                                 Long refId, LocalDateTime from, LocalDateTime to,
                                                 String remark, String requestId, String operator) {
        StaffPostScope scope = accessService.getScopeOrThrow(postCode);
        StaffPositionGrant grant = new StaffPositionGrant();
        grant.setStaffId(staff.getId());
        grant.setPostCode(postCode);
        grant.setPostName(scope.getPostName());
        grant.setSource(source);
        grant.setRefId(refId);
        grant.setValidFrom(from);
        grant.setValidTo(to);
        grant.setStatus(StaffPositionGrant.STATUS_ACTIVE);
        grant.setGrantedBy(operator);
        grant.setGrantedTime(LocalDateTime.now());
        grant.setMenuSnapshot(scope.getMenuCodes());
        grant.setApiSnapshot(scope.getApiScopes());
        grant.setRequestId(requestId + "#grant");
        grant.setRemark(remark);
        grantMapper.insert(grant);
        evidence.record(StaffChangeEvidence.ENTITY_GRANT, grant.getId(),
                StaffChangeEvidence.ACTION_GRANT, requestId, operator, null, grant,
                "授权[" + staff.getStaffNo() + "]岗位[" + postCode + "/" + source + "]，菜单="
                        + scope.getMenuCodes() + "，接口=" + scope.getApiScopes());
        return grant;
    }

    /** 收回某岗位全部在任正式授权（证书过期/撤销），写收权台账与证据。 */
    private void revokeFormalGrants(StaffPersonnel staff, String postCode, String reason,
                                    String refType, Long refId, String operator, String detail,
                                    String requestId) {
        List<StaffPositionGrant> grants = grantMapper.selectList(new QueryWrapper<StaffPositionGrant>()
                .eq("staff_id", staff.getId())
                .eq("post_code", postCode)
                .eq("status", StaffPositionGrant.STATUS_ACTIVE)
                .in("source", StaffPositionGrant.SOURCE_CERT, StaffPositionGrant.SOURCE_POST_CHANGE));
        LocalDateTime now = LocalDateTime.now();
        String grantEndStatus = StaffPermissionRevoke.REASON_CERT_EXPIRED.equals(reason)
                ? StaffPositionGrant.STATUS_EXPIRED : StaffPositionGrant.STATUS_REVOKED;
        for (StaffPositionGrant g : grants) {
            g.setStatus(grantEndStatus);
            g.setEndedTime(now);
            g.setEndReason(detail);
            grantMapper.updateById(g);
            writeRevokeLedger(staff, postCode, reason, refType, refId, operator, detail,
                    g.getMenuSnapshot(), g.getApiSnapshot(),
                    requestId + "#revoke-" + g.getId());
            evidence.record(StaffChangeEvidence.ENTITY_REVOKE, g.getId(),
                    StaffChangeEvidence.ACTION_PERMISSION_REVOKE, requestId, operator,
                    StaffPositionGrant.STATUS_ACTIVE, g.getStatus(),
                    detail + "；收回菜单=" + g.getMenuSnapshot() + "，接口=" + g.getApiSnapshot());
        }
    }

    private void endTempGrants(StaffTempGrant temp, String grantStatus, LocalDateTime now, String detail,
                               String operator, String revokeReason, String requestId) {
        List<StaffPositionGrant> grants = grantMapper.selectList(new QueryWrapper<StaffPositionGrant>()
                .eq("ref_id", temp.getId())
                .eq("source", StaffPositionGrant.SOURCE_TEMP)
                .eq("status", StaffPositionGrant.STATUS_ACTIVE));
        StaffPersonnel staff = getOrThrow(temp.getStaffId());
        for (StaffPositionGrant g : grants) {
            g.setStatus(grantStatus);
            g.setEndedTime(now);
            g.setEndReason(detail);
            grantMapper.updateById(g);
            writeRevokeLedger(staff, temp.getPostCode(), revokeReason, "TEMP_GRANT", temp.getId(),
                    operator, detail, temp.getScopeMenuSnapshot(), temp.getScopeApiSnapshot(),
                    requestId + "#revoke-" + g.getId());
            evidence.record(StaffChangeEvidence.ENTITY_REVOKE, g.getId(),
                    StaffChangeEvidence.ACTION_PERMISSION_REVOKE, requestId, operator,
                    StaffPositionGrant.STATUS_ACTIVE, g.getStatus(),
                    detail + "；收回菜单=" + temp.getScopeMenuSnapshot() + "，接口=" + temp.getScopeApiSnapshot());
        }
    }

    private void writeRevokeLedger(StaffPersonnel staff, String postCode, String reason, String refType,
                                   Long refId, String operator, String detail,
                                   String menus, String apis, String requestId) {
        StaffPermissionRevoke revoke = new StaffPermissionRevoke();
        revoke.setStaffId(staff.getId());
        revoke.setStaffNo(staff.getStaffNo());
        revoke.setPostCode(postCode);
        revoke.setReason(reason);
        revoke.setRefType(refType);
        revoke.setRefId(refId);
        revoke.setRevokedBy(operator);
        revoke.setRevokedTime(LocalDateTime.now());
        revoke.setAffectedMenus(menus);
        revoke.setAffectedApis(apis);
        revoke.setDetail(detail);
        revoke.setRequestId(requestId);
        revokeMapper.insert(revoke);
    }

    private void refreshCurrentPost(StaffPersonnel staff) {
        List<StaffPositionGrant> active = grantMapper.selectList(new QueryWrapper<StaffPositionGrant>()
                .eq("staff_id", staff.getId())
                .eq("status", StaffPositionGrant.STATUS_ACTIVE)
                .ne("source", StaffPositionGrant.SOURCE_TEMP)
                .orderByDesc("id"));
        if (!active.isEmpty()) {
            staff.setCurrentPostCode(active.get(0).getPostCode());
            staff.setCurrentPostName(active.get(0).getPostName());
            personnelMapper.updateById(staff);
        }
    }

    private void requireActive(StaffPersonnel staff) {
        if (!StaffPersonnel.STATUS_ACTIVE.equals(staff.getStatus())) {
            throw new IllegalArgumentException("人员当前状态[" + staff.getStatus() + "]不允许执行该操作");
        }
    }

    private List<StaffPersonnel> resolveStaff(List<String> staffNos) {
        List<StaffPersonnel> result = new ArrayList<>();
        List<String> distinct = new ArrayList<>();
        for (String no : staffNos) {
            if (!distinct.contains(no)) {
                distinct.add(no);
            }
        }
        for (String no : distinct) {
            result.add(accessService.getByStaffNoOrThrow(no));
        }
        return result;
    }

    /**
     * 计算冻结影响：OPEN 未完成任务（t_staff_open_task）+ 受影响工单（设备员实时反查保养在途工单）。
     * freezeBatchId 非空表示落库阶段（此时同一查询结果落清单）。
     */
    private Map<String, Object> buildFreezeImpact(List<String> staffNos, Long freezeBatchId) {
        List<StaffPersonnel> staffList = resolveStaff(staffNos);
        List<Map<String, Object>> impacts = new ArrayList<>();
        int workOrderTotal = 0;
        int openTaskTotal = 0;
        List<String> unfinishedStatus = Arrays.asList(
                WorkOrder.STATUS_PLANNED, WorkOrder.STATUS_ASSIGNED, WorkOrder.STATUS_IN_PROGRESS,
                WorkOrder.STATUS_COMPLETED);

        for (StaffPersonnel staff : staffList) {
            // 1) 未完成任务：登记在人名下仍 OPEN 的跨模块事项
            List<StaffOpenTask> openTasks = openTaskMapper.selectList(new QueryWrapper<StaffOpenTask>()
                    .eq("staff_id", staff.getId())
                    .eq("status", StaffOpenTask.STATUS_OPEN)
                    .orderByAsc("id"));
            for (StaffOpenTask t : openTasks) {
                impacts.add(impact(staff, StaffFreezeImpact.TYPE_OPEN_TASK, moduleOf(t.getTaskType()),
                        t.getSourceBizNo(), StaffOpenTask.STATUS_OPEN,
                        (t.getTaskSummary() == null ? "" : t.getTaskSummary())
                                + "（登记于" + t.getRegisteredTime() + "）"));
                openTaskTotal++;
            }

            // 2) 受影响工单：设备员工号与维修工工号一致时，实时反查保养在途工单（含派工与主修）
            MaintWorker worker = maintWorkerMapper.selectOne(
                    new QueryWrapper<MaintWorker>().eq("worker_code", staff.getStaffNo()));
            if (worker != null) {
                List<Long> orderIds = new ArrayList<>();
                List<WorkOrderWorker> links = workOrderWorkerMapper.selectList(
                        new QueryWrapper<WorkOrderWorker>().eq("worker_id", worker.getId()));
                for (WorkOrderWorker link : links) {
                    if (!orderIds.contains(link.getWorkOrderId())) {
                        orderIds.add(link.getWorkOrderId());
                    }
                }
                List<WorkOrder> leadOrders = workOrderMapper.selectList(
                        new QueryWrapper<WorkOrder>().eq("lead_worker_id", worker.getId()));
                for (WorkOrder o : leadOrders) {
                    if (!orderIds.contains(o.getId())) {
                        orderIds.add(o.getId());
                    }
                }
                for (Long orderId : orderIds) {
                    WorkOrder order = workOrderMapper.selectById(orderId);
                    if (order == null || !unfinishedStatus.contains(order.getStatus())) {
                        continue; // 已验收关闭的工单不受冻结影响
                    }
                    impacts.add(impact(staff, StaffFreezeImpact.TYPE_WORK_ORDER, "MAINT",
                            order.getOrderNo(), order.getStatus(),
                            "设备" + (order.getDeviceId() == null ? "" : "#" + order.getDeviceId())
                                    + " 保养工单，完工期限" + order.getDueAt()
                                    + "，冻结后需先交接或处置"));
                    workOrderTotal++;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("staffCount", staffList.size());
        result.put("workOrderCount", workOrderTotal);
        result.put("openTaskCount", openTaskTotal);
        result.put("impacts", impacts);
        return result;
    }

    private Map<String, Object> impact(StaffPersonnel staff, String type, String module,
                                       String bizRef, String bizStatus, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("staffNo", staff.getStaffNo());
        m.put("staffName", staff.getStaffName());
        m.put("impactType", type);
        m.put("sourceModule", module);
        m.put("bizRef", bizRef);
        m.put("bizStatus", bizStatus);
        m.put("detail", detail);
        return m;
    }

    private String moduleOf(String taskType) {
        if (StaffOpenTask.TYPE_WQ_TASK.equals(taskType)) {
            return "WQ";
        }
        if (StaffOpenTask.TYPE_DISEASE_FOLLOWUP.equals(taskType)) {
            return "DISEASE";
        }
        if (StaffOpenTask.TYPE_FISHING_RESERVATION.equals(taskType)) {
            return "FISHING";
        }
        return "OTHER";
    }

    private int countOf(List<Map<String, Object>> items, String type) {
        int n = 0;
        for (Map<String, Object> item : items) {
            if (type.equals(item.get("impactType"))) {
                n++;
            }
        }
        return n;
    }

    private String normalizeCsv(String csv, String field) {
        List<String> list = StaffAccessService.splitCsv(csv);
        if (list.isEmpty()) {
            throw new IllegalArgumentException(field + "不能为空");
        }
        return String.join(",", list);
    }

    private Map<String, Object> map(String k1, Object v1) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        return m;
    }

    private Map<String, Object> map(String k1, Object v1, String k2, Object v2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return m;
    }

    private String pad2(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }
}
