package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.content.FileProvider;
import android.support.v4.print.PrintHelper;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import nz.mega.sdk.MegaApiAndroid;

import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.MegaApiUtils.*;
import static mega.privacy.android.app.utils.OfflineUtils.*;
import static mega.privacy.android.app.utils.ThumbnailUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class OfflineOptionsBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaOffline nodeOffline = null;
    NodeController nC;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    LinearLayout mainLinearLayout;
    ImageView nodeThumb;
    TextView nodeName;
    TextView nodeInfo;
    LinearLayout optionDeleteOffline;
    private LinearLayout optionOpenWith;
    LinearLayout optionPrint;
    LinearLayout copyClip;
    LinearLayout saveFilesystem;

    DisplayMetrics outMetrics;
    private int heightDisplay;

    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    File file;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());

        if(savedInstanceState!=null) {
            logDebug("Bundle is NOT NULL");
            String handle = savedInstanceState.getString("handle");
            logDebug("Handle of the node offline: " + handle);
            if(handle.equals("0")){
                //recovery key will have handle as 0 and have to be handled specifically
                nodeOffline = new MegaOffline("0", getExternalStoragePath(RK_FILE), "MEGARecoveryKey.txt", 0, "0", 0, "0");
            }else{
                nodeOffline = dbH.findByHandle(handle);
            }
        }
        else{
            logWarning("Bundle NULL");
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

        heightDisplay = outMetrics.heightPixels;

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_offline_item, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.offline_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        nodeThumb = (ImageView) contentView.findViewById(R.id.offline_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.offline_name_text);
        nodeInfo  = (TextView) contentView.findViewById(R.id.offline_info_text);
        optionDeleteOffline = (LinearLayout) contentView.findViewById(R.id.option_delete_offline_layout);
        optionOpenWith = (LinearLayout) contentView.findViewById(R.id.option_open_with_layout);

        optionPrint = (LinearLayout) contentView.findViewById(R.id.option_print_offline_layout);
        copyClip = (LinearLayout) contentView.findViewById(R.id.option_copy_offline_layout);
        saveFilesystem = (LinearLayout) contentView.findViewById(R.id.option_save_offline_layout);

        optionPrint.setOnClickListener(this);
        copyClip.setOnClickListener(this);
        saveFilesystem.setOnClickListener(this);
        optionDeleteOffline.setOnClickListener(this);
        optionOpenWith.setOnClickListener(this);

        LinearLayout separatorRK = (LinearLayout) contentView.findViewById(R.id.separator_rk);
        LinearLayout separatorOpen = (LinearLayout) contentView.findViewById(R.id.separator_open);

        nodeName.setMaxWidth(scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(scaleWidthPx(200, outMetrics));

        if(nodeOffline!=null){

            if (MimeTypeList.typeForName(nodeOffline.getName()).isVideoReproducible() || MimeTypeList.typeForName(nodeOffline.getName()).isVideo() || MimeTypeList.typeForName(nodeOffline.getName()).isAudio()
                    || MimeTypeList.typeForName(nodeOffline.getName()).isImage() || MimeTypeList.typeForName(nodeOffline.getName()).isPdf()) {
                optionOpenWith.setVisibility(View.VISIBLE);
                separatorOpen.setVisibility(View.VISIBLE);
            }
            else {
                optionOpenWith.setVisibility(View.GONE);
                separatorOpen.setVisibility(View.GONE);
            }

            nodeName.setText(nodeOffline.getName());

            //Check if the node is the Master Key file
            if(nodeOffline.getHandle().equals("0")){
                file= buildExternalStorageFile(RK_FILE);
                if(isFileAvailable(file)){
                    if(file.exists()){
                        long nodeSize = file.length();
                        nodeInfo.setText(getSizeString(nodeSize));
                    }
                    nodeThumb.setImageResource(MimeTypeList.typeForName(nodeOffline.getName()).getIconResourceId());
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                    optionPrint.setVisibility(View.VISIBLE);
                }
                else {
                    optionPrint.setVisibility(View.GONE);
                }
                copyClip.setVisibility(View.VISIBLE);
                saveFilesystem.setVisibility(View.VISIBLE);
                separatorRK.setVisibility(View.VISIBLE);
            }
            else{

                optionPrint.setVisibility(View.GONE);
                copyClip.setVisibility(View.GONE);
                saveFilesystem.setVisibility(View.GONE);
                separatorRK.setVisibility(View.GONE);

                logDebug("Set node info");
                file = getOfflineFile(context, nodeOffline);
                if (file == null) return;

                int folders=0;
                int files=0;
                if (file.isDirectory()){

                    File[] fList = file.listFiles();
                    if(fList != null){
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
                    }else{
                        nodeInfo.setText(" ");
                    }
                }
                else{
                    long nodeSize = file.length();
                    nodeInfo.setText(getSizeString(nodeSize));
                }

                logDebug("Set node thumb");
                if (file.isFile()){
                    logDebug("Search thumb");
                    if (MimeTypeList.typeForName(nodeOffline.getName()).isImage()){
                        Bitmap thumb = null;
                        if (file.exists()){
                            thumb = getThumbnailFromCache(Long.parseLong(nodeOffline.getHandle()));
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
//        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//
//        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            mBehavior.setPeekHeight((heightDisplay / 4) * 2);
//        }
//        else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
//            mBehavior.setPeekHeight(BottomSheetBehavior.PEEK_HEIGHT_AUTO);
//        }
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

    }


    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.option_delete_offline_layout:{
                logDebug("Delete Offline");
                if(context instanceof ManagerActivityLollipop){
                    ((ManagerActivityLollipop) context).showConfirmationRemoveFromOffline();
                }
                break;
            }
            case R.id.option_open_with_layout:{
                logDebug("Open with");
                openWith();
                break;
            }
            case R.id.option_print_offline_layout:{
                logDebug("Option print RK");
                printRK();
                break;
            }
            case R.id.option_save_offline_layout:{
                logDebug("Option save on filesystem");
                AccountController aC = new AccountController(getContext());
                aC.saveRkToFileSystem(true);
                break;
            }
            case R.id.option_copy_offline_layout:{
                logDebug("Option copy to clipboard");
                if (isOnline(context)){
                    AccountController aC = new AccountController(getContext());
                    aC.copyMK(false);
                }
                else {
                    copyFromFile();
                }
                break;
            }
        }
//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    public String readRKFromFile (){
        String line = null;
        if (nodeOffline != null && nodeOffline.getPath() != null){
            File file = new File(nodeOffline.getPath());
            StringBuilder sb = new StringBuilder();

            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                line = br.readLine();
            }
            catch (IOException e) {
                logError("IOException", e);
            }
            return line;
        }


        return null;
    }

    public void copyFromFile () {
        String key = readRKFromFile();
        if (key != null) {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", key);
            clipboard.setPrimaryClip(clip);
            if (clipboard.getPrimaryClip() != null) {
                showAlert(((ManagerActivityLollipop) context), context.getString(R.string.copy_MK_confirmation), null);
            }
            else {
                showAlert(((ManagerActivityLollipop) context), context.getString(R.string.general_text_error), null);
            }
        }
        else {
            showAlert(((ManagerActivityLollipop) context), context.getString(R.string.general_text_error), null);
        }
    }

    public void printRK(){
        Bitmap rKBitmap = null;
        if (isOnline(context)) {
            AccountController aC = new AccountController(getContext());
            rKBitmap = aC.createRkBitmap();
        }
        else {
            rKBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);
            String key =  readRKFromFile();
            if (key != null){
                Canvas canvas = new Canvas(rKBitmap);
                Paint paint = new Paint();

                paint.setTextSize(40);
                paint.setColor(Color.BLACK);
                paint.setStyle(Paint.Style.FILL);
                float height = paint.measureText("yY");
                float width = paint.measureText(key);
                float x = (rKBitmap.getWidth()-width)/2;
                canvas.drawText(key, x, height+15f, paint);
            }
            else {
                showAlert(((ManagerActivityLollipop) context), context.getString(R.string.general_text_error), null);
            }

        }
        if (rKBitmap != null){
            PrintHelper printHelper = new PrintHelper(getActivity());
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            printHelper.printBitmap("rKPrint", rKBitmap);
        }
    }

    public void openWith () {
        logDebug("openWith");
        String type = MimeTypeList.typeForName(nodeOffline.getName()).getType();

        Intent mediaIntent = new Intent(Intent.ACTION_VIEW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", file), type);
        }
        else{
            mediaIntent.setDataAndType(Uri.fromFile(file), type);
        }
        mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (isIntentAvailable(context, mediaIntent)){
            startActivity(mediaIntent);
        }
        else{
            Toast.makeText(context, getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
        }
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
        logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        String handle = nodeOffline.getHandle();
        logDebug("Handle of the node offline: " + handle);
        outState.putString("handle", handle);
    }
}
