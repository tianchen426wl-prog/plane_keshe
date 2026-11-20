"""Reproducible V9 reference catalog, not a live airline inventory feed.

Route durations and economy base fares below are editable planning estimates.
Each entry is an airport pair with independent outbound/return block times.
Do not overwrite a published migration after deployment.
"""
from pathlib import Path
from build_flight_catalog import AIRPORTS, q

ROOT = Path(__file__).resolve().parents[1]
EXTRA = [
    ('JHG','西双版纳','西双版纳嘎洒国际机场'), ('DIG','香格里拉','迪庆香格里拉机场'),
    ('LXA','拉萨','拉萨贡嘎国际机场'), ('LJG','丽江','丽江三义国际机场'),
    ('DLU','大理','大理凤仪机场'), ('TCZ','腾冲','腾冲驼峰机场'),
    ('SHE','沈阳','沈阳桃仙国际机场'), ('CGQ','长春','长春龙嘉国际机场'),
    ('DLC','大连','大连周水子国际机场'), ('YNJ','延吉','延吉朝阳川国际机场'),
    ('NBS','长白山','长白山机场'), ('MDG','牡丹江','牡丹江海浪国际机场'),
    ('JMU','佳木斯','佳木斯东郊机场'), ('HEK','黑河','黑河瑷珲机场'),
    ('OHE','漠河','漠河古莲机场'), ('HLD','海拉尔','呼伦贝尔海拉尔机场'),
    ('HET','呼和浩特','呼和浩特白塔国际机场'), ('SJW','石家庄','石家庄正定国际机场'),
    ('TYN','太原','太原武宿国际机场'), ('HFE','合肥','合肥新桥国际机场'),
    ('KHN','南昌','南昌昌北国际机场'), ('NNG','南宁','南宁吴圩国际机场'),
    ('KWL','桂林','桂林两江国际机场'), ('BHY','北海','北海福成机场'),
    ('KWE','贵阳','贵阳龙洞堡国际机场'), ('ZUH','珠海','珠海金湾机场'),
    ('NGB','宁波','宁波栎社国际机场'), ('WNZ','温州','温州龙湾国际机场'),
    ('WUX','无锡','无锡硕放机场'), ('YNT','烟台','烟台蓬莱国际机场'),
    ('WEH','威海','威海大水泊机场'), ('LHW','兰州','兰州中川国际机场'),
    ('XNN','西宁','西宁曹家堡国际机场'), ('INC','银川','银川河东国际机场'),
    ('DYG','张家界','张家界荷花国际机场'), ('TXN','黄山','黄山屯溪国际机场'),
    ('DNH','敦煌','敦煌莫高国际机场'), ('KHG','喀什','喀什徕宁国际机场'),
    ('YIN','伊宁','伊宁机场'), ('AAT','阿勒泰','阿勒泰雪都机场'),
]

