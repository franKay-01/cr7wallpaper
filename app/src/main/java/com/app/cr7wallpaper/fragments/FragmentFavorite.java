package com.app.cr7wallpaper.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.app.cr7wallpaper.R;
import com.app.cr7wallpaper.activities.ActivityWallpaperDetail;
import com.app.cr7wallpaper.adapters.AdapterWallpaper;
import com.app.cr7wallpaper.databases.prefs.AdsPref;
import com.app.cr7wallpaper.databases.prefs.SharedPref;
import com.app.cr7wallpaper.databases.sqlite.DBHelper;
import com.app.cr7wallpaper.models.Wallpaper;
import com.app.cr7wallpaper.utils.AdsManager;
import com.app.cr7wallpaper.utils.Constant;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class FragmentFavorite extends Fragment {

    private View root_view;
    RelativeLayout parent_view;
    private RecyclerView recyclerView;
    private AdapterWallpaper adapterWallpaper;
    SharedPref sharedPref;
    DBHelper dbHelper;
    View lyt_no_favorite;
    List<Wallpaper> items = new ArrayList<>();
    AdsManager adsManager;
    AdsPref adsPref;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        root_view = inflater.inflate(R.layout.fragment_favorite, container, false);
        lyt_no_favorite = root_view.findViewById(R.id.lyt_not_found);
        dbHelper = new DBHelper(getActivity());
        sharedPref = new SharedPref(getActivity());
        adsManager = new AdsManager(getActivity());
        adsPref = new AdsPref(getActivity());

        parent_view = root_view.findViewById(R.id.parent_view);
        if (sharedPref.getIsDarkTheme()) {
            parent_view.setBackgroundColor(getResources().getColor(R.color.colorBackgroundDark));
        } else {
            parent_view.setBackgroundColor(getResources().getColor(R.color.colorBackgroundLight));
        }

        recyclerView = root_view.findViewById(R.id.recyclerView);
        //ItemOffsetDecoration itemDecoration = new ItemOffsetDecoration(getActivity(), R.dimen.grid_space_wallpaper);
        //recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(sharedPref.getWallpaperColumns(), StaggeredGridLayoutManager.VERTICAL));
        recyclerView.setHasFixedSize(true);

        adapterWallpaper = new AdapterWallpaper(getActivity(), recyclerView, items);
        recyclerView.setAdapter(adapterWallpaper);

        displayData();

        return root_view;
    }

    private void displayData() {
        List<Wallpaper> list = dbHelper.getAllFavorite(DBHelper.TABLE_FAVORITE);
        adapterWallpaper.setItems(list);
        if (list.size() == 0) {
            lyt_no_favorite.setVisibility(View.VISIBLE);
        } else {
            lyt_no_favorite.setVisibility(View.GONE);
        }

        adapterWallpaper.setOnItemClickListener((v, obj, position) -> {
            Intent intent = new Intent(getActivity(), ActivityWallpaperDetail.class);
            intent.putExtra(Constant.POSITION, position);
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constant.ARRAY_LIST, (Serializable) list);
            intent.putExtra(Constant.BUNDLE, bundle);
            intent.putExtra(Constant.EXTRA_OBJC, obj);
            startActivity(intent);
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        displayData();
    }

}
