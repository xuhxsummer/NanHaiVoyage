using System.Collections.Generic;
using UnityEngine;

/// <summary>
/// 一期核心循环：登录/存档、十港跑商、补给借债、天气、摇杆航行、
/// 全图点港自动驶、海盗锁定连射与逃跑、靠港暂停、岛菜单搜采、图鉴。
/// 未确认数字走 GameBalance 占位。后期内容（船队、上岛自走、草药用、装备栏、近景海战、联网）不做。
/// </summary>
public class GameDirector : MonoBehaviour
{
    public enum Mode { Login, Port, Sea, Combat, Island, Fail }

    public static GameDirector I { get; private set; }

    public Mode mode = Mode.Login;
    public string user;
    public ShipSave ship;
    public WeatherSystem weather = new WeatherSystem();

    Transform _shipTf;
    Camera _cam;
    float _speed;
    bool _holdAccel, _holdDecel, _joystickHeld;
    Vector2 _joy;
    string _autoPort;
    bool _showFullMap;
    bool _showCodex;
    bool _worldPaused;

    // combat
    Transform _pirate;
    bool _locked;
    bool _playerReturnedFire;
    float _playerFireCd, _pirateFireCd, _pirateCheck;
    float _pirateHull = 40f;

    // login ui
    string _u = "";
    string _p = "";
    string _msg = "";
    Vector2 _scroll;
    string _selectedGood;
    string _dumpGood;

    GameObject _sea, _shipGo;
    readonly List<GameObject> _markers = new List<GameObject>();

    [RuntimeInitializeOnLoadMethod(RuntimeInitializeLoadType.AfterSceneLoad)]
    static void AutoBoot()
    {
        if (FindObjectOfType<GameDirector>() != null) return;
        var go = new GameObject("GameDirector");
        go.AddComponent<GameDirector>();
    }

    void Awake()
    {
        I = this;
        WorldCatalog.EnsurePrices();
        BuildWorld();
    }

    void BuildWorld()
    {
        _sea = GameObject.CreatePrimitive(PrimitiveType.Plane);
        _sea.name = "Sea";
        _sea.transform.localScale = new Vector3(16, 1, 16);
        _sea.GetComponent<Renderer>().material.color = new Color(0.12f, 0.28f, 0.42f);

        foreach (var kv in WorldCatalog.PortPositions)
        {
            var m = GameObject.CreatePrimitive(PrimitiveType.Cylinder);
            m.name = "Port_" + kv.Key;
            m.transform.position = kv.Value;
            m.transform.localScale = new Vector3(2.2f, 0.4f, 2.2f);
            m.GetComponent<Renderer>().material.color = new Color(0.72f, 0.55f, 0.28f);
            _markers.Add(m);
        }
        for (int i = 0; i < WorldCatalog.IslandNames.Length; i++)
        {
            var m = GameObject.CreatePrimitive(PrimitiveType.Cube);
            m.name = "Island_" + WorldCatalog.IslandNames[i];
            m.transform.position = WorldCatalog.IslandPositions[i] + Vector3.up * 0.4f;
            m.transform.localScale = new Vector3(3.2f, 0.8f, 2.4f);
            m.GetComponent<Renderer>().material.color = new Color(0.28f, 0.5f, 0.28f);
            _markers.Add(m);
        }

        _shipGo = GameObject.CreatePrimitive(PrimitiveType.Capsule);
        _shipGo.name = "Ship";
        _shipGo.transform.localScale = new Vector3(0.8f, 0.35f, 1.6f);
        _shipTf = _shipGo.transform;
        _shipGo.GetComponent<Renderer>().material.color = new Color(0.82f, 0.78f, 0.68f);

        _cam = Camera.main;
        if (_cam == null)
        {
            var c = new GameObject("Main Camera");
            _cam = c.AddComponent<Camera>();
            c.tag = "MainCamera";
            c.AddComponent<AudioListener>();
        }
        _cam.orthographic = false;
        _cam.clearFlags = CameraClearFlags.SolidColor;
        _cam.backgroundColor = new Color(0.45f, 0.62f, 0.78f);
        _cam.transform.rotation = Quaternion.Euler(70f, 0f, 0f);
    }

