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
import android.os.Environment;
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
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;

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

        heightDisplay = outMetrics.heightPixels;

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_offline_item, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.offline_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        nodeThumb = (ImageView) contentView.findViewById(R.id.offline_thumbnail);
        nodeName = (TextView) contentView.findViewById(R.id.offline_name_text);
        nodeInfo  = (TextView) contentView.findViewById(R.id.offline_info_text);
        optionDeleteOffline = (LinearLayout) contentView.findViewById(R.id.option_delete_offline_layout);
        optionOpenWith = (LinearLayout) contentView.findViewById(R.id.option_open_with_layout);

        optionDeleteOffline.setOnClickListener(this);
        optionOpenWith.setOnClickListener(this);

        LinearLayout separatorRK = (LinearLayout) contentView.findViewById(R.id.separator_rk);
        LinearLayout separatorOpen = (LinearLayout) contentView.findViewById(R.id.separator_open);

        nodeName.setMaxWidth(Util.scaleWidthPx(200, outMetrics));
        nodeInfo.setMaxWidth(Util.scaleWidthPx(200, outMetrics));

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

            separatorRK.setVisibility(View.GONE);

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

            if (Environment.getExternalStorageDirectory() != null){
                String finalPath = path + nodeOffline.getPath()+nodeOffline.getName();
                file = new File(finalPath);
                log("Path to find file: "+finalPath);
            }
            else{
                file = context.getFilesDir();
            }

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
                nodeInfo.setText(Util.getSizeString(nodeSize));
            }

            log("Set node thumb");
            if (file.isFile()){
                log("...........................Busco Thumb");
                if (MimeTypeList.typeForName(nodeOffline.getName()).isImage()){
                    Bitmap thumb = null;
                    if (file.exists()){
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

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 81));
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
            case R.id.option_open_with_layout:{
                log("Open with");
                openWith();
                break;
            }
        }

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
                log("IOException: " + e.getMessage());
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
                Util.showAlert(((ManagerActivityLollipop) context), context.getString(R.string.copy_MK_confirmation), null);
            }
            else {
                Util.showAlert(((ManagerActivityLollipop) context), context.getString(R.string.general_text_error), null);
            }
        }
        else {
            Util.showAlert(((ManagerActivityLollipop) context), context.getString(R.string.general_text_error), null);
        }
    }

    public void printRK(){
        Bitmap rKBitmap = null;
        if (Util.isOnline(context)) {
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
                Util.showAlert(((ManagerActivityLollipop) context), context.getString(R.string.general_text_error), null);
            }

        }
        if (rKBitmap != null){
            PrintHelper printHelper = new PrintHelper(getActivity());
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            printHelper.printBitmap("rKPrint", rKBitmap);
        }
    }

    public void openWith () {
        log("openWith");
        String type = MimeTypeList.typeForName(nodeOffline.getName()).getType();

        Intent mediaIntent = new Intent(Intent.ACTION_VIEW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", file), type);
        }
        else{
            mediaIntent.setDataAndType(Uri.fromFile(file), type);
        }
        mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        if (MegaApiUtils.isIntentAvailable(context, mediaIntent)){
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
