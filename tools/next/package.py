"""Package only tested sources, game artifacts and reproducible editor exports."""
from pathlib import Path
import json,os,subprocess,shutil,zipfile
root=Path.cwd();out=root/'build/next';base=root/'build/v7'
def copy(src,dest):
 dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(src,dest)
for folder,ext in [('Blockbench','.bbmodel'),('Blender','.blend'),('Exports/FBX','.fbx'),('Exports/GLB','.glb')]:
 copy(base/('GhostCamaro-V7'+ext),out/folder/'Vehicles'/('GhostCamaro'+ext))
shutil.copytree(base/'Textures',out/'Textures/Vehicles',dirs_exist_ok=True)
for ext in ['blend','glb','fbx','bbmodel']:\n assert (base/('GhostCamaro-V7.'+ext)).stat().st_size>1000\nsrc=out/'Minecraft/GhostCamaro-NEXT-Source.zip'
with zipfile.ZipFile(src,'w',zipfile.ZIP_DEFLATED) as z:
 for name in subprocess.check_output(['git','ls-files'],text=True).splitlines():z.write(root/name,name)
jar=next((out/'Minecraft').glob('*.jar'))
with zipfile.ZipFile(jar) as z:
 for n in z.namelist():
  if n.startswith('assets/ghostcamaro/sounds/') and n.endswith('.ogg'):copy_path=out/'Sounds'/Path(n).name;copy_path.parent.mkdir(exist_ok=True);copy_path.write_bytes(z.read(n))
  if n.startswith('assets/ghostcamaro/textures/') and n.endswith('.png'):
   p=out/'Textures/Minecraft'/n.split('textures/',1)[1];p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(z.read(n))
pack=out/'ResourcePack/GHOST-ROUTE-1990s.zip';pack.parent.mkdir(exist_ok=True)
amb=out/'Sounds/workshop_vent.ogg'
subprocess.run(['ffmpeg','-y','-loglevel','error','-f','lavfi','-i','anoisesrc=color=pink:amplitude=0.12:duration=8:seed=1979','-af','lowpass=f=340,afade=t=in:d=1,afade=t=out:st=7:d=1','-c:a','libvorbis',str(amb)],check=True)
with zipfile.ZipFile(pack,'w',zipfile.ZIP_DEFLATED) as z:
 z.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':15,'description':'GHOST ROUTE | 1990s · Road and workshop'}}))
 for target,skin in [('gray_concrete','old_concrete'),('black_concrete','asphalt'),('light_gray_concrete','cream_plaster')]:
  z.write(out/'Textures/Minecraft/block'/(skin+'.png'),'assets/minecraft/textures/block/'+target+'.png')
 z.write(amb,'assets/ghostroute/sounds/workshop_vent.ogg')
 z.writestr('assets/ghostroute/sounds.json',json.dumps({'workshop_vent':{'sounds':['ghostroute:workshop_vent']}}))
docs=out/'Documentation';docs.mkdir(exist_ok=True)
for name in ['README.md','BLENDER.md','ASSET_LICENSES.md','ANIMATIONS.md']:copy(root/name,docs/('V7-'+name))
copy(base/'ANIMATIONS.txt',out/'Animations/Vehicle.txt');copy(base/'FBX_CHECK.json',out/'Checks/Vehicle-FBX.json')
copy(Path('/tmp/export-checks.log'),out/'Checks/Blender.log')
copy(root/'.github/workflows/build.yml',out/'.github/workflows/build.yml')
rows=['| Объект | Blockbench | Blender | FBX | GLB | Minecraft | Клипы |','|---|---|---|---|---|---|---|']
for p in sorted((out/'Blockbench').rglob('*.bbmodel')):
 rel=p.relative_to(out/'Blockbench');n=rel.stem;cat=rel.parts[0]
 fmt=lambda d,ext:str(Path(d)/rel.with_suffix(ext))
 game=('vehicle/scene.json' if cat=='Vehicles' else ('models/block/' if cat=='Building' else 'models/item/')+n+'.json')
 rows.append('| '+' | '.join([n,str(p.relative_to(out)),fmt('Blender','.blend'),fmt('Exports/FBX','.fbx'),fmt('Exports/GLB','.glb'),game,'62' if cat=='Vehicles' else '0'])+' |')