    void Update()
    {
        if (mode == Mode.Login || mode == Mode.Fail) return;
        FollowCam();
        if (mode == Mode.Port || _worldPaused) return;

        weather.Tick(Time.deltaTime);
        HandleSailing(Time.deltaTime);
        DrainSupplies(Time.deltaTime);
        if (ship.supplies <= 0) { Fail("补给空了"); return; }
        if (ship.hull <= 0) { Fail("船被打沉"); return; }

        if (mode == Mode.Sea)
        {
            TryAutoDock();
            TryIsland();
            MaybeSpawnPirate(Time.deltaTime);
        }
        else if (mode == Mode.Combat)
            TickCombat(Time.deltaTime);
    }

    void FollowCam()
    {
        if (_cam == null || _shipTf == null) return;
        var t = _shipTf.position + new Vector3(0f, 28f, -12f);
        _cam.transform.position = Vector3.Lerp(_cam.transform.position, t, 8f * Time.deltaTime);
        _cam.transform.LookAt(_shipTf.position);
    }

    void HandleSailing(float dt)
    {
        ReadTouches();
        if (_joystickHeld && _autoPort != null)
        {
            _autoPort = null; // 推摇杆改回手动，取消按钮消失
        }
        if (!string.IsNullOrEmpty(_autoPort) && WorldCatalog.PortPositions.TryGetValue(_autoPort, out var dest))
        {
            var to = dest - _shipTf.position;
            to.y = 0;
            if (to.sqrMagnitude > 0.01f)
            {
                var look = Quaternion.LookRotation(to.normalized, Vector3.up);
                _shipTf.rotation = Quaternion.RotateTowards(_shipTf.rotation, look, GameBalance.TurnDegreesPerSecond * dt);
            }
            _speed = Mathf.MoveTowards(_speed, GameBalance.MaxSpeed * 0.7f, GameBalance.AccelPerSecond * dt);
        }
        else
        {
            if (_joystickHeld)
            {
                float yaw = _joy.x * GameBalance.TurnDegreesPerSecond * dt;
                _shipTf.Rotate(0f, yaw, 0f, Space.World);
            }
            if (_holdAccel) _speed += GameBalance.AccelPerSecond * dt;
            else if (_holdDecel) _speed -= GameBalance.DecelPerSecond * dt;
            else _speed = Mathf.MoveTowards(_speed, 0f, GameBalance.CoastPerSecond * dt); // 松手滑行
        }
        _speed = Mathf.Clamp(_speed, 0f, GameBalance.MaxSpeed);
        float mul = weather.SpeedMul; // 风只改航速，不吹偏
        _shipTf.position += _shipTf.forward * (_speed * mul * dt);
        ship.x = _shipTf.position.x;
        ship.z = _shipTf.position.z;
        ship.headingY = _shipTf.eulerAngles.y;
    }

    void ReadTouches()
    {
        // 运行时由 OnGUI 按钮设置 _holdAccel/_holdDecel/_joy
        if (Application.isEditor)
        {
            if (Input.GetKey(KeyCode.W)) _holdAccel = true;
            if (Input.GetKey(KeyCode.S)) _holdDecel = true;
            if (Input.GetKey(KeyCode.A) || Input.GetKey(KeyCode.D))
            {
                _joystickHeld = true;
                _joy = new Vector2((Input.GetKey(KeyCode.D) ? 1f : 0f) + (Input.GetKey(KeyCode.A) ? -1f : 0f), 0f);
            }
        }
    }

    void DrainSupplies(float dt)
    {
        float drain = GameBalance.SupplyDrainPerSecondAtSea
                      + ship.crew * GameBalance.SupplyDrainPerCrewPerSecond;
        ship.supplies -= drain * dt;
        if (ship.supplies < 0) ship.supplies = 0;
    }

    void TryAutoDock()
    {
        foreach (var kv in WorldCatalog.PortPositions)
        {
            if (Vector3.Distance(_shipTf.position, kv.Value) <= GameBalance.PortDockRadius)
            {
                EnterPort(kv.Key);
                return;
            }
        }
    }

    void TryIsland()
    {
        for (int i = 0; i < WorldCatalog.IslandPositions.Length; i++)
        {
            if (Vector3.Distance(_shipTf.position, WorldCatalog.IslandPositions[i]) <= GameBalance.IslandExploreRadius)
            {
                mode = Mode.Island;
                _worldPaused = true;
                _autoPort = null;
                _msg = "靠上" + WorldCatalog.IslandNames[i] + "，用菜单搜采。";
                return;
            }
        }
    }

