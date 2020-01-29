package com.martru118.fundict.Helper;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemeHelper {
    private SharedPreferences themePreferences;

    public ThemeHelper(Context context) {
        themePreferences = context.getSharedPreferences("app_theme", Context.MODE_PRIVATE);
    }

    public void setNightModeState(Boolean state) {
        SharedPreferences.Editor editor = themePreferences.edit();
        editor.putBoolean("NightMode", state);
        editor.apply();
    }

    public Boolean loadNightMode() {
        return themePreferences.getBoolean("NightMode", false);
    }
}
