package com.app.cr7wallpaper.fragments;

import static com.app.cr7wallpaper.utils.Constant.EXTRA_OBJC;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.app.cr7wallpaper.Config;
import com.app.cr7wallpaper.R;
import com.app.cr7wallpaper.activities.ActivityCategoryDetails;
import com.app.cr7wallpaper.activities.MainActivity;
import com.app.cr7wallpaper.adapters.AdapterCategory;
import com.app.cr7wallpaper.callbacks.CallbackCategory;
import com.app.cr7wallpaper.databases.prefs.AdsPref;
import com.app.cr7wallpaper.databases.prefs.SharedPref;
import com.app.cr7wallpaper.databases.sqlite.DBHelper;
import com.app.cr7wallpaper.models.Category;
import com.app.cr7wallpaper.rests.ApiInterface;
import com.app.cr7wallpaper.rests.RestAdapter;
import com.app.cr7wallpaper.utils.AdsManager;
import com.app.cr7wallpaper.utils.Constant;
import com.app.cr7wallpaper.utils.ItemOffsetDecoration;
import com.app.cr7wallpaper.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FragmentCategory extends Fragment {

    private View root_view;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ShimmerFrameLayout lyt_shimmer;
    private AdapterCategory adapterCategory;
    private Call<CallbackCategory> callbackCall = null;
    private SharedPref sharedPref;
    DBHelper dbHelper;
    LinearLayout parent_view;
    AdsPref adsPref;
    AdsManager adsManager;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root_view = inflater.inflate(R.layout.fragment_category, container, false);
        dbHelper = new DBHelper(getActivity());
        sharedPref = new SharedPref(getActivity());
        adsPref = new AdsPref(getActivity());
        adsManager = new AdsManager(getActivity());

        parent_view = root_view.findViewById(R.id.parent_view);
        swipeRefreshLayout = root_view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        lyt_shimmer = root_view.findViewById(R.id.shimmer_view_container);

        if (sharedPref.getIsDarkTheme()) {
            parent_view.setBackgroundColor(getResources().getColor(R.color.colorBackgroundDark));
        } else {
            parent_view.setBackgroundColor(getResources().getColor(R.color.colorBackgroundLight));
        }

        recyclerView = root_view.findViewById(R.id.recyclerView);
        int padding = getResources().getDimensionPixelOffset(R.dimen.grid_space_wallpaper);
        recyclerView.setPadding(padding, padding, padding, padding);

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(Config.DEFAULT_CATEGORY_COLUMN, LinearLayoutManager.VERTICAL));
        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(getActivity(), R.dimen.grid_space_wallpaper);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setHasFixedSize(true);

        //set data and list adapter
        adapterCategory = new AdapterCategory(getActivity(), new ArrayList<>());
        recyclerView.setAdapter(adapterCategory);

        // on item list clicked
        adapterCategory.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityCategoryDetails.class);
            intent.putExtra(EXTRA_OBJC, obj);
            startActivity(intent);

            ((MainActivity) getActivity()).showInterstitialAd();
        });

        // on swipe list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            adapterCategory.resetListData();
            requestAction();
        });

        requestAction();

        return root_view;
    }

    private void displayApiResult(final List<Category> categories) {
        adapterCategory.setListData(categories);
        swipeProgress(false);
        if (categories.size() == 0) {
            showNoItemView(true);
        }
    }

    private void requestCategoriesApi() {
        ApiInterface apiInterface = RestAdapter.createAPI();
        callbackCall = apiInterface.getCategories();
        callbackCall.enqueue(new Callback<CallbackCategory>() {
            @Override
            public void onResponse(Call<CallbackCategory> call, Response<CallbackCategory> response) {
                CallbackCategory resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    displayApiResult(resp.categories);
                    dbHelper.truncateTableCategory(DBHelper.TABLE_CATEGORY);
                    dbHelper.addListCategory(resp.categories, DBHelper.TABLE_CATEGORY);
                } else {
                    onFailRequest();
                }
            }

            @Override
            public void onFailure(Call<CallbackCategory> call, Throwable t) {
                swipeProgress(false);
                List<Category> posts = dbHelper.getAllCategory(DBHelper.TABLE_CATEGORY);
                adapterCategory.setListData(posts);
                if (posts.size() == 0) {
                    if (!call.isCanceled()) onFailRequest();
                }
            }

        });
    }

    private void onFailRequest() {
        swipeProgress(false);
        if (Tools.isConnect(getActivity())) {
            showFailedView(true, getString(R.string.failed_text));
        } else {
            showFailedView(true, getString(R.string.failed_text));
        }
    }

    private void requestAction() {
        showFailedView(false, "");
        swipeProgress(true);
        showNoItemView(false);
        new Handler(Looper.getMainLooper()).postDelayed(this::requestCategoriesApi, Constant.DELAY_TIME);
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

    private void showFailedView(boolean flag, String message) {
        View lyt_failed = root_view.findViewById(R.id.lyt_failed_category);
        ((TextView) root_view.findViewById(R.id.failed_message)).setText(message);
        if (flag) {
            recyclerView.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        root_view.findViewById(R.id.failed_retry).setOnClickListener(view -> requestAction());
    }

    private void showNoItemView(boolean show) {
        View lyt_no_item = root_view.findViewById(R.id.lyt_no_item_category);
        ((TextView) root_view.findViewById(R.id.no_item_message)).setText(R.string.no_category_found);
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

}
