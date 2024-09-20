//copyright: https://stackoverflow.com/questions/34927748/android-5-0-documentfile-from-tree-uri/36162691#36162691
package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.CacheFolderManager.buildTempFile;
import static mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.TYPE_TEXT_PLAIN;
import static mega.privacy.android.app.utils.OfflineUtils.getOfflineFile;
import static mega.privacy.android.app.utils.TimeUtils.formatLongDateTime;
import static mega.privacy.android.app.utils.Util.getSizeString;
import static mega.privacy.android.app.utils.Util.isAndroid11OrUpper;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaOffline;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.di.DbHandlerModuleKt;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.data.model.MegaPreferences;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

public class FileUtil {

    static final String MAIN_DIR = File.separator + "MEGA";

    public static final String CAMERA_FOLDER = "Camera";

    public static final String DOWNLOAD_DIR = "MEGA Downloads";

    public static final String OLD_MK_FILE = MAIN_DIR + File.separator + "MEGAMasterKey.txt";

    public static final String OLD_RK_FILE = MAIN_DIR + File.separator + "MEGARecoveryKey.txt";

    public static final String JPG_EXTENSION = ".jpg";
    public static final String TXT_EXTENSION = ".txt";
    public static final String _3GP_EXTENSION = ".3gp";
    public static final String ANY_TYPE_FILE = "*/*";

    private static final String VOLUME_EXTERNAL = "external";
    private static final String VOLUME_INTERNAL = "internal";

    public static String getRecoveryKeyFileName(Context context) {
        return context.getString(R.string.general_rk) + TXT_EXTENSION;
    }

    public static boolean isAudioOrVideo(MegaNode node) {
        return MimeTypeList.typeForName(node.getName()).isVideoMimeType() || MimeTypeList.typeForName(node.getName()).isAudio();
    }

    public static boolean isInternalIntent(MegaNode node) {
        return !MimeTypeList.typeForName(node.getName()).isVideoNotSupported() && !MimeTypeList.typeForName(node.getName()).isAudioNotSupported();
    }

    private static boolean isOnMegaDownloads(MegaNode node) {
        File f = new File(getDownloadLocation(), node.getName());

        return isFileAvailable(f) && f.length() == node.getSize();
    }

    public static boolean isLocalFile(MegaNode node, MegaApiAndroid megaApi, String localPath) {
        String fingerprintNode = node.getFingerprint();
        return localPath != null && (isOnMegaDownloads(node) || (fingerprintNode != null && fingerprintNode.equals(megaApi.getFingerprint(localPath))));
    }

    public static boolean setLocalIntentParams(Context context, MegaOffline offline, Intent intent,
                                               String localPath, boolean isText, SnackbarShower snackbarShower) {
        return setLocalIntentParams(context, getOfflineFile(context, offline).getName(), intent,
                localPath, isText, snackbarShower);
    }

    public static boolean setLocalIntentParams(Context context, MegaNode node, Intent intent,
                                               String localPath, boolean isText, SnackbarShower snackbarShower) {
        return setLocalIntentParams(context, node.getName(), intent, localPath, isText,
                snackbarShower);
    }

