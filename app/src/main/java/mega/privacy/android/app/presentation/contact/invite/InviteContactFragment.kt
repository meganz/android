package mega.privacy.android.app.presentation.contact.invite

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.components.chatsession.ChatSessionContainer
import mega.privacy.android.app.components.session.SessionContainer
import mega.privacy.android.app.presentation.contact.invite.navigation.InviteContactScreenResult
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.passcode.model.PasscodeCryptObjectFactory
import mega.privacy.android.app.presentation.psa.PsaContainer
import mega.privacy.android.app.presentation.qrcode.QRCodeComposeActivity
import mega.privacy.android.app.presentation.security.check.PasscodeContainer
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * Fragment for displaying the [InviteContactRoute]
 */
@AndroidEntryPoint
class InviteContactFragment : Fragment() {

    /**
     * Current theme
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Passcode crypt object factory
     */
    @Inject
    lateinit var passcodeCryptObjectFactory: PasscodeCryptObjectFactory

    /**
     * Called to have this fragment instantiate its user interface view.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val themeMode by monitorThemeModeUseCase().collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            SessionContainer {
                ChatSessionContainer {
                    OriginalTheme(isDark = themeMode.isDarkMode()) {
                        PasscodeContainer(
                            passcodeCryptObjectFactory = passcodeCryptObjectFactory,
                            loading = {},
                            content = {
                                PsaContainer {
                                    // This is necessary to prevent the viewmodel class from being recreated when the configuration changes.
                                    // This can be removed after we fully migrate to a single activity and Compose navigation.
                                    CompositionLocalProvider(LocalViewModelStoreOwner provides activity as InviteContactActivity) {
                                        InviteContactRoute(
                                            modifier = Modifier.fillMaxSize(),
                                            isDarkMode = themeMode.isDarkMode(),
                                            onNavigateUp = ::setActivityResultAndFinish,
                                            onBackPressed = ::onBackPressed,
                                            onShareContactLink = ::shareContactLink,
                                            onOpenPersonalQRCode = ::initMyQr,
                                            onOpenQRScanner = ::initScanQR,
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun setActivityResultAndFinish(screenResult: InviteContactScreenResult?) {
        screenResult?.let {
            activity?.setResult(
                RESULT_OK,
                Intent().putExtra(it.key, it.totalInvitationsSent)
            )
        }
        activity?.finish()
    }

    private fun onBackPressed() {
        (activity as? InviteContactActivity)?.onBackPressed()
    }

    private fun shareContactLink(contactLink: String) {
        Timber.i("more button clicked - share invitation through other app")
        val message = resources.getString(
            R.string.invite_contacts_to_start_chat_text_message,
            contactLink
        )
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = Constants.TYPE_TEXT_PLAIN
        }
        startActivity(
            Intent.createChooser(
                sendIntent,
                getString(R.string.invite_contact_chooser_title)
            )
        )
    }

    private fun initScanQR() {
        Timber.d("initScanQR")
        Intent(requireActivity(), QRCodeComposeActivity::class.java).apply {
            putExtra(Constants.OPEN_SCAN_QR, true)
            startActivity(this)
        }
    }

    private fun initMyQr() {
        startActivity(Intent(requireActivity(), QRCodeComposeActivity::class.java))
    }
}
