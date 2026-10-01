-- H2（MySQL 兼容模式）测试库：仅建催办派发链路用到的表
CREATE TABLE t_sys_department (
  id bigint NOT NULL,
  parent_id bigint,
  dept_name varchar(255),
  leader varchar(255),
  phone varchar(255),
  email varchar(255),
  status int,
  order_num int,
  create_by varchar(64),
  create_time datetime,
  update_by varchar(64),
  update_time datetime,
  remark varchar(500),
  PRIMARY KEY (id)
);

CREATE TABLE t_is_haz_task (
  id bigint NOT NULL,
  item_no varchar(64),
  due_at datetime,
  amount decimal(12,2),
  content varchar(255),
  dept_id bigint,
  accepted_at datetime,
  urge_count int DEFAULT 0,
  suspend_reason varchar(255),
  status int,
  del_flag int DEFAULT 0,
  create_by varchar(64),
  create_time datetime,
  update_by varchar(64),
  update_time datetime,
  remark varchar(500),
  PRIMARY KEY (id)
);

CREATE TABLE t_is_haz_task_log (
  id bigint NOT NULL,
  task_id bigint,
  item_no varchar(64),
  action int,
  from_dept_id bigint,
  to_dept_id bigint,
  due_at datetime,
  accepted_at datetime,
  reason varchar(255),
  round_no varchar(32),
  del_flag int DEFAULT 0,
  create_by varchar(64),
  create_time datetime,
  update_by varchar(64),
  update_time datetime,
  remark varchar(500),
  PRIMARY KEY (id),
  UNIQUE (task_id, round_no, action)
);

-- 三个启用责任部门（活最少者先接），9 为已停用撤并部门
INSERT INTO t_sys_department (id, dept_name, status, order_num) VALUES
  (1, '应急一科', 1, 1),
  (2, '应急二科', 1, 2),
  (3, '危化监管科', 1, 3),
  (9, '已撤并科室', 0, 9);
