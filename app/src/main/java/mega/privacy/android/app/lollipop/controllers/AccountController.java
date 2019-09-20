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
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.JobUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

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
        logDebug("AccountController created");
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
        logDebug("deleteAccount");
        if (((ManagerActivityLollipop) context).is2FAEnabled()){
            ((ManagerActivityLollipop) context).showVerifyPin2FA(CANCEL_ACCOUNT_2FA);
        }
        else {
            megaApi.cancelAccount((ManagerActivityLollipop) context);
        }
    }

    public void confirmDeleteAccount(String link, String pass){
        logDebug("confirmDeleteAccount");
        megaApi.confirmCancelAccount(link, pass, (ManagerActivityLollipop)context);
    }

    public void confirmChangeMail(String link, String pass){
        logDebug("confirmChangeMail");
        megaApi.confirmChangeEmail(link, pass, (ManagerActivityLollipop)context);
    }

    public boolean existsAvatar() {
        File avatar = buildAvatarFile(context,megaApi.getMyEmail() + ".jpg");
        if (isFileAvailable(avatar)) {
            logDebug("Avatar exists in: " + avatar.getAbsolutePath());
            return true;
        }
        return false;
    }

    public void removeAvatar() {
        logDebug("removeAvatar");
        File avatar = buildAvatarFile(context,megaApi.getMyEmail() + ".jpg");
        File qrFile = buildQrFile(context,megaApi.getMyEmail() + "QRcode.jpg");

        if (isFileAvailable(avatar)) {
            logDebug("Avatar to delete: " + avatar.getAbsolutePath());
            avatar.delete();
        }
        if (isFileAvailable(qrFile)) {
            qrFile.delete();
        }
        megaApi.setAvatar(null,(ManagerActivityLollipop)context);
    }

    public void exportMK(String path, boolean fromOffline){
        logDebug("exportMK");
        if (!isOnline(context)){
            if (context instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_server_connection_problem), -1);
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
            File mainDir = buildExternalStorageFile(MAIN_DIR);
            logDebug("Path main Dir: " + getExternalStoragePath(MAIN_DIR));
            mainDir.mkdirs();

            if (path == null){
                path = getExternalStoragePath(RK_FILE);
                pathNull = true;
            }
            logDebug("Export in: " + path);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    if (context instanceof ManagerActivityLollipop) {
                        ActivityCompat.requestPermissions((ManagerActivityLollipop) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
                    }
                    else if (context instanceof TestPasswordActivity) {
                        ActivityCompat.requestPermissions((TestPasswordActivity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
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
                    ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.error_not_enough_free_space), -1);
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
                    ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.save_MK_confirmation), -1);
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
            logError("ERROR", e);
        }catch (IOException e) {
            e.printStackTrace();
            logError("ERROR", e);
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

    public void renameMK(){
        logDebug("renameMK");
        File oldMKF = buildExternalStorageFile(OLD_MK_FILE);
        File newMKFile = buildExternalStorageFile(RK_FILE);

        oldMKF.renameTo(newMKFile);
    }

    public void copyMK(boolean logout){
        logDebug("copyMK");
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
                    showAlert(((ManagerActivityLollipop) context), context.getString(R.string.copy_MK_confirmation), null);
                }
            }
            else {
                showAlert(((ManagerActivityLollipop) context), context.getString(R.string.general_text_error), null);
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
        logDebug("saveRkToFileSystem");
        Intent intent = new Intent(context, FileStorageActivityLollipop.class);
        intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
        if (context instanceof TestPasswordActivity){
            ((TestPasswordActivity) context).startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
        }
        else if (context instanceof ManagerActivityLollipop){
            if(fromOffline){
                ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_SAVE_MK_FROM_OFFLINE);
            }
            else{
                ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
            }
        }
        else if (context instanceof TwoFactorAuthenticationActivity){
            ((TwoFactorAuthenticationActivity) context).startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
        }
    }

    public void copyRkToClipboard () {
        logDebug("copyRkToClipboard");
        if (context instanceof  ManagerActivityLollipop) {
            copyMK(false);
        }
        else if (context instanceof TestPasswordActivity) {
            copyMK(((TestPasswordActivity) context).isLogout());
        }
        else if (context instanceof TwoFactorAuthenticationActivity) {
            Intent intent = new Intent(context, ManagerActivityLollipop.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.setAction(ACTION_RECOVERY_KEY_COPY_TO_CLIPBOARD);
            intent.putExtra("logout", false);
            context.startActivity(intent);
            ((TwoFactorAuthenticationActivity) context).finish();
        }
    }

    public Bitmap createRkBitmap (){
        logDebug("createRkBitmap");

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
            showAlert(((ManagerActivityLollipop) context), context.getString(R.string.general_text_error), null);
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
        logDebug("removeMK");
        final File f = buildExternalStorageFile(RK_FILE);
        if (isFileAvailable(f)) {
            f.delete();
        }

        //Check if old MK file exists
        final File fOldMK = buildExternalStorageFile(OLD_MK_FILE);
        if(isFileAvailable(fOldMK)){
            logDebug("The old file of MK was also removed");
            fOldMK.delete();
        }

        String message = context.getString(R.string.toast_master_key_removed);
        ((ManagerActivityLollipop) context).invalidateOptionsMenu();
        MyAccountFragmentLollipop mAF = ((ManagerActivityLollipop) context).getMyAccountFragment();
        if(mAF!=null && mAF.isAdded()){
            mAF.setMkButtonText();
        }
        showAlert(((ManagerActivityLollipop) context), message, null);
    }

    public void killAllSessions(Context context){
        logDebug("killAllSessions");
        megaApi.killSession(-1, (ManagerActivityLollipop) context);
    }

    static public void localLogoutApp(Context context){
        logDebug("localLogoutApp");

        try {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancelAll();
        }
        catch(Exception e){
            logError("EXCEPTION removing all the notifications", e);
            e.printStackTrace();
        }

        File privateDir = context.getFilesDir();
        removeFolder(context, privateDir);

        File externalCacheDir = context.getExternalCacheDir();
        removeFolder(context, externalCacheDir);

        File cacheDir = context.getCacheDir();
        removeFolder(context, cacheDir);

        removeOldTempFolders(context);

        final File fMKOld = buildExternalStorageFile(OLD_MK_FILE);
        if (isFileAvailable(fMKOld)){
            logDebug("Old MK file removed!");
            fMKOld.delete();
        }

        final File fMK = buildExternalStorageFile(RK_FILE);
        if (isFileAvailable(fMK)){
            logDebug("RK file removed!");
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
            logWarning("Cancelling services not allowed by the OS", e);
        }

        DatabaseHandler dbH = DatabaseHandler.getDbHandler(context);
        dbH.clearCredentials();

        if (dbH.getPreferences() != null){
            dbH.clearPreferences();
            dbH.setFirstTime(false);
            stopRunningCameraUploadService(context);
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

    public static void removeFolder(Context context, File folder) {
        try {
            deleteFolderAndSubfolders(context, folder);
        } catch (IOException e) {
            logError("Exception deleting" + folder.getName() + "directory", e);
            e.printStackTrace();
        }
    }

    static public void logout(Context context, MegaApiAndroid megaApi) {
        logDebug("logout");

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

        Intent intent = new Intent();
        intent.setAction(ACTION_LOG_OUT);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    static public void logoutConfirmed(Context context){
        logDebug("logoutConfirmed");

        localLogoutApp(context);

        PackageManager m = context.getPackageManager();
        String s = context.getPackageName();
        try {
            PackageInfo p = m.getPackageInfo(s, 0);
            s = p.applicationInfo.dataDir;
        } catch (PackageManager.NameNotFoundException e) {
            logDebug("Error Package name not found " + e);
        }

        File appDir = new File(s);

        for (File c : appDir.listFiles()){
            if (c.isFile()){
                c.delete();
            }
        }
    }

    public int updateUserAttributes(String oldFirstName, String newFirstName, String oldLastName, String newLastName, String oldMail, String newMail){
        logDebug("updateUserAttributes");
        MyAccountFragmentLollipop myAccountFragmentLollipop = ((ManagerActivityLollipop)context).getMyAccountFragment();
        if(!oldFirstName.equals(newFirstName)){
            logDebug("Changes in first name");
            if(myAccountFragmentLollipop!=null){
                count++;
                megaApi.setUserAttribute(MegaApiJava.USER_ATTR_FIRSTNAME, newFirstName, (ManagerActivityLollipop)context);
            }
        }
        if(!oldLastName.equals(newLastName)){
            logDebug("Changes in last name");
            if(myAccountFragmentLollipop!=null){
                count++;
                megaApi.setUserAttribute(MegaApiJava.USER_ATTR_LASTNAME, newLastName, (ManagerActivityLollipop)context);
            }
        }
        if(!oldMail.equals(newMail)){
            logDebug("Changes in mail, new mail: " + newMail);
            if (((ManagerActivityLollipop) context).is2FAEnabled()){
                ((ManagerActivityLollipop) context).setNewMail(newMail);
                ((ManagerActivityLollipop) context).showVerifyPin2FA(CHANGE_MAIL_2FA);
            }
            else {
                megaApi.changeEmail(newMail, (ManagerActivityLollipop)context);
            }
        }
        logDebug("The number of attributes to change is: " + count);
        return count;
    }

    public int getCount() {
        return count;
    }

    static public void setCount(int countUa) {
        count = countUa;
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
