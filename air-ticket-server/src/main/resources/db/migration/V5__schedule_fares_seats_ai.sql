-- Additive, resumable migration. Existing orders retain LEGACY_UNSPLIT amounts.
CREATE TABLE IF NOT EXISTS airport (
 code CHAR(3) PRIMARY KEY,city VARCHAR(30) NOT NULL,name VARCHAR(100) NOT NULL,
 timezone VARCHAR(60) NOT NULL DEFAULT 'Asia/Shanghai',terminal VARCHAR(10),source_url VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS aircraft_layout (
 code VARCHAR(30) PRIMARY KEY,name VARCHAR(100) NOT NULL,aircraft_type VARCHAR(40) NOT NULL,
 version INT NOT NULL DEFAULT 1,source_type VARCHAR(20) NOT NULL DEFAULT 'DEMO'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS layout_seat (
 layout_code VARCHAR(30) NOT NULL,seat_no VARCHAR(6) NOT NULL,seat_row INT NOT NULL,seat_column CHAR(1) NOT NULL,
 cabin_class INT NOT NULL,seat_fee DECIMAL(10,2) NOT NULL DEFAULT 0,
 PRIMARY KEY(layout_code,seat_no),FOREIGN KEY(layout_code) REFERENCES aircraft_layout(code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS fare_policy (
 code VARCHAR(40) PRIMARY KEY,name VARCHAR(80) NOT NULL,tax_fee DECIMAL(10,2) NOT NULL,fuel_fee DECIMAL(10,2) NOT NULL,
 baggage_kg INT NOT NULL,refund_cutoff_hours INT NOT NULL DEFAULT 2,early_refund_rate DECIMAL(5,4) NOT NULL,
 late_refund_rate DECIMAL(5,4) NOT NULL,description VARCHAR(1000) NOT NULL,
 currency CHAR(3) NOT NULL DEFAULT 'CNY',source_type VARCHAR(20) NOT NULL DEFAULT 'DEMO',
 effective_from DATE NOT NULL,effective_to DATE NOT NULL,
 CHECK(tax_fee>=0 AND fuel_fee>=0),CHECK(early_refund_rate BETWEEN 0 AND 1 AND late_refund_rate BETWEEN 0 AND 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS flight_schedule (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,code VARCHAR(40) NOT NULL UNIQUE,company_id INT NOT NULL,flight_no VARCHAR(30) NOT NULL,
 departure_airport CHAR(3) NOT NULL,arrival_airport CHAR(3) NOT NULL,departure_time TIME NOT NULL,duration_minutes INT NOT NULL,
 weekdays VARCHAR(7) NOT NULL DEFAULT '1234567',valid_from DATE NOT NULL,valid_to DATE NOT NULL,
 layout_code VARCHAR(30) NOT NULL,policy_code VARCHAR(40) NOT NULL,base_price DECIMAL(10,2) NOT NULL,
 version INT NOT NULL DEFAULT 1,enabled TINYINT NOT NULL DEFAULT 1,
 FOREIGN KEY(company_id) REFERENCES company(company_id),FOREIGN KEY(departure_airport) REFERENCES airport(code),
 FOREIGN KEY(arrival_airport) REFERENCES airport(code),FOREIGN KEY(layout_code) REFERENCES aircraft_layout(code),
 FOREIGN KEY(policy_code) REFERENCES fare_policy(code),CHECK(departure_airport<>arrival_airport),CHECK(duration_minutes>0 AND base_price>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS flight_schedule_exception (
 schedule_id BIGINT NOT NULL,service_date DATE NOT NULL,reason VARCHAR(200),PRIMARY KEY(schedule_id,service_date),
 FOREIGN KEY(schedule_id) REFERENCES flight_schedule(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS flight_generation_run (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,finished_at DATETIME,
 window_start DATE NOT NULL,window_end DATE NOT NULL,status VARCHAR(20) NOT NULL,created_count INT NOT NULL DEFAULT 0,
 skipped_count INT NOT NULL DEFAULT 0,failed_count INT NOT NULL DEFAULT 0,error_summary VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS flight_quote (
 id VARCHAR(36) PRIMARY KEY,userid INT NOT NULL,hbid INT NOT NULL,cabin_id INT NOT NULL,quantity INT NOT NULL,
 total DECIMAL(10,2) NOT NULL,snapshot JSON NOT NULL,expires_at DATETIME NOT NULL,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
 used_hold_id BIGINT,KEY idx_quote_owner(userid,created_at),FOREIGN KEY(hbid) REFERENCES hbxx(hbid),
 FOREIGN KEY(cabin_id) REFERENCES hbxx_cabin(cabin_id),CHECK(quantity BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS flight_seat (
 hbid INT NOT NULL,seat_no VARCHAR(6) NOT NULL,cabin_id INT NOT NULL,seat_row INT NOT NULL,seat_column CHAR(1) NOT NULL,
 seat_fee DECIMAL(10,2) NOT NULL DEFAULT 0,state VARCHAR(12) NOT NULL DEFAULT 'AVAILABLE',hold_id BIGINT,order_id INT,
 PRIMARY KEY(hbid,seat_no),KEY idx_seat_cabin(cabin_id,state,seat_row),KEY idx_seat_hold(hold_id),KEY idx_seat_order(order_id),
 FOREIGN KEY(hbid) REFERENCES hbxx(hbid),FOREIGN KEY(cabin_id) REFERENCES hbxx_cabin(cabin_id),
 CHECK(state IN('AVAILABLE','HELD','SOLD','BLOCKED')),CHECK(seat_fee>=0),
 CHECK((state='AVAILABLE' AND hold_id IS NULL AND order_id IS NULL) OR (state='HELD' AND hold_id IS NOT NULL AND order_id IS NULL) OR (state='SOLD' AND hold_id IS NOT NULL AND order_id IS NOT NULL) OR state='BLOCKED')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS order_passenger (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,order_id INT NOT NULL,passenger_index INT NOT NULL,passenger_name VARCHAR(50) NOT NULL,
 seat_no VARCHAR(6),UNIQUE KEY uk_order_passenger(order_id,passenger_index),FOREIGN KEY(order_id) REFERENCES jpdd(ddid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS order_price_item (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,order_id INT NOT NULL,item_code VARCHAR(30) NOT NULL,item_name VARCHAR(60) NOT NULL,
 amount DECIMAL(10,2) NOT NULL,refundable TINYINT NOT NULL,FOREIGN KEY(order_id) REFERENCES jpdd(ddid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS seat_assignment_history (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,hbid INT NOT NULL,seat_no VARCHAR(6) NOT NULL,hold_id BIGINT NOT NULL,
 action VARCHAR(20) NOT NULL,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,KEY idx_assignment_hold(hold_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_plan (
 id VARCHAR(36) PRIMARY KEY,userid INT NOT NULL,result JSON NOT NULL,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
 KEY idx_ai_owner(userid,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS ai_call_audit (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,userid INT NOT NULL,provider VARCHAR(30) NOT NULL,status VARCHAR(30) NOT NULL,
 duration_ms INT NOT NULL,created_at DATETIME DEFAULT CURRENT_TIMESTAMP,KEY idx_ai_rate(userid,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS add_v5_column;
DELIMITER $$
CREATE PROCEDURE add_v5_column(IN t VARCHAR(64),IN col VARCHAR(64),IN ddl TEXT)
BEGIN
 IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name=t AND column_name=col) THEN
  SET @ddl=CONCAT('ALTER TABLE `',t,'` ADD COLUMN `',col,'` ',ddl);PREPARE q FROM @ddl;EXECUTE q;DEALLOCATE PREPARE q;
 END IF;
END$$
DELIMITER ;
CALL add_v5_column('company','airline_key','VARCHAR(30) UNIQUE');
CALL add_v5_column('company','logo_path','VARCHAR(255)');
CALL add_v5_column('company','source_url','VARCHAR(500)');
CALL add_v5_column('company','verified_at','DATE');
CALL add_v5_column('hbxx','schedule_id','BIGINT');
CALL add_v5_column('hbxx','schedule_version','INT');
CALL add_v5_column('hbxx','service_date','DATE');
CALL add_v5_column('hbxx','departure_airport','CHAR(3)');
CALL add_v5_column('hbxx','arrival_airport','CHAR(3)');
CALL add_v5_column('hbxx','arrival_time','DATETIME');
CALL add_v5_column('hbxx','departure_terminal','VARCHAR(10)');
CALL add_v5_column('hbxx','arrival_terminal','VARCHAR(10)');
CALL add_v5_column('hbxx','duration_minutes','INT');
CALL add_v5_column('hbxx','layout_code','VARCHAR(30)');
CALL add_v5_column('hbxx','policy_code','VARCHAR(40)');
CALL add_v5_column('hbxx','source_type','VARCHAR(20) NOT NULL DEFAULT ''LEGACY''');
CALL add_v5_column('hbxx','sale_open_at','DATETIME');
CALL add_v5_column('hbxx','sale_close_at','DATETIME');
CALL add_v5_column('hbxx','operational_status','VARCHAR(20) NOT NULL DEFAULT ''SCHEDULED''');
CALL add_v5_column('seat_hold','quote_id','VARCHAR(36)');
CALL add_v5_column('seat_hold','price_snapshot','JSON');
CALL add_v5_column('seat_hold','total_cost','DECIMAL(10,2)');
CALL add_v5_column('seat_hold','request_fingerprint','VARCHAR(64)');
CALL add_v5_column('jpdd','price_snapshot','JSON');
CALL add_v5_column('jpdd','refund_amount','DECIMAL(10,2)');
CALL add_v5_column('jpdd','pricing_mode','VARCHAR(30) NOT NULL DEFAULT ''LEGACY_UNSPLIT''');
CALL add_v5_column('tdjp','refund_amount','DECIMAL(10,2)');
DROP PROCEDURE add_v5_column;
DROP PROCEDURE IF EXISTS v5_indexes;
DELIMITER $$
CREATE PROCEDURE v5_indexes()
BEGIN
 IF EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='hbxx' AND index_name='uk_flight_no') THEN
  ALTER TABLE hbxx DROP INDEX uk_flight_no;
 END IF;
 IF NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='hbxx' AND index_name='uk_schedule_date') THEN
  ALTER TABLE hbxx ADD UNIQUE KEY uk_schedule_date(schedule_id,service_date),
   ADD UNIQUE KEY uk_flight_segment(company_id,flight_no,service_date,departure_airport,arrival_airport),
   ADD KEY idx_flight_calendar(service_date,departure_airport,arrival_airport,sale_status);
 END IF;
END$$
DELIMITER ;
CALL v5_indexes();
DROP PROCEDURE v5_indexes;
CREATE OR REPLACE VIEW v_seat_health AS
SELECT c.cabin_id,c.hbid,c.total_number,c.available_number,COUNT(s.seat_no) seat_count,
 SUM(s.state='AVAILABLE') free_seats,SUM(s.state='HELD') held_seats,SUM(s.state='SOLD') sold_seats,
 c.available_number-SUM(s.state='AVAILABLE') difference
FROM hbxx_cabin c JOIN flight_seat s ON s.cabin_id=c.cabin_id GROUP BY c.cabin_id,c.hbid,c.total_number,c.available_number;
INSERT IGNORE INTO schema_version(version)VALUES(5);
