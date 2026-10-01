-- H2(MODE=MySQL) 测试库：列与 doc/schema/is.sql 对齐，去掉引擎/字符集/内联索引。
CREATE TABLE IF NOT EXISTS t_is_haz_task (
  id bigint NOT NULL,
  bill_no varchar(64) UNIQUE,
  item_no varchar(64),
  due_at timestamp,
  amount decimal(12,2),
  hazard_summary varchar(500),
  dept_group varchar(32),
  pin_dept_id bigint,
  pin_dept_name varchar(128),
  accept_dept_id bigint,
  accept_dept_name varchar(128),
  accepted_at timestamp,
  first_dispatched_at timestamp,
  urge_rounds int DEFAULT 0,
  suspend_reason varchar(500),
  content varchar(255),
  status int,
  del_flag int DEFAULT 0,
  create_by varchar(64),
  create_time timestamp,
  update_by varchar(64),
  update_time timestamp,
  remark varchar(500),
  PRIMARY KEY (id)
);
CREATE INDEX IF NOT EXISTS idx_haz_status_due ON t_is_haz_task(status, due_at);
CREATE INDEX IF NOT EXISTS idx_haz_accept ON t_is_haz_task(accept_dept_id);

CREATE TABLE IF NOT EXISTS t_is_hazard (
  id bigint NOT NULL,
  hazard_no varchar(64) UNIQUE,
  summary varchar(500),
  dept_group varchar(32),
  pin_dept_id bigint,
  rectify_date date,
  closed int DEFAULT 0,
  del_flag int DEFAULT 0,
  create_by varchar(64),
  create_time timestamp,
  update_by varchar(64),
  update_time timestamp,
  remark varchar(500),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_is_dispatch_dept (
  id bigint NOT NULL,
  dept_name varchar(128),
  dept_group varchar(32),
  status int DEFAULT 0,
  order_num int DEFAULT 0,
  del_flag int DEFAULT 0,
  create_by varchar(64),
  create_time timestamp,
  update_by varchar(64),
  update_time timestamp,
  remark varchar(500),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_is_dispatch_window (
  id bigint NOT NULL,
  win_type int,
  win_name varchar(128),
  start_date date,
  end_date date,
  open_hh int,
  close_hh int,
  status int DEFAULT 0,
  del_flag int DEFAULT 0,
  create_by varchar(64),
  create_time timestamp,
  update_by varchar(64),
  update_time timestamp,
  remark varchar(500),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_is_holiday (
  id bigint NOT NULL,
  holiday_date date NOT NULL UNIQUE,
  holiday_name varchar(64),
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS t_is_dispatch_round (
  id bigint NOT NULL,
  round_date date NOT NULL,
  win_type int NOT NULL,
  win_name varchar(128),
  started_at timestamp,
  finished_at timestamp,
  wait_cnt int DEFAULT 0,
  dispatched_cnt int DEFAULT 0,
  suspended_cnt int DEFAULT 0,
  result_type int,
  message varchar(255),
  create_by varchar(64),
  create_time timestamp,
  update_by varchar(64),
  update_time timestamp,
  PRIMARY KEY (id),
  UNIQUE (round_date, win_type)
);

CREATE TABLE IF NOT EXISTS t_is_dispatch_log (
  id bigint NOT NULL,
  round_id bigint,
  bill_id bigint,
  bill_no varchar(64),
  action varchar(32),
  from_status int,
  to_status int,
  dept_id bigint,
  dept_name varchar(128),
  orig_due_at timestamp,
  first_dispatched_at timestamp,
  detail varchar(500),
  create_by varchar(64),
  create_time timestamp
);
CREATE INDEX IF NOT EXISTS idx_log_bill ON t_is_dispatch_log(bill_id);
