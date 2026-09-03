#if UNITY_EDITOR
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEngine;

public static class BuildAndroid
{
    [MenuItem("NanHai/Build Android APK")]
    public static void Build()
    {
        PlayerSettings.companyName = "Shipgame";
        PlayerSettings.productName = "NanHaiVoyage";
        PlayerSettings.SetApplicationIdentifier(BuildTargetGroup.Android, "com.shipgame.nanhai");
        PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel22;
        EditorUserBuildSettings.SwitchActiveBuildTarget(BuildTargetGroup.Android, BuildTarget.Android);
        var opts = new BuildPlayerOptions
        {
            scenes = new[] { "Assets/Scenes/Main.unity" },
            locationPathName = "Builds/NanHaiVoyage.apk",
            target = BuildTarget.Android,
            options = BuildOptions.None
        };
        var report = BuildPipeline.BuildPlayer(opts);
        Debug.Log("Build " + report.summary.result + " " + report.summary.outputPath);
    }
}
#endif
