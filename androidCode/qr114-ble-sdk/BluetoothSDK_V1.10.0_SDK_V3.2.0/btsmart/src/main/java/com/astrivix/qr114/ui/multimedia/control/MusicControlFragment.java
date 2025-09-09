package com.astrivix.qr114.ui.multimedia.control;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.jieli.audio.media_player.JL_PlayMode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.utils.JL_Log;
import com.astrivix.qr114.R;
import com.astrivix.qr114.tool.playcontroller.PlayControlCallback;
import com.astrivix.qr114.tool.playcontroller.PlayControlImpl;
import com.astrivix.qr114.ui.ContentActivity;
import com.astrivix.qr114.ui.music.device.ContainerFragment;
import com.astrivix.qr114.ui.music.local.LocalMusicFragment;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.filebrowse.bean.SDCardBean;


/**
 * device music controller
 */
public class MusicControlFragment extends Jl_BaseFragment implements SeekBar.OnSeekBarChangeListener {
    private final RCSPController mRCSPController = RCSPController.getInstance();

    private TextView tvTitle;
    private TextView tvStartTime;
    private SeekBar sbMusic;
    private TextView tvEndTime;
    private ImageButton ibPlaymode;
    private ImageButton ibPlaylast;
    private ImageButton ibPlayOrPause;
    private ImageButton ibPlaynext;
    private ImageButton ibPlaylist;
    private TextView tv_reciter_name;


    public MusicControlFragment() {

    }

    public static MusicControlFragment newInstance() {
        return new MusicControlFragment();
    }

    public static Fragment newInstanceForCache(FragmentManager fragmentManager) {
        if (fragmentManager.findFragmentByTag(MusicControlFragment.class.getSimpleName()) != null) {
            return fragmentManager.findFragmentByTag(MusicControlFragment.class.getSimpleName());
        }
        return new MusicControlFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_music_control, container, false);
        tvTitle = view.findViewById(R.id.tv_title);
        tvStartTime = view.findViewById(R.id.tv_start_time);
        sbMusic = view.findViewById(R.id.sb_music);
        tvEndTime = view.findViewById(R.id.tv_end_time);
        ibPlaymode = view.findViewById(R.id.ib_playmode);
        ibPlaylast = view.findViewById(R.id.ib_playlast);
        ibPlayOrPause = view.findViewById(R.id.ib_play_or_pause);
        ibPlaynext = view.findViewById(R.id.ib_playnext);
        ibPlaylist = view.findViewById(R.id.ib_playlist);
        tv_reciter_name = view.findViewById(R.id.tv_reciter_name);

        ibPlaymode.setOnClickListener(mOnClickListener);
        ibPlaylast.setOnClickListener(mOnClickListener);
        ibPlayOrPause.setOnClickListener(mOnClickListener);
        ibPlaynext.setOnClickListener(mOnClickListener);
        ibPlaylist.setOnClickListener(mOnClickListener);
        sbMusic.setOnSeekBarChangeListener(this);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        PlayControlImpl.getInstance().registerPlayControlListener(mControlCallback);
        PlayControlImpl.getInstance().refresh();
        JL_Log.e("sen", "MusicControlFragment     onActivityCreated ");

