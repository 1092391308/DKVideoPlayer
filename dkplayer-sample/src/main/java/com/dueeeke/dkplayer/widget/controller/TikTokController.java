package com.dueeeke.dkplayer.widget.controller;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.dueeeke.dkplayer.R;
import com.dueeeke.videocontroller.StatusView;
import com.dueeeke.videoplayer.controller.BaseVideoController;
import com.dueeeke.videoplayer.controller.MediaPlayerControl;
import com.dueeeke.videoplayer.player.VideoView;
import com.dueeeke.videoplayer.util.L;

/**
 * 抖音
 * Created by xinyu on 2018/1/6.
 */

public class TikTokController extends BaseVideoController<MediaPlayerControl> {

    private ImageView thumb;
    protected StatusView mStatusView;
    public TikTokController(@NonNull Context context) {
        super(context);
    }

    public TikTokController(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TikTokController(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.layout_tiktok_controller;
    }

    @Override
    protected void initView() {
        super.initView();
        thumb = mControllerView.findViewById(R.id.iv_thumb);

        mStatusView = new StatusView(getContext());
    }

    @Override
    public void setMediaPlayer(MediaPlayerControl mediaPlayer) {
        super.setMediaPlayer(mediaPlayer);
        mStatusView.attachMediaPlayer(mMediaPlayer);
    }

    @Override
    public void setPlayState(int playState) {
        super.setPlayState(playState);

        switch (playState) {
            case VideoView.STATE_IDLE:
                L.e("STATE_IDLE " + hashCode());
                thumb.setVisibility(VISIBLE);
                mStatusView.dismiss();
                break;
            case VideoView.STATE_PLAYING:
                L.e("STATE_PLAYING " + hashCode());
                thumb.setVisibility(GONE);
                break;
            case VideoView.STATE_PAUSED:
                L.e("STATE_PAUSED " + hashCode());
                thumb.setVisibility(GONE);
                break;
            case VideoView.STATE_PREPARED:
                L.e("STATE_PREPARED " + hashCode());
                break;
            case VideoView.STATE_ERROR:
                mStatusView.showErrorView(this);
                break;
        }
    }

    @Override
    public boolean showNetWarning() {
        //不显示移动网络播放警告
        return false;
    }

    public ImageView getThumb() {
        return thumb;
    }
}
