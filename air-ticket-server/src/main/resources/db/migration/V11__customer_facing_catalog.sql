-- Presentation updates only. Preserve inventory, prices, rules and order snapshots.
UPDATE flight_schedule SET flight_no=CONCAT('航班',SUBSTRING(flight_no,3))
WHERE code LIKE 'REF-%' AND flight_no REGEXP '^参考[0-9]+$';
UPDATE hbxx h JOIN flight_schedule s ON s.id=h.schedule_id
SET h.flight_no=s.flight_no WHERE h.flight_no REGEXP '^参考[0-9]+$' AND s.code LIKE 'REF-%';
UPDATE fare_policy SET name='国内成人票',description=CONCAT('托运行李',baggage_kg,'kg/人。费用包含基础运价、税费和燃油附加费。退订按购票时规则办理。')
WHERE name REGEXP '演示|课程|参考' OR description REGEXP '演示|课程|参考|非实时';
UPDATE aircraft_layout SET name=REPLACE(REPLACE(name,'课程演示',''),'演示','') WHERE name LIKE '%演示%';
UPDATE company SET company_purport=NULL WHERE company_purport LIKE '%课程演示%';
UPDATE company SET company_culture=NULL WHERE company_culture LIKE '资料依据航空公司官网整理%';
INSERT INTO schema_version(version) VALUES(11);
