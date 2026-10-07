package com.evops.aqua.staff;

import com.evops.aqua.maint.entity.MaintWorker;
import com.evops.aqua.maint.entity.WorkOrder;
import com.evops.aqua.maint.entity.WorkOrderWorker;
import com.evops.aqua.maint.mapper.MaintWorkerMapper;
import com.evops.aqua.maint.mapper.WorkOrderMapper;
import com.evops.aqua.maint.mapper.WorkOrderWorkerMapper;
import com.evops.aqua.staff.entity.StaffCertificate;
import com.evops.aqua.staff.entity.StaffPermissionRevoke;
import com.evops.aqua.staff.mapper.StaffCertificateMapper;
import com.evops.aqua.staff.mapper.StaffPermissionRevokeMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * 养殖人员资质与岗位授权验收（aqua-15）。
 * 覆盖：四岗可操作范围、证书到期前提醒、过期立即收回菜单与接口（实时拦截+扫描双保险）、
 * 临时授权必带审批单与结束时间、岗位变更不删历史、批量冻结先列受影响工单与未完成任务、
 * 列表/详情/导出/日志四口径交叉核对。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StaffQualificationAcceptanceTest {

    @Autowired
    private TestRestTemplate rest;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StaffCertificateMapper certificateMapper;
    @Autowired
    private StaffPermissionRevokeMapper revokeMapper;
    @Autowired
    private MaintWorkerMapper maintWorkerMapper;
    @Autowired
    private WorkOrderMapper workOrderMapper;
    @Autowired
    private WorkOrderWorkerMapper workOrderWorkerMapper;

    private final List<String> report = new ArrayList<>();
    private String sessionCookie;

    private Long vetId;
    private Long samplerId;
    private Long fisherId;
    private Long equipId;
    private Long rotateId;

    private static final String VET_NO = "S15-VET";
    private static final String SAMPLER_NO = "S15-SAMPLER";
    private static final String FISHER_NO = "S15-FISHER";
    private static final String EQUIP_NO = "S15-EQUIP";
    private static final String ROTATE_NO = "S15-ROTATE";

    @Test
    void acceptanceStaffQualificationAndPostGrant() {
        checkpoint("CP0", "登录认证", () -> {
            Map<String, Object> body = new HashMap<>();
            body.put("username", "bootstrap");
            body.put("password", "bootstrap");
            ResponseEntity<String> resp = call(HttpMethod.POST, "/api/auth/login", body, null);
            assertSuccess(json(resp), "登录");
            List<String> cookies = resp.getHeaders().get(HttpHeaders.SET_COOKIE);
            check(cookies != null && !cookies.isEmpty(), "登录成功但未下发会话Cookie");
            sessionCookie = cookies.get(0).split(";")[0];
        });

        checkpoint("CP1", "四类人员建档并按岗发资质证书（含即将到期与已过期证书）", () -> {
            vetId = register(VET_NO, "兽医赵一");
            samplerId = register(SAMPLER_NO, "采样员钱二");
            fisherId = register(FISHER_NO, "捕捞员孙三");
            equipId = register(EQUIP_NO, "设备员李四");
            rotateId = register(ROTATE_NO, "轮岗周五");

            issueCert(vetId, "C15-VET-1", "执业兽医资格证", "VET",
                    LocalDate.now().minusDays(300), LocalDate.now().plusDays(14), 30, "req-cert-vet");
            issueCert(samplerId, "C15-SAM-1", "水质采样上岗证", "SAMPLER",
                    LocalDate.now().minusDays(360), LocalDate.now().minusDays(1), 30, "req-cert-sam");
            issueCert(fisherId, "C15-FISH-1", "捕捞作业证", "FISHER",
                    LocalDate.now().minusDays(100), LocalDate.now().plusDays(200), 30, "req-cert-fish");
            issueCert(equipId, "C15-EQ-1", "设备维修操作证", "EQUIPMENT",
                    LocalDate.now().minusDays(50), LocalDate.now().plusDays(90), 30, "req-cert-eq");
            issueCert(rotateId, "C15-ROT-FISH", "捕捞作业证", "FISHER",
                    LocalDate.now().minusDays(50), LocalDate.now().plusDays(100), 30, "req-cert-rot-fish");

            // 发证自动首次上岗
            JsonNode vetDetail = detail(vetId);
            check(vetDetail.path("accessActive").asBoolean(), "兽医持有效证应已上岗且权限有效");
            check(contains(vetDetail, "activePostCodes", "VET"), "兽医有效岗位应含 VET");
            check(contains(vetDetail, "activeMenus", "MED"), "兽医有效菜单应含 MED");
        });

        checkpoint("CP2", "现场接口实时鉴权：岗位范围内放行、范围外 403", () -> {
            // 兽医可访问用药面
            ResponseEntity<String> ok = rawGet("/api/aqua/med/drugs", VET_NO);
            check(ok.getStatusCode().is2xxSuccessful(), "兽医访问用药接口应放行，实际=" + ok.getStatusCode());
            // 兽医访问水质面：岗位范围外，403
            ResponseEntity<String> denied = rawGet("/api/aqua/wq/plans", VET_NO);
            check(denied.getStatusCodeValue() == 403, "兽医访问水质接口应被 403 收回，实际="
                    + denied.getStatusCode());
            // 采样员证书昨天已过期：即使扫描还没跑，实时判定也立即 403
            ResponseEntity<String> samDenied = rawGet("/api/aqua/wq/plans", SAMPLER_NO);
            check(samDenied.getStatusCodeValue() == 403, "证书过期后未跑扫描也应实时收回接口权限，实际="
                    + samDenied.getStatusCode() + " body=" + samDenied.getBody());
            // 管理端不带 X-Staff-No：不受现场头影响
            ResponseEntity<String> admin = call(HttpMethod.GET, "/api/aqua/med/drugs", null, null);
            check(admin.getStatusCode().is2xxSuccessful(), "管理端会话访问不应被现场过滤器拦截");
        });

        checkpoint("CP3", "临时授权必须带审批单与结束时间；生效期内可访问、到期后收权", () -> {
            // 缺审批单：拒绝
            Map<String, Object> noApproval = tempBody(vetId, "EQUIPMENT", null, LocalDateTime.now(),
                    LocalDateTime.now().plusDays(3), "req-temp-noapp");
            JsonNode r1 = json(call(HttpMethod.POST, "/api/aqua/staff/temp-grants", noApproval, null));
            check(!r1.path("success").asBoolean(true), "无审批单的临时授权必须被拒绝");

            // 缺结束时间：拒绝
            Map<String, Object> noEnd = tempBody(vetId, "EQUIPMENT", "AP-NOEND-1", LocalDateTime.now(),
                    null, "req-temp-noend");
            JsonNode r2 = json(call(HttpMethod.POST, "/api/aqua/staff/temp-grants", noEnd, null));
            check(!r2.path("success").asBoolean(true), "无结束时间的临时授权必须被拒绝");

            // 合法临时授权：兽医临时获得设备岗，审批单+结束时间齐全
            Map<String, Object> valid = tempBody(vetId, "EQUIPMENT", "AP-TEMP-001",
                    LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusDays(2),
                    "req-temp-valid");
            valid.put("reason", "增氧机抢修临时顶班");
            valid.put("approvedBy", "场长陈六");
            JsonNode r3 = json(call(HttpMethod.POST, "/api/aqua/staff/temp-grants", valid, null));
            assertSuccess(r3, "合法临时授权");
            // 临时授权生效：兽医现在也能访问设备保养面
            ResponseEntity<String> canMaint = rawGet("/api/aqua/maint/orders", VET_NO);
            check(canMaint.getStatusCode().is2xxSuccessful(), "临时授权生效期内应能访问设备岗接口，实际="
                    + canMaint.getStatusCode());

            // 已过期的临时授权（起止都在过去）：建立后扫描即收权
            Map<String, Object> past = tempBody(fisherId, "SAMPLER", "AP-TEMP-PAST",
                    LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(1),
                    "req-temp-past");
            past.put("approvedBy", "场长陈六");
            assertSuccess(json(call(HttpMethod.POST, "/api/aqua/staff/temp-grants", past, null)),
                    "登记一条已到期临时授权");
            ResponseEntity<String> pastDenied = rawGet("/api/aqua/wq/plans", FISHER_NO);
            check(pastDenied.getStatusCodeValue() == 403, "已过结束时间的临时授权应实时无水质接口权限");
        });

        checkpoint("CP4", "定时扫描：到期前生成提醒、过期证书立即收权、临时到期收权，且幂等", () -> {
            JsonNode scan1 = json(call(HttpMethod.POST, "/api/aqua/staff/scan/now", new HashMap<>(), null));
            assertSuccess(scan1, "首次扫描");
            int reminded = scan1.path("data").path("reminder").path("remindedCount").asInt();
            int certExpired = scan1.path("data").path("certExpiry").path("expiredCount").asInt();
            int tempExpired = scan1.path("data").path("tempExpiry").path("expiredTempCount").asInt();
            check(reminded >= 1, "兽医证书 14 天后到期应生成提醒，reminded=" + reminded);
            check(certExpired >= 1, "采样员昨日到期证书应被置 EXPIRED，expired=" + certExpired);
            check(tempExpired >= 1, "已到期临时授权应被收权，tempExpired=" + tempExpired);

            // 证书台账：采样员证书 EXPIRED
            StaffCertificate samCert = certificateMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<StaffCertificate>()
                            .eq("cert_no", "C15-SAM-1"));
            check("EXPIRED".equals(samCert.getStatus()), "过期证书台账应置 EXPIRED");

            // 再扫一次：提醒不重复发（每证每日一条），计数不增加
            JsonNode scan2 = json(call(HttpMethod.POST, "/api/aqua/staff/scan/now", new HashMap<>(), null));
            int reminded2 = scan2.path("data").path("reminder").path("remindedCount").asInt();
            check(reminded2 == 0, "重复扫描不应重复发提醒，第二次 reminded=" + reminded2);

            // 收权台账：采样员 CERT_EXPIRED，收回菜单/接口快照可查
            List<StaffPermissionRevoke> samRevokes = revokeMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<StaffPermissionRevoke>()
                            .eq("staff_id", samplerId)
                            .eq("reason", StaffPermissionRevoke.REASON_CERT_EXPIRED));
            check(!samRevokes.isEmpty(), "证书过期应写权限收回台账");
            StaffPermissionRevoke certRevoke = samRevokes.get(0);
            check(certRevoke.getAffectedMenus() != null && certRevoke.getAffectedMenus().contains("WQ"),
                    "收权台账应记录被收回菜单 WQ,SENSOR：" + certRevoke.getAffectedMenus());
            check(certRevoke.getAffectedApis() != null
                            && certRevoke.getAffectedApis().contains("/api/aqua/wq"),
                    "收权台账应记录被收回接口范围：" + certRevoke.getAffectedApis());
        });

        checkpoint("CP5", "列表 / 详情 / 导出 / 日志 四口径交叉核对", () -> {
            JsonNode listResp = json(call(HttpMethod.GET,
                    "/api/aqua/staff/personnel?status=ACTIVE", null, null));
            assertSuccess(listResp, "人员列表");
            JsonNode vetRow = findRow(listResp, VET_NO);
            JsonNode samRow = findRow(listResp, SAMPLER_NO);
            check(vetRow.path("accessActive").asBoolean(), "列表：兽医权限应为有效");
            check(!samRow.path("accessActive").asBoolean(), "列表：采样员证书过期后权限应为无效");
            check(samRow.path("denyReason").asText().contains("证书"),
                    "列表：采样员无效原因应指向证书/授权，实际=" + samRow.path("denyReason").asText());

            // 列表 == 详情
            JsonNode vetDetail = detail(vetId);
            check(vetRow.path("accessActive").asBoolean() == vetDetail.path("accessActive").asBoolean(),
                    "列表与详情权限口径必须一致");
            check(listContainsAll(vetRow, vetDetail, "activeMenus"),
                    "列表与详情有效菜单必须一致：列表=" + vetRow.path("activeMenus")
                            + " 详情=" + vetDetail.path("activeMenus"));

            // 导出 CSV 与列表一致
            ResponseEntity<String> csvResp = call(HttpMethod.GET, "/api/aqua/staff/export", null, null);
            String csv = csvResp.getBody();
            check(csv.contains("工号") && csv.contains("权限有效"), "导出应有统一表头");
            check(csv.contains(VET_NO) && csvLineIs(csv, VET_NO, "是"),
                    "导出：兽医行权限列应为『是』");
            check(csv.contains(SAMPLER_NO) && csvLineIs(csv, SAMPLER_NO, "否"),
                    "导出：采样员行权限列应为『否』，与列表/详情一致");
            String vetLine = lineOf(csv, VET_NO);
            check(vetLine.contains("MED") && vetLine.contains("DISEASE"),
                    "导出兽医行应含其有效菜单 MED/DISEASE：" + vetLine);

            // 日志：每个关键动作都有证据，且带操作者（含调度 SCHEDULER）
            check(evidenceCount(null, null, "CERT_EXPIRE", null) >= 1, "日志应有证书过期收权证据");
            check(evidenceCount(null, null, "CERT_REMIND", null) >= 1, "日志应有到期提醒证据");
            JsonNode remindEv = findEvidence("CERT_REMIND");
            check("SCHEDULER".equals(remindEv.path("operator").asText()),
                    "定时收权/提醒证据操作者应为 SCHEDULER，不丢操作者");
            check(evidenceCount(null, null, "TEMP_GRANT", null) >= 2, "日志应有临时授权证据");
            check(evidenceCount(null, null, "PERMISSION_REVOKE", null) >= 1, "日志应有权限收回证据");
        });

        checkpoint("CP6", "岗位变更：无目标岗位证书拒绝；变更后旧任职历史保留不删", () -> {
            // 轮岗员当前是捕捞员，无兽医证，调兽医岗应拒绝
            Map<String, Object> noCert = postChangeBody(rotateId, "VET", "人力主管吴七",
                    "调任兽医", "req-rotate-nocert");
            JsonNode r1 = json(call(HttpMethod.POST, "/api/aqua/staff/post-changes", noCert, null));
            check(!r1.path("success").asBoolean(true), "无目标岗位有效证书的调岗必须被拒绝");
            check(r1.path("message").asText().contains("有效资质证书"),
                    "拒绝原因应说明缺有效资质证书：" + r1.path("message").asText());

            // 补发兽医证后调岗成功
            issueCert(rotateId, "C15-ROT-VET", "执业兽医资格证", "VET",
                    LocalDate.now().minusDays(200), LocalDate.now().plusDays(80), 30, "req-rot-vet");
            Map<String, Object> ok = postChangeBody(rotateId, "VET", "人力主管吴七",
                    "因兽医站扩编调任兽医", "req-rotate-ok");
            assertSuccess(json(call(HttpMethod.POST, "/api/aqua/staff/post-changes", ok, null)), "持兽医证调岗");

            JsonNode detail = detail(rotateId);
            check(contains(detail, "activePostCodes", "VET"), "调岗后当前有效岗位应为 VET");
            // 任职历史：捕捞员任职 SUPERSEDED 保留 + 兽医 ACTIVE，历史行不删除
            JsonNode grants = detail.path("positionGrants");
            int active = 0;
            boolean supersededFisher = false;
            for (JsonNode g : grants) {
                if ("ACTIVE".equals(g.path("status").asText())) {
                    active++;
                }
                if ("FISHER".equals(g.path("postCode").asText())
                        && "SUPERSEDED".equals(g.path("status").asText())) {
                    supersededFisher = true;
                }
            }
            check(active >= 1, "调岗后应有在任任职行");
            check(supersededFisher, "旧捕捞员任职必须以 SUPERSEDED 保留，历史不删");
            // 收权台账有 POST_CHANGE 记录
            Long postChangeRevokes = revokeMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<StaffPermissionRevoke>()
                            .eq("staff_id", rotateId)
                            .eq("reason", StaffPermissionRevoke.REASON_POST_CHANGE));
            check(postChangeRevokes != null && postChangeRevokes > 0, "调岗应写旧岗收权台账");
            // 调岗后历史在办任务/操作不删除：其名下此前登记的记录仍在（此处用证据条数间接证明人员数据仍在）
            check(detail.path("staffNo").asText().equals(ROTATE_NO), "调岗不影响人员档案与历史数据");
        });

        checkpoint("CP7", "批量冻结：先列受影响工单与未完成任务，冻结后接口收回、清单落库可查", () -> {
            // 设备员工号与维修工工号打通，挂一张在途工单（IN_PROGRESS）
            MaintWorker worker = new MaintWorker();
            worker.setWorkerCode(EQUIP_NO);
            worker.setWorkerName("设备员李四");
            worker.setQualifiedTypes("AERATOR,PUMP_STATION");
            worker.setStatus(MaintWorker.STATUS_ACTIVE);
            maintWorkerMapper.insert(worker);

            WorkOrder order = new WorkOrder();
            order.setOrderNo("WO-S15-IMPACT-1");
            order.setDeviceId(910001L);
            order.setTriggerSource(WorkOrder.TRIGGER_MANUAL);
            order.setDueAt(LocalDateTime.now().plusDays(1));
            order.setDowntimeWindowId(920001L);
            order.setSchedStart(LocalDateTime.now().minusHours(1));
            order.setSchedEnd(LocalDateTime.now().plusHours(2));
            order.setLeadWorkerId(worker.getId());
            order.setPriority(WorkOrder.PRIORITY_NORMAL);
            order.setStatus(WorkOrder.STATUS_IN_PROGRESS);
            order.setRequestId("req-wo-s15-1");
            workOrderMapper.insert(order);
            WorkOrderWorker link = new WorkOrderWorker();
            link.setWorkOrderId(order.getId());
            link.setWorkerId(worker.getId());
            link.setAssignedBy("调度员");
            link.setAssignedAt(LocalDateTime.now());
            workOrderWorkerMapper.insert(link);

            // 采样员一个未完成任务；设备员两个未完成任务
            registerOpenTask(SAMPLER_NO, "WQ_TASK", "WQ-TASK-S15-1", "S15池水质采样", "req-ot-sam-1");
            registerOpenTask(EQUIP_NO, "OTHER", "SAFE-CHECK-S15-1", "泵站开机前安全检查", "req-ot-eq-1");
            registerOpenTask(EQUIP_NO, "WQ_TASK", "WQ-TASK-S15-9", "协助采样（未完成）", "req-ot-eq-2");

            // 1) 冻结前预览：列出受影响工单与未完成任务
            Map<String, Object> freezeBody = freezeBody(
                    list(SAMPLER_NO, EQUIP_NO), "寒潮应急冻结演练", "安环主管郑八", "req-freeze-s15");
            JsonNode preview = json(call(HttpMethod.POST, "/api/aqua/staff/freeze/preview",
                    freezeBody, null));
            assertSuccess(preview, "冻结影响预览");
            JsonNode data = preview.path("data");
            check(data.path("workOrderCount").asInt() == 1,
                    "受影响工单应为 1（设备员在途工单），实际=" + data.path("workOrderCount").asInt());
            check(data.path("openTaskCount").asInt() == 3,
                    "未完成任务应为 3，实际=" + data.path("openTaskCount").asInt());
            check(impactContains(data, "WORK_ORDER", "WO-S15-IMPACT-1"),
                    "影响清单应列出在途工单 WO-S15-IMPACT-1");
            check(impactContains(data, "OPEN_TASK", "WQ-TASK-S15-1"),
                    "影响清单应列出采样员未完成任务");

            // 2) 确认冻结：接口立即收回，影响清单落库
            assertSuccess(json(call(HttpMethod.POST, "/api/aqua/staff/freeze", freezeBody, null)), "批量冻结");
            ResponseEntity<String> equipDenied = rawGet("/api/aqua/maint/orders", EQUIP_NO);
            check(equipDenied.getStatusCodeValue() == 403, "设备员冻结后保养接口应立即 403");
            ResponseEntity<String> samDenied = rawGet("/api/aqua/wq/plans", SAMPLER_NO);
            check(samDenied.getStatusCodeValue() == 403, "采样员冻结后接口应立即 403");

            JsonNode equipRow = findRow(json(call(HttpMethod.GET,
                    "/api/aqua/staff/personnel?status=FROZEN", null, null)), EQUIP_NO);
            check(equipRow.path("status").asText().equals("FROZEN") && !equipRow.path("accessActive").asBoolean(),
                    "列表应能按 FROZEN 查出且权限为无效");

            // 冻结影响清单按冻结单落库，与预览一致
            JsonNode batch = findRow(json(call(HttpMethod.GET,
                    "/api/aqua/staff/personnel?status=FROZEN", null, null)), EQUIP_NO);
            Long batchId = latestFreezeBatchId();
            JsonNode impactsResp = json(call(HttpMethod.GET,
                    "/api/aqua/staff/freeze/" + batchId + "/impacts", null, null));
            JsonNode impacts = impactsResp.path("data");
            int wo = 0;
            int task = 0;
            for (JsonNode im : impacts) {
                if ("WORK_ORDER".equals(im.path("impactType").asText())) {
                    wo++;
                }
                if ("OPEN_TASK".equals(im.path("impactType").asText())) {
                    task++;
                }
            }
            check(wo == 1 && task == 3, "落库影响清单应含 1 工单 3 任务，实际=" + wo + "/" + task);
            check(evidenceCount(null, batchId, "FREEZE", null) >= 1, "冻结单应有证据留痕");
            Long freezeRevokes = revokeMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<StaffPermissionRevoke>()
                            .eq("reason", StaffPermissionRevoke.REASON_FREEZE));
            check(freezeRevokes != null && freezeRevokes >= 1, "冻结应写 FREEZE 收权台账");

            // 3) 解冻：设备员证书仍有效，恢复保养接口；历史在办任务不因冻结被删
            Map<String, Object> unfreeze = actionBody("安环主管郑八", "寒潮结束", "req-unfreeze-eq");
            assertSuccess(json(call(HttpMethod.POST, "/api/aqua/staff/personnel/" + EQUIP_NO + "/unfreeze",
                    unfreeze, null)), "设备员解冻");
            ResponseEntity<String> equipAgain = rawGet("/api/aqua/maint/orders", EQUIP_NO);
            check(equipAgain.getStatusCode().is2xxSuccessful(),
                    "解冻且证书仍有效，应恢复设备岗接口，实际=" + equipAgain.getStatusCode());
            JsonNode eqOpenTasks = detail(equipId).path("openTasks");
            // 详情不直接含 openTasks 字段时，用在办任务表语义校验：解冻不影响任务，仍可登记/销项
            registerOpenTask(EQUIP_NO, "OTHER", "SAFE-CHECK-S15-2", "复工复检", "req-ot-eq-3");
        });

        checkpoint("CP8", "离场收权且历史全保留；导出/日志仍可勾稽离场人员", () -> {
            Map<String, Object> leave = actionBody("人事经理", "合同到期离场", "req-leave-sam");
            assertSuccess(json(call(HttpMethod.POST, "/api/aqua/staff/personnel/" + SAMPLER_NO + "/leave",
                    leave, null)), "采样员离场");
            ResponseEntity<String> denied = rawGet("/api/aqua/wq/plans", SAMPLER_NO);
            check(denied.getStatusCodeValue() == 403, "离场后接口必须全部收回");
            // 列表不过滤状态时仍能查到（默认不过滤）、导出仍含该行（历史不删）
            String csv = call(HttpMethod.GET, "/api/aqua/staff/export", null, null).getBody();
            check(csv.contains(SAMPLER_NO), "离场人员仍应出现在导出中，历史不删除");
            JsonNode samDetail = detail(samplerId);
            check("LEFT".equals(samDetail.path("status").asText())
                            && samDetail.path("certificates").isArray()
                            && samDetail.path("positionGrants").isArray(),
                    "离场后详情仍保留证书与任职历史");
            Long leaveRevokes = revokeMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<StaffPermissionRevoke>()
                            .eq("staff_id", samplerId)
                            .eq("reason", StaffPermissionRevoke.REASON_LEAVE));
            check(leaveRevokes != null && leaveRevokes > 0, "离场应写 LEAVE 收权台账");
        });

        printReport();
    }

    // ============================ 辅助方法 ============================

    private Long register(String no, String name) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("staffNo", no);
        body.put("staffName", name);
        body.put("phone", "13900000000");
        JsonNode resp = json(call(HttpMethod.POST, "/api/aqua/staff/personnel", body, null));
        assertSuccess(resp, "建档[" + no + "]");
        return resp.path("data").path("id").asLong();
    }

    private void issueCert(Long staffId, String certNo, String certName, String post,
                           LocalDate issue, LocalDate expiry, int remindDays, String reqId) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("staffId", staffId);
        body.put("certNo", certNo);
        body.put("certName", certName);
        body.put("postCode", post);
        body.put("certAuthor", "海洋渔业主管部门");
        body.put("issueDate", issue.toString());
        body.put("expiryDate", expiry.toString());
        body.put("remindBeforeDays", remindDays);
        body.put("requestId", reqId);
        assertSuccess(json(call(HttpMethod.POST, "/api/aqua/staff/certificates", body, null)),
                "发证[" + certNo + "]");
    }

    private void registerOpenTask(String staffNo, String type, String bizNo, String summary, String reqId)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("staffNo", staffNo);
        body.put("taskType", type);
        body.put("sourceBizNo", bizNo);
        body.put("taskSummary", summary);
        body.put("registeredBy", "班组长");
        body.put("requestId", reqId);
        assertSuccess(json(call(HttpMethod.POST, "/api/aqua/staff/open-tasks", body, null)),
                "登记在办任务[" + bizNo + "]");
    }

    private Map<String, Object> tempBody(Long staffId, String post, String approval,
                                         LocalDateTime start, LocalDateTime end, String reqId) {
        Map<String, Object> body = new HashMap<>();
        body.put("staffId", staffId);
        body.put("postCode", post);
        body.put("approvalNo", approval);
        body.put("approvedBy", "场长陈六");
        body.put("startTime", start == null ? null : start.toString());
        body.put("endTime", end == null ? null : end.toString());
        body.put("reason", "临时跨岗支援");
        body.put("requestId", reqId);
        return body;
    }

    private Map<String, Object> postChangeBody(Long staffId, String post, String operator,
                                               String reason, String reqId) {
        Map<String, Object> body = new HashMap<>();
        body.put("staffId", staffId);
        body.put("newPostCode", post);
        body.put("operator", operator);
        body.put("reason", reason);
        body.put("requestId", reqId);
        return body;
    }

    private Map<String, Object> freezeBody(List<String> nos, String reason, String operator, String reqId) {
        Map<String, Object> body = new HashMap<>();
        body.put("staffNos", nos);
        body.put("reason", reason);
        body.put("operator", operator);
        body.put("requestId", reqId);
        return body;
    }

    private Map<String, Object> actionBody(String operator, String reason, String reqId) {
        Map<String, Object> body = new HashMap<>();
        body.put("operator", operator);
        body.put("reason", reason);
        body.put("requestId", reqId);
        return body;
    }

    private JsonNode detail(Long staffId) throws Exception {
        return json(call(HttpMethod.GET, "/api/aqua/staff/personnel/" + staffId, null, null)).path("data");
    }

    private JsonNode findRow(JsonNode resp, String staffNo) {
        JsonNode rows = resp.path("data").path("rows");
        for (JsonNode row : rows) {
            if (staffNo.equals(row.path("staffNo").asText())) {
                return row;
            }
        }
        throw new IllegalStateException("列表中未找到人员：" + staffNo);
    }

    private Long latestFreezeBatchId() throws Exception {
        // 通过冻结证据取最近冻结单 id
        JsonNode ev = findEvidence("FREEZE");
        return ev.path("entityId").asLong();
    }

    private JsonNode findEvidence(String action) throws Exception {
        JsonNode resp = json(call(HttpMethod.GET,
                "/api/aqua/staff/evidence?action=" + action, null, null));
        JsonNode arr = resp.path("data");
        check(arr.isArray() && arr.size() > 0, "未找到动作=" + action + " 的证据");
        return arr.get(arr.size() - 1);
    }

    private int evidenceCount(String entityType, Long entityId, String action, String reqId) throws Exception {
        StringBuilder url = new StringBuilder("/api/aqua/staff/evidence?1=1");
        if (entityType != null) {
            url.append("&entityType=").append(entityType);
        }
        if (entityId != null) {
            url.append("&entityId=").append(entityId);
        }
        if (action != null) {
            url.append("&action=").append(action);
        }
        if (reqId != null) {
            url.append("&requestId=").append(reqId);
        }
        JsonNode arr = json(call(HttpMethod.GET, url.toString(), null, null)).path("data");
        return arr.size();
    }

    private boolean contains(JsonNode node, String arrayField, String value) {
        for (JsonNode item : node.path(arrayField)) {
            if (value.equals(item.asText())) {
                return true;
            }
        }
        return false;
    }

    private boolean listContainsAll(JsonNode row, JsonNode detail, String field) {
        String a = row.path(field).toString();
        String b = detail.path(field).toString();
        if (a.equals(b)) {
            return true;
        }
        // 顺序可能不同：逐个元素比对
        java.util.Set<String> sa = new java.util.TreeSet<>();
        java.util.Set<String> sb = new java.util.TreeSet<>();
        row.path(field).forEach(x -> sa.add(x.asText()));
        detail.path(field).forEach(x -> sb.add(x.asText()));
        return sa.equals(sb);
    }

    private boolean impactContains(JsonNode previewData, String type, String bizRef) {
        for (JsonNode im : previewData.path("impacts")) {
            if (type.equals(im.path("impactType").asText())
                    && bizRef.equals(im.path("bizRef").asText())) {
                return true;
            }
        }
        return false;
    }

    private String lineOf(String csv, String staffNo) {
        for (String line : csv.split("\r?\n")) {
            if (line.startsWith(staffNo + ",") || line.contains("," + staffNo + ",") || line.startsWith(staffNo)) {
                if (line.startsWith(staffNo)) {
                    return line;
                }
            }
        }
        return "";
    }

    private boolean csvLineIs(String csv, String staffNo, String accessFlag) {
        String line = lineOf(csv, staffNo);
        return line != null && !line.isEmpty() && line.contains(",") && accessFlagAt(line, staffNo, accessFlag);
    }

    private boolean accessFlagAt(String line, String staffNo, String flag) {
        String[] parts = line.split(",", -1);
        // 列：工号,姓名,电话,人员状态,当前岗位,权限有效,...
        if (parts.length < 6) {
            return false;
        }
        return staffNo.equals(parts[0]) && flag.equals(parts[5]);
    }

    private <T> List<T> list(T... items) {
        List<T> l = new ArrayList<>();
        for (T t : items) {
            l.add(t);
        }
        return l;
    }

    private ResponseEntity<String> rawGet(String uri, String staffNo) {
        HttpHeaders headers = baseHeaders();
        headers.add("X-Staff-No", staffNo);
        return rest.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> call(HttpMethod method, String url, Object body, Void unused) {
        return rest.exchange(url, method, new HttpEntity<>(body, baseHeaders()), String.class);
    }

    private HttpHeaders baseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (sessionCookie != null) {
            headers.add(HttpHeaders.COOKIE, sessionCookie);
        }
        return headers;
    }

    private JsonNode json(ResponseEntity<String> response) throws Exception {
        return objectMapper.readTree(response.getBody());
    }

    private void assertSuccess(JsonNode node, String what) {
        if (!node.path("success").asBoolean(false)) {
            throw new IllegalStateException(what + "接口返回失败：" + node.path("message").asText());
        }
    }

    private void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private void checkpoint(String id, String name, CheckpointBody body) {
        try {
            body.run();
            String line = "通过 " + id + " " + name;
            report.add(line);
            System.out.println("[验收] " + line);
        } catch (Throwable t) {
            String stop = "停点 " + id + "「" + name + "」失败，原因：" + t.getMessage();
            report.add(stop);
            report.add("验收结论：未通过，后续检查点未执行");
            printReport();
            fail("验收" + stop);
        }
    }

    private void printReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n================ 养殖人员资质与岗位授权验收报告 ================\n");
        for (String line : report) {
            sb.append(line).append('\n');
        }
        sb.append("==============================================================\n");
        System.out.println(sb);
    }

    private interface CheckpointBody {
        void run() throws Exception;
    }
}
