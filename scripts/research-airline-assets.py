"""Inspect official page image URLs; downloads are handled by import-airline-assets.py."""
import concurrent.futures, html, json, re
from urllib.parse import urljoin
from pathlib import Path
import requests

PAGES = {
 'airchina':'https://www.airchina.com.cn/zh-CN/content/about_us/company/',
 'ceair':'https://globalgt.ceair.com/global/static/AboutChinaEasternAirlines/intoEasternAirlines/chinaeasternInto/',
 'csair':'https://www.csair.com/cn/about/gongsijianjie/',
 'hnair':'https://www.hnair.com/guanyuhaihang/hhgs/gsjj/',
 'shenzhen':'https://www.shenzhenair.com/szair_B2C/getStaticHtml.action?htmlName=aboutUiue',
 'xiamen':'https://www.xiamenair.com/',
 'sichuan':'https://www.sichuanair.com/about-introduce.html',
 'shandong':'https://www.sda.cn/about/introductionAir.html',
 'juneyao':'https://www.juneyaoair.com/about/company-profile',
 'spring':'https://www.ch.com/invester',
 'capital':'https://jdair.net/',
 'tianjin':'https://www.tianjin-air.com/',
 'united':'https://info.flycua.com/jcms/publish/newb2c/intro/',
 'west':'https://www.westair.cn/'
}

def inspect(entry):
 key,url=entry
 try:
  r=requests.get(url,timeout=25,headers={'User-Agent':'Mozilla/5.0'})
  r.raise_for_status()
  r.encoding=r.apparent_encoding
  urls=[]
  for s in re.findall(r'''(?:src|data-src)\s*=\s*["']([^"']+)|url\(["']?([^\)'"\s]+)''',r.text,re.I):
   value=html.unescape(s[0] or s[1])
   if value and not value.startswith('data:') and (re.search(r'\.(?:png|jpe?g|gif|webp|svg)(?:[?#]|$)',value,re.I) or '/_next/image?' in value): urls.append(urljoin(r.url,value))
  return key,{'page':r.url,'images':list(dict.fromkeys(urls))[:90]}
 except Exception as e:return key,{'page':url,'error':type(e).__name__}

if __name__=='__main__':
 with concurrent.futures.ThreadPoolExecutor(max_workers=7) as pool: data=dict(pool.map(inspect,PAGES.items()))
 target=Path(__file__).resolve().parents[1]/'.local/airline-research.json'
 target.write_text(json.dumps(data,ensure_ascii=False,indent=2),encoding='utf-8')
 for key,v in data.items():print(key,json.dumps(v,ensure_ascii=True))
