package com.dueeeke.videocontroller.component;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dueeeke.videocontroller.R;
import com.dueeeke.videoplayer.controller.IControlComponent;
import com.dueeeke.videoplayer.controller.MediaPlayerControlWrapper;
import com.dueeeke.videoplayer.player.VideoView;
import com.dueeeke.videoplayer.util.PlayerUtils;

/**
 * 直播底部控制栏
 */
public class LiveControlView extends FrameLayout implements IControlComponent, View.OnClickListener {

    private MediaPlayerControlWrapper mMediaPlayer;

    private ImageView mFullScreen;
    private LinearLayout mBottomContainer;
    private ImageView mPlayButton;

    private ObjectAnimator mShowAnimator;
    private ObjectAnimator mHideAnimator;

    public LiveControlView(@NonNull Context context) {
        super(context);
    }

    public LiveControlView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public LiveControlView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    
    
    {
        setVisibility(GONE);
        LayoutInflater.from(getContext()).inflate(R.layout.dkplayer_layout_live_control_view, this, true);
        mFullScreen = findViewById(R.id.fullscreen);
        mFullScreen.setOnClickListener(this);
        mBottomContainer = findViewById(R.id.bottom_container);
        mPlayButton = findViewById(R.id.iv_play);
        mPlayButton.setOnClickListener(this);
        ImageView refresh = findViewById(R.id.iv_refresh);
        refresh.setOnClickListener(this);

        mShowAnimator = ObjectAnimator.ofFloat(this, "alpha", 0, 1);
        mShowAnimator.setDuration(300);
        mHideAnimator = ObjectAnimator.ofFloat(this, "alpha", 1, 0);
        mHideAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                setVisibility(GONE);
            }
        });
        mHideAnimator.setDuration(300);

    }

    @Override
    public void show() {
        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
            mHideAnimator.cancel();
            mShowAnimator.start();
        }
    }

    @Override
    public void hide() {
        if (getVisibility() == VISIBLE) {
            mShowAnimator.cancel();
            mHideAnimator.start();
        }
    }

    @Override
    public void onPlayStateChanged(int playState) {
        switch (playState) {
            case VideoView.STATE_IDLE:
            case VideoView.STATE_START_ABORT:
            case VideoView.STATE_PREPARING:
            case VideoView.STATE_PREPARED:
            case VideoView.STATE_ERROR:
            case VideoView.STATE_PLAYBACK_COMPLETED:
                setVisibility(GONE);
                break;
            case VideoView.STATE_PLAYING:
                mPlayButton.setSelected(true);
                break;
            case VideoView.STATE_PAUSED:
                mPlayButton.setSelected(false);
            case VideoView.STATE_BUFFERING:
            case VideoView.STATE_BUFFERED:
                mPlayButton.setSelected(mMediaPlayer.isPlaying());
                break;
        }
    }

    @Override
    public void onPlayerStateChanged(int playerState) {
        switch (playerState) {
            case VideoView.PLAYER_NORMAL:
                mFullScreen.setSelected(false);
                break;
            case VideoView.PLAYER_FULL_SCREEN:
                mFullScreen.setSelected(true);
                break;
        }
    }

    @Override
    public void attach(MediaPlayerControlWrapper mediaPlayer) {
        mMediaPlayer = mediaPlayer;
    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public void adjustPortrait(int space) {
        mBottomContainer.setPadding(0, 0, 0, 0);
    }

    @Override
    public void adjustLandscape(int space) {
        mBottomContainer.setPadding(space, 0, 0, 0);
    }

    @Override
    public void adjustReserveLandscape(int space) {
        mBottomContainer.setPadding(0, 0, space, 0);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.fullscreen) {
            toggleFullScreen();
        } else if (id == R.id.iv_play) {
            mMediaPlayer.togglePlay();
        } else if (id == R.id.iv_refresh) {
            mMediaPlayer.replay(true);
        }
    }

    /**
     * 横竖屏切换
     */
    private void toggleFullScreen() {
        Activity activity = PlayerUtils.scanForActivity(getContext());
        if (activity == null || activity.isFinishing()) return;
        mMediaPlayer.toggleFullScreen(activity);
    }
}
