package mega.privacy.android.app.utils;

import static mega.privacy.android.app.utils.Constants.COPY_FILE_BUFFER_SIZE;
import static mega.privacy.android.app.utils.FileUtil.getFullPathFromTreeUri;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mega.privacy.android.data.database.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;
import timber.log.Timber;

public class SDCardOperator {

    public final static String TEST = "test";
    public final static String TEMP = "temp";
    public final static String SD_PATH_REGEX = "^/storage/\\w{4}-\\w{4}/?.*$";

    private Context context;

    private String downloadRoot;

    private String sdCardRoot;

    private String targetRoot;

    private DocumentFile sdCardRootDocument;

    private boolean isSDCardDownload;

    public static class SDCardException extends Exception {

        public SDCardException(String message) {
            super(message);
        }

        private SDCardException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private SDCardOperator(boolean isSDCardDownload) {
        this.isSDCardDownload = isSDCardDownload;
    }

    public SDCardOperator(Context context) throws SDCardException {
        this.context = context;
        //package cache folder on sd card.
        File[] fs = context.getExternalCacheDirs();
        if (fs.length > 1 && fs[1] != null) {
            downloadRoot = fs[1].getAbsolutePath() + File.separator + System.currentTimeMillis();
        } else {
            throw new SDCardException("No sd card installed!");
        }
        sdCardRoot = SDCardUtils.getSDCardRoot(downloadRoot);
    }

    public void initDocumentFileRoot(String uriString) throws SDCardException {
        if (TextUtils.isEmpty(uriString)) {
            throw new SDCardException("Haven't got sd card root uri!");
        } else {
            try {
                Uri tragetUri = Uri.parse(uriString);
                targetRoot = getFullPathFromTreeUri(tragetUri, context);
                sdCardRootDocument = DocumentFile.fromTreeUri(context, tragetUri);
                if (sdCardRootDocument == null || !sdCardRootDocument.canWrite()) {
                    throw new SDCardException("Permission required!");
                }
            } catch (Exception e) {
                if (!(e instanceof SDCardException)) {
                    throw new SDCardException("Invalid uri string.", e);
                } else {
                    throw e;
                }
            }
        }
    }

    public String getDownloadRoot() {
        return downloadRoot;
    }

    public String getSDCardRoot() {
        return sdCardRoot;
    }

    public boolean isNewSDCardPath(String path) {
        return !path.startsWith(sdCardRoot);
    }

    public static boolean isSDCardPath(String path) {
        return path.matches(SD_PATH_REGEX);
    }

    public void buildFileStructure(Map<Long, String> dlList, String parent, MegaApiJava api, MegaNode node) {
        if (node.isFolder()) {
            createFolder(parent, node.getName());
            parent += (File.separator + node.getName());
            ArrayList<MegaNode> children = api.getChildren(node);
            if (children != null) {
                for (int i = 0; i < children.size(); i++) {
                    MegaNode child = children.get(i);
                    if (child.isFolder()) {
                        buildFileStructure(dlList, parent, api, child);
                    } else {
                        dlList.put(child.getHandle(), parent);
                    }
                }
            }
        }
    }

    private DocumentFile getDocumentFileByPath(String parentPath) {
        DocumentFile root = sdCardRootDocument;
        for (String folder : getSubFolders(targetRoot, parentPath)) {
            if (root.findFile(folder) == null) {
                root = root.createDirectory(folder);
            } else {
                root = root.findFile(folder);
            }
        }
        return root;
    }

    public DocumentFile createFolder(String path, String name) {
        DocumentFile parent = getDocumentFileByPath(path);
        DocumentFile folder = parent.findFile(name);
        if (folder != null) {
            return folder;
        }
        return parent.createDirectory(name);
    }

    /**
     * Checks if the file already exists in targetPath.
     *
     * @param file       File to check.
     * @param targetPath Path where the file is checked for.
     */
    public boolean fileExistsInTargetPath(File file, String targetPath) {
        DocumentFile parent = getDocumentFileByPath(targetPath);
        if (parent == null) {
            return false;
        }

        DocumentFile destFile = parent.findFile(file.getName());

        return destFile != null && destFile.length() == file.length();
    }

    /**
     * Moves a file from its location to targetPath.
     *
     * @param targetPath Path where the file has to be moved.
     * @param file       File to move.
     * @throws IOException If some error happens opening output stream.
     */
    public void moveFile(String targetPath, File file) throws IOException {
        String name = file.getName();
        DocumentFile parent = getDocumentFileByPath(targetPath);
        DocumentFile df = parent.findFile(name);

        //Already exists
        if (df != null && df.length() == file.length()) {
            Timber.d("%s already exists.", name);
            return;
        }

        //Update
        if (df != null && df.length() != file.length()) {
            Timber.d("delete former file.");
            df.delete();
        }

        DocumentFile targetFile = parent.createFile(MimeTypeList.typeForName(name).getType(), name);
        if (targetFile == null) {
            throw new IOException("Create file on SD card failed.");
        }

        Uri uri = targetFile.getUri();
        OutputStream os = null;
        InputStream is = null;

        try {
            os = context.getContentResolver().openOutputStream(uri, "rwt");
            if (os == null) {
                throw new IOException("Open output stream exception!");
            }

            is = new FileInputStream(file);

            copyStream(is, os);
        } finally {
            if (is != null) {
                is.close();
            }

            if (os != null) {
                os.close();
            }
        }
    }

