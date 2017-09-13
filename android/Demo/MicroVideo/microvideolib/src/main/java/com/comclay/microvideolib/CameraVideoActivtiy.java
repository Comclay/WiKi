package com.comclay.microvideolib;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

public class CameraVideoActivtiy extends Activity {

    private MovieRecorderView mRecorderView;//视频录制控件
    private Button mShootBtn;//视频开始录制按钮
    private boolean isFinish = true;
    private boolean success = false;//防止录制完成后出现多次跳转事件

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Android 5.0 以上 全透明
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            // 状态栏（以上几行代码必须，参考setStatusBarColor|setNavigationBarColor方法源码）
            window.setStatusBarColor(Color.TRANSPARENT);
            // 虚拟导航键
            window.setNavigationBarColor(Color.TRANSPARENT);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // Android 4.4 以上 半透明
            Window window = getWindow();
            // 状态栏
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // 虚拟导航键
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        setContentView(R.layout.camera_activity);

        mRecorderView = (MovieRecorderView) findViewById(R.id.movieRecorderView);

        if (this.getIntent() != null) {
            mRecorderView.setRecordDir(this.getIntent().getStringExtra(MediaStore.EXTRA_OUTPUT));
        }

        mShootBtn = (Button) findViewById(R.id.shoot_button);

        //用户长按事件监听
        mShootBtn.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {//用户按下拍摄按钮
                    mShootBtn.setBackgroundResource(R.drawable.bg_movie_add_shoot_select);
                    mRecorderView.record(new MovieRecorderView.OnRecordFinishListener() {

                        @Override
                        public void onRecordFinish() {
                            if (!success && mRecorderView.getTimeCount() < 10) {//判断用户按下时间是否大于10秒
                                success = true;
                                handler.sendEmptyMessage(1);
                            }
                        }
                    });
                } else if (event.getAction() == MotionEvent.ACTION_UP) {//用户抬起拍摄按钮
                    mShootBtn.setBackgroundResource(R.drawable.bg_movie_add_shoot);
                    if (mRecorderView.getTimeCount() > 3 && mRecorderView.getmVecordFile().length() > 0) {//判断用户按下时间是否大于3秒以及是否录制成功
                        if (!success) {
                            success = true;
                            handler.sendEmptyMessage(1);
                        }
                    } else {
                        success = false;
                        if (mRecorderView.getmVecordFile() != null)
                            mRecorderView.getmVecordFile().delete();//删除录制的过短视频
                        mRecorderView.stop();//停止录制
                        Toast.makeText(CameraVideoActivtiy.this, "视频录制时间太短", Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            }
        });
    }

   /* @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if( hasFocus ) {
            hideNavigationBar();
        }
    }
    private void hideNavigationBar() {
        // TODO Auto-generated method stub
        final View decorView = getWindow().getDecorView();
        final int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(flags);
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(flags);
                }
            }
        });
    }*/

    @Override
    public void onResume() {
        super.onResume();
        isFinish = true;
//		if (mRecorderView.getmVecordFile() != null)
//			mRecorderView.getmVecordFile().delete();//视频使用后删除
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        isFinish = false;
        success = false;
        mRecorderView.stop();//停止录制
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (success) {
                finishActivity();
            }
        }
    };

    //视频录制结束后，跳转的函数
    private void finishActivity() {
        Intent intent = new Intent();
        // 初始化为无效返回码
        int resultCode;
        if (isFinish) {
            mRecorderView.stop();
            // 将录制的视频路径返回
            intent.setData(Uri.fromFile(mRecorderView.getmVecordFile()));
            resultCode = Activity.RESULT_OK;

        } else {
            success = false;
            resultCode = Activity.RESULT_CANCELED;
        }
        setResult(resultCode, intent);

        dismissActivity();
//
    }

    private void dismissActivity() {
        this.finish();
        this.overridePendingTransition(0, R.anim.bottom_out);
    }

    /**
     * 录制完成回调
     */
    public interface OnShootCompletionListener {
        void OnShootSuccess(String path, int second);

        void OnShootFailure();
    }

    @Override
    public void onBackPressed() {
        dismissActivity();
//		this.overridePendingTransition(0, R.anim.bottom_out);
//		super.onBackPressed();
    }
}
