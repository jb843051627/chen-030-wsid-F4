package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fc.v2.common.support.ConvertUtil;
import com.fc.v2.mapper.auto.TWsidSpeciesBookMapper;
import com.fc.v2.mapper.auto.TWsidTplBookMapper;
import com.fc.v2.model.auto.TWsidSpeciesBook;
import com.fc.v2.model.auto.TWsidTplBook;
import com.fc.v2.service.ITWsidTplBookService;
import com.fc.v2.util.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 证面版式模板 Service 业务层处理——建版取式只认这一处，控制层与页面不另辟径路。
 *
 * <p>六段规矩的落点：</p>
 * <ol>
 *   <li>建行：代号由系统排上（{@link #nextTplNo()}），递来的一概不听；物种、层别名目、
 *       版式名目三样一同撞上而册上那一条仍立着（在位未删），新建挡回去；递来的正是
 *       那一条自己则按改一处处理，转手给 {@link #updateTWsidTplBook}。槽位文面空着的
 *       行进不了库，提示点名「槽位文面」这一栏。</li>
 *   <li>取用只三式（0 只出正联／1 只出副本联／2 顺层回落）；一屏联数量程只认 1..3，
 *       量程外的数视同没填。</li>
 *   <li>取式回落：{@link #resolveTWsidTplBook} 顺继承链 县定→市定→省定→系统兜底
 *       一层层上去，三级都空才落兜底版；兜底版只出正联。同名目并立几行先挑最后落笔
 *       （ver_no 大）的那条，落笔分不出先后再比代号串字面，取靠后那条。按层代号问与
 *       通表翻查同走这一处核心，两种说法无从分叉。</li>
 *   <li>完成比不留填数的空位：每回落库之前拿应发、已渲重轧一遍（两位小数），盖掉上一轮
 *       存的那个；头一趟建行、已渲被更正、挪挂别的物种，三回都走到同一处重算。应发空着、
 *       带负号、小于已渲，或已渲反超应发，这一行进不了库。</li>
 *   <li>情形只两态（0 在位／1 已撤下）：撤下之后只剩一张脸——文面改不了，整行也抽不走，
 *       单条与成批同走 {@link #removeOne} 一处说法。撤下的行不再上对外排面，但仍旧列得
 *       出来、仍旧计入在册套数。冗余的物种称谓与底册对不起来时，以底册为准回写。</li>
 *   <li>物种代号对不出名录底册建不成行（提示写明是代号这一栏）；已调出的物种段不再收
 *       新行，旧行照旧数得着。检索三个口子：代号、名目认起头几个字，文面认全文关键词，
 *       全等与模糊各开一路。层级范围在 {@link #scopeOf} 一处切：本层值守只见本层，
 *       省局岗三层都见但限于划归物种；一段没划归就空着回话，不从邻层牵行来填。</li>
 * </ol>
 *
 * @author fuce
 * @date 2026-09-12
 */
@Service
public class TWsidTplBookServiceImpl extends ServiceImpl<TWsidTplBookMapper, TWsidTplBook> implements ITWsidTplBookService {

    /** 定档层级：县定 */
    public static final String LEVEL_COUNTY = "县定";
    /** 定档层级：市定 */
    public static final String LEVEL_CITY = "市定";
    /** 定档层级：省定 */
    public static final String LEVEL_PROVINCE = "省定";
    /** 定档层级：系统兜底 */
    public static final String LEVEL_SYSTEM = "系统兜底";

    /** 继承链：回落按此次序一层层上去，三级都空才落系统兜底 */
    private static final String[] LEVEL_CHAIN = {LEVEL_COUNTY, LEVEL_CITY, LEVEL_PROVINCE, LEVEL_SYSTEM};

    /** 取用式：只出正联 */
    public static final int STYLE_MAIN_ONLY = 0;
    /** 取用式：只出副本联 */
    public static final int STYLE_COPY_ONLY = 1;
    /** 取用式：顺层回落 */
    public static final int STYLE_FALLBACK = 2;

    /** 行的情形：在位 */
    public static final int STATUS_ACTIVE = 0;
    /** 行的情形：已撤下 */
    public static final int STATUS_WITHDRAWN = 1;

    /** 一屏联数量程下沿 */
    private static final int STYLE_NUM_MIN = 1;
    /** 一屏联数量程上沿 */
    private static final int STYLE_NUM_MAX = 3;

    @Resource
    private TWsidSpeciesBookMapper wsidSpeciesBookMapper;

    // ---------------------------------------------------------------- 读

    @Override
    public TWsidTplBook selectTWsidTplBookById(Long id) {
        if (id == null) {
            return null;
        }
        TWsidTplBook row = this.baseMapper.selectOne(new QueryWrapper<TWsidTplBook>()
                .eq("id", id)
                .eq("del_flag", 0));
        if (row != null) {
            reconcileSpec(java.util.Collections.singletonList(row));
        }
        return row;
    }

    /**
     * 通表读法只此一条，定死不许改出口：删除标记是硬闸，其余条件全听调用方带来的
     * Wrapper；分页由调用方统一处理，服务层不另起一屏。撤下的行仍旧列得出来（不按
     * 情形过滤）、仍旧计入在册套数；存量行与新行同走这一读法。
     */
    @Override
    public List<TWsidTplBook> selectTWsidTplBookList(Wrapper<TWsidTplBook> queryWrapper) {
        Wrapper<TWsidTplBook> w = queryWrapper == null ? new QueryWrapper<TWsidTplBook>() : queryWrapper;
        if (w instanceof QueryWrapper) {
            ((QueryWrapper<TWsidTplBook>) w).eq("del_flag", 0);
        }
        List<TWsidTplBook> rows = this.baseMapper.selectList(w);
        // 不管来路怎么写，划掉的行不出这个口
        rows.removeIf(r -> r == null || (r.getDelFlag() != null && r.getDelFlag() != 0));
        reconcileSpec(rows);
        return rows;
    }

    /**
     * 模糊检索那一路：物种代号、层别名目只认起头那几个字（前匹配），槽位文面认全文
     * 关键词（包含）。层级与物种范围在此切死；本层此刻一行未立，空着回话是合法结果。
     */
    @Override
    public List<TWsidTplBook> searchTWsidTplBook(TWsidTplBook query, String levelScope, List<Integer> specScope) {
        QueryWrapper<TWsidTplBook> q = scopeOf(levelScope, specScope);
        if (q == null) {
            return new ArrayList<TWsidTplBook>();
        }
        if (query != null) {
            if (StringUtils.isNotBlank(query.getSpecNo())) {
                q.likeRight("spec_no", query.getSpecNo().trim());
            }
            if (StringUtils.isNotBlank(query.getLevelKind())) {
                q.likeRight("level_kind", query.getLevelKind().trim());
            }
            if (StringUtils.isNotBlank(query.getTplText())) {
                q.like("tpl_text", query.getTplText().trim());
            }
        }
        q.orderByAsc("id");
        List<TWsidTplBook> rows = this.baseMapper.selectList(q);
        reconcileSpec(rows);
        return rows;
    }

    /**
     * 全等检索那一路：三个口子都给全等。与模糊那一路各开一路，互不通气。
     */
    @Override
    public List<TWsidTplBook> matchTWsidTplBook(TWsidTplBook query, String levelScope, List<Integer> specScope) {
        QueryWrapper<TWsidTplBook> q = scopeOf(levelScope, specScope);
        if (q == null) {
            return new ArrayList<TWsidTplBook>();
        }
        if (query != null) {
            if (StringUtils.isNotBlank(query.getSpecNo())) {
                q.eq("spec_no", query.getSpecNo().trim());
            }
            if (StringUtils.isNotBlank(query.getLevelKind())) {
                q.eq("level_kind", query.getLevelKind().trim());
            }
            if (StringUtils.isNotBlank(query.getTplText())) {
                q.eq("tpl_text", query.getTplText().trim());
            }
        }
        q.orderByAsc("id");
        List<TWsidTplBook> rows = this.baseMapper.selectList(q);
        reconcileSpec(rows);
        return rows;
    }

    /**
     * 取式：顺继承链 县定→市定→省定→系统兜底 取说话的那一版。寻不见行不答空白，
     * 一层层退上去；三级都空才落系统兜底，兜底版只出正联（不许顺手取成全出副本联）。
     * 年底复核按这一条逐条回看，缺槽位时回落到哪一版，看回来那一行的定档层级就指得出来。
     */
    @Override
    public TWsidTplBook resolveTWsidTplBook(Integer specId, String tplName) {
        return resolveFrom(specId, tplName, null);
    }

    // ---------------------------------------------------------------- 建行与改行

    /**
     * 新建一行。代号由系统排上，不留敲键的格子；三样撞车而册上那一条仍立着就挡回去，
     * 递来的正是那一条自己则按改一处处理。
     */
    @Override
    public int insertTWsidTplBook(TWsidTplBook record) {
        if (record == null) {
            return 0;
        }

        // 槽位文面空着的行进不了库——提示点名哪一栏不合
        if (StringUtils.isBlank(record.getTplText())) {
            throw new IllegalArgumentException("槽位文面（tpl_text）空着，这一行进不了库");
        }

        // 代号拿到名录底册里对不出账，这一行建不成——提示写明是代号这一栏
        if (record.getSpecId() == null) {
            throw new IllegalArgumentException("物种代号（spec_id）空着，名录底册里对不出账");
        }
        TWsidSpeciesBook spec = findSpecies(record.getSpecId());
        if (spec == null) {
            throw new IllegalArgumentException("物种代号（spec_id=" + record.getSpecId() + "）在名录底册里对不出账");
        }

        // 三样一同撞上而册上那一条仍立着：挡回去；递来的正是那一条自己，按改一处处理
        List<TWsidTplBook> standing = findStanding(record.getSpecId(), record.getLevelKind(), record.getTplName());
        if (!standing.isEmpty()) {
            boolean self = record.getId() != null && standing.stream().anyMatch(r -> r.getId().equals(record.getId()));
            if (self) {
                return updateTWsidTplBook(record);
            }
            return 0;
        }

        // 已调出名录的物种段再也收不进新行（调出之前立在库里的照旧数得着）
        if (spec.getStatus() != null && spec.getStatus() == 1) {
            return 0;
        }

        // 取用只三式；情形只两态
        if (record.getTplStyle() != null && !validStyle(record.getTplStyle())) {
            return 0;
        }
        if (record.getStatus() != null && !validStatus(record.getStatus())) {
            return 0;
        }

        // 应发／已渲把门：空着、带负号、已渲反超应发，都进不了库
        Integer plan = record.getPlanNum();
        int done = record.getDoneNum() == null ? 0 : record.getDoneNum();
        if (plan == null || plan < 0 || done < 0 || done > plan) {
            return 0;
        }

        // 代号由系统排上，递来的一概不听
        record.setTplNo(nextTplNo());

        // 一屏并列排出的联数量程只认 1..3，量程外视同没填
        if (record.getStyleNum() == null || record.getStyleNum() < STYLE_NUM_MIN || record.getStyleNum() > STYLE_NUM_MAX) {
            record.setStyleNum(null);
        }

        // 完成比轧出来盖掉，不留填数的空位（头一趟建行即重算头一回）
        record.setCoverNum(coverOf(plan, done));

        // 冗余代号与称谓以底册为准回写，两处不各摆一套
        record.setSpecNo(spec.getSpecNo());
        record.setSpecName(spec.getSpecName());

        if (record.getStatus() == null) {
            record.setStatus(STATUS_ACTIVE);
        }
        record.setDelFlag(0);
        record.setCreateBy(null);
        record.setCreateTime(null);
        record.setUpdateBy(null);
        record.setUpdateTime(null);
        return this.baseMapper.insert(record);
    }

    /**
     * 改一处。撤下之后只剩一张脸：文面改不了。挪挂到别的物种底下是重算的三回之一；
     * 完成比每回落库之前重轧一遍，盖掉上一轮存的那一个。
     */
    @Override
    public int updateTWsidTplBook(TWsidTplBook record) {
        if (record == null || record.getId() == null) {
            return 0;
        }
        TWsidTplBook cur = this.baseMapper.selectOne(new QueryWrapper<TWsidTplBook>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
        if (cur == null) {
            return 0;
        }
        // 撤下之后只剩一张脸——文面改不了
        if (cur.getStatus() != null && cur.getStatus() == STATUS_WITHDRAWN) {
            return 0;
        }

        // 槽位文面递了空白：空着的行进不了库（点名哪一栏）
        if (record.getTplText() != null && StringUtils.isBlank(record.getTplText())) {
            throw new IllegalArgumentException("槽位文面（tpl_text）空着，这一行进不了库");
        }

        // 挪挂到别的物种底下：代号先对账，已调出的段不收；没挪挂也照旧以底册回写冗余称谓
        Integer effSpecId = cur.getSpecId();
        if (record.getSpecId() != null && !record.getSpecId().equals(cur.getSpecId())) {
            TWsidSpeciesBook spec = findSpecies(record.getSpecId());
            if (spec == null) {
                throw new IllegalArgumentException("物种代号（spec_id=" + record.getSpecId() + "）在名录底册里对不出账");
            }
            if (spec.getStatus() != null && spec.getStatus() == 1) {
                return 0;
            }
            effSpecId = record.getSpecId();
            record.setSpecNo(spec.getSpecNo());
            record.setSpecName(spec.getSpecName());
        } else {
            TWsidSpeciesBook spec = findSpecies(effSpecId);
            if (spec != null) {
                record.setSpecNo(spec.getSpecNo());
                record.setSpecName(spec.getSpecName());
            }
        }

        // 有效三样撞上册上别的立着的行：不许改出第二条来
        String effLevel = record.getLevelKind() != null ? record.getLevelKind() : cur.getLevelKind();
        String effName = record.getTplName() != null ? record.getTplName() : cur.getTplName();
        List<TWsidTplBook> standing = findStanding(effSpecId, effLevel, effName);
        boolean other = standing.stream().anyMatch(r -> !r.getId().equals(cur.getId()));
        if (other) {
            return 0;
        }

        // 取用只三式；情形只两态
        if (record.getTplStyle() != null && !validStyle(record.getTplStyle())) {
            return 0;
        }
        if (record.getStatus() != null && !validStatus(record.getStatus())) {
            return 0;
        }

        // 一屏联数量程外视同没填——这一栏这趟不动
        if (record.getStyleNum() != null && (record.getStyleNum() < STYLE_NUM_MIN || record.getStyleNum() > STYLE_NUM_MAX)) {
            record.setStyleNum(null);
        }

        // 代号系统排定，改行不许顺手改号；情形走推进那一趟，不从改行挪；删除标记不从这趟动
        record.setTplNo(null);
        record.setStatus(null);
        record.setDelFlag(null);

        // 应发／已渲按有效值把门；完成比每回落库之前重轧一遍，盖掉上一轮存的
        Integer effPlan = record.getPlanNum() != null ? record.getPlanNum() : cur.getPlanNum();
        int effDone = record.getDoneNum() != null ? record.getDoneNum()
                : (cur.getDoneNum() == null ? 0 : cur.getDoneNum());
        if (effPlan == null || effPlan < 0 || effDone < 0 || effDone > effPlan) {
            return 0;
        }
        record.setCoverNum(coverOf(effPlan, effDone));

        record.setCreateBy(null);
        record.setCreateTime(null);
        record.setUpdateBy(null);
        record.setUpdateTime(null);
        return this.baseMapper.update(record, new UpdateWrapper<TWsidTplBook>()
                .eq("id", record.getId())
                .eq("del_flag", 0));
    }

    // ---------------------------------------------------------------- 情形两态

    /**
     * 推进一态：在位 → 已撤下。情形只两态，已撤下无处可推。
     */
    @Override
    public int advanceTWsidTplBook(Long id) {
        if (id == null) {
            return 0;
        }
        TWsidTplBook cur = this.baseMapper.selectOne(new QueryWrapper<TWsidTplBook>()
                .eq("id", id)
                .eq("del_flag", 0));
        if (cur == null) {
            return 0;
        }
        int st = cur.getStatus() == null ? STATUS_ACTIVE : cur.getStatus();
        if (st != STATUS_ACTIVE) {
            return 0;
        }
        UpdateWrapper<TWsidTplBook> uw = new UpdateWrapper<TWsidTplBook>()
                .eq("id", id)
                .eq("del_flag", 0)
                .eq("status", st);
        uw.set("status", STATUS_WITHDRAWN);
        uw.set("update_time", new Date());
        return this.baseMapper.update(null, uw);
    }

    /**
     * 退回一态：撤下是单程，撤下之后只剩一张脸，不存在退回去这一态。
     */
    @Override
    public int revertTWsidTplBook(Long id) {
        return 0;
    }

    // ---------------------------------------------------------------- 抽行

    /**
     * 成批抽掉：与单条同走 {@link #removeOne} 一处说法，两处不各摆一套。
     */
    @Override
    public int deleteTWsidTplBookByIds(String ids) {
        if (StringUtils.isEmpty(ids)) {
            return 0;
        }
        Long[] idArr = ConvertUtil.toLongArray(ids);
        int n = 0;
        for (Long id : idArr) {
            n += removeOne(id);
        }
        return n;
    }

    @Override
    public int deleteTWsidTplBookById(Long id) {
        return removeOne(id);
    }

    /**
     * 抽一行的唯一说法：撤下的行整行抽不走；抽掉只划删除标记，不物理抹行。
     */
    private int removeOne(Long id) {
        if (id == null) {
            return 0;
        }
        TWsidTplBook cur = this.baseMapper.selectOne(new QueryWrapper<TWsidTplBook>()
                .eq("id", id)
                .eq("del_flag", 0));
        if (cur == null) {
            return 0;
        }
        if (cur.getStatus() != null && cur.getStatus() == STATUS_WITHDRAWN) {
            return 0;
        }
        UpdateWrapper<TWsidTplBook> uw = new UpdateWrapper<TWsidTplBook>()
                .eq("id", id)
                .eq("del_flag", 0);
        uw.set("del_flag", 1);
        uw.set("update_time", new Date());
        return this.baseMapper.update(null, uw);
    }

    // ---------------------------------------------------------------- 回落核心

    /**
     * 回落核心：通表翻查一趟取回该槽位全部在位行，按层代号逐级问——两路同一摊数据，
     * 同一刻给不出两种说法。每一层先挑最后落笔（ver_no 大）的那条，落笔分不出先后
     * 再比代号串字面先后，取靠后那条。
     */
    private TWsidTplBook resolveFrom(Integer specId, String tplName, String fromLevel) {
        if (specId == null || StringUtils.isBlank(tplName)) {
            return null;
        }
        List<TWsidTplBook> all = this.baseMapper.selectList(new QueryWrapper<TWsidTplBook>()
                .eq("del_flag", 0)
                .and(w -> w.eq("status", STATUS_ACTIVE).or().isNull("status"))
                .eq("spec_id", specId)
                .eq("tpl_name", tplName.trim()));
        Map<String, TWsidTplBook> bestByLevel = new HashMap<String, TWsidTplBook>();
        for (TWsidTplBook r : all) {
            TWsidTplBook cur = bestByLevel.get(r.getLevelKind());
            if (cur == null || betterThan(r, cur)) {
                bestByLevel.put(r.getLevelKind(), r);
            }
        }
        boolean started = fromLevel == null;
        for (String level : LEVEL_CHAIN) {
            if (!started) {
                started = level.equals(fromLevel);
                if (!started) {
                    continue;
                }
            }
            TWsidTplBook hit = bestByLevel.get(level);
            if (hit == null) {
                continue;
            }
            if (LEVEL_SYSTEM.equals(level)) {
                // 兜底版只出正联：读时摆这张脸，库里的行照当初那一版摆着，不回头刷新
                TWsidTplBook face = new TWsidTplBook();
                org.springframework.beans.BeanUtils.copyProperties(hit, face);
                face.setTplStyle(STYLE_MAIN_ONLY);
                return face;
            }
            return hit;
        }
        return null;
    }

    /** 同名目并立几行：先挑最后落笔的那条；落笔分不出先后，比代号串字面取靠后那条 */
    private static boolean betterThan(TWsidTplBook a, TWsidTplBook b) {
        int va = a.getVerNo() == null ? 0 : a.getVerNo();
        int vb = b.getVerNo() == null ? 0 : b.getVerNo();
        if (va != vb) {
            return va > vb;
        }
        String na = a.getTplNo() == null ? "" : a.getTplNo();
        String nb = b.getTplNo() == null ? "" : b.getTplNo();
        return na.compareTo(nb) > 0;
    }

    // ---------------------------------------------------------------- 小件

    /** 层级与物种范围只在这一处切：本层值守只见本层；划归物种一段没有就空着回话 */
    private QueryWrapper<TWsidTplBook> scopeOf(String levelScope, List<Integer> specScope) {
        QueryWrapper<TWsidTplBook> q = new QueryWrapper<TWsidTplBook>().eq("del_flag", 0);
        if (StringUtils.isNotBlank(levelScope)) {
            q.eq("level_kind", levelScope.trim());
        }
        if (specScope != null) {
            if (specScope.isEmpty()) {
                return null;
            }
            q.in("spec_id", specScope);
        }
        return q;
    }

    /** 册上仍立着（在位未删）的同三样行；存量行情形栏空着的按在位认 */
    private List<TWsidTplBook> findStanding(Integer specId, String levelKind, String tplName) {
        QueryWrapper<TWsidTplBook> q = new QueryWrapper<TWsidTplBook>()
                .eq("del_flag", 0)
                .and(w -> w.eq("status", STATUS_ACTIVE).or().isNull("status"))
                .eq("spec_id", specId);
        if (levelKind == null) {
            q.isNull("level_kind");
        } else {
            q.eq("level_kind", levelKind);
        }
        if (tplName == null) {
            q.isNull("tpl_name");
        } else {
            q.eq("tpl_name", tplName);
        }
        return this.baseMapper.selectList(q);
    }

    /** 名录底册里对这一代号（未划掉的） */
    private TWsidSpeciesBook findSpecies(Integer specId) {
        if (specId == null) {
            return null;
        }
        return wsidSpeciesBookMapper.selectOne(new QueryWrapper<TWsidSpeciesBook>()
                .eq("id", specId)
                .eq("del_flag", 0));
    }

    /**
     * 冗余的物种代号、称谓与底册对不起来时，以底册为准回写——两处各摆一套即算毛病。
     * 底册里对不上这条的不在这儿动（建行那道闸已经把过）。
     */
    private void reconcileSpec(List<TWsidTplBook> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        List<Long> specIds = new ArrayList<Long>();
        for (TWsidTplBook r : rows) {
            if (r != null && r.getSpecId() != null) {
                specIds.add(r.getSpecId().longValue());
            }
        }
        if (specIds.isEmpty()) {
            return;
        }
        Map<String, TWsidSpeciesBook> book = new HashMap<String, TWsidSpeciesBook>();
        for (TWsidSpeciesBook sp : wsidSpeciesBookMapper.selectBatchIds(specIds)) {
            if (sp.getDelFlag() != null && sp.getDelFlag() != 0) {
                continue;
            }
            book.put(String.valueOf(sp.getId()), sp);
        }
        for (TWsidTplBook r : rows) {
            if (r == null || r.getSpecId() == null) {
                continue;
            }
            TWsidSpeciesBook sp = book.get(String.valueOf(r.getSpecId()));
            if (sp == null) {
                continue;
            }
            boolean noDiff = sp.getSpecNo() == null ? r.getSpecNo() == null : sp.getSpecNo().equals(r.getSpecNo());
            boolean nameDiff = sp.getSpecName() == null ? r.getSpecName() != null : !sp.getSpecName().equals(r.getSpecName());
            if (!noDiff || nameDiff) {
                r.setSpecNo(sp.getSpecNo());
                r.setSpecName(sp.getSpecName());
                TWsidTplBook patch = new TWsidTplBook();
                patch.setId(r.getId());
                patch.setSpecNo(sp.getSpecNo());
                patch.setSpecName(sp.getSpecName());
                this.baseMapper.updateById(patch);
            }
        }
    }

    /** 完成比：已渲 ÷ 应发 × 100，两位小数；应发为 0 时只可能是 0/0，按 0.00 落 */
    private static BigDecimal coverOf(Integer plan, int done) {
        if (plan == null || plan <= 0) {
            return new BigDecimal("0.00");
        }
        return BigDecimal.valueOf(done).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(plan), 2, RoundingMode.HALF_UP);
    }

    /** 代号由系统排上 */
    private static String nextTplNo() {
        return "TPL" + IdWorker.getId();
    }

    private static boolean validStyle(Integer tplStyle) {
        return tplStyle == STYLE_MAIN_ONLY || tplStyle == STYLE_COPY_ONLY || tplStyle == STYLE_FALLBACK;
    }

    private static boolean validStatus(Integer status) {
        return status == STATUS_ACTIVE || status == STATUS_WITHDRAWN;
    }
}
