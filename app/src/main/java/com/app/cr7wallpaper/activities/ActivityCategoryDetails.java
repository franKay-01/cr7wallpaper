package com.app.cr7wallpaper.activities;

import static com.app.cr7wallpaper.utils.Constant.EXTRA_OBJC;
import static com.app.cr7wallpaper.utils.Constant.FILTER_ALL;
import static com.app.cr7wallpaper.utils.Constant.FILTER_LIVE;
import static com.app.cr7wallpaper.utils.Constant.ORDER_FEATURED;
import static com.app.cr7wallpaper.utils.Constant.ORDER_LIVE;
import static com.app.cr7wallpaper.utils.Constant.ORDER_POPULAR;
import static com.app.cr7wallpaper.utils.Constant.ORDER_RANDOM;
import static com.app.cr7wallpaper.utils.Constant.ORDER_RECENT;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.cr7wallpaper.Config;
import com.app.cr7wallpaper.R;
import com.app.cr7wallpaper.adapters.AdapterWallpaper;
import com.app.cr7wallpaper.callbacks.CallbackWallpaper;
import com.app.cr7wallpaper.databases.prefs.AdsPref;
import com.app.cr7wallpaper.databases.prefs.SharedPref;
import com.app.cr7wallpaper.databases.sqlite.DBHelper;
import com.app.cr7wallpaper.models.Category;
import com.app.cr7wallpaper.models.Wallpaper;
import com.app.cr7wallpaper.rests.ApiInterface;
import com.app.cr7wallpaper.rests.RestAdapter;
import com.app.cr7wallpaper.utils.AdsManager;
import com.app.cr7wallpaper.utils.Constant;
import com.app.cr7wallpaper.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivityCategoryDetails extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdapterWallpaper adapterWallpaper;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout lyt_shimmer;
    private Call<CallbackWallpaper> callbackCall = null;
    private int post_total = 0;
    private int failed_page = 0;
    List<Wallpaper> items = new ArrayList<>();
    Category category;
    private String single_choice_selected;
    SharedPref sharedPref;
    DBHelper dbHelper;
    AdsPref adsPref;
    AdsManager adsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        sharedPref = new SharedPref(this);
        adsPref = new AdsPref(this);
        if (sharedPref.getIsDarkTheme()) {
            Tools.darkNavigation(this);
        } else {
            Tools.lightNavigation(this);
        }
        setContentView(R.layout.activity_category_details);
        Tools.getRtlDirection(this);
        Tools.resetAppOpenAdToken(this);
        dbHelper = new DBHelper(this);
        sharedPref.setDefaultSortWallpaper();
        category = (Category) getIntent().getSerializableExtra(EXTRA_OBJC);

        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(adsPref.getBannerAdStatusCategoryDetail());
        adsManager.loadInterstitialAd(adsPref.getInterstitialAdClickWallpaper(), adsPref.getInterstitialAdInterval());

        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        lyt_shimmer = findViewById(R.id.shimmer_view_container);
        initShimmerLayout();

        recyclerView = findViewById(R.id.recyclerView);
        //ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.grid_space_wallpaper);
        //recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(sharedPref.getWallpaperColumns(), StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setHasFixedSize(true);

        //set data and list adapter
        adapterWallpaper = new AdapterWallpaper(this, recyclerView, items);
        recyclerView.setAdapter(adapterWallpaper);

        // on item list clicked
        adapterWallpaper.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityWallpaperDetail.class);
            intent.putExtra(Constant.POSITION, position);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constant.ARRAY_LIST, (Serializable) items);
            intent.putExtra(Constant.BUNDLE, bundle);
            intent.putExtra(EXTRA_OBJC, obj);
            startActivity(intent);

            adsManager.showInterstitialAd();
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView v, int state) {
                super.onScrollStateChanged(v, state);
            }
        });

        // detect when scroll reach bottom
        adapterWallpaper.setOnLoadMoreListener(current_page -> {
            if (adsPref.getNativeAdWallpaperList() != 0) {
                setLoadMoreNativeAd(current_page);
            } else {
                setLoadMore(current_page);
            }
        });

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
            adapterWallpaper.resetListData();
            if (Tools.isConnect(this)) {
                dbHelper.deleteWallpaperByCategory(DBHelper.TABLE_CATEGORY_DETAIL, category.category_id);
            }
            requestAction(1);
        });

        requestAction(1);
        setupToolbar();
        onOptionMenuClicked();

    }

    public void setLoadMoreNativeAd(int current_page) {
        Log.d("page", "currentPage: " + current_page);
        // Assuming final total items equal to real post items plus the ad
        int totalItemBeforeAds = (adapterWallpaper.getItemCount() - current_page);
        if (post_total > totalItemBeforeAds && current_page != 0) {
            int next_page = current_page + 1;
            requestAction(next_page);
        } else {
            adapterWallpaper.setLoaded();
        }
    }

    public void setLoadMore(int current_page) {
        if (post_total > adapterWallpaper.getItemCount() && current_page != 0) {
            int next_page = current_page + 1;
            requestAction(next_page);
        } else {
            adapterWallpaper.setLoaded();
        }
    }

    public void setupToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        if (sharedPref.getIsDarkTheme()) {
            Tools.darkToolbar(this, toolbar);
        } else {
            Tools.lightToolbar(this, toolbar);
        }
        final TextView title_toolbar = findViewById(R.id.title_toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            title_toolbar.setText("" + category.category_name);
        }
    }

    private void displayApiResult(final List<Wallpaper> wallpapers) {
        insertData(wallpapers);
        swipeProgress(false);
        if (wallpapers.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestListPostApi(final int page_no) {

        ApiInterface apiInterface = RestAdapter.createAPI();

        if (sharedPref.getWallpaperColumns() == 3) {
            if (sharedPref.getCurrentSortWallpaper() == 0) {
                callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE_3_COLUMNS, category.category_id, FILTER_ALL, ORDER_RECENT);
            } else if (sharedPref.getCurrentSortWallpaper() == 1) {
                callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE_3_COLUMNS, category.category_id, FILTER_ALL, ORDER_FEATURED);
            } else if (sharedPref.getCurrentSortWallpaper() == 2) {
                callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE_3_COLUMNS, category.category_id, FILTER_ALL, ORDER_POPULAR);
            } else if (sharedPref.getCurrentSortWallpaper() == 3) {
                callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE_3_COLUMNS, category.category_id, FILTER_ALL, ORDER_RANDOM);
            } else if (sharedPref.getCurrentSortWallpaper() == 4) {
                callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE_3_COLUMNS, category.category_id, FILTER_LIVE, ORDER_LIVE);
            }
        } else {
            if (sharedPref.getCurrentSortWallpaper() == 0) {
                callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE_2_COLUMNS, category.category_id, FILTER_ALL, ORDER_RECENT);
            } else if (sharedPref.getCurrentSortWallpaper() == 1) {
                callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE_2_COLUMNS, category.category_id, FILTER_ALL, ORDER_FEATURED);
            } else if (sharedPref.getCurrentSortWallpaper() == 2) {
                callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE_2_COLUMNS, category.category_id, FILTER_ALL, ORDER_POPULAR);
            } else if (sharedPref.getCurrentSortWallpaper() == 3) {
                callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE_2_COLUMNS, category.category_id, FILTER_ALL, ORDER_RANDOM);
            } else if (sharedPref.getCurrentSortWallpaper() == 4) {
                callbackCall = apiInterface.getCategoryDetails(page_no, Constant.LOAD_MORE_2_COLUMNS, category.category_id, FILTER_LIVE, ORDER_LIVE);
            }
        }

        callbackCall.enqueue(new Callback<CallbackWallpaper>() {
            @Override
            public void onResponse(Call<CallbackWallpaper> call, Response<CallbackWallpaper> response) {
                CallbackWallpaper resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    post_total = resp.count_total;
                    displayApiResult(resp.posts);
                    if (page_no == 1)
                        dbHelper.truncateTableWallpaper(DBHelper.TABLE_CATEGORY_DETAIL);
                    dbHelper.addListWallpaper(resp.posts, DBHelper.TABLE_CATEGORY_DETAIL);
                } else {
                    onFailRequest(page_no);
                }
            }

            @Override
            public void onFailure(Call<CallbackWallpaper> call, Throwable t) {
                swipeProgress(false);
                loadDataFromDatabase(call, page_no);
            }
        });
    }

    private void loadDataFromDatabase(Call<CallbackWallpaper> call, final int page_no) {
        List<Wallpaper> wallpapers = dbHelper.getAllWallpaperByCategory(DBHelper.TABLE_CATEGORY_DETAIL, category.category_id);
        insertData(wallpapers);
        if (wallpapers.size() == 0) {
            if (!call.isCanceled()) onFailRequest(page_no);
        }
    }

    private void insertData(List<Wallpaper> wallpapers) {
        if (adsPref.getNativeAdWallpaperList() != 0) {
            if (adsPref.getAdType().equals("unity")) {
                adapterWallpaper.insertData(wallpapers);
            } else {
                adapterWallpaper.insertDataWithNativeAd(wallpapers);
            }
        } else {
            adapterWallpaper.insertData(wallpapers);
        }
    }

    private void onFailRequest(int page_no) {
        failed_page = page_no;
        adapterWallpaper.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(this)) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void requestAction(final int page_no) {
        showFailedView(false, "");
        showNoItemView(false);
        if (page_no == 1) {
            swipeProgress(true);
        } else {
            adapterWallpaper.setLoading();
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> requestListPostApi(page_no), Constant.DELAY_TIME);
    }

    private void showFailedView(boolean show, String message) {
        View lyt_failed = findViewById(R.id.lyt_failed);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction(failed_page));
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.msg_no_item);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            swipeRefreshLayout.setRefreshing(show);
            lyt_shimmer.setVisibility(View.GONE);
            lyt_shimmer.stopShimmer();
            return;
        }
        swipeRefreshLayout.post(() -> {
            swipeRefreshLayout.setRefreshing(show);
            lyt_shimmer.setVisibility(View.VISIBLE);
            lyt_shimmer.startShimmer();
        });
    }

    public void initShimmerLayout() {
        View view_shimmer_2_columns = findViewById(R.id.view_shimmer_2_columns);
        View view_shimmer_3_columns = findViewById(R.id.view_shimmer_3_columns);
        View view_shimmer_2_columns_square = findViewById(R.id.view_shimmer_2_columns_square);
        View view_shimmer_3_columns_square = findViewById(R.id.view_shimmer_3_columns_square);

        if (Config.DISPLAY_WALLPAPER == 1) {
            if (sharedPref.getWallpaperColumns() == 3) {
                view_shimmer_3_columns.setVisibility(View.VISIBLE);
            } else {
                view_shimmer_2_columns.setVisibility(View.VISIBLE);
            }
        } else {
            if (sharedPref.getWallpaperColumns() == 3) {
                view_shimmer_3_columns_square.setVisibility(View.VISIBLE);
            } else {
                view_shimmer_2_columns_square.setVisibility(View.VISIBLE);
            }
        }
    }

    public void onOptionMenuClicked() {

        findViewById(R.id.btn_search).setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), ActivitySearch.class);
            intent.putExtra(Constant.EXTRA_OBJC, "wallpaper");
            startActivity(intent);
        });

        findViewById(R.id.btn_sort).setOnClickListener(view -> {
            String[] items = getResources().getStringArray(R.array.dialog_sort_wallpaper);
            single_choice_selected = items[sharedPref.getCurrentSortWallpaper()];
            int itemSelected = sharedPref.getCurrentSortWallpaper();
            new AlertDialog.Builder(ActivityCategoryDetails.this)
                    .setTitle(getString(R.string.title_sort))
                    .setSingleChoiceItems(items, itemSelected, (dialogInterface, i) -> single_choice_selected = items[i])
                    .setPositiveButton(R.string.dialog_option_ok, (dialogInterface, i) -> {
                        if (callbackCall != null && callbackCall.isExecuted())
                            callbackCall.cancel();
                        adapterWallpaper.resetListData();
                        if (Tools.isConnect(this)) {
                            dbHelper.deleteWallpaperByCategory(DBHelper.TABLE_CATEGORY_DETAIL, category.category_id);
                        }
                        requestAction(1);

                        if (single_choice_selected.equals(getResources().getString(R.string.menu_recent))) {
                            sharedPref.updateSortWallpaper(0);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.menu_featured))) {
                            sharedPref.updateSortWallpaper(1);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.menu_popular))) {
                            sharedPref.updateSortWallpaper(2);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.menu_random))) {
                            sharedPref.updateSortWallpaper(3);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.menu_live))) {
                            sharedPref.updateSortWallpaper(4);
                        } else {
                            sharedPref.updateSortWallpaper(0);
                        }

                        dialogInterface.dismiss();
                    })
                    .show();
        });
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
    public void onDestroy() {
        super.onDestroy();
        swipeProgress(false);
        if (callbackCall != null && callbackCall.isExecuted()) {
            callbackCall.cancel();
        }
        lyt_shimmer.stopShimmer();
    }

}
