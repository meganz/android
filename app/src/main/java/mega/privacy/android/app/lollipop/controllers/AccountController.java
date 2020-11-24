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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.print.PrintHelper;
import androidx.appcompat.app.AlertDialog;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.OpenLinkActivity;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.audioplayer.service.AudioPlayerService;
import mega.privacy.android.app.audioplayer.service.AudioPlayerServiceViewModel;
import mega.privacy.android.app.jobservices.SyncRecord;
import mega.privacy.android.app.listeners.LogoutListener;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PinLockActivityLollipop;
import mega.privacy.android.app.lollipop.TestPasswordActivity;
import mega.privacy.android.app.lollipop.TwoFactorAuthenticationActivity;
import mega.privacy.android.app.lollipop.managerSections.MyAccountFragmentLollipop;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
import mega.privacy.android.app.utils.LastShowSMSDialogTimeChecker;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;

import static mega.privacy.android.app.lollipop.qrcode.MyCodeFragment.*;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.CameraUploadUtil.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.JobUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class AccountController {

    Context context;
    MegaApiAndroid megaApi;

    static int count = 0;

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
        File qrFile = buildQrFile(context,megaApi.getMyEmail() + QR_IMAGE_FILE_NAME);

        if (isFileAvailable(avatar)) {
            logDebug("Avatar to delete: " + avatar.getAbsolutePath());
            avatar.delete();
        }
        if (isFileAvailable(qrFile)) {
            qrFile.delete();
        }
        megaApi.setAvatar(null,(ManagerActivityLollipop)context);
    }

    public void printRK(){
        Bitmap rKBitmap = createRkBitmap();

        if (rKBitmap != null){
            PrintHelper printHelper = new PrintHelper(context);
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            printHelper.printBitmap("rKPrint", rKBitmap, new PrintHelper.OnPrintFinishCallback() {
                @Override
                public void onFinish() {
                    if (context instanceof TestPasswordActivity) {
                        ((TestPasswordActivity) context).passwordReminderSucceeded();
                    }
                }
            });
        }
    }

    public void exportMK(String path){
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
                ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, context.getString(R.string.save_MK_confirmation), -1);
            }
            else if (context instanceof TestPasswordActivity) {
                ((TestPasswordActivity) context).showSnackbar(context.getString(R.string.save_MK_confirmation));
                ((TestPasswordActivity) context).passwordReminderSucceeded();
            }

        }catch (FileNotFoundException e) {
            e.printStackTrace();
            logError("ERROR", e);
        }catch (IOException e) {
            e.printStackTrace();
            logError("ERROR", e);
        }
    }

    /**
     * Rename the old MK or RK file to the new RK file name.
     * @param oldFile Old MK or RK file to be renamed
     */
    public void renameRK(File oldFile){
        logDebug("renameRK");
        File newRKFile = new File(oldFile.getParentFile(), getRecoveryKeyFileName());
        oldFile.renameTo(newRKFile);
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

    public void saveRkToFileSystem () {
        logDebug("saveRkToFileSystem");

        Intent intent = new Intent(context, FileStorageActivityLollipop.class);
        intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_SAVE_RECOVERY_KEY, true);
        if (context instanceof TestPasswordActivity){
            ((TestPasswordActivity) context).startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
        }
        else if (context instanceof ManagerActivityLollipop){
            ((ManagerActivityLollipop) context).startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
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

        File [] downloadToSDCardCahce = context.getExternalCacheDirs();
        if(downloadToSDCardCahce.length > 1) {
            removeFolder(context, downloadToSDCardCahce[1]);
        }

        File cacheDir = context.getCacheDir();
        removeFolder(context, cacheDir);

        removeOldTempFolders(context);

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

        //clear mega contacts and reset last sync time.
        dbH.clearMegaContacts();
        new MegaContactGetter(context).clearLastSyncTimeStamp();

        // clean time stamps preference settings after logout
        clearCUBackUp();

        new LastShowSMSDialogTimeChecker(context).reset();
        AudioPlayerService.stopAudioPlayer(context);
        AudioPlayerServiceViewModel.clearSettings(context);

        //Clear MyAccountInfo
        MegaApplication app = MegaApplication.getInstance();
        app.getMyAccountInfo().clear();
        app.setStorageState(MegaApiJava.STORAGE_STATE_UNKNOWN);
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
            megaApi = MegaApplication.getInstance().getMegaApi();
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
            megaApi.logout(new LogoutListener(context));
        }

        Intent intent = new Intent();
        intent.setAction(ACTION_LOG_OUT);
        context.sendBroadcast(intent);
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
}
