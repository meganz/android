package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileContactListActivityLollipop;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

public class NodeAttachmentBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaNode node = null;
    AndroidMegaChatMessage message = null;
    NodeController nC;

    private BottomSheetBehavior mBehavior;

    LinearLayout mainLinearLayout;
    CoordinatorLayout coordinatorLayout;

    ImageView nodeThumb;
    TextView nodeName;
    TextView nodeInfo;
    RelativeLayout nodeIconLayout;
    ImageView nodeIcon;
    LinearLayout optionView;
    LinearLayout optionDownload;
    LinearLayout optionImport;
    LinearLayout optionSaveOffline;
    LinearLayout optionRevoke;

    DisplayMetrics outMetrics;

    static ManagerActivityLollipop.DrawerItem drawerItem = null;
    Bitmap thumb = null;

    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        log("onCreate");
        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if(savedInstanceState!=null) {
            log("Bundle is NOT NULL");
//            long handle = savedInstanceState.getLong("handle", -1);
//            log("Handle of the node: "+handle);
//            node = megaApi.getNodeByHandle(handle);
//            if(context instanceof ChatActivityLollipop){
//                drawerItem = ((ManagerActivityLollipop) context).getDrawerItem();
//            }
        }
        else{
            log("Bundle NULL");
            if(context instanceof ChatActivityLollipop){
                message = ((ChatActivityLollipop) context).getSelectedMessage();
            }
        }

        nC = new NodeController(context);

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        super.setupDialog(dialog, style);
        log("setupDialog");
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_node_attachment_item, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.node_attachment_bottom_sheet);

        nodeThumb = (ImageView) contentView.findViewById(R.id.node_attachment_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.node_attachment_name_text);
        nodeInfo  = (TextView) contentView.findViewById(R.id.node_attachment_info_text);
        nodeIconLayout = (RelativeLayout) contentView.findViewById(R.id.node_attachment_relative_layout_icon);
        nodeIcon = (ImageView) contentView.findViewById(R.id.node_attachment_icon);
        optionDownload = (LinearLayout) contentView.findViewById(R.id.option_download_layout);
        optionView = (LinearLayout) contentView.findViewById(R.id.option_view_layout);
        optionRevoke = (LinearLayout) contentView.findViewById(R.id.option_revoke_layout);
        optionSaveOffline = (LinearLayout) contentView.findViewById(R.id.option_save_offline_layout);
        optionImport = (LinearLayout) contentView.findViewById(R.id.option_import_layout);


        optionDownload.setOnClickListener(this);
        optionView.setOnClickListener(this);
        optionRevoke.setOnClickListener(this);
        optionSaveOffline.setOnClickListener(this);
        optionImport.setOnClickListener(this);

        nodeIconLayout.setVisibility(View.GONE);

        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            log("onCreate: Landscape configuration");
            nodeName.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
            nodeInfo.setMaxWidth(Util.scaleWidthPx(275, outMetrics));
        }
        else{
            nodeName.setMaxWidth(Util.scaleWidthPx(210, outMetrics));
            nodeInfo.setMaxWidth(Util.scaleWidthPx(210, outMetrics));
        }

        if (message != null) {
            MegaNodeList nodeList = message.getMessage().getMegaNodeList();
            if(nodeList.size()==1){
                node = nodeList.get(0);

                if(node!=null) {
                    log("node is NOT null");
                    if (Util.isOnline(context)) {
                        nodeName.setText(node.getName());

                        long nodeSize = node.getSize();
                        nodeInfo.setText(Util.getSizeString(nodeSize));

                        if (node.hasThumbnail()) {
                            log("Node has thumbnail");
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
                        }
                        else {
                            nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                        }
                    }



                    dialog.setContentView(contentView);

                    mBehavior = BottomSheetBehavior.from((View) contentView.getParent());
                    mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }
                else{
                    log("Node NULL");
                }
            }
            else{
                log("Several nodes in the message");

            }

        }

    }

//    private int getBottomSheetMaximumHeight() {
//        // get toolbar height
//        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
//        int toolbarHeight = toolbar.getHeight();
//
//        //get status bar height
//        Rect rectangle = new Rect();
//        Window window = getActivity().getWindow();
//        window.getDecorView().getWindowVisibleDisplayFrame(rectangle);
//        int windowHeight = rectangle.bottom;
//
//        // material design recommended bottomsheet padding from actionbar
//        final int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,8, getContext().getResources().getDisplayMetrics());
//
//        // maximum height of the bottomsheet
////        return windowHeight - toolbarHeight - rectangle.top - padding;
//        return toolbarHeight + rectangle.top + padding;
//
//    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.option_download_layout:{
                log("Download option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }

                break;
            }
            case R.id.option_view_layout:{
                log("View option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }

//                context.startActivity(i);
//                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_save_offline_layout:{
                log("Save for offline option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }

                break;
            }
            case R.id.option_import_layout:{
                log("Import option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }

                break;
            }
            case R.id.option_revoke_layout:{
                log("Revoke option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }


                break;
            }
        }

//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }


    @Override
    public void onAttach(Activity activity) {
        log("onAttach");
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
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        long handle = node.getHandle();
        log("Handle of the node: "+handle);
        outState.putLong("handle", handle);
    }

    private static void log(String log) {
        Util.log("NodeAttachmentBottomSheetDialogFragment", log);
    }
}
