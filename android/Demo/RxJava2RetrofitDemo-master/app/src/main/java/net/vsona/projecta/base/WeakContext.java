package net.vsona.projecta.base;

import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * 作者 : zhoukang
 * 日期 : 2017-06-17  12:22
 * 说明 : 若引用类型的Context包装类
 */

public class WeakContext {
    private WeakReference<Context> mWeakContext;

    private WeakContext(Context context) {
        this.mWeakContext = new WeakReference<>(context);
    }


    public static WeakContext create(Context ctx) {
        return new WeakContext(ctx);
    }

    public Context getContext() {
        if (mWeakContext == null) {
            return null;
        }
        return mWeakContext.get();
    }

    public boolean isNull(){
        return mWeakContext.get() == null;
    }
}
