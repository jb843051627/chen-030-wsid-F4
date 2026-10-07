package com.fc.v2.model.custom.wsid;

/**
 * 年度核查与换证提醒一摊的口径定数。
 *
 * <p>够不够日子、走哪一路、静默、去重、缺栏——取舍全写死在此，程序不自己拍时辰、
 * 不自己定提醒日，也不代填栏。老路子签名不动；日后限定单一物种，另立并列路子接。</p>
 *
 * @author fuce
 * @date 2026-10-07
 */
public final class WsidRemindConsts {

    private WsidRemindConsts() {
    }

    /** 条目情形：搁着（还够不上日子；静默里搁着的也仍是此态） */
    public static final int STATUS_WAIT = 0;

    /** 条目情形：已发出（走通了一式） */
    public static final int STATUS_SENT = 1;

    /** 条目情形：发不成（三式挨个退，最后一式也落不了地） */
    public static final int STATUS_FAIL = 2;

    /** 条目情形：文面缺栏，本行按下不发，也不代填 */
    public static final int STATUS_LACK = 3;

    /** 事由名目：年度核查（往应交之日前数） */
    public static final String AFFAIR_CHECK = "年度核查";

    /** 事由名目：换证（往届满之日前数） */
    public static final String AFFAIR_RENEW = "换证";

    /** 通道：站内 */
    public static final int CHAN_STATION = 0;

    /** 通道：短信息 */
    public static final int CHAN_SMS = 1;

    /** 通道：上门告知 */
    public static final int CHAN_VISIT = 2;

    /** 通道取用式 0：只走站内 */
    public static final int KIND_STATION_ONLY = 0;

    /** 通道取用式 1：先站内后短信息 */
    public static final int KIND_STATION_THEN_SMS = 1;

    /** 通道取用式 2：站内、短信息、上门三式挨个退 */
    public static final int KIND_ALL_FALLBACK = 2;

    /**
     * 「至多许提前几日」的定数上限：栏里空着、取负、或大过此数，本行本轮不动，
     * 也不许借别行的数来顶。提醒日的取舍钉死在此。
     */
    public static final int MAX_LEAD_DAYS = 30;

    /** 空跑答复的唯一句：一行没捞着只落此句，不牵首行填、不抄上一轮的数 */
    public static final String EMPTY_TEXT = "本轮空跑";
}
