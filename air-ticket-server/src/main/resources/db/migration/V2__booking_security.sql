-- Run once against the selected database; no USE statement and no data reset.
CREATE TABLE IF NOT EXISTS schema_version(version INT PRIMARY KEY, applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
DROP PROCEDURE IF EXISTS upgrade_v2;
DELIMITER $$
CREATE PROCEDURE upgrade_v2()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM schema_version WHERE version=2) THEN
  ALTER TABLE `user` ADD status TINYINT NOT NULL DEFAULT 1, ADD auth_version INT NOT NULL DEFAULT 0;
  ALTER TABLE admin ADD status TINYINT NOT NULL DEFAULT 1, ADD auth_version INT NOT NULL DEFAULT 0;
  ALTER TABLE hbxx ADD sale_status TINYINT NOT NULL DEFAULT 1, ADD flight_no VARCHAR(30);
  UPDATE hbxx SET flight_no=CONCAT('DEMO',hbid) WHERE flight_no IS NULL;
  ALTER TABLE hbxx_cabin ADD status TINYINT NOT NULL DEFAULT 1;
  ALTER TABLE seat_hold ADD unit_price DECIMAL(10,2), ADD request_id VARCHAR(64),
    ADD UNIQUE KEY uk_hold_request(userid,request_id), ADD KEY idx_hold_cleanup(status,expire_time,hold_id),
    ADD CONSTRAINT chk_hold_quantity CHECK(quantity BETWEEN 1 AND 5);
  UPDATE seat_hold s JOIN hbxx_cabin c ON c.cabin_id=s.cabin_id SET s.unit_price=c.price;
  ALTER TABLE jpdd ADD hold_id BIGINT, ADD order_no VARCHAR(40), ADD flight_snapshot TEXT,
    ADD UNIQUE KEY uk_order_hold(hold_id), ADD UNIQUE KEY uk_order_no(order_no);
  UPDATE jpdd SET order_no=CONCAT('LEGACY',ddid) WHERE order_no IS NULL;
  ALTER TABLE tdjp ADD review_reason VARCHAR(500), ADD reviewer_id INT, ADD review_time DATETIME;
  CREATE TABLE admin_menu(admin_id INT NOT NULL, menu_id INT NOT NULL,
    PRIMARY KEY(admin_id,menu_id), FOREIGN KEY(admin_id) REFERENCES admin(id) ON DELETE CASCADE,
    FOREIGN KEY(menu_id) REFERENCES menu(id) ON DELETE CASCADE);
  INSERT INTO admin_menu SELECT a.id,m.id FROM admin a JOIN menu m
    ON FIND_IN_SET(m.id,REPLACE(COALESCE(a.menu_id,''),' ',''))>0;
  CREATE TABLE inventory_repair_log(id BIGINT AUTO_INCREMENT PRIMARY KEY,hbid INT NOT NULL,
    old_number INT, cabin_available INT, reason VARCHAR(255), repaired_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
  INSERT INTO inventory_repair_log(hbid,old_number,cabin_available,reason)
    SELECT h.hbid,h.number,COALESCE(c.n,0),'V2: retire stale flight cache; preserve cabin capacity, holds and orders'
    FROM hbxx h LEFT JOIN (SELECT hbid,SUM(available_number) n FROM hbxx_cabin GROUP BY hbid)c ON c.hbid=h.hbid
    WHERE h.number<>COALESCE(c.n,0);
  UPDATE hbxx h LEFT JOIN (SELECT hbid,SUM(available_number)n FROM hbxx_cabin GROUP BY hbid)c ON c.hbid=h.hbid
    SET h.number=COALESCE(c.n,0);
  INSERT INTO schema_version(version) VALUES(2);
 END IF;
END$$
DELIMITER ;
CALL upgrade_v2();
DROP PROCEDURE upgrade_v2;
