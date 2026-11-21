"""Small purpose-drawn pixel textures. For regeneration install Pillow. PNG files are committed."""
from pathlib import Path
import random
from PIL import Image, ImageDraw
dest=Path(__file__).resolve().parents[1]/'src/main/resources/assets/ghostcamaro/textures/entity'
dest.mkdir(parents=True,exist_ok=True)
for index,(kind,base) in enumerate([('food',(161,119,73)),('medical',(182,186,169)),('tools',(78,98,108)),('fuel',(92,109,67)),('parcel',(139,110,86))]):
    rng=random.Random(1979+index); im=Image.new('RGB',(64,32));pix=im.load()
    for y in range(32):
        for x in range(64):
            delta=rng.randrange(-7,8);pix[x,y]=tuple(c+delta for c in base)
    d=ImageDraw.Draw(im)
    if kind in ('food','parcel'):
        tape=(207,171,117) if kind=='food' else (193,180,147)
        d.rectangle((13,0,15,15),fill=tape);d.rectangle((30,10,32,15),fill=tape)
        d.line((10,5,17,5),fill=(91,76,60));d.rectangle((1,11,6,13),fill=(216,212,180));d.line((2,12,4,12),fill=(75,71,64))
    elif kind=='medical':
        for x,y in [(4,12),(13,5),(23,12),(32,12),(13,23)]:
            d.rectangle((x-2,y,x+2,y),fill=(43,104,101));d.rectangle((x,y-2,x,y+2),fill=(43,104,101))
    elif kind=='tools':
        for y in [11,13,27]:d.line((1,y,34,y),fill=(113,130,137))
        d.rectangle((31,0,42,4),fill=(171,164,128))
    else:
        d.line((1,8,6,11),fill=(127,139,86));d.line((1,11,6,8),fill=(127,139,86));d.rectangle((33,0,37,3),fill=(65,70,56))
    im.save(dest/('load_'+kind+'.png'))
