CREATE TABLE IF NOT EXISTS schema_version(version INT PRIMARY KEY, applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
DROP PROCEDURE IF EXISTS upgrade_v3;
DELIMITER $$
CREATE PROCEDURE upgrade_v3()
BEGIN
 IF NOT EXISTS(SELECT 1 FROM schema_version WHERE version=3) THEN
  ALTER TABLE hbxx ADD UNIQUE KEY uk_flight_no(flight_no), ADD CONSTRAINT chk_flight_sale CHECK(sale_status IN(0,1));
  ALTER TABLE hbxx_cabin ADD CONSTRAINT chk_cabin_status CHECK(status IN(0,1)), ADD CONSTRAINT chk_cabin_price CHECK(price>0), ADD CONSTRAINT chk_cabin_total CHECK(total_number>=0);
  ALTER TABLE tdjp ADD CONSTRAINT chk_refund_state CHECK(process IN(0,1,2));
  ALTER TABLE admin ADD CONSTRAINT chk_admin_role CHECK(role IN(0,1)), ADD CONSTRAINT chk_admin_status CHECK(status IN(0,1));
  ALTER TABLE `user` ADD CONSTRAINT chk_user_status CHECK(status IN(0,1));
  INSERT INTO schema_version(version)VALUES(3);
 END IF;
END$$
DELIMITER ;
CALL upgrade_v3();
DROP PROCEDURE upgrade_v3;
