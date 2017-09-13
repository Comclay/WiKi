package com.comclay.microvideolib;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OutputFormat;
import android.media.MediaRecorder.VideoSource;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;



import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 视频播放控件
 */
public class MovieRecorderView extends LinearLayout implements OnErrorListener {

    private static int mMaxCaptureTimes = 60;

    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private ProgressBar mProgressBar;

    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private Timer mTimer;// 计时器
    private OnRecordFinishListener mOnRecordFinishListener;// 录制完成回调接口

    private int mWidth;// 视频分辨率宽度
    private int mHeight;// 视频分辨率高度
    private boolean isOpenCamera;// 是否一开始就打开摄像头
    private int mRecordMaxTime;// 一次拍摄最长时间
    private int mTimeCount;// 时间计数
    private File mVecordFile = null;// 文件

    public MovieRecorderView(Context context) {
        this(context, null);
    }

    public MovieRecorderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("NewApi")
    public MovieRecorderView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.MovieRecorderView, defStyle, 0);
        mWidth = a.getInteger(R.styleable.MovieRecorderView_attr_width, 320);// 默认320
        mHeight = a.getInteger(R.styleable.MovieRecorderView_attr_height, 240);// 默认240

        isOpenCamera = a.getBoolean(
                R.styleable.MovieRecorderView_attr_is_open_camera, true);// 默认打开
        mRecordMaxTime = a.getInteger(
                R.styleable.MovieRecorderView_attr_record_max_time, mMaxCaptureTimes);// 默认为60

        initView(context);

        a.recycle();
    }

    /**
     * 初始化录制视频的布局
     */
    private void initView(Context ctx) {
        View view = LayoutInflater.from(ctx)
                .inflate(R.layout.movie_recorder_view, this);

        mSurfaceView = (SurfaceView) view.findViewById(R.id.surfaceview);

        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mProgressBar.setMax(mRecordMaxTime);// 设置进度条最大量

        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.setSizeFromLayout();
        mSurfaceHolder.addCallback(new CustomCallBack());
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private class CustomCallBack implements Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            if (!isOpenCamera)
                return;
            try {
                initCamera();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
//            mSurfaceHolder.setFixedSize(mSurfaceView.getWidth(),mSurfaceView.getHeight());
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (!isOpenCamera)
                return;
            freeCameraResource();
        }
    }

    /**
     * 初始化摄像头
     */
    private void initCamera() throws IOException {
        if (mCamera != null) {
            freeCameraResource();
        }
        try {
            mCamera = Camera.open();
        } catch (Exception e) {
            e.printStackTrace();
            freeCameraResource();
        }
        if (mCamera == null)
            return;

        setCameraParams();
//        mCamera.setDisplayOrientation(90);
        setCameraDisplayOrientation((Activity) getContext(), Camera.CameraInfo.CAMERA_FACING_BACK, mCamera);
        mCamera.setPreviewDisplay(mSurfaceHolder);
        mCamera.startPreview();
        mCamera.unlock();
    }

    public void setCameraDisplayOrientation(Activity activity,
                                            int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    private Size mVideoSize;

    /**
     * 设置摄像头为竖屏
     */
    private void setCameraParams() {
        if (mCamera != null) {
            Parameters parameters = mCamera.getParameters();
            parameters.set("orientation", "portrait");   // 这个在电脑上播放有的会旋转90度
            parameters.set("rotation", 90);

            Point screenResolution = getSurfaceViewSize();

            List<Size> list;
            String sizeValues;

//            // 照片大小
//            String pictureSizeValues = parameters.get("picture-size-values");
//            list = stringToSizeList(pictureSizeValues);
//            Size pictureSize = getBestSupportSize(screenResolution, list);// 从List取出Size
//            parameters.setPictureSize(pictureSize.width, pictureSize.height);// 设置照片的大小
//
//            // 预览大小
//            sizeValues = parameters.get("preview-size-values");
//            list = stringToSizeList(sizeValues);
//            Size previewSize = getBestSupportSize(screenResolution, list); // 从List取出Size
//            parameters.setPreviewSize(previewSize.height, previewSize.width);// 设置预览照片的大小

            // 视频大小
            sizeValues = parameters.get("video-size-values");
            list = stringToSizeList(sizeValues);
            mVideoSize = getBestSupportSize(screenResolution, list);// 从List取出Size

            mCamera.cancelAutoFocus();// 2如果要实现连续的自动对焦，这一句必须加上
            try {
                List<String> focusModesList = parameters.getSupportedFocusModes();
                //增加对聚焦模式的判断
                if (focusModesList.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                } else if (focusModesList.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 防抖
            if ("true".equals(parameters.get("video-stabilization-supported")))
                parameters.set("video-stabilization", "true");

            mCamera.setParameters(parameters);
        }
    }

    public Point getSurfaceViewSize() {
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int surfaceViewWidth = displayMetrics.widthPixels;
        int surfaceViewHeight = displayMetrics.widthPixels / 4 * 3; // 高位宽的0.75倍
        return new Point(surfaceViewWidth, surfaceViewHeight);
    }

    private List<Size> stringToSizeList(String value) {
        if (value == null) {
            return null;
        }

        List<Size> list = new ArrayList<>();
        // 用',' 和 'x' 来分割字符串
        String[] temps = value.split("[,x]");

        for (int i = 0; i < temps.length; i++) {
            Size size = mCamera.new Size(Integer.valueOf(temps[i]), Integer.valueOf(temps[++i]));
            list.add(size);
        }

        return list;
    }

    /**
     * 获取图片预览的大小
     *
     * @return Point对象
     */
    protected Point getPreViewSize() {
        Point point = new Point(this.mSurfaceView.getWidth(), this.mSurfaceView.getHeight());
        return point;
    }

    protected DisplayMetrics getScreenWH() {
        DisplayMetrics dMetrics = new DisplayMetrics();
        dMetrics = this.getResources().getDisplayMetrics();
        return dMetrics;
    }

    /**
     * 更具当前相机所支持的屏幕尺寸以及预览布局找到一个相对合适的相片尺寸大小
     *
     * @return 尺寸大小
     */
    private Size getBestSupportSize(Point screenResolution, List<Size> supportedSizes) {
        if (supportedSizes == null || supportedSizes.size() == 0) {
            return mCamera.new Size(320, 240);
        }

        if (supportedSizes.get(0).width > supportedSizes.get(supportedSizes.size() - 1).width) {
            // 如果是降序就翻转成升序
            Collections.reverse(supportedSizes);
        }

        float tmp;
        float mindiff = 100f;
        float x_d_y = (float) screenResolution.x / (float) screenResolution.y;
        Size best = null;
        for (Size s : supportedSizes) {
            tmp = Math.abs(((float) s.height / (float) s.width) - x_d_y);
            if (tmp < mindiff) {
                mindiff = tmp;
                best = s;
            }
        }
        return best;
    }

    /**
     * 释放摄像头资源
     */
    private void freeCameraResource() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.lock();
            mCamera.release();
            mCamera = null;
        }
    }

    private void createRecordDir() {
        if (mVecordFile != null) {
            return;
        }

        createVideoFile();
    }

    public void createVideoFile() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), getContext().getApplicationInfo().name);
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        mVecordFile = new File(mediaStorageDir.getPath() + File.separator +
                timeStamp + ".mp4");
    }

    /**
     * 设置保存的路径
     *
     * @param path
     */
    public void setRecordDir(String path) {
        if(path == null || path.trim().length() == 0){
            return;
        }
        mVecordFile = new File(path);
    }

    /**
     * 初始化
     *
     * @throws IOException
     */
    @SuppressLint("NewApi")
    private void initRecord() throws IOException {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.reset();
        if (mCamera != null)
            mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        mMediaRecorder.setAudioSource(AudioSource.MIC);// 音频源
        mMediaRecorder.setVideoSource(VideoSource.CAMERA);// 视频源

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
//        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));

        mMediaRecorder.setOutputFormat(OutputFormat.MPEG_4);// 视频输出格式

//        CamcorderProfile mProfile = CamcorderProfile.get(getVideoQuality());
        CamcorderProfile mProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_TIME_LAPSE_QVGA);
        //after setVideoSource(),after setOutFormat()

        mMediaRecorder.setOrientationHint(90);// 输出旋转90度，保持竖屏录制
        mMediaRecorder.setVideoSize(mProfile.videoFrameWidth, mProfile.videoFrameHeight);