        // Set the initial reciter name when the fragment is created
        updateReciterName();
    }

    @Override
    public void onDestroyView() {
        JL_Log.e("sen", "MusicControlFragment     onDestroyView ");
        PlayControlImpl.getInstance().unregisterPlayControlListener(mControlCallback);
        super.onDestroyView();
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int id = v.getId();
            if (id == R.id.ib_playmode) {
                PlayControlImpl.getInstance().setNextPlaymode();
            } else if (id == R.id.ib_playlast) {
                PlayControlImpl.getInstance().playPre();
            } else if (id == R.id.ib_play_or_pause) {
                PlayControlImpl.getInstance().playOrPause();
            } else if (id == R.id.ib_playnext) {
                PlayControlImpl.getInstance().playNext();
            } else if (id == R.id.ib_playlist) {
                toMusicListByFun();
            }
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        PlayControlImpl.getInstance().onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        PlayControlImpl.getInstance().onPause();
    }


    private void toMusicListByFun() {
        if (!mRCSPController.isDeviceConnected()) return;
        if (PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_BT) {
            ContentActivity.startActivity(requireContext(), LocalMusicFragment.class.getCanonicalName(), getString(R.string.multi_media_local));
        } else if (PlayControlImpl.getInstance().getMode() == PlayControlImpl.MODE_MUSIC) {
            JL_Log.d(TAG, "toMusicListByFun : >>> music , " +  mRCSPController.getDeviceInfo().getCurrentDevIndex());
            Bundle bundle = new Bundle();
            bundle.putInt(ContainerFragment.KEY_TYPE, mRCSPController.getDeviceInfo().getCurrentDevIndex() == SDCardBean.INDEX_USB ? SDCardBean.USB : SDCardBean.SD);
            bundle.putInt(ContainerFragment.KEY_DEVICE_INDEX, mRCSPController.getDeviceInfo().getCurrentDevIndex());
            ContentActivity.startActivity(requireContext(), ContainerFragment.class.getCanonicalName(), bundle);
        }
    }

    private boolean isFragmentAlive() {
        return isAdded() && !isDetached();
    }

    /**
     * Helper method to update the reciter name TextView
     */
    private void updateReciterName() {
        if (!isFragmentAlive() || tv_reciter_name == null) return;
        // Call the new method from PlayControlImpl
        String reciterName = PlayControlImpl.getInstance().getCurrentReciterName();
        tv_reciter_name.setText(reciterName);
    }

    private final PlayControlCallback mControlCallback = new PlayControlCallback() {
        @Override
        public void onTitleChange(String title) {
            super.onTitleChange(title);
            JL_Log.d(TAG, "onTitleChange : " + title);
            JL_Log.d("sasa", "onTitleChange : " + title);

            if (isFragmentAlive()) {
                // Remove ".mp3" if it exists sasa
                if (title != null && title.endsWith(".mp3")) {
                    title = title.replaceAll("\\.mp3$", "");
                }
                tvTitle.setText(title);

                // Update reciter name whenever the song title changes
                updateReciterName();
            }
        }

        @Override
        public void onModeChange(int mode) {
            super.onModeChange(mode);
            if (isResumed() && mode == PlayControlImpl.MODE_MUSIC) {
                PlayControlImpl.getInstance().onStart();
            }
        }

        @Override
        public void onTimeChange(int current, int total) {
            super.onTimeChange(current, total);
            JL_Log.d(TAG, "onTimeChange : " + current + " : " + total + ", " + sbMusic.isPressed() + ", isFragmentAlive = " + isFragmentAlive());
            if (!isFragmentAlive()) return;
            if (!sbMusic.isPressed()) {
                sbMusic.setMax(total);
                sbMusic.setProgress(current);
            }
            tvEndTime.setText(PlayControlImpl.formatTime(total));
            tvStartTime.setText(PlayControlImpl.formatTime(current));
        }

        @Override
        public void onPlayModeChange(JL_PlayMode mode) {
            super.onPlayModeChange(mode);
            if (!isFragmentAlive()) return;
            int resId = R.drawable.ic_playmode_circle_selector;
            if (mode != null) {
                switch (mode) {
                    case ONE_LOOP:
                        resId = R.drawable.ic_playmode_single_selector;
                        break;
                    case ALL_LOOP:
                        resId = R.drawable.ic_playmode_circle_selector;
                        break;
                    case ALL_RANDOM:
                        resId = R.drawable.ic_playmode_random_selector;
                        break;
                    case SEQUENCE:
                        resId = R.drawable.ic_playmode_sequence_nor;
                        break;
                    case FOLDER_LOOP:
                        resId = R.drawable.ic_playmode_folder_loop_selector;
                        break;
                    case DEVICE_LOOP:
                        resId = R.drawable.ic_playmode_device_loop_selector;
                        break;
                }
            }
            ibPlaymode.setImageResource(resId);
        }

        @Override
        public void onPlayStateChange(boolean isPlay) {
            super.onPlayStateChange(isPlay);
            if (!isFragmentAlive()) return;
            tvTitle.setSelected(isPlay);
            ibPlayOrPause.setSelected(isPlay);
        }

    };

    // In your file browsing fragment (e.g., FilesFragment.java)


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        int progress = seekBar.getProgress();
        PlayControlImpl.getInstance().seekTo(progress);
    }
}