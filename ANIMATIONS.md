# 40 клипов Ghost Camaro V5

Для каждой строки исходники — masters/Ghost_Camaro_V5.bbmodel и одноимённый GLB. Конвертер проверяет равенство множеств имён, затем включает все ключи GLB в ресурсы JAR. Сборка проверяет перенос каждого клипа. Покадровое визуальное воспроизведение каждого из 40 клипов в игре отдельно не проверено. Серверные и клиентские проверки отдельных сценариев перечислены в TEST_REPORT.md.

| Клип V5 | Секунды | Узлы оригинала | Реализация и триггер в игре |
|---|---:|---|---|
| `Door_L_Open` | 0.85 | Door_L_hinge | Фаза петли при переключении двери |
| `Door_L_Close` | 0.85 | Door_L_hinge | Фаза петли при переключении двери; обратный ход дорожки открытия |
| `Window_L_Down` | 1.2 | Window_L, Crank_L | Фаза стекла и ручки соответствующей двери |
| `Window_L_Up` | 1.2 | Window_L, Crank_L | Фаза стекла и ручки соответствующей двери; обратный ход дорожки открытия |
| `Door_R_Open` | 0.85 | Door_R_hinge | Фаза петли при переключении двери |
| `Door_R_Close` | 0.85 | Door_R_hinge | Фаза петли при переключении двери; обратный ход дорожки открытия |
| `Window_R_Down` | 1.2 | Window_R, Crank_R | Фаза стекла и ручки соответствующей двери |
| `Window_R_Up` | 1.2 | Window_R, Crank_R | Фаза стекла и ручки соответствующей двери; обратный ход дорожки открытия |
| `Wipers` | 1.4 | Wiper_L, Wiper_R | Повтор цикла при включённых дворниках |
| `Clutch_Cycle` | 1.2 | Clutch | Сцепление участвует в Gear_Shift |
| `Brake_Cycle` | 1.2 | Brake, Stop_L, Stop_R | Педаль и стоп-сигналы при рабочем/экстренном торможении |
| `Throttle_Cycle` | 1.2 | Throttle | Педаль при газе с работающим двигателем |
| `Handbrake_Cycle` | 1.2 | Handbrake | Положение рычага при удержании/отпускании пробела; переход через базовую позу |
| `Handbrake_Engage` | 0.45 | Handbrake | Положение рычага при удержании/отпускании пробела |
| `Handbrake_Release` | 0.45 | Handbrake | Положение рычага при удержании/отпускании пробела; переход через базовую позу |
| `Gear_Shift` | 1.3 | Clutch, Transmission | Смена ступени во время движения |
| `Hood_Open` | 0.85 | Hood | Взаимодействие спереди или клавиша капота |
| `Hood_Close` | 0.85 | Hood | Взаимодействие спереди или клавиша капота; обратный ход дорожки открытия |
| `Trunk_Open` | 0.85 | Trunk | Крышка багажника и доступ к контейнеру |
| `Trunk_Close` | 0.85 | Trunk | Крышка багажника и доступ к контейнеру; обратный ход дорожки открытия |
| `Glovebox_Open` | 0.85 | Glovebox | Клавиша бардачка |
| `Glovebox_Close` | 0.85 | Glovebox | Клавиша бардачка; обратный ход дорожки открытия |
| `Steering_Left_Right` | 2.8 | Steering_Wheel, Knuckle_FL, Knuckle_FR | Выборка по углу руления A/D |
| `Wheels_Roll_Loop` | 1 | Wheel_FL, Wheel_FR, Wheel_RL, Wheel_RR | Вращение от фактического пути и радиуса |
| `Drive_Straight` | 2.4 | Vehicle_Root, Wheel_FL, Wheel_FR, Wheel_RL, Wheel_RR, RPM_needle, Speed_needle | Физика Entity задаёт перемещение и поворот, колёса следуют пройденному расстоянию |
| `Drive_Turn_Left` | 3 | Vehicle_Root, Wheel_FL, Wheel_FR, Wheel_RL, Wheel_RR, Steering_Wheel, Knuckle_FL, Knuckle_FR, FrontTurn_L, RearTurn_L, DashTurn_L | Физика Entity задаёт перемещение и поворот, колёса следуют пройденному расстоянию |
| `Drive_Turn_Right` | 3 | Vehicle_Root, Wheel_FL, Wheel_FR, Wheel_RL, Wheel_RR, Steering_Wheel, Knuckle_FL, Knuckle_FR, FrontTurn_R, RearTurn_R, DashTurn_R | Физика Entity задаёт перемещение и поворот, колёса следуют пройденному расстоянию |
| `Speed_needle_Sweep` | 2 | Speed_needle | Угол стрелки по скорости |
| `RPM_needle_Sweep` | 2 | RPM_needle | Угол стрелки по оборотам |
| `Headlights_On` | 0.36 | Head_L, Head_R, BeamPreview_L, BeamPreview_R | Состояние видимости фар и светового следа по L |
| `Headlights_Off` | 0.36 | Head_L, Head_R, BeamPreview_L, BeamPreview_R | Состояние видимости фар и светового следа по L; переход через базовую позу |
| `Indicator_Left` | 1.44 | FrontTurn_L, RearTurn_L, DashTurn_L | Мигают передняя, задняя и приборная лампы выбранной стороны |
| `Indicator_Right` | 1.44 | FrontTurn_R, RearTurn_R, DashTurn_R | Мигают передняя, задняя и приборная лампы выбранной стороны |
| `Hazards` | 1.44 | FrontTurn_L, RearTurn_L, DashTurn_L, FrontTurn_R, RearTurn_R, DashTurn_R | Все шесть ламп при аварийке |
| `Brake_Normal` | 2.72 | Vehicle_Root, Brake, Speed_needle, Wheel_FL, Wheel_FR, Wheel_RL, Wheel_RR, Stop_L, Stop_R | Педаль и стоп-сигналы при рабочем/экстренном торможении |
| `Brake_Emergency` | 1.2 | Vehicle_Root, Brake, Speed_needle, Wheel_FL, Wheel_FR, Wheel_RL, Wheel_RR, Stop_L, Stop_R | Педаль и стоп-сигналы при рабочем/экстренном торможении |
| `Breakdown_Stall` | 2 | Engine_V8, RPM_needle, Hood | Колебания под капотом при повреждённом работающем двигателе |
| `Smoke_Minor_Loop` | 3 | Vapour_00, Vapour_01, Vapour_02, Vapour_03, Vapour_04, Vapour_05 | Светлый дым при HP < 45% |
| `Smoke_Critical_Loop` | 3 | Soot_00, Soot_01, Soot_02, Soot_03, Soot_04, Soot_05, Soot_06, Soot_07, Soot_08, Soot_09, Soot_10 | Густой дым в первые три секунды критического отказа |
| `Critical_Failure_Burst` | 6 | Soot_00, Soot_01, Soot_02, Soot_03, Soot_04, Soot_05, Soot_06, Soot_07, Soot_08, Soot_09, Soot_10, Flare_00, Flare_01, Flare_02, Flare_03, Flare_04, Hood, Engine_V8, RPM_needle | Финальная шестисекундная фаза после густого дыма |

Углы трёх зеркал управляются процедурно. Новых клипов в мастер-модель не добавлено.


## Дополнения V7

Номер, имя и длительность каждого нового клипа в секундах.

```text
41  Bullet_Impact_Body  0.18s
42  Bullet_Impact_Armor  0.18s
43  Mirror_Damage_L  0.30s
44  Mirror_Damage_R  0.30s
45  Headlight_Break_L  0.20s
46  Window_Shatter_L  0.20s
47  Headlight_Break_R  0.20s
48  Window_Shatter_R  0.20s
49  Tire_Damage_FL  0.48s
50  Tire_Damage_FR  0.48s
51  Tire_Damage_RL  0.48s
52  Tire_Damage_RR  0.48s
53  Windshield_Crack  0.12s
54  Hood_Damage  0.45s
55  Bumper_Damage  0.45s
56  Armor_Install  0.55s
57  Armor_Remove  0.55s
58  Fuel_Cap_Open  0.55s
59  Fuel_Cap_Close  0.55s
60  Refueling  0.80s
61  Trunk_Cargo_Shift  0.60s
62  Vehicle_Repair  0.55s
```
