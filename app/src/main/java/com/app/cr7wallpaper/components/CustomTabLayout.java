package com.app.cr7wallpaper.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.app.cr7wallpaper.Config;
import com.app.cr7wallpaper.R;
import com.ogaclejapan.smarttablayout.SmartTabLayout;

public class CustomTabLayout extends SmartTabLayout {

    public CustomTabLayout(Context context) {
        super(context);
    }

    public CustomTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomTabLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected TextView createDefaultTabView(CharSequence title) {
        TextView textView = super.createDefaultTabView(title);
        Typeface typeface = ResourcesCompat.getFont(getContext(), R.font.custom_font);
        textView.setTypeface(typeface);
        if (Config.ENABLE_RTL_MODE) {
            textView.setPadding(36, 24, 36, 24);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, 0, 0);
            textView.setLayoutParams(params);
        }
        return textView;
    }

}
