"""Lossless glTF triangle/index expansion for Minecraft's entity vertex format."""
import json, struct, pathlib, base64, zlib
root=pathlib.Path(__file__).resolve().parents[1]
out=root/'build/generated/ghost/assets/ghostcamaro'
(out/'vehicle').mkdir(parents=True,exist_ok=True);(out/'textures/entity').mkdir(parents=True,exist_ok=True)
import subprocess, sys
subprocess.run([sys.executable,str(root/'tools/v7/model.py')],check=True)
raw=(root/'build/v7/GhostCamaro-V7.glb').read_bytes()
n=struct.unpack_from('<I',raw,12)[0];g=json.loads(raw[20:20+n]);buf=raw[28+n:]
def view(i):
 v=g['bufferViews'][i];p=v.get('byteOffset',0);return buf[p:p+v['byteLength']]
def acc(i):
 a=g['accessors'][i];fmt='<'+{'SCALAR':'','VEC2':'2','VEC3':'3','VEC4':'4'}[a['type']]+{5126:'f',5121:'B',5123:'H',5125:'I'}[a['componentType']]
 v=g['bufferViews'][a['bufferView']];offset=v.get('byteOffset',0)+a.get('byteOffset',0);stride=v.get('byteStride',struct.calcsize(fmt))
 return [struct.unpack_from(fmt,buf,offset+j*stride) for j in range(a['count'])]
for i,img in enumerate(g['images']):(out/'textures/entity'/('atlas%d.png'%i)).write_bytes(view(img['bufferView']))
# Vanilla entity_translucent discards very low alpha. Keep a cool tint above that cutoff.
def glass_png(rgb, alpha):
 def chunk(tag, data):
  return struct.pack('>I',len(data))+tag+data+struct.pack('>I',zlib.crc32(tag+data))
 rows=b''.join(b'\0'+bytes((*rgb,alpha+y//3))*16 for y in range(16))
 return b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(rows))+chunk(b'IEND',b'')
for index,color,opacity in [(1,(168,198,206),49),(2,(92,125,136),88)]:
 (out/'textures/entity'/('atlas%d.png'%index)).write_bytes(glass_png(color,opacity))
model={'roots':g['scenes'][0]['nodes'],'nodes':g['nodes'],'clips':{},'meshes':[]}
with (out/'vehicle/geometry.bin').open('wb') as f:
 for m in g['meshes']:
  primitives=[]
  for p in m['primitives']:
   pos,uv,norm=(acc(p['attributes'][k]) for k in ['POSITION','TEXCOORD_0','NORMAL'])
   idx=acc(p['indices']);mat=g['materials'][p['material']];tex=g['textures'][mat['pbrMetallicRoughness']['baseColorTexture']['index']]['source']
   primitives.append(dict(texture=tex,alpha=mat.get('alphaMode')=='BLEND',glow='emissiveFactor' in mat,count=len(idx)))
   for (j,) in idx:f.write(struct.pack('>8f',*pos[j],*uv[j],*norm[j]))
  model['meshes'].append(primitives)
for a in g['animations']:
 tracks=[]
 for ch in a['channels']:
  sm=a['samplers'][ch['sampler']];tracks.append({'node':ch['target']['node'],'kind':ch['target']['path'],'times':[v[0] for v in acc(sm['input'])],'values':acc(sm['output']),'step':sm.get('interpolation')=='STEP'})
 model['clips'][a['name']]=tracks
(out/'vehicle/scene.json').write_text(json.dumps(model,separators=(',',':')))
bb=json.loads((root/'build/v7/GhostCamaro-V7.bbmodel').read_bytes())
assert len(model['clips'])>=61 and set(model['clips'])=={a['name'] for a in bb['animations']}
assert sum(p['count']//3 for m in model['meshes'] for p in m)>=37800
audit={'nodes':g['nodes'],'materials':g['materials'],'textures':g['images'],'animation_files':['Ghost_Camaro_V5.bbmodel','Ghost_Camaro_V5.glb']}
(root/'V5_AUDIT.json').write_text(json.dumps(audit,ensure_ascii=False,indent=1))
print('Animation channels checked:',len(model['clips']))
subprocess.run([sys.executable,str(root/'tools/v7/streets.py')],check=True)
