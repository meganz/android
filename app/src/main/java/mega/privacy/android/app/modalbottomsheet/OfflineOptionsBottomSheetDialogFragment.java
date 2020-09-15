package mega.privacy.android.app.modalbottomsheet;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.content.FileProvider;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.io.File;

import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.TimeUtils.formatLongDateTime;
import static mega.privacy.android.app.utils.Util.*;

public class OfflineOptionsBottomSheetDialogFragment extends BaseBottomSheetDialogFragment implements View.OnClickListener {

    private MegaOffline nodeOffline = null;

    private File file;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            String handle = savedInstanceState.getString(HANDLE);
            nodeOffline = dbH.findByHandle(handle);
        } else if (context instanceof ManagerActivityLollipop) {
            nodeOffline = ((ManagerActivityLollipop) context).getSelectedOfflineNode();
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        contentView = View.inflate(getContext(), R.layout.bottom_sheet_offline_item, null);
        mainLinearLayout = contentView.findViewById(R.id.offline_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);

        contentView.findViewById(R.id.option_download_layout).setOnClickListener(this);
        contentView.findViewById(R.id.option_properties_layout).setOnClickListener(this);
        TextView optionInfoText = contentView.findViewById(R.id.option_properties_text);

        SimpleDraweeView nodeThumb = contentView.findViewById(R.id.offline_thumbnail);
        TextView nodeName = contentView.findViewById(R.id.offline_name_text);
        TextView nodeInfo = contentView.findViewById(R.id.offline_info_text);
        LinearLayout optionOpenWith = contentView.findViewById(R.id.option_open_with_layout);
        LinearLayout optionShare = contentView.findViewById(R.id.option_share_layout);

        contentView.findViewById(R.id.option_delete_offline_layout).setOnClickListener(this);
        contentView.findViewById(R.id.available_offline_switch).setOnClickListener(this);
        optionOpenWith.setOnClickListener(this);
        optionShare.setOnClickListener(this);

        View separatorOpen = contentView.findViewById(R.id.separator_open);

        nodeName.setMaxWidth(scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(scaleWidthPx(200, outMetrics));

        if (nodeOffline != null) {
            optionInfoText.setText(nodeOffline.isFolder() ? R.string.general_folder_info
                    : R.string.general_file_info);

            if (MimeTypeList.typeForName(nodeOffline.getName()).isVideoReproducible() || MimeTypeList.typeForName(nodeOffline.getName()).isVideo() || MimeTypeList.typeForName(nodeOffline.getName()).isAudio()
                    || MimeTypeList.typeForName(nodeOffline.getName()).isImage() || MimeTypeList.typeForName(nodeOffline.getName()).isPdf()) {
                optionOpenWith.setVisibility(View.VISIBLE);
                separatorOpen.setVisibility(View.VISIBLE);
            } else {
                optionOpenWith.setVisibility(View.GONE);
                separatorOpen.setVisibility(View.GONE);
            }

            nodeName.setText(nodeOffline.getName());

            logDebug("Set node info");
            file = getOfflineFile(context, nodeOffline);
            if (!isFileAvailable(file)) return;

            int folders = 0;
            int files = 0;
            if (file.isDirectory()) {
                File[] fList = file.listFiles();
                if (fList != null) {
                    for (File f : fList) {
                        if (f.isDirectory()) {
                            folders++;
                        } else {
                            files++;
                        }
                    }

                    String info = "";
                    if (folders > 0) {
                        info = folders + " " + context.getResources().getQuantityString(R.plurals.general_num_folders, folders);
                        if (files > 0) {
                            info = info + ", " + files + " " + context.getResources().getQuantityString(R.plurals.general_num_files, folders);
                        }
                    } else {
                        info = files + " " + context.getResources().getQuantityString(R.plurals.general_num_files, files);
                    }

                    nodeInfo.setText(info);
                } else {
                    nodeInfo.setText(" ");
                }
            } else {
                long nodeSize = file.length();
                nodeInfo.setText(String.format("%s . %s", getSizeString(nodeSize), formatLongDateTime(file.lastModified() / 1000)));
            }

            if (file.isFile()) {
                if (MimeTypeList.typeForName(nodeOffline.getName()).isImage()) {
                    if (file.exists()) {
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

            if (nodeOffline.isFolder()) {
                optionShare.setVisibility(View.GONE);
                contentView.findViewById(R.id.separator_share).setVisibility(View.GONE);
            }
        }

        dialog.setContentView(contentView);
        setBottomSheetBehavior(HEIGHT_HEADER_LARGE, false);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.option_delete_offline_layout:
            case R.id.available_offline_switch:
                ((SwitchMaterial) contentView.findViewById(R.id.available_offline_switch))
                        .setChecked(true);
                if (context instanceof ManagerActivityLollipop) {
                    ((ManagerActivityLollipop) context)
                            .showConfirmationRemoveFromOffline(nodeOffline,
                                    this::setStateBottomSheetBehaviorHidden);
                }
                return;
            case R.id.option_open_with_layout:
                openWith();
                break;
            case R.id.option_share_layout:
                shareOfflineNode(context, nodeOffline);
                break;
            case R.id.option_download_layout:
                ((ManagerActivityLollipop) context).saveOfflineNodeToDevice(nodeOffline);
                break;
            case R.id.option_properties_layout:
                ((ManagerActivityLollipop) requireActivity()).showOfflineFileInfo(nodeOffline);
                break;
            default:
                break;
        }

        setStateBottomSheetBehaviorHidden();
    }

    private void openWith() {
        String type = MimeTypeList.typeForName(nodeOffline.getName()).getType();
        Intent mediaIntent = new Intent(Intent.ACTION_VIEW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaIntent.setDataAndType(FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, file), type);
        } else {
            mediaIntent.setDataAndType(Uri.fromFile(file), type);
        }
        mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (isIntentAvailable(context, mediaIntent)) {
            startActivity(mediaIntent);
        } else {
            Toast.makeText(context, getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        String handle = nodeOffline.getHandle();
        outState.putString(HANDLE, handle);
    }
}
