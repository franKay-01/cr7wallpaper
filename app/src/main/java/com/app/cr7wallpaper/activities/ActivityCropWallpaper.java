package com.app.cr7wallpaper.activities;

import static com.app.cr7wallpaper.utils.Constant.BOTH;
import static com.app.cr7wallpaper.utils.Constant.DELAY_SET;
import static com.app.cr7wallpaper.utils.Constant.HOME_SCREEN;
import static com.app.cr7wallpaper.utils.Constant.LOCK_SCREEN;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.app.cr7wallpaper.R;
import com.app.cr7wallpaper.databases.prefs.AdsPref;
import com.app.cr7wallpaper.utils.AdsManager;
import com.app.cr7wallpaper.utils.Tools;
import com.app.cr7wallpaper.utils.WallpaperHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageView;
import com.google.android.material.snackbar.Snackbar;

public class ActivityCropWallpaper extends AppCompatActivity {

    String image_url;
    Bitmap bitmap = null;
    CropImageView cropImageView;
    private String single_choice_selected;
    CoordinatorLayout parent_view;
    AdsPref adsPref;
    ProgressDialog progressDialog;
    WallpaperHelper wallpaperHelper;
    AdsManager adsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        Tools.transparentStatusBarNavigation(ActivityCropWallpaper.this);
        setContentView(R.layout.activity_set_wallpaper);
        Tools.getRtlDirection(this);
        Tools.resetAppOpenAdToken(this);

        adsPref = new AdsPref(this);
        progressDialog = new ProgressDialog(this);
        wallpaperHelper = new WallpaperHelper(this);
        adsManager = new AdsManager(this);

        adsManager.loadInterstitialAd(adsPref.getInterstitialAdDetail(), 1);

        Intent intent = getIntent();
        image_url = intent.getStringExtra("image_url");

        cropImageView = findViewById(R.id.cropImageView);
        parent_view = findViewById(R.id.coordinatorLayout);

        loadWallpaper();

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void loadWallpaper() {
        Glide.with(this)
                .load(image_url.replace(" ", "%20"))
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        bitmap = ((BitmapDrawable) resource).getBitmap();
                        cropImageView.setImageBitmap(bitmap);

                        findViewById(R.id.btn_set_wallpaper).setOnClickListener(view -> dialogOptionSetWallpaper());
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        Snackbar.make(parent_view, getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void dialogOptionSetWallpaper() {
        String[] items = getResources().getStringArray(R.array.dialog_set_crop_wallpaper);
        single_choice_selected = items[0];
        int itemSelected = 0;
        bitmap = cropImageView.getCroppedImage();
        new AlertDialog.Builder(ActivityCropWallpaper.this)
                .setTitle(R.string.dialog_set_title)
                .setSingleChoiceItems(items, itemSelected, (dialogInterface, i) -> single_choice_selected = items[i])
                .setPositiveButton(R.string.dialog_option_ok, (dialogInterface, i) -> {

                    progressDialog.setMessage(getString(R.string.msg_preparing_wallpaper));
                    progressDialog.setCancelable(false);
                    progressDialog.show();

                    new Handler().postDelayed(() -> {

                        if (single_choice_selected.equals(getResources().getString(R.string.set_home_screen))) {
                            wallpaperHelper.setWallpaper(parent_view, progressDialog, adsManager, bitmap, HOME_SCREEN);
                            progressDialog.setMessage(getString(R.string.msg_apply_wallpaper));
                        } else if (single_choice_selected.equals(getResources().getString(R.string.set_lock_screen))) {
                            wallpaperHelper.setWallpaper(parent_view, progressDialog, adsManager, bitmap, LOCK_SCREEN);
                            progressDialog.setMessage(getString(R.string.msg_apply_wallpaper));
                        } else if (single_choice_selected.equals(getResources().getString(R.string.set_both))) {
                            wallpaperHelper.setWallpaper(parent_view, progressDialog, adsManager, bitmap, BOTH);
                            progressDialog.setMessage(getString(R.string.msg_apply_wallpaper));
                        }

                    }, DELAY_SET);

                })
                .setNegativeButton(R.string.dialog_option_cancel, null)
                .show();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUriContent();
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

}
