package mega.privacy.android.app.lollipop.controllers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.lollipop.LoginActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.managerSections.MyAccountFragmentLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaUser;

public class AccountController {

    Context context;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH;
    MegaPreferences prefs = null;

    static int count = 0;

    public AccountController(Context context){
        log("AccountController created");
        this.context = context;

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (dbH == null){
            dbH = DatabaseHandler.getDbHandler(context);
        }
    }

    public AccountController(Context context, MegaApiAndroid megaApi){
        this.context = context;
        this.megaApi = megaApi;
    }

    public void resetPass(String myEmail){
        megaApi.resetPassword(myEmail, true, (ManagerActivityLollipop)context);
    }

    public void deleteAccount(){
        log("deleteAccount");
        megaApi.cancelAccount((ManagerActivityLollipop)context);
    }

    public void confirmDeleteAccount(String link, String pass){
        log("confirmDeleteAccount");
        megaApi.confirmCancelAccount(link, pass, (ManagerActivityLollipop)context);
    }

    public void confirmChangeMail(String link, String pass){
        log("confirmChangeMail");
        megaApi.confirmChangeEmail(link, pass, (ManagerActivityLollipop)context);
    }

    public void takeProfilePicture(){
        log("takePicture");

        String path = Environment.getExternalStorageDirectory().getAbsolutePath() +"/"+ Util.profilePicDIR;
        File newFolder = new File(path);
        newFolder.mkdirs();

        String file = path + "/picture.jpg";
        File newFile = new File(file);
        try {
            newFile.createNewFile();
        } catch (IOException e) {}

        Uri outputFileUri = FileProvider.getUriForFile(context, "mega.privacy.android.app.providers.fileprovider", newFile);

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
        cameraIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        ((ManagerActivityLollipop)context).startActivityForResult(cameraIntent, Constants.TAKE_PICTURE_PROFILE_CODE);
    }

    public boolean existsAvatar(){
        MegaUser myContact = ((ManagerActivityLollipop)context).getMyAccountInfo().getMyUser();
        String myEmail = myContact.getEmail();
        if(myEmail!=null){
            File avatar = null;
            if (context.getExternalCacheDir() != null){
                avatar = new File(context.getExternalCacheDir().getAbsolutePath(), myEmail + ".jpg");
            }
            else{
                avatar = new File(context.getCacheDir().getAbsolutePath(), myEmail + ".jpg");
            }

            if (avatar.exists()) {
                log("avatar exists in: " + avatar.getAbsolutePath());
                return true;
            }
        }
        return false;
    }

    public void removeAvatar(){
        log("removeAvatar");

        MegaUser myContact = ((ManagerActivityLollipop)context).getMyAccountInfo().getMyUser();
        String myEmail = myContact.getEmail();
        if(myEmail!=null){
            File avatar = null;
            if (context.getExternalCacheDir() != null){
                avatar = new File(context.getExternalCacheDir().getAbsolutePath(), myEmail + ".jpg");
            }
            else{
                avatar = new File(context.getCacheDir().getAbsolutePath(), myEmail + ".jpg");
            }

            if (avatar.exists()) {
                log("avatar to delete: " + avatar.getAbsolutePath());
                avatar.delete();
            }
        }

        megaApi.setAvatar(null, (ManagerActivityLollipop)context);
    }

