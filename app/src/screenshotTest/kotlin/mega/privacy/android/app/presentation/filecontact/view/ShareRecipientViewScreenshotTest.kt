package mega.privacy.android.app.presentation.filecontact.view

import androidx.compose.runtime.Composable
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.ShareRecipient
import mega.privacy.android.domain.entity.user.UserVisibility

/**
 * Component-level baselines for [ShareRecipientView]. Captures the variants
 * that exercise the bespoke avatar/status/permission rendering before the
 * migration to the shared `:shared:contact` `ContactItemView`. Goldens
 * recorded against the pre-migration rendering are used to detect any
 * unintended regressions when the view is replaced.
 *
 * Single-character display names are used so the underlying avatar-letter
 * extraction skips the emoji-parsing code path (which fails to initialize
 * inside the layoutlib render JVM used for screenshot tests).
 */
class ShareRecipientViewScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareRecipientViewContactOnlineVerified() {
        AndroidThemeForPreviews {
            ShareRecipientView(
                shareRecipient = contact(
                    displayName = "A",
                    isVerified = true,
                    status = UserChatStatus.Online,
                    permission = AccessPermission.READ,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareRecipientViewContactAwayUnverified() {
        AndroidThemeForPreviews {
            ShareRecipientView(
                shareRecipient = contact(
                    displayName = "B",
                    isVerified = false,
                    status = UserChatStatus.Away,
                    permission = AccessPermission.READWRITE,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareRecipientViewContactBusyVerified() {
        AndroidThemeForPreviews {
            ShareRecipientView(
                shareRecipient = contact(
                    displayName = "C",
                    isVerified = true,
                    status = UserChatStatus.Busy,
                    permission = AccessPermission.FULL,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareRecipientViewContactOfflineUnverified() {
        AndroidThemeForPreviews {
            ShareRecipientView(
                shareRecipient = contact(
                    displayName = "D",
                    isVerified = false,
                    status = UserChatStatus.Offline,
                    permission = AccessPermission.READ,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareRecipientViewContactSelected() {
        AndroidThemeForPreviews {
            ShareRecipientView(
                shareRecipient = contact(
                    displayName = "A",
                    isVerified = true,
                    status = UserChatStatus.Online,
                    permission = AccessPermission.READ,
                ),
                selected = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareRecipientViewNonContact() {
        AndroidThemeForPreviews {
            ShareRecipientView(
                shareRecipient = ShareRecipient.NonContact(
                    email = "n",
                    permission = AccessPermission.READWRITE,
                    isPending = false,
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ShareRecipientViewNonContactSelected() {
        AndroidThemeForPreviews {
            ShareRecipientView(
                shareRecipient = ShareRecipient.NonContact(
                    email = "n",
                    permission = AccessPermission.READ,
                    isPending = true,
                ),
                selected = true,
            )
        }
    }

    private fun contact(
        displayName: String,
        isVerified: Boolean,
        status: UserChatStatus,
        permission: AccessPermission,
    ): ShareRecipient.Contact = ShareRecipient.Contact(
        handle = displayName.hashCode().toLong(),
        email = "${displayName.lowercase()}@mega.io",
        contactData = ContactData(
            fullName = displayName,
            alias = null,
            avatarUri = null,
            userVisibility = UserVisibility.Visible,
        ),
        isVerified = isVerified,
        permission = permission,
        isPending = false,
        status = status,
        defaultAvatarColor = 0xFF2E7D32.toInt(),
    )
}
