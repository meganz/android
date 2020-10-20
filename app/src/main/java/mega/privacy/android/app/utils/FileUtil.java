//copyright: https://stackoverflow.com/questions/34927748/android-5-0-documentfile-from-tree-uri/36162691#36162691
package mega.privacy.android.app.utils;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

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
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.OfflineUtils.getOfflineFile;
import static mega.privacy.android.app.utils.Util.showSnackbar;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

public class FileUtil {

    static final String MAIN_DIR = File.separator + "MEGA";

    public static final String DOWNLOAD_DIR = MAIN_DIR + File.separator + "MEGA Downloads";

    public static final String LOG_DIR = MAIN_DIR + File.separator + "MEGA Logs";

    public static final String OLD_MK_FILE = MAIN_DIR + File.separator + "MEGAMasterKey.txt";

    public static final String OLD_RK_FILE = MAIN_DIR + File.separator + "MEGARecoveryKey.txt";

    public static final String JPG_EXTENSION = ".jpg";

    private static final String VOLUME_EXTERNAL = "external";
    private static final String VOLUME_INTERNAL = "internal";

    private static final String PRIMARY_VOLUME_NAME = "primary";

    public static String getRecoveryKeyFileName() {
        return MegaApplication.getInstance().getApplicationContext().getString(R.string.general_rk) + ".txt";
    }

    public static boolean isAudioOrVideo(MegaNode node) {
        return MimeTypeList.typeForName(node.getName()).isVideoReproducible() || MimeTypeList.typeForName(node.getName()).isAudio();
    }

    public static boolean isInternalIntent(MegaNode node) {
        return !MimeTypeList.typeForName(node.getName()).isVideoNotSupported() && !MimeTypeList.typeForName(node.getName()).isAudioNotSupported();
    }

    public static boolean isOpusFile(MegaNode node) {
        String[] s = node.getName().split("\\.");

        return s.length > 1 && s[s.length - 1].equals("opus");
    }

    private static boolean isOnMegaDownloads(MegaNode node) {
        File f = new File(getDownloadLocation(), node.getName());

        return isFileAvailable(f) && f.length() == node.getSize();
    }

    public static boolean isLocalFile(MegaNode node, MegaApiAndroid megaApi, String localPath) {
        String fingerprintNode = megaApi.getFingerprint(node);
        return localPath != null && (isOnMegaDownloads(node) || (fingerprintNode != null && fingerprintNode.equals(megaApi.getFingerprint(localPath))));
    }

    public static boolean setLocalIntentParams(Context context, MegaOffline offline, Intent intent,
            String localPath, boolean isText) {
        return setLocalIntentParams(context, getOfflineFile(context, offline).getName(), intent,
                localPath, isText);
    }

    public static boolean setLocalIntentParams(Context context, MegaNode node, Intent intent,
            String localPath, boolean isText) {
        return setLocalIntentParams(context, node.getName(), intent, localPath, isText);
    }

    public static boolean setLocalIntentParams(Context context, String nodeName, Intent intent,
            String localPath, boolean isText) {
        File mediaFile = new File(localPath);

        Uri mediaFileUri;
        try {
            mediaFileUri = FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER,
                    mediaFile);
        } catch (IllegalArgumentException e) {
            mediaFileUri = Uri.fromFile(mediaFile);
        }

