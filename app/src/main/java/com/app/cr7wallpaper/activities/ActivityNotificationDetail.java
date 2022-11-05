package com.app.cr7wallpaper.activities;

import static com.app.cr7wallpaper.utils.Constant.BASE_IMAGE_URL;
import static com.app.cr7wallpaper.utils.Constant.BOTH;
import static com.app.cr7wallpaper.utils.Constant.HOME_SCREEN;
import static com.app.cr7wallpaper.utils.Constant.LOCK_SCREEN;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.app.cr7wallpaper.Config;
import com.app.cr7wallpaper.R;
import com.app.cr7wallpaper.adapters.AdapterTags;
import com.app.cr7wallpaper.callbacks.CallbackDetail;
import com.app.cr7wallpaper.databases.prefs.AdsPref;
import com.app.cr7wallpaper.databases.prefs.SharedPref;
import com.app.cr7wallpaper.databases.sqlite.DBHelper;
import com.app.cr7wallpaper.models.Wallpaper;
import com.app.cr7wallpaper.rests.RestAdapter;
import com.app.cr7wallpaper.utils.AdsManager;
import com.app.cr7wallpaper.utils.Constant;
import com.app.cr7wallpaper.utils.Tools;
import com.app.cr7wallpaper.utils.WallpaperHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityNotificationDetail extends AppCompatActivity {

    Wallpaper wallpaper;
    Toolbar toolbar;
    ActionBar actionBar;
    private String single_choice_selected;
    CoordinatorLayout parent_view;
    private BottomSheetDialog mBottomSheetDialog;
    SharedPref sharedPref;
    DBHelper dbHelper;
    AdsPref adsPref;
    Call<CallbackDetail> callbackCall = null;
    boolean flag = true;
    String wallpaper_id;
    AdsManager adsManager;
    WallpaperHelper wallpaperHelper;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        if (adsPref.getBannerAdStatusDetail() != 0) {
            Tools.transparentStatusBar(this);
            if (sharedPref.getIsDarkTheme()) {
                Tools.darkNavigation(this);
            }
        } else {
            Tools.transparentStatusBarNavigation(this);
        }
        setContentView(R.layout.activity_notification_detail);
        Tools.getRtlDirection(this);
        Tools.resetAppOpenAdToken(this);
        parent_view = findViewById(R.id.coordinatorLayout);

        progressDialog = new ProgressDialog(this);
        wallpaperHelper = new WallpaperHelper(this);
        dbHelper = new DBHelper(this);

        Intent intent = getIntent();
        wallpaper_id = intent.getStringExtra("id");

        setupToolbar();
        requestWallpaperDetail();

        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(adsPref.getBannerAdStatusDetail());
        adsManager.loadInterstitialAd(adsPref.getInterstitialAdDetail(), 1);

    }

    private void requestWallpaperDetail() {
        callbackCall = RestAdapter.createAPI().getOneWallpaper(wallpaper_id);
        callbackCall.enqueue(new Callback<CallbackDetail>() {
            public void onResponse(Call<CallbackDetail> call, Response<CallbackDetail> response) {
                CallbackDetail resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    wallpaper = resp.wallpaper;
                    loadView(wallpaper);
                }
            }

            public void onFailure(Call<CallbackDetail> call, Throwable th) {
                Log.e("onFailure", "" + th.getMessage());
            }
        });
    }

    public void loadView(Wallpaper wallpaper) {

        String UPLOAD_URL = BASE_IMAGE_URL + wallpaper.image_upload;
        String DIRECT_URL = wallpaper.image_url;

        final PhotoView imageView = findViewById(R.id.image_view);
        if (Config.ENABLE_CENTER_CROP_IN_DETAIL_WALLPAPER) {
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        }

        imageView.setOnClickListener(v -> {
            if (flag) {
                fullScreenMode(true);
                flag = false;
            } else {
                fullScreenMode(false);
                flag = true;
            }
        });

        final ProgressBar progressBar = findViewById(R.id.progress_bar);

        if (wallpaper.type.equals("url")) {
            Glide.with(ActivityNotificationDetail.this)
                    .load(wallpaper.image_url.replace(" ", "%20"))
                    .placeholder(R.drawable.ic_transparent)
                    .thumbnail(0.3f)
                    //.centerCrop()
                    .addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        } else {
            Glide.with(ActivityNotificationDetail.this)
                    .load(BASE_IMAGE_URL + wallpaper.image_upload.replace(" ", "%20"))
                    .placeholder(R.drawable.ic_transparent)
                    .thumbnail(0.3f)
                    //.centerCrop()
                    .addListener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView);
        }

        TextView title_toolbar = findViewById(R.id.title_toolbar);
        TextView sub_title_toolbar = findViewById(R.id.sub_title_toolbar);

        if (!Config.ENABLE_DISPLAY_WALLPAPER_NAME) {
            title_toolbar.setVisibility(View.GONE);
            sub_title_toolbar.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.text_size_large));
        }

        if (!Config.ENABLE_DISPLAY_WALLPAPER_CATEGORY) {
            sub_title_toolbar.setVisibility(View.GONE);
        }

        if (wallpaper.image_name.equals("")) {
            title_toolbar.setText(wallpaper.category_name);
            sub_title_toolbar.setVisibility(View.GONE);
        } else {
            title_toolbar.setText(wallpaper.image_name);
            sub_title_toolbar.setText(wallpaper.category_name);
        }

        findViewById(R.id.btn_info).setOnClickListener(view -> showBottomSheetDialog(wallpaper));

        findViewById(R.id.btn_save).setOnClickListener(view -> {
            if (!verifyPermissions()) {
                return;
            }
            if (wallpaper.type.equals("upload")) {
                wallpaperHelper.downloadWallpaper(wallpaper, parent_view, progressDialog, UPLOAD_URL);
            } else if (wallpaper.type.equals("url")) {
                wallpaperHelper.downloadWallpaper(wallpaper, parent_view, progressDialog, DIRECT_URL);
            }
        });

        findViewById(R.id.btn_share).setOnClickListener(view -> {
            if (!verifyPermissions()) {
                return;
            }
            if (wallpaper.type.equals("upload")) {
                wallpaperHelper.shareWallpaper(progressDialog, UPLOAD_URL);
            } else if (wallpaper.type.equals("url")) {
                wallpaperHelper.shareWallpaper(progressDialog, DIRECT_URL);
            }
        });

        findViewById(R.id.btn_set_wallpaper).setOnClickListener(view -> {
            if (!verifyPermissions()) {
                return;
            }
            if (wallpaper.image_upload.endsWith(".gif") || wallpaper.image_url.endsWith(".gif")) {
                if (wallpaper.type.equals("upload")) {
                    wallpaperHelper.setGif(parent_view, progressDialog, UPLOAD_URL);
                } else if (wallpaper.type.equals("url")) {
                    wallpaperHelper.setGif(parent_view, progressDialog, DIRECT_URL);
                }
            } else {
                if (Build.VERSION.SDK_INT >= 24) {
                    if (wallpaper.type.equals("upload")) {
                        dialogOptionSetWallpaper(UPLOAD_URL, wallpaper);
                    } else if (wallpaper.type.equals("url")) {
                        dialogOptionSetWallpaper(DIRECT_URL, wallpaper);
                    }
                } else {
                    if (wallpaper.type.equals("upload")) {
                        wallpaperHelper.setWallpaper(parent_view, progressDialog, adsManager, UPLOAD_URL);
                    } else if (wallpaper.type.equals("url")) {
                        wallpaperHelper.setWallpaper(parent_view, progressDialog, adsManager, DIRECT_URL);
                    }
                }
            }
        });

        favToggle(wallpaper);
        findViewById(R.id.btn_favorite).setOnClickListener(view -> {
            if (dbHelper.isFavoritesExist(wallpaper.image_id)) {
                dbHelper.deleteFavorites(wallpaper);
                Snackbar.make(parent_view, getString(R.string.snack_bar_favorite_removed), Snackbar.LENGTH_SHORT).show();
            } else {
                dbHelper.addOneFavorite(wallpaper);
                Snackbar.make(parent_view, getString(R.string.snack_bar_favorite_added), Snackbar.LENGTH_SHORT).show();
            }
            favToggle(wallpaper);
        });

        wallpaperHelper.updateView(wallpaper.image_id);

    }

    private void favToggle(Wallpaper wallpaper) {
        ImageView img_favorite = findViewById(R.id.img_favorite);
        if (dbHelper.isFavoritesExist(wallpaper.image_id)) {
            img_favorite.setImageResource(R.drawable.ic_action_fav);
        } else {
            img_favorite.setImageResource(R.drawable.ic_action_fav_outline);
        }
    }

    public void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }
    }

    @SuppressWarnings("rawtypes")
    private void showBottomSheetDialog(Wallpaper wallpaper) {
        @SuppressLint("InflateParams") final View view = getLayoutInflater().inflate(R.layout.include_info, null);
        FrameLayout lyt_bottom_sheet = view.findViewById(R.id.bottom_sheet);

        if (sharedPref.getIsDarkTheme()) {
            lyt_bottom_sheet.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_rounded_dark));
        } else {
            lyt_bottom_sheet.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_rounded_default));
        }

        if (wallpaper.image_name.equals("")) {
            ((TextView) view.findViewById(R.id.txt_wallpaper_name)).setText("-");
        } else {
            ((TextView) view.findViewById(R.id.txt_wallpaper_name)).setText(wallpaper.image_name);
        }

        ((TextView) view.findViewById(R.id.txt_category_name)).setText(wallpaper.category_name);

        if (wallpaper.resolution.equals("0")) {
            ((TextView) view.findViewById(R.id.txt_resolution)).setText("-");
        } else {
            ((TextView) view.findViewById(R.id.txt_resolution)).setText(wallpaper.resolution);
        }

        if (wallpaper.size.equals("0")) {
            ((TextView) view.findViewById(R.id.txt_size)).setText("-");
        } else {
            ((TextView) view.findViewById(R.id.txt_size)).setText(wallpaper.size);
        }

        if (wallpaper.mime.equals("")) {
            ((TextView) view.findViewById(R.id.txt_mime_type)).setText("image/jpeg");
        } else {
            ((TextView) view.findViewById(R.id.txt_mime_type)).setText(wallpaper.mime);
        }

        ((TextView) view.findViewById(R.id.txt_view_count)).setText(Tools.withSuffix(wallpaper.views) + "");
        ((TextView) view.findViewById(R.id.txt_download_count)).setText(Tools.withSuffix(wallpaper.downloads) + "");

        LinearLayout lyt_tags = view.findViewById(R.id.lyt_tags);
        if (wallpaper.tags.equals("")) {
            lyt_tags.setVisibility(View.GONE);
        } else {
            lyt_tags.setVisibility(View.VISIBLE);
        }

        @SuppressWarnings("unchecked") ArrayList<String> arrayListTags = new ArrayList(Arrays.asList(wallpaper.tags.split(",")));
        AdapterTags adapterTags = new AdapterTags(this, arrayListTags);
        RecyclerView recycler_view_tags = view.findViewById(R.id.recycler_view_tags);
        FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setFlexDirection(FlexDirection.ROW);
        //layoutManager.setJustifyContent(JustifyContent.CENTER);
        layoutManager.setAlignItems(AlignItems.FLEX_START);
        recycler_view_tags.setLayoutManager(layoutManager);
        recycler_view_tags.setAdapter(adapterTags);

        adapterTags.setOnItemClickListener((v, keyword, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
            intent.putExtra("tags", keyword);
            intent.putExtra(Constant.EXTRA_OBJC, "wallpaper");
            startActivity(intent);

            mBottomSheetDialog.dismiss();
        });

        if (sharedPref.getIsDarkTheme()) {
            mBottomSheetDialog = new BottomSheetDialog(this, R.style.SheetDialogDark);
        } else {
            mBottomSheetDialog = new BottomSheetDialog(this, R.style.SheetDialogLight);
        }
        mBottomSheetDialog.setContentView(view);
        mBottomSheetDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        BottomSheetBehavior bottomSheetBehavior = mBottomSheetDialog.getBehavior();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        bottomSheetBehavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {

            }
        });

        mBottomSheetDialog.show();
        mBottomSheetDialog.setOnDismissListener(dialog -> mBottomSheetDialog = null);
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void dialogOptionSetWallpaper(String imageURL, Wallpaper wp) {
        String[] items = getResources().getStringArray(R.array.dialog_set_wallpaper);
        single_choice_selected = items[0];
        int itemSelected = 0;
        new AlertDialog.Builder(ActivityNotificationDetail.this)
                .setTitle(R.string.dialog_set_title)
                .setSingleChoiceItems(items, itemSelected, (dialogInterface, i) -> single_choice_selected = items[i])
                .setPositiveButton(R.string.dialog_option_ok, (dialogInterface, i) -> {

                    progressDialog.setMessage(getString(R.string.msg_preparing_wallpaper));
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    new Handler(Looper.getMainLooper()).postDelayed(() -> Glide.with(this)
                            .load(imageURL.replace(" ", "%20"))
                            .into(new CustomTarget<Drawable>() {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                                    Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                                    if (single_choice_selected.equals(getResources().getString(R.string.set_home_screen))) {
                                        wallpaperHelper.setWallpaper(parent_view, progressDialog, adsManager, bitmap, HOME_SCREEN);
                                        progressDialog.setMessage(getString(R.string.msg_apply_wallpaper));
                                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_lock_screen))) {
                                        wallpaperHelper.setWallpaper(parent_view, progressDialog, adsManager, bitmap, LOCK_SCREEN);
                                        progressDialog.setMessage(getString(R.string.msg_apply_wallpaper));
                                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_both))) {
                                        wallpaperHelper.setWallpaper(parent_view, progressDialog, adsManager, bitmap, BOTH);
                                        progressDialog.setMessage(getString(R.string.msg_apply_wallpaper));
                                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_crop))) {
                                        if (wp.type.equals("upload")) {
                                            Intent intent = new Intent(getApplicationContext(), ActivityCropWallpaper.class);
                                            intent.putExtra("image_url", BASE_IMAGE_URL + wp.image_upload);
                                            startActivity(intent);
                                        } else if (wp.type.equals("url")) {
                                            Intent intent = new Intent(getApplicationContext(), ActivityCropWallpaper.class);
                                            intent.putExtra("image_url", wp.image_url);
                                            startActivity(intent);
                                        }
                                        progressDialog.dismiss();
                                    } else if (single_choice_selected.equals(getResources().getString(R.string.set_with))) {
                                        if (wp.type.equals("upload")) {
                                            wallpaperHelper.setWallpaperFromOtherApp(BASE_IMAGE_URL + wp.image_upload);
                                        } else if (wp.type.equals("url")) {
                                            wallpaperHelper.setWallpaperFromOtherApp(wp.image_url);
                                        }
                                        progressDialog.dismiss();
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder) {
                                    progressDialog.dismiss();
                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                    super.onLoadFailed(errorDrawable);
                                    Snackbar.make(parent_view, getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                                    progressDialog.dismiss();
                                }
                            }), Constant.DELAY_SET);

                })
                .setNegativeButton(R.string.dialog_option_cancel, null)
                .show();
    }

    public Boolean verifyPermissions() {
        int permissionExternalMemory = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionExternalMemory != PackageManager.PERMISSION_GRANTED) {
            String[] STORAGE_PERMISSIONS = {Manifest.permission.WRITE_EXTERNAL_STORAGE};
            ActivityCompat.requestPermissions(this, STORAGE_PERMISSIONS, 1);
            return false;
        }
        return true;
    }

    public void fullScreenMode(boolean on) {
        LinearLayout lyt_bottom = findViewById(R.id.lyt_bottom);
        RelativeLayout bg_shadow = findViewById(R.id.bg_shadow_bottom);
        if (on) {
            toolbar.setVisibility(View.GONE);
            toolbar.animate().translationY(-112);
            lyt_bottom.setVisibility(View.GONE);
            lyt_bottom.animate().translationY(lyt_bottom.getHeight());

            bg_shadow.setVisibility(View.GONE);
            bg_shadow.animate().translationY(lyt_bottom.getHeight());

            Tools.transparentStatusBarNavigation(this);

            hideSystemUI();

        } else {
            toolbar.setVisibility(View.VISIBLE);
            toolbar.animate().translationY(0);
            lyt_bottom.setVisibility(View.VISIBLE);
            lyt_bottom.animate().translationY(0);

            bg_shadow.setVisibility(View.VISIBLE);
            bg_shadow.animate().translationY(0);

            if (adsPref.getBannerAdStatusDetail() != 0) {
                Tools.transparentStatusBar(this);
            } else {
                Tools.transparentStatusBarNavigation(this);
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void hideSystemUI() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

}