# dest: outbound minutes / return minutes / reference base fare (CNY).
# Explicit lists avoid inventing a direct flight between every pair of cities.
NETWORK = [
 ('PEK','airchina','SHA:135/145/680 CAN:205/190/850 SZX:210/195/880 TFU:180/165/780 HGH:135/145/620 NKG:120/130/580 XMN:180/175/760 FOC:175/170/720 TNA:75/80/380 TAO:95/100/420 HAK:230/220/950 SYX:250/235/1080 CKG:175/160/720 XIY:135/120/580 WUH:140/125/620 CSX:155/145/650 KMG:225/200/850 HRB:120/130/520 TSN:0/0/0 CGO:105/100/480 URC:255/220/1280 SHE:95/105/480 CGQ:115/130/520 DLC:90/90/420 HET:80/85/380 TYN:75/80/380 HFE:125/130/520 KHN:150/140/620 NNG:215/195/820 KWL:190/175/760 KWE:190/170/760 ZUH:215/200/820 NGB:145/145/620 WNZ:155/155/660 YNT:90/90/420 WEH:100/100/450 LHW:155/140/650 XNN:165/150/720 INC:120/110/520 LXA:270/225/1450 LJG:235/210/980 DLU:240/210/980 JHG:260/225/1080 HLD:135/145/650 MDG:140/145/620 JMU:140/150/620'),
 ('PVG','ceair','CAN:155/145/620 SZX:155/145/650 TFU:195/165/780 XMN:110/105/460 FOC:100/95/420 HAK:200/175/820 SYX:205/180/920 CKG:180/150/720 XIY:155/135/650 WUH:115/95/450 CSX:135/115/520 KMG:215/180/850 HRB:180/190/760 TSN:135/130/550 CGO:125/110/480 URC:315/265/1480 SHE:155/155/650 CGQ:165/175/680 DLC:120/125/520 SJW:145/125/550 TYN:155/135/580 KHN:100/85/420 NNG:195/165/780 KWL:170/140/680 KWE:180/150/720 ZUH:165/150/650 YNT:110/115/480 LHW:200/165/820 XNN:215/180/850 INC:180/155/720 LJG:220/185/920 DLU:225/190/980 JHG:240/205/1080 YNJ:180/185/850 NBS:165/175/820 TXN:70/65/350'),
 ('CAN','csair','SZX:0/0/0 TFU:155/135/650 HGH:125/140/550 NKG:130/145/550 XMN:85/90/380 FOC:95/100/420 TNA:175/180/720 TAO:180/185/760 HAK:75/70/320 SYX:95/90/420 CKG:130/115/520 XIY:155/140/620 WUH:110/105/450 CSX:85/80/380 KMG:145/115/550 HRB:255/285/1180 CGO:145/140/580 URC:330/290/1480 SHE:225/245/980 CGQ:245/260/1080 DLC:200/210/880 HFE:120/130/480 KHN:90/95/380 NNG:85/80/350 KWL:70/65/320 KWE:110/95/450 ZUH:0/0/0 LHW:190/170/780 LJG:185/150/780 JHG:170/140/720 DYG:95/90/420'),
 ('TFU','sichuan','SZX:150/130/620 HGH:165/180/680 NKG:155/175/650 XMN:170/180/720 FOC:170/180/720 TNA:145/160/620 TAO:160/180/680 HAK:150/145/620 SYX:165/155/720 CKG:0/0/0 XIY:90/85/380 WUH:120/130/480 CSX:115/125/480 KMG:95/85/380 HRB:230/250/1080 CGO:120/130/480 URC:225/200/1080 SHE:205/225/950 CGQ:220/240/980 KWE:80/75/320 NNG:110/105/450 LHW:90/85/380 XNN:100/95/420 INC:110/100/450 LXA:145/125/820 LJG:90/85/450 DLU:95/90/450 JHG:120/110/550 DIG:100/95/620 DNH:150/145/720'),
 ('KMG','ceair','JHG:65/70/350 DIG:75/65/480 LJG:60/55/320 DLU:55/55/320 TCZ:70/65/420 LXA:180/150/880 KWE:70/65/320 CKG:95/90/420 WUH:145/130/580 CSX:125/115/520 XIY:145/130/580 SZX:140/120/580 NNG:90/80/380'),
 ('HRB','csair','SHE:70/75/350 DLC:100/105/420 CGQ:0/0/0 HET:145/150/650 HLD:70/75/380 OHE:95/100/520 HEK:65/70/350 JMU:65/65/350 MDG:55/60/320 HGH:190/205/820 NKG:180/195/780 XIY:210/200/880 SYX:350/345/1480'),
 ('URC','csair','KHG:120/110/550 YIN:65/60/380 AAT:75/65/420 LHW:155/165/650 XIY:200/220/850'),
 ('SZX','shenzhen','HGH:125/140/550 NKG:140/155/580 CGQ:250/265/1080 SHE:235/250/980 WUX:140/155/580 BHY:75/70/350'),
 ('HGH','ceair','LJG:210/180/850 JHG:225/195/980 DYG:135/120/620'),
 ('SHE','csair','YNJ:75/80/380'),
 ('CTU','sichuan','LXA:140/120/820'),
 ('PKX','united','PVG:145/150/650 TFU:180/165/750'),
]

