package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.VersionsFileActivity;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.LogUtil;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

public class VersionBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private Context context;
    private MegaNode node = null;
    private NodeController nC;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    private LinearLayout mainLinearLayout;
    private ImageView nodeThumb;
    private TextView nodeName;
    private TextView nodeInfo;
    private RelativeLayout nodeIconLayout;
    private ImageView nodeIcon;
    private LinearLayout optionDownload;
    private LinearLayout optionRevert;
    private LinearLayout optionDelete;

    private DisplayMetrics outMetrics;
    private int heightDisplay;

    private Bitmap thumb = null;

    private MegaApiAndroid megaApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LogUtil.logDebug("onCreate");
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if(savedInstanceState!=null) {
            LogUtil.logDebug("Bundle is NOT NULL");
            long handle = savedInstanceState.getLong("handle", -1);
            LogUtil.logDebug("Handle of the node: " + handle);
            node = megaApi.getNodeByHandle(handle);
        }
        else{
            LogUtil.logWarning("Bundle NULL");
            if(context instanceof VersionsFileActivity){
                node = ((VersionsFileActivity) context).getSelectedNode();
            }
        }

        nC = new NodeController(context);
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        super.setupDialog(dialog, style);
        LogUtil.logDebug("setupDialog");
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_versions_file, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.versions_file_bottom_sheet);
        items_layout  =(LinearLayout) contentView.findViewById(R.id.item_list_bottom_sheet_contact_file);

        nodeThumb = (ImageView) contentView.findViewById(R.id.versions_file_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.versions_file_name_text);
        nodeInfo  = (TextView) contentView.findViewById(R.id.versions_file_info_text);
        nodeIconLayout = (RelativeLayout) contentView.findViewById(R.id.versions_file_relative_layout_icon);
        nodeIcon = (ImageView) contentView.findViewById(R.id.versions_file_icon);
        optionDownload = (LinearLayout) contentView.findViewById(R.id.option_download_layout);
        optionRevert = (LinearLayout) contentView.findViewById(R.id.option_revert_layout);
        optionDelete = (LinearLayout) contentView.findViewById(R.id.option_delete_layout);

        optionDownload.setOnClickListener(this);
        optionRevert.setOnClickListener(this);
        optionDelete.setOnClickListener(this);

        LinearLayout separatorRevert = (LinearLayout) contentView.findViewById(R.id.separator_revert);

        nodeName.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(Util.scaleWidthPx(200, outMetrics));

        if(node!=null) {
            LogUtil.logDebug("Node is NOT null");

            nodeName.setText(node.getName());

            long nodeSize = node.getSize();
            String fileInfo = Util.getSizeString(nodeSize) + " . " + getNodeDate(node);
            nodeInfo.setText(fileInfo);

            if (node.hasThumbnail()) {
                LogUtil.logDebug("Node has thumbnail");
                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) nodeThumb.getLayoutParams();
                params1.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 36, context.getResources().getDisplayMetrics());
                params1.setMargins(20, 0, 12, 0);
                nodeThumb.setLayoutParams(params1);

                thumb = ThumbnailUtils.getThumbnailFromCache(node);
                if (thumb != null) {
                    nodeThumb.setImageBitmap(thumb);
                } else {
                    thumb = ThumbnailUtils.getThumbnailFromFolder(node, context);
                    if (thumb != null) {
                        nodeThumb.setImageBitmap(thumb);
                    } else {
                        nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                    }
                }
            } else {
                nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
            }
            optionDownload.setVisibility(View.VISIBLE);
            optionDelete.setVisibility(View.VISIBLE);

            if(((VersionsFileActivity) context).getSelectedPosition()==0){
                optionRevert.setVisibility(View.GONE);
                separatorRevert.setVisibility(View.GONE);
            }
            else{
                optionRevert.setVisibility(View.VISIBLE);
                separatorRevert.setVisibility(View.VISIBLE);
            }

            dialog.setContentView(contentView);
            mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());

            mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        }
        else{
            LogUtil.logWarning("Node NULL");
        }
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.option_download_layout:{
                LogUtil.logDebug("Download option");
                if(node==null){
                    LogUtil.logWarning("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                nC.prepareForDownload(handleList, false);
                break;
            }
            case R.id.option_revert_layout:{
                LogUtil.logDebug("Revert option");
                if(node==null){
                    LogUtil.logWarning("The selected node is NULL");
                    return;
                }
                ((VersionsFileActivity) context).revertVersion();
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_delete_layout:{
                LogUtil.logDebug("Delete option");
                if(node==null){
                    LogUtil.logWarning("The selected node is NULL");
                    return;
                }
                ((VersionsFileActivity) context).showConfirmationRemoveVersion();
                break;
            }
        }

//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public String getNodeDate(MegaNode node){

        Calendar calendar = Util.calculateDateFromTimestamp(node.getModificationTime());
        String format3 = new SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault()).format(calendar.getTime());
        return format3;
    }


    @Override
    public void onAttach(Activity activity) {
        LogUtil.logDebug("onAttach");
        super.onAttach(activity);
        this.context = activity;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        LogUtil.logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        long handle = node.getHandle();
        LogUtil.logDebug("Handle of the node: " + handle);
        outState.putLong("handle", handle);
    }
}
