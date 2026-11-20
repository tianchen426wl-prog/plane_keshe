const {chromium}=require('playwright'),fs=require('fs'),path=require('path');
const root=path.resolve(__dirname,'..'),data=JSON.parse(fs.readFileSync(path.join(root,'.local/airline-research.json'),'utf8'));
(async()=>{const browser=await chromium.launch({headless:true,channel:'chrome'});try{const cache=path.join(root,'.local/airline-browser-research.json');const out=fs.existsSync(cache)?JSON.parse(fs.readFileSync(cache,'utf8')):{};
 for(const key of Object.keys(data)){
  const page=await browser.newPage({ignoreHTTPSErrors:true});
  try{await page.goto((key==='csair'?'https://www.csair.com/cn/':data[key].page),{waitUntil:'domcontentloaded',timeout:25000});await page.waitForTimeout(3500);
   out[key]=await page.evaluate(()=>({images:[...document.images].map(i=>({src:i.currentSrc||i.src,alt:i.alt,width:i.naturalWidth,height:i.naturalHeight})).filter(i=>i.width>80&&i.height>20),backgrounds:[...document.querySelectorAll('*')].slice(0,800).map(e=>getComputedStyle(e).backgroundImage).filter(x=>x.includes('url('))}));
   console.log(key,JSON.stringify({...out[key],images:out[key].images.map(i=>({...i,src:i.src.startsWith('data:')?'embedded image':i.src})),backgrounds:[]}).slice(0,1500));
  }catch(e){out[key]={error:e.name};console.log(key,e.name);}finally{await page.close();}
 }
 fs.writeFileSync(path.join(root,'.local/airline-browser-research.json'),JSON.stringify(out,null,2));
}finally{await browser.close();}})();
