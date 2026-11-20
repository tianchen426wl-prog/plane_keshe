-- Keep the teaching stored-procedure entry point consistent with seat and fare snapshots.
DROP PROCEDURE IF EXISTS sp_approve_refund;
DELIMITER $$
CREATE PROCEDURE sp_approve_refund(IN p_tdjp_id INT,OUT p_result INT)
proc: BEGIN
 DECLARE v_order INT;DECLARE v_flight INT;DECLARE v_cabin INT;DECLARE v_qty INT;
 DECLARE v_status INT;DECLARE v_process INT;DECLARE v_hold BIGINT;DECLARE v_cost DECIMAL(10,2);
 DECLARE v_amount DECIMAL(10,2);DECLARE v_snapshot JSON;DECLARE v_dummy INT;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK;SET p_result=0;RESIGNAL;END;
 SET p_result=-1;
 SELECT MAX(ddid) INTO v_order FROM tdjp WHERE id=p_tdjp_id;
 IF v_order IS NULL THEN LEAVE proc;END IF;
 START TRANSACTION;
 SELECT hbid,cabin_id,quantity,status,hold_id,cost,price_snapshot INTO v_flight,v_cabin,v_qty,v_status,v_hold,v_cost,v_snapshot FROM jpdd WHERE ddid=v_order FOR UPDATE;
 SELECT process,refund_amount INTO v_process,v_amount FROM tdjp WHERE id=p_tdjp_id FOR UPDATE;
 IF v_process=1 AND v_status=3 THEN COMMIT;SET p_result=1;LEAVE proc;END IF;
 IF v_process<>0 OR v_status<>2 THEN ROLLBACK;SET p_result=-2;LEAVE proc;END IF;
 IF v_snapshot IS NOT NULL AND v_amount IS NULL THEN ROLLBACK;SET p_result=-4;LEAVE proc;END IF;
 SELECT hbid INTO v_dummy FROM hbxx WHERE hbid=v_flight FOR UPDATE;
 SELECT cabin_id INTO v_dummy FROM hbxx_cabin WHERE cabin_id=v_cabin FOR UPDATE;
 UPDATE hbxx_cabin SET available_number=available_number+v_qty WHERE cabin_id=v_cabin AND available_number+v_qty<=total_number;
 IF ROW_COUNT()<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Refund inventory invariant failed';END IF;
 INSERT INTO seat_assignment_history(hbid,seat_no,hold_id,action) SELECT hbid,seat_no,hold_id,'RELEASE' FROM flight_seat WHERE hold_id=v_hold AND state IN('HELD','SOLD');
 UPDATE flight_seat SET state='AVAILABLE',hold_id=NULL,order_id=NULL WHERE hold_id=v_hold AND state IN('HELD','SOLD');
 UPDATE jpdd SET status=3,refund_amount=COALESCE(v_amount,v_cost) WHERE ddid=v_order;
 UPDATE tdjp SET process=1,review_reason='数据库存储过程审核',review_time=NOW() WHERE id=p_tdjp_id;
 INSERT INTO order_log(ddid,action,operator,remark)VALUES(v_order,'REFUND_APPROVE','DATABASE','存储过程审核，按申请时金额退款并释放座位');
 COMMIT;SET p_result=1;
END$$
DELIMITER ;
INSERT IGNORE INTO schema_version(version)VALUES(8);
