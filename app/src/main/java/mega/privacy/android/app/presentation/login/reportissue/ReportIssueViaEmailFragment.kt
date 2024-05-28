package mega.privacy.android.app.presentation.login.reportissue

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.presentation.extensions.canBeHandled
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.login.LoginActivity
import mega.privacy.android.app.presentation.login.model.LoginFragmentType
import mega.privacy.android.app.presentation.login.reportissue.view.ReportIssueViaEmailView
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.support.SupportEmailTicket
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import timber.log.Timber
import javax.inject.Inject

/**
 * Report issue via email fragment
 *
 */
@AndroidEntryPoint
class ReportIssueViaEmailFragment : Fragment() {

    private val viewModel: ReportIssueViaEmailViewModel by viewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            val themeMode by getThemeMode()
                .collectAsState(initial = ThemeMode.System)
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                ReportIssueViaEmailView(
                    uiState = uiState,
                    onDescriptionChanged = viewModel::setDescription,
                    onIncludeLogsChanged = viewModel::setIncludeLogs,
                    onBackPress = {
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    },
                    onDiscard = {
                        goToLoginScreen()
                    },
                    onSubmit = viewModel::submit
                )
            }

            EventEffect(
                event = uiState.sendEmailEvent,
                onConsumed = viewModel::onSendEmailEventConsumed,
            ) { supportEmailTicket ->
                sendSupportEmail(supportEmailTicket)
                goToLoginScreen()
            }
        }
    }

    private fun goToLoginScreen() {
        (requireActivity() as LoginActivity).showFragment(LoginFragmentType.Login)
    }

    private fun sendSupportEmail(ticket: SupportEmailTicket) {
        runCatching {
            val fileUri = getLogFileUri(ticket)
            val emailIntent = getEmailIntent(ticket, fileUri)
            if (emailIntent.canBeHandled(requireContext())) {
                startActivity(emailIntent)
            }
        }.onFailure {
            Timber.e(it)
        }
    }

    private fun getLogFileUri(
        ticket: SupportEmailTicket,
    ) = ticket.logs?.let { file ->
        context?.let {
            FileProvider.getUriForFile(
                it,
                Constants.AUTHORITY_STRING_FILE_PROVIDER,
                file
            )
        }
    }

    private fun getEmailIntent(
        ticket: SupportEmailTicket,
        fileUri: Uri?,
    ) = Intent(Intent.ACTION_SEND).apply {
        type = "message/rfc822"
        putExtra(Intent.EXTRA_EMAIL, arrayOf(ticket.email))
        putExtra(Intent.EXTRA_SUBJECT, ticket.subject)
        putExtra(Intent.EXTRA_TEXT, ticket.ticket)
        fileUri?.let<Uri, Unit> {
            putExtra(Intent.EXTRA_STREAM, it)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}