"""Editor actions for fictional range props; transforms are in Minecraft model units."""
import bpy,math,uuid
from mathutils import Vector
def animate(name,bb):
    uid=lambda s:str(uuid.uuid5(uuid.NAMESPACE_URL,'ghostcamaro/props/'+name+'/'+s))
    children=bb['outliner'];groupid=uid('GripRoot');partid=uid('Removable')
    partName='Magazine' if name=='range_rifle' else 'Base'
    partel=next(e for e in bb['elements'] if e['name']==partName)
    children.remove(partel['uuid'])
    def group(n,ident,sub):
        return dict(name=n,uuid=ident,origin=[8,0,8],rotation=[0,0,0],children=sub,export=True,visibility=True,isOpen=True)
    children.append(group('Removable',partid,[partel['uuid']]))
    bb['outliner']=[group('GripRoot',groupid,children)]
    armData=bpy.data.armatures.new('Prop joints');rig=bpy.data.objects.new('Prop rig',armData);bpy.context.collection.objects.link(rig)
    bpy.context.view_layer.objects.active=rig;bpy.ops.object.mode_set(mode='EDIT')
    for n in ['GripRoot','Removable']:
        bone=armData.edit_bones.new(n);bone.head=(0,0,0);bone.tail=(0,.1,0)
        if n=='Removable':bone.parent=armData.edit_bones['GripRoot']
    bpy.ops.object.mode_set(mode='OBJECT')
    for obj in list(bpy.context.scene.objects):
        if obj.type!='MESH':continue
        mat=obj.matrix_world.copy();obj.parent=rig;obj.parent_type='BONE';obj.parent_bone='Removable' if obj.name==partName else 'GripRoot'
        bpy.context.view_layer.update();obj.matrix_world=mat
    # frame, root pitch/roll (degrees), root vertical offset, removable-piece vertical offset.
    zero=(0,0,0,0,0)
    clips={'Idle':[zero,(15,0,.6,.08,0),(30,0,0,0,0)],
      'Draw':[(0,-22,16,-4,0),(10,0,0,0,0)],'Holster':[zero,(10,-22,16,-4,0)],
      'Aim':[zero,(7,0,0,1.2,0)],'Fire':[zero,(1,-4,0,.15,0),(4,0,0,0,0)],
      'Recoil':[zero,(2,-8,1,.3,0),(7,0,0,0,0)],
      'Reload':[zero,(6,10,30,-.4,0),(12,14,40,-.6,-4),(21,14,40,-.6,-4),(27,8,25,-.4,0),(36,0,0,0,0)],
      'Empty':[zero,(3,-1,1,-.12,0),(6,0,0,0,0)],'Inspect':[zero,(14,10,60,.3,0),(32,-5,-34,.2,0),(48,0,0,0,0)]}
    bb['animations']=[];rig.animation_data_create()
    for title,keys in clips.items():
        action=bpy.data.actions.new(title);action.use_fake_user=True
        rig.animation_data.action=action
        aa={gid:{'name':nm,'type':'bone','keyframes':[]} for gid,nm in [(groupid,'GripRoot'),(partid,'Removable')]}
        for frame,pitch,roll,z,mag in keys:
            for boneName,gid,vertical,angles in [('GripRoot',groupid,z,(pitch,0,roll)),('Removable',partid,mag,(0,0,0))]:
                bone=rig.pose.bones[boneName];bone.rotation_mode='XYZ';bone.location=(0,0,vertical/16)
                bone.rotation_euler=(math.radians(pitch if gid==groupid else 0),-math.radians(roll if gid==groupid else 0),0)
                for prop in ['location','rotation_euler']:bone.keyframe_insert(data_path=prop,frame=frame,group=boneName)
                for channel,values in [('position',(0,vertical,0)),('rotation',angles)]:
                    aa[gid]['keyframes'].append(dict(uuid=uid(title+gid+channel+str(frame)),channel=channel,time=frame/24,interpolation='linear',data_points=[dict(zip('xyz',map(str,values)))]))
        for curve in action.fcurves:
            for k in curve.keyframe_points:k.interpolation='LINEAR'
        rig.animation_data.action=None
        tr=rig.animation_data.nla_tracks.new();tr.name=title;tr.strips.new(title,0,action);tr.mute=True
        bb['animations'].append(dict(uuid=uid(title),name=title,length=keys[-1][0]/24,loop='loop' if title=='Idle' else 'once',animators=aa))
    for bone in rig.pose.bones:bone.matrix_basis.identity()
    bpy.context.scene.frame_set(0)
