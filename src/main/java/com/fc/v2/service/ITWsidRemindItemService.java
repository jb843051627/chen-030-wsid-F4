package com.fc.v2.service;

import java.util.Date;
import java.util.List;

import com.fc.v2.model.auto.TWsidRemindItem;
import com.fc.v2.model.custom.wsid.WsidScanResult;

/**
 * 年度核查与换证提醒条目 Service。
 *
 * <p>六问只许 {@link #scan(Date)} 这一条扫描方法开口：本轮捞得着哪几行、话从哪一路递出去、
 * 静默那一截算不算当日起话、隔日再扫认不认作同一桩、行上文面短了栏走哪条道、末了三笔数
 * 打哪儿来——都在这里定夺。旁处不留第二道门：页面不许先把数亮出来回头再找依据，
 * 三笔数一律取当轮扫描返回的那一份，别轮逐行点出来的数顶不上用。</p>
 *
 * <p>入参出参就此钉住：入参只一个「开跑那一刻」（{@code null} 即以当下那一刻开跑）；
 * 出参只一份 {@link WsidScanResult}（三笔数＋逐行记号，同一次算定）。老路子不改签名；
 * 日后要把一趟圈在单一物种底下，另立一个并列名目去接，不许在这条本体上动手术。</p>
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITWsidRemindItemService {

    /** 按主键回查条目（老路子，签名不动） */
    TWsidRemindItem selectTWsidRemindItemById(Long id);

    /** 该时刻够得上条件的行（老路子，签名不动；够不够日子的口径以 scan 为准） */
    List<TWsidRemindItem> listDue(Date at);

    /**
     * 老路子：执行一次，返回走通条数。签名不动，内部即一轮 {@link #scan(Date)}。
     */
    int runOnce(Date at);

    /**
     * 一轮日照扫描——唯一一道门。一轮只认开跑那一刻：
     * 挑行（至多许提前几日，空着/取负/大过定数上限本轮不动）、通道三式挨个退且逐式留痕、
     * 静默窗（配置两栏）够条件不当日起话但仍记当初那一瞬、缺栏只停本行并原样报缺哪几栏、
     * 去重键「一处主体＋一桩事由＋一日」同日只一回、三笔数与逐行记号同一次算定。
     *
     * @param at 开跑那一刻；null 取当下
     * @return 本轮回面（一行没捞着只落「本轮空跑」）
     */
    WsidScanResult scan(Date at);
}