    public void exportMK(){
        log("exportMK");
        if (!Util.isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            return;
        }

        String key = megaApi.exportMasterKey();

        BufferedWriter out;
        try {

            final String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
            log("Export in: "+path);
            FileWriter fileWriter= new FileWriter(path);
            out = new BufferedWriter(fileWriter);
            out.write(key);
            out.close();
            String message = context.getString(R.string.toast_master_key, path);
            ((ManagerActivityLollipop) context).invalidateOptionsMenu();
            MyAccountFragmentLollipop mAF = ((ManagerActivityLollipop) context).getMyAccountFragment();
            if(mAF!=null){
                mAF.updateMKButton();
            }
            Util.showAlert(((ManagerActivityLollipop) context), message, null);

        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

//    public void updateMK(){
//        log("updateMK");
//
//        String key = megaApi.exportMasterKey();
//        BufferedWriter out;
//        try {
//
//            final String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
//            log("Export in: "+path);
//            FileWriter fileWriter= new FileWriter(path);
//            out = new BufferedWriter(fileWriter);
//            if(out==null){
//                log("Error!!!");
//                return;
//            }
//            out.write(key);
//            out.close();
//
//        }catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public void renameMK(){
        log("renameMK");

        final String oldPath = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.oldMKFile;
        File oldMKFile = new File(oldPath);

        final String newPath = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
        File newMKFile = new File(newPath);

        oldMKFile.renameTo(newMKFile);
    }

    public void copyMK(){
        log("copyMK");
        String key = megaApi.exportMasterKey();
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", key);
        clipboard.setPrimaryClip(clip);
        Util.showAlert(((ManagerActivityLollipop) context), context.getString(R.string.copy_MK_confirmation), null);
    }

    public void removeMK() {
        log("removeMK");
        final String path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
        final File f = new File(path);
        f.delete();

        //Check if old MK file exists
        final String pathOldMK = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.oldMKFile;
        final File fOldMK = new File(pathOldMK);
        if(fOldMK.exists()){
            log("The old file of MK was also removed");
            f.delete();
        }

        String message = context.getString(R.string.toast_master_key_removed);
        ((ManagerActivityLollipop) context).invalidateOptionsMenu();
        MyAccountFragmentLollipop mAF = ((ManagerActivityLollipop) context).getMyAccountFragment();
        if(mAF!=null){
            mAF.updateMKButton();
        }
        Util.showAlert(((ManagerActivityLollipop) context), message, null);
    }

    public void killAllSessions(Context context){
        log("killAllSessions");
        megaApi.killSession(-1, (ManagerActivityLollipop) context);
    }

    static public void logout(Context context, MegaApiAndroid megaApi, MegaChatApiAndroid megaChatApi, boolean confirmAccount) {
        log("logout");
        logout(context, megaApi, megaChatApi, confirmAccount, false);
    }

    static public void logout(Context context, MegaApiAndroid megaApi, boolean confirmAccount) {
        log("logout");
        logout(context, megaApi, null, confirmAccount, false);
    }


    static public void logout(Context context, MegaApiAndroid megaApi, MegaChatApiAndroid megaChatApi, boolean confirmAccount, boolean logoutBadSession) {
        log("logout");

        boolean isManagerActivityLollipop = false;

        if (megaApi == null){
            if (context instanceof Activity){
                megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
            }
        }

        if (megaChatApi == null){
            if (context instanceof Activity){
                megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
            }
        }

        if (!logoutBadSession){
            if (context instanceof ManagerActivityLollipop){
                isManagerActivityLollipop = true;
                megaApi.logout((ManagerActivityLollipop)context);
            }
            else{
                isManagerActivityLollipop = false;
                megaApi.logout();
            }
        }

        File offlineDirectory = null;
        if (Environment.getExternalStorageDirectory() != null){
            offlineDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Util.offlineDIR);
        }
        else{
            offlineDirectory = context.getFilesDir();
        }

        try {
            Util.deleteFolderAndSubfolders(context, offlineDirectory);
        } catch (IOException e) {}

        File thumbDir = ThumbnailUtils.getThumbFolder(context);
        File previewDir = PreviewUtils.getPreviewFolder(context);

        try {
            Util.deleteFolderAndSubfolders(context, thumbDir);
        } catch (IOException e) {}

        try {
            Util.deleteFolderAndSubfolders(context, previewDir);
        } catch (IOException e) {}

        File externalCacheDir = context.getExternalCacheDir();
        File cacheDir = context.getCacheDir();
        try {
            Util.deleteFolderAndSubfolders(context, externalCacheDir);
        } catch (IOException e) {}

        try {
            Util.deleteFolderAndSubfolders(context, cacheDir);
        } catch (IOException e) {}

        final String pathOldMK = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.oldMKFile;
        final File fMKOld = new File(pathOldMK);
        if (fMKOld.exists()){
            log("Old MK file removed!");
            fMKOld.delete();
        }

        final String pathMK = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
        final File fMK = new File(pathMK);
        if (fMK.exists()){
            log("MK file removed!");
            fMK.delete();
        }

        PackageManager m = context.getPackageManager();
        String s = context.getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            log("Error Package name not found " + e);
        }

        File appDir = new File(s);

        for (File c : appDir.listFiles()){
            if (c.isFile()){
                c.delete();
            }
        }

        Intent cancelTransfersIntent = new Intent(context, DownloadService.class);
        cancelTransfersIntent.setAction(DownloadService.ACTION_CANCEL);
        context.startService(cancelTransfersIntent);
        cancelTransfersIntent = new Intent(context, UploadService.class);
        cancelTransfersIntent.setAction(UploadService.ACTION_CANCEL);
        context.startService(cancelTransfersIntent);

        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
        dbH.clearCredentials();

        if (dbH.getPreferences() != null){
            dbH.clearPreferences();
            dbH.setFirstTime(false);
            Intent stopIntent = null;
            stopIntent = new Intent(context, CameraSyncService.class);
            stopIntent.setAction(CameraSyncService.ACTION_LOGOUT);
            context.startService(stopIntent);
        }
        dbH.clearOffline();

        dbH.clearContacts();

        dbH.clearNonContacts();

        log("logout: switch OFF chat");
        dbH.setEnabledChat(false+"");

        if (!isManagerActivityLollipop){
            Intent tourIntent = new Intent(context, LoginActivityLollipop.class);
            tourIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            context.startActivity(tourIntent);
        }
    }

    public int updateUserAttributes(String oldFirstName, String newFirstName, String oldLastName, String newLastName, String oldMail, String newMail){
        log("updateUserAttributes");
        MyAccountFragmentLollipop myAccountFragmentLollipop = ((ManagerActivityLollipop)context).getMyAccountFragment();
        if(!oldFirstName.equals(newFirstName)){
            log("Changes in first name");
            if(myAccountFragmentLollipop!=null){
                count++;
                megaApi.setUserAttribute(MegaApiJava.USER_ATTR_FIRSTNAME, newFirstName, (ManagerActivityLollipop)context);
            }
        }
        if(!oldLastName.equals(newLastName)){
            log("Changes in last name");
            if(myAccountFragmentLollipop!=null){
                count++;
                megaApi.setUserAttribute(MegaApiJava.USER_ATTR_LASTNAME, newLastName, (ManagerActivityLollipop)context);
            }
        }
        if(!oldMail.equals(newMail)){
            log("Changes in mail, new mail: "+newMail);
            megaApi.changeEmail(newMail, (ManagerActivityLollipop)context);
        }
        log("The number of attributes to change is: "+count);
        return count;
    }

    public int getCount() {
        return count;
    }

    static public void setCount(int countUa) {
        count = countUa;
    }

    public static void log(String message) {
        Util.log("AccountController", message);
    }
}
