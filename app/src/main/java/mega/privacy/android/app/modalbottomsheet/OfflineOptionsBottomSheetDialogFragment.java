package mega.privacy.android.app.modalbottomsheet;

import static mega.privacy.android.app.utils.Constants.HANDLE;
import static mega.privacy.android.app.utils.Constants.THUMB_MARGIN_DP;
import static mega.privacy.android.app.utils.Constants.THUMB_SIZE_DP;
import static mega.privacy.android.app.utils.FileUtil.getFileFolderInfo;
import static mega.privacy.android.app.utils.FileUtil.getFileInfo;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.util.Collections;

import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.utils.OfflineUtils;
import timber.log.Timber;

public class OfflineOptionsBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaOffline nodeOffline = null;

    private File file;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        contentView = View.inflate(getContext(), R.layout.bottom_sheet_offline_item, null);
        itemsLayout = contentView.findViewById(R.id.items_layout);

        if (savedInstanceState != null) {
            String handle = savedInstanceState.getString(HANDLE);
            nodeOffline = dbH.findByHandle(handle);
        } else if (requireActivity() instanceof ManagerActivity) {
            nodeOffline = ((ManagerActivity) requireActivity()).getSelectedOfflineNode();
        }

        return contentView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        contentView.findViewById(R.id.option_download_layout).setOnClickListener(this);
        contentView.findViewById(R.id.option_properties_layout).setOnClickListener(this);
        TextView optionInfoText = contentView.findViewById(R.id.option_properties_text);

        SimpleDraweeView nodeThumb = contentView.findViewById(R.id.offline_thumbnail);
        TextView nodeName = contentView.findViewById(R.id.offline_name_text);
        TextView nodeInfo = contentView.findViewById(R.id.offline_info_text);
        LinearLayout optionOpenWith = contentView.findViewById(R.id.option_open_with_layout);
        LinearLayout optionShare = contentView.findViewById(R.id.option_share_layout);

        contentView.findViewById(R.id.option_delete_offline_layout).setOnClickListener(this);
        optionOpenWith.setOnClickListener(this);
        optionShare.setOnClickListener(this);

        View separatorOpen = contentView.findViewById(R.id.separator_open);

        nodeName.setMaxWidth(scaleWidthPx(200, getResources().getDisplayMetrics()));
        nodeInfo.setMaxWidth(scaleWidthPx(200, getResources().getDisplayMetrics()));

        if (nodeOffline != null) {
            optionInfoText.setText(R.string.general_info);

            if (nodeOffline.isFolder()) {
                optionOpenWith.setVisibility(View.GONE);
                separatorOpen.setVisibility(View.GONE);
            } else {
                optionOpenWith.setVisibility(View.VISIBLE);
                separatorOpen.setVisibility(View.VISIBLE);
            }

            nodeName.setText(nodeOffline.getName());

            Timber.d("Set node info");
            file = OfflineUtils.getOfflineFile(requireContext(), nodeOffline);
            if (!isFileAvailable(file)) return;

            if (file.isDirectory()) {
                nodeInfo.setText(getFileFolderInfo(file, requireContext()));
            } else {
                nodeInfo.setText(getFileInfo(file, requireContext()));
            }

            if (file.isFile()) {
                if (MimeTypeList.typeForName(nodeOffline.getName()).isImage()) {
                    if (file.exists()) {
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) nodeThumb.getLayoutParams();
                        params.height = params.width = dp2px(THUMB_SIZE_DP);
                        int margin = dp2px(THUMB_MARGIN_DP);
                        params.setMargins(margin, margin, margin, margin);
                        nodeThumb.setLayoutParams(params);

                        nodeThumb.setImageURI(Uri.fromFile(file));
                    } else {
                        nodeThumb.setActualImageResource(MimeTypeList.typeForName(nodeOffline.getName()).getIconResourceId());
                    }
                } else {
                    nodeThumb.setImageResource(MimeTypeList.typeForName(nodeOffline.getName()).getIconResourceId());
                }
            } else {
                nodeThumb.setImageResource(R.drawable.ic_folder_list);
            }

            if (nodeOffline.isFolder() && !isOnline(requireContext())) {
                optionShare.setVisibility(View.GONE);
                contentView.findViewById(R.id.separator_share).setVisibility(View.GONE);
            }
        }

        super.onViewCreated(view, savedInstanceState);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.option_delete_offline_layout) {
            if (requireActivity() instanceof ManagerActivity) {
                ((ManagerActivity) requireActivity())
                        .showConfirmationRemoveFromOffline(nodeOffline,
                                this::setStateBottomSheetBehaviorHidden);
            }
        } else if (id == R.id.option_open_with_layout) {
            OfflineUtils.openWithOffline(requireContext(), Long.valueOf(nodeOffline.getHandle()));
        } else if (id == R.id.option_share_layout) {
            OfflineUtils.shareOfflineNode(requireContext(), Long.valueOf(nodeOffline.getHandle()));
        } else if (id == R.id.option_download_layout) {
            ((ManagerActivity) requireActivity()).saveOfflineNodesToDevice(
                    Collections.singletonList(nodeOffline));
        } else if (id == R.id.option_properties_layout) {
            ((ManagerActivity) requireActivity()).showOfflineFileInfo(nodeOffline);
        }

        setStateBottomSheetBehaviorHidden();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String handle = nodeOffline.getHandle();
        outState.putString(HANDLE, handle);
    }
}
