"""Generate editor reference meshes for the five runtime cargo models (BootRenderer)."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[2];d=root/'build/generated/ghost/assets/ghostcamaro'
for kind in ['food','medical','tools','fuel','parcel']:
    parts=[]
    def add(name,x,y,z,w,h,l,u=0,v=0):
        # Runtime ModelPart uses 64x32 texture, with .33 drawing scale.
        fx,fy,fz=8+x*.33,y*.33,8+z*.33
        uv={'west':(u,v+l,u+l,v+l+h),'north':(u+l,v+l,u+l+w,v+l+h),'east':(u+l+w,v+l,u+2*l+w,v+l+h),'south':(u+2*l+w,v+l,u+2*l+2*w,v+l+h),'up':(u+l+w,v,u+l+2*w,v+l),'down':(u+l,v,u+l+w,v+l)}
        parts.append({'name':name,'from':[fx,fy,fz],'to':[fx+w*.33,fy+h*.33,fz+l*.33],'faces':{k:{'texture':'#skin','uv':[a/4,b/2,c/4,e/2]} for k,(a,b,c,e) in uv.items()}})
    if kind=='fuel':
        add('Can',-3,0,-4,6,4,8);add('Cap',-2.6,4,-3.6,2,1,2,32)
        for z in [0,3]:add('Handle post '+str(z),-1,4,z-1,2,1,1,32)
        add('Handle',-1,5,-1,2,1,4,32)
    elif kind in ('tools','medical'):
        add('Case',-4,0,-5,8,4,10);add('Lid',-4.1,4,-5.1,8.2,1,10.2,0,18)
        for z in [-3,2]:add('Latch '+str(z),3.93,2,z,.3,2,1,32)
    else:add('Parcel',-4,0,-5,8,5,10)
    p=d/'models/cargo'/('load_'+kind+'.json');p.parent.mkdir(exist_ok=True)
    p.write_text(json.dumps({'elements':parts,'textures':{'skin':'ghostcamaro:entity/load_'+kind}}))
