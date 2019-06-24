package mega.privacy.android.app.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;

public class FileUtils {

    public static boolean isAudioOrVideo (MegaNode node) {
        if (MimeTypeList.typeForName(node.getName()).isVideoReproducible() || MimeTypeList.typeForName(node.getName()).isAudio()) return true;

        return false;
    }

    public static boolean isInternalIntent (MegaNode node) {
        if (MimeTypeList.typeForName(node.getName()).isVideoNotSupported() || MimeTypeList.typeForName(node.getName()).isAudioNotSupported()) return false;

        return true;
    }

    public static boolean isOpusFile (MegaNode node) {
        String[] s = node.getName().split("\\.");
        if (s != null && s.length > 1 && s[s.length-1].equals("opus")) return true;

        return false;
    }

    public static boolean isOnMegaDownloads (Context context, MegaNode node) {
        File f = new File(Util.getDownloadLocation(context), node.getName());

        if(f.exists() && (f.length() == node.getSize())){
            return true;
        }

        return false;
    }

    public static boolean isLocalFile (Context context, MegaNode node, MegaApiAndroid megaApi, String localPath) {
        if (localPath != null && (isOnMegaDownloads(context, node) || (megaApi.getFingerprint(node) != null && megaApi.getFingerprint(node).equals(megaApi.getFingerprint(localPath))))) return true;

        return false;
    }

    public static boolean setLocalIntentParams(Context context, MegaNode node, Intent intent, String localPath, boolean isText) {
        File mediaFile = new File(localPath);

        Uri mediaFileUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
            mediaFileUri = FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile);
        }
        else{
            mediaFileUri = Uri.fromFile(mediaFile);
        }

        if(mediaFileUri!=null){
            if (isText) {
                intent.setDataAndType(mediaFileUri, "text/plain");
            }
            else {
                intent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return true;
        }

        ((ManagerActivityLollipop)context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.general_text_error), -1);
        return false;
    }

    public static boolean setStreamingIntentParams(Context context, MegaNode node, MegaApiJava megaApi, Intent intent) {
        if (megaApi.httpServerIsRunning() == 0) {
            megaApi.httpServerStart();
        }

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);

        if(mi.totalMem>Constants.BUFFER_COMP){
            log("Total mem: "+mi.totalMem+" allocate 32 MB");
            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB);
        }
        else{
            log("Total mem: "+mi.totalMem+" allocate 16 MB");
            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_16MB);
        }

        String url = megaApi.httpServerGetLocalLink(node);
        if (url != null) {
            Uri uri = Uri.parse(url);
            if (uri != null) {
                intent.setDataAndType(uri, MimeTypeList.typeForName(node.getName()).getType());
                return true;
            }
        }

        ((ManagerActivityLollipop)context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.general_text_error), -1);
        return false;
    }

    public static boolean setURLIntentParams (Context context, MegaNode node, Intent intent, String localPath) {
        File mediaFile = new File(localPath);
        InputStream instream = null;
        boolean paramsSetSuccessfully = false;

        try {
            // open the file for reading
            instream = new FileInputStream(mediaFile.getAbsolutePath());
            // if file the available for reading
            if (instream != null) {
                // prepare the file for reading
                InputStreamReader inputreader = new InputStreamReader(instream);
                BufferedReader buffreader = new BufferedReader(inputreader);

                String line1 = buffreader.readLine();
                if(line1!=null){
                    String line2= buffreader.readLine();
                    String url = line2.replace("URL=","");
                    log("Is URL - launch browser intent");
                    intent.setData(Uri.parse(url));
                    paramsSetSuccessfully = true;
                }
            }
        } catch (Exception ex) {
            log("EXCEPTION reading file");
        } finally {
            // close the file.
            try {
                instream.close();
            } catch (IOException e) {
                log("EXCEPTION closing InputStream");
            }
        }
        if (paramsSetSuccessfully) {
            return true;
        }
        log("Not expected format: Exception on processing url file");
        return setLocalIntentParams(context, node, intent, localPath, true);
    }

    public static MegaNode getOutgoingOrIncomingParent(MegaApiAndroid megaApi, MegaNode node) {
        if (isOutgoingOrIncomingFolder(node)) {
            return node;
        }

        MegaNode parentNode = node;

        while (megaApi.getParentNode(parentNode) != null) {
            parentNode = megaApi.getParentNode(parentNode);

            if (isOutgoingOrIncomingFolder(parentNode)) {
                return parentNode;
            }
        }

        return null;
    }

    private static boolean isOutgoingOrIncomingFolder(MegaNode node) {
        if (node.isOutShare() || node.isInShare()) {
            return true;
        }

        return false;
    }

    private static void log(String log) {
        Util.log("FileUtils",log);
    }
}
