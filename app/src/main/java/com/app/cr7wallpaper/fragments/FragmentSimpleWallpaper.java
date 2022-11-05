package com.app.cr7wallpaper.fragments;

import static com.app.cr7wallpaper.utils.Constant.FILTER_ALL;
import static com.app.cr7wallpaper.utils.Constant.FILTER_LIVE;
import static com.app.cr7wallpaper.utils.Constant.ORDER_FEATURED;
import static com.app.cr7wallpaper.utils.Constant.ORDER_LIVE;
import static com.app.cr7wallpaper.utils.Constant.ORDER_POPULAR;
import static com.app.cr7wallpaper.utils.Constant.ORDER_RANDOM;
import static com.app.cr7wallpaper.utils.Constant.ORDER_RECENT;
import static com.solodroid.ads.sdk.util.Constant.APPLOVIN;
import static com.solodroid.ads.sdk.util.Constant.MOPUB;
import static com.solodroid.ads.sdk.util.Constant.UNITY;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.cr7wallpaper.Config;
import com.app.cr7wallpaper.R;
import com.app.cr7wallpaper.activities.ActivityWallpaperDetail;
import com.app.cr7wallpaper.activities.MainActivity;
import com.app.cr7wallpaper.adapters.AdapterWallpaper;
import com.app.cr7wallpaper.callbacks.CallbackWallpaper;
import com.app.cr7wallpaper.databases.prefs.AdsPref;
import com.app.cr7wallpaper.databases.prefs.SharedPref;
import com.app.cr7wallpaper.databases.sqlite.DBHelper;
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

public class FragmentSimpleWallpaper extends Fragment {

    View root_view;
    private RecyclerView recyclerView;
    private AdapterWallpaper adapterWallpaper;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout lyt_shimmer;
    private Call<CallbackWallpaper> callbackCall = null;
    private int post_total = 0;
    private int failed_page = 0;
    private SharedPref sharedPref;
    private AdsPref adsPref;
    List<Wallpaper> items = new ArrayList<>();
    RelativeLayout parent_view;
    DBHelper dbHelper;
    private String single_choice_selected;
    AdsManager adsManager;

    public FragmentSimpleWallpaper() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root_view = inflater.inflate(R.layout.fragment_simple_wallpaper, container, false);
        setHasOptionsMenu(true);

        dbHelper = new DBHelper(getActivity());
        sharedPref = new SharedPref(getActivity());
        sharedPref.setDefaultSortWallpaper();
        adsPref = new AdsPref(getActivity());
        adsManager = new AdsManager(getActivity());

        parent_view = root_view.findViewById(R.id.parent_view);
        if (sharedPref.getIsDarkTheme()) {
            parent_view.setBackgroundColor(getResources().getColor(R.color.colorBackgroundDark));
        } else {
            parent_view.setBackgroundColor(getResources().getColor(R.color.colorBackgroundLight));
        }

        swipeRefreshLayout = root_view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        lyt_shimmer = root_view.findViewById(R.id.shimmer_view_container);
        initShimmerLayout();

        recyclerView = root_view.findViewById(R.id.recyclerView);
        //ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(getActivity(), R.dimen.grid_space_wallpaper);
        //recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(sharedPref.getWallpaperColumns(), StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setHasFixedSize(true);

        //set data and list adapter
        adapterWallpaper = new AdapterWallpaper(getActivity(), recyclerView, items);
        recyclerView.setAdapter(adapterWallpaper);

