package com.dueeeke.dkplayer.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.dueeeke.dkplayer.fragment.list.ListViewFragment;
import com.dueeeke.dkplayer.fragment.list.RecyclerViewAutoPlayFragment;
import com.dueeeke.dkplayer.fragment.list.RecyclerViewFragment;
import com.dueeeke.dkplayer.fragment.list.RecyclerViewPortraitFragment;
import com.dueeeke.dkplayer.fragment.list.SeamlessPlayFragment;
import com.dueeeke.dkplayer.fragment.list.TikTokListFragment;

import java.util.List;

/**
 * List主页适配器
 * Created by dueeeke on 2018/1/3.
 */
public class ListPagerAdapter extends FragmentStatePagerAdapter {

    private List<String> mTitles;

    public ListPagerAdapter(FragmentManager fm, List<String> titles) {
        super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        mTitles = titles;
    }

    @Override
    @NonNull
    public Fragment getItem(int position) {
        switch (position) {
            default:
            case 0:
                return new ListViewFragment();
            case 1:
                return new RecyclerViewFragment();
            case 2:
                return new RecyclerViewAutoPlayFragment();
            case 3:
                return new TikTokListFragment();
            case 4:
                return new RecyclerViewPortraitFragment();
            case 5:
                return new SeamlessPlayFragment();
        }
    }

    @Override
    public int getCount() {
        return mTitles.size();
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return mTitles.get(position);
    }
}
