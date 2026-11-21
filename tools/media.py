"""Original CC0 synthesized game audio and small inventory pictograms. Optional regeneration."""
from pathlib import Path
import math, wave, struct, random, subprocess, tempfile,json
from PIL import Image,ImageDraw
root=Path(__file__).resolve().parents[1];p=root/'src/main/resources/assets/ghostcamaro'
sr=22050;rng=random.Random(1979)
for name,sec in [('engine',.62),('start',.53),('horn',.52),('click',.065),('burst',.72)]:
 data=[];phase=0
 for j in range(int(sr*sec)):
  t=j/sr;fade=min(1,t/.012,(sec-t)/.035);noise=rng.uniform(-1,1)
  if name in ('engine','start'):
   hz=47 if name=='engine' else 12+80*t;phase+=math.tau*hz/sr
   val=.50*math.sin(phase)+.24*math.sin(2*phase+.12*math.sin(phase/4))+.10*noise
  elif name=='horn':val=.35*math.sin(math.tau*370*t)+.27*math.sin(math.tau*470*t)
  elif name=='burst':val=(.64*noise+.30*math.sin(math.tau*(80-65*t)*t))*math.exp(-6*t)
  else:val=.8*noise*math.exp(-100*t)
  data.append(struct.pack('<h',int(25000*fade*val)))
 with tempfile.NamedTemporaryFile(suffix='.wav') as f:
  with wave.open(f.name,'wb') as w:w.setparams((1,2,sr,0,'NONE','not compressed'));w.writeframes(b''.join(data))
  subprocess.run(['ffmpeg','-loglevel','error','-y','-i',f.name,'-c:a','libvorbis',str(p/'sounds'/(name+'.ogg'))],check=True)
(p/'sounds.json').write_text(json.dumps({s.stem:{'sounds':['ghostcamaro:'+s.stem]} for s in (p/'sounds').glob('*.ogg')}))
for name in ['ghost_camaro','fuel_can','repair_kit']:
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 if name=='ghost_camaro':
  d.polygon([(2,19),(5,16),(12,15),(15,10),(22,10),(26,15),(30,17),(30,22),(2,22)],fill='#4e5251',outline='#242222');d.polygon([(14,15),(16,11),(21,11),(24,15)],fill='#88a4ab')
  for x in [7,25]:d.ellipse((x-3,19,x+3,25),fill='#212121',outline='#869093')
 elif name=='fuel_can':
  d.rounded_rectangle((6,8,25,29),3,fill='#726d48',outline='#b0a574');d.rectangle((10,4,22,10),outline='#817958',width=3);d.line((9,12,21,25),fill='#aba079',width=2);d.line((21,12,9,25),fill='#aba079',width=2)
 else:
  d.rectangle((3,11,29,26),fill='#9d4c3f',outline='#442c2a',width=2);d.rectangle((10,6,22,12),outline='#655955',width=3);d.line((4,17,28,17),fill='#c88368',width=2)
 im.save(p/'textures/item'/(name+'.png'));(p/'models/item'/(name+'.json')).write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'ghostcamaro:item/'+name}}))