    void MaybeSpawnPirate(float dt)
    {
        _pirateCheck += dt;
        if (_pirateCheck < GameBalance.PirateCheckInterval) return;
        _pirateCheck = 0f;
        if (Random.value > GameBalance.PirateChance) return;
        BeginCombat();
    }

    void BeginCombat()
    {
        mode = Mode.Combat;
        _locked = false;
        _playerReturnedFire = false;
        _pirateHull = 40f;
        _playerFireCd = 0f;
        _pirateFireCd = 0f;
        if (_autoPort != null)
        {
            _autoPort = null; // 自动开遇海盗停战，改回手动
            _msg = "遇海盗，自动驶向已取消。";
        }
        if (_pirate != null) Destroy(_pirate.gameObject);
        var go = GameObject.CreatePrimitive(PrimitiveType.Cube);
        go.name = "Pirate";
        go.GetComponent<Renderer>().material.color = new Color(0.45f, 0.12f, 0.12f);
        var side = _shipTf.right * (Random.value < 0.5f ? 12f : -12f);
        go.transform.position = _shipTf.position + _shipTf.forward * 10f + side;
        go.transform.localScale = new Vector3(1.2f, 0.6f, 2f);
        _pirate = go.transform;
    }

    void TickCombat(float dt)
    {
        if (_pirate == null) { mode = Mode.Sea; return; }
        var toPlayer = _shipTf.position - _pirate.position;
        toPlayer.y = 0;
        float dist = toPlayer.magnitude;

        // 默认可就地打；还击会追得紧
        float chase = _playerReturnedFire ? GameBalance.PirateChaseMulIfReturnFire : 0.35f;
        if (dist > 3f)
        {
            _pirate.position += toPlayer.normalized * (GameBalance.BaseSpeed * chase * dt);
            if (toPlayer.sqrMagnitude > 0.01f)
                _pirate.rotation = Quaternion.LookRotation(toPlayer.normalized);
        }

        if (_locked)
        {
            _playerFireCd -= dt;
            if (_playerFireCd <= 0f)
            {
                _playerFireCd = GameBalance.PlayerFireInterval;
                _playerReturnedFire = true;
                float dmg = ship.cannonDamage + ship.crew * 0.4f; // 人数加成火力；未满员按实际人数
                _pirateHull -= dmg;
                _msg = "连射命中，海盗耐久 " + Mathf.CeilToInt(_pirateHull);
                if (_pirateHull <= 0f) { WinCombat(); return; }
            }
        }

        if (dist <= GameBalance.CombatLockRange)
        {
            _pirateFireCd -= dt;
            if (_pirateFireCd <= 0f)
            {
                _pirateFireCd = GameBalance.PirateFireInterval;
                ship.hull -= Mathf.RoundToInt(GameBalance.PirateDamage);
                if (ship.hull <= 0) { Fail("船被打沉"); return; }
            }
        }

        if (dist >= GameBalance.EscapeRange)
        {
            // 开出战斗范围才算逃跑；无额外惩罚
            EndCombat(false);
            _msg = "已驶出交战范围，海盗停火。";
        }
    }

    void WinCombat()
    {
        int sil = Random.Range(GameBalance.DropSilverMin, GameBalance.DropSilverMax + 1);
        ship.silver += sil;
        string lootGood = WorldCatalog.TradeGoods[Random.Range(0, WorldCatalog.TradeGoods.Length)];
        int q = Random.Range(GameBalance.DropGoodLootMin, GameBalance.DropGoodLootMax + 1);
        if (ship.cargo.UsedSlots() + q <= ship.cargoCap)
            ship.cargo.AddTrade(lootGood, q);
        if (Random.value < GameBalance.RarePartAsCargoChance && ship.cargo.UsedSlots() < ship.cargoCap)
            ship.cargo.AddTrade("缴获部件(当货卖)", 1); // v0.1 缴获当货物卖掉
        _msg = "打赢。银子 +" + sil + "，货 +" + lootGood;
        EndCombat(true);
    }