# Reference distance bands are reviewed separately from block time (which includes taxiing).
SHORT_PAIRS = {frozenset(pair.split('-')) for pair in '''
PEK-TNA PEK-TAO PEK-SHE PEK-CGQ PEK-DLC PEK-HET PEK-TYN PEK-CGO PEK-YNT PEK-WEH
PVG-XMN PVG-FOC PVG-WUH PVG-KHN PVG-YNT PVG-TXN
CAN-XMN CAN-FOC CAN-HAK CAN-SYX CAN-CSX CAN-NNG CAN-KWL CAN-KWE CAN-KHN CAN-DYG
TFU-XIY TFU-WUH TFU-CSX TFU-KMG TFU-KWE TFU-LHW TFU-XNN TFU-LJG TFU-DLU TFU-DIG
KMG-JHG KMG-DIG KMG-LJG KMG-DLU KMG-TCZ KMG-KWE KMG-CKG KMG-NNG
HRB-SHE HRB-DLC HRB-HLD HRB-OHE HRB-HEK HRB-JMU HRB-MDG URC-YIN URC-AAT
SZX-BHY SHE-YNJ
'''.split()}

def routes():
    result = {}
    for origin, airline, entries in NETWORK:
        for entry in entries.split():
            dest, details = entry.split(':')
            outward, inward, fare = map(int, details.split('/'))
            if not outward:  # Intentionally no invented short-hop service.
                continue
            key = tuple(sorted((origin, dest)))
            assert key not in result, key
            result[key] = (origin, dest, airline, outward, inward, fare)
    return list(result.values())

def build():
    sql = ['-- V9: reference domestic routes, reviewed 2026-11-20. NOT live inventory.',
           '-- Existing flights/orders/quotes/seats are preserved. Retire old template set only.',
           "UPDATE flight_schedule SET enabled=0 WHERE code LIKE 'COURSE-S%';"]
    for code, city, name in EXTRA:
        sql.append(f"INSERT IGNORE INTO airport(code,city,name,terminal) VALUES({q(code)},{q(city)},{q(name)},'以出票信息为准');")
    # Latest checked adult domestic fuel reference: Eastern notice 2026-11-04.
    for band, fuel in [('SHORT',80),('LONG',140)]:
        desc = f'成人参考产品：基础票价另加民航发展基金50元、燃油附加费{fuel}元（参照2026-11-05标准）；托运行李20kg。退票比例和座位为课程配置。票价、时刻非实时，请以航司实际查询为准。'
        sql.append(f"INSERT IGNORE INTO fare_policy(code,name,tax_fee,fuel_fee,baggage_kg,early_refund_rate,late_refund_rate,description,effective_from,effective_to) VALUES('REF_{band}_202611','国内成人参考产品',50,{fuel},20,0.10,0.20,{q(desc)},'2026-11-05','2099-12-31');")
    count = 0
    for index, (a,b,airline,outward,inward,fare) in enumerate(routes()):
        # Daily reference timetable; offset times per route; no claim of exact real flight number.
        for direction, (origin,dest,minutes) in enumerate([(a,b,outward),(b,a,inward)]):
            count += 1
            hour = (7 + index % 7) if not direction else (14 + index % 5)
            time = f'{hour:02d}:{(index%6)*10:02d}:00'
            if (origin,dest) == ('KMG','JHG'):
                airline, time, minutes = 'csair', '11:45:00', 55
            elif (origin,dest) == ('JHG','KMG'):
                airline, time, minutes = 'csair', '13:20:00', 65
            band = 'SHORT' if frozenset((origin,dest)) in SHORT_PAIRS else 'LONG'
            code = f'REF-{origin}-{dest}'
            sql.append("INSERT IGNORE INTO flight_schedule(code,company_id,flight_no,departure_airport,arrival_airport,departure_time,duration_minutes,valid_from,valid_to,layout_code,policy_code,base_price) "
                       f"SELECT {q(code)},{'company_id'},{q('参考'+str(count).zfill(4))},{q(origin)},{q(dest)},{q(time)},{minutes},'2026-11-05','2099-12-31','DEMO_C16Y156_V1','REF_{band}_202611',{fare + direction*30} FROM company WHERE airline_key={q(airline)};")
    sql.append('INSERT IGNORE INTO schema_version(version) VALUES(9);')
    target = ROOT/'air-ticket-server/src/main/resources/db/migration/V9__domestic_reference_network.sql'
    content = '\n'.join(sql)+'\n'
    if target.exists() and target.read_text(encoding='utf-8') != content:
        raise RuntimeError('Published migration differs; create a new migration instead')
    target.write_text(content,encoding='utf-8')
    print(f'{len(AIRPORTS)+len(EXTRA)} airports; {len(routes())} return pairs; {count} daily reference templates')

if __name__ == '__main__':
    build()