    public static boolean setLocalIntentParams(Context context, String nodeName, Intent intent,
                                               String localPath, boolean isText,
                                               SnackbarShower snackbarShower) {
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

        snackbarShower.showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_text_error),
                MEGACHAT_INVALID_HANDLE);

        return false;
    }

    public static boolean setStreamingIntentParams(Context context, MegaNode node,
                                                   MegaApiJava megaApi, Intent intent,
                                                   SnackbarShower snackbarShower) {
        if (megaApi.httpServerIsRunning() == 0) {
            megaApi.httpServerStart();
            intent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
        }

        String url = megaApi.httpServerGetLocalLink(node);
        if (url != null) {
            Uri uri = Uri.parse(url);
            if (uri != null) {
                intent.setDataAndType(uri, MimeTypeList.typeForName(node.getName()).getType());
                return true;
            }
        }

        snackbarShower.showSnackbar(SNACKBAR_TYPE, context.getString(R.string.general_text_error),
                MEGACHAT_INVALID_HANDLE);

        return false;
    }

    public static File createTemporalTextFile(Context context, String name, String data) {
        String fileName = name + TXT_EXTENSION;

        return createTemporalFile(context, fileName, data);
    }

    public static File createTemporalURLFile(Context context, String name, String data) {
        String fileName = name + ".url";

        return createTemporalFile(context, fileName, data);
    }

    private static File createTemporalFile(Context context, String fileName, String data) {
        final File file = buildTempFile(fileName);

        try {
            FileOutputStream fOut = new FileOutputStream(file);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(data);

            myOutWriter.close();

            fOut.flush();
            fOut.close();

            return file;
        } catch (IOException e) {
            Timber.e(e, "File write failed");
            return null;
        }
    }

    /**
     * Checks if the received MegaNode exists in local.
     *
     * @param node MegaNode to check.
     * @return The path of the file if the local file exists, null otherwise.
     */
    public static String getLocalFile(MegaNode node) {
        if (node == null) {
            Timber.w("Node is null");
            return null;
        }

        String path;
        Context context = MegaApplication.getInstance();
        String data = MediaStore.Files.FileColumns.DATA;
        final String[] projection = {data};
        final String selection = MediaStore.Files.FileColumns.DISPLAY_NAME + " = ? AND "
                + MediaStore.Files.FileColumns.SIZE + " = ? AND "
                + MediaStore.Files.FileColumns.DATE_MODIFIED + " = ?";

        final String[] selectionArgs = {
                node.getName(),
                String.valueOf(node.getSize()),
                String.valueOf(node.getModificationTime())};

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
            Timber.e(e, "Haven't granted the permission.");
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

    /**
     * Checks if the tapped MegaNode exists in local.
     * Note: for node tapped event, only query system database by size.
     *
     * @param node MegaNode to check.
     * @return The path of the file if the local file exists, null otherwise.
     */
    public static String getTappedNodeLocalFile(MegaNode node) {
        if (node == null) {
            Timber.w("Node is null");
            return null;
        }

        String data = MediaStore.Files.FileColumns.DATA;
        final String[] projection = {data};
        // Only query file by size, then compare file name. Since modification time will changed(copy to SD card)
        // Display name may be null in the database.
        final String selection = MediaStore.Files.FileColumns.SIZE + " = ?";
        final String[] selectionArgs = {String.valueOf(node.getSize())};

        Context context = MegaApplication.getInstance();

        try {
            Cursor cursor = context.getContentResolver().query(MediaStore.Files.getContentUri(VOLUME_EXTERNAL), projection, selection, selectionArgs, null);

            List<String> candidates = getPotentialLocalPath(context, cursor, data, node.getName());

            for (String path : candidates) {
                if (isFileAvailable(new File(path))) {
                    return path;
                }
            }
        } catch (SecurityException e) {
            // Workaround: devices with system below Android 10 cannot execute the query without storage permission.
            Timber.e(e, "Haven't granted the permission.");
        }

        File file = new File(getDownloadLocationForPreviewingFiles() + "/" + node.getName());

        return isFileAvailable(file) ? file.getAbsolutePath() : null;
    }

    /**
     * Searches in the correspondent storage established if the file exists.
     * If a file path is found both on internal storage and SD card,
     * put the path on internal storage before that on SD card.
     *
     * @param context    Context object.
     * @param cursor     Cursor which contains all the requirements to find the file
     * @param columnName Column name in which search
     * @param fileName   Name of the searching node.
     * @return A list of file path that may be the path of the searching file.
     */
    private static List<String> getPotentialLocalPath(Context context, Cursor cursor, String columnName, String fileName) {
        List<String> candidates = new ArrayList<>();
        List<String> sdCandidates = new ArrayList<>();

        while (cursor != null && cursor.moveToNext()) {
            int dataColumn = cursor.getColumnIndexOrThrow(columnName);
            String path = cursor.getString(dataColumn);

            // Check file name.
            if (path.endsWith(fileName)) {
                if (SDCardUtils.isLocalFolderOnSDCard(context, path)) {
                    sdCandidates.add(path);
                } else {
                    candidates.add(path);
                }
            }
        }

        candidates.addAll(sdCandidates);

        if (cursor != null) {
            cursor.close();
        }

        return candidates;
    }

    /**
     * Copies a file from source to dest
     *
     * @param source Source file.
     * @param dest   Final copied file.
     * @throws IOException if some error happens while copying.
     */
    public static void copyFile(File source, File dest) throws IOException {
        if (!source.getAbsolutePath().equals(dest.getAbsolutePath())) {
            FileInputStream inputStream = new FileInputStream(source);
            FileOutputStream outputStream = new FileOutputStream(dest);
            FileChannel inputChannel = inputStream.getChannel();
            FileChannel outputChannel = outputStream.getChannel();
            outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
            inputChannel.close();
            outputChannel.close();
            inputStream.close();
            outputStream.close();

            sendBroadcastToUpdateGallery(MegaApplication.getInstance(), dest);
        }
    }

    /**
     * Checks if the file is a video file.
     *
     * @param path Local path of the file.
     * @deprecated use @link{#IsVideoFileUseCase} instead.
     */
    @Deprecated()
    public static boolean isVideoFile(String path) {
        Timber.d("isVideoFile: %s", path);
        try {
            String mimeType = URLConnection.guessContentTypeFromName(path);
            return mimeType != null && mimeType.indexOf("video") == 0;
        } catch (Exception e) {
            Timber.e(e);
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

    /**
     * Download location for previewing files
     *
     * @return File
     */
    public static File getDownloadLocationForPreviewingFiles() {
        Context context = MegaApplication.getInstance();
        // Using cache to save the files for previewing
        File downloadsDir = context.getExternalCacheDir();
        return downloadsDir != null ? downloadsDir : context.getCacheDir();
    }

    public static String getDownloadLocation() {
        if (isAndroid11OrUpper()) {
            File file = buildDefaultDownloadDir(MegaApplication.getInstance());
            file.mkdirs();
            return file.getAbsolutePath();
        }

        DatabaseHandler dbH = DbHandlerModuleKt.getDbHandler();
        MegaPreferences prefs = dbH.getPreferences();

        if (prefs != null
                && prefs.getStorageAskAlways() != null
                && !Boolean.parseBoolean(prefs.getStorageAskAlways())
                && prefs.getStorageDownloadLocation() != null
                && prefs.getStorageDownloadLocation().compareTo("") != 0) {
            return prefs.getStorageDownloadLocation();
        }

        return buildDefaultDownloadDir(MegaApplication.getInstance()).getAbsolutePath();
    }

    public static boolean isFileAvailable(File file) {
        return file != null && file.exists();
    }

    public static boolean isFileDownloadedLatest(File downloadedFile, MegaNode node) {
        return downloadedFile.lastModified() - node.getModificationTime() * 1000 >= 0;
    }

    public static File buildDefaultDownloadDir(Context context) {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadsDir != null) {
            return new File(downloadsDir, DOWNLOAD_DIR);
        } else {
            return context.getFilesDir();
        }
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
        return FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, file);
    }

    /**
     * Shares a file.
     *
     * @param context current Context.
     * @param file    file to share.
     */
    public static void shareFile(Context context, File file, String fileName) {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType(MimeTypeList.typeForName(file.getName()).getType() + "/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, getUriForFile(context, file));
        if (fileName != null && !fileName.isBlank()) {
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
        }
        // To avoid java.lang.SecurityException: Permission Denial
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.context_share)));
    }

    /**
     * Shares an uri.
     *
     * @param context current Context.
     * @param name    name of the uri.
     * @param uri     uri to share.
     */
    public static void shareUri(Context context, String name, Uri uri) {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType(MimeTypeList.typeForName(name).getType() + "/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        // To avoid java.lang.SecurityException: Permission Denial
        shareIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.context_share)));
    }

    /**
     * @Deprecated use [NodeShareContentUrisIntentMapper].
     * Share multiple files to other apps.
     * <p>
     * credit: https://stackoverflow.com/a/15577579/3077508
     *
     * @param context current Context
     * @param files   files to share
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
     * @param context   Current context.
     * @param extention The file's extention, for example, pdf, jpg. Use to infer the mimetype.
     * @param uri       The file's Uri.
     */
    public static void shareWithUri(Context context, String extention, Uri uri) {
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType(MimeTypeMap.getSingleton().getMimeTypeFromExtension(extention) + "/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.context_share)));
    }

    /**
     * The full path of a SD card URI has two parts:
     * 1. SD card root path, can get it from SDCardOperator. For example: /storage/2BA3-12F1.
     * 2. User selected specific folder path on the SD card, can get it from getDocumentPathFromTreeUri.
     *
     * @param treeUri The URI of the selected SD card location.
     * @param con     Context object.
     * @return Path of the selected SD card location.
     */
    @Nullable
    public static String getFullPathFromTreeUri(@Nullable final Uri treeUri, Context con) {
        if (treeUri == null) return null;

        String volumePath;
        SDCardOperator operator;
        try {
            operator = new SDCardOperator(con);
        } catch (SDCardOperator.SDCardException e) {
            Timber.e(e);
            return null;
        }

        volumePath = operator.getSDCardRoot();

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

    private static String getDocumentPathFromTreeUri(final Uri treeUri) {
        final String docId = DocumentsContract.getTreeDocumentId(treeUri);
        final String[] split = docId.split(":");
        if ((split.length >= 2) && (split[1] != null)) return split[1];
        else return File.separator;
    }

    /**
     * Copies a file to DCIM directory.
     *
     * @param fileToCopy File to copy.
     * @return The copied file on DCIM.
     */
    public static File copyFileToDCIM(File fileToCopy) {
        File storageDir = getCameraFolder();

        File copyFile = new File(storageDir, fileToCopy.getName());
        try {
            copyFile(fileToCopy, copyFile);
        } catch (IOException e) {
            Timber.e(e, "IOException copying file.");
            copyFile.delete();
        }

        return copyFile;
    }

    /**
     * Notifies a new file has been added to the local storage.
     *
     * @param context Current context.
     * @param file    File to notify.
     */
    public static void sendBroadcastToUpdateGallery(Context context, File file) {
        try {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

            Uri finishedContentUri = FileProvider.getUriForFile(context, AUTHORITY_STRING_FILE_PROVIDER, file);

            mediaScanIntent.setData(finishedContentUri);
            mediaScanIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mediaScanIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }

            mediaScanIntent.setPackage(context.getApplicationContext().getPackageName());
            context.sendBroadcast(mediaScanIntent);
        } catch (Exception e) {
            Timber.w(e, "Exception sending mediaScanIntent.");
        }

        try {
            MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()}, null, (path1, uri)
                    -> Timber.d("File was scanned successfully"));
        } catch (Exception e) {
            Timber.w(e, "Exception scanning file.");
        }
    }

    public static File getCameraFolder() {
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), CAMERA_FOLDER);

        if (!storageDir.exists()) {
            storageDir.mkdir();
        }

        return storageDir;
    }

    /**
     * Gets the string to show as file info details with the next format: "size · modification date".
     *
     * @param file The file  from which to get the details.
     * @return The string so show as file info details.
     */
    public static String getFileInfo(File file, Context context) {
        return TextUtil.getFileInfo(getSizeString(file.length(), context), formatLongDateTime(file.lastModified() / 1000));
    }
}