    void EndCombat(bool sunk)
    {
        if (_pirate != null) Destroy(_pirate.gameObject);
        _pirate = null;
        _locked = false;
        _playerReturnedFire = false;
        _autoPort = null;
        mode = Mode.Sea;
    }

    void EnterPort(string port)
    {
        mode = Mode.Port;
        _worldPaused = true; // 进港世界暂停：不扣补给、不刷海盗
        ship.dockedPort = port;
        _autoPort = null;
        _speed = 0f;
        ApplyDebtInterest();
        SaveSystem.SaveShip(user, ship); // 靠港自动存
        _msg = "靠泊 " + port + "（已自动存档）。欠债已计息。";
        var p = WorldCatalog.PortPositions[port];
        _shipTf.position = p + new Vector3(0, 0.4f, 0);
    }

    void ApplyDebtInterest()
    {
        if (ship.debt <= 0) return;
        ship.debt += Mathf.CeilToInt(ship.debt * GameBalance.DebtInterestPerPortCall);
    }

    public void LeavePort()
    {
        var p = WorldCatalog.PortPositions[ship.dockedPort];
        // 出港位置：文档开放问题未答。占位：港北外一点。
        _shipTf.position = p + new Vector3(0, 0.4f, 6f);
        _worldPaused = false;
        mode = Mode.Sea;
        _pirateCheck = 0f;
    }

    void Fail(string why)
    {
        mode = Mode.Fail;
        _worldPaused = true;
        _msg = why + "。读档重来。";
        if (_pirate != null) Destroy(_pirate.gameObject);
    }

    void ReloadSave()
    {
        var s = SaveSystem.LoadShip(user);
        if (s == null) { _msg = "没有存档"; return; }
        ship = s;
        ApplyShipTransform();
        mode = Mode.Port;
        _worldPaused = true;
        _msg = "已读档，仍在 " + ship.dockedPort;
    }

    void ApplyShipTransform()
    {
        _shipTf.position = new Vector3(ship.x, 0.4f, ship.z);
        _shipTf.rotation = Quaternion.Euler(0f, ship.headingY, 0f);
        _speed = 0f;
    }

    int FreeCargo => ship.cargoCap - ship.cargo.UsedSlots();

    void Buy(string good, int n)
    {
        int price = WorldCatalog.Price(good, ship.dockedPort) * n;
        if (n <= 0 || price > ship.silver) { _msg = "银子不够"; return; }
        if (n > FreeCargo) { _msg = "货舱满了，先卖或丢掉"; return; }
        ship.silver -= price;
        ship.cargo.AddTrade(good, n);
        _msg = "买入 " + good + " x" + n;
    }

    void SellTrade(string good, int n)
    {
        int price = WorldCatalog.Price(good, ship.dockedPort) * n;
        if (!ship.cargo.RemoveTrade(good, n)) { _msg = "没有这么多货"; return; }
        ship.silver += price;
        _msg = "卖出 " + good + " +" + price;
    }

    void DumpAtSea(string kind, string id)
    {
        if (kind == "trade")
        {
            if (!ship.cargo.RemoveTrade(id, 1)) { _msg = "没有这货"; return; }
        }
        else if (kind == "beast")
        {
            if (!ship.cargo.beasts.Remove(id)) return;
        }
        else if (kind == "herb")
        {
            if (!ship.cargo.herbs.Remove(id)) return;
        }
        _msg = "已丢弃（没了）: " + id;
    }

    void OnGUI()
    {
        GUI.skin.button.fontSize = 16;
        GUI.skin.label.fontSize = 16;
        GUI.skin.textField.fontSize = 16;
        float w = Screen.width, h = Screen.height;
        GUI.Label(new Rect(8, 8, w - 16, 28), _msg ?? "");

        if (_showCodex) { DrawCodex(); return; }

        if (mode == Mode.Login) { DrawLogin(); return; }
        if (mode == Mode.Fail) { DrawFail(); return; }

        DrawHud();
        if (GUI.Button(new Rect(w - 120, 8, 110, 36), "图鉴")) _showCodex = true;

        if (mode == Mode.Port) DrawPort();
        else if (mode == Mode.Island) DrawIsland();
        else
        {
            DrawSailingControls();
            if (mode == Mode.Combat) DrawCombat();
            DrawMinimap();
        }
    }

