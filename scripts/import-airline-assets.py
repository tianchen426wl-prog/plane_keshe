"""Import official brand assets located by the research scripts; validate and record provenance."""
import base64, concurrent.futures, datetime, hashlib, io, json, shutil
from pathlib import Path
from urllib.parse import urlparse
import requests
from PIL import Image
from build_flight_catalog import AIRLINES

ROOT=Path(__file__).resolve().parents[1]
ALLOWED=('airchina.com.cn','ceair.com','csair.com','hnair.com','shenzhenair.com','xiamenair.cn','xiamenair.com','sichuanair.com','sda.cn','juneyaoair.com','springairlines.com','jdair.net','tianjin-air.com','flycua.com','westair.cn')
FIXED={
 'csair':'https://www.csair.com/cn/new/images/logo70_CN.png',
 'hnair':'https://www.hnair.com/images/logo.png',
 'shenzhen':'https://www.shenzhenair.com/szair_B2C/static/ued/images/header/logo_202511new.png',
 'xiamen':'https://rescms.xiamenair.cn/brandnew_CN/upload/images/2018/4/2394745259.png',
 'sichuan':'https://www.sichuanair.com/3uair/theme/scal_201607/img/logo.png',
 'shandong':'https://www.sda.cn/imgs/logo.36d0f3b.png',
 'juneyao':'https://staticb2c.juneyaoair.com/b2cpro1795082079269/logo-new.Uk7TvbkQ.png',
 'spring':'https://media.springairlines.com/cache/style/site5/img/logo.png?vs=20160803',
 'capital':'https://jdair.net/_next/static/media/logo.JD.09159f83.png',
 'tianjin':'https://www.tianjin-air.com/dotCms/webfile/content/header/Homepage-headerlog-red_zh_CN.png?language_id=1',
 'united':'https://info.flycua.com/cuacms/upimg/%E8%81%94%E8%88%AA%E5%9B%BE%E6%A0%87_1638427706350.jpg',
 'west':'https://www.westair.cn/dotCms/webfile/content/hearderbar/logo%20-dark_zh_CN.png?language_id=1'
}
def allowed(url):
 p=urlparse(url);return p.scheme=='https' and any(p.hostname==d or (p.hostname or '').endswith('.'+d) for d in ALLOWED)
def fetch(url):
 for _ in range(4):
  if not allowed(url):raise ValueError('Unapproved asset domain')
  r=requests.get(url,timeout=25,allow_redirects=False,stream=True,headers={'User-Agent':'Mozilla/5.0'})
  if r.is_redirect:
   from urllib.parse import urljoin
   url=urljoin(url,r.headers['Location']);continue
  r.raise_for_status()
  if not r.headers.get('Content-Type','').lower().startswith('image/'):raise ValueError('Not an image MIME')
  data=b''
  for part in r.iter_content(65536):
   data+=part
   if len(data)>8*1024*1024:raise ValueError('Image too large')
  return data,url
 raise ValueError('Too many redirects')
def run(record):
 key,name,_,page=record
 if key in FIXED:raw,url=fetch(FIXED[key]);source_kind='official_image_url'
 else:
  research=json.loads((ROOT/'.local/airline-browser-research.json').read_text(encoding='utf-8'))
  source=research[key]['images'][0]['src']
  if not source.startswith('data:image/png;base64,'):raise ValueError('Expected embedded brand image')
  raw=base64.b64decode(source.split(',',1)[1],validate=True);url=page;source_kind='official_page_embedded_png'
 if len(raw)>8*1024*1024:raise ValueError('Image too large')
 Image.MAX_IMAGE_PIXELS=12000000
 image=Image.open(io.BytesIO(raw));image.load()
 if image.width<80 or image.height<20:raise ValueError('Brand image unexpectedly small')
 folder=ROOT/'air-ticket-web/portal/public/images/airlines'/key;folder.mkdir(parents=True,exist_ok=True)
 image.convert('RGBA').save(folder/'logo.png')
 image.convert('RGBA').save(folder/'cover.webp',format='WEBP',lossless=True)
 target=ROOT/'air-ticket-web/admin/public/images/airlines'/key;target.mkdir(parents=True,exist_ok=True)
 for filename in ['logo.png','cover.webp']:shutil.copy2(str(folder/filename),str(target/filename))
 print(key,'downloaded',image.width,'x',image.height)
 return {'airlineKey':key,'airlineName':name,'assetType':'official_brand_image','sourcePage':page,'sourceImage':url,'sourceKind':source_kind,'retrievedAt':datetime.date.today().isoformat(),'copyright':'航空公司或其权利人保留版权；课程品牌识别展示，非开放许可声明','usage':'课程演示；公开分发前核实授权条件','file':'air-ticket-web/portal/public/images/airlines/'+key+'/cover.webp','logoFile':'air-ticket-web/portal/public/images/airlines/'+key+'/logo.png','sha256':hashlib.sha256((folder/'cover.webp').read_bytes()).hexdigest(),'sourceSha256':hashlib.sha256(raw).hexdigest(),'width':image.width,'height':image.height,'status':'downloaded_verified_image'}
if __name__=='__main__':
 with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:results=list(pool.map(run,AIRLINES))
 target=ROOT/'docs/assets/airline-assets.json';target.parent.mkdir(parents=True,exist_ok=True);target.write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print('Saved',len(results),'official brand assets and provenance records')
