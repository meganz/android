package mega.privacy.android.app.utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLConnection;
import java.nio.channels.FileChannel;

import mega.privacy.android.app.MimeTypeList;

import static mega.privacy.android.app.utils.CacheFolderManager.temporalPicDIR;

public class FileUtils {

    public static void cleanDir(File dir) {
        File[] files = dir.listFiles();

        if (files != null) {
            for (File file : files) {

                if (file.isFile()) {
                    file.delete();
                } else {
                    cleanDir(file);
                    file.delete();
                }
            }
        } else {
            log("Files is NULL");
        }
    }

    public static void deleteFolderAndSubfolders(Context context, File f) throws IOException {

        if (f == null) return;

        log("deleteFolderAndSubfolders: " + f.getAbsolutePath());
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    contentUri = FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", fileToDelete);
                } else {
                    contentUri = Uri.fromFile(fileToDelete);
                }
                mediaScanIntent.setData(contentUri);
                mediaScanIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.sendBroadcast(mediaScanIntent);
            } catch (Exception e) {
                log("Exception while deleting media scanner file: " + e.getMessage());
            }

        }
    }

    public static File createTemporalTextFile(String name, String data){

        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + temporalPicDIR + "/";
        File tempDownDirectory = new File(path);
        if(!tempDownDirectory.exists()){
            tempDownDirectory.mkdirs();
        }

        String fileName = name+".txt";
        final File file = new File(path, fileName);

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
            log("File write failed: " + e.toString());
            return null;
        }
    }

    public static File createTemporalURLFile(String name, String data){

        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + temporalPicDIR + "/";
        File tempDownDirectory = new File(path);
        if(!tempDownDirectory.exists()){
            tempDownDirectory.mkdirs();
        }

        String fileName = name+".url";
        final File file = new File(path, fileName);

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
            log("File write failed: " + e.toString());
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
        log("Files is NULL");
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

    public static void copyFile(File source, File dest) throws IOException{
        log("copyFile");

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
        log("isVideoFile: "+path);
        try{
            String mimeType = URLConnection.guessContentTypeFromName(path);
            return mimeType != null && mimeType.indexOf("video") == 0;
        }
        catch(Exception e){
            log("Exception: "+e.getMessage());
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
        else{
            return false;
        }
    }

    public static void log(String message) {
        Util.log("FileUtils", message);
    }
}
