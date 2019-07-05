package com.dueeeke.dkplayer.activity.api;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;

import com.dueeeke.dkplayer.R;
import com.dueeeke.videocontroller.StandardVideoController;
import com.dueeeke.videoplayer.ijk.IjkPlayer;
import com.dueeeke.videoplayer.player.AbstractPlayer;
import com.dueeeke.videoplayer.player.VideoView;
import com.dueeeke.videoplayer.player.PlayerFactory;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

/**
 * rtsp/concat,只有ijkplayer支持
 */

public class CustomMediaPlayerActivity extends AppCompatActivity {

    private VideoView mVideoView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_media_player);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.str_rtsp_concat);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        String rtspUrl = "rtsp://184.72.239.149/vod/mp4://BigBuckBunny_175k.mov";
//        String rtspUrl = "rtsp://ajj:12345678@218.21.217.122:65523/h264/ch40/sub/av_stream";
//        String rtspUrl = "rtsp://live.3gv.ifeng.com/live/71";
//        String url = "http://storage.gzstv.net/uploads/media/huangmeiyan/jr05-09.mp4";
        //测试concat,将项目根目录的other文件夹中的test.ffconcat文件复制到sd卡根目录测试
//        String absolutePath = Environment.getExternalStorageDirectory().getAbsolutePath();
//        String url = "file://" + absolutePath + File.separator + "test.ffconcat";
        //concat测试地址
        String concatUrl = "http://64.154.38.44/test.ffconcat";

        mVideoView = findViewById(R.id.player);
        StandardVideoController controller = new StandardVideoController(this);
        mVideoView.setVideoController(controller);
        RadioGroup radioGroup = findViewById(R.id.rg_type);
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.concat:
                    mVideoView.setUrl(concatUrl);
                    break;
                case R.id.rtsp:
                    mVideoView.setUrl(rtspUrl);
                    break;
            }
        });
//        mVideoView.setCustomMediaPlayer(new IjkPlayer(this) {
//            @Override
//            public void setOptions() {
//                super.setOptions();
//                //支持concat
//                mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "safe", 0);
//                mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist",
//                        "rtmp,concat,ffconcat,file,subfile,http,https,tls,rtp,tcp,udp,crypto,rtsp");
//                //使用tcp方式拉取rtsp流，默认是通过udp方式
//                mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp");
//            }
//        });

        class MyPlayerFactory extends PlayerFactory {

            @Override
            public AbstractPlayer createPlayer() {
                return new IjkPlayer(CustomMediaPlayerActivity.this) {
                    @Override
                    public void setOptions() {
                        super.setOptions();
                        //支持concat
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "safe", 0);
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "protocol_whitelist",
                                "rtmp,concat,ffconcat,file,subfile,http,https,tls,rtp,tcp,udp,crypto,rtsp");
                        //使用tcp方式拉取rtsp流，默认是通过udp方式
                        mMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp");
                    }
                };
            }
        }

        mVideoView.setPlayerFactory(new MyPlayerFactory());
    }


    public void startPlay(View view) {
        mVideoView.release();
        mVideoView.start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mVideoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mVideoView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mVideoView.release();
    }


    @Override
    public void onBackPressed() {
        if (!mVideoView.onBackPressed()) {
            super.onBackPressed();
        }
    }
}
