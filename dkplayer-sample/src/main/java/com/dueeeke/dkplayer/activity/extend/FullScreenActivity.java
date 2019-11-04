package com.dueeeke.dkplayer.activity.extend;

import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.dueeeke.dkplayer.R;
import com.dueeeke.dkplayer.activity.BaseActivity;
import com.dueeeke.videocontroller.StandardVideoController;
import com.dueeeke.videocontroller.component.CompleteView;
import com.dueeeke.videocontroller.component.ErrorView;
import com.dueeeke.videocontroller.component.PrepareView;
import com.dueeeke.videocontroller.component.TitleView;
import com.dueeeke.videocontroller.component.VodControlView;
import com.dueeeke.videoplayer.player.VideoView;
import com.dueeeke.videoplayer.util.PlayerUtils;

/**
 * 全屏播放
 * Created by dueeeke on 2017/4/21.
 */

public class FullScreenActivity extends BaseActivity<VideoView> {

    private StandardVideoController mController;

    @Override
    protected View getContentView() {
        mVideoView = new VideoView(this);
        adaptCutoutAboveAndroidP();
        return mVideoView;
    }

    @Override
    protected int getTitleResId() {
        return R.string.str_fullscreen_directly;
    }

    @Override
    protected void initView() {
        super.initView();
        mVideoView.startFullScreen();
        mVideoView.setUrl("http://vfx.mtime.cn/Video/2019/03/12/mp4/190312143927981075.mp4");
        mController = new StandardVideoController(this);
        mController.addControlComponent(new CompleteView(this));
        mController.addControlComponent(new ErrorView(this));
        mController.addControlComponent(new PrepareView(this));
        TitleView titleView = new TitleView(this);
        titleView.findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        titleView.setTitle(getString(R.string.str_fullscreen_directly));
        mController.addControlComponent(titleView);
        VodControlView vodControlView = new VodControlView(this);
        vodControlView.findViewById(R.id.fullscreen).setVisibility(View.GONE);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) vodControlView.findViewById(R.id.total_time).getLayoutParams();
        lp.rightMargin = PlayerUtils.dp2px(this, 16);
        mController.addControlComponent(vodControlView);
        mVideoView.setVideoController(mController);
        mVideoView.setScreenScaleType(VideoView.SCREEN_SCALE_16_9);
        mVideoView.start();
    }

    private void adaptCutoutAboveAndroidP() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }
    }


    @Override
    public void onBackPressed() {
        if (!mController.isLocked()) {
            finish();
        }
    }
}
