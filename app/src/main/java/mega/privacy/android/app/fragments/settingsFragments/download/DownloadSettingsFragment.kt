package mega.privacy.android.app.fragments.settingsFragments.download

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.TwoLineCheckPreference
import mega.privacy.android.app.constants.SettingsConstants.KEY_STORAGE_ASK_ME_ALWAYS
import mega.privacy.android.app.constants.SettingsConstants.KEY_STORAGE_DOWNLOAD_LOCATION
import mega.privacy.android.app.main.FileStorageActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.TextUtil
import mega.privacy.android.data.database.DatabaseHandler
import mega.privacy.android.data.model.MegaPreferences
import timber.log.Timber

/**
 * The fragment for download settings
 */
class DownloadSettingsFragment : PreferenceFragmentCompat(),
    Preference.OnPreferenceClickListener,
    Preference.OnPreferenceChangeListener {
    private var downloadLocation: Preference? = null
    private var storageAskMeAlways: TwoLineCheckPreference? = null
    private var askMe = false
    private var downloadLocationPath: String? = null
    private val dbH: DatabaseHandler = MegaApplication.getInstance().dbH
    private val prefs: MegaPreferences? = dbH.preferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_download)
        downloadLocation = findPreference(KEY_STORAGE_DOWNLOAD_LOCATION)
        storageAskMeAlways = findPreference(KEY_STORAGE_ASK_ME_ALWAYS)

        downloadLocation?.onPreferenceClickListener = this
        storageAskMeAlways?.onPreferenceClickListener = this
        if (prefs?.storageAskAlways == null) {
            askMe = true
            dbH.setStorageAskAlways(true)
        } else {
            askMe = java.lang.Boolean.parseBoolean(prefs.storageAskAlways)
        }
        storageAskMeAlways?.isChecked = askMe
        if (askMe) {
            downloadLocation?.let {
                preferenceScreen.removePreference(it)
            }
        }
        if (TextUtil.isTextEmpty(prefs?.storageDownloadLocation)) {
            resetDefaultDownloadLocation()
        } else {
            downloadLocationPath = prefs?.storageDownloadLocation
        }
        setDownloadLocation()
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            KEY_STORAGE_ASK_ME_ALWAYS -> {
                storageAskMeAlways?.let {
                    dbH.setStorageAskAlways(it.isChecked.also { isChecked -> askMe = isChecked })
                }
                downloadLocation?.let {
                    if (askMe) {
                        preferenceScreen.removePreference(it)
                    } else {
                        resetDefaultDownloadLocation()
                        preferenceScreen.addPreference(it)
                    }
                }
                setDownloadLocation()
            }
            KEY_STORAGE_DOWNLOAD_LOCATION -> toSelectFolder()
        }
        return true
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean = false

    private fun resetDefaultDownloadLocation() {
        try {
            val defaultDownloadLocation = FileUtil.buildDefaultDownloadDir(context)
            downloadLocationPath = defaultDownloadLocation.absolutePath
        } catch (e: RuntimeException) {
            Timber.e(e)
        }
    }

    private fun setDownloadLocation() {
        dbH.setStorageDownloadLocation(downloadLocationPath)
        prefs?.storageDownloadLocation = downloadLocationPath
        downloadLocation?.run {
            summary = downloadLocationPath
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == Constants.REQUEST_DOWNLOAD_FOLDER && intent != null) {
            if (resultCode == Activity.RESULT_OK) {
                downloadLocationPath = intent.getStringExtra(FileStorageActivity.EXTRA_PATH)
                setDownloadLocation()
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Timber.d("REQUEST_DOWNLOAD_FOLDER - canceled")
            }
        }
    }

    private fun toSelectFolder() {
        val intent = Intent(context, FileStorageActivity::class.java)
        intent.action = FileStorageActivity.Mode.PICK_FOLDER.action
        intent.putExtra(
            FileStorageActivity.PICK_FOLDER_TYPE,
            FileStorageActivity.PickFolderType.DOWNLOAD_FOLDER.folderType
        )
        startActivityForResult(intent, Constants.REQUEST_DOWNLOAD_FOLDER)
    }
}