package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.MimeTypeMime;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.FileInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.MyAccountInfo;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

public class OfflineOptionsBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaOffline nodeOffline = null;
    NodeController nC;

    private BottomSheetBehavior mBehavior;

    LinearLayout mainLinearLayout;
    ImageView nodeThumb;
    TextView nodeName;
    TextView nodeInfo;
    LinearLayout optionDeleteOffline;

    DisplayMetrics outMetrics;

    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());

        if(savedInstanceState!=null) {
            log("Bundle is NOT NULL");
            String handle = savedInstanceState.getString("handle");
            log("Handle of the node offline: "+handle);
            nodeOffline = dbH.findByHandle(handle);
        }
        else{
            log("Bundle NULL");
            if(context instanceof ManagerActivityLollipop){
                nodeOffline = ((ManagerActivityLollipop) context).getSelectedOfflineNode();
            }
        }

        nC = new NodeController(context);
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        super.setupDialog(dialog, style);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_offline_item, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.offline_bottom_sheet);

        nodeThumb = (ImageView) contentView.findViewById(R.id.offline_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.offline_name_text);
        nodeInfo  = (TextView) contentView.findViewById(R.id.offline_info_text);
        optionDeleteOffline = (LinearLayout) contentView.findViewById(R.id.option_delete_offline_layout);

        optionDeleteOffline.setOnClickListener(this);

        nodeName.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(Util.scaleWidthPx(200, outMetrics));

        if(nodeOffline!=null){
            nodeName.setText(nodeOffline.getName());

            //Check if the node is the Master Key file
            if(nodeOffline.getHandle().equals("0")){
                String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
                File file= new File(path);
                if(file.exists()){
                    long nodeSize;
                    if(file.exists()){
                        nodeSize = file.length();
                        nodeInfo.setText(Util.getSizeString(nodeSize));
                    }
                    nodeThumb.setImageResource(MimeTypeList.typeForName(nodeOffline.getName()).getIconResourceId());
                }
            }
            else{

                log("Set node info");
                String path=null;

                if(nodeOffline.getOrigin()==MegaOffline.INCOMING){
                    path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/" + nodeOffline.getHandleIncoming() + "/";
                }
                else if(nodeOffline.getOrigin()==MegaOffline.INBOX){
                    path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR + "/in";
                }
                else{
                    path= Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR;
                }

                File currentFile = null;
                if (Environment.getExternalStorageDirectory() != null){
                    String finalPath = path + nodeOffline.getPath()+nodeOffline.getName();
                    currentFile = new File(finalPath);
                    log("Path to find file: "+finalPath);
                }
                else{
                    currentFile = context.getFilesDir();
                }

                int folders=0;
                int files=0;
                if (currentFile.isDirectory()){

                    File[] fList = currentFile.listFiles();
                    for (File f : fList){

                        if (f.isDirectory()){
                            folders++;
                        }
                        else{
                            files++;
                        }
                    }

                    String info = "";
                    if (folders > 0){
                        info = folders +  " " + context.getResources().getQuantityString(R.plurals.general_num_folders, folders);
                        if (files > 0){
                            info = info + ", " + files + " " + context.getResources().getQuantityString(R.plurals.general_num_files, folders);
                        }
                    }
                    else {
                        info = files +  " " + context.getResources().getQuantityString(R.plurals.general_num_files, files);
                    }

                    nodeInfo.setText(info);
                }
                else{
                    long nodeSize = currentFile.length();
                    nodeInfo.setText(Util.getSizeString(nodeSize));
                }

                log("Set node thumb");
                if (currentFile.isFile()){
                    log("...........................Busco Thumb");
                    if (MimeTypeList.typeForName(nodeOffline.getName()).isImage()){
                        Bitmap thumb = null;
                        if (currentFile.exists()){
                            thumb = ThumbnailUtils.getThumbnailFromCache(Long.parseLong(nodeOffline.getHandle()));
                            if (thumb != null){
                                nodeThumb.setImageBitmap(thumb);
                            }
                            else{
                                nodeThumb.setImageResource(MimeTypeList.typeForName(nodeOffline.getName()).getIconResourceId());
                            }
                        }
                        else{
                            nodeThumb.setImageResource(MimeTypeList.typeForName(nodeOffline.getName()).getIconResourceId());
                        }
                    }
                    else{
                        nodeThumb.setImageResource(MimeTypeList.typeForName(nodeOffline.getName()).getIconResourceId());
                    }
                }
                else{
                    nodeThumb.setImageResource(R.drawable.ic_folder_list);
                }

                optionDeleteOffline.setVisibility(View.VISIBLE);
            }
        }

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }


    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.option_delete_offline_layout:{
                log("Delete Offline");
                if(context instanceof ManagerActivityLollipop){
                    ((ManagerActivityLollipop) context).showConfirmationRemoveFromOffline();
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
        String handle = nodeOffline.getHandle();
        log("Handle of the node offline: "+handle);
        outState.putString("handle", handle);
    }

    private static void log(String log) {
        Util.log("OfflineOptionsBottomSheetDialogFragment", log);
    }
}
