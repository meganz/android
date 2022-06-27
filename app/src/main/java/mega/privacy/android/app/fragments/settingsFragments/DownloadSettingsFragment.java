package mega.privacy.android.app.fragments.settingsFragments;

import static mega.privacy.android.app.constants.SettingsConstants.KEY_STORAGE_ASK_ME_ALWAYS;
import static mega.privacy.android.app.constants.SettingsConstants.KEY_STORAGE_DOWNLOAD_LOCATION;
import static mega.privacy.android.app.utils.Constants.REQUEST_DOWNLOAD_FOLDER;
import static mega.privacy.android.app.utils.FileUtil.buildDefaultDownloadDir;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;

import java.io.File;

import mega.privacy.android.app.R;
import mega.privacy.android.app.components.TwoLineCheckPreference;
import mega.privacy.android.app.main.FileStorageActivity;
import timber.log.Timber;

public class DownloadSettingsFragment extends SettingsBaseFragment {

    private Preference downloadLocation;
    private TwoLineCheckPreference storageAskMeAlways;

    private boolean askMe;
    private String downloadLocationPath = "";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_download);

        downloadLocation = findPreference(KEY_STORAGE_DOWNLOAD_LOCATION);
        downloadLocation.setOnPreferenceClickListener(this);

        storageAskMeAlways = findPreference(KEY_STORAGE_ASK_ME_ALWAYS);
        storageAskMeAlways.setOnPreferenceClickListener(this);

        if (prefs.getStorageAskAlways() == null) {
            askMe = true;
            dbH.setStorageAskAlways(true);
        } else {
            askMe = Boolean.parseBoolean(prefs.getStorageAskAlways());
        }

        storageAskMeAlways.setChecked(askMe);
        if (askMe) {
            getPreferenceScreen().removePreference(downloadLocation);
        }

        if (isTextEmpty(prefs.getStorageDownloadLocation())) {
            resetDefaultDownloadLocation();
        } else {
            downloadLocationPath = prefs.getStorageDownloadLocation();
        }

        setDownloadLocation();
    }

    private void resetDefaultDownloadLocation() {
        File defaultDownloadLocation = buildDefaultDownloadDir(context);
        downloadLocationPath = defaultDownloadLocation.getAbsolutePath();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {

        switch (preference.getKey()) {
            case KEY_STORAGE_ASK_ME_ALWAYS:
                dbH.setStorageAskAlways(askMe = storageAskMeAlways.isChecked());
                if (askMe) {
                    getPreferenceScreen().removePreference(downloadLocation);
                } else {
                    resetDefaultDownloadLocation();
                    getPreferenceScreen().addPreference(downloadLocation);
                }
                setDownloadLocation();
                break;

            case KEY_STORAGE_DOWNLOAD_LOCATION:
                toSelectFolder();
                break;
        }

        return true;
    }

    private void setDownloadLocation() {
        dbH.setStorageDownloadLocation(downloadLocationPath);
        prefs.setStorageDownloadLocation(downloadLocationPath);

        if (downloadLocation != null) {
            downloadLocation.setSummary(downloadLocationPath);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_DOWNLOAD_FOLDER && intent != null) {
            if (resultCode == Activity.RESULT_OK) {
                downloadLocationPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH);
                setDownloadLocation();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Timber.d("REQUEST_DOWNLOAD_FOLDER - canceled");
            }
        }
    }

    private void toSelectFolder() {
        Intent intent = new Intent(context, FileStorageActivity.class);
        intent.setAction(FileStorageActivity.Mode.PICK_FOLDER.getAction());
        intent.putExtra(FileStorageActivity.PICK_FOLDER_TYPE, FileStorageActivity.PickFolderType.DOWNLOAD_FOLDER.getFolderType());
        startActivityForResult(intent, REQUEST_DOWNLOAD_FOLDER);
    }
}
