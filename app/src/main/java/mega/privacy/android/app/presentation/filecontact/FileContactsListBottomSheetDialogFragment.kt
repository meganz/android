package mega.privacy.android.app.presentation.filecontact

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.modalbottomsheet.BaseBottomSheetDialogFragment
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.fileinfo.view.ShareContactOptionsContent
import mega.privacy.android.app.presentation.fileinfo.view.ShareNonContactOptionsContent
import mega.privacy.android.app.utils.AvatarUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.ContactUtil
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.user.UserId
import mega.privacy.android.domain.repository.ContactsRepository
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import nz.mega.sdk.MegaUser
import timber.log.Timber
import javax.inject.Inject

/**
 * FileContactsListBottomSheetDialogFragment
 */
@AndroidEntryPoint
@Deprecated(message = "This bottom sheet should be replaced by direct use [ShareContactOptionsContent] in a compose bottom sheet in a compose screen, is left here until [FileContactListActivity] is migrated to compose.")
class FileContactsListBottomSheetDialogFragment : BaseBottomSheetDialogFragment {
    private var contact: MegaUser? = null
    private var share: MegaShare? = null
    private var nonContactEmail: String? = null
    private var node: MegaNode? = null
    private var listener: FileContactsListBottomSheetDialogListener? = null

    /**
     * Get theme mode
     */
    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    /**
     * Contacts repository
     */
    @Inject
    lateinit var contactsRepository: ContactsRepository

    /**
     * This constructor shouldn't be used, is just here to avoid crashes on recreation. Fragment will be automatically dismissed.
     * This is a temporal fix while this dialog is migrated to compose
     *
     */
    @Deprecated("use other constructor")
    constructor()

    constructor(
        share: MegaShare?,
        contact: MegaUser?,
        node: MegaNode?,
        listener: FileContactsListBottomSheetDialogListener?,
    ) {
        this.share = share
        this.contact = contact
        this.node = node
        this.listener = listener
        if (this.contact == null) {
            nonContactEmail = this.share?.user
        }
    }

    /**
     * On create
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (share == null || node == null || listener == null) {
            dismissAllowingStateLoss()
        }
    }

    /**
     * On create view
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        if (contact == null) {
            contact = megaApi.getContact(share?.user ?: "")
        }
        if (this.contact == null) {
            nonContactEmail = share?.user
        }

        if (savedInstanceState != null) {
            val email = savedInstanceState.getString(Constants.EMAIL)
            if (email != null) {
                contact = megaApi.getContact(email)
                if (contact == null) {
                    nonContactEmail = email
                }
            }
        }

        val isNonContact = nonContactEmail != null
        val columnModifier = if (isNonContact) {
            Modifier.fillMaxWidth()
        } else {
            Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp)
        }

        contentView = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var contactPermission by remember {
                    mutableStateOf<ContactPermission?>(null)
                }
                LaunchedEffect(share?.user) {
                    contactsRepository.getContactItem(UserId(contact?.handle ?: -1L), false)?.let {
                        contactPermission = ContactPermission(
                            contactItem = it,
                            accessPermission = getAccessPermission(share?.access),
                        )
                    } ?: run {
                        Timber.Forest.e("Contact item not found ${contact?.handle}")
                    }
                }
                val themeMode by monitorThemeModeUseCase()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                OriginalTheme(isDark = themeMode.isDarkMode()) {
                    Column(
                        modifier = columnModifier
                    ) {
                        contactPermission?.let { contactPermission ->
                            ShareContactOptionsContent(
                                contactPermission = contactPermission,
                                allowChangePermission = node == null || !megaApi.isInVault(node),
                                onInfoClicked = {
                                    email()?.let {
                                        ContactUtil.openContactInfoActivity(requireActivity(), it)
                                    }
                                    dismissAllowingStateLoss()
                                },
                                onChangePermissionClicked = {
                                    email()?.let { listener?.changePermissions(it) }
                                    dismissAllowingStateLoss()
                                },
                                onRemoveClicked = {
                                    email()?.let { listener?.removeFileContactShare(it) }
                                    dismissAllowingStateLoss()
                                })
                        } ?: nonContactEmail?.let {
                            ShareNonContactOptionsContent(
                                nonContactEmail = it,
                                accessPermission = getAccessPermission(share?.access),
                                avatarColor = AvatarUtil.getSpecificAvatarColor(Constants.AVATAR_PRIMARY_COLOR),
                                allowChangePermission = node == null || !megaApi.isInVault(node),
                                onChangePermissionClicked = {
                                    email()?.let { listener?.changePermissions(it) }
                                    dismissAllowingStateLoss()
                                },
                                onRemoveClicked = {
                                    email()?.let { listener?.removeFileContactShare(it) }
                                    dismissAllowingStateLoss()
                                })
                        }
                    }
                }
            }
        }
        return contentView
    }

    /**
     * On dismiss
     */
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        listener?.fileContactsDialogDismissed()
    }

    private fun email() = contact?.email ?: nonContactEmail

    private fun getAccessPermission(intRawValue: Int?) = when (intRawValue) {
        MegaShare.ACCESS_READ -> AccessPermission.READ
        MegaShare.ACCESS_READWRITE -> AccessPermission.READWRITE
        MegaShare.ACCESS_FULL -> AccessPermission.FULL
        MegaShare.ACCESS_OWNER -> AccessPermission.OWNER
        else -> AccessPermission.UNKNOWN
    }

    /**
     * On save instance state
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val email = contact?.email ?: nonContactEmail
        outState.putString(Constants.EMAIL, email)
    }
}