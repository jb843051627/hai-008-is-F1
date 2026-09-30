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

CREATE TABLE IF NOT EXISTS t_is_haz_task (
  id bigint NOT NULL COMMENT '主键',
  item_no varchar(64) DEFAULT NULL COMMENT '整改事项号',
  due_at datetime DEFAULT NULL COMMENT '整改期限那一天最后一刻',
  amount decimal(12,2) DEFAULT NULL COMMENT '单条提前派发的自然日数',
  content varchar(255) DEFAULT NULL COMMENT '隐患事由与受派部门记要',
  status int DEFAULT NULL COMMENT '条目情形 0待派 1已派 2派不出去',
  del_flag int DEFAULT '0' COMMENT '删除标记 0正常 1删除',
  create_by varchar(64) DEFAULT NULL COMMENT '创建者',
  create_time datetime DEFAULT NULL COMMENT '创建时间',
  update_by varchar(64) DEFAULT NULL COMMENT '更新者',
  update_time datetime DEFAULT NULL COMMENT '更新时间',
  remark varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='隐患整改到期事项条';

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

-- 初始档案数据（id=1 启用 / id=2 停用）
-- 初始档案数据：id=0 在用、id=1 已退役
-- 储存规模上限 50/200/500 吨：等于上限算高一档。
INSERT IGNORE INTO t_is_enterprise (id, site_no, site_name, site_type, road_name, th1_max, th2_max, th3_max, status, del_flag, create_by, create_time)
VALUES (0, 'EN00', '临江市东湖化工园区（在用）', '生产企业', '临江省—临江市—东湖区—东湖街道', 50.00, 200.00, 500.00, 0, 0, 'seed', NOW()),
       (1, 'EN01', '临江市城南气体储运（已退役）', '储存企业', '临江省—临江市—城南区—望湖街道', 50.00, 200.00, 500.00, 1, 0, 'seed', NOW());

