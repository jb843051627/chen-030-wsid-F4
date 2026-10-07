package com.fc.v2.model.custom.wsid;

/**
 * 标识申领名册核收一摊的定数。
 *
 * <p>去处只三样，第四样存不进表里；来路只两条，同一串各走一遍各留一面。</p>
 *
 * @author fuce
 * @date 2026-10-06
 */
public final class WsidApplyConsts {

    private WsidApplyConsts() {
    }

    /** 行去处：未勾（两个数没同时落到行上，这一行不办，不代填不按零认） */
    public static final int STATUS_PENDING = 0;

    /** 行去处：收下（两个数齐且摆得平） */
    public static final int STATUS_OK = 1;

    /** 行去处：退回（两个数齐但摆不平，退回缘由写明在备注里） */
    public static final int STATUS_BACK = 2;

    /** 来路：站所自录（头一遍） */
    public static final String PUT_STATION = "站所自录";

    /** 来路：县局代收（第二遍，只对照不改账） */
    public static final String PUT_COUNTY = "县局代收";

    /** 册头那一处空着时按 150 行认 */
    public static final int DEFAULT_ROW_LIMIT = 150;

    /**
     * 册头上限的有量上沿：写成没边的大数（超过此值）与空着等价，按 150 落。
     * 一串册子行数不可能越过此数，越过即视为「没边的大数」。
     */
    public static final int ROW_LIMIT_SANITY = 100000;
}
