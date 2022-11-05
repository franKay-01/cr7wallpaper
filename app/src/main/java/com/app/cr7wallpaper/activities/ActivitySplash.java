package com.app.cr7wallpaper.activities;

import static com.solodroid.ads.sdk.util.Constant.ADMOB;
import static com.solodroid.ads.sdk.util.Constant.AD_STATUS_ON;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.app.cr7wallpaper.Config;
import com.app.cr7wallpaper.R;
import com.app.cr7wallpaper.callbacks.CallbackAds;
import com.app.cr7wallpaper.databases.prefs.AdsPref;
import com.app.cr7wallpaper.databases.prefs.SharedPref;
import com.app.cr7wallpaper.models.AdStatus;
import com.app.cr7wallpaper.models.Ads;
import com.app.cr7wallpaper.rests.RestAdapter;
import com.app.cr7wallpaper.utils.Tools;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.solodroid.ads.sdk.format.AppOpenAd;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySplash extends AppCompatActivity {

    private static final String TAG = "AppOpenManager";
    AppOpenAd appOpenAdManager;
    private boolean isAdShown = false;
    private boolean isAdDismissed = false;
    private boolean isLoadCompleted = false;
    ProgressBar progressBar;
    long nid = 0;
    String url = "";
    ImageView img_splash;
    Call<CallbackAds> callbackCall = null;
    SharedPref sharedPref;
    AdsPref adsPref;
    AdStatus ad_status;
    Ads ads;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.transparentStatusBarNavigation(ActivitySplash.this);
        setContentView(R.layout.activity_splash);
        Tools.getRtlDirection(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);

        img_splash = findViewById(R.id.img_splash);
        if (sharedPref.getIsDarkTheme()) {
            img_splash.setImageResource(R.drawable.splash_dark);
        } else {
            img_splash.setImageResource(R.drawable.splash_default);
        }

        progressBar = findViewById(R.id.progressBar);

        if (getIntent().hasExtra("nid")) {
            nid = getIntent().getLongExtra("nid", 0);
            url = getIntent().getStringExtra("external_link");
        }

        requestAds();

    }

    private void requestAds() {
        this.callbackCall = RestAdapter.createAPI().getAds();
        this.callbackCall.enqueue(new Callback<CallbackAds>() {
            public void onResponse(Call<CallbackAds> call, Response<CallbackAds> response) {
                CallbackAds resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    ads = resp.ads;
                    ad_status = resp.ads_status;
                    adsPref.saveAds(
                            ads.ad_status.replace("on", "1"),
                            ads.ad_type, ads.admob_publisher_id,
                            ads.admob_app_id, ads.admob_banner_unit_id,
                            ads.admob_interstitial_unit_id,
                            ads.admob_native_unit_id,
                            ads.admob_app_open_ad_unit_id,
                            ads.fan_banner_unit_id,
                            ads.fan_interstitial_unit_id,
                            ads.fan_native_unit_id,
                            ads.startapp_app_id,
                            ads.unity_game_id,
                            ads.unity_banner_placement_id,
                            ads.unity_interstitial_placement_id,
                            ads.applovin_banner_ad_unit_id,
                            ads.applovin_interstitial_ad_unit_id,
                            ads.mopub_banner_ad_unit_id,
                            ads.mopub_interstitial_ad_unit_id,
                            ads.interstitial_ad_interval,
                            ads.native_ad_interval,
                            ads.native_ad_index,
                            ads.last_update_ads
                    );

                    adsPref.saveAdStatus(
                            ad_status.banner_ad_on_home_page,
                            ad_status.banner_ad_on_search_page,
                            ad_status.banner_ad_on_wallpaper_detail,
                            ad_status.banner_ad_on_wallpaper_by_category,
                            ad_status.interstitial_ad_on_click_wallpaper,
                            ad_status.interstitial_ad_on_wallpaper_detail,
                            ad_status.native_ad_on_wallpaper_list,
                            ad_status.native_ad_on_exit_dialog,
                            ad_status.app_open_ad,
                            ad_status.last_update_ads_status
                    );
                    onSplashFinished();
                } else {
                    onSplashFinished();
                }
            }

            public void onFailure(Call<CallbackAds> call, Throwable th) {
                Log.e("onFailure", "" + th.getMessage());
                onSplashFinished();
            }
        });
    }

    private void onSplashFinished() {
        if (adsPref.getAdType().equals(ADMOB) && adsPref.getAdStatus().equals(AD_STATUS_ON)) {
            if (adsPref.getAppOpenAd() != 0) {
                launchAppOpenAd();
            } else {
                launchMainScreen();
            }
        } else {
            launchMainScreen();
        }
    }

    private void launchMainScreen() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        new Handler(Looper.getMainLooper()).postDelayed(this::finish, 2000);
    }

    private void launchAppOpenAd() {
        appOpenAdManager = ((MyApplication) getApplication()).getAppOpenAdManager();
        loadResources();
        appOpenAdManager.showAdIfAvailable(adsPref.getAdMobAppOpenId(), new FullScreenContentCallback() {

            @Override
            public void onAdShowedFullScreenContent() {
                isAdShown = true;
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                isAdDismissed = true;
                if (isLoadCompleted) {
                    launchMainScreen();
                    Log.d(TAG, "isLoadCompleted and launch main screen...");
                } else {
                    Log.d(TAG, "Waiting resources to be loaded...");
                }
            }
        });
    }

    private void loadResources() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isLoadCompleted = true;
            // Check whether App Open ad was shown or not.
            if (isAdShown) {
                // Check App Open ad was dismissed or not.
                if (isAdDismissed) {
                    launchMainScreen();
                    Log.d(TAG, "isAdDismissed and launch main screen...");
                } else {
                    Log.d(TAG, "Waiting for ad to be dismissed...");
                }
            } else {
                launchMainScreen();
            }
        }, Config.SPLASH_TIME);
    }

}
