package com.app.cr7wallpaper;

import com.app.cr7wallpaper.utils.Constant;

public class Config {

    //admin panel url
    public static final String ADMIN_PANEL_URL = "https://www.kideation.com/cr7wallpaper/";

    //default theme in first launch
    public static final boolean DEFAULT_THEME = Constant.THEME_LIGHT;

    //column count
    public static final int DEFAULT_WALLPAPER_COLUMN = Constant.WALLPAPER_TWO_COLUMNS;
    public static final int DEFAULT_CATEGORY_COLUMN = Constant.CATEGORY_COLUMNS;

    //UI Config
    public static final boolean ENABLE_CENTER_CROP_IN_DETAIL_WALLPAPER = true;
    public static final boolean ENABLE_DISPLAY_WALLPAPER_NAME = true;
    public static final boolean ENABLE_DISPLAY_WALLPAPER_CATEGORY = true;
    public static final boolean ENABLE_WALLPAPER_COUNT_ON_CATEGORY = true;

    //display grid wallpaper style
    public static final int DISPLAY_WALLPAPER = Constant.DISPLAY_WALLPAPER_RECTANGLE;

    //If simple mode enabled, scrollable tab layout on the main screen will be disabled
    public static final boolean ENABLE_SIMPLE_MODE = false;

    //set category as main screen
    public static final boolean DISPLAY_CATEGORY_AS_MAIN_SCREEN = false;

    //RTL Mode
    public static final boolean ENABLE_RTL_MODE = false;

    //GDPR Consent
    public static final boolean ENABLE_LEGACY_GDPR = false;

    //splash duration
    public static final int SPLASH_TIME = 1000;

}