        if (mediaFileUri != null) {
            if (isText) {
                intent.setDataAndType(mediaFileUri, TYPE_TEXT_PLAIN);
            } else {
                intent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(nodeName).getType());
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return true;
        }

        ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE,
                context.getString(R.string.general_text_error), MEGACHAT_INVALID_HANDLE);
        return false;
    }

    public static boolean setStreamingIntentParams(Context context, MegaNode node, MegaApiJava megaApi, Intent intent) {
        if (megaApi.httpServerIsRunning() == 0) {
            megaApi.httpServerStart();
        }

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
        } else {
            activityManager.getMemoryInfo(mi);

            if (mi.totalMem > BUFFER_COMP) {
                megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_32MB);
            } else {
                megaApi.httpServerSetMaxBufferSize(MAX_BUFFER_16MB);
            }
        }

        String url = megaApi.httpServerGetLocalLink(node);
        if (url != null) {
            Uri uri = Uri.parse(url);
            if (uri != null) {
                intent.setDataAndType(uri, MimeTypeList.typeForName(node.getName()).getType());
                return true;
            }
        }

        ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_text_error), -1);
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
            InputStreamReader inputreader = new InputStreamReader(instream);
            BufferedReader buffreader = new BufferedReader(inputreader);

            String line1 = buffreader.readLine();
            if (line1 != null) {
                String line2 = buffreader.readLine();
                String url = line2.replace("URL=", "");
                intent.setData(Uri.parse(url));
                paramsSetSuccessfully = true;
            }
        } catch (Exception ex) {
            logError("EXCEPTION reading file", ex);
        } finally {
            // close the file.
            try {
                if (instream != null) {
                    instream.close();
                }
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

    public static File createTemporalTextFile(Context context, String name, String data) {
        String fileName = name + ".txt";

        return createTemporalFile(context, fileName, data);
    }

    public static File createTemporalURLFile(Context context, String name, String data) {
        String fileName = name + ".url";

        return createTemporalFile(context, fileName, data);
    }

    private static File createTemporalFile(Context context, String fileName, String data) {
        final File file = buildTempFile(context, fileName);

        try {
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);

            myOutWriter.close();

            fOut.flush();
            fOut.close();

            return file;
        } catch (IOException e) {
            logError("File write failed", e);
            return null;
        }
    }

    public static long getDirSize(File dir) {

        long size = 0;
        if (dir == null) {
            return -1;
        }

        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    size += file.length();
                } else {
                    size += getDirSize(file);
                }
            }
            return size;
        }
        logDebug("Dir size: " + size);
        return size;
    }

    /**
     * Checks if a local file exists
     *
     * @param context  the current context
     * @param fileName name of the file
     * @param fileSize size of the file
     * @return The path of the file if the local file exists, null otherwise
     */
    public static String getLocalFile(Context context, String fileName, long fileSize) {
        String data = MediaStore.Files.FileColumns.DATA;
        final String[] projection = {data};
        final String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + " = ? AND " + MediaStore.Files.FileColumns.SIZE + " = ?";
        final String[] selectionArgs = {fileName, String.valueOf(fileSize)};
        String path;

        try {
            Cursor cursor = context.getContentResolver().query(
                    MediaStore.Files.getContentUri(VOLUME_EXTERNAL), projection, selection,
                    selectionArgs, null);

            path = checkFileInStorage(cursor, data);
            if (path == null) {
                cursor = context.getContentResolver().query(
                        MediaStore.Files.getContentUri(VOLUME_INTERNAL), projection, selection,
                        selectionArgs, null);
                path = checkFileInStorage(cursor, data);
            }
        } catch (SecurityException e) {
            // Workaround: devices with system below Android 10 cannot execute the query without storage permission.
            logError("Haven't granted the permission.", e);
            return null;
        }

        return path;
    }

    /**
     * Searches in the correspondent storage established if the file exists
     *
     * @param cursor Cursor which contains all the requirements to find the file
     * @param data   Column name in which search
     * @return The path of the file if exists
     */
    private static String checkFileInStorage(Cursor cursor, String data) {
        if (cursor != null && cursor.moveToFirst()) {
            int dataColumn = cursor.getColumnIndexOrThrow(data);
            String path = cursor.getString(dataColumn);
            cursor.close();
            cursor = null;
            if (new File(path).exists()) {
                return path;
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return null;
    }

    /*
     * Check is file belongs to the app
     */
    public static boolean isLocal(Context context, File file) {
        File tmp = context.getDir("tmp", 0);
        return tmp != null && tmp.getParent() != null && file.getAbsolutePath().contains(tmp.getParent());
    }

    /*
     * Check is file belongs to the app and temporary
     */
    public static boolean isLocalTemp(Context context, File file) {
        return isLocal(context, file) && file.getAbsolutePath().endsWith(".tmp");
    }

    public static void copyFile(File source, File dest) throws IOException {
        logDebug("copyFile");

        if (!source.getAbsolutePath().equals(dest.getAbsolutePath())) {
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
        try {
            String mimeType = URLConnection.guessContentTypeFromName(path);
            return mimeType != null && mimeType.indexOf("video") == 0;
        } catch (Exception e) {
            logError("Exception", e);
            return false;
        }
    }

    public static boolean isFile(String path) {
        if (path == null) {
            path = "";
        }
        String fixedName = path.trim().toLowerCase();
        String extension = null;
        int index = fixedName.lastIndexOf(".");
        if ((index != -1) && ((index + 1) < fixedName.length())) {
            extension = fixedName.substring(index + 1);
        }

        return extension != null;
    }

    public static String getDownloadLocation() {
        DatabaseHandler dbH = DatabaseHandler.getDbHandler(MegaApplication.getInstance());
        MegaPreferences prefs = dbH.getPreferences();

        if (prefs != null
                && prefs.getStorageAskAlways() != null
                && !Boolean.parseBoolean(prefs.getStorageAskAlways())
                && prefs.getStorageDownloadLocation() != null
                && prefs.getStorageDownloadLocation().compareTo("") != 0) {
            return prefs.getStorageDownloadLocation();
        }
        return DOWNLOAD_DIR;
    }

    public static boolean isFileAvailable(File file) {
        return file != null && file.exists();
    }

    public static boolean isFileDownloadedLatest(File downloadedFile, MegaNode node) {
        return downloadedFile.lastModified() - node.getModificationTime() * 1000 >= 0;
    }

    public static String getExternalStoragePath(String filePath) {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + filePath;
    }

    public static File buildExternalStorageFile(String filePath) {
        return new File(getExternalStoragePath(filePath));
    }

    public static File buildDefaultDownloadDir(Context context) {
        if (Environment.getExternalStorageDirectory() != null) {
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
    public static String findVideoLocalPath(Context context, MegaNode node) {
        String path = queryByNameAndSize(context, MediaStore.Video.Media.INTERNAL_CONTENT_URI, node);
        if (path == null) {
            path = queryByNameAndSize(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, node);
        }

        if (path == null) {
            path = queryByNameOrSize(context, MediaStore.Video.Media.INTERNAL_CONTENT_URI, node);
            if (path == null) {
                path = queryByNameOrSize(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, node);
            }
        }
        // if needed, can add file system scanning here.
        return path;
    }

    private static String queryByNameOrSize(Context context, Uri uri, MegaNode node) {
        String selection = MediaStore.Video.Media.DISPLAY_NAME + " = ? OR " + MediaStore.Video.Media.SIZE + " = ?";
        return query(context, uri, selection, node);
    }

    private static String queryByNameAndSize(Context context, Uri uri, MegaNode node) {
        String selection = MediaStore.Video.Media.DISPLAY_NAME + " = ? AND " + MediaStore.Video.Media.SIZE + " = ?";
        return query(context, uri, selection, node);
    }

    @Nullable
    private static String query(Context context, Uri uri, String selection, MegaNode node) {
        String fileName = node.getName();
        long fileSize = node.getSize();
        String[] selectionArgs = {fileName, String.valueOf(fileSize)};
        Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.Video.Media.DATA}, selection, selectionArgs, null);
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
        if (!dir.exists()) {
            return;
        }

        try {
            if (dir.listFiles() == null) return;

            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    purgeDirectory(file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param fileName The original file name
     * @return the file name without extension. For example, 1.jpg would return 1
     */
    static String getFileNameWithoutExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        if ((index != -1) && ((index + 1) < fileName.length())) {
            return fileName.substring(0, fileName.lastIndexOf("."));
        } else {
            return "";
        }
    }

    /**
     * @param filename The original file name
     * @return whether the file name is purely number
     */
    static boolean isFileNameNumeric(String filename) {
        return getFileNameWithoutExtension(filename).matches("-?\\d+(\\.\\d+)?");
    }

    public static String addPdfFileExtension(String title) {
        if (title != null && !title.endsWith(".pdf")) {
            title += ".pdf";
        }
        return title;
    }

    /**
     * Gets the uri of a local file.
     *
     * @param context current Context.
     * @param file    file to get the uri.
     * @return The uri of the file.
     */
    public static Uri getUriForFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", file);
        }

        return Uri.fromFile(file);
    }

    /**
     * Shares a file.
     *
     * @param context current Context.
     * @param file    file to share.
     */
    public static void shareFile(Context context, File file) {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType(MimeTypeList.typeForName(file.getName()).getType() + "/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, getUriForFile(context, file));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.context_share)));
    }

    /**
     * Share multiple files to other apps.
     *
     * credit: https://stackoverflow.com/a/15577579/3077508
     *
     * @param context current Context
     * @param files files to share
     */
    public static void shareFiles(Context context, List<File> files) {
        String intentType = null;
        for (File file : files) {
            String type = MimeTypeList.typeForName(file.getName()).getType();
            if (intentType == null) {
                intentType = type;
            } else if (!TextUtils.equals(intentType, type)) {
                intentType = "*";
                break;
            }
        }
        ArrayList<Uri> uris = new ArrayList<>();
        for (File file : files) {
            uris.add(getUriForFile(context, file));
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        shareIntent.setType(intentType + "/*");
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(
                Intent.createChooser(shareIntent, context.getString(R.string.context_share)));
    }

    /**
     * Share a file via its Uri.
     *
     * @param context Current context.
     * @param extention The file's extention, for example, pdf, jpg. Use to infer the mimetype.
     * @param uri The file's Uri.
     */
    public static void shareWithUri(Context context, String extention, Uri uri) {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extention) + "/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.context_share)));
    }

    /**
     * According device's Android version to see if get file path and write permission by FileStorageActivity.
     *
     * @return true if using FileStorageActivity to get file path and write permission on the path.
     * false by SAF
     */
    public static boolean isBasedOnFileStorage() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q;
    }

    @Nullable
    public static String getFullPathFromTreeUri(@Nullable final Uri treeUri, Context con) {
        if (treeUri == null) return null;
        String volumePath = getVolumePath(getVolumeIdFromTreeUri(treeUri), con);
        if (volumePath == null) return File.separator;
        if (volumePath.endsWith(File.separator))
            volumePath = volumePath.substring(0, volumePath.length() - 1);

        String documentPath = getDocumentPathFromTreeUri(treeUri);
        if (documentPath.endsWith(File.separator))
            documentPath = documentPath.substring(0, documentPath.length() - 1);

        if (documentPath.length() > 0) {
            if (documentPath.startsWith(File.separator))
                return volumePath + documentPath;
            else
                return volumePath + File.separator + documentPath;
        } else return volumePath;
    }


    @SuppressLint("ObsoleteSdkInt")
    private static String getVolumePath(final String volumeId, Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null;
        try {
            StorageManager mStorageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
            Class<?> storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getUuid = storageVolumeClazz.getMethod("getUuid");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isPrimary = storageVolumeClazz.getMethod("isPrimary");
            Object result = getVolumeList.invoke(mStorageManager);

            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String uuid = (String) getUuid.invoke(storageVolumeElement);
                Boolean primary = (Boolean) isPrimary.invoke(storageVolumeElement);

                // primary volume?
                if (primary && PRIMARY_VOLUME_NAME.equals(volumeId))
                    return (String) getPath.invoke(storageVolumeElement);

                // other volumes?
                if (uuid != null && uuid.equals(volumeId))
                    return (String) getPath.invoke(storageVolumeElement);
            }
            // not found.
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getVolumeIdFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if (split.length > 0) return split[0];
        else return null;
    }


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private static String getDocumentPathFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) return split[1];
        else return File.separator;
    }

    public static File getFileFromContentUri(Context context, Uri uri) {
        File file = new File(context.getCacheDir(), uri.getLastPathSegment());
        InputStream in = null;
        OutputStream out = null;

        try {
            in = context.getContentResolver().openInputStream(uri);
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file;
    }
}

