package com.comclay.microvideo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import com.comclay.microvideolib.CameraVideoActivtiy;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private VideoView mVideoView;
    private Button mBtnRecordVideo;
    private Button mBtnPlayVideo;

    private String mVideoPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // MP4文件路径
        mVideoPath = Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + "temp.mp4";
        File file = new File(mVideoPath);
        if(file.exists()){
            file.delete();
        }

        mVideoView = (VideoView) findViewById(R.id.videoView);
        mBtnPlayVideo = (Button) findViewById(R.id.btn_play_video);
        mBtnRecordVideo = (Button) findViewById(R.id.btn_record_video);

        mBtnPlayVideo.setOnClickListener(this);
        mBtnRecordVideo.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == mBtnPlayVideo.getId()) {
            playVideo(mVideoPath);
        } else if (id == mBtnRecordVideo.getId()) {
            recordVideo(mVideoPath);
        }
    }

    private void recordVideo(String videoPath) {
        Intent intent = new Intent(this, CameraVideoActivtiy.class);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, videoPath);

//        MediaStore.EXTRA_OUTPUT：设置媒体文件的保存路径。
//        MediaStore.EXTRA_VIDEO_QUALITY：设置视频录制的质量，0为低质量，1为高质量。
//        MediaStore.EXTRA_DURATION_LIMIT：设置视频最大允许录制的时长，单位为毫秒。
//        MediaStore.EXTRA_SIZE_LIMIT：指定视频最大允许的尺寸，单位为byte。

        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0); // set the video image quality to low
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 30);            //限制持续时长
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 获取录制的视频文件路径
    }

    private void playVideo(String videoPath) {
        File videoFile = new File(videoPath);
        if (!videoFile.exists()) {
            Toast.makeText(this, "视频文件不存在", Toast.LENGTH_SHORT).show();
            return;
        }

        mVideoView.setVideoPath(videoPath);
        MediaController mediaController = new MediaController(this);
        mVideoView.setMediaController(mediaController);
        mVideoView.start();
    }
}
