package com.app.cr7wallpaper.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.cr7wallpaper.R;

import java.util.ArrayList;

public class AdapterTags extends RecyclerView.Adapter<AdapterTags.ViewHolder> {

    Context context;
    ArrayList<String> arrayList;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, String keyword, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mOnItemClickListener = mItemClickListener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView txt_tags;

        public ViewHolder(View view) {
            super(view);
            txt_tags = view.findViewById(R.id.item_tags);
        }

    }

    public AdapterTags(Context context, ArrayList<String> arrayList) {
        this.context = context;
        this.arrayList = arrayList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tags, parent, false);
        return new ViewHolder(itemView);

    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position) {

        final String keyword = arrayList.get(position).toLowerCase();

        holder.txt_tags.setText(keyword);

        holder.txt_tags.setOnClickListener(view -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(view, keyword, position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }

}