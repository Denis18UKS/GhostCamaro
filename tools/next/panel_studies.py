"""Portable preset dents derived from V7's actual outer panels, in master orientation."""
import base64,copy,json,math,uuid
from pathlib import Path
import bpy
from mathutils import Vector
root=Path(__file__).resolve().parents[2];base=root/'build/v7';out=root/'build/next'
source=json.loads((base/'GhostCamaro-V7.bbmodel').read_bytes())
names={'Door_L','Door_R','Front_fender_L','Front_fender_R','Quarter_L','Quarter_R','Power_hood','Decklid','Roof_pressing','Rear_bumper','Urethane_front_bumper','Blade_wing','Blade_wing.001','Side_Plate_L_mesh','Side_Plate_R_mesh'}
parts=[e for e in source['elements'] if e['name'] in names]
bb=copy.deepcopy(source);bb.update(name='Camaro Panel Damage Library',elements=[],outliner=[],animations=[])
def identifier(s):return str(uuid.uuid5(uuid.NAMESPACE_URL,'ghostcamaro/shape/'+s))
def group(n,l):return dict(name=n,uuid=identifier(n),origin=[0,0,0],children=l,visibility=True,isOpen=False)
def convert(v):return Vector((v[2]/16,v[0]/16,v[1]/16))
def bounds(v):return [(min(p[i] for p in v),max(p[i] for p in v)) for i in range(3)]
bpy.ops.wm.read_factory_settings(use_empty=True)
tex=bpy.data.images.load(str(base/'Textures'/'Ghost_Camaro_Textures.png'));tex.pack()
mat=bpy.data.materials.new('V7 body atlas');mat.use_nodes=True
node=mat.node_tree.nodes.new('ShaderNodeTexImage');node.image=tex
mat.node_tree.links.new(node.outputs['Color'],mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'])
for e in parts:
 n=e['name'];keys=list(e['vertices']);verts=[Vector(e['vertices'][k])+Vector(e['origin']) for k in keys];box=bounds(verts)
 flat=min(range(3),key=lambda i:box[i][1]-box[i][0]);plane=[i for i in range(3) if i!=flat]
 center=[(a+b)*.5 for a,b in box];inward=-1 if center[flat]>0 else 1
 if flat==1:inward=-1
 widths=[max(.02,(b-a)*.53) for a,b in box]
 weights=[max(0,1-sum(((v[i]-center[i])/widths[i])**2 for i in plane))**1.6 for v in verts]
 if max(weights)<.02:
  nearest=min(range(len(verts)),key=lambda k:sum((verts[k][i]-center[i])**2 for i in plane));center=list(verts[nearest]);weights=[math.exp(-2*sum(((v[i]-center[i])/widths[i])**2 for i in plane)) for v in verts]
 def displace(k,stage):
  v=verts[k].copy();v[flat]+=inward*(stage/5)**1.35*1.8*weights[k];return v
 faces=list(e['faces'].values());mesh=bpy.data.meshes.new(n)
 mesh.from_pydata([convert(v) for v in verts],[],[[keys.index(j) for j in f['vertices']] for f in faces]);mesh.update();mesh.materials.append(mat)
 ob=bpy.data.objects.new(n,mesh);bpy.context.collection.objects.link(ob);uv=mesh.uv_layers.new()
 for poly,face in zip(mesh.polygons,faces):
  for ix,k in zip(poly.loop_indices,face['vertices']):
   u,v=face['uv'][k];uv.data[ix].uv=(u/source['resolution']['width'],1-v/source['resolution']['height'])
 ob.shape_key_add(name='Basis');ids=[]
 for stage in range(6):
  title=n+'_Stage_'+str(stage);item=copy.deepcopy(e);item.update(name=title,uuid=identifier(title),visibility=(stage==0),origin=[0,0,0],vertices={k:list(displace(i,stage)) for i,k in enumerate(keys)})
  ids.append(item['uuid']);bb['elements'].append(item)
  if stage:
   morph=ob.shape_key_add(name='Dent_'+str(stage))
   for i,p in enumerate(morph.data):p.co=convert(displace(i,stage))
 bb['outliner'].append(group(n,ids))
for folder,ext in [('Blockbench','bbmodel'),('Blender','blend'),('Exports/FBX','fbx'),('Exports/GLB','glb')]:
 d=out/folder/'Damage';d.mkdir(parents=True,exist_ok=True);file=d/('CamaroPanels.'+ext)
 if ext=='bbmodel':file.write_text(json.dumps(bb,separators=(',',':')))
 if ext=='blend':bpy.context.preferences.filepaths.save_version=0;bpy.ops.wm.save_as_mainfile(filepath=str(file))
 if ext=='fbx':bpy.ops.export_scene.fbx(filepath=str(file),path_mode='COPY',embed_textures=True,bake_anim=False)
 if ext=='glb':bpy.ops.export_scene.gltf(filepath=str(file),export_morph=True)
for ext,folder in [('fbx','FBX'),('glb','GLB')]:
 bpy.ops.wm.read_factory_settings(use_empty=True);file=out/'Exports'/folder/'Damage'/('CamaroPanels.'+ext)
 if ext=='fbx':bpy.ops.import_scene.fbx(filepath=str(file))
 else:bpy.ops.import_scene.gltf(filepath=str(file))
 keyed=[o for o in bpy.context.scene.objects if o.type=='MESH' and o.data.shape_keys and len(o.data.shape_keys.key_blocks)==6]
 assert len(keyed)==len(parts),(ext,len(keyed),len(parts))
print('Panel roundtrip',len(parts),'panels, five progressive shape keys, FBX and GLB',flush=True)
