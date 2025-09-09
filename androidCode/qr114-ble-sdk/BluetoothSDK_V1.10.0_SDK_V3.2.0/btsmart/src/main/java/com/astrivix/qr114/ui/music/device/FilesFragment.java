package com.astrivix.qr114.ui.music.device;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.astrivix.qr114.ui.home.HomeActivity;
import com.chad.library.adapter.base.module.BaseLoadMoreModule;
import com.jieli.bluetooth.bean.device.DeviceInfo;
import com.jieli.bluetooth.bean.device.music.MusicNameInfo;
import com.jieli.bluetooth.bean.device.music.MusicStatusInfo;
import com.jieli.bluetooth.constant.AttrAndFunCode;
import com.jieli.bluetooth.impl.rcsp.RCSPController;
import com.jieli.bluetooth.interfaces.rcsp.callback.BTRcspEventCallback;
import com.astrivix.qr114.R;
import com.astrivix.qr114.data.adapter.FileListAdapter;
import com.astrivix.qr114.data.adapter.FileRouterAdapter;
import com.astrivix.qr114.tool.playcontroller.PlayControlImpl;
import com.astrivix.qr114.util.JL_MediaPlayerServiceManager;
import com.jieli.component.base.Jl_BaseFragment;
import com.jieli.component.utils.HandlerManager;
import com.jieli.component.utils.ToastUtil;
import com.jieli.component.utils.ValueUtil;
import com.jieli.filebrowse.FileBrowseConstant;
import com.jieli.filebrowse.FileBrowseManager;
import com.jieli.filebrowse.bean.FileStruct;
import com.jieli.filebrowse.bean.Folder;
import com.jieli.filebrowse.bean.SDCardBean;
import com.jieli.filebrowse.interfaces.FileObserver;
import com.jieli.bluetooth.utils.JL_Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FilesFragment extends Jl_BaseFragment implements FileObserver {
    private RecyclerView rvFilePathNav;
    private TextView tvHeaderTitle;
    private ImageView ivHeaderIcon;
    private ImageView ivBackButton;

    protected final RCSPController mRCSPController = RCSPController.getInstance();
    private FileBrowseManager mFileBrowseManager;

    private SDCardBean mSdCardBean = new SDCardBean();
    private FileListAdapter mFileListAdapter;
    private BaseLoadMoreModule mLoadMoreModule;
    private final FileRouterAdapter mFileRouterAdapter = new FileRouterAdapter();

    public void setSdCardBean(SDCardBean mSdCardBean) {
        this.mSdCardBean = mSdCardBean;
    }

    public FilesFragment() { /* Required empty constructor */ }

    public static FilesFragment newInstance(SDCardBean sdCardBean) {
        Bundle args = new Bundle();
        FilesFragment fragment = new FilesFragment();
        fragment.mSdCardBean = sdCardBean;
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_files, container, false);
        RecyclerView rvDeviceFiles = view.findViewById(R.id.rv_device_files);
        rvFilePathNav = view.findViewById(R.id.rv_file_path_nav);
        tvHeaderTitle = view.findViewById(R.id.tv_header_title);
        ivHeaderIcon = view.findViewById(R.id.iv_header_icon);
        ivBackButton = view.findViewById(R.id.iv_back_button);

        ivBackButton.setOnClickListener(v -> {
            Folder current = getFileBrowseManager().getCurrentReadFile(mSdCardBean);
            if (current != null && current.getParent() != null) {
                mFileListAdapter.setNewInstance(new ArrayList<>());
                getFileBrowseManager().backBrowse(mSdCardBean, true);
                refreshFileRouterView(getFileBrowseManager().getCurrentReadFile(mSdCardBean));
            }
        });

        rvFilePathNav.setLayoutManager(new LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false));
        rvFilePathNav.setAdapter(mFileRouterAdapter);
        mFileRouterAdapter.setOnItemClickListener((adapter, view12, position) -> handleFileRouterClick(position));

        mFileListAdapter = createFileAdapter();
        rvDeviceFiles.setAdapter(mFileListAdapter);
        rvDeviceFiles.setLayoutManager(new LinearLayoutManager(getContext()));
        mLoadMoreModule = mFileListAdapter.getLoadMoreModule();
        mLoadMoreModule.setOnLoadMoreListener(this::onLoadMoreRequested);

        if (mSdCardBean == null) {
            return view;
        }

        if (mRCSPController.isDeviceConnected()) {
            mFileListAdapter.setSelected(mRCSPController.getDeviceInfo().getCurrentDevIndex()
                    , mRCSPController.getDeviceInfo().getCluster());
        }
        createEmptyView();
        mFileListAdapter.setOnItemClickListener((adapter, view1, position) -> {
            FileStruct fileStruct = mFileListAdapter.getItem(position);
            JL_Log.d(TAG, "click file item -->" + position + "\treading-->" + getFileBrowseManager().isReading());
            if (fileStruct != null && fileStruct.isFile()) {
                handleFileClick(fileStruct);
            } else {
                handleFolderClick(fileStruct);
            }
        });

        initWithCurrentPath();
        mRCSPController.addBTRcspEventCallback(mBTEventCallback);
        getFileBrowseManager().addFileObserver(this);
        return view;
    }

    private FileBrowseManager getFileBrowseManager() {
        if (mFileBrowseManager == null) {
            mFileBrowseManager = FileBrowseManager.getInstance();
        }
        return mFileBrowseManager;
    }

    private void initWithCurrentPath() {
        Folder currentFolder = getFileBrowseManager().getCurrentReadFile(mSdCardBean);
        if (currentFolder != null) {
            refreshFileRouterView(getFileBrowseManager().getCurrentReadFile(mSdCardBean));
            mFileListAdapter.setNewInstance(currentFolder.getChildFileStructs());
            if (mFileListAdapter.getData().size() < 1 && !currentFolder.isLoadFinished(false)) {
                onLoadMoreRequested();
            } else if (currentFolder.isLoadFinished(false)) {
                mLoadMoreModule.loadMoreEnd();
            }
        }
    }

    protected FileListAdapter createFileAdapter() { return new FileListAdapter(); }
    protected FileListAdapter getFileListAdapter() { return mFileListAdapter; }

    // --- START: CORRECTED METHOD ---
    protected void handleFileClick(FileStruct fileStruct) {
        if (getFileBrowseManager().isReading()) {
            ToastUtil.showToastShort(R.string.msg_read_file_err_reading);
            return;
        }

        // Pass the context to the player controller
        // Get the current folder directly from the manager at the moment of the click.
        Folder currentFolder = getFileBrowseManager().getCurrentReadFile(mSdCardBean);
        if (currentFolder != null && currentFolder.getFileStruct() != null) {
            // We have the folder, so pass its name (the reciter name) to the player.
            String reciterName = currentFolder.getFileStruct().getName();
            PlayControlImpl.getInstance().onFileSelectedToPlay(reciterName, fileStruct.getCluster());
        } else {
            // This is a safety check. If we can't get the folder, play without the name.
            JL_Log.e(TAG, "Could not get current folder. Playing without reciter name.");
            PlayControlImpl.getInstance().onFileSelectedToPlay("", fileStruct.getCluster());
        }

        boolean isPlaying = JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer().isPlaying();
        if (isPlaying) {
            JL_MediaPlayerServiceManager.getInstance().getJl_mediaPlayer().pause();
        }

        HandlerManager.getInstance().getMainHandler().postDelayed(() -> {
            if (!mRCSPController.isDeviceConnected()) return;
            getFileBrowseManager().playFile(fileStruct, mSdCardBean);
            PlayControlImpl.getInstance().updateMode(PlayControlImpl.MODE_MUSIC);
            mRCSPController.getDeviceInfo().setCurrentDevIndex(fileStruct.getDevIndex());
            mRCSPController.getDeviceInfo().setCluster(fileStruct.getCluster());
            mFileListAdapter.setSelected(fileStruct.getDevIndex(), fileStruct.getCluster());
        }, isPlaying ? 100 : 0);
    }
    // --- END: CORRECTED METHOD ---

    protected void handleFolderClick(FileStruct fileStruct) {
        showEmptyView(false);
        int ret = getFileBrowseManager().appenBrowse(fileStruct, mSdCardBean);
        if (ret == FileBrowseConstant.ERR_READING) {
            ToastUtil.showToastShort(R.string.msg_read_file_err_reading);
        } else if (ret == FileBrowseConstant.ERR_OFFLINE) {
            ToastUtil.showToastShort(R.string.msg_read_file_err_offline);
        } else if (ret == FileBrowseConstant.ERR_BEYOND_MAX_DEPTH) {
            ToastUtil.showToastShort(R.string.msg_read_file_err_beyond_max_depth);
        } else if (ret == FileBrowseConstant.SUCCESS) {
            mFileListAdapter.setNewInstance(new ArrayList<>());
            refreshFileRouterView(getFileBrowseManager().getCurrentReadFile(mSdCardBean));
        }
    }

    private void handleFileRouterClick(int position) {
        FileStruct fileStruct = mFileRouterAdapter.getItem(position);
        Folder current = getFileBrowseManager().getCurrentReadFile(mSdCardBean);
        if (current == null || current.getFileStruct().getCluster() == fileStruct.getCluster()) {
            return;
        }
        while (current.getParent() != null && current.getParent().getFileStruct().getCluster() != fileStruct.getCluster()) {
            getFileBrowseManager().backBrowse(mSdCardBean, false);
            current = getFileBrowseManager().getCurrentReadFile(mSdCardBean);
        }
        mFileListAdapter.setNewInstance(new ArrayList<>());
        getFileBrowseManager().backBrowse(mSdCardBean, true);
        refreshFileRouterView(getFileBrowseManager().getCurrentReadFile(mSdCardBean));
    }


    private void refreshFileRouterView(Folder folder) {
        if (folder == null) return;

        if (tvHeaderTitle != null && ivBackButton != null && ivHeaderIcon != null) {
            if (folder.getParent() == null) {
                // We are at the root (Reciters list)
                tvHeaderTitle.setText("Reciters");
                ivBackButton.setVisibility(View.VISIBLE);
                ivHeaderIcon.setVisibility(View.VISIBLE);
                ivBackButton.setOnClickListener(view -> {
                    Intent intent = new Intent(getContext(), HomeActivity.class);
                    startActivity(intent);
                });
            } else {
                // We are inside a folder (File list)
                tvHeaderTitle.setText(folder.getFileStruct().getName());
                ivBackButton.setVisibility(View.VISIBLE);
                ivHeaderIcon.setVisibility(View.VISIBLE);
            }
        }

        List<FileStruct> list = new ArrayList<>();
        list.add(folder.getFileStruct());
        while (folder.getParent() != null) {
            folder = (Folder) folder.getParent();
            list.add(0, folder.getFileStruct());
        }
        mFileRouterAdapter.setNewInstance(list);
        if(rvFilePathNav != null) {
            rvFilePathNav.scrollToPosition(mFileRouterAdapter.getData().size() - 1);
        }
    }

    @Override
    public void onDestroyView() {
        mRCSPController.removeBTRcspEventCallback(mBTEventCallback);
        if (mFileBrowseManager != null) {
            mFileBrowseManager.removeFileObserver(this);
            mFileBrowseManager = null;
        }
        super.onDestroyView();
    }


    @Override
    public void onFileReceiver(List<FileStruct> fileStructs) {
        if (mFileListAdapter != null && getActivity() != null && !getActivity().isDestroyed()) {
            mFileListAdapter.addData(fileStructs);
        }
    }

    @Override
    public void onFileReadStop(boolean isEnd) {
        if (isEnd) {
            mLoadMoreModule.loadMoreEnd();
        } else {
            mLoadMoreModule.loadMoreComplete();
        }
        showEmptyView(true);
    }

    @Override
    public void OnFlayCallback(boolean success) {
        if (mRCSPController.isDeviceConnected() && success) {
            mRCSPController.getDeviceMusicInfo(mRCSPController.getUsingDevice(), null);
        }
    }

    @Override
    public void onSdCardStatusChange(List<SDCardBean> onLineCards) {
    }


    protected final BTRcspEventCallback mBTEventCallback = new BTRcspEventCallback() {
        @Override
        public void onMusicNameChange(BluetoothDevice device, MusicNameInfo nameInfo) {
            if (mFileListAdapter != null && isAdded() && !isDetached()) {
                mFileListAdapter.setSelected(mFileListAdapter.getDevIndex(), nameInfo.getCluster());
            }
        }

        @Override
        public void onMusicStatusChange(BluetoothDevice device, MusicStatusInfo statusInfo) {
            DeviceInfo deviceInfo = mRCSPController.getDeviceInfo(device);
            if (deviceInfo != null && deviceInfo.getDevStorageInfo() != null && deviceInfo.getDevStorageInfo().isDeviceReuse()) {
                if (statusInfo.getCurrentDev() != mSdCardBean.getDevHandler()) {
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                    return;
                }
            }
            if (mFileListAdapter != null && isAdded() && !isDetached()) {
                mFileListAdapter.setSelected((byte) statusInfo.getCurrentDev(), mFileListAdapter.getSelectedCluster());
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onDeviceModeChange(BluetoothDevice device, int mode) {
            if (mode != AttrAndFunCode.SYS_INFO_FUNCTION_MUSIC) {
                mFileListAdapter.notifyDataSetChanged();
            }
        }
    };

    private void onLoadMoreRequested() {
        int ret = getFileBrowseManager().loadMore(mSdCardBean);
        if (ret == FileBrowseConstant.ERR_LOAD_FINISHED) {
            mLoadMoreModule.loadMoreEnd();
        } else if (ret == FileBrowseConstant.ERR_READING) {
            mLoadMoreModule.loadMoreComplete();
        } else if (ret != FileBrowseConstant.SUCCESS) {
            mLoadMoreModule.loadMoreFail();
        }
    }


    @Override
    public void onFileReadStart() {
        showEmptyView(false);
    }

    @Override
    public void onFileReadFailed(int reason) {
        mLoadMoreModule.loadMoreFail();
    }


    private void createEmptyView() {
        TextView textView = new TextView(getContext());
        textView.setText(getString(R.string.empty_folder));
        textView.setTextSize(16);
        textView.setTextColor(getResources().getColor(R.color.gray_text_5A5A5A));
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(0, R.drawable.ic_folder_img_empty, 0, 0);
        textView.setGravity(Gravity.CENTER);
        textView.setPadding(0, ValueUtil.dp2px(getContext(), 98), 0, 0);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setLayoutParams(layoutParams);
        mFileListAdapter.setEmptyView(textView);
        mFileListAdapter.setUseEmpty(false);
    }


    private void showEmptyView(boolean show) {
        if (mFileListAdapter.isUseEmpty() != show) {
            mFileListAdapter.setUseEmpty(show);
            mFileListAdapter.notifyDataSetChanged();
        }
    }
}