        // on item list clicked
        adapterWallpaper.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityWallpaperDetail.class);
            intent.putExtra(Constant.POSITION, position);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constant.ARRAY_LIST, (Serializable) items);
            intent.putExtra(Constant.BUNDLE, bundle);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);

            ((MainActivity) getActivity()).showInterstitialAd();
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
                switch (adsPref.getAdType()) {
                    case UNITY:
                    case APPLOVIN:
                    case MOPUB:
                        setLoadMore(current_page);
                        break;
                    default:
                        setLoadMoreNativeAd(current_page);
                        break;
                }
            } else {
                setLoadMore(current_page);
            }
        });

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            if (callbackCall != null && callbackCall.isExecuted()) callbackCall.cancel();
            adapterWallpaper.resetListData();
            if (Tools.isConnect(getActivity())) {
                dbHelper.deleteAll(DBHelper.TABLE_RECENT);
            }
            requestAction(1);
        });

        requestAction(1);

        return root_view;
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
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
                callbackCall = apiInterface.getWallpapers(page_no, Constant.LOAD_MORE_3_COLUMNS, FILTER_ALL, ORDER_RECENT);
            } else if (sharedPref.getCurrentSortWallpaper() == 1) {
                callbackCall = apiInterface.getWallpapers(page_no, Constant.LOAD_MORE_3_COLUMNS, FILTER_ALL, ORDER_FEATURED);
            } else if (sharedPref.getCurrentSortWallpaper() == 2) {
                callbackCall = apiInterface.getWallpapers(page_no, Constant.LOAD_MORE_3_COLUMNS, FILTER_ALL, ORDER_POPULAR);
            } else if (sharedPref.getCurrentSortWallpaper() == 3) {
                callbackCall = apiInterface.getWallpapers(page_no, Constant.LOAD_MORE_3_COLUMNS, FILTER_ALL, ORDER_RANDOM);
            } else if (sharedPref.getCurrentSortWallpaper() == 4) {
                callbackCall = apiInterface.getWallpapers(page_no, Constant.LOAD_MORE_3_COLUMNS, FILTER_LIVE, ORDER_LIVE);
            }
        } else {
            if (sharedPref.getCurrentSortWallpaper() == 0) {
                callbackCall = apiInterface.getWallpapers(page_no, Constant.LOAD_MORE_2_COLUMNS, FILTER_ALL, ORDER_RECENT);
            } else if (sharedPref.getCurrentSortWallpaper() == 1) {
                callbackCall = apiInterface.getWallpapers(page_no, Constant.LOAD_MORE_2_COLUMNS, FILTER_ALL, ORDER_FEATURED);
            } else if (sharedPref.getCurrentSortWallpaper() == 2) {
                callbackCall = apiInterface.getWallpapers(page_no, Constant.LOAD_MORE_2_COLUMNS, FILTER_ALL, ORDER_POPULAR);
            } else if (sharedPref.getCurrentSortWallpaper() == 3) {
                callbackCall = apiInterface.getWallpapers(page_no, Constant.LOAD_MORE_2_COLUMNS, FILTER_ALL, ORDER_RANDOM);
            } else if (sharedPref.getCurrentSortWallpaper() == 4) {
                callbackCall = apiInterface.getWallpapers(page_no, Constant.LOAD_MORE_2_COLUMNS, FILTER_LIVE, ORDER_LIVE);
            }
        }

        callbackCall.enqueue(new Callback<CallbackWallpaper>() {
            @Override
            public void onResponse(Call<CallbackWallpaper> call, Response<CallbackWallpaper> response) {
                CallbackWallpaper resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    post_total = resp.count_total;
                    displayApiResult(resp.posts);
                    if (sharedPref.getCurrentSortWallpaper() == 0) {
                        if (page_no == 1)
                            dbHelper.truncateTableWallpaper(DBHelper.TABLE_RECENT);
                        dbHelper.addListWallpaper(resp.posts, DBHelper.TABLE_RECENT);
                    } else if (sharedPref.getCurrentSortWallpaper() == 1) {
                        if (page_no == 1)
                            dbHelper.truncateTableWallpaper(DBHelper.TABLE_FEATURED);
                        dbHelper.addListWallpaper(resp.posts, DBHelper.TABLE_FEATURED);
                    } else if (sharedPref.getCurrentSortWallpaper() == 2) {
                        if (page_no == 1)
                            dbHelper.truncateTableWallpaper(DBHelper.TABLE_POPULAR);
                        dbHelper.addListWallpaper(resp.posts, DBHelper.TABLE_POPULAR);
                    } else if (sharedPref.getCurrentSortWallpaper() == 3) {
                        if (page_no == 1)
                            dbHelper.truncateTableWallpaper(DBHelper.TABLE_RANDOM);
                        dbHelper.addListWallpaper(resp.posts, DBHelper.TABLE_RANDOM);
                    } else if (sharedPref.getCurrentSortWallpaper() == 4) {
                        if (page_no == 1)
                            dbHelper.truncateTableWallpaper(DBHelper.TABLE_GIF);
                        dbHelper.addListWallpaper(resp.posts, DBHelper.TABLE_GIF);
                    }
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
        if (sharedPref.getCurrentSortWallpaper() == 0) {
            List<Wallpaper> wallpapers = dbHelper.getAllWallpaper(DBHelper.TABLE_RECENT);
            insertData(wallpapers);
            if (wallpapers.size() == 0) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }
        } else if (sharedPref.getCurrentSortWallpaper() == 1) {
            List<Wallpaper> wallpapers = dbHelper.getAllWallpaper(DBHelper.TABLE_FEATURED);
            insertData(wallpapers);
            if (wallpapers.size() == 0) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }
        } else if (sharedPref.getCurrentSortWallpaper() == 2) {
            List<Wallpaper> wallpapers = dbHelper.getAllWallpaper(DBHelper.TABLE_POPULAR);
            insertData(wallpapers);
            if (wallpapers.size() == 0) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }
        } else if (sharedPref.getCurrentSortWallpaper() == 3) {
            List<Wallpaper> wallpapers = dbHelper.getAllWallpaper(DBHelper.TABLE_RANDOM);
            insertData(wallpapers);
            if (wallpapers.size() == 0) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }
        } else if (sharedPref.getCurrentSortWallpaper() == 4) {
            List<Wallpaper> wallpapers = dbHelper.getAllWallpaper(DBHelper.TABLE_GIF);
            insertData(wallpapers);
            if (wallpapers.size() == 0) {
                if (!call.isCanceled()) onFailRequest(page_no);
            }
        }
    }

    private void insertData(List<Wallpaper> wallpapers) {
        if (adsPref.getNativeAdWallpaperList() != 0) {
            if (adsPref.getAdType().equals(UNITY)) {
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
        if (Tools.isConnect(getActivity())) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    public void requestAction(final int page_no) {
        showFailedView(false, "");
        showNoItemView(false);
        if (page_no == 1) {
            swipeProgress(true);
        } else {
            adapterWallpaper.setLoading();
        }
        new Handler(Looper.getMainLooper()).postDelayed(() -> requestListPostApi(page_no), Constant.DELAY_TIME);
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

    private void showFailedView(boolean show, String message) {
        View lyt_failed = root_view.findViewById(R.id.lyt_failed);
        ((TextView) root_view.findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        root_view.findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction(failed_page));
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = root_view.findViewById(R.id.lyt_no_item);
        ((TextView) root_view.findViewById(R.id.no_item_message)).setText(R.string.msg_no_item);
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
        View view_shimmer_2_columns = root_view.findViewById(R.id.view_shimmer_2_columns);
        View view_shimmer_3_columns = root_view.findViewById(R.id.view_shimmer_3_columns);
        View view_shimmer_2_columns_square = root_view.findViewById(R.id.view_shimmer_2_columns_square);
        View view_shimmer_3_columns_square = root_view.findViewById(R.id.view_shimmer_3_columns_square);

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_sort) {
            String[] items = getResources().getStringArray(R.array.dialog_sort_wallpaper);
            single_choice_selected = items[sharedPref.getCurrentSortWallpaper()];
            int itemSelected = sharedPref.getCurrentSortWallpaper();
            new AlertDialog.Builder(getActivity())
                    .setTitle(getString(R.string.title_sort))
                    .setSingleChoiceItems(items, itemSelected, (dialogInterface, i) -> single_choice_selected = items[i])
                    .setPositiveButton(R.string.dialog_option_ok, (dialogInterface, i) -> {

                        if (callbackCall != null && callbackCall.isExecuted())
                            callbackCall.cancel();
                        adapterWallpaper.resetListData();
                        requestAction(1);

                        if (single_choice_selected.equals(getResources().getString(R.string.menu_recent))) {
                            sharedPref.updateSortWallpaper(0);
                            dbHelper.deleteAll(DBHelper.TABLE_RECENT);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.menu_featured))) {
                            sharedPref.updateSortWallpaper(1);
                            dbHelper.deleteAll(DBHelper.TABLE_FEATURED);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.menu_popular))) {
                            sharedPref.updateSortWallpaper(2);
                            dbHelper.deleteAll(DBHelper.TABLE_POPULAR);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.menu_random))) {
                            sharedPref.updateSortWallpaper(3);
                            dbHelper.deleteAll(DBHelper.TABLE_RANDOM);
                        } else if (single_choice_selected.equals(getResources().getString(R.string.menu_live))) {
                            sharedPref.updateSortWallpaper(4);
                            dbHelper.deleteAll(DBHelper.TABLE_GIF);
                        }

                        dialogInterface.dismiss();
                    })
                    .show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

}