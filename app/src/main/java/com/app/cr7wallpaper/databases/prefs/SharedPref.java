package com.app.cr7wallpaper.databases.prefs;

import android.content.Context;
import android.content.SharedPreferences;

import com.app.cr7wallpaper.Config;
import com.app.cr7wallpaper.utils.Constant;

public class SharedPref {

    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    public SharedPref(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveGif(String path, String gif_name) {
        editor.putString("path", path);
        editor.putString("gif_name", gif_name);
        editor.apply();
    }

    public String getPath() {
        return sharedPreferences.getString("path", "0");
    }

    public String getGifName() {
        return sharedPreferences.getString("gif_name", "0");
    }

    public Integer getDisplayPosition(int default_value) {
        return sharedPreferences.getInt("display_position", default_value);
    }

    public void updateDisplayPosition(int position) {
        editor.putInt("display_position", position);
        editor.apply();
    }

    public Integer getWallpaperColumns() {
        return sharedPreferences.getInt("wallpaper_columns", Config.DEFAULT_WALLPAPER_COLUMN);
    }

    public void updateWallpaperColumns(int columns) {
        editor.putInt("wallpaper_columns", columns);
        editor.apply();
    }

    public void setDefaultSortWallpaper() {
        editor.putInt("sort_act", Constant.SORT_RECENT);
        editor.apply();
    }

    public Integer getCurrentSortWallpaper() {
        return sharedPreferences.getInt("sort_act", 0);
    }

    public void updateSortWallpaper(int position) {
        editor.putInt("sort_act", position);
        editor.apply();
    }

    public Boolean getIsDarkTheme() {
        return sharedPreferences.getBoolean("theme", Config.DEFAULT_THEME);
    }

    public void setIsDarkTheme(Boolean isDarkTheme) {
        editor.putBoolean("theme", isDarkTheme);
        editor.apply();
    }

    public Boolean getIsNotification() {
        return sharedPreferences.getBoolean("noti", true);
    }

    public void setIsNotification(Boolean isNotification) {
        editor.putBoolean("noti", isNotification);
        editor.apply();
    }

    public Integer getAppOpenToken() {
        return sharedPreferences.getInt("app_open_token", 0);
    }

    public void updateAppOpenToken(int value) {
        editor.putInt("app_open_token", value);
        editor.apply();
    }

    public Integer getInAppReviewToken() {
        return sharedPreferences.getInt("in_app_review_token", 0);
    }

    public void updateInAppReviewToken(int value) {
        editor.putInt("in_app_review_token", value);
        editor.apply();
    }

}
