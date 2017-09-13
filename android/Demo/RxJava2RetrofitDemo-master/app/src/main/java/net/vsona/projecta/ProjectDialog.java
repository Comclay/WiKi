package net.vsona.projecta;

import android.content.Context;

import net.vsona.common.BaseMaterialDialog;

/**
 * Author   : roy
 * Data     : 2017-01-09  18:25
 * Describe :
 */

public class ProjectDialog extends BaseMaterialDialog {

    public void dismiss(){
        super.dismiss();
    }

    public void showProgressDialog(Context context) {
        showLoading(context);
    }

    public void showProgressDialog(Context context, CharSequence msg) {
        showLoading(context, msg);
    }
}
