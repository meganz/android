package mega.privacy.android.app.modalbottomsheet;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;

/**
 * Created by mega on 22/06/18.
 */

public class UtilsModalBottomSheet {

    public static int getPeekHeight (LinearLayout items_layout, int heightDisplay, Context context, int heightHeader) {
        int numOptions = items_layout.getChildCount();
        int numOptionsVisibles = 0;
        int heightScreen = (heightDisplay / 2);
        int heightChild = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, context.getResources().getDisplayMetrics());
        int peekHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightHeader, context.getResources().getDisplayMetrics());

        for (int i=0; i<numOptions; i++){
            if (items_layout.getChildAt(i).getVisibility() == View.VISIBLE) {
                numOptionsVisibles++;
            }
        }

        if ((numOptionsVisibles <= 3 && heightHeader == 81) || (numOptionsVisibles <= 4 && heightHeader == 48)){
            peekHeight += (heightChild * numOptions);
        }
        else {
            for (int i = 0; i < numOptions; i++) {
                if (items_layout.getChildAt(i).getVisibility() == View.VISIBLE && peekHeight < heightScreen) {
                    log("Child i: " + i + " is visible; peekHeight: " + peekHeight + " heightScreen: " + heightScreen + " heightChild: " + heightChild);
                    peekHeight += heightChild;
                    if (peekHeight >= heightScreen) {
                        if (items_layout.getChildAt(i + 2) != null) {
                            boolean visible = false;
                            for (int j = i + 2; j < numOptions; j++) {
                                if (items_layout.getChildAt(j).getVisibility() == View.VISIBLE) {
                                    visible = true;
                                    break;
                                }
                            }
                            if (visible) {
                                peekHeight += (heightChild / 2);
                                break;
                            } else {
                                peekHeight += heightChild;
                                break;
                            }
                        } else if (items_layout.getChildAt(i + 1) != null) {
                            if (items_layout.getChildAt(i + 1).getVisibility() == View.VISIBLE) {
                                peekHeight += (heightChild / 2);
                                break;
                            } else {
                                peekHeight += heightChild;
                                break;
                            }
                        } else {
                            peekHeight += heightChild;
                            break;
                        }
                    }
                }
            }
        }
        return peekHeight;
    }

    public static void openWith (MegaApiAndroid megaApi, Context context, MegaNode node) {
        log("openWith");

        boolean isError = false;

        String mimeType = MimeTypeList.typeForName(node.getName()).getType();
        log("FILENAME: " + node.getName());

        Intent mediaIntent = new Intent(Intent.ACTION_VIEW);
        mediaIntent.putExtra("HANDLE", node.getHandle());
        mediaIntent.putExtra("FILENAME", node.getName());

        String downloadLocationDefaultPath = getDownloadLocation(context);
        boolean isOnMegaDownloads = false;
        String localPath = getLocalFile(context, node.getName(), node.getSize(), downloadLocationDefaultPath);
        File f = new File(downloadLocationDefaultPath, node.getName());
        if(f.exists() && (f.length() == node.getSize())){
            isOnMegaDownloads = true;
        }
        if (localPath != null && (isOnMegaDownloads || (megaApi.getFingerprint(node) != null && megaApi.getFingerprint(node).equals(megaApi.getFingerprint(localPath))))) {
            File mediaFile = new File(localPath);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                mediaIntent.setDataAndType(FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile), MimeTypeList.typeForName(node.getName()).getType());
            }
            else{
                mediaIntent.setDataAndType(Uri.fromFile(mediaFile), MimeTypeList.typeForName(node.getName()).getType());
            }
            mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        else {
            if (megaApi.httpServerIsRunning() == 0) {
                megaApi.httpServerStart();
            }

            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            activityManager.getMemoryInfo(mi);

            if(mi.totalMem> Constants.BUFFER_COMP){
                log("Total mem: "+mi.totalMem+" allocate 32 MB");
                megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB);
            }
            else{
                log("Total mem: "+mi.totalMem+" allocate 16 MB");
                megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB);
            }

            String url = megaApi.httpServerGetLocalLink(node);

            if(url==null){
                isError=true;
            }
            else{
                mediaIntent.setDataAndType(Uri.parse(url), mimeType);
            }
        }

        if(isError){
            Toast.makeText(context, context.getResources().getString(R.string.error_open_file_with), Toast.LENGTH_LONG).show();
        }
        else{
            if (MegaApiUtils.isIntentAvailable(context, mediaIntent)){
                context.startActivity(mediaIntent);
            }
            else{
                Toast.makeText(context, context.getResources().getString(R.string.intent_not_available), Toast.LENGTH_LONG).show();
            }
        }
    }

    private static void log(String log) {
        Util.log("UtilsModalBottomSheet", log);
    }
}
