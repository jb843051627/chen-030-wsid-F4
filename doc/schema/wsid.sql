-- wsid 野生动植物专用标识申领核发与年度核查管理 -- schema (chen-030)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：chen_030

CREATE TABLE IF NOT EXISTS t_wsid_apply_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '申领名册核收批次码',
  row_no int DEFAULT NULL COMMENT '原册行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被对上的繁育主体册代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行申领张数',
  chk_num decimal(12,2) DEFAULT NULL COMMENT '本行核对齐数',
  put_kind varchar(32) DEFAULT NULL COMMENT '来路（站所自录／县局代收两条来路）',
  status int DEFAULT NULL COMMENT '行落地情形 0未勾 1已收下 2已退回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_apply_row_batch_put_row (batch_no, put_kind, row_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标识申领名册核收行';

-- 一串一册：同一串站所自录、县局代收各走一遍，各留各的收讫单面，(batch_no, put_kind) 唯一。
-- 点行数（file_num）与册头写定的上限（row_limit）进门头一步刻死，隔半年仍查得回当初报了几行。
CREATE TABLE IF NOT EXISTS t_wsid_apply_receipt (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '申领名册核收批次码（一串一个）',
  put_kind varchar(32) DEFAULT NULL COMMENT '本面来路（站所自录／县局代收，两条来路各留一面）',
  file_num int DEFAULT NULL COMMENT '进门点出的行数（空册为 0）',
  row_limit int DEFAULT NULL COMMENT '册头那一处写定的本行数上限（空着/负数/没边的大数按 150 落）',
  in_num int DEFAULT NULL COMMENT '收讫三笔·进门行（取自逐行定去处那一趟）',
  ok_num int DEFAULT NULL COMMENT '收讫三笔·收下行',
  back_num int DEFAULT NULL COMMENT '收讫三笔·退回行',
  empty_flag int DEFAULT '0' COMMENT '空册记号 0正经串 1空册（空册只落「空册」字样，不出收讫那一句）',
  face_kind int DEFAULT '0' COMMENT '单面来头 0头一遍 1第二遍只对照不改账',
  hit_limit int DEFAULT '0' COMMENT '撞上限记号 0没撞 1点行数撞上上限已收手（余下另起一串）',
  pending_num int DEFAULT NULL COMMENT '本面尚未定去处的行数（未勾行计数）',
  rollback_num int DEFAULT '0' COMMENT '本面累计回撤摆回未勾的行数',
  content varchar(1000) DEFAULT NULL COMMENT '单面文面（收讫那一句／空册字样／对照与回撤一记）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_apply_receipt_batch_put (batch_no, put_kind)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标识申领名册核收批次收讫单面';

CREATE TABLE IF NOT EXISTS t_wsid_case_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '专用标识申领单的件次',
  stage int DEFAULT NULL COMMENT '当前关次 0..3（站所收单/县局核批/省局备案/发放到位）',
  status int DEFAULT NULL COMMENT '单子走到哪一关 0未起 1在办 2已封住',
  content varchar(255) DEFAULT NULL COMMENT '一关一记',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次挪关动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专用标识申领单流转单';

CREATE TABLE IF NOT EXISTS t_wsid_holder_book (
  id bigint NOT NULL COMMENT '主键',
  holder_no varchar(64) DEFAULT NULL COMMENT '繁育主体册代号',
  spec_id int DEFAULT NULL COMMENT '挂在哪一个名录物种名下',
  spec_no varchar(64) DEFAULT NULL COMMENT '名录物种代号（冗余自物种底册）',
  src_kind varchar(32) DEFAULT NULL COMMENT '来路（站所代收／县局自录／市里补报三条来路）',
  herd_num decimal(12,2) DEFAULT NULL COMMENT '在册在养个体数',
  content varchar(255) DEFAULT NULL COMMENT '随件交来的主体名号与繁育点记要',
  status int DEFAULT NULL COMMENT '册面情形 0新入册 1已核齐 2缺项',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人工繁育主体底册';

CREATE TABLE IF NOT EXISTS t_wsid_issue_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '专用标识核发签批单单码',
  node_no int DEFAULT NULL COMMENT '当前所在关 0..2（站所初审/县局核批/省局备案）',
  sign_mode int DEFAULT NULL COMMENT '本关算齐办法 0一名即齐 1两名须同判',
  need_count int DEFAULT NULL COMMENT '本关应签人数',
  sign_count int DEFAULT NULL COMMENT '本关已到人数',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次落笔动作',
  content varchar(255) DEFAULT NULL COMMENT '一关一记（按语与那一刻接成的一段追溯）',
  status int DEFAULT NULL COMMENT '签批情形 0在签 1已点齐 2已压回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专用标识核发签批单';

CREATE TABLE IF NOT EXISTS t_wsid_quota_rule (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '额度贴线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '线名',
  obj_kind varchar(32) DEFAULT NULL COMMENT '这条线管甲层、乙层还是丙层',
  th1_max decimal(12,3) DEFAULT NULL COMMENT '关注限值',
  th2_max decimal(12,3) DEFAULT NULL COMMENT '告警限值',
  th3_max decimal(12,3) DEFAULT NULL COMMENT '封顶限值',
  unit_kind varchar(128) DEFAULT NULL COMMENT '计量口径（每张计／每捆计 两式，一捆合十二张）',
  eff_start datetime DEFAULT NULL COMMENT '启用那一一日',
  eff_end datetime DEFAULT NULL COMMENT '交班那一一日(不含)',
  priority int DEFAULT NULL COMMENT '交班次序(数值大的先说话)',
  status int DEFAULT NULL COMMENT '线的情形 0在位 1已撤下',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年度可发额度贴线档';

CREATE TABLE IF NOT EXISTS t_wsid_remind_item (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '年度核查与换证提醒条目代号',
  holder_no varchar(64) DEFAULT NULL COMMENT '一处主体代号（繁育主体册代号）',
  level_name varchar(128) DEFAULT NULL COMMENT '层别名目（挂在哪一层／哪一名目下）',
  affair_kind varchar(32) DEFAULT NULL COMMENT '一桩事由名目（年度核查／换证）',
  due_at datetime DEFAULT NULL COMMENT '该发那一一日（应交之日／届满之日，落库为 datetime）',
  amount decimal(12,2) DEFAULT NULL COMMENT '至多许提前几日（空着/取负/大过定数上限30，本行本轮不动）',
  channel_kind int DEFAULT NULL COMMENT '通道取用式 0只走站内 1先站内后短信息 2三式依次回落',
  mobile_no varchar(32) DEFAULT NULL COMMENT '短信息那一栏的可用号子（空着则短信息一式不通）',
  content varchar(255) DEFAULT NULL COMMENT '事由与所对主体的记要',
  status int DEFAULT NULL COMMENT '条目情形 0搁着 1已发出 2发不成 3文面缺栏',
  eligible_at datetime DEFAULT NULL COMMENT '够上条件的那一瞬（静默后起话仍记此瞬，年底对流水用）',
  fired_at datetime DEFAULT NULL COMMENT '实际起话那一刻',
  chan_used int DEFAULT NULL COMMENT '走通的是第几式 0站内 1短信息 2上门（发不成时空着）',
  receipts varchar(2000) DEFAULT NULL COMMENT '各式回执连缀（只追加，前一式回执不擦）',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注（含「上门不通」时第三式回不通）',
  PRIMARY KEY (id),
  KEY idx_remind_due (due_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='年度核查与换证提醒条目';

-- 去重键钉在「一处主体＋一桩事由＋一日」：同日第二趟只对照不重发，隔日重新累计。
-- 同刻两趟并发各排各的队，唯一键兜底，后到一趟的插入被挡下转对照，不并趟也不挤掉。
CREATE TABLE IF NOT EXISTS t_wsid_remind_send_log (
  id bigint NOT NULL COMMENT '主键',
  item_id bigint DEFAULT NULL COMMENT '所对条目',
  holder_no varchar(64) DEFAULT NULL COMMENT '一处主体代号',
  affair_kind varchar(32) DEFAULT NULL COMMENT '一桩事由名目（年度核查／换证）',
  send_day char(10) DEFAULT NULL COMMENT '起话那一日（yyyy-MM-dd，去重键里的一日）',
  chan_used int DEFAULT NULL COMMENT '走通的是第几式 0站内 1短信息 2上门',
  eligible_at datetime DEFAULT NULL COMMENT '够上条件的那一瞬',
  fired_at datetime DEFAULT NULL COMMENT '实际起话那一刻',
  run_seq varchar(32) DEFAULT NULL COMMENT '本趟排队先后',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_remind_holder_affair_day (holder_no, affair_kind, send_day),
  KEY idx_remind_send_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='提醒起话沉底';

CREATE TABLE IF NOT EXISTS t_wsid_species_book (
  id bigint NOT NULL COMMENT '主键',
  spec_no varchar(64) DEFAULT NULL COMMENT '名录物种代号',
  spec_name varchar(128) DEFAULT NULL COMMENT '名录物种称谓',
  spec_kind varchar(32) DEFAULT NULL COMMENT '层类别（甲层陆生兽类·乙层鸟类·丙层水生类）',
  origin_place varchar(255) DEFAULT NULL COMMENT '分布辖段详址（榛原省—桦川市—磐泽县—乡—片区）',
  src_kind varchar(32) DEFAULT NULL COMMENT '来路（省局名录下发／市里增补／本局自录）',
  status int DEFAULT NULL COMMENT '名录情形 0在册 1已调出',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='保护名录物种底册';

CREATE TABLE IF NOT EXISTS t_wsid_tpl_book (
  id bigint NOT NULL COMMENT '主键',
  tpl_no varchar(64) DEFAULT NULL COMMENT '证面版式代号（由系统排上，不留手填的格子）',
  tpl_name varchar(128) DEFAULT NULL COMMENT '版式名目（正联名／副本联名／槽位段名）',
  tpl_style int DEFAULT NULL COMMENT '版式取用三式 0只出正联 1只出副本联 2按层回落',
  style_num int DEFAULT NULL COMMENT '一屏并列排出的联数 1..3（量程外的数视同没填）',
  tpl_text varchar(255) DEFAULT NULL COMMENT '槽位文面',
  level_kind varchar(32) DEFAULT NULL COMMENT '定档层级（县定／市定／省定／系统兜底）',
  parent_no varchar(64) DEFAULT NULL COMMENT '继承自哪一版式',
  spec_id int DEFAULT NULL COMMENT '挂在哪一个名录物种名下',
  spec_no varchar(64) DEFAULT NULL COMMENT '适用名录物种代号（冗余自物种底册）',
  spec_name varchar(128) DEFAULT NULL COMMENT '名录物种称谓（冗余一份，以物种底册为准回写）',
  ver_no int DEFAULT NULL COMMENT '改动次序（数值大的先说话）',
  plan_num int DEFAULT NULL COMMENT '本套应发张数',
  done_num int DEFAULT NULL COMMENT '已渲张数',
  cover_num decimal(12,2) DEFAULT NULL COMMENT '完成比（轧出来，不留填数的空位）',
  eff_start datetime DEFAULT NULL COMMENT '启用那一一日',
  eff_end datetime DEFAULT NULL COMMENT '交班那一一日(不含)',
  status int DEFAULT NULL COMMENT '行的情形 0在位 1已撤下',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='专用标识证面版式模板册';

-- 初始档案数据（id=1 启用 / id=2 停用）
-- 名录物种两条种子：species id=0 为在册、id=1 为已调出，供挂靠校验与「已调出不再收新行」判定取用
-- 层别与分布辖段按业务口径给值
INSERT IGNORE INTO t_wsid_species_book (id, spec_no, spec_name, spec_kind, origin_place, src_kind, status, del_flag, create_by, create_time)
VALUES (0, 'WS00', '榛原省桦川市磐泽县磐泽湿地甲层陆生兽类·在册示范种', '甲层', '榛原省—桦川市—磐泽县—北磐乡—东滩片区', '省局名录下发', 0, 0, 'seed', NOW()),
       (1, 'WS01', '南泽乡西汊乙层鸟类·已调出的旧名录（已调出）', '乙层', '榛原省—桦川市—磐泽县—南泽乡—西汊片区', '市里增补', 1, 0, 'seed', NOW());

