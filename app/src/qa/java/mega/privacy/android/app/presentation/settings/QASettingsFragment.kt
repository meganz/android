package mega.privacy.android.app.presentation.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.extensions.canBeHandled
import mega.privacy.android.app.presentation.featureflag.FeatureFlagActivity
import mega.privacy.android.app.utils.Constants
import java.io.File

@AndroidEntryPoint
class QASettingsFragment : PreferenceFragmentCompat() {
    val viewModel by viewModels<QASettingViewModel>()

    private val checkForUpdatesPreferenceKey = "settings_qa_check_update"
    private val exportLogsPreferenceKey = "settings_qa_export_logs"
    private val saveLogsPreferenceKey = "settings_qa_save_logs"
    private val featureFlagsPreferenceKey = "settings_qa_feature_flags"

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_qa, rootKey)
    }


    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        return when (preference.key) {
            checkForUpdatesPreferenceKey -> {
                viewModel.checkUpdatePressed()
                true
            }

            exportLogsPreferenceKey -> {
                viewModel.exportLogs(::sendShareLogFileIntent)
                true
            }

            saveLogsPreferenceKey -> {
                viewModel.exportLogs(::sendViewLogFileIntent)
                true
            }

            featureFlagsPreferenceKey -> {
                startActivity(Intent(requireContext(), FeatureFlagActivity::class.java))
                true
            }

            else -> super.onPreferenceTreeClick(preference)
        }
    }

    private fun sendShareLogFileIntent(logFile: File) = Intent(Intent.ACTION_SEND).apply {
        val uri = getLogFileUri(logFile)
        putExtra(Intent.EXTRA_TITLE, "Send log file")
        putExtra(Intent.EXTRA_SUBJECT, "Mega Log")
        uri?.let<Uri, Unit> {
            type = getMimeType(logFile)
            putExtra(Intent.EXTRA_STREAM, it)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.let {
        if (it.canBeHandled(requireContext())) {
            startActivity(it)
        }
    }

    private fun sendViewLogFileIntent(logFile: File) = Intent(Intent.ACTION_VIEW).apply {
        val uri = getLogFileUri(logFile)
        uri?.let {
            type = getMimeType(logFile)
            putExtra(Intent.EXTRA_STREAM, it)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.let {
        if (it.canBeHandled(requireContext())) {
            startActivity(it)
        }
    }

    private fun getMimeType(logFile: File) = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(logFile.extension)

    private fun getLogFileUri(
        file: File,
    ) = context?.let {
        FileProvider.getUriForFile(
            it,
            Constants.AUTHORITY_STRING_FILE_PROVIDER,
            file
        )
    }
}