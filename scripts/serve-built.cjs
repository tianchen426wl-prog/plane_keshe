// Serve both built frontends with one backend proxy. No credentials are exposed.
const http=require('node:http'),fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'..'),backend=new URL(process.env.BACKEND_URL||'http://127.0.0.1:18080');
const pageUrls={};
const types={'.html':'text/html; charset=utf-8','.js':'application/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.json':'application/json; charset=utf-8','.png':'image/png','.jpg':'image/jpeg','.jpeg':'image/jpeg','.webp':'image/webp','.svg':'image/svg+xml','.woff2':'font/woff2','.woff':'font/woff','.ttf':'font/ttf','.ico':'image/x-icon'};
for(const [app,port] of [['portal',Number(process.env.PORTAL_PORT||18081)],['admin',Number(process.env.ADMIN_PORT||18082)]]){
 const base=path.resolve(root,'air-ticket-web',app,'dist');
 if(!fs.existsSync(path.join(base,'index.html')))throw Error('Build '+app+' first');
 const server=http.createServer((req,res)=>{
  if(req.url.startsWith('/api/')){
   if(req.headers.origin){try{if(new URL(req.headers.origin).host!==req.headers.host){res.writeHead(403);res.end();return;}}catch(e){res.writeHead(400);res.end();return;}}
   const headers={...req.headers,host:backend.host};if(headers.origin)headers.origin=backend.origin;
   const proxy=http.request({hostname:backend.hostname,port:backend.port,path:req.url,method:req.method,headers},r=>{res.writeHead(r.statusCode,r.headers);r.pipe(res)});proxy.on('error',()=>{res.writeHead(502,{'Content-Type':'application/json'});res.end(JSON.stringify({code:502,message:'后端服务尚未就绪'}))});req.pipe(proxy);return;
  }
  let relative;try{relative=decodeURIComponent(new URL(req.url,'http://localhost').pathname)}catch(e){res.writeHead(400);res.end();return;}
  const file=path.resolve(base,'.'+(relative==='/'?'/index.html':relative));
  if(!file.startsWith(base+path.sep)){res.writeHead(403);res.end();return;}
  if(!fs.existsSync(file)||!fs.statSync(file).isFile()){res.writeHead(404);res.end();return;}
  res.setHeader('Content-Type',types[path.extname(file).toLowerCase()]||'application/octet-stream');fs.createReadStream(file).pipe(res);
 });
 server.listen(port,'127.0.0.1',()=>{
  const origin='http://127.0.0.1:'+server.address().port;
  pageUrls[app]=origin+(app==='admin'?'/#/admin/login':'/#/');
  if(pageUrls.portal&&pageUrls.admin){
   console.log('\n航空售票管理系统 · 前端已启动\n');
   console.log('用户门户（浏览器打开）：'+pageUrls.portal);
   console.log('管理后台（登录页面）：  '+pageUrls.admin);
   console.log('\n后端 API：'+backend.origin+'（接口服务，无网页首页）');
   console.log('请打开上方用户门户或管理后台地址。保持此终端运行，按 Ctrl+C 停止前端。\n');
  }
 });
}
