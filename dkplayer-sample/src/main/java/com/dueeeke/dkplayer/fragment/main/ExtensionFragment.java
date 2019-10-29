package com.dueeeke.dkplayer.fragment.main;

import android.content.Intent;
import android.view.View;

import com.dueeeke.dkplayer.R;
import com.dueeeke.dkplayer.activity.extend.ADActivity;
import com.dueeeke.dkplayer.activity.extend.CacheActivity;
import com.dueeeke.dkplayer.activity.extend.CustomPlayerActivity;
import com.dueeeke.dkplayer.activity.extend.DanmakuActivity;
import com.dueeeke.dkplayer.activity.extend.FullScreenActivity;
import com.dueeeke.dkplayer.activity.extend.PadActivity;
import com.dueeeke.dkplayer.activity.extend.PlayListActivity;
import com.dueeeke.dkplayer.activity.extend.RotateInFullscreenActivity;
import com.dueeeke.dkplayer.activity.extend.SwitchPlayerActivity;
import com.dueeeke.dkplayer.fragment.BaseFragment;

public class ExtensionFragment extends BaseFragment implements View.OnClickListener {
    @Override
    protected int getLayoutResId() {
        return R.layout.fragment_extension;
    }

    @Override
    protected void initViews() {
        super.initViews();
        findViewById(R.id.btn_fullscreen).setOnClickListener(this);
        findViewById(R.id.btn_danmu).setOnClickListener(this);
        findViewById(R.id.btn_ad).setOnClickListener(this);
        findViewById(R.id.btn_fullscreen_rotate).setOnClickListener(this);
        findViewById(R.id.btn_switch_player).setOnClickListener(this);
        findViewById(R.id.btn_proxy_cache).setOnClickListener(this);
        findViewById(R.id.btn_play_list).setOnClickListener(this);
        findViewById(R.id.btn_pad).setOnClickListener(this);
        findViewById(R.id.btn_custom_player).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_fullscreen:
                startActivity(new Intent(getActivity(), FullScreenActivity.class));
                break;
            case R.id.btn_danmu:
                startActivity(new Intent(getActivity(), DanmakuActivity.class));
                break;
            case R.id.btn_ad:
                startActivity(new Intent(getActivity(), ADActivity.class));
                break;
            case R.id.btn_fullscreen_rotate:
                startActivity(new Intent(getActivity(), RotateInFullscreenActivity.class));
                break;
            case R.id.btn_switch_player:
                startActivity(new Intent(getActivity(), SwitchPlayerActivity.class));
                break;
            case R.id.btn_proxy_cache:
                startActivity(new Intent(getActivity(), CacheActivity.class));
                break;
            case R.id.btn_play_list:
                startActivity(new Intent(getActivity(), PlayListActivity.class));
                break;
            case R.id.btn_pad:
                startActivity(new Intent(getActivity(), PadActivity.class));
                break;
            case R.id.btn_custom_player:
                startActivity(new Intent(getActivity(), CustomPlayerActivity.class));
                break;
        }
    }
}
