package com.ramotion.circlemenu;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

public class RelativeLayoutSquareHorizontal extends RelativeLayout {

    public RelativeLayoutSquareHorizontal(Context context) {
        super(context);
    }

    public RelativeLayoutSquareHorizontal(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RelativeLayoutSquareHorizontal(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}