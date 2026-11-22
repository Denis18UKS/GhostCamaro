const fs=require('node:fs'),path=require('node:path'),{chromium}=require('playwright');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox','--enable-unsafe-swiftshader']});
 try{
 const files=fs.readdirSync('build/next/Blockbench',{recursive:true}).filter(n=>n.endsWith('.bbmodel')).sort(),results=[];
 for(const name of files){
  const page=await browser.newPage();try {
   await page.goto('https://web.blockbench.net',{waitUntil:'domcontentloaded',timeout:90000});
   await page.waitForFunction(()=>window.Codecs?.project&&window.Formats?.free,{},{timeout:90000});
   const raw=fs.readFileSync(path.join('build/next/Blockbench',name),'utf8');
   const result=await page.evaluate(raw=>{const model=JSON.parse(raw);
    Codecs.project.load(model,{name:model.name+'.bbmodel',path:model.name+'.bbmodel'});
    return {format:Project.format.id,cubes:Cube.all.length,images:Texture.all.length};},raw);
   if(result.format!=='free'||result.cubes!==JSON.parse(raw).elements.length||result.images!==1)throw Error(name+JSON.stringify(result));
   results.push({name,...result});
  }finally{await page.close();}
 }
 fs.mkdirSync('build/next/Checks',{recursive:true});fs.writeFileSync('build/next/Checks/Blockbench.json',JSON.stringify(results,null,2));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});