package com.app.cr7wallpaper.utils;

import static com.app.cr7wallpaper.utils.Constant.BOTH;
import static com.app.cr7wallpaper.utils.Constant.DELAY_SET;
import static com.app.cr7wallpaper.utils.Constant.DOWNLOAD;
import static com.app.cr7wallpaper.utils.Constant.HOME_SCREEN;
import static com.app.cr7wallpaper.utils.Constant.LOCK_SCREEN;
import static com.app.cr7wallpaper.utils.Constant.SET_GIF;
import static com.app.cr7wallpaper.utils.Constant.SET_WITH;
import static com.app.cr7wallpaper.utils.Constant.SHARE;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.app.cr7wallpaper.R;
import com.app.cr7wallpaper.models.Wallpaper;
import com.app.cr7wallpaper.rests.ApiInterface;
import com.app.cr7wallpaper.rests.RestAdapter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WallpaperHelper {

    private static final String TAG = "WallpaperHelper";
    Activity activity;

    public WallpaperHelper(Activity activity) {
        this.activity = activity;
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void setWallpaper(View view, ProgressDialog progressDialog, AdsManager adsManager, Bitmap bitmap, String setAs) {
        switch (setAs) {
            case HOME_SCREEN:
                try {
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(activity);
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM);
                    onWallpaperApplied(progressDialog, adsManager);
                } catch (IOException e) {
                    e.printStackTrace();
                    Snackbar.make(view, activity.getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
                break;

            case LOCK_SCREEN:
                try {
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(activity);
                    wallpaperManager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK);
                    onWallpaperApplied(progressDialog, adsManager);
                } catch (IOException e) {
                    e.printStackTrace();
                    Snackbar.make(view, activity.getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
                break;

            case BOTH:
                try {
                    WallpaperManager wallpaperManager = WallpaperManager.getInstance(activity);
                    wallpaperManager.setBitmap(bitmap);
                    onWallpaperApplied(progressDialog, adsManager);
                } catch (IOException e) {
                    e.printStackTrace();
                    Snackbar.make(view, activity.getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
                break;
        }
    }

    public void setWallpaper(View view, ProgressDialog progressDialog, AdsManager adsManager, String imageURL) {

        progressDialog.setMessage(activity.getString(R.string.msg_preparing_wallpaper));
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Handler().postDelayed(() -> Glide.with(activity)
                .load(imageURL.replace(" ", "%20"))
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                        try {
                            WallpaperManager wallpaperManager = WallpaperManager.getInstance(activity);
                            wallpaperManager.setBitmap(bitmap);
                            progressDialog.setMessage(activity.getString(R.string.msg_apply_wallpaper));
                            onWallpaperApplied(progressDialog, adsManager);
                        } catch (IOException e) {
                            e.printStackTrace();
                            Snackbar.make(view, activity.getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                        }
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        Snackbar.make(view, activity.getString(R.string.snack_bar_error), Snackbar.LENGTH_SHORT).show();
                        progressDialog.dismiss();
                    }
                }), DELAY_SET);
    }

    public void onWallpaperApplied(ProgressDialog progressDialog, AdsManager adsManager) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            showSuccessDialog();
            progressDialog.dismiss();
            new Handler(Looper.getMainLooper()).postDelayed(adsManager::showInterstitialAd, 1500);
        }, DELAY_SET);
    }

    public void showSuccessDialog() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(activity);
        LayoutInflater mInflater = LayoutInflater.from(activity);
        final View viewLayout = mInflater.inflate(R.layout.dialog_success, null);
        dialog.setView(viewLayout);
        dialog.setCancelable(false);

        final AlertDialog alertDialog = dialog.create();

        Button btn_done = viewLayout.findViewById(R.id.btn_done);
        btn_done.setOnClickListener(v -> new Handler().postDelayed(() -> {
            alertDialog.dismiss();
            activity.finish();
        }, 250));

        alertDialog.show();

    }

    public void setGif(View view, ProgressDialog progressDialog, String imageURL) {

        progressDialog.setMessage(activity.getString(R.string.msg_preparing_wallpaper));
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Handler().postDelayed(() -> Glide.with(activity)
                .download(imageURL.replace(" ", "%20"))
                .listener(new RequestListener<File>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                        progressDialog.dismiss();
                        Snackbar.make(view, activity.getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                        try {
                            Tools.setAction(activity, Tools.getBytesFromFile(resource), Tools.createName(imageURL), SET_GIF);
                            progressDialog.dismiss();
                        } catch (IOException e) {
                            e.printStackTrace();
                            progressDialog.dismiss();
                            Snackbar.make(view, activity.getString(R.string.snack_bar_failed), Snackbar.LENGTH_SHORT).show();
                        }
                        return true;
                    }
                })
                .submit(), DELAY_SET);
    }

    public void setWallpaperFromOtherApp(String imageURL) {

        new Handler().postDelayed(() -> Glide.with(activity)
                .download(imageURL.replace(" ", "%20"))
                .listener(new RequestListener<File>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                        try {
                            Tools.setAction(activity, Tools.getBytesFromFile(resource), Tools.createName(imageURL), SET_WITH);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                    }
                })
                .submit(), DELAY_SET);

    }

    public void downloadWallpaper(Wallpaper wallpaper, View view, ProgressDialog progressDialog, String imageURL) {

        progressDialog.setMessage(activity.getString(R.string.snack_bar_saving));
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Handler().postDelayed(() -> Glide.with(activity)
                .download(imageURL.replace(" ", "%20"))
                .listener(new RequestListener<File>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                        progressDialog.dismiss();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                        try {
                            Tools.setAction(activity, Tools.getBytesFromFile(resource), Tools.createName(imageURL), DOWNLOAD);

                            Handler handler = new Handler(Looper.getMainLooper());
                            handler.postDelayed(() -> {
                                Snackbar.make(view, activity.getString(R.string.snack_bar_saved), Snackbar.LENGTH_SHORT).show();
                                updateDownload(wallpaper.image_id);
                                progressDialog.dismiss();
                            }, Constant.DELAY_SET);

                        } catch (IOException e) {
                            e.printStackTrace();
                            progressDialog.dismiss();
                        }
                        return true;
                    }
                })
                .submit(), DELAY_SET);

    }

    public void shareWallpaper(ProgressDialog progressDialog, String imageURL) {

        progressDialog.setMessage(activity.getString(R.string.msg_preparing_wallpaper));
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Handler().postDelayed(() -> Glide.with(activity)
                .download(imageURL.replace(" ", "%20"))
                .listener(new RequestListener<File>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<File> target, boolean isFirstResource) {
                        progressDialog.dismiss();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(File resource, Object model, Target<File> target, DataSource dataSource, boolean isFirstResource) {
                        try {
                            Tools.setAction(activity, Tools.getBytesFromFile(resource), Tools.createName(imageURL), SHARE);
                            progressDialog.dismiss();
                        } catch (IOException e) {
                            e.printStackTrace();
                            progressDialog.dismiss();
                        }
                        return true;
                    }
                })
                .submit(), DELAY_SET);

    }

    public void updateView(String image_id) {
        ApiInterface apiInterface = RestAdapter.createAPI();
        Call<Wallpaper> call = apiInterface.updateView(image_id);
        call.enqueue(new Callback<Wallpaper>() {
            @Override
            public void onResponse(Call<Wallpaper> call, Response<Wallpaper> response) {
                Log.d(TAG, "success update view");
            }

            @Override
            public void onFailure(Call<Wallpaper> call, Throwable t) {
                Log.d(TAG, "failed update view");
            }
        });
    }

    public void updateDownload(String image_id) {
        ApiInterface apiInterface = RestAdapter.createAPI();
        Call<Wallpaper> call = apiInterface.updateDownload(image_id);
        call.enqueue(new Callback<Wallpaper>() {
            @Override
            public void onResponse(Call<Wallpaper> call, Response<Wallpaper> response) {
                Log.d(TAG, "success update download");
            }

            @Override
            public void onFailure(Call<Wallpaper> call, Throwable t) {
                Log.d(TAG, "failed update download");
            }
        });
    }

    public void downloadWallpaperManager(View view, String filename, String image_url, String extension, String mime) {
        try {
            DownloadManager dm = (DownloadManager) activity.getSystemService(Context.DOWNLOAD_SERVICE);
            Uri downloadUri = Uri.parse(image_url);
            DownloadManager.Request request = new DownloadManager.Request(downloadUri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                    .setAllowedOverRoaming(false)
                    .setTitle(filename)
                    .setMimeType(mime) // Your file type. You can use this code to download other file types also.
                    //.setMimeType("image/jpeg") // Your file type. You can use this code to download other file types also.
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, File.separator + filename + "." + extension);
            dm.enqueue(request);
            //Toast.makeText(activity, "Image download started.", Toast.LENGTH_SHORT).show();
            Snackbar.make(view, activity.getString(R.string.start_download), Snackbar.LENGTH_SHORT).show();
        } catch (Exception e) {
            //Toast.makeText(activity, "Image download failed.", Toast.LENGTH_SHORT).show();
            Snackbar.make(view, activity.getString(R.string.failed_download), Snackbar.LENGTH_SHORT).show();
        }
    }

}