//        mMediaRecorder.setVideoSize(mVideoSize.width, mVideoSize.height);// 设置分辨率：
        mMediaRecorder.setAudioEncodingBitRate(44100);   // cd的音频采样率

        // 1280 * 1024 ，1500000 ，1280 x 720 ，210000
//        if (mProfile.videoBitRate > getBitRate() * 320 * 240)
            mMediaRecorder.setVideoEncodingBitRate(getBitRate() * 320 * 240);
//        else
//            mMediaRecorder.setVideoEncodingBitRate(mProfile.videoBitRate);

        //after setVideoSource(),after setOutFormat();
        mMediaRecorder.setVideoFrameRate(mProfile.videoFrameRate);
        //after setOutputFormat()
        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        //after setOutputFormat()
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        mMediaRecorder.setOutputFile(mVecordFile.getAbsolutePath());
        mMediaRecorder.prepare();
        try {
            mMediaRecorder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据配置获取视频的清晰度
     */
    private int getBitRate() {
//        String videoQuality = MyApplication.getInstance().getConfigValue("VideoQuality");
        String videoQuality = "中";
        int rate = 4;
        switch (videoQuality) {
            case "中":
                rate = 6;
                break;
            case "高":
                rate = 9;
                break;
            case "低":
            default:
                break;
        }
        return rate;
    }


    /**
     * 开始录制视频
     *
     * @param onRecordFinishListener 达到指定时间之后回调接口
     */
    public void record(final OnRecordFinishListener onRecordFinishListener) {
        this.mOnRecordFinishListener = onRecordFinishListener;
        createRecordDir();
        try {
            if (!isOpenCamera)// 如果未打开摄像头，则打开
                initCamera();
            initRecord();
            mTimeCount = 0;// 时间计数器重新赋值
            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {

                @Override
                public void run() {
                    mTimeCount++;
                    mProgressBar.setProgress(mTimeCount);// 设置进度条
                    if (mTimeCount == mRecordMaxTime) {// 达到指定时间，停止拍摄
                        stop();
                        if (mOnRecordFinishListener != null)

                            mOnRecordFinishListener.onRecordFinish();
                    }
                }
            }, 0, 1000);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 停止拍摄
     */
    public void stop() {
        stopRecord();
        releaseRecord();
        freeCameraResource();
    }

    /**
     * 停止录制
     */
    public void stopRecord() {
        mProgressBar.setProgress(0);
        if (mTimer != null)
            mTimer.cancel();
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.setPreviewDisplay(null);
            try {
                mMediaRecorder.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 释放资源
     */
    private void releaseRecord() {
        if (mMediaRecorder != null) {
            mMediaRecorder.setOnErrorListener(null);
            try {
                mMediaRecorder.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mMediaRecorder = null;
    }

    public int getTimeCount() {
        return mTimeCount;
    }

    //返回录制的视频文件
    public File getmVecordFile() {
        return mVecordFile;
    }

    /**
     * 录制完成回调接口
     */
    public interface OnRecordFinishListener {
        // 这个方法执行实在线程里面
        void onRecordFinish();
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        try {
            if (mr != null)
                mr.reset();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}