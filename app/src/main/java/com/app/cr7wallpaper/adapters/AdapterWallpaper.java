package com.app.cr7wallpaper.adapters;

import static com.solodroid.ads.sdk.util.Constant.APPLOVIN;
import static com.solodroid.ads.sdk.util.Constant.MOPUB;
import static com.solodroid.ads.sdk.util.Constant.UNITY;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.app.cr7wallpaper.Config;
import com.app.cr7wallpaper.R;
import com.app.cr7wallpaper.databases.prefs.AdsPref;
import com.app.cr7wallpaper.databases.prefs.SharedPref;
import com.app.cr7wallpaper.models.Wallpaper;
import com.app.cr7wallpaper.utils.Constant;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.solodroid.ads.sdk.format.NativeAdViewHolder;

import java.util.List;

public class AdapterWallpaper extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_PROG = 0;
    private final int VIEW_ITEM = 1;
    private final int VIEW_AD = 2;
    private List<Wallpaper> items;
    private boolean loading;
    private OnLoadMoreListener onLoadMoreListener;

    private Context context;
    private OnItemClickListener mOnItemClickListener;
    private Wallpaper pos;
    private CharSequence charSequence = null;
    private boolean scrolling = false;

    public interface OnItemClickListener {
        void onItemClick(View view, Wallpaper obj, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public AdapterWallpaper(Context context, RecyclerView view, List<Wallpaper> items) {
        this.items = items;
        this.context = context;
        lastItemViewDetector(view);
        view.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    scrolling = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scrolling = false;
                }
                super.onScrollStateChanged(recyclerView, newState);
            }
        });
    }

    public static class OriginalViewHolder extends RecyclerView.ViewHolder {

        public ImageView wallpaper_image;
        public TextView wallpaper_name;
        public TextView category_name;
        public CardView card_view;
        LinearLayout bg_shadow;
        ProgressBar progress_bar;
        FrameLayout lyt_parent;

        public OriginalViewHolder(View v) {
            super(v);
            wallpaper_image = v.findViewById(R.id.wallpaper_image);
            wallpaper_name = v.findViewById(R.id.wallpaper_name);
            category_name = v.findViewById(R.id.category_name);
            card_view = v.findViewById(R.id.card_view);
            bg_shadow = v.findViewById(R.id.bg_shadow_bottom);
            progress_bar = v.findViewById(R.id.progress_bar);
            lyt_parent = v.findViewById(R.id.lyt_parent);
        }

    }

    public static class ProgressViewHolder extends RecyclerView.ViewHolder {
        public ProgressBar progressBar;

        ProgressViewHolder(View v) {
            super(v);
            progressBar = v.findViewById(R.id.load_more);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder vh;
        if (viewType == VIEW_ITEM) {
            if (Config.DISPLAY_WALLPAPER == 2) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper_square, parent, false);
                vh = new OriginalViewHolder(v);
            } else if (Config.DISPLAY_WALLPAPER == 3) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper_dynamic, parent, false);
                vh = new OriginalViewHolder(v);
            } else {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallpaper_rectangle, parent, false);
                vh = new OriginalViewHolder(v);
            }
        } else if (viewType == VIEW_AD) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_native_ad_medium, parent, false);
            vh = new NativeAdViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_load_more, parent, false);
            vh = new ProgressViewHolder(v);
        }
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
        if (holder instanceof OriginalViewHolder) {
            final Wallpaper p = items.get(position);
            final OriginalViewHolder vItem = (OriginalViewHolder) holder;

            vItem.wallpaper_name.setText(p.image_name);
            vItem.category_name.setText(p.category_name);

            if (!Config.ENABLE_DISPLAY_WALLPAPER_NAME) {
                vItem.wallpaper_name.setVisibility(View.GONE);
                vItem.category_name.setTextSize(TypedValue.COMPLEX_UNIT_PX, context.getResources().getDimension(R.dimen.text_size_medium));
            }

            if (!Config.ENABLE_DISPLAY_WALLPAPER_CATEGORY) {
                vItem.category_name.setVisibility(View.GONE);
            }

            SharedPref sharedPref = new SharedPref(context);
            if (sharedPref.getIsDarkTheme()) {
                vItem.card_view.setCardBackgroundColor(context.getResources().getColor(R.color.colorToolbarDark));
            } else {
                vItem.card_view.setCardBackgroundColor(context.getResources().getColor(R.color.grey_soft));
            }

            if (!Config.ENABLE_DISPLAY_WALLPAPER_NAME && !Config.ENABLE_DISPLAY_WALLPAPER_CATEGORY) {
                vItem.bg_shadow.setBackgroundResource(R.drawable.ic_transparent);
            }

            if (p.type.equals("url")) {
                Glide.with(context)
                        .load(p.image_url.replace(" ", "%20"))
                        //.transition(withCrossFade())
                        .thumbnail(0.1f)
                        .addListener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                vItem.progress_bar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        //.apply(new RequestOptions().override(Constant.THUMBNAIL_WIDTH, Constant.THUMBNAIL_HEIGHT))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_transparent)
                        //.centerCrop()
                        .into(vItem.wallpaper_image);
            } else {
                Glide.with(context)
                        .load(Config.ADMIN_PANEL_URL + "/upload/thumbs/" + items.get(position).image_upload.replace(" ", "%20"))
                        //.transition(withCrossFade())
                        .thumbnail(0.1f)
                        .addListener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                vItem.progress_bar.setVisibility(View.GONE);
                                return false;
                            }
                        })
                        //.apply(new RequestOptions().override(Constant.THUMBNAIL_WIDTH, Constant.THUMBNAIL_HEIGHT))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.ic_transparent)
                        //.centerCrop()
                        .into(vItem.wallpaper_image);
            }

            vItem.lyt_parent.setOnClickListener(view -> {
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(view, p, position);
                }
            });

        } else if (holder instanceof NativeAdViewHolder) {

            final NativeAdViewHolder vItem = (NativeAdViewHolder) holder;
            final AdsPref adsPref = new AdsPref(context);
            final SharedPref sharedPref = new SharedPref(context);

            vItem.loadNativeAd(context,
                    adsPref.getAdStatus(),
                    adsPref.getNativeAdWallpaperList(),
                    adsPref.getAdType(),
                    adsPref.getAdMobNativeId(),
                    sharedPref.getIsDarkTheme(),
                    context.getResources().getDimensionPixelOffset(R.dimen.grid_space_wallpaper)
            );

        } else {
            ((ProgressViewHolder) holder).progressBar.setIndeterminate(true);
        }

        StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) holder.itemView.getLayoutParams();
        if (getItemViewType(position) == VIEW_PROG || getItemViewType(position) == VIEW_AD) {
            layoutParams.setFullSpan(true);
        } else {
            layoutParams.setFullSpan(false);
        }

    }

    public void insertDataWithNativeAd(List<Wallpaper> items) {
        setLoaded();
        int positionStart = getItemCount();
        for (Wallpaper post : items) {
            //Log.d("item", "TITLE: " + post.image_id);
        }
        // if there are more than POST_LAST_POSITION_BEFORE_AD new posts
        // them insert a new fake Post to represent an Ad
        // Fake Post is Post that doesn't contain any data (title, desc, etc)
        SharedPref sharedPref = new SharedPref(context);
        if (sharedPref.getWallpaperColumns() == 3) {
            if (items.size() >= Constant.NATIVE_AD_INDEX_3_COLUMNS)
                items.add(Constant.NATIVE_AD_INDEX_3_COLUMNS, new Wallpaper());
            Log.d("INSERT_DATA", "3 columns");
        } else {
            if (items.size() >= Constant.NATIVE_AD_INDEX_2_COLUMNS)
                items.add(Constant.NATIVE_AD_INDEX_2_COLUMNS, new Wallpaper());
            Log.d("INSERT_DATA", "2 columns");
        }

        int itemCount = items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    public void insertData(List<Wallpaper> items) {
        setLoaded();
        int positionStart = getItemCount();
        int itemCount = items.size();
        this.items.addAll(items);
        notifyItemRangeInserted(positionStart, itemCount);
    }

    public void setItems(List<Wallpaper> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setLoaded() {
        loading = false;
        for (int i = 0; i < getItemCount(); i++) {
            if (items.get(i) == null) {
                items.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    public void setLoading() {
        if (getItemCount() != 0) {
            this.items.add(null);
            notifyItemInserted(getItemCount() - 1);
            loading = true;
        }
    }

    public void insertAd() {
        if (getItemCount() != 0) {
            this.items.add(new Wallpaper());
            notifyItemInserted(getItemCount() - 1);
        }
    }

    public void resetListData() {
        this.items.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        Wallpaper wallpaper = items.get(position);
        if (wallpaper != null) {
            // Real Wallpaper should contain some data such as title, desc, and so on.
            // A Wallpaper having no title etc is assumed to be a fake Wallpaper which represents an Native Ad view
            if (wallpaper.image_name == null) {
                return VIEW_AD;
            }
            return VIEW_ITEM;
        } else {
            return VIEW_PROG;
        }
    }

    public void setOnLoadMoreListener(OnLoadMoreListener onLoadMoreListener) {
        this.onLoadMoreListener = onLoadMoreListener;
    }

    private void lastItemViewDetector(RecyclerView recyclerView) {
        if (recyclerView.getLayoutManager() instanceof StaggeredGridLayoutManager) {
            final StaggeredGridLayoutManager layoutManager = (StaggeredGridLayoutManager) recyclerView.getLayoutManager();
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    int lastPos = getLastVisibleItem(layoutManager.findLastVisibleItemPositions(null));
                    if (!loading && lastPos == getItemCount() - 1 && onLoadMoreListener != null) {
                        SharedPref sharedPref = new SharedPref(context);
                        AdsPref adsPref = new AdsPref(context);
                        if (sharedPref.getWallpaperColumns() == 3) {
                            if (adsPref.getNativeAdWallpaperList() != 0) {
                                switch (adsPref.getAdType()) {
                                    case UNITY:
                                    case APPLOVIN:
                                    case MOPUB: {
                                        int current_page = getItemCount() / (Constant.LOAD_MORE_3_COLUMNS);
                                        onLoadMoreListener.onLoadMore(current_page);
                                        break;
                                    }
                                    default: {
                                        int current_page = getItemCount() / (Constant.LOAD_MORE_3_COLUMNS + 1); //posts per page plus 1 Ad
                                        onLoadMoreListener.onLoadMore(current_page);
                                        break;
                                    }
                                }
                            } else {
                                int current_page = getItemCount() / (Constant.LOAD_MORE_3_COLUMNS);
                                onLoadMoreListener.onLoadMore(current_page);
                            }
                        } else {
                            if (adsPref.getNativeAdWallpaperList() != 0) {
                                switch (adsPref.getAdType()) {
                                    case UNITY:
                                    case APPLOVIN:
                                    case MOPUB: {
                                        int current_page = getItemCount() / (Constant.LOAD_MORE_2_COLUMNS);
                                        onLoadMoreListener.onLoadMore(current_page);
                                        break;
                                    }
                                    default: {
                                        int current_page = getItemCount() / (Constant.LOAD_MORE_2_COLUMNS + 1); //posts per page plus 1 Ad
                                        onLoadMoreListener.onLoadMore(current_page);
                                        break;
                                    }
                                }
                            } else {
                                int current_page = getItemCount() / (Constant.LOAD_MORE_2_COLUMNS);
                                onLoadMoreListener.onLoadMore(current_page);
                            }
                        }
                        loading = true;
                    }
                }
            });
        }
    }

    public interface OnLoadMoreListener {
        void onLoadMore(int current_page);
    }

    private int getLastVisibleItem(int[] into) {
        int last_idx = into[0];
        for (int i : into) {
            if (last_idx < i) last_idx = i;
        }
        return last_idx;
    }

}