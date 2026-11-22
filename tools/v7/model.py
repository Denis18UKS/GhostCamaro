"""Produce editable BB and GLB masters by extending, never rebuilding, the V5 hierarchy."""
from pathlib import Path
import struct,json,uuid,math,base64
root=Path(__file__).resolve().parents[2];dest=root/'build/v7';dest.mkdir(parents=True,exist_ok=True)
b=json.loads((root/'masters/Ghost_Camaro_V5.bbmodel').read_bytes())
raw=(root/'masters/Ghost_Camaro_V5.glb').read_bytes();n=struct.unpack_from('<I',raw,12)[0];g=json.loads(raw[20:20+n]);buf=bytearray(raw[28+n:])
nodes=g['nodes'];ids={x['name']:i for i,x in enumerate(nodes)}; bg={}
def walk(x):
 if isinstance(x,dict):
  bg[x['name']]=x
  for c in x['children']:walk(c)
for x in b['outliner']:walk(x)
def uid(n):return str(uuid.uuid5(uuid.NAMESPACE_URL,'ghostcamaro/v7/'+n))
def bb(v):return [-v[2]*16,v[1]*16,v[0]*16]
def acc(rows,typ='VEC3',ct=5126):
 while len(buf)%4:buf.append(0)
 start=len(buf);fmt='f' if ct==5126 else 'H'
 for row in rows:buf.extend(struct.pack('<'+fmt*len(row),*row))
 vi=len(g['bufferViews']);g['bufferViews'].append(dict(buffer=0,byteOffset=start,byteLength=len(buf)-start))
 a=dict(bufferView=vi,componentType=ct,count=len(rows),type=typ,min=list(map(min,zip(*rows))),max=list(map(max,zip(*rows))))
 g['accessors'].append(a);return len(g['accessors'])-1
def world(i):
 t=nodes[i].get('translation',[0]*3);p=next((j for j,x in enumerate(nodes) if i in x.get('children',[])),None)
 return [a+c for a,c in zip(t,world(p))] if p!=None else t
def joint(name,pivot,parent):
 p=ids[parent];ids[name]=len(nodes);nodes[p].setdefault('children',[]).append(len(nodes))
 nodes.append(dict(name=name,translation=[a-c for a,c in zip(pivot,world(p))],children=[]))
 bg[name]=dict(name=name,uuid=uid(name),origin=bb(pivot),rotation=[0,0,0],children=[],export=True,visibility=True)
 bg[parent]['children'].append(bg[name])
