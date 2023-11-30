package mega.privacy.android.app.main.dialog.contactlink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.BaseActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialog
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import nz.mega.sdk.MegaChatApiJava
import javax.inject.Inject

@AndroidEntryPoint
internal class ContactLinkDialogFragment : DialogFragment() {
    private val viewModel: ContactLinkViewModel by viewModels()

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var inviteContactRequestStringMapper: InviteContactRequestStringMapper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val state by viewModel.state.collectAsStateWithLifecycle()
                val themeMode by getThemeMode().collectAsStateWithLifecycle(initialValue = ThemeMode.System)

                state.contactLinkResult?.let { result ->
                    if (result.isSuccess) {
                        val contactLink = result.getOrThrow()
                        val contactLinkHandle = contactLink.contactLinkHandle
                        val email = contactLink.email
                        val fullName = contactLink.fullName
                        if (contactLinkHandle != null && !email.isNullOrEmpty() && !fullName.isNullOrEmpty()) {
                            val confirmButtonText = if (contactLink.isContact) {
                                stringResource(id = R.string.contact_view)
                            } else {
                                stringResource(id = R.string.contact_invite)
                            }
                            val message = if (contactLink.isContact) {
                                stringResource(id = R.string.context_contact_already_exists, email)
                            } else {
                                email
                            }
                            MegaAppTheme(isDark = themeMode.isDarkMode()) {
                                ConfirmationDialog(
                                    title = contactLink.fullName.orEmpty(),
                                    text = message,
                                    confirmButtonText = confirmButtonText,
                                    cancelButtonText = stringResource(id = R.string.general_cancel),
                                    onConfirm = {
                                        if (contactLink.isContact) {
                                            ContactUtil.openContactInfoActivity(
                                                requireContext(),
                                                email
                                            )
                                            dismissAllowingStateLoss()
                                        } else {
                                            viewModel.sendContactInvitation(
                                                contactLinkHandle,
                                                email
                                            )
                                        }
                                    },
                                    onDismiss = { dismissAllowingStateLoss() })
                            }
                        } else {
                            dismissAllowingStateLoss()
                        }
                    } else {
                        dismissAllowingStateLoss()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.collectFlow(viewModel.state) {
            val sentInviteResult = it.sentInviteResult
            val contactLinkResult = it.contactLinkResult
            if (sentInviteResult != null) {
                if (sentInviteResult.isSuccess) {
                    showMessage(
                        inviteContactRequestStringMapper(
                            sentInviteResult.getOrThrow(),
                            contactLinkResult?.getOrNull()?.email.orEmpty()
                        )
                    )
                } else {
                    showMessage(getString(R.string.general_error))
                }
                dismissAllowingStateLoss()
            }
        }
    }

    private fun showMessage(message: String) {
        (activity as? BaseActivity)?.showSnackbar(
            Constants.SNACKBAR_TYPE,
            message,
            MegaChatApiJava.MEGACHAT_INVALID_HANDLE
        )
    }

    companion object {
        const val TAG = "ContactLinkFragment"
        const val EXTRA_USER_HANDLE = "EXTRA_USER_HANDLE"

        fun newInstance(userHandle: Long) = ContactLinkDialogFragment().apply {
            arguments = bundleOf(EXTRA_USER_HANDLE to userHandle)
        }
    }
}