package com.evops.aqua.staff.controller;

import com.evops.aqua.staff.entity.StaffChangeEvidence;
import com.evops.aqua.staff.entity.StaffPermissionRevoke;
import com.evops.aqua.staff.mapper.StaffPermissionRevokeMapper;
import com.evops.aqua.staff.service.StaffAccessService;
import com.evops.aqua.staff.service.StaffEvidenceService;
import com.evops.aqua.staff.service.StaffQueryService;
import com.evops.common.ApiResponse;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 养殖人员资质授权查询出口：列表、详情、CSV 导出、变更/收权日志。
 * 四者共用 StaffAccessService 同一有效权限口径——验收必须交叉核对，不能只看详情。
 */
@RestController
@RequestMapping("/api/aqua/staff")
public class StaffQueryController {

    private final StaffQueryService queryService;
    private final StaffAccessService accessService;
    private final StaffEvidenceService evidenceService;
    private final StaffPermissionRevokeMapper revokeMapper;

    public StaffQueryController(StaffQueryService queryService,
                                StaffAccessService accessService,
                                StaffEvidenceService evidenceService,
                                StaffPermissionRevokeMapper revokeMapper) {
        this.queryService = queryService;
        this.accessService = accessService;
        this.evidenceService = evidenceService;
        this.revokeMapper = revokeMapper;
    }

    /** 人员权限列表：页面与后台同口径。 */
    @GetMapping("/personnel")
    public ApiResponse<Map<String, Object>> list(@RequestParam(required = false) String status,
                                                 @RequestParam(required = false) String postCode,
                                                 @RequestParam(required = false) Boolean accessOnly) {
        LocalDateTime now = LocalDateTime.now();
        List<Map<String, Object>> rows = queryService.listRows(status, postCode, accessOnly, now);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", rows.size());
        result.put("rows", rows);
        return ApiResponse.ok(result);
    }

    /** 单人详情：档案、有效权限、证书、任职历史、临时授权、提醒。 */
    @GetMapping("/personnel/{staffId}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long staffId) {
        return ApiResponse.ok(queryService.detail(staffId, LocalDateTime.now()));
    }

    /** CSV 导出：与列表/详情同一口径同一过滤条件。 */
    @GetMapping(value = "/export", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String status,
                                         @RequestParam(required = false) String postCode,
                                         @RequestParam(required = false) Boolean accessOnly) {
        String csv = queryService.exportCsv(status, postCode, accessOnly, LocalDateTime.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"staff-qualification-grants.csv\"");
        return ResponseEntity.ok().headers(headers).body(csv.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 现场人员接口实时鉴权：现场 App 带 X-Staff-No 调用任意受管接口前由网关/前端校验，
     * 或直接按本接口判定。证书过期/冻结/临时到期立即拒绝（与列表/导出口径一致）。
     */
    @GetMapping("/access-check")
    public ApiResponse<Map<String, Object>> accessCheck(@RequestHeader("X-Staff-No") String staffNo,
                                                        @RequestParam String uri) {
        StaffAccessService.AccessProfile profile =
                accessService.checkApi(staffNo, uri, LocalDateTime.now());
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("staffNo", profile.getStaffNo());
        view.put("allowed", profile.isAccessActive());
        view.put("denyReason", profile.getDenyReason());
        view.put("activeMenus", accessService.activeMenus(profile));
        return ApiResponse.ok(view);
    }

    /** 变更日志：证书/授权/冻结/收权等全部写操作证据。 */
    @GetMapping("/evidence")
    public ApiResponse<List<StaffChangeEvidence>> evidence(@RequestParam(required = false) String entityType,
                                                           @RequestParam(required = false) Long entityId,
                                                           @RequestParam(required = false) String requestId,
                                                           @RequestParam(required = false) String action) {
        return ApiResponse.ok(evidenceService.list(entityType, entityId, requestId, action));
    }

    /** 权限收回台账：证书过期/临时到期/调岗/冻结/离场逐笔可查，供与详情、日志勾稽。 */
    @GetMapping("/permission-revokes")
    public ApiResponse<List<StaffPermissionRevoke>> revokes(@RequestParam(required = false) Long staffId,
                                                            @RequestParam(required = false) String reason) {
        QueryWrapper<StaffPermissionRevoke> wrapper = new QueryWrapper<>();
        wrapper.eq(staffId != null, "staff_id", staffId);
        wrapper.eq(reason != null && !reason.trim().isEmpty(), "reason", reason);
        wrapper.orderByAsc("id");
        return ApiResponse.ok(revokeMapper.selectList(wrapper));
    }
}
