package mega.privacy.android.app.lollipop.controllers;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaPreferences;
import mega.privacy.android.app.OpenLinkActivity;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.jobservices.SyncRecord;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinLockActivityLollipop;
import mega.privacy.android.app.lollipop.TestPasswordActivity;
import mega.privacy.android.app.lollipop.TwoFactorAuthenticationActivity;
import mega.privacy.android.app.lollipop.managerSections.MyAccountFragmentLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.JobUtil;
import mega.privacy.android.app.utils.PreviewUtils;
import mega.privacy.android.app.utils.ThumbnailUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.Constants.ACTION_LOG_OUT;

import static mega.privacy.android.app.utils.CacheFolderManager.buildAvatarFile;
import static mega.privacy.android.app.utils.CacheFolderManager.buildQrFile;
import static mega.privacy.android.app.utils.CacheFolderManager.isFileAvailable;

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
        if (((ManagerActivityLollipop) context).is2FAEnabled()){
            ((ManagerActivityLollipop) context).showVerifyPin2FA(Constants.CANCEL_ACCOUNT_2FA);
        }
        else {
            megaApi.cancelAccount((ManagerActivityLollipop) context);
        }
    }

    public void confirmDeleteAccount(String link, String pass){
        log("confirmDeleteAccount");
        megaApi.confirmCancelAccount(link, pass, (ManagerActivityLollipop)context);
    }

    public void confirmChangeMail(String link, String pass){
        log("confirmChangeMail");
        megaApi.confirmChangeEmail(link, pass, (ManagerActivityLollipop)context);
    }

    public boolean existsAvatar() {
        File avatar = buildAvatarFile(context,megaApi.getMyEmail() + ".jpg");
        if (isFileAvailable(avatar)) {
            log("avatar exists in: " + avatar.getAbsolutePath());
            return true;
        }
        return false;
    }

    public void removeAvatar() {
        log("removeAvatar");
        File avatar = buildAvatarFile(context,megaApi.getMyEmail() + ".jpg");
        File qrFile = buildQrFile(context,megaApi.getMyEmail() + "QRcode.jpg");

        if (isFileAvailable(avatar)) {
            log("avatar to delete: " + avatar.getAbsolutePath());
            avatar.delete();
        }
        if (isFileAvailable(qrFile)) {
            qrFile.delete();
        }
        megaApi.setAvatar(null,(ManagerActivityLollipop)context);
    }

    public void exportMK(String path, boolean fromOffline){
        log("exportMK");
        if (!Util.isOnline(context)){
            if (context instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
            }
            else if (context instanceof TestPasswordActivity) {
                ((TestPasswordActivity) context).showSnackbar(context.getString(R.string.error_server_connection_problem));
            }
            return;
        }

        boolean pathNull = false;

        String key = megaApi.exportMasterKey();
        if (context instanceof ManagerActivityLollipop) {
            megaApi.masterKeyExported((ManagerActivityLollipop) context);
        }
        else if (context instanceof TestPasswordActivity) {
            ((TestPasswordActivity) context).incrementRequests();
            megaApi.masterKeyExported((TestPasswordActivity) context);
        }

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
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (context instanceof ManagerActivityLollipop) {
                        ActivityCompat.requestPermissions((ManagerActivityLollipop) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_WRITE_STORAGE);
                    }
                    else if (context instanceof TestPasswordActivity) {
                        ActivityCompat.requestPermissions((TestPasswordActivity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.REQUEST_WRITE_STORAGE);
                    }
                    return;
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
                if (context instanceof ManagerActivityLollipop) {
                    ((ManagerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.error_not_enough_free_space), -1);
                }
                else if (context instanceof TestPasswordActivity) {
                    ((TestPasswordActivity) context).showSnackbar(context.getString(R.string.error_not_enough_free_space));
                }
                return;
            }

            FileWriter fileWriter= new FileWriter(path);
            out = new BufferedWriter(fileWriter);
            out.write(key);
            out.close();

            if (context instanceof ManagerActivityLollipop) {
                if (pathNull){
                    ((ManagerActivityLollipop) context).invalidateOptionsMenu();
                    MyAccountFragmentLollipop mAF = ((ManagerActivityLollipop) context).getMyAccountFragment();
                    if (mAF != null) {
                        mAF.setMkButtonText();
                    }
                    showConfirmationExportedDialog();
                }
                else if(fromOffline) {
                    ((ManagerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, context.getString(R.string.save_MK_confirmation), -1);
                }
                else{
                    showConfirmDialogRecoveryKeySaved();
                }
            }
            else if (context instanceof TestPasswordActivity) {
                if (pathNull) {
                    showConfirmationExportedDialog();
                }
                else {
                    ((TestPasswordActivity) context).showSnackbar(context.getString(R.string.save_MK_confirmation));
                    ((TestPasswordActivity) context).passwordReminderSucceeded();
                }
            }

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
        LayoutInflater inflater = null;
        if (context instanceof ManagerActivityLollipop) {
            inflater = ((ManagerActivityLollipop) context).getLayoutInflater();
        }
        else if (context instanceof TestPasswordActivity) {
            inflater = ((TestPasswordActivity) context).getLayoutInflater();
        }
        View v = inflater.inflate(R.layout.dialog_recovery_key_exported, null);
        builder.setView(v);

        recoveryKeyExportedButton = (Button) v.findViewById(R.id.dialog_recovery_key_button);
        recoveryKeyExportedButton.setOnClickListener(this);

        recoveryKeyExportedDialog = builder.create();
        recoveryKeyExportedDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (context instanceof TestPasswordActivity) {
                    ((TestPasswordActivity) context).passwordReminderSucceeded();
                }
            }
        });
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
        if (context instanceof ManagerActivityLollipop) {
            if (key != null) {
                megaApi.masterKeyExported((ManagerActivityLollipop) context);
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", key);
                clipboard.setPrimaryClip(clip);
                if (logout) {
                    showConfirmDialogRecoveryKeySaved();
                }
                else {
                    Util.showAlert(((ManagerActivityLollipop) context), context.getString(R.string.copy_MK_confirmation), null);
                }
            }
            else {
                Util.showAlert(((ManagerActivityLollipop) context), context.getString(R.string.general_text_error), null);
            }
        }
        else if (context instanceof TestPasswordActivity) {
            if (key != null) {
                ((TestPasswordActivity) context).incrementRequests();
                megaApi.masterKeyExported((TestPasswordActivity) context);
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", key);
                clipboard.setPrimaryClip(clip);
                if (logout) {
                    showConfirmDialogRecoveryKeySaved();
                }
                else {
                    ((TestPasswordActivity) context).showSnackbar(context.getString(R.string.copy_MK_confirmation));
                    ((TestPasswordActivity) context).passwordReminderSucceeded();
                }
            }
            else {
                ((TestPasswordActivity) context).showSnackbar(context.getString(R.string.general_text_error));
            }
        }
    }

    public void saveRkToFileSystem (boolean fromOffline) {
        log("saveRkToFileSystem");
        Intent intent = new Intent(context, FileStorageActivityLollipop.class);
        intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
        if (context instanceof TestPasswordActivity){
            ((TestPasswordActivity) context).startActivityForResult(intent, Constants.REQUEST_DOWNLOAD_FOLDER);
        }
        else if (context instanceof ManagerActivityLollipop){
            if(fromOffline){
                ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_SAVE_MK_FROM_OFFLINE);
            }
            else{
                ((ManagerActivityLollipop) context).startActivityForResult(intent, Constants.REQUEST_DOWNLOAD_FOLDER);
            }
        }
        else if (context instanceof TwoFactorAuthenticationActivity){
            ((TwoFactorAuthenticationActivity) context).startActivityForResult(intent, Constants.REQUEST_DOWNLOAD_FOLDER);
        }
    }

    public void copyRkToClipboard () {
        log("copyRkToClipboard");
        if (context instanceof  ManagerActivityLollipop) {
            copyMK(false);
        }
        else if (context instanceof TestPasswordActivity) {
            copyMK(((TestPasswordActivity) context).isLogout());
        }
        else if (context instanceof TwoFactorAuthenticationActivity) {
            Intent intent = new Intent(context, ManagerActivityLollipop.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(Constants.ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD);
            intent.putExtra("logout", false);
            context.startActivity(intent);
            ((TwoFactorAuthenticationActivity) context).finish();
        }
    }

    public Bitmap createRkBitmap (){
        log("createRkBitmap");

        Bitmap rKBitmap = Bitmap.createBitmap(1000, 1000, Bitmap.Config.ARGB_8888);
        String key = megaApi.exportMasterKey();

        if (key != null) {
            Canvas canvas = new Canvas(rKBitmap);
            Paint paint = new Paint();

            paint.setTextSize(40);
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.FILL);
            float height = paint.measureText("yY");
            float width = paint.measureText(key);
            float x = (rKBitmap.getWidth() - width) / 2;
            canvas.drawText(key, x, height + 15f, paint);

            if (rKBitmap != null) {
                return rKBitmap;
            }
        }
        else {
            Util.showAlert(((ManagerActivityLollipop) context), context.getString(R.string.general_text_error), null);
        }

        return null;
    }

    void showConfirmDialogRecoveryKeySaved(){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(context.getString(R.string.copy_MK_confirmation));
        builder.setPositiveButton(context.getString(R.string.action_logout), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (context instanceof TestPasswordActivity) {
                    ((TestPasswordActivity) context).passwordReminderSucceeded();
                }
                else {
                    logout(context, megaApi);
                }
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (context instanceof TestPasswordActivity) {
                    ((TestPasswordActivity) context).passwordReminderSucceeded();
                }
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
        if(mAF!=null && mAF.isAdded()){
            mAF.setMkButtonText();
        }
        Util.showAlert(((ManagerActivityLollipop) context), message, null);
    }

    public void killAllSessions(Context context){
        log("killAllSessions");
        megaApi.killSession(-1, (ManagerActivityLollipop) context);
    }

    static public void localLogoutApp(Context context){

        log("localLogoutApp");

        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.cancelAll();
        }
        catch(Exception e){
            log("EXCEPTION removing all the notifications");
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

        try{
            Intent cancelTransfersIntent = new Intent(context, DownloadService.class);
            cancelTransfersIntent.setAction(DownloadService.ACTION_CANCEL);
            context.startService(cancelTransfersIntent);
            cancelTransfersIntent = new Intent(context, UploadService.class);
            cancelTransfersIntent.setAction(UploadService.ACTION_CANCEL);
            context.startService(cancelTransfersIntent);
        }
        catch(IllegalStateException e){
            //If the application is in a state where the service can not be started (such as not in the foreground in a state when services are allowed) - included in API 26
            log("Cancelling services not allowed by the OS: "+e.getMessage());
        }

        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
        dbH.clearCredentials();

        if (dbH.getPreferences() != null){
            dbH.clearPreferences();
            dbH.setFirstTime(false);
            JobUtil.stopRunningCameraUploadService(context);
        }
        dbH.clearOffline();

        dbH.clearContacts();

        dbH.clearNonContacts();

        dbH.clearChatItems();

        dbH.clearCompletedTransfers();

        dbH.clearPendingMessage();

        dbH.clearAttributes();

        dbH.deleteAllSyncRecords(SyncRecord.TYPE_ANY);

        dbH.clearChatSettings();
        dbH.setEnabledChat(true + "");
    }

    static public void logout(Context context, MegaApiAndroid megaApi) {
        log("logout");

        MegaApplication application = (MegaApplication) ((Activity)context).getApplication();
        //Clear num verions after logout
        application.getMyAccountInfo().setNumVersions(-1);

        if (megaApi == null){
            megaApi = application.getMegaApi();
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

        localLogoutApp(context);

        Intent intent = new Intent();
        intent.setAction(ACTION_LOG_OUT);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
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
            if (((ManagerActivityLollipop) context).is2FAEnabled()){
                ((ManagerActivityLollipop) context).setNewMail(newMail);
                ((ManagerActivityLollipop) context).showVerifyPin2FA(Constants.CHANGE_MAIL_2FA);
            }
            else {
                megaApi.changeEmail(newMail, (ManagerActivityLollipop)context);
            }
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
                if (context instanceof TestPasswordActivity) {
                    ((TestPasswordActivity) context).passwordReminderSucceeded();
                }
                break;
            }
        }
    }
}
