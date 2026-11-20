-- Views follow cabin inventory; historical transactions are never cascade-deleted.
CREATE OR REPLACE VIEW v_hbxx_cabin_summary AS
SELECT h.hbid,h.company_id,c.company_name,h.xclb,h.start,h.`end`,h.starttime,h.duration,h.place,h.price,
 COALESCE(SUM(cab.available_number),0) AS total_remaining,
 COALESCE(SUM(CASE WHEN cab.cabin_class=1 THEN cab.available_number ELSE 0 END),0) economy_remaining,
 COALESCE(SUM(CASE WHEN cab.cabin_class=2 THEN cab.available_number ELSE 0 END),0) business_remaining,
 COALESCE(SUM(CASE WHEN cab.cabin_class=3 THEN cab.available_number ELSE 0 END),0) first_remaining,
 COALESCE(SUM(cab.available_number),0) cabin_total_remaining
FROM hbxx h LEFT JOIN company c ON c.company_id=h.company_id LEFT JOIN hbxx_cabin cab ON cab.hbid=h.hbid
GROUP BY h.hbid,h.company_id,c.company_name,h.xclb,h.start,h.`end`,h.starttime,h.duration,h.place,h.price;
CREATE OR REPLACE VIEW v_inventory_health AS
SELECT c.cabin_id,c.hbid,c.total_number,c.available_number,
 (SELECT COALESCE(SUM(s.quantity),0) FROM seat_hold s WHERE s.cabin_id=c.cabin_id AND s.status=0) AS held_number,
 (SELECT COALESCE(SUM(j.quantity),0) FROM jpdd j WHERE j.cabin_id=c.cabin_id AND j.status IN(1,2)) AS sold_number,
 c.total_number-c.available_number-(SELECT COALESCE(SUM(s.quantity),0) FROM seat_hold s WHERE s.cabin_id=c.cabin_id AND s.status=0)-(SELECT COALESCE(SUM(j.quantity),0) FROM jpdd j WHERE j.cabin_id=c.cabin_id AND j.status IN(1,2)) AS difference
FROM hbxx_cabin c;
DROP TRIGGER IF EXISTS tr_hbxx_before_delete;
DROP TRIGGER IF EXISTS tr_hbxx_cabin_before_delete;
DELIMITER $$
CREATE TRIGGER tr_hbxx_before_delete BEFORE DELETE ON hbxx FOR EACH ROW
BEGIN
 IF EXISTS(SELECT 1 FROM jpdd WHERE hbid=OLD.hbid) OR EXISTS(SELECT 1 FROM seat_hold WHERE hbid=OLD.hbid) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Flight has booking history; disable instead';
 END IF;
 DELETE FROM hbxx_cabin WHERE hbid=OLD.hbid;
END$$
CREATE TRIGGER tr_hbxx_cabin_before_delete BEFORE DELETE ON hbxx_cabin FOR EACH ROW
BEGIN
 IF EXISTS(SELECT 1 FROM jpdd WHERE cabin_id=OLD.cabin_id) OR EXISTS(SELECT 1 FROM seat_hold WHERE cabin_id=OLD.cabin_id) THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Cabin has booking history; disable instead';
 END IF;
END$$
DELIMITER ;
INSERT IGNORE INTO schema_version(version)VALUES(4);
