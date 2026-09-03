# 南海航程 · libGDX（Java）

依据 `/home/box/shipgame/需求文档.md` **v0.23**：引擎 **libGDX / Java**，交付 Android APK。画面为俯视像素风，贴图在 `assets/textures/`。

原 Unity 半成品在 `废弃-unity/`。需求文档与 `需求文档历史/` 未改。

包名：`com.shipgame.nanhai`  
模块：`core`（玩法）、`lwjgl3`（桌面）、`android`（APK，需 SDK）

## JDK

```
export JAVA_HOME=/home/box/.local/jdk/current
export PATH="$JAVA_HOME/bin:$PATH"
```

## 桌面

```
cd /home/box/shipgame
./gradlew lwjgl3:run
./gradlew core:compileJava lwjgl3:compileJava
```

### 操作

- 左虚拟摇杆：只转向（推摇杆会取消自动驶向）
- 右侧 **加速 / 减速** 长按；松手滑行
- 桌面：**A/D**（或左右）转向，**W/上** 加速，**S/下** 减速
- 点左上小地图开全图（只显示港和岛，无海盗）；全图点港口自动驶向；**取消自动** 按钮；**M** 也可开关全图
- 点海盗锁定自动开火；**取消锁定**；开出范围逃跑
- **图鉴**随时可开；**货物**三栏（商货 / 异兽 / 草药）共用容量；海上点货再点丢掉

靠近港口自动靠泊，世界暂停。岛上菜单搜采。

## 一期已实现（core）

- 本机用户名+密码登录、本机 JSON 存档（靠港及港口操作时写盘；失败读上次靠港档）
- 十港、14 种商货固定差价；点开一种货看各港行情；买/卖
- 货舱三列表共用容量；满舱不能买；海上列表丢货
- 航行扣补给（按实际船员加快）；空则失败；回港按缺口花钱；不够则借债；每次回港欠款 2% 计息；手动还债；欠债可离港
- 风只改航速；雨/雾让海图/标签看不清
- 随机海盗；锁定连射；默认就地打；玩家开火后海盗追得紧；打沉/补给空失败读档；战后改回手动
- 岛搜采；图鉴；草药只卖
- 回港立刻花钱修船；升仓库=共用容量；升炮火只加伤害；先升编制再雇人，火力与补给按实际人数

未点名的数字均为占位。画面为俯视像素风：海水平铺，船/海盗/港/岛用贴图图标。

## Android APK

`settings.gradle` 在 `local.properties` 的 `sdk.dir` 或 `ANDROID_HOME` 存在时才 include `:android`。

```
./gradlew android:assembleDebug
```

APK 拷贝到 `Builds/NanHaiVoyage.apk`（组装成功时）。最低 API 21，目标 33，横屏。

libGDX **1.13.1**。
