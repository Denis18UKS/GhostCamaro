"""Run Blender in background after model.py. FBX uses one armature Action per named glTF clip."""
import bpy,struct,json,math
from pathlib import Path
from mathutils import Vector,Matrix,Quaternion
bpy.context.preferences.filepaths.save_version=0
d=Path(__file__).resolve().parents[2]/'build/v7';source=d/'GhostCamaro-V7.glb'
raw=source.read_bytes();n=struct.unpack_from('<I',raw,12)[0];g=json.loads(raw[20:20+n]);buf=raw[28+n:]
def read(i):
 a=g['accessors'][i];v=g['bufferViews'][a['bufferView']];w={'VEC3':3,'VEC4':4,'SCALAR':1}[a['type']]
 return [struct.unpack_from('<'+'f'*w,buf,v.get('byteOffset',0)+a.get('byteOffset',0)+k*w*4) for k in range(a['count'])]
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.gltf(filepath=str(source))
nodes=g['nodes'];scene=bpy.context.scene;scene.frame_set(0);scene.render.fps=24
for ob in list(scene.objects):
 if ob.animation_data:ob.animation_data_clear()
for a in list(bpy.data.actions):bpy.data.actions.remove(a)
def axis(v):return Vector((v[0],-v[2],v[1]))
parents={c:i for i,n in enumerate(nodes) for c in n.get('children',[])}
def rest(i):
 pos=axis(nodes[i].get('translation',[0]*3))
 return pos+rest(parents[i]) if i in parents else pos
rig=bpy.data.armatures.new('Camaro joints');arm=bpy.data.objects.new('GhostCamaro_Rig',rig);scene.collection.objects.link(arm);bpy.context.view_layer.objects.active=arm
arm.show_in_front=False;rig.display_type='STICK';bpy.ops.object.mode_set(mode='EDIT')
joints={i for i,n in enumerate(nodes) if 'children' in n}|{c['target']['node'] for a in g['animations'] for c in a['channels']}
for i in sorted(joints):
 bone=rig.edit_bones.new(nodes[i]['name']);bone.head=rest(i);bone.tail=bone.head+Vector((0,.08,0))
for i in joints:
 if parents.get(i) in joints:rig.edit_bones[nodes[i]['name']].parent=rig.edit_bones[nodes[parents[i]]['name']]
bpy.ops.object.mode_set(mode='OBJECT')
for i,n in enumerate(nodes):
 obj=bpy.data.objects.get(n['name'])
 if not obj or obj.type not in {'MESH','LIGHT'}:continue
 world=obj.matrix_world.copy();p=i if i in joints else parents.get(i)
 if p not in joints:continue
 obj.parent=None;obj.matrix_world=world;obj.parent=arm;obj.parent_type='BONE';obj.parent_bone=nodes[p]['name']
 bpy.context.view_layer.update();obj.matrix_world=world
for o in list(scene.objects):
 if o.type=='EMPTY':bpy.data.objects.remove(o,do_unlink=True)
for clip in g['animations']:
 act=bpy.data.actions.new(clip['name']);act.use_fake_user=True
 for ch in clip['channels']:
  i=ch['target']['node'];kind=ch['target']['path'];p=arm.pose.bones[nodes[i]['name']];sm=clip['samplers'][ch['sampler']];times=read(sm['input']);values=read(sm['output'])
  prop={'rotation':'rotation_quaternion','translation':'location','scale':'scale'}[kind];rows=[]
  for v in values:
   if kind=='rotation':v=(v[3],v[0],-v[2],v[1])
   elif kind=='translation':v=axis(v)-axis(nodes[i].get('translation',[0]*3))
   else:v=(v[0],v[2],v[1])
   rows.append(v)
  for col in range(len(rows[0])):
   f=act.fcurves.new(p.path_from_id(prop),index=col,action_group=p.name)
   f.keyframe_points.add(len(times))
   for k,((t,),v) in enumerate(zip(times,rows)):
    f.keyframe_points[k].co=(t*24,v[col]);f.keyframe_points[k].interpolation='CONSTANT' if sm.get('interpolation')=='STEP' else 'LINEAR'
   f.update()
arm.animation_data_create()
for action in list(bpy.data.actions):
 tr=arm.animation_data.nla_tracks.new();tr.name=action.name;tr.strips.new(action.name,0,action);tr.mute=True
for im in bpy.data.images:
 if im.type=='IMAGE' and im.has_data and not im.packed_file:im.pack()
bpy.context.view_layer.update();scene.frame_set(0);scene.frame_end=144;scene.unit_settings.system='METRIC'
bpy.ops.wm.save_as_mainfile(filepath=str(d/'GhostCamaro-V7.blend'))
# Rigidly attached meshes export once. All animation curves are on the single rig.
bpy.ops.export_scene.fbx(filepath=str(d/'GhostCamaro-V7.fbx'),object_types={'ARMATURE','MESH','LIGHT'},add_leaf_bones=False,bake_anim_use_nla_strips=False,bake_anim_use_all_actions=True,bake_anim_simplify_factor=.5,path_mode='COPY',embed_textures=True)
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.ops.import_scene.fbx(filepath=str(d/'GhostCamaro-V7.fbx'))
names={a.name.split('|')[-1] for a in bpy.data.actions};expected={a['name'] for a in g['animations']};missing=expected-names
print('FBX ROUNDTRIP:',len(names),len([o for o in bpy.context.scene.objects if o.type=='MESH']),sorted(missing),flush=True)
(d/'FBX_CHECK.json').write_text(json.dumps(dict(actions=sorted(names),missing=sorted(missing),meshes=sum(o.type=='MESH' for o in bpy.context.scene.objects)),indent=2))
assert not missing
