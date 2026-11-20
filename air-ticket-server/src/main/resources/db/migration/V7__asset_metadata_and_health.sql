-- Finalize local asset metadata and consistency diagnostics without repricing old orders.
UPDATE company SET logo_path=CONCAT('/images/airlines/',airline_key,'/logo.png') WHERE airline_key IS NOT NULL AND logo_path IS NULL;
CREATE OR REPLACE VIEW v_seat_health AS
SELECT c.cabin_id,c.hbid,c.total_number,c.available_number,COUNT(s.seat_no) seat_count,
 SUM(s.state='AVAILABLE') free_seats,SUM(s.state='HELD') held_seats,SUM(s.state='SOLD') sold_seats,
 c.available_number-SUM(s.state='AVAILABLE') difference,
 c.total_number-COUNT(s.seat_no) capacity_difference
FROM hbxx_cabin c JOIN flight_seat s ON s.cabin_id=c.cabin_id GROUP BY c.cabin_id,c.hbid,c.total_number,c.available_number;
INSERT IGNORE INTO schema_version(version)VALUES(7);
