package mega.privacy.android.app;

import static mega.privacy.android.app.utils.FileUtil.addPdfFileExtension;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.main.FileExplorerActivity;
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity;
import mega.privacy.android.domain.entity.chat.FileGalleryItem;
import timber.log.Timber;


/*
 * Helper class to process shared files from other activities
 */
public class ShareInfo implements Serializable {

    private static final String APP_PRIVATE_DIR1 = "/data/data/mega.privacy.android.app";
    private static final String APP_PRIVATE_DIR2 = "/data/user/0/mega.privacy.android.app";

    public String title = null;
    private long lastModified;
    public transient InputStream inputStream = null;
    public long size = -1;
    private File file = null;
    public boolean isContact = false;
    public Uri contactUri = null;

    private static Intent mIntent;

    /*
     * Get ShareInfo from File
     */
    public static ShareInfo infoFromFile(File file) {
        ShareInfo info = new ShareInfo();
        info.file = file;
        info.lastModified = file.lastModified();
        info.size = file.length();
        info.title = file.getName();
        try {
            info.inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            return null;
        }
        return info;
    }

    public String getOriginalFileName() {
        return file.getName();
    }

    public String getFileAbsolutePath() {
        return file.getAbsolutePath();
    }

    public String getTitle() {
        return title;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public long getSize() {
        return size;
    }

    public long getLastModified() {
        return lastModified;
    }

    /*
     * Process incoming Intent and get list of ShareInfo objects
     */
    public static List<ShareInfo> processIntent(Intent intent, Context context) {
        Timber.d("%s of action", intent.getAction());

        if (intent.getAction() == null || intent.getAction().equals(FileExplorerActivity.ACTION_PROCESSED) || intent.getAction().equals(FileExplorerActivity.ACTION_PROCESSED)) {
            return null;
        }
        if (context == null) {
            return null;
        }

        mIntent = intent;

        // Process multiple items
        if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            Timber.d("Multiple!");
            return processIntentMultiple(intent, context);
        }
        ShareInfo shareInfo = new ShareInfo();

        Bundle extras = intent.getExtras();
        // File data in EXTRA_STREAM
        if (extras != null && extras.containsKey(Intent.EXTRA_STREAM)) {
            Timber.d("Extras is not null");
            Object streamObject = extras.get(Intent.EXTRA_STREAM);
            if (streamObject instanceof Uri) {
                Timber.d("Instance of URI");
                Timber.d(streamObject.toString());
                shareInfo.processUri((Uri) streamObject, context);
            } else if (streamObject == null) {
                Timber.d("Stream object is null!");
                return null;
            } else {
                Timber.d("Unhandled type %s", streamObject.getClass().getName());
                for (String key : extras.keySet()) {
                    Timber.d("Key %s", key);
                }
                return processIntentMultiple(intent, context);
            }
        } else if (intent.getClipData() != null) {
            if (Intent.ACTION_GET_CONTENT.equals(intent.getAction()) || Intent.ACTION_OPEN_DOCUMENT.equals(intent.getAction())) {
                Timber.d("Multiple ACTION");
                return processGetContentMultiple(intent, context);
            }
        }
        // Get File info from Data URI
        else {
            Uri dataUri = intent.getData();
            if (dataUri == null) {
                Timber.w("Data uri is null");

                return null;
            }

            if (isPathInsecure(dataUri.getPath())) {
                Timber.w("Data uri is insecure");
                return null;
            }

            shareInfo.processUri(dataUri, context);
        }
        if (shareInfo.file == null) {
            Timber.w("Share info file is null");
            return null;
        }
        intent.setAction(FileExplorerActivity.ACTION_PROCESSED);

        ArrayList<ShareInfo> result = new ArrayList<ShareInfo>();
        result.add(shareInfo);
        return result;
    }

    /**
     * Process files to upload to chat from gallery
     *
     * @param context Context
     * @param files   List of FileGalleryItem
     * @return List of ShareInfo
     */
    public static List<ShareInfo> processUploadFile(Context context, ArrayList<FileGalleryItem> files) {
        List<ShareInfo> result = new ArrayList<>();

        if (files.isEmpty())
            return result;

        // Get File info from Data URI
        for (int i = 0; i < files.size(); i++) {
            ShareInfo shareInfo = new ShareInfo();
            FileGalleryItem file = files.get(i);
            Uri dataUri = Uri.parse(file.getFileUri());
            if (dataUri == null) {
                Timber.w("Data uri is null");
                continue;
            }

            if (isPathInsecure(dataUri.getPath())) {
                Timber.w("Data uri is insecure");
                continue;
            }

            shareInfo.processUri(dataUri, context);
            result.add(shareInfo);
        }

        return result;
    }

    private static boolean isPathInsecure(String path) {
        return path.contains("../") || path.contains(APP_PRIVATE_DIR1)
                || path.contains(APP_PRIVATE_DIR2);
    }