    void DrawLogin()
    {
        var r = new Rect(Screen.width * 0.25f, Screen.height * 0.25f, Screen.width * 0.5f, 280);
        GUI.Box(r, "本机登录（不联网）");
        GUI.Label(new Rect(r.x + 20, r.y + 40, 80, 28), "用户");
        _u = GUI.TextField(new Rect(r.x + 100, r.y + 40, r.width - 140, 28), _u);
        GUI.Label(new Rect(r.x + 20, r.y + 80, 80, 28), "密码");
        _p = GUI.PasswordField(new Rect(r.x + 100, r.y + 80, r.width - 140, 28), _p, '*');
        if (GUI.Button(new Rect(r.x + 20, r.y + 130, 140, 40), "登录"))
        {
            if (SaveSystem.TryLogin(_u, _p, out var e))
            {
                user = _u;
                if (SaveSystem.HasSave(user))
                {
                    ship = SaveSystem.LoadShip(user);
                    ApplyShipTransform();
                    mode = Mode.Port;
                    _worldPaused = true;
                    _msg = "读档成功 " + ship.dockedPort;
                }
                else
                {
                    ship = SaveSystem.NewGame();
                    ApplyShipTransform();
                    SaveSystem.SaveShip(user, ship);
                    mode = Mode.Port;
                    _worldPaused = true;
                    _msg = "新档，广州。";
                }
            }
            else _msg = e;
        }
        if (GUI.Button(new Rect(r.x + 180, r.y + 130, 140, 40), "注册并登录"))
        {
            if (SaveSystem.TryRegister(_u, _p, out var e))
            {
                user = _u;
                ship = SaveSystem.NewGame();
                ApplyShipTransform();
                SaveSystem.SaveShip(user, ship);
                mode = Mode.Port;
                _worldPaused = true;
                _msg = "注册完成，广州起航。";
            }
            else _msg = e;
        }
    }

    void DrawFail()
    {
        GUI.Box(new Rect(40, 80, Screen.width - 80, 160), "失败");
        if (GUI.Button(new Rect(60, 140, 200, 50), "读档重来")) ReloadSave();
    }

    void DrawHud()
    {
        string t = string.Format(
            "{0} 银{1} 债{2} 补给{3:0} 耐久{4} 舱{5}/{6} 人{7}/{8} 炮伤{9} {10} {11}",
            mode, ship.silver, ship.debt, ship.supplies, ship.hull,
            ship.cargo.UsedSlots(), ship.cargoCap, ship.crew, ship.crewCap,
            ship.cannonDamage, weather.Label(),
            string.IsNullOrEmpty(_autoPort) ? "手动" : ("驶向" + _autoPort));
        GUI.Label(new Rect(8, 36, Screen.width - 16, 28), t);
    }

    void DrawSailingControls()
    {
        float h = Screen.height, w = Screen.width;
        var joy = new Rect(24, h - 180, 160, 160);
        GUI.Box(joy, "转向");
        // 简易摇杆：点盒子左右
        var e = Event.current;
        _joystickHeld = false;
        _joy = Vector2.zero;
        if ((e.type == EventType.MouseDown || e.type == EventType.MouseDrag) && joy.Contains(e.mousePosition))
        {
            _joystickHeld = true;
            float x = (e.mousePosition.x - joy.center.x) / (joy.width * 0.5f);
            _joy = new Vector2(Mathf.Clamp(x, -1f, 1f), 0f);
        }
        _holdAccel = GUI.RepeatButton(new Rect(w - 150, h - 170, 130, 70), "加速(按住)");
        _holdDecel = GUI.RepeatButton(new Rect(w - 150, h - 90, 130, 70), "减速(按住)");

        if (!string.IsNullOrEmpty(_autoPort))
        {
            if (GUI.Button(new Rect(w * 0.5f - 90, h - 50, 180, 40), "取消自动驶向"))
                _autoPort = null;
        }

        DrawDumpAtSea();
    }

