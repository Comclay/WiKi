package net.vsona.projecta.base;

import android.content.Context;
import android.os.AsyncTask;

import net.vsona.common.utils.EmptyUtils;
import net.vsona.common.utils.ResUtils;
import net.vsona.projecta.ProjectDialog;
import net.vsona.projecta.R;

/**
 * 作者 : zhoukang
 * 日期 : 2017-06-17  12:20
 * 说明 : 带有加载进度的AsyncTask的封装
 */

public class BaseAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

    private WeakContext mWeakContext;
    private boolean isShowProgress = false;
    private CharSequence msg;
    private ProjectDialog mProgressDialog;
    private boolean isCancelable;

    public BaseAsyncTask() {
        this(null, false, null);
    }

    public BaseAsyncTask(Context context, boolean isShowProgress) {
        this(context, isShowProgress, ResUtils.getString(R.string.msg_loading));
    }

    public BaseAsyncTask(Context context, boolean isShowProgress, CharSequence msg) {
        this.mWeakContext = WeakContext.create(context);
        this.isShowProgress = isShowProgress;
        this.msg = msg;
    }

    public BaseAsyncTask setmWeakContext(Context context) {
        this.mWeakContext = WeakContext.create(context);
        return this;
    }

    public BaseAsyncTask setShowProgress(boolean showProgress) {
        this.isShowProgress = showProgress;
        return this;
    }

    public BaseAsyncTask setMsg(CharSequence msg) {
        this.msg = msg;
        return this;
    }

    public BaseAsyncTask setCancelable(boolean cancelable) {
        isCancelable = cancelable;
        return this;
    }

    @Override
    protected void onPreExecute() {
        if (isShowProgress && EmptyUtils.isNotEmpty(msg) && !mWeakContext.isNull()) {
            mProgressDialog = new ProjectDialog();
            mProgressDialog.showProgressDialog(mWeakContext.getContext(), msg);
            if (isCancelable){
                mProgressDialog.getDialog().setOnDismissListener(dialog -> onCancelled());
            }
        }
    }

    @Override
    protected Result doInBackground(Params[] params) {
        return null;
    }

    @Override
    protected void onPostExecute(Result result) {
        if (mProgressDialog != null) {
            mProgressDialog.getDialog().setOnDismissListener(null);
            mProgressDialog.dismiss();
        }

        if (isShowProgress && mWeakContext.isNull()) {
            return;
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }
}
