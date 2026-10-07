package com.fc.v2.common.conf;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 提醒静默时段配置（fuce.wsid.remind 下就两栏）。
 *
 * <p>开不开窗只由这两栏说了算：{@code silence-from} 与 {@code silence-to}。
 * 两栏都填着才算开窗（HH:mm 或 HH:mm:ss，讫比起點小即跨午夜）；任一栏空着就是不开窗，
 * 够上条件当日即起话。程序不许自己掐时辰，也不在代码里写死会务期间。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
@Component
@ConfigurationProperties(prefix = "fuce.wsid.remind")
public class WsidRemindProperties {

    /** 静默起（含），HH:mm 或 HH:mm:ss；与 silenceTo 两栏都填着才开窗 */
    private String silenceFrom;

    /** 静默讫（不含）；跨午夜时讫点写得比起點小 */
    private String silenceTo;

    public String getSilenceFrom() {
        return silenceFrom;
    }

    public void setSilenceFrom(String silenceFrom) {
        this.silenceFrom = silenceFrom;
    }

    public String getSilenceTo() {
        return silenceTo;
    }

    public void setSilenceTo(String silenceTo) {
        this.silenceTo = silenceTo;
    }
}
