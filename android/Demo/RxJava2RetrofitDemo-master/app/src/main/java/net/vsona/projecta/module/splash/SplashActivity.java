package net.vsona.projecta.module.splash;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.view.View;
import android.view.Window;

import net.vsona.common.utils.ToastUtils;
import net.vsona.projecta.R;
import net.vsona.projecta.module.login.LoginActivity;

/**
 * 闪屏页面
 */
public class SplashActivity extends AppCompatActivity {

    /**
     * 默认自动进入登录页面的延迟时间
     */
    private static final int AUTO_DELAY_MILLIS = 3000;

    private final Handler mHideHandler = new Handler();
    private View mContentView;

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            enterLoginActivity();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置进入动画
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
            Transition explode = TransitionInflater.from(this).inflateTransition(android.R.transition.explode);
            getWindow().setEnterTransition(explode);
        }

        setContentView(R.layout.activity_splash);
        mContentView = findViewById(R.id.fullscreen_content);

        mHideHandler.postDelayed(mShowPart2Runnable, AUTO_DELAY_MILLIS);
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enterLoginActivity();
            }
        });

    }

    @Override
    public void onEnterAnimationComplete() {
        super.onEnterAnimationComplete();
        ToastUtils.show("进场动画执行完毕，3秒后进入登录界面");
    }

    @Override
    protected void onDestroy() {
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        super.onDestroy();
    }

    private void enterLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
