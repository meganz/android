package mega.privacy.android.app.main.controllers;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.print.PrintHelper;
import androidx.appcompat.app.AlertDialog;

import com.jeremyliao.liveeventbus.LiveEventBus;

import java.io.File;
import java.io.IOException;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.DownloadService;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.OpenLinkActivity;
import mega.privacy.android.app.R;
import mega.privacy.android.app.UploadService;
import mega.privacy.android.app.listeners.LogoutListener;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.mediaplayer.service.MediaPlayerService;
import mega.privacy.android.app.mediaplayer.service.MediaPlayerServiceViewModel;
import mega.privacy.android.app.jobservices.SyncRecord;
import mega.privacy.android.app.main.FileStorageActivity;
import mega.privacy.android.app.main.TestPasswordActivity;
import mega.privacy.android.app.main.TwoFactorAuthenticationActivity;
import mega.privacy.android.app.main.VerifyTwoFactorActivity;
import mega.privacy.android.app.sync.BackupToolsKt;
import mega.privacy.android.app.psa.PsaManager;
import mega.privacy.android.app.utils.ZoomUtil;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
import mega.privacy.android.app.utils.LastShowSMSDialogTimeChecker;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;

import static mega.privacy.android.app.constants.SettingsConstants.KEY_MOBILE_DATA_HIGH_RESOLUTION;
import static mega.privacy.android.app.fragments.offline.OfflineFragment.SHOW_OFFLINE_WARNING;
import static mega.privacy.android.app.middlelayer.push.PushMessageHanlder.PUSH_TOKEN;
import static mega.privacy.android.app.textEditor.TextEditorViewModel.SHOW_LINE_NUMBERS;
import static mega.privacy.android.app.utils.CacheFolderManager.*;
import static mega.privacy.android.app.utils.CameraUploadUtil.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.JobUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES;
import static mega.privacy.android.app.utils.StorageUtils.thereIsNotEnoughFreeSpace;
import static mega.privacy.android.app.utils.StringResourcesUtils.getString;
import static mega.privacy.android.app.utils.permission.PermissionUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class AccountController {

    Context context;
    MegaApiAndroid megaApi;

    public AccountController(Context context){
        logDebug("AccountController created");
        this.context = context;
        this.megaApi = MegaApplication.getInstance().getMegaApi();
    }

    public void resetPass(String myEmail){
        megaApi.resetPassword(myEmail, true, (ManagerActivity)context);
    }

    public void deleteAccount(){
        logDebug("deleteAccount");
        if (((ManagerActivity) context).is2FAEnabled()){
            Intent intent = new Intent(context, VerifyTwoFactorActivity.class);
            intent.putExtra(VerifyTwoFactorActivity.KEY_VERIFY_TYPE, CANCEL_ACCOUNT_2FA);

            context.startActivity(intent);
        }
        else {
            megaApi.cancelAccount((ManagerActivity) context);
        }
    }

    public boolean existsAvatar() {
        File avatar = buildAvatarFile(context,megaApi.getMyEmail() + ".jpg");
        if (isFileAvailable(avatar)) {
            logDebug("Avatar exists in: " + avatar.getAbsolutePath());
            return true;
        }
        return false;
    }

    public void printRK(){
        Bitmap rKBitmap = createRkBitmap();

        if (rKBitmap != null){
            PrintHelper printHelper = new PrintHelper(context);
            printHelper.setScaleMode(PrintHelper.SCALE_MODE_FIT);
            printHelper.printBitmap("rKPrint", rKBitmap, () -> {
                if (context instanceof TestPasswordActivity) {
                    ((TestPasswordActivity) context).passwordReminderSucceeded();
                }
            });
        }
    }

    /**
     * Export recovery key file to a selected location on file system.
     *
     * @param path The selected location.
     */
    public void exportMK(String path) {
        logDebug("exportMK");
        if (isOffline(context)) {
            return;
        }

        String key = megaApi.exportMasterKey();

        if (context instanceof ManagerActivity) {
            megaApi.masterKeyExported((ManagerActivity) context);
        } else if (context instanceof TestPasswordActivity) {
            ((TestPasswordActivity) context).incrementRequests();
            megaApi.masterKeyExported((TestPasswordActivity) context);
        }

        if (!hasPermissions(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            if (context instanceof ManagerActivity) {
                requestPermission((ManagerActivity) context, REQUEST_WRITE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else if (context instanceof TestPasswordActivity) {
                requestPermission((TestPasswordActivity) context, REQUEST_WRITE_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
            return;
        }

        if (thereIsNotEnoughFreeSpace(path)) {
            showSnackbar(context, getString(R.string.error_not_enough_free_space));
            return;
        }

        if (saveTextOnFile(context, key, path)) {
            showSnackbar(context, getString(R.string.save_MK_confirmation));

            if (context instanceof TestPasswordActivity) {
                ((TestPasswordActivity) context).passwordReminderSucceeded();
            }
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
        if (context instanceof ManagerActivity) {
            if (key != null) {
                megaApi.masterKeyExported((ManagerActivity) context);
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", key);
                clipboard.setPrimaryClip(clip);
                if (logout) {
                    showConfirmDialogRecoveryKeySaved();
                }
                else {
                    showAlert(((ManagerActivity) context), context.getString(R.string.copy_MK_confirmation), null);
                }
            }
            else {
                showAlert(((ManagerActivity) context), context.getString(R.string.general_text_error), null);
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

    public static void saveRkToFileSystem(Activity activity) {
        Intent intent = new Intent(activity, FileStorageActivity.class)
                .setAction(FileStorageActivity.Mode.PICK_FOLDER.getAction())
                .putExtra(FileStorageActivity.EXTRA_SAVE_RECOVERY_KEY, true);

        activity.startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
    }

    public void copyRkToClipboard () {
        logDebug("copyRkToClipboard");
        if (context instanceof ManagerActivity) {
            copyMK(false);
        }
        else if (context instanceof TestPasswordActivity) {
            copyMK(((TestPasswordActivity) context).isLogout());
        }
        else if (context instanceof TwoFactorAuthenticationActivity) {
            Intent intent = new Intent(context, ManagerActivity.class);
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
            showAlert(((ManagerActivity) context), context.getString(R.string.general_text_error), null);
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

    static public void localLogoutApp(Context context) {
        MegaApplication app = MegaApplication.getInstance();

        logDebug("Logged out. Resetting account auth token for folder links.");
        app.getMegaApiFolder().setAccountAuth(null);

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
            ContextCompat.startForegroundService(context, cancelTransfersIntent);
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

        dbH.clearBackups();

        //clear mega contacts and reset last sync time.
        dbH.clearMegaContacts();
        new MegaContactGetter(context).clearLastSyncTimeStamp();

        // clean time stamps preference settings after logout
        clearCUBackUp();

        SharedPreferences preferences = context.getSharedPreferences(MegaContactGetter.LAST_SYNC_TIMESTAMP_FILE, Context.MODE_PRIVATE);
        preferences.edit().putLong(MegaContactGetter.LAST_SYNC_TIMESTAMP_KEY, 0).apply();

        //clear push token
        context.getSharedPreferences(PUSH_TOKEN, Context.MODE_PRIVATE).edit().clear().apply();

        //clear text editor preference
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(SHOW_LINE_NUMBERS, false).apply();

        //clear offline warning preference
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(SHOW_OFFLINE_WARNING, true).apply();

        //clear user interface preferences
        context.getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
                .edit().clear().apply();

        //reset zoom level
        ZoomUtil.INSTANCE.resetZoomLevel();

        removeEmojisSharedPreferences();

        new LastShowSMSDialogTimeChecker(context).reset();
        MediaPlayerService.stopAudioPlayer(context);
        MediaPlayerServiceViewModel.clearSettings(context);

        PsaManager.INSTANCE.stopChecking();

        //Clear MyAccountInfo
        app.resetMyAccountInfo();
        app.setStorageState(MegaApiJava.STORAGE_STATE_UNKNOWN);

        // Clear get banner success flag
        LiveEventBus.get(EVENT_LOGOUT_CLEARED).post(null);

        // Clear Key mobile data high resolution preference
        SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultPreferences.edit().remove(KEY_MOBILE_DATA_HIGH_RESOLUTION).apply();
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

        MegaApplication.setLoggingOut(true);

        BackupToolsKt.removeBackupsBeforeLogout();

        if (megaApi == null){
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        if (context instanceof ManagerActivity) {
            megaApi.logout((ManagerActivity) context);
        } else if (context instanceof OpenLinkActivity) {
            megaApi.logout((OpenLinkActivity) context);
        } else if (context instanceof TestPasswordActivity) {
            megaApi.logout(((TestPasswordActivity) context));
        } else {
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
}