def panel(name,pivot,size,parent='Body',mat=2,tilt=0):
 joint(name,pivot,parent);verts=[];norm=[];uv=[];indices=[];faces={};bv={}
 for axis in range(3):
  a=(axis+1)%3;c=(axis+2)%3
  for side in (-1,1):
   keys=[];faceuv={}
   for u,v in [(-1,-1),(1,-1),(1,1),(-1,1)]:
    xyz=[0]*3;xyz[axis]=side*size[axis]/2;xyz[a]=u*size[a]/2;xyz[c]=v*side*size[c]/2
    co,si=math.cos(tilt),math.sin(tilt);x,y,z=xyz;xyz=[co*x-si*y,si*x+co*y,z]
    k=len(verts);verts.append(xyz);nn=[0]*3;nn[axis]=side;x,y,z=nn;norm.append([co*x-si*y,si*x+co*y,z]);tc=[(mat%4+.5+.35*u)/4,(mat//4+.5+.35*v)/4];uv.append(tc)
    key=str(k);keys.append(key);bv[key]=bb([p+q for p,q in zip(pivot,xyz)]);faceuv[key]=[j*2048 for j in tc]
   start=len(verts)-4;indices.extend((start+i,) for i in [0,1,2,0,2,3]);faces[str(len(faces))]=dict(vertices=keys,texture=0,uv=faceuv)
 mi=len(g['meshes']);g['meshes'].append(dict(name=name+'_mesh',primitives=[dict(attributes=dict(POSITION=acc(verts),NORMAL=acc(norm),TEXCOORD_0=acc(uv,'VEC2')),indices=acc(indices,'SCALAR',5123),material=0)]))
 nodes[ids[name]]['children'].append(len(nodes));nodes.append(dict(name=name+'_mesh',mesh=mi))
 eid=uid(name+'_mesh');b['elements'].append(dict(type='mesh',name=name+'_mesh',uuid=eid,origin=[0]*3,rotation=[0]*3,vertices=bv,faces=faces,visibility=True,export=True));bg[name]['children'].append(eid)
panel('Service_Cap',(2.436,.49,.015),(.014,.105,.185),'Rear',2)
for side,sgn in [('L',1),('R',-1)]:panel('Side_Plate_'+side,(.10,.575,.922*sgn),(.99,.24,.018),'Door_'+side+'_hinge',2)
panel('Engine_Guard',(-1.56,.49,0),(.90,.035,.58),'Front',2)
# Thin graphite braces under the bumpers, independently removable.
panel('Rear_Guard',(2.39,.36,0),(.075,.09,1.24),'Rear',2)
# Master-scene cargo kit. Minecraft selects the corresponding V6.1 baked meshes per slot.
joint('Boot_Cargo_Kit',(1.99,.51,0),'Cargo')
for name,z,mat in [('Food',-.51,11),('Medical',-.255,10),('Tools',0,2),('Fuel',.255,1),('Parcel',.51,11)]:
 part='Load_'+name;panel(part,(2.04,.56,z),(.17,.096,.21),'Boot_Cargo_Kit',mat)
 if name in ('Tools','Medical','Fuel'):
  panel(part+'_lip',(2.04,.612,z),(.173,.014,.213),part,2)
 if name in ('Tools','Fuel'):
  for k,off in enumerate([-.035,.035]):panel(part+'_post'+str(k),(2.04,.633,z+off),(.014,.03,.012),part,2)
  panel(part+'_handle',(2.04,.65,z),(.014,.01,.083),part,2)
 if name in ('Food','Parcel'):panel(part+'_tape',(2.04,.61,z),(.17,.004,.022),part,12)
 if name=='Medical':
  panel('Medical_stripe_A',(2.04,.621,z),(.057,.003,.013),part,14)
  panel('Medical_stripe_B',(2.04,.623,z),(.013,.003,.053),part,14)
joint('Windshield_Fracture',(-.365,1.01,.18),'Windows')
for k,(dx,dy,dz) in enumerate([(.075,.046,.004),(.011,.006,.07),(.038,.023,.004)]):
 panel('Fracture_Line_'+str(k),(-1.36+.03*k,.41+.018*k,.18+.02*k),(dx,.002,dz),'Windshield_Fracture',12,.57)
def wrap(name,target,pivot,parent):
 joint(name,pivot,parent);j=ids[name];k=ids[target];old=next(i for i,n in enumerate(nodes) if k in n.get('children',[]))
 prev=world(k);nodes[old]['children'].remove(k);nodes[j]['children'].append(k);nodes[k]['translation']=[a-b for a,b in zip(prev,pivot)]
 elem=next(e['uuid'] for e in b['elements'] if e['name']==target)
 group=next(gr for gr in bg.values() if elem in gr['children']);group['children'].remove(elem);bg[name]['children'].append(elem)
for side,idx,z in [('L','',1),('R','.001',-1)]:
 wrap('Mirror_Hinge_'+side,'Mirror_glass'+idx,[-.431,.908,z*.953],'Door_'+side+'_hinge')
 wrap('Lamp_Damage_'+side,'Headlamp_insert'+idx,[-2.32,.64,z*.71],'Lights')
 wrap('Glass_Damage_'+side,'Door_glass'+idx,[0,.84,z*.79],'Window_'+side)
def clip(name,tracks,length=.55):
 anim=dict(name=name,uuid=uid(name),length=length,loop='hold',snapping=24,animators={});ga=dict(name=name,channels=[],samplers=[])
 for node,kind,keys in tracks:
  aid=bg[node]['uuid'];obj=anim['animators'].setdefault(aid,dict(type='bone',name=node,keyframes=[]));values=[]
  for idx,(t,v) in enumerate(keys):
   if kind=='rotation':
    axis,deg=v;vec=[0]*3;vec[axis]=math.sin(math.radians(deg)/2);values.append(vec+[math.cos(math.radians(deg)/2)]);x,y,z=[deg if i==axis else 0 for i in range(3)];disp=[z,-y,x]
   elif kind=='scale':values.append(v);disp=[v[2],v[1],v[0]]
   else:values.append([a+c for a,c in zip(v,nodes[ids[node]].get('translation',[0]*3))]);disp=[v[2]*16,v[1]*16,v[0]*16]
   obj['keyframes'].append(dict(channel=kind,time=t,uuid=uid(f'{name}/{node}/{kind}/{idx}'),interpolation='linear',data_points=[dict(zip('xyz',disp))]))
  ga['channels'].append(dict(sampler=len(ga['samplers']),target=dict(node=ids[node],path='translation' if kind=='position' else kind)))
  ga['samplers'].append(dict(input=acc([(t,)for t,v in keys],'SCALAR'),output=acc(values,'VEC4' if kind=='rotation' else 'VEC3'),interpolation='LINEAR'))
 b['animations'].append(anim);g['animations'].append(ga)
def move(node,values):return(node,'position',values)
def rot(node,axis,values):return(node,'rotation',[(t,(axis,v))for t,v in values])
for name,node in [('Bullet_Impact_Body','Body'),('Bullet_Impact_Armor','Armor')]:clip(name,[move(node,[(0,[0,0,0]),(.05,[0,.009,.008]),(.18,[0,0,0])])],.18)
for side,sgn in [('L',1),('R',-1)]:clip('Mirror_Damage_'+side,[rot('Mirror_Hinge_'+side,0,[(0,0),(.08,sgn*12),(.3,sgn*24)])],.3)
for side in ['L','R']:
 clip('Headlight_Break_'+side,[('Lamp_Damage_'+side,'scale',[(0,[1,1,1]),(.2,[.65,.75,.88])])],.2)
 clip('Window_Shatter_'+side,[('Glass_Damage_'+side,'scale',[(0,[1,1,1]),(.2,[.001,.001,.001])])],.2)
for wheel in ['FL','FR','RL','RR']:clip('Tire_Damage_'+wheel,[('Wheel_'+wheel,'scale',[(0,[1,1,1]),(.48,[1,.87,1.035])])],.48)
clip('Windshield_Crack',[move('Windshield_Fracture',[(0,[0,0,0]),(.12,[1,.6,0])])],.12)
clip('Hood_Damage',[rot('Hood',2,[(0,0),(.12,-4),(.45,-2.3)])],.45)
clip('Bumper_Damage',[move('Armored_Plough',[(0,[0]*3),(.15,[.054,.013,0]),(.45,[.032,.008,0])])],.45)
for name,rev in [('Armor_Install',False),('Armor_Remove',True)]:
 clip(name,[move('Side_Plate_'+side,[(0,[0,0,0 if rev else s*.19]),(.55,[0,0,s*.19 if rev else 0])]) for side,s in [('L',1),('R',-1)]])
for name,angles in [('Fuel_Cap_Open',(0,75)),('Fuel_Cap_Close',(75,0))]:clip(name,[rot('Service_Cap',1,[(0,angles[0]),(.55,angles[1])])])
clip('Refueling',[rot('Service_Cap',1,[(0,75),(.38,79),(.8,75)])],.8)
clip('Trunk_Cargo_Shift',[rot('Cargo',2,[(0,0),(.15,1.4),(.35,-.65),(.6,0)])],.6)
clip('Vehicle_Repair',[move('Engine_V8',[(0,[0]*3),(.24,[0,.015,0]),(.55,[0]*3)])])
assert len(b['animations'])==len(g['animations'])
b['name']='GhostCamaro-V7';(dest/'GhostCamaro-V7.bbmodel').write_text(json.dumps(b,separators=(',',':')))
while len(buf)%4:buf.append(0)
g['buffers'][0]['byteLength']=len(buf);encoded=json.dumps(g,separators=(',',':')).encode();encoded+=b' '*(-len(encoded)%4)
(dest/'GhostCamaro-V7.glb').write_bytes(struct.pack('<4sIIII',b'glTF',2,28+len(encoded)+len(buf),len(encoded),0x4e4f534a)+encoded+struct.pack('<II',len(buf),0x004e4942)+buf)
tex=dest/'Textures';tex.mkdir(exist_ok=True)
for t in b['textures']:(tex/Path(t['name']).name).write_bytes(base64.b64decode(t['source'].split(',')[1]))
(dest/'ANIMATIONS.txt').write_text('\n'.join(f"{i+1:02}  {a['name']}  {a['length']:.2f}s" for i,a in enumerate(b['animations'])))
print(len(b['animations']),len(b['elements']))