    /*
     * Process Multiple files from GET_CONTENT Intent
     */
    @SuppressLint("NewApi")
    public static List<ShareInfo> processGetContentMultiple(Intent intent, Context context) {
        Timber.d("processGetContentMultiple");
        ArrayList<ShareInfo> result = new ArrayList<>();
        ClipData cD = intent.getClipData();

        if (cD != null && cD.getItemCount() != 0) {
            for (int i = 0; i < cD.getItemCount(); i++) {
                ClipData.Item item = cD.getItemAt(i);
                Uri uri = item.getUri();
                Timber.d("ClipData uri: %s", uri);
                if (uri == null)
                    continue;
                Timber.d("Uri: %s", uri.toString());
                ShareInfo info = new ShareInfo();
                info.processUri(uri, context);
                if (info.file == null) {
                    continue;
                }
                result.add(info);
            }
        } else {
            Timber.w("ClipData or uploadResults NUll or size=0");
            return null;
        }

        intent.setAction(FileExplorerActivity.ACTION_PROCESSED);

        return result;
    }


    /*
     * Process Multiple files
     */
    public static List<ShareInfo> processIntentMultiple(Intent intent, Context context) {
        Timber.d("processIntentMultiple");
        ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

        ArrayList<Uri> imageUri = intent.getParcelableArrayListExtra(Intent.EXTRA_ALLOW_MULTIPLE);

        if (imageUris == null || imageUris.size() == 0) {
            Timber.w("imageUris == null || imageUris.size() == 0");
            return null;
        }
        ArrayList<ShareInfo> result = new ArrayList<ShareInfo>();
        for (Uri uri : imageUris) {
            if (uri == null) {
                Timber.w("continue --> uri null");
                continue;
            }
            Timber.d("Uri: %s", uri.toString());
            ShareInfo info = new ShareInfo();
            info.processUri(uri, context);
            if (info.file == null) {
                Timber.w("continue -->info.file null");
                continue;
            }
            result.add(info);
        }

        intent.setAction(FileExplorerActivity.ACTION_PROCESSED);

        return result;
    }

    /*
     * Get info from Uri
     */
    public void processUri(Uri uri, Context context) {
        Timber.d("processUri: %s", uri);
        // getting input stream
        inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
        } catch (FileNotFoundException fileNotFound) {
            Timber.e(fileNotFound, "Can't find uri: %s", uri);
            return;
        } catch (Exception e) {
            Timber.e(e, "inputStream EXCEPTION!");
            String path = uri.getPath();
            Timber.d("Process Uri path in the exception: %s", path);
        }

        String scheme = uri.getScheme();
        if (scheme != null) {
            if (scheme.equals("content")) {
                Timber.d("processUri go to scheme content");
                processContent(uri, context);
            } else if (scheme.equals("file")) {
                Timber.d("processUri go to file content");
                processFile(uri, context);
            }
        } else {
            Timber.w("Scheme NULL");
        }

        if (inputStream != null) {
            Timber.d("processUri inputStream != null");

            file = null;
            String path = uri.getPath();
            try {
                file = new File(path);
            } catch (Exception e) {
                Timber.e(e, "Error when creating File!");
            }

            if ((file != null) && file.exists() && file.canRead()) {
                size = file.length();
                Timber.d("The file is accesible!");
                return;
            }

            file = null;
            path = getRealPathFromURI(context, uri);
            if (path != null) {
                Timber.d("RealPath: %s", path);
                try {
                    file = new File(path);
                } catch (Exception e) {
                    Timber.e(e, "EXCEPTION: No real path from URI");
                }
            } else {
                Timber.w("Real path is NULL");
            }

            if ((file != null) && file.exists() && file.canRead()) {
                size = file.length();
                Timber.d("Return here");
                return;
            }

            if (title == null) {
                Timber.w("Title is null, return!");
                return;
            }
            if (title.contains("../") || title.contains(("..%2F"))) {
                Timber.d("Internal path traversal: %s", title);
                return;
            }
            Timber.d("Internal No path traversal: %s", title);
            if (context instanceof PdfViewerActivity
                    || (mIntent != null && mIntent.getType() != null && mIntent.getType().equals("application/pdf"))) {
                title = addPdfFileExtension(title);
            }
            file = new File(context.getCacheDir(), title);
            Timber.d("Start copy to: %s", file.getAbsolutePath());

            try {
                OutputStream stream = new BufferedOutputStream(new FileOutputStream(file));
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len = 0;
                while ((len = inputStream.read(buffer)) != -1) {
                    stream.write(buffer, 0, len);
                }
                if (stream != null) {
                    stream.close();
                }

                inputStream = new FileInputStream(file);
                size = file.length();
                Timber.d("File size: %s", size);
            } catch (IOException e) {
                Timber.e(e, "Catch IO exception");
                inputStream = null;
                if (file != null) {
                    file.delete();
                }
            }
        } else {
            Timber.d("inputStream is NULL");
            String path = uri.getPath();
            Timber.d("PATH: %s", path);
            if (path != null) {
                String[] s = path.split("file://");
                if (s.length > 1) {
                    String p = s[1];
                    String[] s1 = p.split("/ORIGINAL");
                    if (s1.length > 1) {
                        path = s1[0];
                        try {
                            path = URLDecoder.decode(path, "UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            path.replaceAll("%20", " ");
                        }
                    }
                }
            }
            Timber.d("REAL PATH: %s", path);

            file = null;
            try {
                file = new File(path);
            } catch (Exception e) {
                Timber.e(e, "Error when creating File!");
            }
            if ((file != null) && file.exists() && file.canRead()) {
                size = file.length();
                Timber.d("The file is accesible!");
                return;
            } else {
                Timber.w("The file is not accesible!");
                isContact = true;
                contactUri = uri;
            }
        }
        Timber.d("END processUri");
    }

