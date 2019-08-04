package mega.privacy.android.app.utils;


import android.content.Context;
import android.net.Uri;
import android.support.v4.provider.DocumentFile;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaPreferences;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;

public class SDCardOperator {

    private Context context;

    private String downloadRoot;

    private String sdCardRoot;

    private DocumentFile sdCardRootDocument;

    private DatabaseHandler dbh;

    //32kb
    private static final int BUFFER_SIZE = 32 * 1024;

    public static class SDCardException extends Exception {

        public SDCardException(String message) {
            super(message);
        }
    }

    public SDCardOperator(Context context) throws SDCardException {
        this.context = context;
        //package cache folder on sd card.
        File[] fs = context.getExternalCacheDirs();
        if (fs.length > 1 && fs[1] != null) {
            downloadRoot = fs[1].getAbsolutePath();
        } else {
            throw new SDCardException("No sd card installed!");
        }
        sdCardRoot = Util.getSDCardRoot(downloadRoot);
        dbh = DatabaseHandler.getDbHandler(context);

        MegaPreferences prefs = dbh.getPreferences();
        if (prefs != null) {
            String uriString = prefs.getUriExternalSDCard();
            if (TextUtils.isEmpty(uriString)) {
                throw new SDCardException("Permission required! No uri string.");
            } else {
                Uri rootUri = Uri.parse(uriString);
                sdCardRootDocument = DocumentFile.fromTreeUri(context, rootUri);
                if (!sdCardRootDocument.canWrite()) {
                    throw new SDCardException("Permission required!");
                }
            }
        } else {
            throw new SDCardException("Preferences has not been initialized!");
        }
    }

    public String getDownloadRoot() {
        return downloadRoot;
    }

    public String getSDCardRoot() {
        return sdCardRoot;
    }

    public boolean isSDCardPath(String path) {
        return path.contains(sdCardRoot);
    }

    public boolean canWriteWithFile() {
        return new File(sdCardRoot).canWrite();
    }

    public String move(String targetPath, File source) throws IOException {
        moveFile(targetPath, source);
        return targetPath + File.separator + source.getName();
    }

    public void buildFileStructure(Map<MegaNode, String> dlList, String parent, MegaApiJava api, MegaNode node) {
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
                        dlList.put(child, parent);
                    }
                }
            }
        }
    }

    public String getUriString() {
        String uri = dbh.getPreferences().getUriExternalSDCard();
        if (!TextUtils.isEmpty(uri)) {
            DocumentFile root = DocumentFile.fromTreeUri(context, Uri.parse(uri));
            if (root.canWrite()) {
                return uri;
            }
        }
        return null;
    }

    private DocumentFile getDocumentFileByPath(String parentPath) {
        DocumentFile root = sdCardRootDocument;
        for (String folder : getSubFolders(sdCardRoot, parentPath)) {
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

    public void moveFile(String targetPath, File file) throws IOException {
        String name = file.getName();
        DocumentFile parent = getDocumentFileByPath(targetPath);
        DocumentFile df = parent.findFile(name);
        //alreay exists
        if (df != null && df.length() == file.length()) {
            log(name + " already exists.");
            return;
        }
        //update
        if (df != null && df.length() != file.length()) {
            log("delete former file.");
            df.delete();
        }
        Uri uri = parent.createFile(null, name).getUri();
        OutputStream os = null;
        InputStream is = null;
        try {
            os = context.getContentResolver().openOutputStream(uri, "rwt");
            if (os == null) {
                throw new IOException("open output stream exception!");
            }
            is = new FileInputStream(file);
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(buffer)) != -1) {
                os.write(buffer, 0, length);
                os.flush();
            }
        } finally {
            if (is != null) {
                is.close();
            }
            if (os != null) {
                os.close();
            }
        }
    }

    private List<String> getSubFolders(String root, String parent) {
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

    public static void log(String log) {
        Util.log("SDCardOperator", log);
    }
}
