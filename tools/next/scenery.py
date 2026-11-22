"""Build editable versions of the registered environment assets in Blender 4.3."""
import bpy, json, base64, uuid
from pathlib import Path
root=Path(__file__).resolve().parents[2]
assets=root/'build/generated/ghost/assets/ghostcamaro'
out=root/'build/next'
def uid(name):return str(uuid.uuid5(uuid.NAMESPACE_URL,'ghostcamaro/environment/'+name))
def coords(x,y,z):return ((x-8)/16,(8-z)/16,y/16)
faces={'north':(1,0,4,5),'south':(3,2,6,7),'east':(2,1,5,6),'west':(0,3,7,4),'up':(4,7,6,5),'down':(1,2,3,0)}
for model in sorted((assets/'models/block').glob('*.json'))+sorted((assets/'models/item').glob('*.json'))+sorted((assets/'models/cargo').glob('*.json')):
 n=model.stem
 bpy.ops.wm.read_factory_settings(use_empty=True)
 src=json.loads(model.read_text())
 if not src.get('elements'):continue
 cat='Cargo' if model.parent.name=='cargo' else 'Building' if model.parent.name=='block' else ('Weapons' if n in ('range_pistol','range_rifle') else 'Ammunition' if (n.endswith('_rounds') or n=='range_round') else 'Equipment')
 imgpath=assets/'textures'/(src['textures']['skin'].split(':')[1]+'.png')
 if not imgpath.exists():imgpath=root/'src/main/resources/assets/ghostcamaro/textures/entity'/imgpath.name
 im=bpy.data.images.load(str(imgpath));im.pack()
 mat=bpy.data.materials.new(n);mat.use_nodes=True;node=mat.node_tree.nodes.new('ShaderNodeTexImage');node.image=im;node.interpolation='Closest'
 mat.node_tree.links.new(node.outputs['Color'],mat.node_tree.nodes['Principled BSDF'].inputs['Base Color'])
 bb={'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':n,'resolution':{'width':16,'height':16},'elements':[],'outliner':[],'textures':[{'uuid':uid(n+'/texture'),'id':'0','name':n+'.png','source':'data:image/png;base64,'+base64.b64encode(imgpath.read_bytes()).decode()}]}
 for k,e in enumerate(src['elements']):
  a,b,c=e['from'];A,B,C=e['to'];v=[(a,b,c),(A,b,c),(A,b,C),(a,b,C),(a,B,c),(A,B,c),(A,B,C),(a,B,C)]
  rect={'north':(16-A,16-B,16-a,16-b),'south':(a,16-B,A,16-b),'east':(16-C,16-B,16-c,16-b),'west':(c,16-B,C,16-b),'up':(a,c,A,C),'down':(a,16-C,A,16-c)}
  m=bpy.data.meshes.new(n+str(k));m.from_pydata([coords(*p) for p in v],[],list(faces.values()));m.materials.append(mat);m.update()
  ob=bpy.data.objects.new(e.get('name',f'{n}_{k+1:02}'),m);bpy.context.collection.objects.link(ob);uv=m.uv_layers.new()
  efaces={}
  for poly,(direction,inds) in zip(m.polygons,faces.items()):
   u,t,U,T=e['faces'][direction].get('uv',rect[direction]);uvals= [(u,t),(u,T),(U,T),(U,t)] if direction=='up' else [(U,T),(U,t),(u,t),(u,T)] if direction=='down' else [(u,T),(U,T),(U,t),(u,t)]
   for index,(x,y) in zip(poly.loop_indices,uvals):uv.data[index].uv=(x/16,1-y/16)
   efaces[direction]={'uv':[u,t,U,T],'texture':0}
  id=uid(n+str(k));bb['outliner'].append(id);bb['elements'].append({'type':'cube','uuid':id,'name':ob.name,'from':e['from'],'to':e['to'],'origin':[8,0,8],'faces':efaces,'box_uv':False})
 for folder in [f'Blockbench/{cat}',f'Blender/{cat}',f'Exports/FBX/{cat}',f'Exports/GLB/{cat}',f'Textures/{cat}']:(out/folder).mkdir(parents=True,exist_ok=True)
 (out/f'Textures/{cat}'/(n+'.png')).write_bytes(imgpath.read_bytes())
 (out/f'Blockbench/{cat}'/(n+'.bbmodel')).write_text(json.dumps(bb))
 bpy.context.scene.unit_settings.system='METRIC';bpy.context.preferences.filepaths.save_version=0
 bpy.ops.wm.save_as_mainfile(filepath=str(out/f'Blender/{cat}'/(n+'.blend')))
 bpy.ops.export_scene.fbx(filepath=str(out/f'Exports/FBX/{cat}'/(n+'.fbx')),path_mode='COPY',embed_textures=True,bake_anim=False)
 bpy.ops.export_scene.gltf(filepath=str(out/f'Exports/GLB/{cat}'/(n+'.glb')),export_animations=False)
 for ext in ['fbx','glb']:
  bpy.ops.wm.read_factory_settings(use_empty=True)
  file=out/'Exports'/ext.upper()/cat/(n+'.'+ext)
  if ext=='fbx':bpy.ops.import_scene.fbx(filepath=str(file))
  else:bpy.ops.import_scene.gltf(filepath=str(file))
  assert len([o for o in bpy.context.scene.objects if o.type=='MESH'])==len(src['elements'])
 print('ROUNDTRIP',n,len(src['elements']),flush=True)
