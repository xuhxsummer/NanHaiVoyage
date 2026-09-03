# 南海行 · Unity 一期工程

依据 `/home/box/shipgame/需求文档.md` **v0.21 已确认** 搭的 Unity 工程骨架。
FreeBuff CLI 在本环境是交互 TUI，无法无头跑完实现；脚本按文档手写。需求文档与 `需求文档历史/` **未改**。

引擎目标：Unity 2022.3 LTS（`ProjectSettings/ProjectVersion.txt` 写的是 2022.3.50f1）。
交付：Android APK。本机没有 Unity Editor、没有 Android SDK，因此这里打不出 APK。

## 已接线的一期玩法（规则不另编）

- 本机用户名+密码登录，JSON 存档；靠港自动存；失败读档
- 十港：广州、潮州、雷州、琼州、崖州、合浦、交州、占城、真腊、佛逝
- 14 商货固定差价（具体价格是占位；文档只确认固定、不随时间变）
- 货舱三列表（商货/异兽/草药）共用容量；满舱须卖或丢；海上列表丢货即没
- 航行扣补给，空则失败；只能回港按缺口花钱补；没钱借债；回港手动还、不还能走；每次回港利息 2%
- 天气：风只改航速；雨雾挡海图
- 左摇杆转向、右长按加速/减速、松手滑行
- 小地图只显示港和岛；点开放大全图（船继续开）；全图点港自动驶向；取消按钮；推摇杆改手动
- 随机海盗；点船/按钮锁定自动连射；取消锁定；开出范围逃跑；还击会追得紧；缴获当货卖
- 靠近自动进港，进港世界暂停；立刻花钱修船；货舱升级加大共用容量；炮火升级只加伤害；先升人数上限再雇人立刻上船；未满员按实际人数算火力和补给
- 岛菜单搜采；图鉴随时打开；草药一期只卖

后期未做：船队托管、上岛自走、草药使用、装备栏、近景海战、联网存档。

占位（文档没点名，改 `Assets/Scripts/Data/GameBalance.cs` 与 `WorldCatalog.cs`）：
起航银两、补给/耐久上限、航速、遇敌频率、物价表、岛名、山海经具体条目、出港落点（文档开放问题）。画面现为程序生成几何体，不是最终半写实美术。

## 本机缺什么

- Unity 2022.3 Editor + Android Build Support：打开工程、出 APK
- Android SDK / NDK / JDK（Unity Hub 可装）
- 一台 Android 手机或模拟器
- 可选：在真实终端交互跑 FreeBuff 继续改代码

## 人要做的：打出 APK

1. 安装 Unity Hub，装 Unity 2022.3 LTS，模块勾选 Android Build Support（含 SDK/NDK/OpenJDK）。
2. Hub 里 Open，选本目录 `/home/box/shipgame`（或拷到有 Unity 的电脑再打开）。
3. 首次打开会补全 ProjectSettings 缺项并生成 .meta。GameDirector 进 Main 场景会自举。
4. File > Build Settings > Android > Switch Platform。包名 com.shipgame.nanhai，Min SDK 22，横屏。
5. 菜单 NanHai > Build Android APK，或 Build Settings > Build，输出 Builds/NanHaiVoyage.apk。
6. adb install -r Builds/NanHaiVoyage.apk，注册账号，广州出港，跑一两个港、遇海盗、回港存档。

Editor 装好后的命令行示例：

    /path/to/Unity -batchmode -quit -projectPath /home/box/shipgame -executeMethod BuildAndroid.Build -logFile -

## FreeBuff

完整尝试日志：`/workspace/freebuff-phase1.log`。
CLI 0.0.166 只有 login 子命令；把 prompt 当文件或参数会报：
Allowed choices are login.
stdin 会打开 TUI（广告+模型选择），不会无头写代码。

在真实终端继续：

    freebuff --cwd /home/box/shipgame

默认模型 GLM 5.3 Flash，以需求文档为准，不要改历史目录。