(docs/'ASSET_MANIFEST.md').write_text('\n'.join(rows))
(docs/'NEXT.md').write_text("""# Ghost Camaro NEXT.1 · промежуточная сборка
Для запуска положите JAR и Fabric API 0.92.2+1.20.1 в mods клиента и сервера. Minecraft 1.20.1, Fabric Loader 0.16.10, Java 17. Команда /ghostcamaro spawn.
В комплекте исходники, 40 объектов окружения, переносимые модели предметов и базовая модель автомобиля с 62 клипами. Каждое импортированное в Blender окружение проверено обратным импортом FBX и GLB.
Стёкла получают собственные сохранённые стадии 0–5. Модуль 0 усиливает стекло, модуль 1 даёт многослойное. На остановленной машине ПКМ ремкомплектом по повреждённому стеклу заменяет выбранную панель. Сначала рисуются непрозрачные поверхности, затем прозрачные. Сколы, осколки и вмятины рассчитываются в игровом рендерере. Для металла применяются локальное подразделение и смещение вершин около попаданий; число отображаемых мест ограничено 24.
Отдельный ресурспак ставится в resourcepacks. Он заменяет три поверхности ванильного бетона и содержит синтезированный звук мастерской, доступный через /playsound ghostroute:workshop_vent ambient @s.
Исходные файлы Blender и Blockbench автомобиля содержат 62 клипа V7. Новые процедурные эффекты игры пока отсутствуют в редактируемой модели автомобиля. Стадии вмятин привязаны к текущему списку отметин. Полная библиотека груза, анимации оружия и все пункты большого ТЗ ещё в работе. Ручной прогон и визуальная оценка всех ракурсов не проводились. См. каталог Checks для фактически выполненных игровых проверок и Blender.log для реимпорта.
""")

(docs/'CONTROLS.md').write_text("""# Управление NEXT.1

Автомобиль: R — зажигание, W/S — газ и тормоз, A/D — руль, пробел — экстренный тормоз. ПКМ с кормы открывает багажник. При ремонте стекла цельтесь в саму панель. Параметры standardGlassStep, reinforcedGlassStep и laminatedGlassStep задаются в config/ghostcamaro.json. Полная таблица управления автомобилем приведена в V7-README.md.

Предметы тира: удерживать ПКМ для прицеливания, ЛКМ для выстрела, G для перезарядки. В режиме выживания положите соответствующие патроны в инвентарь. На пополнение магазина отводится 30 тиков, смена предмета прерывает операцию. HUD показывает остаток. Магазины на 8 и 24 игровых выстрела. Цифры предназначены для игрового баланса.
""")
(docs/'TEST_REPORT.md').write_text("""# Автоматические проверки NEXT.1

Сборка и выделенный сетевой сервер являются обязательными зависимостями создания архива. Сценарий Fabric GameTest проверяет независимые стёкла, накопление повреждений, сохранение, удержание разбитой панели защитным слоем и замену выбранного стекла. Проверяются появление снаряда до попадания и отложенная перезарядка. Результаты в Checks/Minecraft/dedicated/reports/gametest.xml. Приём соединения отдельным сервером — в Checks/Network. Снимки из кабины и багажника сделаны клиентом Minecraft в тестовом мире.

В Checks/Blockbench.json записан результат открытия каждого отдельного проекта предметов и окружения в Blockbench Web. Импорт FBX и GLB в пустые сцены Blender проверен для каждого экспорта библиотеки (Checks/Blender.log). Автомобиль проверен по отдельному FBX-аудиту. Целостность текстур в каждом материале после импорта и субъективное качество каждого ракурса вручную не удостоверялись.

Это промежуточный выпуск. Нет результатов испытаний UE5, Unity и шейдерпаков. Все сочетания остекления, посадки пассажира и LOD вручную не проверены.
""")
