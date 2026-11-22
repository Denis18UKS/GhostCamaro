const fs=require('node:fs'),path=require('node:path'),{chromium}=require('playwright');
(async()=>{
 const browser=await chromium.launch({headless:true,executablePath:process.env.CHROME_BIN||undefined,args:['--no-sandbox','--enable-unsafe-swiftshader']});
 try{
 const files=fs.readdirSync('build/next/Blockbench',{recursive:true}).filter(n=>n.endsWith('.bbmodel')&&!n.startsWith('Vehicles/')).sort(),results=[];
 const page=await browser.newPage({ignoreHTTPSErrors:true});
   await page.goto('https://web.blockbench.net',{waitUntil:'domcontentloaded',timeout:90000});
   await page.waitForFunction(()=>window.Codecs?.project&&window.Formats?.free,{},{timeout:90000});
 for(const name of files){
   const raw=fs.readFileSync(path.join('build/next/Blockbench',name),'utf8');
   const result=await page.evaluate(raw=>{const model=JSON.parse(raw);
    Codecs.project.load(model,{name:model.name+'.bbmodel',path:model.name+'.bbmodel'});
    const snapshot={format:Project.format.id,parts:Cube.all.length+Mesh.all.length,images:Texture.all.length};Project.close(true);return snapshot;},raw);
   if(result.format!=='free'||result.parts!==JSON.parse(raw).elements.length||result.images!==JSON.parse(raw).textures.length)throw Error(name+JSON.stringify(result));
   results.push({name,...result});

 }
 fs.mkdirSync('build/next/Checks',{recursive:true});fs.writeFileSync('build/next/Checks/Blockbench.json',JSON.stringify(results,null,2));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});