package mega.privacy.android.app.lollipop.controllers;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;

import mega.privacy.android.app.CameraSyncService;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.OpenLinkActivity;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinLockActivityLollipop;
import mega.privacy.android.app.lollipop.TestPasswordActivity;
import mega.privacy.android.app.lollipop.managerSections.MyAccountFragmentLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaUser;

public class AccountController implements View.OnClickListener{

    Context context;
    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;
    DatabaseHandler dbH;
    MegaPreferences prefs = null;

    static int count = 0;

    AlertDialog recoveryKeyExportedDialog;
    Button recoveryKeyExportedButton;

    public AccountController(Context context){
        log("AccountController created");
        this.context = context;

        if (megaApi == null){
            if (context instanceof MegaApplication){
                megaApi = ((MegaApplication)context).getMegaApi();
            }
            else{
                megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
            }
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
            File qrFile = null;
            if (context.getExternalCacheDir() != null){
                avatar = new File(context.getExternalCacheDir().getAbsolutePath(), myEmail + ".jpg");
                qrFile = new File(context.getExternalCacheDir().getAbsolutePath(), myEmail + "QRcode.jpg");
            }
            else{
                avatar = new File(context.getCacheDir().getAbsolutePath(), myEmail + ".jpg");
                qrFile = new File(context.getCacheDir().getAbsolutePath(), myEmail + "QRcode.jpg");
            }

            if (avatar.exists()) {
                log("avatar to delete: " + avatar.getAbsolutePath());
                avatar.delete();
            }

            if (qrFile.exists()){
                qrFile.delete();
            }
        }

        megaApi.setAvatar(null, (ManagerActivityLollipop)context);
    }

    public void exportMK(String path){
        log("exportMK");
        if (!Util.isOnline(context)){
            ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            return;
        }

        boolean pathNull = false;

        String key = megaApi.exportMasterKey();
        megaApi.masterKeyExported((ManagerActivityLollipop) context);

        BufferedWriter out;
        try {
            String mainDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + Util.mainDIR;
            File mainDir = new File(mainDirPath);
            log("Path main Dir: " + mainDirPath);
            mainDir.mkdirs();

            if (path == null){
                path = Environment.getExternalStorageDirectory().getAbsolutePath()+Util.rKFile;
                pathNull = true;
            }
            log("Export in: "+path);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean hasStoragePermission = (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
                if (!hasStoragePermission) {
                    ActivityCompat.requestPermissions((ManagerActivityLollipop) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_WRITE_STORAGE);
                }
            }

            double availableFreeSpace = Double.MAX_VALUE;
            try{
                StatFs stat = new StatFs(path);
                availableFreeSpace = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
            }
            catch(Exception ex){}

            File file = new File(path);
            if(availableFreeSpace < file.length()) {
                ((ManagerActivityLollipop) context).showSnackbar(context.getString(R.string.error_not_enough_free_space));
                return;
            }

            FileWriter fileWriter= new FileWriter(path);
            out = new BufferedWriter(fileWriter);
            out.write(key);
            out.close();
//            String message = context.getString(R.string.toast_master_key, path);
//            try{
//                message = message.replace("[A]", "\n");
//            }
//            catch (Exception e){}
            if (pathNull){
                ((ManagerActivityLollipop) context).invalidateOptionsMenu();
                MyAccountFragmentLollipop mAF = ((ManagerActivityLollipop) context).getMyAccountFragment();
                if(mAF!=null){
                    mAF.setMkButtonText();
                }

                showConfirmationExportedDialog();
            }
            else {
                showConfirmDialogRecoveryKeySaved();
            }
//            Util.showAlert(((ManagerActivityLollipop) context), message, null);

        }catch (FileNotFoundException e) {
            e.printStackTrace();
            log("ERROR: " + e.getMessage());
        }catch (IOException e) {
            e.printStackTrace();
            log("ERROR: " + e.getMessage());
        }
    }

    void showConfirmationExportedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = ((ManagerActivityLollipop)context).getLayoutInflater();
        View v = inflater.inflate(R.layout.dialog_recovery_key_exported, null);
        builder.setView(v);

        recoveryKeyExportedButton = (Button) v.findViewById(R.id.dialog_recovery_key_button);
        recoveryKeyExportedButton.setOnClickListener(this);

        recoveryKeyExportedDialog = builder.create();
        recoveryKeyExportedDialog.show();
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

    public void copyMK(boolean logout){
        log("copyMK");
        String key = megaApi.exportMasterKey();
        megaApi.masterKeyExported((ManagerActivityLollipop) context);
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", key);
        clipboard.setPrimaryClip(clip);
        if (logout){
            showConfirmDialogRecoveryKeySaved();
        }
        else {
            Util.showAlert(((ManagerActivityLollipop) context), context.getString(R.string.copy_MK_confirmation), null);
        }
    }

    void showConfirmDialogRecoveryKeySaved(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getString(R.string.copy_MK_confirmation));
        builder.setPositiveButton(context.getString(R.string.action_logout), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                logout(context, megaApi);
            }
        });
        builder.show();
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
            mAF.setMkButtonText();;
        }
        Util.showAlert(((ManagerActivityLollipop) context), message, null);
    }

    public void killAllSessions(Context context){
        log("killAllSessions");
        megaApi.killSession(-1, (ManagerActivityLollipop) context);
    }

    static public void logout(Context context, MegaApiAndroid megaApi) {
        log("logout");

        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.cancelAll();
        }
        catch(Exception e){
            log("EXCEPTION removing all the notifications");
        }

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if (context instanceof ManagerActivityLollipop){
            megaApi.logout((ManagerActivityLollipop)context);
        }
        else if (context instanceof OpenLinkActivity){
            megaApi.logout((OpenLinkActivity)context);
        }
        else if (context instanceof PinLockActivityLollipop){
            megaApi.logout((PinLockActivityLollipop)context);
        }
        else if (context instanceof TestPasswordActivity){
            megaApi.logout(((TestPasswordActivity)context));
        }
        else{
            megaApi.logout();
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

        dbH.clearChatItems();

        dbH.clearCompletedTransfers();

        dbH.clearPendingMessage();

        dbH.clearAttributes();
    }

    static public void logoutConfirmed(Context context){
        log("logoutConfirmed");

        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
        dbH.clearChatSettings();

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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.dialog_recovery_key_button:{
                recoveryKeyExportedDialog.dismiss();
                break;
            }
        }
    }
}
