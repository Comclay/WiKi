package com.comclay.microvideolib;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/**
 * 作者 : zhoukang
 * 日期 : 2017-07-31  21:34
 * 说明 :
 */

public class TimerView extends View {

    private Paint mInnerPaint;
    private Paint mOuterPaint;
    private Paint mPrgsPaint;

    private int mViewSize;

    public TimerView(Context context) {
        super(context);
        init();
    }

    public TimerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TimerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mInnerPaint.setStyle(Paint.Style.FILL);
        mInnerPaint.setColor(Color.BLUE);

        mOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOuterPaint.setStyle(Paint.Style.FILL);
        mOuterPaint.setColor(Color.WHITE);

        mPrgsPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPrgsPaint.setStyle(Paint.Style.STROKE);
        mPrgsPaint.setColor(Color.GREEN);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // 加载完毕
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        mViewSize = Math.min(width, height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (width)


    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
    }
}
