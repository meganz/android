package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
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

import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContact;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactFileListActivityLollipop;
import mega.privacy.android.app.lollipop.FilePropertiesActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;

public class ContactFileListBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaNode node = null;
    NodeController nC;

    private BottomSheetBehavior mBehavior;

    LinearLayout mainLinearLayout;
    ImageView nodeThumb;
    TextView nodeName;
    TextView nodeInfo;
    LinearLayout optionDownload;
    LinearLayout optionInfo;
    TextView optionInfoText;
    ImageView optionInfoImage;
    LinearLayout optionLeave;
    LinearLayout optionCopy;
    LinearLayout optionMove;
    LinearLayout optionRename;
    LinearLayout optionRubbish;

    DisplayMetrics outMetrics;

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
            long handle = savedInstanceState.getLong("handle", -1);
            log("Handle of the node: "+handle);
            node = megaApi.getNodeByHandle(handle);
        }
        else{
            log("Bundle NULL");
            if(context instanceof ContactFileListActivityLollipop){
                node = ((ContactFileListActivityLollipop) context).getSelectedNode();
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

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_contact_file_list, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.contact_file_list_bottom_sheet);

        nodeThumb = (ImageView) contentView.findViewById(R.id.contact_file_list_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.contact_file_list_name_text);
        nodeInfo  = (TextView) contentView.findViewById(R.id.contact_file_list_info_text);
        optionDownload = (LinearLayout) contentView.findViewById(R.id.option_download_layout);
        optionInfo = (LinearLayout) contentView.findViewById(R.id.option_properties_layout);
        optionInfoText = (TextView) contentView.findViewById(R.id.option_properties_text);
        optionInfoImage = (ImageView) contentView.findViewById(R.id.option_properties_image);
        optionLeave = (LinearLayout) contentView.findViewById(R.id.option_leave_layout);
        optionCopy = (LinearLayout) contentView.findViewById(R.id.option_copy_layout);
        optionMove = (LinearLayout) contentView.findViewById(R.id.option_move_layout);
        optionRename = (LinearLayout) contentView.findViewById(R.id.option_rename_layout);
        optionRubbish = (LinearLayout) contentView.findViewById(R.id.option_rubbish_bin_layout);

        optionDownload.setOnClickListener(this);
        optionInfo.setOnClickListener(this);
        optionCopy.setOnClickListener(this);
        optionMove.setOnClickListener(this);
        optionRename.setOnClickListener(this);
        optionLeave.setOnClickListener(this);
        optionRubbish.setOnClickListener(this);

        nodeName.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(Util.scaleWidthPx(200, outMetrics));

        if(node!=null) {
            log("node is NOT null");

            nodeName.setText(node.getName());

            boolean firstLevel = ((ContactFileListActivityLollipop) context).isEmptyParentHandleStack();
            log("First LEVEL is: "+firstLevel);
            long parentHandle = -1;
            parentHandle = ((ContactFileListActivityLollipop) context).getParentHandle();
            log("Parent handle is: "+parentHandle);

            if (node.isFolder()) {

                nodeThumb.setImageResource(R.drawable.ic_folder_shared_list);
                optionInfoText.setText(R.string.general_folder_info);

                if(firstLevel||parentHandle == -1){
                    log("Fist level!!");
                    optionLeave.setVisibility(View.VISIBLE);
                    ArrayList<MegaShare> sharesIncoming = megaApi.getInSharesList();
                    for(int j=0; j<sharesIncoming.size();j++){
                        MegaShare mS = sharesIncoming.get(j);
                        if(mS.getNodeHandle()==node.getHandle()){
                            MegaUser user= megaApi.getContact(mS.getUser());
                            if(user!=null){
                                MegaContact contactDB = dbH.findContactByHandle(String.valueOf(user.getHandle()));
                                if(contactDB!=null){
                                    if(!contactDB.getName().equals("")){
                                        nodeInfo.setText(contactDB.getName()+" "+contactDB.getLastName());
                                    }
                                    else{
                                        nodeInfo.setText(user.getEmail());
                                    }
                                }
                                else{
                                    log("The contactDB is null: ");
                                    nodeInfo.setText(user.getEmail());
                                }
                            }
                            else{
                                nodeInfo.setText(mS.getUser());
                            }
                        }
                    }
                }
                else{
                    nodeInfo.setText(MegaApiUtils.getInfoFolder(node, context, megaApi));
                    optionLeave.setVisibility(View.GONE);
                }

            } else {
                optionInfoText.setText(R.string.general_file_info);
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
                } else {
                    nodeThumb.setImageResource(MimeTypeList.typeForName(node.getName()).getIconResourceId());
                }
                optionLeave.setVisibility(View.GONE);
            }

            int accessLevel = megaApi.getAccess(node);

            switch (accessLevel) {
                case MegaShare.ACCESS_FULL: {
                    optionMove.setVisibility(View.VISIBLE);
                    optionRename.setVisibility(View.VISIBLE);

                    if(firstLevel||parentHandle == -1){
                        optionRubbish.setVisibility(View.GONE);
                    }
                    else{
                        optionRubbish.setVisibility(View.VISIBLE);
                    }

                    break;
                }
                case MegaShare.ACCESS_READ: {
                    log("read");
                    optionRename.setVisibility(View.GONE);
                    optionRubbish.setVisibility(View.GONE);
                    optionMove.setVisibility(View.GONE);
                    break;
                }
                case MegaShare.ACCESS_READWRITE: {
                    log("readwrite");
                    optionMove.setVisibility(View.GONE);
                    optionRename.setVisibility(View.GONE);
                    optionRubbish.setVisibility(View.GONE);
                    break;
                }
            }

            dialog.setContentView(contentView);
            mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
            mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
        else{
            log("Node NULL");
        }
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.option_download_layout:{
                log("Download option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ContactFileListActivityLollipop) context).onFileClick(handleList);
                break;
            }
            case R.id.option_properties_layout:{
                log("Properties option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                Intent i = new Intent(context, FilePropertiesActivityLollipop.class);
                i.putExtra("handle", node.getHandle());
                i.putExtra("from", FilePropertiesActivityLollipop.FROM_INCOMING_SHARES);
                if (node.isFolder()) {
                    if (megaApi.isShared(node)){
                        i.putExtra("imageId", R.drawable.folder_shared_mime);
                    }
                    else{
                        i.putExtra("imageId", R.drawable.folder_mime);
                    }
                }
                else {
                    i.putExtra("imageId", MimeTypeMime.typeForName(node.getName()).getIconResourceId());
                }
                i.putExtra("name", node.getName());
                context.startActivity(i);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_leave_layout:{
                log("Share with");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ContactFileListActivityLollipop) context).showConfirmationLeaveIncomingShare(node);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_rename_layout:{
                log("Rename option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ((ContactFileListActivityLollipop) context).showRenameDialog(node, node.getName());
                break;
            }
            case R.id.option_move_layout:{
                log("Move option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ContactFileListActivityLollipop) context).showMoveLollipop(handleList);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_copy_layout:{
                log("Copy option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ContactFileListActivityLollipop) context).showCopyLollipop(handleList);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.option_rubbish_bin_layout:{
                log("Delete/Move to rubbish option");
                if(node==null){
                    log("The selected node is NULL");
                    return;
                }
                ArrayList<Long> handleList = new ArrayList<Long>();
                handleList.add(node.getHandle());
                ((ContactFileListActivityLollipop) context).askConfirmationMoveToRubbish(handleList);
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
        Util.log("ContactFileListBottomSheetDialogFragment", log);
    }
}
