using UnityEngine;

public class WeatherSystem
{
    public enum Wind { Fair, Head, Side }
    public Wind wind = Wind.Side;
    public bool chartObscured; // 雨或雾：海图看不清
    float windT, weatherT;

    public float SpeedMul
    {
        get
        {
            if (wind == Wind.Fair) return GameBalance.WindFairMul;
            if (wind == Wind.Head) return GameBalance.WindHeadMul;
            return 1f;
        }
    }

    public void Tick(float dt)
    {
        windT -= dt;
        weatherT -= dt;
        if (windT <= 0f)
        {
            wind = (Wind)Random.Range(0, 3);
            windT = GameBalance.WindChangeSeconds + Random.Range(0f, 20f);
        }
        if (weatherT <= 0f)
        {
            if (chartObscured)
            {
                chartObscured = false;
                weatherT = GameBalance.WeatherClearSecondsMin + Random.Range(0f, 20f);
            }
            else
            {
                chartObscured = Random.value < 0.35f;
                weatherT = chartObscured
                    ? GameBalance.WeatherFogRainSecondsMin + Random.Range(0f, 20f)
                    : GameBalance.WeatherClearSecondsMin + Random.Range(0f, 25f);
            }
        }
    }

    public string Label()
    {
        string w = wind == Wind.Fair ? "顺风" : wind == Wind.Head ? "逆风" : "侧风";
        string v = chartObscured ? "雨雾（海图不清）" : "晴";
        return w + " · " + v;
    }
}
