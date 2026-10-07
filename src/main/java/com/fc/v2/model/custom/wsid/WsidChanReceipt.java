package com.fc.v2.model.custom.wsid;

/**
 * 一式通道的回执：说没说得通 + 留痕的那一句。
 *
 * @author fuce
 * @date 2026-10-07
 */
public class WsidChanReceipt {

    /** 这一式说没说得通 */
    private final boolean ok;

    /** 回执那一句（通与不通都要留痕，前一式的旧记号不许擦） */
    private final String note;

    public WsidChanReceipt(boolean ok, String note) {
        this.ok = ok;
        this.note = note;
    }

    public static WsidChanReceipt fail(String note) {
        return new WsidChanReceipt(false, note);
    }

    public static WsidChanReceipt done(String note) {
        return new WsidChanReceipt(true, note);
    }

    public boolean isOk() {
        return ok;
    }

    public String getNote() {
        return note;
    }
}
