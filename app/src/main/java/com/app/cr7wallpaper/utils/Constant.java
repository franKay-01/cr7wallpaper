package com.app.cr7wallpaper.utils;

import com.app.cr7wallpaper.Config;

public class Constant {

    public static final int DELAY_TIME = 0;
    public static final int DELAY_SET = 2500;
    public static final int DELAY_ADS = 1000;

    public static final int LOAD_MORE_2_COLUMNS = 20;
    public static final int LOAD_MORE_3_COLUMNS = 24;
    public static final int NATIVE_AD_INDEX_2_COLUMNS = 6;
    public static final int NATIVE_AD_INDEX_3_COLUMNS = 9;

    public static final String POSITION = "POSITION_ID";
    public static final String EXTRA_OBJC = "key.EXTRA_OBJC";
    public static final String BUNDLE = "key.BUNDLE";
    public static final String ARRAY_LIST = "key.ARRAY_LIST";
    public static final String BASE_IMAGE_URL = Config.ADMIN_PANEL_URL + "/upload/";
    public static final String NOTIFICATION_CHANNEL_NAME = "wallpaper_channel_01";

    public static final int THUMBNAIL_WIDTH = 250;
    public static final int THUMBNAIL_HEIGHT = 375;

    public static final boolean THEME_LIGHT = false;
    public static final boolean THEME_DARK = true;

    //do not make any changes to the code below
    public static final String FILTER_ALL = "g.image_extension != 'all'";
    public static final String FILTER_WALLPAPER = "g.image_extension != 'image/gif'";
    public static final String FILTER_LIVE = "g.image_extension = 'image/gif'";

    public static final String ORDER_RECENT = "ORDER BY g.id DESC";
    public static final String ORDER_FEATURED = "AND g.featured = 'yes' ORDER BY g.last_update DESC";
    public static final String ORDER_POPULAR = "ORDER BY g.view_count DESC";
    public static final String ORDER_RANDOM = "ORDER BY RAND()";
    public static final String ORDER_LIVE = "ORDER BY g.id DESC ";

    public static final int DISPLAY_WALLPAPER_RECTANGLE = 1;
    public static final int DISPLAY_WALLPAPER_SQUARE = 2;
    public static final int DISPLAY_WALLPAPER_DYNAMIC = 3;

    public static final int SORT_RECENT = 0;
    public static final int SORT_FEATURED = 1;
    public static final int SORT_POPULAR = 2;
    public static final int SORT_RANDOM = 3;
    public static final int SORT_LIVE = 4;

    public static final int CATEGORY_COLUMNS = 1;
    public static final int WALLPAPER_TWO_COLUMNS = 2;
    public static final int WALLPAPER_THREE_COLUMNS = 3;

    public static final String DOWNLOAD = "download";
    public static final String SHARE = "share";
    public static final String SET_WITH = "setWith";
    public static final String SET_GIF = "setGif";

    public static final String HOME_SCREEN = "home_screen";
    public static final String LOCK_SCREEN = "lock_screen";
    public static final String BOTH = "both";

    public static String gifName = "";
    public static String gifPath = "";

    //startapp native ad image parameters
    public static final int STARTAPP_IMAGE_XSMALL = 1; //for image size 100px X 100px
    public static final int STARTAPP_IMAGE_SMALL = 2; //for image size 150px X 150px
    public static final int STARTAPP_IMAGE_MEDIUM = 3; //for image size 340px X 340px
    public static final int STARTAPP_IMAGE_LARGE = 4; //for image size 1200px X 628px
    public static final int STARTAPP_IMAGE_RECTANGLE_MEDIUM = 6; //for image size 1200px X 628px

    //unity banner ad size
    public static final int UNITY_ADS_BANNER_WIDTH = 320;
    public static final int UNITY_ADS_BANNER_HEIGHT = 50;

}