    void DrawDumpAtSea()
    {
        if (mode != Mode.Sea && mode != Mode.Combat) return;
        if (GUI.Button(new Rect(200, Screen.height - 50, 100, 40), "丢货"))
        {
            if (ship.cargo.tradeIds.Count > 0)
                DumpAtSea("trade", ship.cargo.tradeIds[0]);
            else if (ship.cargo.beasts.Count > 0)
                DumpAtSea("beast", ship.cargo.beasts[0]);
            else if (ship.cargo.herbs.Count > 0)
                DumpAtSea("herb", ship.cargo.herbs[0]);
            else _msg = "没有可丢的货";
        }
        // 列表选择丢掉哪一种
        float x = 310;
        for (int i = 0; i < ship.cargo.tradeIds.Count && i < 6; i++)
        {
            if (GUI.Button(new Rect(x, Screen.height - 50, 90, 40), "丢" + ship.cargo.tradeIds[i]))
                DumpAtSea("trade", ship.cargo.tradeIds[i]);
            x += 94;
        }
    }

    void DrawCombat()
    {
        if (_pirate == null) return;
        if (!_locked)
        {
            if (GUI.Button(new Rect(Screen.width * 0.5f - 80, 80, 160, 44), "锁定敌船"))
                _locked = true;
        }
        else
        {
            GUI.Label(new Rect(Screen.width * 0.5f - 80, 72, 200, 24), "已锁定，自动连射");
            if (GUI.Button(new Rect(Screen.width * 0.5f - 80, 98, 160, 40), "取消锁定"))
                _locked = false;
        }
        // 点选 3D 船：点击海盗
        if (Event.current.type == EventType.MouseDown && _cam != null)
        {
            var ray = _cam.ScreenPointToRay(Input.mousePosition);
            if (Physics.Raycast(ray, out var hit) && hit.transform == _pirate)
                _locked = true;
        }
    }

    void DrawMinimap()
    {
        float s = 140;
        var box = new Rect(8, 70, s, s);
        if (_showFullMap) box = new Rect(Screen.width * 0.15f, Screen.height * 0.12f, Screen.width * 0.7f, Screen.height * 0.7f);
        GUI.Box(box, weather.chartObscured && !_showFullMap ? "海图（雨雾不清）" : (_showFullMap ? "全图（船仍在开）" : "小地图"));
        if (weather.chartObscured && !_showFullMap)
        {
            GUI.Label(new Rect(box.x + 10, box.y + 40, box.width - 20, 60), "雨雾：海图看不清");
        }
        else
        {
            DrawMapContents(box);
        }
        if (!_showFullMap)
        {
            if (GUI.Button(box, GUIContent.none)) _showFullMap = true; // 点小地图放大
        }
        else
        {
            if (GUI.Button(new Rect(box.xMax - 90, box.y + 8, 80, 32), "关闭全图")) _showFullMap = false;
        }
    }

    void DrawMapContents(Rect box)
    {
        // 只显示港和岛，不显示海盗
        float minX = -50, maxX = 30, minZ = -70, maxZ = 55;
        Vector2 Map(Vector3 p)
        {
            float nx = (p.x - minX) / (maxX - minX);
            float nz = (p.z - minZ) / (maxZ - minZ);
            return new Vector2(box.x + nx * box.width, box.yMax - nz * box.height);
        }
        foreach (var kv in WorldCatalog.PortPositions)
        {
            var m = Map(kv.Value);
            var r = new Rect(m.x - 8, m.y - 8, 16, 16);
            if (GUI.Button(r, "●"))
            {
                if (_showFullMap)
                {
                    _autoPort = kv.Key; // 全图点港口自动驶向
                    _msg = "自动驶向 " + kv.Key;
                    _showFullMap = false;
                }
            }
            GUI.Label(new Rect(m.x + 8, m.y - 10, 48, 20), kv.Key);
        }
        foreach (var ip in WorldCatalog.IslandPositions)
        {
            var m = Map(ip);
            GUI.Box(new Rect(m.x - 5, m.y - 5, 10, 10), "");
        }
        if (_shipTf != null)
        {
            var m = Map(_shipTf.position);
            GUI.Box(new Rect(m.x - 4, m.y - 4, 8, 8), "船");
        }
    }

