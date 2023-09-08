package mega.privacy.android.app.presentation.qrcode

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import javax.inject.Inject

/**
 * QR code compose activity
 */
@AndroidEntryPoint
class QRCodeComposeActivity : ComponentActivity() {

    private val viewModel: QRCodeViewModel by viewModels()

    /**
     * QR code mapper
     */
    @Inject
    lateinit var qrCodeMapper: QRCodeMapper

    /**
     * Get theme mode
     */
    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val mode by getThemeMode()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            val viewState by viewModel.uiState.collectAsStateWithLifecycle()
            AndroidTheme(isDark = mode.isDarkMode()) {
                QRCodeView(
                    viewState = viewState,
                    onBackPressed = onBackPressedDispatcher::onBackPressed,
                    onDeleteQRCode = viewModel::deleteQRCode,
                    onResetQRCode = viewModel::resetQRCode,
                    onSaveQRCode = { },
                    onScanQrCodeClicked = { viewModel.scanCode(this) },
                    onCopyLinkClicked = viewModel::copyContactLink,
                    onViewContactClicked = ::onViewContact,
                    onInviteContactClicked = viewModel::sendInvite,
                    onResultMessageConsumed = viewModel::resetResultMessage,
                    onScannedContactLinkResultConsumed = viewModel::resetScannedContactLinkResult,
                    onInviteContactResultConsumed = viewModel::resetInviteContactResult,
                    onInviteResultDialogDismiss = viewModel::resetScannedContactEmail,
                    onInviteContactDialogDismiss = viewModel::resetScannedContactAvatar,
                    qrCodeMapper = qrCodeMapper,
                )
            }
        }
        viewModel.createQRCode()
    }

    private fun onViewContact(email: String) {
        ContactUtil.openContactInfoActivity(this, email)
        finish()
    }
}

/**
 * Find the Activity in a given Context.
 */
internal fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}