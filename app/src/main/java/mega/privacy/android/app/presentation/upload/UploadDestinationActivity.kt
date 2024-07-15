package mega.privacy.android.app.presentation.upload

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.extensions.parcelable
import mega.privacy.android.app.presentation.extensions.parcelableArrayList
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * Activity to upload files
 */
@AndroidEntryPoint
class UploadDestinationActivity : AppCompatActivity() {

    /**
     * Application Theme Mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    /**
     * PasscodeCryptObjectFactory
     */
    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    /**
     * ViewModel for [UploadDestinationActivity]
     */
    private val uploadDestinationViewModel: UploadDestinationViewModel by viewModels()

    /**
     * OnCreate
     *
     * @param savedInstanceState
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent()
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uploadActivityUiState by uploadDestinationViewModel.uiState.collectAsStateWithLifecycle()
            val isNewUploadActivityEnabled = uploadActivityUiState.isNewUploadScreenEnabled
            if (isNewUploadActivityEnabled != null) {
                if (isNewUploadActivityEnabled) {
                    SessionContainer(shouldCheckChatSession = true) {
                        OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                            PasscodeContainer(
                                passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                                content = {
                                    UploadView()
                                }
                            )
                        }
                    }
                } else {
                    finish()
                    startActivity(
                        intent.setClass(this, FileExplorerActivity::class.java)
                    )
                }
            }
        }
    }

    private fun handleIntent() = when (intent?.action) {
        Intent.ACTION_SEND -> when {
            intent.type == Constants.TYPE_TEXT_PLAIN
                    && intent.extras?.containsKey(Intent.EXTRA_STREAM)?.not() ?: true ->
                handleSendText(intent)

            else -> handleSendFiles(intent)
        }

        Intent.ACTION_SEND_MULTIPLE -> handleSendMultipleFiles(intent) // Handle multiple images being sent
        else ->
            Timber.e("handleIntent: No action ${intent.action}")
    }

    private fun handleSendText(intent: Intent) {
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
            Timber.d("Upload activity handleSendText: $it")
        }
    }

    private fun handleSendFiles(intent: Intent) {
        (intent.parcelable<Parcelable>(Intent.EXTRA_STREAM) as? Uri)?.let {
            Timber.d("Upload activity handleSendImage: $it")
        }
    }

    private fun handleSendMultipleFiles(intent: Intent) {
        intent.parcelableArrayList<Parcelable>(Intent.EXTRA_STREAM)?.let {
            Timber.d("Upload activity handleSendMultipleImages: $it")
        }
    }
}