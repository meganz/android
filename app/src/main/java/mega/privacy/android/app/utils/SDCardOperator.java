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

import mega.privacy.android.app.DatabaseHandler;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaNode;

public class SDCardOperator {

    private Context context;

    private String downloadRoot;

    private String sdCardRoot;

    private DocumentFile targetRoot;

    private DatabaseHandler dbh;

    private Uri rootUri;

    //32kb
    private static final int BUFFER_SIZE = 32 * 1024;

    public static class SDCardException extends Exception {

        public SDCardException(String message) {
            super(message);
        }
    }

    public SDCardOperator(Context context) throws SDCardException {
        this.context = context;

        File[] fs = context.getExternalCacheDirs();
        if (fs.length > 1 && fs[1] != null) {
            downloadRoot = fs[1].getAbsolutePath();
        } else {
            throw new SDCardException("SDCardOperator initialization exception!");
        }
        dbh = DatabaseHandler.getDbHandler(context);
        sdCardRoot = Util.getSDCardRoot(downloadRoot);
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

    public boolean canWriteWithDocumentFile() {
        return false;
    }

    public void move(DocumentFile target, File source) throws IOException {
        String parent = source.getParent();
        if (downloadRoot.equals(parent)) {
            moveFile(target, source);
        } else {
            for (String folder : getSubFolders(downloadRoot, parent)) {
                target = createFolder(target, folder);
            }
            moveFile(target, source);
        }
    }

    public void buildFileStructure(DocumentFile parent, MegaApiJava api, MegaNode node) {
        TL.log("buildFileStructure", "@#@", node.getName());
        if (node.isFolder()) {
            parent = createFolder(parent, node.getName());
            ArrayList<MegaNode> children = api.getChildren(node);
            if (children != null) {
                for (int i = 0; i < children.size(); i++) {
                    buildFileStructure(parent, api, children.get(i));
                }
            }
        }
    }

    public String getUriString() {
        String uri = dbh.getPreferences().getUriExternalSDCard();
        if(!TextUtils.isEmpty(uri)) {
            DocumentFile root = DocumentFile.fromTreeUri(context, Uri.parse(uri));
            if(root.canWrite()) {
                return uri;
            }
        }
        return null;
    }

    public DocumentFile getParent(String rootUriString, String parentPath) {
        DocumentFile root = DocumentFile.fromTreeUri(context, Uri.parse(rootUriString));
        for (String folder : getSubFolders(sdCardRoot,parentPath)) {
            root = root.findFile(folder);
        }
        return root;
    }


    public DocumentFile createFolder(DocumentFile parent, String name) {
        DocumentFile folder = parent.findFile(name);
        if (folder != null) {
            return folder;
        }
        return parent.createDirectory(name);
    }

    public void moveFile(DocumentFile parent, File file) throws IOException {
        String name = file.getName();
        DocumentFile df = parent.findFile(name);
        if (df != null && df.length() > 0) {
            log(name + " already exists.");
            return;
        }
        if (df != null && df.length() == 0) {
            log("delete temp file.");
            df.delete();
        }
        Uri uri = parent.createFile(null, name).getUri();
        OutputStream os = null;
        InputStream is = null;
        try {
            os = context.getContentResolver().openOutputStream(uri, "rwt");
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

    public List<String> getSubFolders(String root, String parent) {
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
