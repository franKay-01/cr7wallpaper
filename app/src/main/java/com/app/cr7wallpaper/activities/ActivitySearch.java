package com.app.cr7wallpaper.activities;

import static com.app.cr7wallpaper.utils.Constant.EXTRA_OBJC;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.app.cr7wallpaper.Config;
import com.app.cr7wallpaper.R;
import com.app.cr7wallpaper.adapters.AdapterCategory;
import com.app.cr7wallpaper.adapters.AdapterSearch;
import com.app.cr7wallpaper.adapters.AdapterWallpaper;
import com.app.cr7wallpaper.callbacks.CallbackCategory;
import com.app.cr7wallpaper.callbacks.CallbackWallpaper;
import com.app.cr7wallpaper.databases.prefs.AdsPref;
import com.app.cr7wallpaper.databases.prefs.SharedPref;
import com.app.cr7wallpaper.models.Category;
import com.app.cr7wallpaper.models.Wallpaper;
import com.app.cr7wallpaper.rests.ApiInterface;
import com.app.cr7wallpaper.rests.RestAdapter;
import com.app.cr7wallpaper.utils.AdsManager;
import com.app.cr7wallpaper.utils.Constant;
import com.app.cr7wallpaper.utils.ItemOffsetDecoration;
import com.app.cr7wallpaper.utils.Tools;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.android.material.snackbar.Snackbar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ActivitySearch extends AppCompatActivity {

    private EditText edt_search;
    private EditText edt_index;
    private RecyclerView recycler_view_wallpaper;
    private RecyclerView recycler_view_category;
    private RecyclerView recycler_view_suggestion;
    private AdapterWallpaper adapterWallpaper;
    private AdapterCategory adapterCategory;
    private AdapterSearch mAdapterSuggestion;
    private LinearLayout lyt_suggestion;
    private ImageButton bt_clear;
    private Call<CallbackWallpaper> callbackCallWallpaper = null;
    private Call<CallbackCategory> callbackCallCategory = null;
    private ShimmerFrameLayout lyt_shimmer;
    private RelativeLayout view_shimmer_wallpaper;
    private RelativeLayout view_shimmer_category;
    private int post_total = 0;
    private int failed_page = 0;
    String tags = "";
    List<Wallpaper> wallpapers = new ArrayList<>();
    List<Category> categories = new ArrayList<>();
    SharedPref sharedPref;
    AdsPref adsPref;
    CoordinatorLayout parent_view;
    RadioGroup radio_group_search;
    String data;
    String flag_type;
    AdsManager adsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tools.getTheme(this);
        adsPref = new AdsPref(this);
        sharedPref = new SharedPref(this);
        if (sharedPref.getIsDarkTheme()) {
            Tools.darkNavigation(this);
        } else {
            Tools.lightNavigation(this);
        }
        setContentView(R.layout.activity_search);
        Tools.getRtlDirection(this);
        Tools.resetAppOpenAdToken(this);
        initComponent();
        initShimmerLayout();
        setupToolbar();

        adsManager = new AdsManager(this);
        adsManager.loadBannerAd(adsPref.getBannerAdStatusSearch());
        adsManager.loadInterstitialAd(adsPref.getInterstitialAdClickWallpaper(), adsPref.getInterstitialAdInterval());
    }

    private void initComponent() {

        Intent intent = getIntent();
        data = intent.getStringExtra(Constant.EXTRA_OBJC);

        parent_view = findViewById(R.id.coordinatorLayout);
        view_shimmer_wallpaper = findViewById(R.id.view_shimmer_wallpaper);
        view_shimmer_category = findViewById(R.id.view_shimmer_category);
        radio_group_search = findViewById(R.id.radioGroupSearch);
        edt_index = findViewById(R.id.edt_index);

        initRecyclerView();

        lyt_suggestion = findViewById(R.id.lyt_suggestion);
        edt_search = findViewById(R.id.et_search);
        bt_clear = findViewById(R.id.bt_clear);
        bt_clear.setVisibility(View.GONE);
        lyt_shimmer = findViewById(R.id.shimmer_view_container);

        edt_search.addTextChangedListener(textWatcher);
        if (getIntent().hasExtra("tags")) {
            tags = getIntent().getStringExtra("tags");
            hideKeyboard();
            searchActionTags(1);
        } else {
            edt_search.requestFocus();
            swipeProgress(false);
        }

        recycler_view_suggestion.setLayoutManager(new LinearLayoutManager(this));
        recycler_view_suggestion.setHasFixedSize(true);

        //set data and list adapter suggestion
        mAdapterSuggestion = new AdapterSearch(this);
        recycler_view_suggestion.setAdapter(mAdapterSuggestion);
        showSuggestionSearch();
        mAdapterSuggestion.setOnItemClickListener((view, viewModel, pos) -> {
            edt_search.setText(viewModel);
            lyt_suggestion.setVisibility(View.GONE);
            hideKeyboard();
            searchActionWallpaper(1);
        });

        bt_clear.setOnClickListener(view -> edt_search.setText(""));

        edt_search.setOnTouchListener((view, motionEvent) -> {
            showSuggestionSearch();
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
            return false;
        });

        if (data.equals("category")) {
            radio_group_search.check(radio_group_search.getChildAt(1).getId());
            requestSearchCategory();
        } else {
            radio_group_search.check(radio_group_search.getChildAt(0).getId());
            requestSearchWallpaper();
        }

        flag_type = edt_index.getText().toString();
        edt_index.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                flag_type = edt_index.getText().toString();
                showKeyboard();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        radio_group_search.setOnCheckedChangeListener((radioGroup, i) -> {
            switch (i) {
                case R.id.radio_button_wallpaper:
                    requestSearchWallpaper();
                    recycler_view_wallpaper.setVisibility(View.VISIBLE);
                    recycler_view_category.setVisibility(View.GONE);
                    findViewById(R.id.lyt_no_item).setVisibility(View.GONE);
                    break;
                case R.id.radio_button_category:
                    requestSearchCategory();
                    recycler_view_wallpaper.setVisibility(View.GONE);
                    recycler_view_category.setVisibility(View.VISIBLE);
                    findViewById(R.id.lyt_no_item).setVisibility(View.GONE);
                    break;
            }
        });

    }

    public void initRecyclerView() {
        recycler_view_wallpaper = findViewById(R.id.recycler_view_wallpaper);
        recycler_view_category = findViewById(R.id.recycler_view_category);
        recycler_view_suggestion = findViewById(R.id.recycler_view_suggestion);
    }

    public void requestSearchWallpaper() {
        edt_index.setText("0");
        recycler_view_wallpaper.setVisibility(View.VISIBLE);
        recycler_view_category.setVisibility(View.GONE);
        view_shimmer_wallpaper.setVisibility(View.VISIBLE);
        view_shimmer_category.setVisibility(View.GONE);

        recycler_view_wallpaper.setLayoutManager(new StaggeredGridLayoutManager(sharedPref.getWallpaperColumns(), StaggeredGridLayoutManager.VERTICAL));
        recycler_view_wallpaper.setHasFixedSize(true);

        //set data and list adapter
        adapterWallpaper = new AdapterWallpaper(this, recycler_view_wallpaper, wallpapers);
        recycler_view_wallpaper.setAdapter(adapterWallpaper);
        adapterWallpaper.setOnItemClickListener((view, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityWallpaperDetail.class);
            intent.putExtra(Constant.POSITION, position);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constant.ARRAY_LIST, (Serializable) wallpapers);
            intent.putExtra(Constant.BUNDLE, bundle);
            intent.putExtra(EXTRA_OBJC, obj);
            startActivity(intent);

            adsManager.showInterstitialAd();
        });

        recycler_view_wallpaper.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView v, int state) {
                super.onScrollStateChanged(v, state);
            }
        });

        // detect when scroll reach bottom
        adapterWallpaper.setOnLoadMoreListener(current_page -> {
            if (adsPref.getNativeAdWallpaperList() != 0) {
                if (adsPref.getAdType().equals("unity")) {
                    setLoadMore(current_page);
                } else {
                    setLoadMoreNativeAd(current_page);
                }
            } else {
                setLoadMore(current_page);
            }
        });

        edt_search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (edt_search.getText().toString().equals("")) {
                    Snackbar.make(parent_view, getString(R.string.msg_search_input), Snackbar.LENGTH_SHORT).show();
                    hideKeyboard();
                    swipeProgress(false);
                } else {
                    adapterWallpaper.resetListData();
                    hideKeyboard();
                    searchActionWallpaper(1);
                }
                return true;
            }
            return false;
        });

    }

    public void setLoadMoreNativeAd(int current_page) {
        Log.d("page", "currentPage: " + current_page);
        // Assuming final total items equal to real post items plus the ad
        int totalItemBeforeAds = (adapterWallpaper.getItemCount() - current_page);
        if (post_total > totalItemBeforeAds && current_page != 0) {
            int next_page = current_page + 1;
            searchActionWallpaper(next_page);
        } else {
            adapterWallpaper.setLoaded();
        }
    }

    public void setLoadMore(int current_page) {
        if (post_total > adapterWallpaper.getItemCount() && current_page != 0) {
            int next_page = current_page + 1;
            searchActionWallpaper(next_page);
        } else {
            adapterWallpaper.setLoaded();
        }
    }

    public void requestSearchCategory() {
        edt_index.setText("1");
        recycler_view_wallpaper.setVisibility(View.GONE);
        recycler_view_category.setVisibility(View.VISIBLE);
        view_shimmer_wallpaper.setVisibility(View.GONE);
        view_shimmer_category.setVisibility(View.VISIBLE);

        ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(this, R.dimen.grid_space_wallpaper);
        int padding = getResources().getDimensionPixelOffset(R.dimen.grid_space_wallpaper);
        recycler_view_category.setPadding(padding, padding, padding, padding);
        recycler_view_category.setLayoutManager(new StaggeredGridLayoutManager(Config.DEFAULT_CATEGORY_COLUMN, LinearLayoutManager.VERTICAL));
        if (0 == recycler_view_category.getItemDecorationCount()) {
            recycler_view_category.addItemDecoration(itemDecoration);
        }
        recycler_view_category.setHasFixedSize(true);

        //set data and list adapter
        adapterCategory = new AdapterCategory(this, categories);
        recycler_view_category.setAdapter(adapterCategory);

        // on item list clicked
        adapterCategory.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getApplicationContext(), ActivityCategoryDetails.class);
            intent.putExtra(EXTRA_OBJC, obj);
            startActivity(intent);
            adsManager.showInterstitialAd();
        });

        edt_search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (edt_search.getText().toString().equals("")) {
                    Snackbar.make(parent_view, getString(R.string.msg_search_input), Snackbar.LENGTH_SHORT).show();
                    hideKeyboard();
                    swipeProgress(false);
                } else {
                    adapterCategory.resetListData();
                    hideKeyboard();
                    searchActionCategory();
                }
                return true;
            }
            return false;
        });

    }

    public void setupToolbar() {
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (sharedPref.getIsDarkTheme()) {
            Tools.darkToolbar(this, toolbar);
        } else {
            Tools.lightToolbar(this, toolbar);
        }
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setTitle("");
        }
    }

    TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void onTextChanged(CharSequence c, int i, int i1, int i2) {
            if (c.toString().trim().length() == 0) {
                bt_clear.setVisibility(View.GONE);
            } else {
                bt_clear.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void beforeTextChanged(CharSequence c, int i, int i1, int i2) {
        }

        @Override
        public void afterTextChanged(Editable editable) {
        }
    };

    private void requestSearchApiWallpaper(final int page_no, final String query) {
        ApiInterface apiInterface = RestAdapter.createAPI();

        if (sharedPref.getWallpaperColumns() == 3) {
            callbackCallWallpaper = apiInterface.getSearch(page_no, Constant.LOAD_MORE_3_COLUMNS, query, Constant.ORDER_RECENT);
        } else {
            callbackCallWallpaper = apiInterface.getSearch(page_no, Constant.LOAD_MORE_2_COLUMNS, query, Constant.ORDER_RECENT);
        }

        callbackCallWallpaper.enqueue(new Callback<CallbackWallpaper>() {
            @Override
            public void onResponse(Call<CallbackWallpaper> call, Response<CallbackWallpaper> response) {
                CallbackWallpaper resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    post_total = resp.count_total;
                    if (adsPref.getNativeAdWallpaperList() != 0) {
                        if (adsPref.getAdType().equals("unity")) {
                            adapterWallpaper.insertData(resp.posts);
                        } else {
                            adapterWallpaper.insertDataWithNativeAd(resp.posts);
                        }
                    } else {
                        adapterWallpaper.insertData(resp.posts);
                    }
                    if (resp.posts.size() == 0) showNotFoundViewWallpaper(true);
                } else {
                    onFailRequestWallpaper(page_no);
                }
                swipeProgress(false);
            }

            @Override
            public void onFailure(Call<CallbackWallpaper> call, Throwable t) {
                onFailRequestWallpaper(page_no);
                swipeProgress(false);
            }

        });
    }

    private void requestSearchApiCategory(final String query) {
        ApiInterface apiInterface = RestAdapter.createAPI();
        callbackCallCategory = apiInterface.getSearchCategory(query);
        callbackCallCategory.enqueue(new Callback<CallbackCategory>() {
            @Override
            public void onResponse(Call<CallbackCategory> call, Response<CallbackCategory> response) {
                CallbackCategory resp = response.body();
                if (resp != null && resp.status.equals("ok")) {
                    adapterCategory.insertData(resp.categories);
                    swipeProgress(false);
                    if (resp.categories.size() == 0) showNotFoundViewCategory(true);
                } else {
                    onFailRequestCategory();
                }
            }

            @Override
            public void onFailure(Call<CallbackCategory> call, Throwable t) {
                onFailRequestCategory();
                swipeProgress(false);
            }

        });
    }

    private void onFailRequestWallpaper(int page_no) {
        failed_page = page_no;
        adapterWallpaper.setLoaded();
        swipeProgress(false);
        if (Tools.isConnect(this)) {
            showFailedViewWallpaper(true, getString(R.string.failed_text));
        } else {
            showFailedViewWallpaper(true, getString(R.string.failed_text));
        }
    }

    private void onFailRequestCategory() {
        swipeProgress(false);
        if (Tools.isConnect(this)) {
            showFailedViewCategory(true, getString(R.string.failed_text));
        } else {
            showFailedViewCategory(true, getString(R.string.failed_text));
        }
    }

    private void searchActionWallpaper(final int page_no) {
        lyt_suggestion.setVisibility(View.GONE);
        showFailedViewWallpaper(false, "");
        showNotFoundViewWallpaper(false);
        final String query = edt_search.getText().toString().trim();
        if (!query.equals("")) {
            if (page_no == 1) {
                swipeProgress(true);
            } else {
                adapterWallpaper.setLoading();
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> requestSearchApiWallpaper(page_no, query), Constant.DELAY_TIME);
        } else {
            Snackbar.make(parent_view, getString(R.string.msg_search_input), Snackbar.LENGTH_SHORT).show();
            swipeProgress(false);
        }
    }

    private void searchActionCategory() {
        lyt_suggestion.setVisibility(View.GONE);
        showFailedViewCategory(false, "");
        showNotFoundViewCategory(false);
        final String query = edt_search.getText().toString().trim();
        if (!query.equals("")) {
            swipeProgress(true);
            new Handler(Looper.getMainLooper()).postDelayed(() -> requestSearchApiCategory(query), Constant.DELAY_TIME);
        } else {
            Snackbar.make(parent_view, getString(R.string.msg_search_input), Snackbar.LENGTH_SHORT).show();
            swipeProgress(false);
        }
    }

    private void searchActionTags(final int page_no) {
        lyt_suggestion.setVisibility(View.GONE);
        showFailedViewWallpaper(false, "");
        showNotFoundViewWallpaper(false);
        edt_search.setText(tags);
        final String query = edt_search.getText().toString().trim();
        if (!query.equals("")) {
            if (page_no == 1) {
                swipeProgress(true);
            } else {
                adapterWallpaper.setLoading();
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> requestSearchApiWallpaper(page_no, query), Constant.DELAY_TIME);
        } else {
            Snackbar.make(parent_view, getString(R.string.msg_search_input), Snackbar.LENGTH_SHORT).show();
            swipeProgress(false);
        }
    }

    private void showSuggestionSearch() {
        mAdapterSuggestion.refreshItems();
        lyt_suggestion.setVisibility(View.VISIBLE);
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showKeyboard() {
        edt_search.requestFocus();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);
    }

    private void showFailedViewWallpaper(boolean show, String message) {
        View lyt_failed = findViewById(R.id.lyt_failed);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recycler_view_wallpaper.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recycler_view_wallpaper.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> searchActionWallpaper(failed_page));
    }

    private void showFailedViewCategory(boolean show, String message) {
        View lyt_failed = findViewById(R.id.lyt_failed);
        ((TextView) findViewById(R.id.failed_message)).setText(message);
        if (show) {
            recycler_view_category.setVisibility(View.GONE);
            lyt_failed.setVisibility(View.VISIBLE);
        } else {
            recycler_view_category.setVisibility(View.VISIBLE);
            lyt_failed.setVisibility(View.GONE);
        }
        findViewById(R.id.failed_retry).setOnClickListener(view -> searchActionCategory());
    }

    private void showNotFoundViewWallpaper(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.no_search_wallpaper_found);
        if (show) {
            recycler_view_wallpaper.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recycler_view_wallpaper.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void showNotFoundViewCategory(boolean show) {
        View lyt_no_item = findViewById(R.id.lyt_no_item);
        ((TextView) findViewById(R.id.no_item_message)).setText(R.string.no_search_category_found);
        if (show) {
            recycler_view_category.setVisibility(View.GONE);
            lyt_no_item.setVisibility(View.VISIBLE);
        } else {
            recycler_view_category.setVisibility(View.VISIBLE);
            lyt_no_item.setVisibility(View.GONE);
        }
    }

    private void swipeProgress(final boolean show) {
        if (!show) {
            lyt_shimmer.setVisibility(View.GONE);
            lyt_shimmer.stopShimmer();
            return;
        } else {
            lyt_shimmer.setVisibility(View.VISIBLE);
            lyt_shimmer.startShimmer();
        }
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

    @Override
    public void onBackPressed() {
        if (getIntent().hasExtra("tags")) {
            super.onBackPressed();
        } else {
            if (edt_search.length() > 0) {
                edt_search.setText("");
            } else {
                super.onBackPressed();
            }
        }
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

}
