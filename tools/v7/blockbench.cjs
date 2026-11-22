const {chromium}=require('playwright');
const fs=require('node:fs');
(async()=>{
 const browser=await chromium.launch({headless:true,args:['--no-sandbox','--enable-unsafe-swiftshader']});
 try {
  const page=await browser.newPage();await page.goto('https://web.blockbench.net',{waitUntil:'domcontentloaded',timeout:90000});
  await page.waitForFunction(()=>window.Codecs?.project&&window.Formats?.free,{},{timeout:90000});
  let actual=await page.evaluate(raw=>{
   const model=JSON.parse(raw);
   Codecs.project.load(model,{name:'GhostCamaro-V7.bbmodel',path:'GhostCamaro-V7.bbmodel'});
   return {format:Project.format.id,meshes:Mesh.all.length,groups:Group.all.length,textures:Texture.all.length,clips:Animation.all.map(a=>a.name)};
  },fs.readFileSync('build/v7/GhostCamaro-V7.bbmodel','utf8'));
  console.log(JSON.stringify(actual));
  if(actual.format!=='free'||actual.meshes!==604||actual.textures!==4||actual.clips.length!==62)throw Error('Unexpected Blockbench import');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