    void DrawPort()
    {
        var box = new Rect(8, 100, Screen.width - 16, Screen.height - 110);
        GUI.Box(box, "港口 · " + ship.dockedPort + "（世界暂停）");
        _scroll = GUI.BeginScrollView(new Rect(box.x + 8, box.y + 28, box.width - 16, box.height - 36), _scroll,
            new Rect(0, 0, box.width - 40, 920));
        float y = 4;
        if (GUI.Button(new Rect(0, y, 160, 36), "出港")) LeavePort();
        y += 44;

        GUI.Label(new Rect(0, y, 800, 24), "补给只能回港按缺口花钱；没钱可借债。回港手动还债，不还能离港。");
        y += 28;
        int miss = GameBalance.MaxSupplies - Mathf.FloorToInt(ship.supplies);
        GUI.Label(new Rect(0, y, 400, 24), "补给缺口 " + miss + "  单价占位 " + GameBalance.SupplySilverPerPoint);
        y += 28;
        if (GUI.Button(new Rect(0, y, 180, 36), "补满补给"))
        {
            int cost = miss * GameBalance.SupplySilverPerPoint;
            if (cost <= 0) _msg = "补给已满";
            else if (ship.silver >= cost)
            {
                ship.silver -= cost;
                ship.supplies = GameBalance.MaxSupplies;
                _msg = "补给已补，花费 " + cost;
            }
            else _msg = "银子不够，可借债";
        }
        if (GUI.Button(new Rect(190, y, 160, 36), "借债+" + GameBalance.LoanStep))
        {
            ship.debt += GameBalance.LoanStep;
            ship.silver += GameBalance.LoanStep;
            _msg = "借债 " + GameBalance.LoanStep;
        }
        if (GUI.Button(new Rect(360, y, 160, 36), "还债"))
        {
            int pay = Mathf.Min(ship.silver, ship.debt);
            ship.silver -= pay;
            ship.debt -= pay;
            _msg = "还债 " + pay;
        }
        y += 44;

        int hullMiss = GameBalance.MaxHull - ship.hull;
        if (GUI.Button(new Rect(0, y, 220, 36), "立刻修船 " + hullMiss * GameBalance.RepairSilverPerHull))
        {
            int cost = hullMiss * GameBalance.RepairSilverPerHull;
            if (ship.silver >= cost)
            {
                ship.silver -= cost;
                ship.hull = GameBalance.MaxHull;
                _msg = "船已修好";
            }
            else _msg = "修船银子不够";
        }
        y += 44;
        if (GUI.Button(new Rect(0, y, 240, 36), "升级货舱+" + GameBalance.CargoUpgradeAmount))
        {
            if (ship.silver >= GameBalance.CargoUpgradeCost)
            {
                ship.silver -= GameBalance.CargoUpgradeCost;
                ship.cargoCap += GameBalance.CargoUpgradeAmount;
                _msg = "共用货舱加大";
            }
            else _msg = "银子不够";
        }
        if (GUI.Button(new Rect(250, y, 220, 36), "升级炮火(只加伤害)"))
        {
            if (ship.silver >= GameBalance.CannonUpgradeCost)
            {
                ship.silver -= GameBalance.CannonUpgradeCost;
                ship.cannonLevel++;
                ship.cannonDamage += GameBalance.CannonUpgradeDamage;
                _msg = "炮伤 " + ship.cannonDamage;
            }
            else _msg = "银子不够";
        }
        y += 44;
        if (GUI.Button(new Rect(0, y, 220, 36), "提高人数上限"))
        {
            if (ship.silver >= GameBalance.CrewCapUpgradeCost)
            {
                ship.silver -= GameBalance.CrewCapUpgradeCost;
                ship.crewCap += GameBalance.CrewCapUpgradeAmount;
                _msg = "人数上限 " + ship.crewCap;
            }
            else _msg = "银子不够";
        }
        if (GUI.Button(new Rect(230, y, 220, 36), "雇人立刻上船"))
        {
            if (ship.crew >= ship.crewCap) _msg = "已满员";
            else if (ship.silver >= GameBalance.HireCrewCost)
            {
                ship.silver -= GameBalance.HireCrewCost;
                ship.crew++;
                _msg = "船员 " + ship.crew;
            }
            else _msg = "银子不够";
        }
        y += 50;

        GUI.Label(new Rect(0, y, 700, 24), "本港价格（点开一种货看各港固定差价）  舱余 " + FreeCargo);
        y += 28;
        foreach (var g in WorldCatalog.TradeGoods)
        {
            int pr = WorldCatalog.Price(g, ship.dockedPort);
            int have = 0;
            int ix = ship.cargo.tradeIds.IndexOf(g);
            if (ix >= 0) have = ship.cargo.tradeQty[ix];
            GUI.Label(new Rect(0, y, 280, 28), g + " 本港" + pr + " 持有" + have);
            if (GUI.Button(new Rect(290, y, 50, 28), "买")) Buy(g, 1);
            if (GUI.Button(new Rect(345, y, 50, 28), "卖")) SellTrade(g, 1);
            if (GUI.Button(new Rect(400, y, 80, 28), "各港价")) _selectedGood = g;
            y += 32;
        }
        if (_selectedGood != null)
        {
            GUI.Label(new Rect(0, y, 700, 24), _selectedGood + " 各港固定价：");
            y += 24;
            string line = "";
            foreach (var p in WorldCatalog.Ports)
                line += p + WorldCatalog.Price(_selectedGood, p) + "  ";
            GUI.Label(new Rect(0, y, 800, 48), line);
            y += 52;
        }

        GUI.Label(new Rect(0, y, 400, 24), "异兽/草药（一期草药只卖钱）");
        y += 28;
        for (int i = 0; i < ship.cargo.beasts.Count; i++)
        {
            var b = ship.cargo.beasts[i];
            if (GUI.Button(new Rect(0, y, 360, 28), "卖异兽 " + b + " +" + WorldCatalog.BeastSellPrice))
            {
                ship.cargo.beasts.RemoveAt(i);
                ship.silver += WorldCatalog.BeastSellPrice;
                break;
            }
            y += 32;
        }
        for (int i = 0; i < ship.cargo.herbs.Count; i++)
        {
            var b = ship.cargo.herbs[i];
            if (GUI.Button(new Rect(0, y, 360, 28), "卖草药 " + b + " +" + WorldCatalog.HerbSellPrice))
            {
                ship.cargo.herbs.RemoveAt(i);
                ship.silver += WorldCatalog.HerbSellPrice;
                break;
            }
            y += 32;
        }
        GUI.EndScrollView();
    }

