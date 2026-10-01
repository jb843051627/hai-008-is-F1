-- is 工业安全生产隐患管理 -- schema (hai-008)
-- 列名与基线实体契约（@TableName/@TableField）逐列对齐，改列必须同步实体。
-- 库：hai_008

CREATE TABLE IF NOT EXISTS t_is_acc_flow (
  id bigint NOT NULL COMMENT '主键',
  biz_no varchar(64) DEFAULT NULL COMMENT '事故报告处置单号',
  stage int DEFAULT NULL COMMENT '当前处置段 0..3（初报/调查/处置/结案封卷）',
  status int DEFAULT NULL COMMENT '单子落定 0未起 1在办 2已封存',
  content varchar(255) DEFAULT NULL COMMENT '单上记事',
  last_action varchar(64) DEFAULT NULL COMMENT '最近一次过口动作',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='事故报告调查处置单';

CREATE TABLE IF NOT EXISTS t_is_check_row (
  id bigint NOT NULL COMMENT '主键',
  batch_no varchar(64) DEFAULT NULL COMMENT '执法检查数据上传批次号',
  row_no int DEFAULT NULL COMMENT '原表行次',
  item_code varchar(64) DEFAULT NULL COMMENT '被校验出的数据项代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本行检查得分',
  status int DEFAULT NULL COMMENT '行落地情形 0待核 1已入库 2退回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='执法检查记录导入核对行';

CREATE TABLE IF NOT EXISTS t_is_enterprise (
  id bigint NOT NULL COMMENT '主键',
  site_no varchar(64) DEFAULT NULL COMMENT '危化企业底档代号',
  site_name varchar(128) DEFAULT NULL COMMENT '底档名称',
  site_type varchar(32) DEFAULT NULL COMMENT '底档类别(生产企业/储存企业)',
  road_name varchar(128) DEFAULT NULL COMMENT '所属行政区划(省—市—区—街道)',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '单档储存规模一档上限(吨)',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二档上限(吨)',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三档上限(吨)',
  status int DEFAULT NULL COMMENT '底档情形 0在用 1已退役',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='危化企业底档';

CREATE TABLE IF NOT EXISTS t_is_haz_card (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '重大危险源档案卡号',
  node_no int DEFAULT NULL COMMENT '本卡版本次序',
  site_id int DEFAULT NULL COMMENT '所属底档',
  site_no varchar(64) DEFAULT NULL COMMENT '所属底档代号',
  qty decimal(12,2) DEFAULT NULL COMMENT '本卡储存量(吨)',
  fine_amt decimal(12,2) DEFAULT NULL COMMENT '本年度新增储存量(吨)',
  content varchar(255) DEFAULT NULL COMMENT '校验串（随版本次序走）',
  grade_level int DEFAULT NULL COMMENT '储存量折到的风险保障层级',
  status int DEFAULT NULL COMMENT '进展 0待核对 1已核对 2已压死',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='重大危险源档案卡';

-- 隐患源：检查发现、限期整改的隐患。催办条由本表生成；closed=1 表示已销号结案。
CREATE TABLE IF NOT EXISTS t_is_hazard (
  id bigint NOT NULL COMMENT '主键',
  hazard_no varchar(64) DEFAULT NULL COMMENT '隐患号（检查记录内唯一）',
  summary varchar(500) DEFAULT NULL COMMENT '隐患概况',
  dept_group varchar(32) DEFAULT NULL COMMENT '责任条线（候选部门分组代号）',
  pin_dept_id bigint DEFAULT NULL COMMENT '原定责任部门（可空；撤并判定看它）',
  rectify_date date DEFAULT NULL COMMENT '限改之日（当天最后一刻为条子期限）',
  closed int DEFAULT '0' COMMENT '是否已销号结案 0否 1是',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_hazard_no (hazard_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='限期整改隐患源';

-- 责任部门候选池：条线内按在手活数加权派发；status=1 表示整个撤并。
CREATE TABLE IF NOT EXISTS t_is_dispatch_dept (
  id bigint NOT NULL COMMENT '主键',
  dept_name varchar(128) DEFAULT NULL COMMENT '责任部门名称',
  dept_group varchar(32) DEFAULT NULL COMMENT '所属责任条线',
  status int DEFAULT '0' COMMENT '部门情形 0在岗 1已撤并',
  order_num int DEFAULT '0' COMMENT '同权重时的次序',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  KEY idx_dept_group (dept_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='催办派发责任部门候选';

-- 派发窗口：日常一条（起止日为空，逢工作日/非节假日按点开闭）；专项检查月按起止日单开，不走日常。
CREATE TABLE IF NOT EXISTS t_is_dispatch_window (
  id bigint NOT NULL COMMENT '主键',
  win_type int DEFAULT NULL COMMENT '窗口种类 1日常 2专项检查月',
  win_name varchar(128) DEFAULT NULL COMMENT '窗口名',
  start_date date DEFAULT NULL COMMENT '专项起始日（含，日常为空）',
  end_date date DEFAULT NULL COMMENT '专项截止日（含，日常为空）',
  open_hh int DEFAULT NULL COMMENT '开窗时点（整点，含）',
  close_hh int DEFAULT NULL COMMENT '关窗时点（整点，不含）',
  status int DEFAULT '0' COMMENT '情形 0启用 1停用',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='催办派发窗口';

-- 法定节假日：落在表内的日子整段静默；周末若不调休上班由本表维护，全系统只认这一处。
CREATE TABLE IF NOT EXISTS t_is_holiday (
  id bigint NOT NULL COMMENT '主键',
  holiday_date date NOT NULL COMMENT '法定节假日日期',
  holiday_name varchar(64) DEFAULT NULL COMMENT '节假日名',
  PRIMARY KEY (id),
  UNIQUE KEY uk_holiday_date (holiday_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='法定节假日历';

-- 催办条。一条五样写死：条号 bill_no、期限最后一刻 due_at、还剩几日露面 amount、
-- 当前情形 status、隐患概况 hazard_summary 与负责部门（pin/accept 两栏，派出前看原定、派出后看接手）。
-- 期限口径：due_at 生成后任何动作都不许改；要调只许动 amount（还剩几日档）。
CREATE TABLE IF NOT EXISTS t_is_haz_task (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '催办条号',
  item_no varchar(64) DEFAULT NULL COMMENT '整改事项号（溯源）',
  due_at datetime DEFAULT NULL COMMENT '整改期限那一天最后一刻（23:59:59，定了不许动）',
  amount decimal(12,2) DEFAULT NULL COMMENT '到期前还剩几日就该露面（唯一允许调度的一档）',
  hazard_summary varchar(500) DEFAULT NULL COMMENT '隐患概况',
  dept_group varchar(32) DEFAULT NULL COMMENT '责任条线（候选部门分组）',
  pin_dept_id bigint DEFAULT NULL COMMENT '原定负责部门（条子生成时落定）',
  pin_dept_name varchar(128) DEFAULT NULL COMMENT '原定负责部门名',
  accept_dept_id bigint DEFAULT NULL COMMENT '实际接手部门',
  accept_dept_name varchar(128) DEFAULT NULL COMMENT '实际接手部门名',
  accepted_at datetime DEFAULT NULL COMMENT '接手时刻（与翻已派同写，缺一不算派成）',
  first_dispatched_at datetime DEFAULT NULL COMMENT '最初真正派出时刻（撤条留痕，永不覆盖）',
  urge_rounds int DEFAULT '0' COMMENT '催过几轮',
  suspend_reason varchar(500) DEFAULT NULL COMMENT '挂起原由',
  content varchar(255) DEFAULT NULL COMMENT '隐患事由与受派部门记要',
  status int DEFAULT NULL COMMENT '情形 0待派 1已派 2派不出去 3挂起 4已撤 5转办',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id),
  UNIQUE KEY uk_bill_no (bill_no),
  KEY idx_status_due (status, due_at),
  KEY idx_accept_dept (accept_dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='隐患整改催办条';

CREATE TABLE IF NOT EXISTS t_is_lim_rule (
  id bigint NOT NULL COMMENT '主键',
  rule_code varchar(64) DEFAULT NULL COMMENT '辨识判定线代号',
  rule_name varchar(128) DEFAULT NULL COMMENT '判定线名',
  th1_max decimal(12,2) DEFAULT NULL COMMENT '一级档界值（含）',
  th2_max decimal(12,2) DEFAULT NULL COMMENT '二级档界值（含）',
  th3_max decimal(12,2) DEFAULT NULL COMMENT '三级档界值（含）',
  eff_start datetime DEFAULT NULL COMMENT '启用之日',
  eff_end datetime DEFAULT NULL COMMENT '交棒之日(不含)',
  priority int DEFAULT NULL COMMENT '判定线顺位(数值大的先说话)',
  status int DEFAULT NULL COMMENT '判定线情形 0现行 1已让位',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='重大危险源辨识判定线';

CREATE TABLE IF NOT EXISTS t_is_rect_bill (
  id bigint NOT NULL COMMENT '主键',
  bill_no varchar(64) DEFAULT NULL COMMENT '整改处置签核单号',
  node_no int DEFAULT NULL COMMENT '当前所在道 0..2（班组提/安环科核/分管领导批）',
  sign_mode int DEFAULT NULL COMMENT '同口并印办法 0任一人 1两名点齐',
  need_count int DEFAULT NULL COMMENT '本道应落印数',
  sign_count int DEFAULT NULL COMMENT '本道已落印数',
  status int DEFAULT NULL COMMENT '报批情形 0在核 1已核讫 2已打回',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='重大隐患整改处置签核单';

-- 轮次点算：同一事务里随派发一起落库；一屏三个数与库底数同一次点算。
-- result_type 只记真正扫描的轮次：1已扫 2本轮无活。窗口关闭/出错不写，不算轮次。
CREATE TABLE IF NOT EXISTS t_is_dispatch_round (
  id bigint NOT NULL COMMENT '主键',
  round_date date NOT NULL COMMENT '本轮日（墙钟 GMT+8）',
  win_type int NOT NULL COMMENT '本轮窗口 1日常 2专项',
  win_name varchar(128) DEFAULT NULL COMMENT '本轮窗口名',
  started_at datetime DEFAULT NULL COMMENT '本轮起刻',
  finished_at datetime DEFAULT NULL COMMENT '本轮止刻',
  wait_cnt int DEFAULT '0' COMMENT '本轮待派几条',
  dispatched_cnt int DEFAULT '0' COMMENT '本轮已派几条',
  suspended_cnt int DEFAULT '0' COMMENT '本轮挂起几条',
  result_type int DEFAULT NULL COMMENT '结果 1已扫 2本轮无活',
  message varchar(255) DEFAULT NULL COMMENT '一屏话术',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_round_date_win (round_date, win_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='催办派发轮次点算';

-- 派发留痕：生成/挂起/派出/催办/撤条/转办/调档全过痕；撤条时把原期限、初派时刻原样抄存。
CREATE TABLE IF NOT EXISTS t_is_dispatch_log (
  id bigint NOT NULL COMMENT '主键',
  round_id bigint DEFAULT NULL COMMENT '所属轮次（手工动作可空）',
  bill_id bigint DEFAULT NULL COMMENT '催办条',
  bill_no varchar(64) DEFAULT NULL COMMENT '催办条号',
  action varchar(32) DEFAULT NULL COMMENT '动作 CREATE生成 SUSPEND挂起 DISPATCH派出 URGE催办 REVOKE撤条 TRANSFER转办 TUNE调档',
  from_status int DEFAULT NULL COMMENT '动作前情形',
  to_status int DEFAULT NULL COMMENT '动作后情形',
  dept_id bigint DEFAULT NULL COMMENT '本动作关涉部门',
  dept_name varchar(128) DEFAULT NULL COMMENT '本动作关涉部门名',
  orig_due_at datetime DEFAULT NULL COMMENT '抄存：最初整改期限最后一刻',
  first_dispatched_at datetime DEFAULT NULL COMMENT '抄存：最初真正派出时刻',
  detail varchar(500) DEFAULT NULL COMMENT '事由/原由',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_log_bill (bill_id),
  KEY idx_log_round (round_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='催办派发留痕';

-- 初始档案数据（id=1 启用 / id=2 停用）
-- 初始档案数据：id=0 在用、id=1 已退役
-- 储存规模上限 50/200/500 吨：等于上限算高一档。
INSERT IGNORE INTO t_is_enterprise (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'EN00', '临江市东湖化工园区（在用）', '生产企业', '临江省—临江市—东湖区—东湖街道', 50.00, 200.00, 500.00, 0, 0, 'seed', NOW()),
       (1, 'EN01', '临江市城南气体储运（已退役）', '储存企业', '临江省—临江市—城南区—望湖街道', 50.00, 200.00, 500.00, 1, 0, 'seed', NOW());

-- 责任部门候选：危化条线三科在岗；另有一个整组撤并的条线（QY），用于生成时挂起演示。
INSERT IGNORE INTO t_is_dispatch_dept (id, dept_name, dept_group, status, order_num, del_flag, create_by, create_time)
VALUES (1001, '危化监管一科', 'HW', 0, 1, 0, 'seed', NOW()),
       (1002, '危化监管二科', 'HW', 0, 2, 0, 'seed', NOW()),
       (1003, '执法大队', 'HW', 0, 3, 0, 'seed', NOW()),
       (1004, '原企管三所（整组撤并）', 'QY', 1, 4, 0, 'seed', NOW());

-- 日常窗口：每天 08:00（含）—20:00（不含）；夜间与法定节假日整段静默。
INSERT IGNORE INTO t_is_dispatch_window (id, win_type, win_name, start_date, end_date, open_hh, close_hh, status, del_flag, create_by, create_time)
VALUES (1, 1, '日常派发窗口', NULL, NULL, 8, 20, 0, 0, 'seed', NOW()),
       (2, 2, '专项检查月窗口', '2026-06-01', '2026-06-15', 8, 20, 0, 0, 'seed', NOW());

-- 法定节假日示例（以当年国务院公告为准，由管理员维护）。
INSERT IGNORE INTO t_is_holiday (id, holiday_date, holiday_name)
VALUES (1, '2026-01-01', '元旦'),
       (2, '2026-02-17', '春节'),
       (3, '2026-02-18', '春节'),
       (4, '2026-02-19', '春节'),
       (5, '2026-02-20', '春节'),
       (6, '2026-02-21', '春节'),
       (7, '2026-02-22', '春节'),
       (8, '2026-02-23', '春节');

-- 隐患源示例：一条在办、一条已销号（生成时应挂起）、一条原定部门已撤并（生成时应挂起）。
INSERT IGNORE INTO t_is_hazard (id, hazard_no, summary, dept_group, pin_dept_id, rectify_date, closed, del_flag, create_by, create_time)
VALUES (1, 'YH20260001', '东湖化工园区罐区可燃气体报警器失效', 'HW', 1001, '2026-09-30', 0, 0, 'seed', NOW()),
       (2, 'YH20260002', '城南气体储运消防通道堆占（已复查销号）', 'HW', 1002, '2026-09-29', 1, 0, 'seed', NOW()),
       (3, 'YH20260003', '东湖园区动火作业票证缺签', 'QY', 1004, '2026-09-28', 0, 0, 'seed', NOW());
