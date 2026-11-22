"""Generate the baked 32px atlas tiles and oriented block/item model JSON (standard library)."""
from pathlib import Path
import re,random,json,struct,zlib
root=Path(__file__).resolve().parents[2]; d=root/'build/generated/ghost/assets/ghostcamaro'
def put(path,content):
 p=d/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(content if isinstance(content,bytes) else json.dumps(content,ensure_ascii=False).encode())
class Tile:
 def __init__(self,color,noise=8):
  self.pix=[[tuple(max(0,min(255,c+z))for c in color[:3])+(color[3] if len(color)>3 else 255,) for z in [rng.randint(-noise,noise) for x in range(32)]] for y in range(32)]
 def rect(self,x,y,X,Y,c):
  for j in range(max(0,y),min(31,Y)+1):
   for i in range(max(0,x),min(31,X)+1):self.pix[j][i]=(*c[:3],c[3] if len(c)>3 else 255)
 def line(self,x,y,X,Y,c):
  steps=max(1,abs(X-x),abs(Y-y))
  for n in range(steps+1):
   i=round(x+(X-x)*n/steps);j=round(y+(Y-y)*n/steps);self.rect(i,j,i,j,c)
 def save(self,path):
  def chunk(n,b):return struct.pack('>I',len(b))+n+b+struct.pack('>I',zlib.crc32(n+b))
  put(path,b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',32,32,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(b''.join(b'\0'+bytes(v for px in row for v in px)for row in self.pix)))+chunk(b'IEND',b''))
names=re.search(r'String NAMES="(.*?)"',(root/'src/main/java/dev/ghostcamaro/RouteWorkshop.java').read_text()).group(1).split()
russian='Старый бетон|Треснувший бетон|Кремовая штукатурка|Охристая штукатурка|Голубая штукатурка|Облезлая штукатурка|Кирпичная стена|Сколотый кирпич|Старая оконная рама|Выбитое окно|Служебная дверь|Балконная решётка|Асфальт|Выцветший асфальт|Трещины на асфальте|Белая полоса|Жёлтая полоса|Бордюр|Выбоина|Дорожный указатель|Битый кирпич|Бетонный обломок|Щебень|Оголённая арматура|Остаток стены|Строительный мусор|Мешки с песком|Бетонное ограждение|Дорожный барьер|Дощатый ящик|Коробка помощи|Медицинский контейнер|Поддон|Бочка|Бензоколонка|Канистра для декорации|Генератор|Инструментальный ящик|Полевая радиостанция|Верстак механика'.split('|')
langs={'ru_ru':{},'en_us':{}}
def label(key,a,b):langs['ru_ru'][key]=a;langs['en_us'][key]=b
faces={f:{'texture':'#skin'}for f in 'north south east west up down'.split()}
for i,n in enumerate(names):
 rng=random.Random(i+1979); concrete=(127,129,119);asphalt=(60,63,61);wood=(133,103,68);metal=(86,105,103);dark=(52,56,52);light=(191,182,155)
 c=[concrete,concrete,(179,169,145),(185,148,97),(128,152,152),(173,158,132),(155,94,67),(147,91,70),metal,metal,metal,dark,asphalt,(81,82,77),asphalt,asphalt,asphalt,concrete,asphalt,(163,151,110),(142,85,63),concrete,concrete,(120,78,56),concrete,concrete,(138,129,94),concrete,(185,156,91),wood,(169,133,83),(155,176,158),wood,(96,112,106),(166,119,80),(111,125,71),(100,114,87),(106,105,96),(71,89,78),wood][i]
 tile=Tile(c,10); boxes=[(0,0,0,16,16,16)]
 if i in [1,5,7,14,18,20,21,22,24,25]:
  for _ in range(4):
   x,y=rng.randrange(32),rng.randrange(32)
   for t in range(5):
    X,Y=x+rng.randint(-3,4),y+rng.randint(1,4);tile.line(x,y,X,Y,dark);x,y=X,Y
 if i in (6,7,20):
  for y in range(0,32,6):
   tile.rect(0,y,31,y,(117,112,95))
   for x in range((y//6%2)*6,32,12):tile.rect(x,y,x,y+5,(122,113,95))
 if i in (15,16):tile.rect(13,0,17,31,(206,198,164)if i==15 else(205,164,74))
 if i in (8,9,11):
  boxes=[(0,0,6,2,16,9),(14,0,6,16,16,9),(2,14,6,14,16,9),(2,0,6,14,2,9),(7,2,6,9,14,8)]
  if i==8:boxes.append((2,7,6,14,8,8))
 if i==10:boxes=[(0,0,6,16,16,9)];tile.rect(2,4,29,20,dark);tile.rect(4,6,27,18,metal);tile.rect(23,22,29,23,light)
 if i in [17,18,20,21,22,23,25]:boxes=[(0,0,1,16,3,15)] if i in [17,18] else [(1,0,1,8,4,6),(8,0,6,14,3,13),(3,0,9,9,2,15)]
 if i==19:boxes=[(7,0,7,9,14,9),(3,9,6,13,16,8)];tile.line(5,15,26,15,dark);tile.line(21,10,26,15,dark)
 if i==24:boxes=[(0,0,6,16,6,10),(0,6,6,10,11,10),(0,11,6,5,15,10)]
 if i in (26,27,28):boxes=[(0,0,4,16,4,12),(1,4,5,15,11,11)]
 if i==26:tile.rect(0,15,31,16,(91,85,66))
 if i==28:
  for x in range(-32,33,12):
   for k in range(4):tile.line(x+k,0,x+k+25,31,(116,70,60))
 if i in (29,30,31,35,36,37,38):
  boxes=[(2,0,3,14,8,13)]
  if i in (29,36):boxes=[(1,0,1,15,13,15)]
  if i in (35,37):boxes.extend([(6,8,7,7,11,9),(9,8,7,10,11,9),(6,10,7,10,11,9)])
  if i in (30,29):tile.rect(13,0,17,31,light);tile.rect(3,18,11,24,(215,209,173))
  if i==31:tile.rect(13,8,18,24,(54,103,100));tile.rect(7,14,24,18,(54,103,100))
  if i in (36,38):
   tile.rect(3,3,28,14,dark)
   for x in range(5,28,3):tile.rect(x,18,x,27,dark)
 if i==32:boxes=[(x,2,1,x+3,4,15)for x in (0,4,8,12)]+[(1,0,z,15,2,z+2)for z in (1,12)]
 if i==33:boxes=[(3,0,3,13,15,13)];tile.rect(0,5,31,7,dark);tile.rect(0,24,31,26,dark)
 if i==34:boxes=[(3,0,3,13,2,13),(4,2,4,12,9,12),(3,9,3,13,16,13)];tile.rect(3,4,28,14,dark);tile.rect(5,6,26,12,(143,165,139));tile.rect(5,19,27,21,(146,64,48))
 if i==39:boxes=[(1,10,1,15,13,15)]+[(x,0,z,x+2,10,z+2) for x in (2,12)for z in(2,12)]
 tile.save(f'textures/block/{n}.png');skin='ghostcamaro:block/'+n
 put(f'models/block/{n}.json',dict(textures={'skin':skin,'particle':skin},elements=[{'from':v[:3],'to':v[3:],'faces':faces}for v in boxes]))
 put(f'blockstates/{n}.json',dict(variants={'facing='+direction:{'model':skin,'y':j*90}for j,direction in enumerate(['north','east','south','west'])}))
 put(f'models/item/{n}.json',{'parent':skin});label('block.ghostcamaro.'+n,russian[i],n.replace('_',' ').title())
for i,(n,ru) in enumerate(zip('armor_panel workshop_key pistol_rounds rifle_rounds range_pistol range_rifle'.split(),['Панель брони','Ключ мастерской','Патроны для пистолета','Патроны для винтовки','Полигонный пистолет','Полигонная винтовка'])):
 rng=random.Random(6+i);t=Tile((0,0,0,0),0);steel=(109,119,114);grip=(98,74,54);gold=(174,144,86)
 if i==0:t.rect(4,6,27,24,steel);t.line(5,6,26,6,light)
 elif i==1:t.line(5,27,24,8,steel);t.line(6,27,25,8,steel);t.rect(18,5,25,10,steel);t.rect(21,4,25,7,(0,0,0,0))
 elif i<4:
  for x in [8,14,20]:t.rect(x,8,x+3,24,gold);t.rect(x+1,5,x+2,8,grip)
 else:
  t.rect(6,11,26,15,steel);t.rect(10,16,14,23,grip);t.rect(15,16,19,18,dark)
  if i==5:t.rect(2,12,9,17,grip);t.rect(18,16,21,22,steel);t.rect(23,12,30,13,dark)
 t.save(f'textures/item/{n}.png');put(f'models/item/{n}.json',{'parent':'item/handheld','textures':{'layer0':'ghostcamaro:item/'+n}});label('item.ghostcamaro.'+n,ru,n.replace('_',' ').title())
for k,ru,en in [('pump_wait','Заглушите мотор и откройте лючок у колонки. Остаток: %s л','Stop the engine and open the fuel door. Stock: %s L'),('pump_stock','Остаток колонки: %s л','Pump reserve: %s L'),('workshop','Мастерская Camaro','Camaro workshop'),('module.0','Двери и днище','Doors and floor'),('module.1','Стекло и моторный отсек','Glass and engine bay'),('module.2','Наружная защита','Exterior guards')]:label('ghostcamaro.'+k,ru,en)
for lang,data in langs.items():
 base=json.loads((root/'tools/v7'/f'{lang}.json').read_text());put('lang/'+lang+'.json',base|data)
for name in ['crack','impact']:
 rng=random.Random(1979);t=Tile((0,0,0,0),0)
 if name=='crack':
  for x,y in [(0,7),(9,0),(30,2),(31,21),(19,31),(0,29)]:
   t.line(16,16,x,y,(173,195,197));t.line((16+x)//2,(16+y)//2,x,min(31,y+7),(131,155,157))
 else:
  for y in range(9,24):
   for x in range(9,24):
    r=((x-16)**2+(y-16)**2)**.5+rng.random()*2
    if r<7:t.rect(x,y,x,y,(51,48,43)if r<4.5 else (143,137,123))
 t.save('textures/entity/'+name+'.png')
