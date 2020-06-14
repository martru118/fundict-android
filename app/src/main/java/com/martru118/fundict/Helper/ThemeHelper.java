package com.martru118.fundict.Helper;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences class.
 * Saves and loads night mode settings for the user.
 */
public class ThemeHelper {
    private SharedPreferences themePreferences;

    public ThemeHelper(Context context) {
        themePreferences = context.getSharedPreferences("app_theme", Context.MODE_PRIVATE);
    }

    /**
     * Sets the night mode state (day mode = false; night mode = true).
     * @param state -- If the user wants to change to night mode.
     */
    public void setNightModeState(Boolean state) {
        SharedPreferences.Editor editor = themePreferences.edit();
        editor.putBoolean("NightMode", state);
        editor.apply();
    }

    /**
     * @return The night mode setting for the user.
     */
    public boolean loadNightMode() {
        return themePreferences.getBoolean("NightMode", false);
    }
}
