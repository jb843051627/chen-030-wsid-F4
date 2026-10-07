package com.fc.v2.model.custom.wsid;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 一轮日照扫描的回面：三笔数 + 逐行去向，同出这一趟算定。
 *
 * <p>三笔——本轮捞着几行（{@link #pickedNum}）、走通几行（{@link #sentNum}）、
 * 回落几行（{@link #fallbackNum}）——与逐行沉进库底的记号出自同一次算定，
 * 只信本轮返回，不许另拉一轮逐行清点对冲。一行没捞着是合法结果：
 * {@link #content} 只落「本轮空跑」，不牵首行填，也不抄上一轮那三个数。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
@ApiModel(value = "WsidScanResult", description = "年度核查与换证提醒一轮扫描回面")
public class WsidScanResult {

    @ApiModelProperty(value = "本趟排队先后（同刻两趟分开排队，各留各的先后）")
    private String runSeq;

    @ApiModelProperty(value = "开跑那一刻")
    private Date scanAt;

    @ApiModelProperty(value = "本轮捞着几行（够上条件的行，含静默搁着与同日已发过的对照行）")
    private Integer pickedNum;

    @ApiModelProperty(value = "走通几行（一趟只准走通一式）")
    private Integer sentNum;

    @ApiModelProperty(value = "回落几行（退过两式及以上的行，发不成也算回落过）")
    private Integer fallbackNum;

    @ApiModelProperty(value = "本轮空跑记号（一行没捞着即为真）")
    private boolean empty;

    @ApiModelProperty(value = "答复文面：空跑只落「本轮空跑」；否则三笔数与逐行说法同出这一趟")
    private String content;

    @ApiModelProperty(value = "逐行去向（捞着的每一行一条，与三笔同一次算定）")
    private List<WsidRemindLineOutcome> lines = new ArrayList<WsidRemindLineOutcome>();

    public String getRunSeq() {
        return runSeq;
    }

    public void setRunSeq(String runSeq) {
        this.runSeq = runSeq;
    }

    public Date getScanAt() {
        return scanAt;
    }

    public void setScanAt(Date scanAt) {
        this.scanAt = scanAt;
    }

    public Integer getPickedNum() {
        return pickedNum;
    }

    public void setPickedNum(Integer pickedNum) {
        this.pickedNum = pickedNum;
    }

    public Integer getSentNum() {
        return sentNum;
    }

    public void setSentNum(Integer sentNum) {
        this.sentNum = sentNum;
    }

    public Integer getFallbackNum() {
        return fallbackNum;
    }

    public void setFallbackNum(Integer fallbackNum) {
        this.fallbackNum = fallbackNum;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<WsidRemindLineOutcome> getLines() {
        return lines;
    }

    public void setLines(List<WsidRemindLineOutcome> lines) {
        this.lines = lines;
    }
}
