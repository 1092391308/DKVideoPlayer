package xyz.doikki.videoplayer;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import xyz.doikki.videoplayer.controller.BaseVideoController;
import xyz.doikki.videoplayer.controller.MediaPlayerControl;
import xyz.doikki.videoplayer.player.AudioFocusHelper;
import xyz.doikki.videoplayer.player.ProgressManager;
import xyz.doikki.videoplayer.render.AspectRatioType;
import xyz.doikki.videoplayer.render.Render;
import xyz.doikki.videoplayer.render.RenderFactory;
import xyz.doikki.videoplayer.util.L;
import xyz.doikki.videoplayer.util.PlayerUtils;

/**
 * 播放器&播放视图  内部包含了对应的{@link AVPlayer} 和  {@link Render}
 * <p>
 * Created by Doikki on 2017/4/7.
 * <p>
 * update by luochao on 2022/9/16
 */
public class VideoView extends FrameLayout
        implements MediaPlayerControl, AVPlayer.EventListener {

    /**
     * 播放出错
     */
    public static final int STATE_ERROR = -1;

    /**
     * 闲置中
     */
    public static final int STATE_IDLE = 0;

    /**
     * 准备中：处于已设置了播放数据源，但是播放器还未回调{@link AVPlayer.EventListener#onPrepared()}
     */
    public static final int STATE_PREPARING = 1;

    /**
     * 已就绪
     */
    public static final int STATE_PREPARED = 2;

    /**
     * 播放中
     */
    public static final int STATE_PLAYING = 3;

    /**
     * 暂停中
     */
    public static final int STATE_PAUSED = 4;

    /**
     * 播放结束
     */
    public static final int STATE_PLAYBACK_COMPLETED = 5;

    /**
     * 缓冲中
     */
    public static final int STATE_BUFFERING = 6;

    /**
     * 缓冲结束
     */
    public static final int STATE_BUFFERED = 7;

    /**
     * 播放过程中停止继续播放：比如手机不允许在手机流量的时候进行播放（此时播放器处于已就绪未播放中状态）
     */
    public static final int STATE_START_ABORT = 8;//开始播放中止

    /**
     * 播放器状态
     */
    @IntDef({STATE_ERROR, STATE_IDLE, STATE_PREPARING, STATE_PREPARED, STATE_PLAYING, STATE_PAUSED, STATE_PLAYBACK_COMPLETED, STATE_BUFFERING, STATE_BUFFERED, STATE_START_ABORT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PlayerState { }

    /**
     * 当前播放器的状态
     */
    @PlayerState
    private int mPlayerState = STATE_IDLE;

    /**
     * 播放器内核
     */
    protected AVPlayer mMediaPlayer;//播放器

    private AVPlayerFactory<? extends AVPlayer> mPlayerFactory;//工厂类，用于实例化播放核心

    @Nullable
    protected BaseVideoController mVideoController;//控制器

    /**
     * 真正承载播放器视图的容器
     */
    protected FrameLayout mPlayerContainer;

    protected Render mRenderView;

    /**
     * 自定义Render工厂
     */
    protected RenderFactory mCustomRenderViewFactory;

    /**
     * 进度管理器，设置之后播放器会记录播放进度，以便下次播放恢复进度
     */
    @Nullable
    protected ProgressManager mProgressManager;

    /**
     * 是否静音
     */
    private boolean mMute = false;

    /**
     * 左声道音量
     */
    private float mLeftVolume = 1.0f;

    /**
     * 右声道音量
     */
    private float mRightVolume = 1.0f;

    /**
     * 是否循环播放
     */
    private boolean mLooping = false;

    public static final int SCREEN_SCALE_DEFAULT = AspectRatioType.SCALE;
    public static final int SCREEN_SCALE_18_9 = AspectRatioType.SCALE_18_9;
    public static final int SCREEN_SCALE_16_9 = AspectRatioType.SCALE_16_9;
    public static final int SCREEN_SCALE_4_3 = AspectRatioType.SCALE_4_3;
    public static final int SCREEN_SCALE_MATCH_PARENT = AspectRatioType.MATCH_PARENT;
    public static final int SCREEN_SCALE_ORIGINAL = AspectRatioType.SCALE_ORIGINAL;
    public static final int SCREEN_SCALE_CENTER_CROP = AspectRatioType.CENTER_CROP;
    protected int mScreenScaleType;

    protected int[] mVideoSize = {0, 0};


    //--------- data sources ---------//
    protected String mUrl;//当前播放视频的地址
    protected Map<String, String> mHeaders;//当前视频地址的请求头
    protected AssetFileDescriptor mAssetFileDescriptor;//assets文件

    protected long mCurrentPosition;//当前正在播放视频的位置


    public static final int PLAYER_NORMAL = 10;        // 普通播放器
    public static final int PLAYER_FULL_SCREEN = 11;   // 全屏播放器
    public static final int PLAYER_TINY_SCREEN = 12;   // 小屏播放器
    protected int mCurrentPlayerState = PLAYER_NORMAL;

    protected boolean mIsFullScreen;//是否处于全屏状态
    protected boolean mIsTinyScreen;//是否处于小屏状态
    protected int[] mTinyScreenSize = {0, 0};

    /**
     * 监听系统中音频焦点改变，见{@link #setEnableAudioFocus(boolean)}
     */
    protected boolean mEnableAudioFocus;
    @Nullable
    protected AudioFocusHelper mAudioFocusHelper;

    /**
     * OnStateChangeListener集合，保存了所有开发者设置的监听器
     */
    protected CopyOnWriteArrayList<OnStateChangeListener> mPlayerStateChangedListeners;


    /**
     * {@link #mPlayerContainer}背景色，默认黑色
     */
    private final int mPlayerBackgroundColor;

    public VideoView(@NonNull Context context) {
        this(context, null);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mProgressManager = VideoViewManager.getProgressManager();
        //读取xml中的配置，并综合全局配置

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.VideoView);
        mEnableAudioFocus = a.getBoolean(R.styleable.VideoView_enableAudioFocus, VideoViewManager.enableAudioFocus());
        mLooping = a.getBoolean(R.styleable.VideoView_looping, false);
        mScreenScaleType = a.getInt(R.styleable.VideoView_screenScaleType, VideoViewManager.getScreenType());
        mPlayerBackgroundColor = a.getColor(R.styleable.VideoView_playerBackgroundColor, Color.BLACK);
        a.recycle();
        initView();
    }

    /**
     * 初始化播放器视图
     */
    protected void initView() {
        mPlayerContainer = new FrameLayout(getContext());
        mPlayerContainer.setBackgroundColor(mPlayerBackgroundColor);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(mPlayerContainer, params);
    }


    /**
     * 第一次播放
     *
     * @return 是否成功开始播放
     */
    protected boolean startPlay() {
        //如果要显示移动网络提示则不继续播放
        if (showNetWarning()) {
            //中止播放
            setPlayState(STATE_START_ABORT);
            return false;
        }
        //todo 此处存在问题就是如果在中途修改了mEnableAudioFocus为false，并且之前已初始化了mAudioFocusHelper，则会导致问题，不过该问题并不太可能出现
        //监听音频焦点改变
        if (mEnableAudioFocus) {
            mAudioFocusHelper = new AudioFocusHelper(this);
        }
        mCurrentPosition = getSavedPlayedProgress();
        setupMediaPlayer();
        setupRenderView();
        startPrepare(false);
        return true;
    }

    /**
     * 是否显示移动网络提示，可在Controller中配置
     */
    protected boolean showNetWarning() {
        //播放本地数据源时不检测网络
        if (isLocalDataSource()) return false;
        return mVideoController != null && mVideoController.showNetWarning();
    }

    /**
     * 判断是否为本地数据源，包括 本地文件、Asset、raw
     */
    protected boolean isLocalDataSource() {
        if (mAssetFileDescriptor != null) {
            return true;
        } else if (!TextUtils.isEmpty(mUrl)) {
            Uri uri = Uri.parse(mUrl);
            return ContentResolver.SCHEME_ANDROID_RESOURCE.equals(uri.getScheme())
                    || ContentResolver.SCHEME_FILE.equals(uri.getScheme())
                    || "rawresource".equals(uri.getScheme());
        }
        return false;
    }

    protected AVPlayer createMediaPlayer() {
        return VideoViewManager.createMediaPlayer(getContext(), mPlayerFactory);
    }

    /**
     * 初始化播放器
     */
    protected void setupMediaPlayer() {
        mMediaPlayer = createMediaPlayer();
        mMediaPlayer.setEventListener(this);
        onMediaPlayerCreate(mMediaPlayer);
        mMediaPlayer.init();
        setupMediaPlayerOptions();
    }

    /**
     * 初始化之前的配置项
     */
    protected void onMediaPlayerCreate(AVPlayer mediaPlayer) {

    }

    /**
     * 初始化之后的配置项
     */
    protected void setupMediaPlayerOptions() {
        setLooping(mLooping);
        setMute(mMute);
    }

    protected Render createRenderView() {
        return VideoViewManager.createRenderView(getContext(), mCustomRenderViewFactory);
    }

    /**
     * 初始化视频渲染View
     */
    protected void setupRenderView() {
        if (mRenderView != null) {
            mPlayerContainer.removeView(mRenderView.getView());
            mRenderView.release();
        }
        mRenderView = createRenderView();
        mRenderView.attachToPlayer(mMediaPlayer);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        mPlayerContainer.addView(mRenderView.getView(), 0, params);
    }

    /**
     * 开始准备播放（直接播放）
     */
    protected void startPrepare(boolean reset) {
        if (reset) {
            mMediaPlayer.reset();
            //重新设置option，media player reset之后，option会失效
            setupMediaPlayerOptions();
        }
        if (prepareDataSource()) {
            mMediaPlayer.prepareAsync();
            setPlayState(STATE_PREPARING);
            setPlayerState(isFullScreen() ? PLAYER_FULL_SCREEN : isTinyScreen() ? PLAYER_TINY_SCREEN : PLAYER_NORMAL);
        }
    }

    /**
     * 设置播放数据
     *
     * @return 播放数据是否设置成功
     */
    protected boolean prepareDataSource() {
        if (mAssetFileDescriptor != null) {
            mMediaPlayer.setDataSource(mAssetFileDescriptor);
            return true;
        } else if (!TextUtils.isEmpty(mUrl)) {
            mMediaPlayer.setDataSource(mUrl, mHeaders);
            return true;
        }
        return false;
    }

    /**
     * 播放状态下开始播放
     */
    protected void startInPlaybackState() {
        mMediaPlayer.start();
        setPlayState(STATE_PLAYING);
        if (mAudioFocusHelper != null && !isMute()) {
            mAudioFocusHelper.requestFocus();
        }
        mPlayerContainer.setKeepScreenOn(true);
    }

    /**
     * 继续播放
     */
    public void resume() {
        if (isInPlaybackState()
                && !mMediaPlayer.isPlaying()) {
            mMediaPlayer.start();
            setPlayState(STATE_PLAYING);
            if (mAudioFocusHelper != null && !isMute()) {
                mAudioFocusHelper.requestFocus();
            }
            mPlayerContainer.setKeepScreenOn(true);
        }
    }

    /**
     * 释放播放器
     */
    public void release() {
        if (!isInIdleState()) {
            //释放播放器
            if (mMediaPlayer != null) {
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
            //释放renderView
            if (mRenderView != null) {
                mPlayerContainer.removeView(mRenderView.getView());
                mRenderView.release();
                mRenderView = null;
            }
            //释放Assets资源
            if (mAssetFileDescriptor != null) {
                try {
                    mAssetFileDescriptor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //关闭AudioFocus监听
            if (mAudioFocusHelper != null) {
                mAudioFocusHelper.abandonFocus();
                mAudioFocusHelper = null;
            }
            //关闭屏幕常亮
            keepScreenOn(false);
            //保存播放进度
            saveCurrentPlayedProgress();
            //重置播放进度
            mCurrentPosition = 0;
            //切换转态
            setPlayState(STATE_IDLE);
        }
    }

    /**
     * 设置进度管理器，用于保存播放进度
     */
    public void setProgressManager(@Nullable ProgressManager progressManager) {
        this.mProgressManager = progressManager;
    }

    /**
     * 是否处于播放状态
     */
    protected boolean isInPlaybackState() {
        return mMediaPlayer != null
                && mPlayerState != STATE_ERROR
                && mPlayerState != STATE_IDLE
                && mPlayerState != STATE_PREPARING
                && mPlayerState != STATE_START_ABORT
                && mPlayerState != STATE_PLAYBACK_COMPLETED;
    }

    /**
     * 是否处于未播放状态
     */
    protected boolean isInIdleState() {
        return mPlayerState == STATE_IDLE;
    }

    /**
     * 是否处于可播放但终止播放状态
     */
    private boolean isInStartingAbortState() {
        return mPlayerState == STATE_START_ABORT;
    }

    /**
     * 重新播放
     *
     * @param resetPosition 是否从头开始播放
     */
    @Override
    public void replay(boolean resetPosition) {
        if (resetPosition) {
            mCurrentPosition = 0;
        }
        setupRenderView();
        startPrepare(true);
    }

    /*************START AVPlayerFunction***********************/

    @Override
    public void setDataSource(@NonNull String path, @Nullable Map<String, String> headers) {
        mAssetFileDescriptor = null;
        mUrl = path;
        mHeaders = headers;
    }

    @Override
    public void setDataSource(@NonNull AssetFileDescriptor fd) {
        mUrl = null;
        this.mAssetFileDescriptor = fd;
    }

    /**
     * 开始播放，注意：调用此方法后必须调用{@link #release()}释放播放器，否则会导致内存泄漏
     */
    @Override
    public void start() {
        if (isInIdleState()
                || isInStartingAbortState()) {
            startPlay();
        } else if (isInPlaybackState()) {
            startInPlaybackState();
        }
    }

    @Override
    public void pause() {
        if (isInPlaybackState() && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            setPlayState(STATE_PAUSED);
            if (mAudioFocusHelper != null && !isMute()) {
                mAudioFocusHelper.abandonFocus();
            }
            keepScreenOn(false);
        }
    }

    @Override
    public long getDuration() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getDuration();
        }
        return 0;
    }

    @Override
    public long getCurrentPosition() {
        if (isInPlaybackState()) {
            mCurrentPosition = mMediaPlayer.getCurrentPosition();
            return mCurrentPosition;
        }
        return 0;
    }

    @Override
    public int getBufferedPercentage() {
        return mMediaPlayer != null ? mMediaPlayer.getBufferedPercentage() : 0;
    }

    @Override
    public void seekTo(long pos) {
        if (isInPlaybackState()) {
            mMediaPlayer.seekTo(pos);
        } else {
            L.w("当前播放器未处于播放中，忽略seek");
        }
    }

    @Override
    public boolean isPlaying() {
        return isInPlaybackState() && mMediaPlayer.isPlaying();
    }

    @Override
    public void setVolume(@FloatRange(from = 0.0f, to = 1.0f) float leftVolume, @FloatRange(from = 0.0f, to = 1.0f) float rightVolume) {
        mLeftVolume = leftVolume;
        mRightVolume = rightVolume;
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(leftVolume, rightVolume);
        }
    }


    /*************END AVPlayerFunction ***********************/

    /*************START VideoController ***********************/
    @Override
    public void screenshot(boolean highQuality, @NonNull Render.ScreenShotCallback callback) {
        if (mRenderView != null) {
            mRenderView.screenshot(highQuality, callback);
            return;
        }
        callback.onScreenShotResult(null);
    }

    /**
     * 设置静音
     *
     * @param isMute true:静音 false：相反
     */
    @Override
    public void setMute(boolean isMute) {
        this.mMute = isMute;
        if (mMediaPlayer != null) {
            float leftVolume = isMute ? 0.0f : mLeftVolume;
            float rightVolume = isMute ? 0.0f : mRightVolume;
            mMediaPlayer.setVolume(leftVolume, rightVolume);
        }
    }

    /**
     * 是否处于静音状态
     */
    @Override
    public boolean isMute() {
        return mMute;
    }

    /*************START VideoController ***********************/

    /*************START AVPlayer#EventListener 实现逻辑***********************/

    /**
     * 视频缓冲完毕，准备开始播放时回调
     */
    @Override
    public void onPrepared() {
        setPlayState(STATE_PREPARED);
        if (!isMute() && mAudioFocusHelper != null) {
            mAudioFocusHelper.requestFocus();
        }
        if (mCurrentPosition > 0) {
            seekTo(mCurrentPosition);
        }
    }

    /**
     * 播放信息回调，播放中的缓冲开始与结束，开始渲染视频第一帧，视频旋转信息
     */
    @Override
    public void onInfo(int what, int extra) {
        switch (what) {
            case AVPlayer.MEDIA_INFO_BUFFERING_START:
                setPlayState(STATE_BUFFERING);
                break;
            case AVPlayer.MEDIA_INFO_BUFFERING_END:
                setPlayState(STATE_BUFFERED);
                break;
            case AVPlayer.MEDIA_INFO_RENDERING_START: // 视频/音频开始渲染
                setPlayState(STATE_PLAYING);
                keepScreenOn(true);
                break;
            case AVPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED:
                if (mRenderView != null) mRenderView.setVideoRotation(extra);
                break;
        }
    }

    /**
     * 视频播放出错回调
     */
    @Override
    public void onError(Throwable e) {
        keepScreenOn(false);
        setPlayState(STATE_ERROR);
    }

    /**
     * 视频播放完成回调
     */
    @Override
    public void onCompletion() {
        keepScreenOn(false);
        mCurrentPosition = 0;
        //播放完成，清除进度
        savePlayedProgress(0);
        setPlayState(STATE_PLAYBACK_COMPLETED);
    }

    /*************END AVPlayer#EventListener 实现逻辑***********************/

    private void keepScreenOn(boolean isOn) {
        mPlayerContainer.setKeepScreenOn(isOn);
    }

    /**
     * 获取当前播放器状态
     */
    @PlayerState
    public int getCurrentPlayState() {
        return mPlayerState;
    }

    /**
     * 向Controller设置播放状态，用于控制Controller的ui展示
     */
    protected void setPlayState(@PlayerState int playState) {
        mPlayerState = playState;
        notifyPlayerStateChanged();
    }

    /**
     * 通知播放器状态发生变化
     */
    private void notifyPlayerStateChanged() {
        int playState = mPlayerState;
        if (mVideoController != null) {
            mVideoController.setPlayState(playState);
        }
        if (mPlayerStateChangedListeners != null) {
            for (OnStateChangeListener listener : mPlayerStateChangedListeners) {
                listener.onPlayStateChanged(playState);
            }
        }
    }

    /**
     * 获取当前播放器的状态
     */
    public int getCurrentPlayerState() {
        return mCurrentPlayerState;
    }


    /**
     * 获取缓冲速度
     */
    @Override
    public long getTcpSpeed() {
        return mMediaPlayer != null ? mMediaPlayer.getTcpSpeed() : 0;
    }

    /**
     * 设置播放速度
     */
    @Override
    public void setSpeed(float speed) {
        if (isInPlaybackState()) {
            mMediaPlayer.setSpeed(speed);
        }
    }

    @Override
    public float getSpeed() {
        if (isInPlaybackState()) {
            return mMediaPlayer.getSpeed();
        }
        return 1f;
    }

    /**
     * 一开始播放就seek到预先设置好的位置
     */
    public void skipPositionWhenPlay(int position) {
        this.mCurrentPosition = position;
    }


    /**
     * 循环播放， 默认不循环播放
     */
    public void setLooping(boolean looping) {
        mLooping = looping;
        if (mMediaPlayer != null) {
            mMediaPlayer.setLooping(looping);
        }
    }

    /**
     * 是否开启AudioFocus监听， 默认开启，用于监听其它地方是否获取音频焦点，如果有其它地方获取了
     * 音频焦点，此播放器将做出相应反应，具体实现见{@link AudioFocusHelper}
     */
    public void setEnableAudioFocus(boolean enableAudioFocus) {
        mEnableAudioFocus = enableAudioFocus;
    }

    /**
     * 自定义播放核心，继承{@link AVPlayerFactory}实现自己的播放核心
     */
    public void setPlayerFactory(@NonNull AVPlayerFactory<? extends AVPlayer> playerFactory) {
        mPlayerFactory = playerFactory;
    }


    /**
     * 自定义RenderView，继承{@link RenderFactory}实现自己的RenderView
     */
    public void setRenderViewFactory(@NonNull RenderFactory renderViewFactory) {
        mCustomRenderViewFactory = renderViewFactory;
    }

    /**
     * 设置{@link #mPlayerContainer}的背景色
     */
    public void setPlayerBackgroundColor(int color) {
        mPlayerContainer.setBackgroundColor(color);
    }

    /**
     * 进入全屏
     */
    @Override
    public void startFullScreen() {
        if (mIsFullScreen)
            return;

        ViewGroup decorView = getDecorView();
        if (decorView == null)
            return;

        mIsFullScreen = true;

        //隐藏NavigationBar和StatusBar
        hideSysBar(decorView);

        //从当前FrameLayout中移除播放器视图
        this.removeView(mPlayerContainer);
        //将播放器视图添加到DecorView中即实现了全屏
        decorView.addView(mPlayerContainer);

        setPlayerState(PLAYER_FULL_SCREEN);
    }

    private void hideSysBar(ViewGroup decorView) {
        int uiOptions = decorView.getSystemUiVisibility();
        uiOptions |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            uiOptions |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        decorView.setSystemUiVisibility(uiOptions);
        getActivity().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (hasWindowFocus && mIsFullScreen) {
            //重新获得焦点时保持全屏状态
            hideSysBar(getDecorView());
        }
    }

    /**
     * 退出全屏
     */
    @Override
    public void stopFullScreen() {
        if (!mIsFullScreen)
            return;

        ViewGroup decorView = getDecorView();
        if (decorView == null)
            return;

        mIsFullScreen = false;

        //显示NavigationBar和StatusBar
        showSysBar(decorView);

        //把播放器视图从DecorView中移除并添加到当前FrameLayout中即退出了全屏
        decorView.removeView(mPlayerContainer);
        this.addView(mPlayerContainer);

        setPlayerState(PLAYER_NORMAL);
    }

    private void showSysBar(ViewGroup decorView) {
        int uiOptions = decorView.getSystemUiVisibility();
        uiOptions &= ~View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            uiOptions &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        decorView.setSystemUiVisibility(uiOptions);
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    /**
     * 获取DecorView
     */
    protected ViewGroup getDecorView() {
        Activity activity = getActivity();
        if (activity == null) return null;
        return (ViewGroup) activity.getWindow().getDecorView();
    }

    /**
     * 获取activity中的content view,其id为android.R.id.content
     */
    protected ViewGroup getContentView() {
        Activity activity = getActivity();
        if (activity == null) return null;
        return activity.findViewById(android.R.id.content);
    }

    /**
     * 获取Activity，优先通过Controller去获取Activity
     */
    protected Activity getActivity() {
        Activity activity;
        if (mVideoController != null) {
            activity = PlayerUtils.scanForActivity(mVideoController.getContext());
            if (activity == null) {
                activity = PlayerUtils.scanForActivity(getContext());
            }
        } else {
            activity = PlayerUtils.scanForActivity(getContext());
        }
        return activity;
    }

    /**
     * 判断是否处于全屏状态
     */
    @Override
    public boolean isFullScreen() {
        return mIsFullScreen;
    }

    /**
     * 开启小屏
     */
    public void startTinyScreen() {
        if (mIsTinyScreen) return;
        ViewGroup contentView = getContentView();
        if (contentView == null) return;
        this.removeView(mPlayerContainer);
        int width = mTinyScreenSize[0];
        if (width <= 0) {
            width = PlayerUtils.getScreenWidth(getContext(), false) / 2;
        }

        int height = mTinyScreenSize[1];
        if (height <= 0) {
            height = width * 9 / 16;
        }

        LayoutParams params = new LayoutParams(width, height);
        params.gravity = Gravity.BOTTOM | Gravity.END;
        contentView.addView(mPlayerContainer, params);
        mIsTinyScreen = true;
        setPlayerState(PLAYER_TINY_SCREEN);
    }

    /**
     * 退出小屏
     */
    public void stopTinyScreen() {
        if (!mIsTinyScreen) return;

        ViewGroup contentView = getContentView();
        if (contentView == null) return;
        contentView.removeView(mPlayerContainer);
        LayoutParams params = new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        this.addView(mPlayerContainer, params);

        mIsTinyScreen = false;
        setPlayerState(PLAYER_NORMAL);
    }

    public boolean isTinyScreen() {
        return mIsTinyScreen;
    }

    @Override
    public void onVideoSizeChanged(int videoWidth, int videoHeight) {
        mVideoSize[0] = videoWidth;
        mVideoSize[1] = videoHeight;

        if (mRenderView != null) {
            mRenderView.setScaleType(mScreenScaleType);
            mRenderView.setVideoSize(videoWidth, videoHeight);
        }
    }

    /**
     * 设置控制器，传null表示移除控制器
     */
    public void setVideoController(@Nullable BaseVideoController mediaController) {
        mPlayerContainer.removeView(mVideoController);
        mVideoController = mediaController;
        if (mediaController != null) {
            mediaController.setMediaPlayer(this);
            LayoutParams params = new LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            mPlayerContainer.addView(mVideoController, params);
        }
    }

    /**
     * 设置视频比例
     */
    @Override
    public void setScreenScaleType(int screenScaleType) {
        mScreenScaleType = screenScaleType;
        if (mRenderView != null) {
            mRenderView.setScaleType(screenScaleType);
        }
    }

    /**
     * 设置镜像旋转，暂不支持SurfaceView
     */
    @Override
    public void setMirrorRotation(boolean enable) {
        if (mRenderView != null) {
            mRenderView.getView().setScaleX(enable ? -1 : 1);
        }
    }


    /**
     * 获取视频宽高,其中width: mVideoSize[0], height: mVideoSize[1]
     */
    @Override
    public int[] getVideoSize() {
        return mVideoSize;
    }

    /**
     * 旋转视频画面
     *
     * @param rotation 角度
     */
    @Override
    public void setRotation(float rotation) {
        if (mRenderView != null) {
            mRenderView.setVideoRotation((int) rotation);
        }
    }

    /**
     * 设置小屏的宽高
     *
     * @param tinyScreenSize 其中tinyScreenSize[0]是宽，tinyScreenSize[1]是高
     */
    public void setTinyScreenSize(int[] tinyScreenSize) {
        this.mTinyScreenSize = tinyScreenSize;
    }


    /**
     * 向Controller设置播放器状态，包含全屏状态和非全屏状态
     */
    protected void setPlayerState(int playerState) {
        mCurrentPlayerState = playerState;
        if (mVideoController != null) {
            mVideoController.setPlayerState(playerState);
        }
        if (mPlayerStateChangedListeners != null) {
            for (OnStateChangeListener l : PlayerUtils.getSnapshot(mPlayerStateChangedListeners)) {
                if (l != null) {
                    l.onPlayerStateChanged(playerState);
                }
            }
        }
    }

    /**
     * 播放状态改变监听器
     */
    public interface OnStateChangeListener {
        default void onPlayerStateChanged(int playerState) {
        }

        default void onPlayStateChanged(int playState) {
        }
    }


    /**
     * 添加一个播放状态监听器，播放状态发生变化时将会调用。
     */
    public void addOnStateChangeListener(@NonNull OnStateChangeListener listener) {
        if (mPlayerStateChangedListeners == null) {
            mPlayerStateChangedListeners = new CopyOnWriteArrayList<>();
        }
        mPlayerStateChangedListeners.add(listener);
    }

    /**
     * 移除某个播放状态监听
     */
    public void removeOnStateChangeListener(@NonNull OnStateChangeListener listener) {
        if (mPlayerStateChangedListeners != null) {
            mPlayerStateChangedListeners.remove(listener);
        }
    }

    /**
     * 移除所有播放状态监听
     */
    public void clearOnStateChangeListeners() {
        if (mPlayerStateChangedListeners != null) {
            mPlayerStateChangedListeners.clear();
        }
    }

    /**
     * 改变返回键逻辑，用于activity
     */
    public boolean onBackPressed() {
        return mVideoController != null && mVideoController.onBackPressed();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        L.d("onSaveInstanceState: currentPosition=" + mCurrentPosition);
        //activity切到后台后可能被系统回收，故在此处进行进度保存
        saveCurrentPlayedProgress();
        return super.onSaveInstanceState();
    }

    /**
     * 获取已保存的当前播放进度
     *
     * @return
     */
    private long getSavedPlayedProgress() {
        //读取播放进度
        if (mProgressManager == null)
            return 0;
        return mProgressManager.getSavedProgress(mUrl);
    }

    /**
     * 保存进度
     *
     * @param position 当前播放位置
     */
    private void savePlayedProgress(long position) {
        if (mProgressManager == null) {
            L.w("savePlayedProgress is ignored,ProgressManager is null.");
            return;
        }
        L.d("saveProgress: " + position);
        mProgressManager.saveProgress(mUrl, position);
    }

    /**
     * 保存当前播放位置
     * 只会在已存在播放的情况下才会保存
     */
    private void saveCurrentPlayedProgress() {
        long position = mCurrentPosition;
        if (position <= 0)
            return;
        savePlayedProgress(position);
    }
}
