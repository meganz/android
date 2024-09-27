package mega.privacy.android.app.presentation.contactinfo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.presentation.contactinfo.view.ContactInfoView
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import nz.mega.sdk.MegaChatApiJava
import javax.inject.Inject


/**
 * Compose contact info activity
 */
@AndroidEntryPoint
class ComposeContactInfoActivity : BaseActivity() {

    /**
     * Application theme mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    private val viewModel by viewModels<ContactInfoViewModel>()

    /**
     * onCreate
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent.extras?.let { getContactData(extras = it) }
        setContent {
            val themeMode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val density = LocalDensity.current.density
            val statusBarHeight = Util.getStatusBarHeight() / density


            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                // A surface container using the 'background' color from the theme
                ContactInfoView(
                    uiState = uiState,
                    onBackPress = onBackPressedDispatcher::onBackPressed,
                    statusBarHeight = statusBarHeight,
                    updateNickName = viewModel::updateNickName,
                    updateNickNameDialogVisibility = viewModel::updateNickNameDialogVisibility,
                )
            }
        }
    }

    private fun getContactData(extras: Bundle) {
        val chatHandle = extras.getLong(Constants.HANDLE, MegaChatApiJava.MEGACHAT_INVALID_HANDLE)
        val userEmailExtra = extras.getString(Constants.NAME)
        viewModel.updateContactInfo(chatHandle, userEmailExtra)
    }
}