    void DrawIsland()
    {
        GUI.Box(new Rect(40, 80, Screen.width - 80, 260), "登岛搜采（菜单，不上岛走）");
        if (GUI.Button(new Rect(60, 130, 220, 44), "搜采异兽"))
        {
            if (FreeCargo <= 0) _msg = "货舱满了";
            else
            {
                var b = WorldCatalog.Beasts[Random.Range(0, WorldCatalog.Beasts.Length)];
                ship.cargo.beasts.Add(b);
                if (!ship.seenBeasts.Contains(b)) ship.seenBeasts.Add(b);
                _msg = "发现 " + b;
            }
        }
        if (GUI.Button(new Rect(300, 130, 220, 44), "搜采草药"))
        {
            if (FreeCargo <= 0) _msg = "货舱满了";
            else
            {
                var b = WorldCatalog.Herbs[Random.Range(0, WorldCatalog.Herbs.Length)];
                ship.cargo.herbs.Add(b);
                if (!ship.seenHerbs.Contains(b)) ship.seenHerbs.Add(b);
                _msg = "发现 " + b;
            }
        }
        if (GUI.Button(new Rect(60, 190, 220, 44), "离岛继续开"))
        {
            // 把船推离岛半径，避免立刻再进
            _shipTf.position += _shipTf.forward * 6f;
            _worldPaused = false;
            mode = Mode.Sea;
        }
    }

    void DrawCodex()
    {
        GUI.Box(new Rect(20, 20, Screen.width - 40, Screen.height - 40), "图鉴（随时可开）");
        if (GUI.Button(new Rect(Screen.width - 120, 28, 90, 36), "关闭")) _showCodex = false;
        float y = 70;
        GUI.Label(new Rect(40, y, 400, 24), "异兽");
        y += 28;
        foreach (var b in WorldCatalog.Beasts)
        {
            bool seen = ship != null && ship.seenBeasts.Contains(b);
            GUI.Label(new Rect(40, y, 500, 24), seen ? b : "？？？");
            y += 24;
        }
        GUI.Label(new Rect(40, y, 400, 24), "草药（一期只卖，不使用）");
        y += 28;
        foreach (var b in WorldCatalog.Herbs)
        {
            bool seen = ship != null && ship.seenHerbs.Contains(b);
            GUI.Label(new Rect(40, y, 500, 24), seen ? b : "？？？");
            y += 24;
        }
    }
}
