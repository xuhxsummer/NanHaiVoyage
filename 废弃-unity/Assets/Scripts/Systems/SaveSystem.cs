using System.IO;
using System.Security.Cryptography;
using System.Text;
using UnityEngine;

public static class SaveSystem
{
    static string Root => Path.Combine(Application.persistentDataPath, "nanhai");
    static string AccountsPath => Path.Combine(Root, "accounts.json");
    static string SavePath(string user) => Path.Combine(Root, "save_" + Sanitize(user) + ".json");

    static string Sanitize(string s)
    {
        var sb = new StringBuilder();
        foreach (var c in s)
            if (char.IsLetterOrDigit(c) || c == '_' || c > 127) sb.Append(c);
        return sb.Length == 0 ? "player" : sb.ToString();
    }

    public static string Hash(string pass)
    {
        using (var sha = SHA256.Create())
        {
            var b = sha.ComputeHash(Encoding.UTF8.GetBytes("nanhai|" + pass));
            var sb = new StringBuilder();
            foreach (var x in b) sb.Append(x.ToString("x2"));
            return sb.ToString();
        }
    }

    static void EnsureDir() { Directory.CreateDirectory(Root); }

    public static AccountDb LoadAccounts()
    {
        EnsureDir();
        if (!File.Exists(AccountsPath)) return new AccountDb();
        return JsonUtility.FromJson<AccountDb>(File.ReadAllText(AccountsPath)) ?? new AccountDb();
    }

    public static void StoreAccounts(AccountDb db)
    {
        EnsureDir();
        File.WriteAllText(AccountsPath, JsonUtility.ToJson(db, true));
    }

    public static bool TryRegister(string user, string pass, out string err)
    {
        err = null;
        if (string.IsNullOrWhiteSpace(user) || string.IsNullOrWhiteSpace(pass))
        { err = "用户名和密码不能空"; return false; }
        var db = LoadAccounts();
        foreach (var a in db.accounts)
            if (a.user == user) { err = "用户已存在"; return false; }
        db.accounts.Add(new AccountRecord { user = user, passHash = Hash(pass) });
        StoreAccounts(db);
        return true;
    }

    public static bool TryLogin(string user, string pass, out string err)
    {
        err = null;
        var db = LoadAccounts();
        foreach (var a in db.accounts)
        {
            if (a.user != user) continue;
            if (a.passHash != Hash(pass)) { err = "密码不对"; return false; }
            return true;
        }
        err = "没有这个用户";
        return false;
    }

    public static bool HasSave(string user) => File.Exists(SavePath(user));

    public static ShipSave LoadShip(string user)
    {
        var p = SavePath(user);
        if (!File.Exists(p)) return null;
        return JsonUtility.FromJson<ShipSave>(File.ReadAllText(p));
    }

    public static void SaveShip(string user, ShipSave s)
    {
        EnsureDir();
        File.WriteAllText(SavePath(user), JsonUtility.ToJson(s, true));
    }

    public static ShipSave NewGame()
    {
        var pos = WorldCatalog.PortPositions["广州"];
        return new ShipSave
        {
            silver = GameBalance.StartingSilver,
            debt = 0,
            supplies = GameBalance.StartingSupplies,
            hull = GameBalance.StartingHull,
            cargoCap = GameBalance.StartingCargoCapacity,
            crew = GameBalance.StartingCrew,
            crewCap = GameBalance.StartingCrewCap,
            cannonLevel = GameBalance.StartingCannonLevel,
            cannonDamage = GameBalance.StartingCannonDamage,
            dockedPort = "广州",
            x = pos.x,
            z = pos.z,
            headingY = 180f
        };
    }
}
