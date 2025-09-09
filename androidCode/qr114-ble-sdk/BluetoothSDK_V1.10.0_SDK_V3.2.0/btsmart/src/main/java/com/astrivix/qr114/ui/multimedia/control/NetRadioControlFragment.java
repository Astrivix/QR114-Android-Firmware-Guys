package com.astrivix.qr114.ui.multimedia.control;

import android.util.Log;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.astrivix.qr114.R;
import com.astrivix.qr114.databinding.FragmentNetRadioControlBinding;
import com.astrivix.qr114.tool.playcontroller.PlayControlCallback;
import com.astrivix.qr114.tool.playcontroller.PlayControlImpl;
import com.astrivix.qr114.tool.room.DataRepository;
import com.astrivix.qr114.tool.room.NetRadioUpdateSelectData;
import com.astrivix.qr114.ui.CommonActivity;
import com.astrivix.qr114.ui.base.BaseViewModelFragment;
import com.astrivix.qr114.ui.music.net_radio.NetRadioFragment;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class NetRadioControlFragment extends BaseViewModelFragment<FragmentNetRadioControlBinding> {

    public NetRadioControlFragment() {
    }

    public static Fragment newInstance() {
        return new NetRadioControlFragment();
    }


    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(NetRadioControlFragment.class.getSimpleName()) != null) {
            return fragmentManager.findFragmentByTag(NetRadioControlFragment.class.getSimpleName());
        }
        return new NetRadioControlFragment();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PlayControlImpl.getInstance().unregisterPlayControlListener(mControlCallback);
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_net_radio_control;
    }

    @Override
    public void actionsOnViewInflate() {
        super.actionsOnViewInflate();
        mBinding.ibNetRadioControlLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayControlImpl.getInstance().playPre();
            }
        });
        mBinding.ibNetRadioControlPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayControlImpl.getInstance().playOrPause();
            }
        });
        mBinding.ibNetRadioControlNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayControlImpl.getInstance().playNext();
            }
        });
        mBinding.tvNetRadioControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommonActivity.startCommonActivity(getActivity(), NetRadioFragment.class.getCanonicalName());
            }
        });
        PlayControlImpl.getInstance().registerPlayControlListener(mControlCallback);
        PlayControlImpl.getInstance().refresh();
    }


    private final PlayControlCallback mControlCallback = new PlayControlCallback() {
        @Override
        public void onTitleChange(String title) {
            super.onTitleChange(title);
            if (null == title) return;
            if (PlayControlImpl.getInstance().getMode() != PlayControlImpl.MODE_NET_RADIO) return;
            mBinding.tvNetRadioControl.setText(title);
            NetRadioUpdateSelectData currentEntity = new NetRadioUpdateSelectData();
            currentEntity.setTitle(title);
            currentEntity.setSelected(true);
            ArrayList<NetRadioUpdateSelectData> list = new ArrayList<>();
            list.add(currentEntity);
            DataRepository.getInstance().updateNetRadioCurrentPlayInfo(list, null);
        }

        @Override
        public void onModeChange(int mode) {
            super.onModeChange(mode);
        }

        @Override
        public void onPlayStateChange(boolean isPlay) {
            super.onPlayStateChange(isPlay);
            mBinding.ibNetRadioControlPlay.setSelected(isPlay);
        }

        @Override
        public void onTimeChange(int current, int total) {
            super.onTimeChange(current, total);
            Log.e(TAG, "onTimeChange: current:" + current + "total:" + total);
        }

    };

}
