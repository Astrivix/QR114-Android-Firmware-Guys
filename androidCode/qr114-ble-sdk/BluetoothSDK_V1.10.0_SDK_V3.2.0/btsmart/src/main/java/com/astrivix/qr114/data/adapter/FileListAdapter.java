package com.astrivix.qr114.data.adapter;


import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.module.LoadMoreModule;
import com.chad.library.adapter.base.viewholder.BaseViewHolder;
import com.astrivix.qr114.R;
import com.jieli.filebrowse.bean.FileStruct;

import java.util.ArrayList;


/**
 * Sasanda Saumya
 * 2025-07-26
 *
 */

public class FileListAdapter extends BaseQuickAdapter<FileStruct, BaseViewHolder> implements LoadMoreModule {

    private int selectedCluster;
    private byte devIndex;

    // --- START OF FIX ---
    // We need two constructors now.

    /**
     * This constructor is for child classes (like BellFileListAdapter)
     * that need to provide their own custom layout.
     * @param layoutId The layout resource ID for the item.
     */
    public FileListAdapter(int layoutId) {
        super(layoutId, new ArrayList<>());
    }

    /**
     * This is the default constructor used by your main FilesFragment.
     * It calls the other constructor with the correct design.
     */
    public FileListAdapter() {
        this(R.layout.item_device_file);
    }
    // --- END OF FIX ---


    public byte getDevIndex() {
        return devIndex;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setSelected(byte devIndex, int selectedCluster) {
        this.devIndex = devIndex;
        this.selectedCluster = selectedCluster;
        notifyDataSetChanged();
    }

    public int getSelectedCluster() {
        return selectedCluster;
    }


    @Override
    protected void convert(@NonNull BaseViewHolder holder, FileStruct item) {
        if (item == null) return;

        // This is the logic for the new Reciters/Files design.
        // It will only run if the layout is R.layout.item_device_file.
        // The BellFileListAdapter will override this method, so this code won't affect it.

        ImageView folderArrow = holder.getView(R.id.iv_folder_arrow);
        ImageView fileSelector = holder.getView(R.id.iv_file_selector);
        TextView nameText = holder.getView(R.id.tv_device_file_name);
        ImageView leftIcon = holder.getView(R.id.iv_device_type);

        // Set the name for all items
        if(nameText != null) {
            nameText.setText(item.getName());
        }

        if (item.isFile()) {
            // It's a file, show the selector, hide the arrow
            if(folderArrow != null) folderArrow.setVisibility(View.GONE);
            if(fileSelector != null) fileSelector.setVisibility(View.VISIBLE);

            // Set the left icon to a "file" icon
            if(leftIcon != null) leftIcon.setImageResource(R.drawable.ic_device_file_file);

            // Check selection state
            if (isSelected(item)) {
                if(fileSelector != null) fileSelector.setImageResource(R.drawable.ic_radio_button_checked);
                if(nameText != null) nameText.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                if(fileSelector != null) fileSelector.setImageResource(R.drawable.ic_radio_button_unchecked);
                if(nameText != null) nameText.setTextColor(Color.WHITE);
            }

        } else {
            // It's a folder, show the arrow, hide the selector
            if(folderArrow != null) folderArrow.setVisibility(View.VISIBLE);
            if(fileSelector != null) fileSelector.setVisibility(View.GONE);

            // Set the left icon to a "folder" icon
            if(leftIcon != null) leftIcon.setImageResource(R.drawable.ic_sd_card_green);

            if(nameText != null) nameText.setTextColor(Color.WHITE);
        }
    }


    protected boolean isSelected(FileStruct fileStruct) {
        if (fileStruct == null) {
            return false;
        }
        return fileStruct.getCluster() == selectedCluster && fileStruct.getDevIndex() == devIndex;
    }
}