    /**
     * Copy an Uri to targetPath.
     *
     * @param targetPath Path where the file has to be moved.
     * @param source     Uri to copy.
     * @param name       File name of the Uri.
     * @throws IOException If some error happens opening output stream.
     */
    public void copyUri(String targetPath, Uri source, String name) throws IOException {
        DocumentFile parent = getDocumentFileByPath(targetPath);
        DocumentFile df = parent.findFile(name);

        if (df != null) {
            Timber.d("delete former file.");
            df.delete();
        }

        DocumentFile targetFile = parent.createFile(MimeTypeList.typeForName(name).getType(), name);
        if (targetFile == null) {
            throw new IOException("Create file on SD card failed.");
        }

        Uri uri = targetFile.getUri();
        OutputStream os = null;
        InputStream is = null;

        try {
            os = context.getContentResolver().openOutputStream(uri, "rwt");
            if (os == null) {
                throw new IOException("Open output stream exception!");
            }

            is = context.getContentResolver().openInputStream(source);

            copyStream(is, os);
        } finally {
            if (is != null) {
                is.close();
            }

            if (os != null) {
                os.close();
            }
        }
    }

    private void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[COPY_FILE_BUFFER_SIZE];
        int length;

        while ((length = is.read(buffer)) != -1) {
            os.write(buffer, 0, length);
            os.flush();
        }
    }

    private List<String> getSubFolders(String root, String parent) {
        if (parent == null) {
            throw new IllegalArgumentException("parent is null");
        }

        if (root == null) {
            throw new IllegalArgumentException("root is null");
        }

        if (parent.length() < root.length()) {
            throw new IllegalArgumentException("no subfolders!");
        }
        if (!parent.contains(root)) {
            throw new IllegalArgumentException("parent is not a subfolder of root!");
        }
        List<String> folders = new ArrayList<>();
        for (String s : parent.substring(root.length()).split(File.separator)) {
            if (!"".equals(s)) {
                folders.add(s);
            }
        }
        return folders;
    }

    public boolean isSDCardDownload() {
        return isSDCardDownload;
    }

    private void setSDCardDownload(boolean SDCardDownload) {
        isSDCardDownload = SDCardDownload;
    }

    /**
     * Checks if the download path belongs to an SD card.
     *
     * @param context      current Context
     * @param downloadPath download path
     * @return The new SDCardOperator to continue its initialization.
     */
    private static SDCardOperator checkDownloadPath(Context context, String downloadPath) {
        DatabaseHandler dbH = MegaApplication.getInstance().getDbH();
        boolean isSDCardPath = isSDCardPath(downloadPath);
        SDCardOperator sdCardOperator = null;

        try {
            sdCardOperator = new SDCardOperator(context);
        } catch (SDCardOperator.SDCardException e) {
            Timber.e(e, "Initialize SDCardOperator failed");
            // user uninstall the sd card. but default download location is still on the sd card
            if (isSDCardPath) {
                Timber.d("select new path as download location.");
                new Handler().postDelayed(() -> Toast.makeText(MegaApplication.getInstance(), R.string.old_sdcard_unavailable, Toast.LENGTH_LONG).show(), 1000);
                return null;
            }
        }

        if (sdCardOperator != null && isSDCardPath) {
            //user has installed another sd card.
            if (sdCardOperator.isNewSDCardPath(downloadPath)) {
                Timber.d("new sd card, check permission.");
                new Handler().postDelayed(() -> Toast.makeText(MegaApplication.getInstance(), R.string.old_sdcard_unavailable, Toast.LENGTH_LONG).show(), 1000);
                return null;
            }

            try {
                sdCardOperator.initDocumentFileRoot(dbH.getSdCardUri());
            } catch (SDCardOperator.SDCardException e) {
                Timber.e(e, "SDCardOperator initDocumentFileRoot failed requestSDCardPermission");
                return null;
            }
        }

        return sdCardOperator;
    }

    /**
     * Inits the SDCardOperator.
     *
     * @param context    current Context
     * @param parentPath SD card parent path
     * @return The initialized SDCardOperator.
     */
    public static SDCardOperator initSDCardOperator(Context context, String parentPath) {
        SDCardOperator sdCardOperator;

        if (SDCardOperator.isSDCardPath(parentPath)) {
            sdCardOperator = checkDownloadPath(context, parentPath);
            if (sdCardOperator != null) {
                sdCardOperator.setSDCardDownload(!isTextEmpty(sdCardOperator.getDownloadRoot()));
            }
        } else {
            sdCardOperator = new SDCardOperator(false);
        }

        return sdCardOperator;
    }

    /**
     * Moves an SD card file download to the targetPath.
     *
     * @param downloadedFile Downloaded file to move.
     * @param targetPath     Path where the file has to be moved.
     * @param uri            Uri to init SD card operator.
     */
    public boolean moveDownloadedFileToDestinationPath(File downloadedFile, String targetPath, String uri) {
        if (!downloadedFile.exists()) {
            Timber.e("Download file doesn't exist!");
            return false;
        }

        if (TextUtil.isTextEmpty(targetPath)) {
            Timber.e("Target path is empty!");
            return false;
        }

        MegaApplication app = MegaApplication.getInstance();
        try {
            initDocumentFileRoot(uri != null ? uri : app.getDbH().getSdCardUri());

            moveFile(targetPath, downloadedFile);

            //New path, after moving to target location.
            File newFile = new File(targetPath + File.separator + downloadedFile.getName());
            if (!newFile.exists() || newFile.length() != downloadedFile.length()) {
                Timber.e("Error moving file to the sd card path");
            } else {
                return true;
            }
        } catch (Exception e) {
            Timber.e(e, "Error moving file to the sd card path");
        } finally {
            downloadedFile.delete();
        }
        return false;
    }
}
