"""Build reviewed course seed SQL; no live price or timetable is implied."""
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
AIRLINES=[
 ('airchina','中国国际航空','总部位于北京的航空公司，提供国内及国际航空运输服务。','https://www.airchina.com.cn/zh-CN/content/about_us/company/'),
 ('ceair','中国东方航空','总部位于上海的航空公司，经营国内及国际航空运输业务。','https://globalgt.ceair.com/global/static/AboutChinaEasternAirlines/intoEasternAirlines/chinaeasternInto/'),
 ('csair','中国南方航空','提供国内及国际客运服务，运输和客票规则以南航公布的对应产品条件为准。','https://www.csair.com/newh5/cn/tourguide/booking/orders/order/gnysztj/'),
 ('hnair','海南航空','提供国内及国际航空运输服务，品牌与公司信息可查阅海南航空官网。','https://www.hnair.com/guanyuhaihang/hhgs/gsjj/'),
 ('shenzhen','深圳航空','以深圳为重要基地的航空公司，提供国内及国际航空运输服务。','https://www.shenzhenair.com/szair_B2C/getStaticHtml.action?htmlName=aboutUiue'),
 ('xiamen','厦门航空','成立于1984年，总部位于福建厦门，提供航空运输服务。','https://www.xiamenair.com/zh-my/article-detail?articleLink=%2Fcms-i18n-ow%2Fcms-zh-my%2Fcontents%2F3988.json'),
 ('sichuan','四川航空','以四川为重要基地的航空公司，经营航空运输业务，品牌强调四川地域文化。','https://www.sichuanair.com/about-introduce.html'),
 ('shandong','山东航空','成立于1994年，以济南、青岛、烟台为主要基地开展航空运输业务。','https://www.sda.cn/about/introductionAir.html'),
 ('juneyao','吉祥航空','以上海、南京为航线网络中心的航空公司，提供航空运输及相关服务。','https://www.juneyaoair.com/about/company-profile'),
 ('spring','春秋航空','在上海成立的航空公司，以低成本航空业务模式经营国内、国际及地区航空运输。','https://www.ch.com/invester'),
 ('capital','首都航空','提供航空客运及相关服务，机票和运输服务说明可查阅首都航空官方网站。','https://jdair.net/'),
 ('tianjin','天津航空','提供航空客运及相关服务，公司资料和服务公告可查阅天津航空官方网站。','https://www.tianjin-air.com/'),
 ('united','中国联合航空','总部位于北京，以北京大兴国际机场为主运营基地，定位于创新经济型航空。','https://info.flycua.com/jcms/publish/newb2c/intro/'),
 ('west','西部航空','经营航空运输业务，承运人信息与服务规则可查阅西部航空官方运输条件。','https://www.westair.cn/stcontent/blankpage/2d169727-f098-4a9b-980d-d1cba2a63b0d')
]
AIRPORTS=[('PEK','北京','北京首都国际机场','T3'),('PKX','北京','北京大兴国际机场','航站楼'),('SHA','上海','上海虹桥国际机场','T2'),('PVG','上海','上海浦东国际机场','T2'),('CAN','广州','广州白云国际机场','T2'),('SZX','深圳','深圳宝安国际机场','T3'),('TFU','成都','成都天府国际机场','T2'),('CTU','成都','成都双流国际机场','T2'),('HGH','杭州','杭州萧山国际机场','T3'),('NKG','南京','南京禄口国际机场','T2'),('XMN','厦门','厦门高崎国际机场','T3'),('FOC','福州','福州长乐国际机场','航站楼'),('TNA','济南','济南遥墙国际机场','航站楼'),('TAO','青岛','青岛胶东国际机场','航站楼'),('HAK','海口','海口美兰国际机场','T2'),('SYX','三亚','三亚凤凰国际机场','航站楼'),('CKG','重庆','重庆江北国际机场','T3'),('XIY','西安','西安咸阳国际机场','T3'),('WUH','武汉','武汉天河国际机场','T3'),('CSX','长沙','长沙黄花国际机场','T2'),('KMG','昆明','昆明长水国际机场','航站楼'),('HRB','哈尔滨','哈尔滨太平国际机场','航站楼'),('TSN','天津','天津滨海国际机场','T2'),('CGO','郑州','郑州新郑国际机场','T2'),('URC','乌鲁木齐','乌鲁木齐天山国际机场','航站楼')]
# Airport names are reference catalog labels; terminal assignment on demo flights is explicitly simulated.
PAIRS=[('PEK','SHA',140),('PKX','PVG',145),('PEK','CAN',200),('SHA','SZX',155),('PVG','TFU',185),('CAN','CTU',150),('SZX','CKG',130),('HGH','XIY',145),('NKG','KMG',180),('XMN','PEK',175),('FOC','SHA',100),('TNA','CAN',180),('TAO','TFU',180),('HAK','PEK',235),('SYX','SHA',190),('CKG','WUH',95),('XIY','CSX',115),('KMG','HGH',175),('HRB','PEK',130),('TSN','SZX',200),('CGO','PVG',115),('URC','PEK',235),('TFU','XMN',160),('CAN','HGH',140),('SHA','NKG',65),('PEK','XIY',130),('PVG','HAK',170),('SZX','SYX',90)]
def q(v):return "'"+str(v).replace("'","''")+"'"
def build():
 s=['-- Official profiles; all schedules, terminal assignments, fares and layouts are COURSE DEMO data.','DROP PROCEDURE IF EXISTS seed_v6;','DELIMITER $$','CREATE PROCEDURE seed_v6()','BEGIN','IF NOT EXISTS(SELECT 1 FROM schema_version WHERE version=6) THEN']
 for key,name,profile,url in AIRLINES:
  s.append(f"INSERT INTO company(company_name) SELECT {q(name)} WHERE NOT EXISTS(SELECT 1 FROM company WHERE company_name={q(name)});")
  s.append(f"UPDATE company SET airline_key={q(key)},company_profile={q(profile)},company_picture={q('/images/airlines/'+key+'/cover.webp')},source_url={q(url)},verified_at='2026-11-20',company_culture='资料依据航空公司官网整理，详细信息请查阅官方来源。',company_purport='本项目展示的航班、票价、行李与服务规则为课程演示配置。' WHERE company_name={q(name)};")
 for code,city,name,terminal in AIRPORTS:s.append(f"INSERT IGNORE INTO airport(code,city,name,terminal)VALUES({q(code)},{q(city)},{q(name)},{q(terminal)});")
 for code,name in [('DEMO_Y180_V1','窄体经济舱演示布局 · 180座'),('DEMO_C16Y156_V1','窄体两舱演示布局 · 172座')]:
  s.append(f"INSERT IGNORE INTO aircraft_layout(code,name,aircraft_type)VALUES({q(code)},{q(name)},'窄体客机（演示）');")
  values=[]
  for row in range(1,31):
   letters='ACDF' if code=='DEMO_C16Y156_V1' and row<=4 else 'ABCDEF'
   cabin=2 if code=='DEMO_C16Y156_V1' and row<=4 else 1
   for letter in letters:values.append('('+','.join(map(q,[code,str(row)+letter,row,letter,cabin,20 if row in (5,6) and letter in 'AF' else 0]))+')')
  s.append('INSERT IGNORE INTO layout_seat(layout_code,seat_no,seat_row,seat_column,cabin_class,seat_fee)VALUES'+','.join(values)+';')
 for code,name,baggage in [('DEMO_STANDARD_V1','标准成人演示产品',20),('DEMO_LIGHT_V1','轻装成人演示产品',0)]:
  desc=f'教学演示规则：基础运价另加演示税费50元、演示附加费40元；含托运行李{baggage}kg；起飞24小时前退订收基础运价10%，24小时内至起飞2小时前收20%；税费、附加费和选座费可退。无真实出票效力。'
  s.append(f"INSERT IGNORE INTO fare_policy(code,name,tax_fee,fuel_fee,baggage_kg,early_refund_rate,late_refund_rate,description,effective_from,effective_to)VALUES({q(code)},{q(name)},50,40,{baggage},0.10,0.20,{q(desc)},'2020-01-01','2099-12-31');")
 n=0
 for i,(origin,dest,duration) in enumerate(PAIRS):
  for a,b in [(origin,dest),(dest,origin)]:
   for hour in (8,16):
    n+=1;key,name,_,_=AIRLINES[(n-1)%len(AIRLINES)];light=key in ('spring','united','west')
    s.append(f"INSERT IGNORE INTO flight_schedule(code,company_id,flight_no,departure_airport,arrival_airport,departure_time,duration_minutes,valid_from,valid_to,layout_code,policy_code,base_price)SELECT 'COURSE-S{n:03d}',company_id,'DEMO{n:04d}',{q(a)},{q(b)},'{hour:02d}:{(i%4)*10:02d}:00',{duration},'2020-01-01','2099-12-31',{q('DEMO_Y180_V1' if light else 'DEMO_C16Y156_V1')},{q('DEMO_LIGHT_V1' if light else 'DEMO_STANDARD_V1')},{380+i*23+(0 if light else 60)} FROM company WHERE airline_key={q(key)};")
 s+=['INSERT INTO schema_version(version)VALUES(6);','END IF;','END$$','DELIMITER ;','CALL seed_v6();','DROP PROCEDURE seed_v6;']
 target=ROOT/'.local/generated/V6__official_airlines_demo_catalog.sql'
 target.parent.mkdir(parents=True,exist_ok=True)
 target.write_text('\n'.join(s)+'\n',encoding='utf-8')
 print('Review-only output:',target,'; published migrations are never overwritten')
 print('Built catalog:',len(AIRLINES),'airlines,',len(AIRPORTS),'airports,',n,'daily templates')
if __name__=='__main__':build()
