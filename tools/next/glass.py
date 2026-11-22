"""Generate reproducible stage textures shared by vehicle materials and editors."""
import math,random,struct,zlib
from pathlib import Path
out=Path(__file__).resolve().parents[2]/'build/generated/ghost/assets/ghostcamaro/textures/entity'
def chunk(tag,data):
 return struct.pack('>I',len(data))+tag+data+struct.pack('>I',zlib.crc32(tag+data))
for stage in range(1,6):
 size=128;pixels=bytearray(size*size*4);rng=random.Random(1979)
 def line(x,y,X,Y,rgb):
  steps=max(1,int(math.hypot(X-x,Y-y)*2))
  for k in range(steps+1):
   f=k/steps;i=round(x+(X-x)*f);j=round(y+(Y-y)*f)
   if 0<=i<size and 0<=j<size:pixels[(j*size+i)*4:(j*size+i+1)*4]=bytes((*rgb,255))
 for ray in range(13):
  angle=(ray+rng.uniform(-.32,.32))*math.tau/13;x=y=64
  for seg in range(6):
   turn=angle+rng.uniform(-.35,.35);r=rng.uniform(5,10)
   X=x+r*math.cos(turn);Y=y+r*math.sin(turn)
   if seg<stage and ray<3+2*stage:
    line(x+1,y,X+1,Y,(54,75,84));line(x,y,X,Y,(205,222,221))
    if stage>=3 and seg>0:
     a=turn+.88;line(x,y,x+math.cos(a)*r*.85,y+math.sin(a)*r*.85,(165,184,194))
    if stage>=4 and seg%2==1:
     a=angle+math.tau/13;d=math.hypot(X-64,Y-64);line(X,Y,64+d*math.cos(a),64+d*math.sin(a),(164,185,189))
   x,y=X,Y
 data=b''.join(bytes(1)+pixels[j*size*4:(j+1)*size*4] for j in range(size))
 (out/f'glass_{stage}.png').write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',size,size,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(data))+chunk(b'IEND',b''))
