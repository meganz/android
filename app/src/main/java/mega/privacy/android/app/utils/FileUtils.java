package mega.privacy.android.app.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLConnection;
import java.nio.channels.FileChannel;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class FileUtils {

    public static final String MAIN_DIR = File.separator + "MEGA";

    public static final String DOWNLOAD_DIR = MAIN_DIR + File.separator + "MEGA Downloads";

    public static final String LOG_DIR = MAIN_DIR + File.separator + "MEGA Logs";

    public static final String OLD_MK_FILE = MAIN_DIR + File.separator + "MEGAMasterKey.txt";

    public static final String OLD_RK_FILE = MAIN_DIR + File.separator + "MEGARecoveryKey.txt";

    public static String getRecoveryKeyFileName() {
        return MegaApplication.getInstance().getApplicationContext().getString(R.string.general_rk) + ".txt";
    }

    public static boolean isAudioOrVideo(MegaNode node) {
        if (MimeTypeList.typeForName(node.getName()).isVideoReproducible() || MimeTypeList.typeForName(node.getName()).isAudio())
            return true;

        return false;
    }

    public static boolean isInternalIntent(MegaNode node) {
        if (MimeTypeList.typeForName(node.getName()).isVideoNotSupported() || MimeTypeList.typeForName(node.getName()).isAudioNotSupported())
            return false;

        return true;
    }

    public static boolean isOpusFile(MegaNode node) {
        String[] s = node.getName().split("\\.");
        if (s != null && s.length > 1 && s[s.length - 1].equals("opus")) return true;

        return false;
    }

    public static boolean isOnMegaDownloads(Context context, MegaNode node) {
        File f = new File(getDownloadLocation(context), node.getName());

        if (isFileAvailable(f) && f.length() == node.getSize()) {
            return true;
        }

        return false;
    }

    public static boolean isLocalFile(Context context, MegaNode node, MegaApiAndroid megaApi, String localPath) {
        if (localPath != null && (isOnMegaDownloads(context, node) || (megaApi.getFingerprint(node) != null && megaApi.getFingerprint(node).equals(megaApi.getFingerprint(localPath)))))
            return true;

        return false;
    }

    public static boolean setLocalIntentParams(Context context, MegaNode node, Intent intent, String localPath, boolean isText) {
        File mediaFile = new File(localPath);

        Uri mediaFileUri;
        try {
            mediaFileUri = FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", mediaFile);
        } catch (IllegalArgumentException e) {
            mediaFileUri = Uri.fromFile(mediaFile);
        }

        if (mediaFileUri != null) {
            if (isText) {
                intent.setDataAndType(mediaFileUri, "text/plain");
            } else {
                intent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return true;
        }

        ((ManagerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.general_text_error), -1);
        return false;
    }

    public static boolean setStreamingIntentParams(Context context, MegaNode node, MegaApiJava megaApi, Intent intent) {
        if (megaApi.httpServerIsRunning() == 0) {
            megaApi.httpServerStart();
        }

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);

        if (mi.totalMem > Constants.BUFFER_COMP) {
            megaApi.httpServerSetMaxBufferSize(Constants.MAX_BUFFER_32MB);
        } else {
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

        ((ManagerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.general_text_error), -1);
        return false;
    }

    public static boolean setURLIntentParams(Context context, MegaNode node, Intent intent, String localPath) {
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
                if (line1 != null) {
                    String line2 = buffreader.readLine();
                    String url = line2.replace("URL=", "");
                    intent.setData(Uri.parse(url));
                    paramsSetSuccessfully = true;
                }
            }
        } catch (Exception ex) {
            logError("EXCEPTION reading file", ex);
        } finally {
            // close the file.
            try {
                instream.close();
            } catch (IOException e) {
                logError("EXCEPTION closing InputStream", e);
            }
        }
        if (paramsSetSuccessfully) {
            return true;
        }
        logError("Not expected format: Exception on processing url file");
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

    public static void deleteFolderAndSubfolders(Context context, File f) throws IOException {

        if (f == null) return;

        logDebug("deleteFolderAndSubfolders: " + f.getAbsolutePath());
        if (f.isDirectory() && f.listFiles() != null) {
            for (File c : f.listFiles()) {
                deleteFolderAndSubfolders(context, c);
            }
        }

        if (!f.delete()) {
            throw new FileNotFoundException("Failed to delete file: " + f);
        } else {
            try {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                File fileToDelete = new File(f.getAbsolutePath());
                Uri contentUri;
                try {
                    contentUri = FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", fileToDelete);
                } catch (IllegalArgumentException e) {
                    contentUri = Uri.fromFile(fileToDelete);
                }
                mediaScanIntent.setData(contentUri);
                mediaScanIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.sendBroadcast(mediaScanIntent);
            } catch (Exception e) {
                logError("Exception while deleting media scanner file", e);
            }

        }
    }

    public static File createTemporalTextFile(Context context, String name, String data){
        String fileName = name+".txt";

        return createTemporalFile(context, fileName, data);
    }

    public static File createTemporalURLFile(Context context, String name, String data){
        String fileName = name+".url";

        return createTemporalFile(context, fileName, data);
    }

    public static File createTemporalFile(Context context, String fileName, String data) {
        final File file = buildTempFile(context, fileName);

        try
        {
            file.createNewFile();
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);

            myOutWriter.close();

            fOut.flush();
            fOut.close();

            return file;
        }
        catch (IOException e)
        {
            logError("File write failed", e);
            return null;
        }
    }

    public static long getDirSize(File dir) {

        long size = 0;
        if(dir==null){
            return -1;
        }

        File[] files = dir.listFiles();

        if(files !=null){
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                }
                else{
                    size += getDirSize(file);
                }
            }
            return size;
        }
        logDebug("Dir size: " + size);
        return size;
    }

    public static String getLocalFile(Context context, String fileName, long fileSize,
                                      String destDir)
    {
        Cursor cursor = null;
        try
        {
            if(MimeTypeList.typeForName(fileName).isImage())
            {
                final String[] projection = { MediaStore.Images.Media.DATA };
                final String selection = MediaStore.Images.Media.DISPLAY_NAME + " = ? AND " + MediaStore.Images.Media.SIZE + " = ?";
                final String[] selectionArgs = { fileName, String.valueOf(fileSize) };

                cursor = context.getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, selection,
                        selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    String path =  cursor.getString(dataColumn);
                    cursor.close();
                    cursor = null;
                    if(new File(path).exists()){
                        return path;
                    }
                }
                if(cursor != null) cursor.close();

                cursor = context.getContentResolver().query(
                        MediaStore.Images.Media.INTERNAL_CONTENT_URI, projection, selection,
                        selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    String path =  cursor.getString(dataColumn);
                    cursor.close();
                    cursor = null;
                    if(new File(path).exists()) return path;
                }
                if(cursor != null) cursor.close();
            }
            else if(MimeTypeList.typeForName(fileName).isVideoReproducible())
            {
                final String[] projection = { MediaStore.Video.Media.DATA };
                final String selection = MediaStore.Video.Media.DISPLAY_NAME + " = ? AND " + MediaStore.Video.Media.SIZE + " = ?";
                final String[] selectionArgs = { fileName, String.valueOf(fileSize) };

                cursor = context.getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection,
                        selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                    String path =  cursor.getString(dataColumn);
                    cursor.close();
                    cursor = null;
                    if(new File(path).exists()) return path;
                }
                if(cursor != null) cursor.close();

                cursor = context.getContentResolver().query(
                        MediaStore.Video.Media.INTERNAL_CONTENT_URI, projection, selection,
                        selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                    String path =  cursor.getString(dataColumn);
                    cursor.close();
                    cursor = null;
                    if(new File(path).exists()) return path;
                }
                if(cursor != null) cursor.close();
            }
            else if (MimeTypeList.typeForName(fileName).isAudio()) {
                final String[] projection = { MediaStore.Audio.Media.DATA };
                final String selection = MediaStore.Audio.Media.DISPLAY_NAME + " = ? AND " + MediaStore.Audio.Media.SIZE + " = ?";
                final String[] selectionArgs = { fileName, String.valueOf(fileSize) };

                cursor = context.getContentResolver().query(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, selection,
                        selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    String path =  cursor.getString(dataColumn);
                    cursor.close();
                    cursor = null;
                    if(new File(path).exists()) return path;
                }
                if(cursor != null) cursor.close();

                cursor = context.getContentResolver().query(
                        MediaStore.Video.Media.INTERNAL_CONTENT_URI, projection, selection,
                        selectionArgs, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                    String path =  cursor.getString(dataColumn);
                    cursor.close();
                    cursor = null;
                    if(new File(path).exists()) return path;
                }
                if(cursor != null) cursor.close();
            }
        } catch (Exception e)
        {
            if(cursor != null) cursor.close();
        }

        //Not found, searching in the download folder
        if(destDir != null)
        {
            File file = new File(destDir, fileName);
            if(file.exists() && (file.length() == fileSize))
                return file.getAbsolutePath();
        }
        return null;
    }

    /*
     * Check is file belongs to the app
     */
    public static boolean isLocal(Context context, File file) {
        File tmp = context.getDir("tmp", 0);
        return file.getAbsolutePath().contains(tmp.getParent());
    }

    /*
     * Check is file belongs to the app and temporary
     */
    public static boolean isLocalTemp(Context context, File file) {
        return isLocal(context, file) && file.getAbsolutePath().endsWith(".tmp");
    }

    public static void copyFile(File source, File dest) throws IOException{
        logDebug("copyFile");

        if (!source.getAbsolutePath().equals(dest.getAbsolutePath())){
            FileChannel inputChannel = null;
            FileChannel outputChannel = null;
            FileInputStream inputStream = new FileInputStream(source);
            FileOutputStream outputStream = new FileOutputStream(dest);
            inputChannel = inputStream.getChannel();
            outputChannel = outputStream.getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            inputChannel.close();
            outputChannel.close();
            inputStream.close();
            outputStream.close();
        }
    }

    public static boolean isVideoFile(String path) {
        logDebug("isVideoFile: " + path);
        try{
            String mimeType = URLConnection.guessContentTypeFromName(path);
            return mimeType != null && mimeType.indexOf("video") == 0;
        }
        catch(Exception e){
            logError("Exception", e);
            return false;
        }
    }

    public static boolean isFile (String path){
        if (path == null) {
            path = "";
        }
        String fixedName = path.trim().toLowerCase();
        String extension = null;
        int index = fixedName.lastIndexOf(".");
        if((index != -1) && ((index+1)<fixedName.length())) {
            extension = fixedName.substring(index + 1);
        }

        if(extension!=null){
            return true;
        }

        return false;
    }

    public static String getDownloadLocation (Context context) {
        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
        MegaPreferences prefs = dbH.getPreferences();

        if (prefs != null
                && prefs.getStorageAskAlways() != null
                && !Boolean.parseBoolean(prefs.getStorageAskAlways())
                && prefs.getStorageDownloadLocation() != null
                && prefs.getStorageDownloadLocation().compareTo("") != 0){
            return prefs.getStorageDownloadLocation();
        }
        return DOWNLOAD_DIR;
    }

    public static boolean isFileAvailable(File file) {
        return file != null && file.exists();
    }

    public static void copyFolder(File source, File destination) throws IOException {

        if (source.isDirectory()) {
            if (!destination.exists() && !destination.mkdirs()) {
                throw new IOException("Cannot create dir " + destination.getAbsolutePath());
            }

            String[] children = source.list();
            for (int i = 0; i < children.length; i++) {
                copyFolder(new File(source, children[i]), new File(destination, children[i]));
            }
        } else {
            File directory = destination.getParentFile();
            if (directory != null && !directory.exists() && !directory.mkdirs()) {
                throw new IOException("Cannot create dir " + directory.getAbsolutePath());
            }

            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(destination);

            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }
    }

    public static String getExternalStoragePath(String filePath) {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + filePath;
    }

    public static File buildExternalStorageFile(String filePath) {
        return new File(getExternalStoragePath(filePath));
    }

    public static File buildDefaultDownloadDir(Context context) {
        if (Environment.getExternalStorageDirectory() != null){
            return buildExternalStorageFile(DOWNLOAD_DIR);
        } else {
            return context.getFilesDir();
        }
    }

    /**
     * Find the local path of a video node.
     *
     * @param node MegaNode in cloud drive which should be a video.
     * @return Corresponding local path of the node.
     */
    public static String findVideoLocalPath (Context context, MegaNode node) {
        String path = queryByNameAndSize(context, MediaStore.Video.Media.INTERNAL_CONTENT_URI,node);
        if(path == null) {
            path = queryByNameAndSize(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI,node);
        }

        if(path == null) {
            path = queryByNameOrSize(context, MediaStore.Video.Media.INTERNAL_CONTENT_URI,node);
            if(path == null) {
                path = queryByNameOrSize(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI,node);
            }
        }
        // if needed, can add file system scanning here.
        return path;
    }

    private static String queryByNameOrSize(Context context, Uri uri,MegaNode node) {
        String selection = MediaStore.Video.Media.DISPLAY_NAME + " = ? OR " + MediaStore.Video.Media.SIZE + " = ?";
        return query(context, uri,selection,node);
    }

    private static String queryByNameAndSize(Context context, Uri uri,MegaNode node) {
        String selection = MediaStore.Video.Media.DISPLAY_NAME + " = ? AND " + MediaStore.Video.Media.SIZE + " = ?";
        return query(context, uri,selection,node);
    }

    @Nullable
    private static String query(Context context, Uri uri,String selection,MegaNode node) {
        String fileName = node.getName();
        long fileSize = node.getSize();
        String[] selectionArgs = { fileName, String.valueOf(fileSize) };
        Cursor cursor = context.getContentResolver().query(uri,new String[] { MediaStore.Video.Media.DATA },selection,selectionArgs,null);
        if (cursor != null && cursor.moveToFirst()) {
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            String path = cursor.getString(dataColumn);
            cursor.close();
            File localFile = new File(path);
            if (localFile.exists()) {
                return path;
            }
        }
        return null;
    }

    public static void purgeDirectory(File dir) {
        logDebug("Removing cache files");
        if(!dir.exists()){
            return;
        }

        try{
            for (File file: dir.listFiles()) {
                logDebug("Removing " + file.getAbsolutePath());
                if (file.isDirectory()) {
                    purgeDirectory(file);
                }
                file.delete();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static boolean appendStringToFile(final String appendContents, final File file) {
        boolean result = false;
        try {
            if (file != null && file.canWrite()) {
                file.createNewFile(); // ok if returns false, overwrite
                Writer out = new BufferedWriter(new FileWriter(file, true), 1024);
                out.write(appendContents);
                out.close();
                result = true;
            }
        } catch (IOException e) {
            logError("Error appending string data to file", e);
            e.printStackTrace();
        }
        return result;
    }


    /**
     * The static class to show taken down notice for mega node
     */
    public static class FileTakenDownNotificationHandler {
        /**
         * alertDialogTakenDown is the dialog to be shown. It resides inside this static class to prevent multiple definition within the class
         */
        private static AlertDialog alertDialogTakenDown = null;

        /**
         * @param activity the activity is the page where dialog is shown
         */
        public static void showTakenDownDialog(final Activity activity) {

            if (activity == null) {
                return;
            }

            if (alertDialogTakenDown != null && alertDialogTakenDown.isShowing()) {
                return;
            }

            if (activity.isFinishing()) {
                return;
            }

            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
            dialogBuilder.setTitle(activity.getString(R.string.general_error_word))
                         .setMessage(activity.getString(R.string.video_takendown_error)).setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                    activity.finish();
                }
            });
            alertDialogTakenDown = dialogBuilder.create();

            alertDialogTakenDown.show();
        }
    }
}

