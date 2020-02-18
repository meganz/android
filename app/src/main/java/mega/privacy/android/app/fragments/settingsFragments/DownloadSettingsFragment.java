package mega.privacy.android.app.fragments.settingsFragments;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.support.v7.preference.Preference;

import java.io.File;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.TwoLineCheckPreference;
import mega.privacy.android.app.lollipop.FileStorageActivityLollipop;
import mega.privacy.android.app.utils.SDCardOperator;
import mega.privacy.android.app.utils.SelectDownloadLocationDialog;

import static mega.privacy.android.app.constants.SettingsConstants.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.FileUtil.*;
import static mega.privacy.android.app.utils.FileUtils.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class DownloadSettingsFragment extends SettingsBaseFragment implements Preference.OnPreferenceClickListener {

    private Preference downloadLocation;
    private TwoLineCheckPreference storageAskMeAlways;

    private boolean askMe;
    private String downloadLocationPath = "";
    private boolean hasSDCard;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_download);

        downloadLocation = findPreference(KEY_STORAGE_DOWNLOAD_LOCATION);
        downloadLocation.setOnPreferenceClickListener(this);

        storageAskMeAlways = (TwoLineCheckPreference) findPreference(KEY_STORAGE_ASK_ME_ALWAYS);
        storageAskMeAlways.setOnPreferenceClickListener(this);

        File[] fs = context.getExternalFilesDirs(null);
        if (fs.length == 1 || (fs.length > 1 && fs[1] == null)) {
            hasSDCard = false;
        } else {
            hasSDCard = true;
        }

        if (prefs.getStorageAskAlways() == null) {
            askMe = false;
            dbH.setStorageAskAlways(false);
        } else {
            askMe = Boolean.parseBoolean(prefs.getStorageAskAlways());
        }

        storageAskMeAlways.setChecked(askMe);

        if (prefs.getStorageAskAlways() == null || prefs.getStorageDownloadLocation() == null || prefs.getStorageDownloadLocation().isEmpty()) {
            File defaultDownloadLocation = buildDefaultDownloadDir(context);
            defaultDownloadLocation.mkdirs();

            downloadLocationPath = defaultDownloadLocation.getAbsolutePath();
            dbH.setStorageDownloadLocation(downloadLocationPath);
        }

        setDownloadLocation();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        switch (preference.getKey()) {
            case KEY_STORAGE_ASK_ME_ALWAYS:
                dbH.setStorageAskAlways(askMe = storageAskMeAlways.isChecked());
                setDownloadLocation();
                break;

            case KEY_STORAGE_DOWNLOAD_LOCATION:
                if (hasStoragePermission()) {
                    if (hasSDCard) {
                        showSelectDownloadLocationDialog();
                    } else {
                        toSelectFolder(null);
                    }
                } else {
                    requestPermissions(new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_DOWNLOAD_LOCATION_INTERNAL_SD_CARD);
                }
                break;
        }

        return true;
    }

    private void setDownloadLocation() {
        if (downloadLocation != null) {
            downloadLocation.setEnabled(!askMe);
            downloadLocation.setSummary(prefs.getStorageDownloadLocation());
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_TREE) {
            if (intent == null) {
                logDebug("intent NULL");
                if (requestCode != Activity.RESULT_OK) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        showSnackbar(context, getString(R.string.download_requires_permission));
                    }
                } else {
                    onCannotWriteOnSDCard();
                }
                return;
            }
            Uri treeUri = intent.getData();
            if (treeUri != null) {
                DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);
                if (pickedDir.canWrite()) {
                    logDebug("sd card uri is " + treeUri);
                    dbH.setSDCardUri(treeUri.toString());
                    SDCardOperator sdCardOperator = null;
                    try {
                        sdCardOperator = new SDCardOperator(context);
                    } catch (SDCardOperator.SDCardException e) {
                        e.printStackTrace();
                        logError("SDCardOperator initialize failed", e);
                    }
                    if (sdCardOperator != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            toSelectFolder(sdCardOperator.getSDCardRoot());
                        } else {
                            String path = getFullPathFromTreeUri(treeUri, context);
                            dbH.setStorageDownloadLocation(path);
                            downloadLocationPath = path;
                            if (downloadLocation != null) {
                                downloadLocation.setSummary(path);
                            }
                        }
                    } else {
                        onCannotWriteOnSDCard();
                    }
                }
            } else {
                logDebug("tree uri is null!");
                onCannotWriteOnSDCard();
            }
        } else if (requestCode == REQUEST_DOWNLOAD_FOLDER && resultCode == Activity.RESULT_OK && intent != null) {
            String path = intent.getStringExtra(FileStorageActivityLollipop.EXTRA_PATH);
            dbH.setStorageDownloadLocation(path);
            downloadLocationPath = path;
            if (downloadLocation != null) {
                downloadLocation.setSummary(path);
            }
        }
    }

    private void onCannotWriteOnSDCard() {
        showSnackbar(context, getString(R.string.no_external_SD_card_detected));
        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                toSelectFolder(null);
            }
        }, 2000);
    }

    public void toSelectFolder(String sdRoot) {
        logDebug("intent to FileStorageActivityLollipop");
        Intent intent = new Intent(context, FileStorageActivityLollipop.class);
        intent.setAction(FileStorageActivityLollipop.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, true);
        if (sdRoot != null) {
            intent.putExtra(FileStorageActivityLollipop.EXTRA_SD_ROOT, sdRoot);
        }
        startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            logDebug("read and write to external storage have granted.");
            if (requestCode == STORAGE_DOWNLOAD_LOCATION_INTERNAL_SD_CARD) {
                toSelectFolder(null);
            }
            if (requestCode == STORAGE_DOWNLOAD_LOCATION_EXTERNAL_SD_CARD) {
                showSelectDownloadLocationDialog();
            }
        } else {
            showSnackbar(context, getString(R.string.download_requires_permission));
        }
    }

    private void showSelectDownloadLocationDialog() {
        SelectDownloadLocationDialog selector = new SelectDownloadLocationDialog(context, SelectDownloadLocationDialog.From.SETTINGS);
        selector.setIsDefaultLocation(true);
        selector.setSettingsFragment(this);
        selector.show();
    }

    private boolean hasStoragePermission() {
        boolean writePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean readPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return writePermission && readPermission;
    }
}
