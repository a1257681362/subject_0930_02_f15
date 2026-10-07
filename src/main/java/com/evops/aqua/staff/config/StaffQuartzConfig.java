package com.evops.aqua.staff.config;

import com.evops.aqua.staff.service.StaffService;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.CronTriggerFactoryBean;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;

/**
 * 人员资质授权定时任务：证书到期前提醒、证书过期立即收权、临时授权到期收权。
 * 业务幂等：提醒每证每日一条（uk 证书+提醒日）、过期/到期状态机重复扫描不重复收权。
 * misfire 不补触发风暴：错过即等下一周期；验收测试把 cron 推到 2099，手工 POST /scan/now 驱动。
 */
@Configuration
public class StaffQuartzConfig {

    @Bean
    public MethodInvokingJobDetailFactoryBean staffScanJobDetail(StaffService service) {
        MethodInvokingJobDetailFactoryBean factory = new MethodInvokingJobDetailFactoryBean();
        factory.setTargetObject(service);
        factory.setTargetMethod("runScheduledScan");
        factory.setName("staffQualificationScanJob");
        factory.setGroup("aqua-staff");
        factory.setConcurrent(false);
        return factory;
    }

    @Bean
    public CronTriggerFactoryBean staffScanTrigger(
            @Qualifier("staffScanJobDetail") JobDetail jobDetail,
            @Value("${aqua.staff.scan-cron:0 0 1 * * ?}") String cron) {
        CronTriggerFactoryBean factory = new CronTriggerFactoryBean();
        factory.setJobDetail(jobDetail);
        factory.setName("staffQualificationScanTrigger");
        factory.setGroup("aqua-staff");
        factory.setCronExpression(cron);
        factory.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        return factory;
    }
}
