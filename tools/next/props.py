"""Exterior geometry for handheld game props. Dimensions are in Minecraft model units."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[2];assets=root/'build/generated/ghost/assets/ghostcamaro'
def box(label,a,b):return label,a,b
def bundle(label,*rows):return [box(label+str(i+1),a,b) for i,(a,b) in enumerate(rows)]
props={
 'range_pistol':('toolbox',[box('Housing',[6.8,8,4],[9.2,10.2,12.2]),box('Top cover',[6.65,10.2,3.7],[9.35,11.7,12.5]),box('Grip',[6.9,3,9.2],[9.1,8.1,12]),box('Base',[6.7,2.7,9.1],[9.3,3.4,12.2]),box('Guard rail',[7.65,6.4,6.4],[8.35,8,7]),box('Guard base',[7.65,6.1,6.4],[8.35,6.6,9.6]),box('Sight',[7.7,11.7,4],[8.3,12.3,4.7])]),
 'range_rifle':('generator',[box('Body',[6.8,8.3,5.6],[9.2,10.8,11.8]),box('Foregrip',[6.9,8.6,1.5],[9.1,10.6,6]),box('Front tube',[7.5,9.2,-3],[8.5,10.2,1.5]),box('Butt',[7.2,8.3,11.8],[8.8,10.3,15.8]),box('Heel',[6.9,7.6,15],[9.1,10.6,16]),box('Grip',[7.2,5.5,10.1],[8.8,8.4,11.6]),box('Magazine',[7.1,4.9,6.8],[8.9,8.4,8.6]),box('Front sight',[7.7,10.2,0],[8.3,11.6,.8])]),
 'range_round':('lane_yellow',[box('Visible tracer',[7,7,4],[9,9,10]),box('Nose',[7.4,7.4,2],[8.6,8.6,4])]),
 'armor_panel':('old_concrete',[box('Plate',[2,3,7],[14,12,8.2]),box('Folded lip',[2,2.8,7],[14,3.4,8.6])]),
 'workshop_key':('toolbox',bundle('Tool',([7.5,2,7.5],[8.5,11,8.5]),([6.5,10,7.4],[9.5,11.3,8.6]),([6.5,11,7.4],[7.3,13,8.6]),([8.7,11,7.4],[9.5,13,8.6]))),
 'fuel_can':('jerry_can',[]),'repair_kit':('medical_case',[]),
 'pistol_rounds':('medical_case',[box('Carton',[3,4,4],[13,8,12]),box('Lid',[2.8,8,3.8],[13.2,8.5,12.2])]),
 'rifle_rounds':('wood_crate',[box('Case',[2,3,4],[14,8,11]),box('Lid',[1.8,8,3.8],[14.2,8.7,11.2])])}
for name,(skin,parts) in props.items():
 block=json.loads((assets/'models/block'/f'{skin}.json').read_text())
 if parts:
  block['elements']=[{'name':label,'from':a,'to':b,'faces':{f:{'texture':'#skin','uv':[1,1,15,15]} for f in ('north','south','west','east','up','down')}} for label,a,b in parts]
 block['display']={'gui':{'rotation':[22,135,0],'scale':[.8]*3},'ground':{'translation':[0,3,0],'scale':[.48]*3},'thirdperson_righthand':{'rotation':[75,0,0],'scale':[.8]*3},'firstperson_righthand':{'rotation':[0,0,0],'translation':[0,1,0],'scale':[.9]*3}}
 (assets/'models/item'/f'{name}.json').write_text(json.dumps(block))
