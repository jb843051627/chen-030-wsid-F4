package com.fc.v2.common.quartz.task;

import com.fc.v2.model.custom.wsid.WsidScanResult;
import com.fc.v2.service.ITWsidRemindItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 年度核查与换证提醒的按日照跑任务。
 *
 * <p>调度器（sys_quartz_job 的 invoke_target 填 {@code wsidRemindTask.remindScan}）
 * 只管点卯；六问全在服务层那一条扫描方法里，这里不另算日子、不另点行数，
 * 只把当轮回面记进日志——三笔数与逐行记号同出那一趟。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
@Component("wsidRemindTask")
public class WsidRemindTask {

    private static final Logger log = LoggerFactory.getLogger(WsidRemindTask.class);

    @Resource
    private ITWsidRemindItemService wsidRemindItemService;

    /** 按日照跑一轮：入参只认当下这一刻 */
    public void remindScan() {
        WsidScanResult result = wsidRemindItemService.scan(null);
        if (result.isEmpty()) {
            log.info("提醒扫描[{}] 本轮空跑", result.getRunSeq());
            return;
        }
        log.info("提醒扫描[{}] {} 逐行 {} 条",
                result.getRunSeq(), result.getContent(), result.getLines().size());
    }
}
