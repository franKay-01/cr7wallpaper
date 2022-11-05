package com.app.cr7wallpaper.activities;

import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.app.cr7wallpaper.BuildConfig;
import com.app.cr7wallpaper.Config;
import com.app.cr7wallpaper.R;
import com.app.cr7wallpaper.databases.prefs.AdsPref;
import com.app.cr7wallpaper.databases.prefs.SharedPref;
import com.app.cr7wallpaper.fragments.FragmentCategory;
import com.app.cr7wallpaper.fragments.FragmentFavorite;
import com.app.cr7wallpaper.fragments.FragmentSimpleWallpaper;
import com.app.cr7wallpaper.fragments.FragmentTabLayout;
import com.app.cr7wallpaper.utils.AdsManager;
import com.app.cr7wallpaper.utils.AppBarLayoutBehavior;
import com.app.cr7wallpaper.utils.Constant;
import com.app.cr7wallpaper.utils.RtlViewPager;
import com.app.cr7wallpaper.utils.Tools;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.LabelVisibilityMode;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.solodroid.ads.sdk.format.AppOpenAd;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";
    AppBarLayout appBarLayout;
    private ViewPager viewPager;
    private RtlViewPager viewPagerRTL;
    private long exitTime = 0;
    private CoordinatorLayout coordinatorLayout;
    MenuItem prevMenuItem;
    int pager_number = 3;
    private BottomNavigationView navigation;
    AdsPref adsPref;
    Toolbar toolbar;
    SharedPref sharedPref;
    RelativeLayout bg_line;
    private AppOpenAd appOpenAdManager;
    int numActivityRestarted = 0;
    Menu menu_sort;
    AdsManager adsManager;
    LinearLayout view_banner_ad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        sharedPref = new SharedPref(this);
        if (sharedPref.getIsDarkTheme()) {
            Tools.darkNavigation(this);
            getWindow().setBackgroundDrawableResource(R.drawable.bg_primary_dark);
        } else {
            Tools.lightNavigation(this);
            getWindow().setBackgroundDrawableResource(R.drawable.bg_primary_light);
        }
        adsPref = new AdsPref(this);
        if (Config.ENABLE_RTL_MODE) {
            setContentView(R.layout.activity_main_rtl);
        } else {
            setContentView(R.layout.activity_main);
        }

        adsManager = new AdsManager(this);
        adsManager.initializeAd();
        adsManager.updateConsentStatus();
        adsManager.loadBannerAd(adsPref.getBannerAdStatusHome());
        adsManager.loadInterstitialAd(adsPref.getInterstitialAdClickWallpaper(), adsPref.getInterstitialAdInterval());
        appOpenAdManager = ((MyApplication) getApplication()).getAppOpenAdManager();

        Tools.getRtlDirection(this);
        coordinatorLayout = findViewById(R.id.coordinatorLayout);
        appBarLayout = findViewById(R.id.appbarLayout);
        ((CoordinatorLayout.LayoutParams) appBarLayout.getLayoutParams()).setBehavior(new AppBarLayoutBehavior());

        setupToolbar();

        navigation = findViewById(R.id.navigation);
        navigation.getMenu().clear();
        if (Config.DISPLAY_CATEGORY_AS_MAIN_SCREEN) {
            navigation.inflateMenu(R.menu.navigation_category);
        } else {
            navigation.inflateMenu(R.menu.navigation_wallpaper);
        }

        bg_line = findViewById(R.id.bg_line);
        view_banner_ad = findViewById(R.id.view_banner_ad);
        if (sharedPref.getIsDarkTheme()) {
            navigation.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
            bg_line.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
            view_banner_ad.setBackgroundColor(getResources().getColor(R.color.colorToolbarDark));
        } else {
            navigation.setBackgroundColor(getResources().getColor(R.color.colorBackgroundLight));
            bg_line.setBackgroundColor(getResources().getColor(R.color.colorBackgroundLight));
            view_banner_ad.setBackgroundColor(getResources().getColor(R.color.colorBackgroundLight));
        }
        navigation.setLabelVisibilityMode(LabelVisibilityMode.LABEL_VISIBILITY_LABELED);
        initViewPager();
        getAdsLog();

        sharedPref.updateAppOpenToken(1);
        Log.d(TAG, "AppOpenAdsToken On start app open token : " + sharedPref.getAppOpenToken());
        Tools.notificationOpenHandler(this, getIntent());
        inAppReview();
    }

    public void showInterstitialAd() {
        adsManager.showInterstitialAd();
    }

    public void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setTitle(getString(R.string.app_name));
        }

        if (!sharedPref.getIsDarkTheme()) {
            toolbar.setPopupTheme(R.style.ThemeOverlay_AppCompat_Light);
        } else {
            Tools.darkToolbar(this, toolbar);
            toolbar.getContext().setTheme(R.style.ThemeOverlay_AppCompat_Dark);
        }
    }

    @SuppressLint("NonConstantResourceId")
    public void initViewPager() {
        if (Config.ENABLE_RTL_MODE) {
            viewPagerRTL = findViewById(R.id.view_pager_rtl);
            viewPagerRTL.setAdapter(new MyAdapter(getSupportFragmentManager()));
            viewPagerRTL.setOffscreenPageLimit(pager_number);

            if (Config.DISPLAY_CATEGORY_AS_MAIN_SCREEN) {
                navigation.setOnNavigationItemSelectedListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.navigation_category:
                            viewPagerRTL.setCurrentItem(0);
                            return true;
                        case R.id.navigation_home:
                            viewPagerRTL.setCurrentItem(1);
                            return true;
                        case R.id.navigation_favorite:
                            viewPagerRTL.setCurrentItem(2);
                            return true;
                    }
                    return false;
                });
            } else {
                navigation.setOnNavigationItemSelectedListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.navigation_home:
                            viewPagerRTL.setCurrentItem(0);
                            return true;
                        case R.id.navigation_category:
                            viewPagerRTL.setCurrentItem(1);
                            return true;
                        case R.id.navigation_favorite:
                            viewPagerRTL.setCurrentItem(2);
                            return true;
                    }
                    return false;
                });
            }

            viewPagerRTL.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    if (prevMenuItem != null) {
                        prevMenuItem.setChecked(false);
                    } else {
                        navigation.getMenu().getItem(0).setChecked(false);
                    }
                    navigation.getMenu().getItem(position).setChecked(true);
                    prevMenuItem = navigation.getMenu().getItem(position);

                    if (viewPagerRTL.getCurrentItem() == 0) {
                        toolbar.setTitle(getResources().getString(R.string.app_name));
                    } else if (viewPagerRTL.getCurrentItem() == 1) {
                        if (Config.DISPLAY_CATEGORY_AS_MAIN_SCREEN) {
                            toolbar.setTitle(getResources().getString(R.string.title_nav_home));
                        } else {
                            toolbar.setTitle(getResources().getString(R.string.title_nav_category));
                        }
                    } else if (viewPagerRTL.getCurrentItem() == 2) {
                        toolbar.setTitle(getResources().getString(R.string.title_nav_favorite));
                    }

                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });
        } else {
            viewPager = findViewById(R.id.view_pager);
            viewPager.setAdapter(new MyAdapter(getSupportFragmentManager()));
            viewPager.setOffscreenPageLimit(pager_number);

            if (Config.DISPLAY_CATEGORY_AS_MAIN_SCREEN) {
                navigation.setOnNavigationItemSelectedListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.navigation_category:
                            viewPager.setCurrentItem(0);
                            return true;
                        case R.id.navigation_home:
                            viewPager.setCurrentItem(1);
                            return true;
                        case R.id.navigation_favorite:
                            viewPager.setCurrentItem(2);
                            return true;
                    }
                    return false;
                });
            } else {
                navigation.setOnNavigationItemSelectedListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.navigation_home:
                            viewPager.setCurrentItem(0);
                            return true;
                        case R.id.navigation_category:
                            viewPager.setCurrentItem(1);
                            return true;
                        case R.id.navigation_favorite:
                            viewPager.setCurrentItem(2);
                            return true;
                    }
                    return false;
                });
            }

            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {
                    if (prevMenuItem != null) {
                        prevMenuItem.setChecked(false);
                    } else {
                        navigation.getMenu().getItem(0).setChecked(false);
                    }
                    navigation.getMenu().getItem(position).setChecked(true);
                    prevMenuItem = navigation.getMenu().getItem(position);

                    if (viewPager.getCurrentItem() == 0) {
                        toolbar.setTitle(getResources().getString(R.string.app_name));
                    } else if (viewPager.getCurrentItem() == 1) {
                        if (Config.DISPLAY_CATEGORY_AS_MAIN_SCREEN) {
                            toolbar.setTitle(getResources().getString(R.string.title_nav_home));
                        } else {
                            toolbar.setTitle(getResources().getString(R.string.title_nav_category));
                        }
                    } else if (viewPager.getCurrentItem() == 2) {
                        toolbar.setTitle(getResources().getString(R.string.title_nav_favorite));
                    }

                }

                @Override
                public void onPageScrollStateChanged(int state) {

                }
            });
        }
    }

    @SuppressWarnings("deprecation")
    public class MyAdapter extends FragmentPagerAdapter {

        MyAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            if (Config.DISPLAY_CATEGORY_AS_MAIN_SCREEN) {
                if (position == 0) {
                    return new FragmentCategory();
                } else if (position == 1) {
                    if (Config.ENABLE_SIMPLE_MODE) {
                        return new FragmentSimpleWallpaper();
                    } else {
                        return new FragmentTabLayout();
                    }
                } else {
                    return new FragmentFavorite();
                }
            } else {
                if (position == 0) {
                    if (Config.ENABLE_SIMPLE_MODE) {
                        return new FragmentSimpleWallpaper();
                    } else {
                        return new FragmentTabLayout();
                    }
                } else if (position == 1) {
                    return new FragmentCategory();
                } else {
                    return new FragmentFavorite();
                }
            }
        }

        @Override
        public int getCount() {
            return pager_number;
        }

    }

    @Override
    public AssetManager getAssets() {
        return getResources().getAssets();
    }

    @Override
    public void onBackPressed() {
        if (Config.ENABLE_RTL_MODE) {
            if (viewPagerRTL.getCurrentItem() != 0) {
                viewPagerRTL.setCurrentItem((0), true);
            } else {
                exitApp();
            }
        } else {
            if (viewPager.getCurrentItem() != 0) {
                viewPager.setCurrentItem((0), true);
            } else {
                exitApp();
            }
        }
        sharedPref.updateAppOpenToken(0);
    }

    public void exitApp() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Snackbar.make(coordinatorLayout, getString(R.string.snackbar_exit), Snackbar.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        menu_sort = menu;
        if (Config.ENABLE_RTL_MODE) {
            if (Config.DISPLAY_CATEGORY_AS_MAIN_SCREEN) {
                if (viewPagerRTL.getCurrentItem() == 1) {
                    menu_sort.findItem(R.id.menu_sort).setVisible(Config.ENABLE_SIMPLE_MODE);
                } else {
                    menu_sort.findItem(R.id.menu_sort).setVisible(false);
                }
            } else {
                if (viewPagerRTL.getCurrentItem() == 0) {
                    menu_sort.findItem(R.id.menu_sort).setVisible(Config.ENABLE_SIMPLE_MODE);
                } else {
                    menu_sort.findItem(R.id.menu_sort).setVisible(false);
                }
            }
        } else {
            if (Config.DISPLAY_CATEGORY_AS_MAIN_SCREEN) {
                if (viewPager.getCurrentItem() == 1) {
                    menu_sort.findItem(R.id.menu_sort).setVisible(Config.ENABLE_SIMPLE_MODE);
                } else {
                    menu_sort.findItem(R.id.menu_sort).setVisible(false);
                }
            } else {
                if (viewPager.getCurrentItem() == 0) {
                    menu_sort.findItem(R.id.menu_sort).setVisible(Config.ENABLE_SIMPLE_MODE);
                } else {
                    menu_sort.findItem(R.id.menu_sort).setVisible(false);
                }
            }
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_search) {
            if (Config.ENABLE_RTL_MODE) {
                if (Config.DISPLAY_CATEGORY_AS_MAIN_SCREEN) {
                    if (viewPagerRTL.getCurrentItem() == 0) {
                        Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
                        intent.putExtra(Constant.EXTRA_OBJC, "category");
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
                        intent.putExtra(Constant.EXTRA_OBJC, "wallpaper");
                        startActivity(intent);
                    }
                } else {
                    if (viewPagerRTL.getCurrentItem() == 1) {
                        Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
                        intent.putExtra(Constant.EXTRA_OBJC, "category");
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
                        intent.putExtra(Constant.EXTRA_OBJC, "wallpaper");
                        startActivity(intent);
                    }
                }
            } else {
                if (Config.DISPLAY_CATEGORY_AS_MAIN_SCREEN) {
                    if (viewPager.getCurrentItem() == 0) {
                        Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
                        intent.putExtra(Constant.EXTRA_OBJC, "category");
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
                        intent.putExtra(Constant.EXTRA_OBJC, "wallpaper");
                        startActivity(intent);
                    }
                } else {
                    if (viewPager.getCurrentItem() == 1) {
                        Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
                        intent.putExtra(Constant.EXTRA_OBJC, "category");
                        startActivity(intent);
                    } else {
                        Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
                        intent.putExtra(Constant.EXTRA_OBJC, "wallpaper");
                        startActivity(intent);
                    }
                }
            }
        } else if (item.getItemId() == R.id.menu_settings) {
            Intent intent = new Intent(getApplicationContext(), ActivitySettings.class);
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_rate) {
            final String package_name = BuildConfig.APPLICATION_ID;
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + package_name)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + package_name)));
            }
        } else if (item.getItemId() == R.id.menu_more) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.play_more_apps))));
        } else if (item.getItemId() == R.id.menu_share) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name));
            intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_text) + "\n" + "https://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID);
            intent.setType("text/plain");
            startActivity(intent);
        } else if (item.getItemId() == R.id.menu_about) {
            aboutDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    public void aboutDialog() {
        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        View view = inflater.inflate(R.layout.dialog_about, null);

        TextView txt_app_version = view.findViewById(R.id.txt_app_version);
        txt_app_version.setText(getString(R.string.msg_about_version) + " " + BuildConfig.VERSION_NAME);

        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
        alert.setView(view);
        alert.setPositiveButton(R.string.dialog_option_ok, (dialog, which) -> dialog.dismiss());
        alert.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (adsPref.getAdStatus().equals(AD_STATUS_ON) && adsPref.getAdType().equals(ADMOB)) {
            if (adsPref.getAppOpenAd() != 0) {
                numActivityRestarted++;
                if (canShowAppOpenAd()) {
                    if (sharedPref.getAppOpenToken() > 0) {
                        appOpenAdManager.showAdIfAvailable(adsPref.getAdMobAppOpenId());
                        Log.d("AppOpenAdsToken", "Show app open ad");
                    }
                }
            }
        }
        sharedPref.updateAppOpenToken(sharedPref.getAppOpenToken() + 1);
        Log.d("AppOpenAdsToken", "On restart app open token : " + sharedPref.getAppOpenToken());
    }

    private boolean canShowAppOpenAd() {
        return true;
    }

    public void getAdsLog() {
        Log.d("Native_ad", "" + adsPref.getBannerAdStatusHome());
    }

    private void inAppReview() {
        if (sharedPref.getInAppReviewToken() < 1) {
            sharedPref.updateInAppReviewToken(sharedPref.getInAppReviewToken() + 1);
            Log.d(TAG, "in app update token");
        } else {
            ReviewManager manager = ReviewManagerFactory.create(this);
            Task<ReviewInfo> request = manager.requestReviewFlow();
            request.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ReviewInfo reviewInfo = task.getResult();
                    manager.launchReviewFlow(MainActivity.this, reviewInfo).addOnFailureListener(e -> {
                    }).addOnCompleteListener(complete -> {
                                Log.d(TAG, "Success");
                            }
                    ).addOnFailureListener(failure -> {
                        Log.d(TAG, "Rating Failed");
                    });
                }
            }).addOnFailureListener(failure -> Log.d(TAG, "In-App Request Failed " + failure));
            Log.d(TAG, "in app token complete, show in app review if available");
        }
        Log.d(TAG, "in app review token : " + sharedPref.getInAppReviewToken());
    }


}