    /*
     * Get info from content provider
     */
    private void processContent(Uri uri, Context context) {
        Timber.d("processContent: %s", uri);

        ContentProviderClient client = null;
        Cursor cursor = null;
        try {
            client = context.getContentResolver().acquireContentProviderClient(uri);
            if (client != null) {
                cursor = client.query(uri, null, null, null, null);
            }
        } catch (Exception e) {
            Timber.e(e, "client or cursor EXCEPTION: ");
        }

        if (cursor == null || cursor.getCount() == 0) {
            Timber.w("Error with cursor");
            if (cursor != null) {
                cursor.close();
            }
            if (client != null) {
                client.close();
            }
            return;
        }

        cursor.moveToFirst();
        int displayIndex = cursor.getColumnIndex("_display_name");
        if (displayIndex != -1)
            title = cursor.getString(displayIndex);
        int sizeIndex = cursor.getColumnIndex("_size");
        if (sizeIndex != -1) {
            String sizeString = cursor.getString(sizeIndex);
            if (sizeString != null) {
                long size = Long.valueOf(sizeString);
                if (size > 0) {
                    Timber.d("Size: %s", size);
                    this.size = size;
                }
            }
        }
        int lastModifiedIndex = cursor.getColumnIndex("last_modified");
        if (lastModifiedIndex != -1) {
            this.lastModified = cursor.getLong(lastModifiedIndex);
        }

        if (size == -1 || inputStream == null) {
            Timber.d("Keep going");
            int dataIndex = cursor.getColumnIndex("_data");
            if (dataIndex != -1) {
                String data = cursor.getString(dataIndex);
                if (data == null) {
                    Timber.w("RETURN - data is NULL");
                    return;
                }
                File dataFile = new File(data);
                if (dataFile.exists() && dataFile.canRead()) {
                    if (size == -1) {
                        long size = dataFile.length();
                        if (size > 0) {
                            Timber.d("Size is: %s", size);
                            this.size = size;
                        }
                    } else {
                        Timber.w("Not valid size");
                    }

                    if (inputStream == null) {
                        try {
                            inputStream = new FileInputStream(dataFile);
                        } catch (FileNotFoundException e) {
                            Timber.e(e, "Exception");
                        }

                    } else {
                        Timber.w("inputStream is NULL");
                    }
                }
            }
        } else {
            Timber.w("Nothing done!");
        }

        cursor.close();
        client.close();

        Timber.d("---- END process content----");
    }

    /*
     * Process Uri as File path
     */
    private void processFile(Uri uri, Context context) {
        Timber.d("processing file");
        File file = null;
        try {
            file = new File(new URI(uri.toString()));
        } catch (URISyntaxException e1) {
            file = new File(uri.toString().replace("file:///", "/"));
        }
        if (!file.exists() || !file.canRead()) {
            Timber.w("Can't read file. exists: %s, canRead: %s, uri: %s", file.exists(), file.canRead(), uri.toString());
            return;
        }
        if (file.isDirectory()) {
            Timber.d("Is folder");
            return;
        }
        Timber.d("Continue processing...");
        size = file.length();
        title = file.getName();
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
        }
        Timber.d("%s %d", title, size);
    }

    private String getRealPathFromURI(Context context, Uri contentURI) {
        if (contentURI == null) return null;
        String path = null;
        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(contentURI, null, null, null, null);
            if (cursor == null) return null;
            if (cursor.getCount() == 0) {
                cursor.close();
                return null;
            }

            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            if (idx == -1) {
                cursor.close();
                return null;
            }

            try {
                path = cursor.getString(idx);
            } catch (Exception ex) {
                cursor.close();
                return null;
            }
        } catch (Exception e) {
            if (cursor != null)
                cursor.close();
            return null;
        }

        if (cursor != null)
            cursor.close();
        return path;
    }
}
