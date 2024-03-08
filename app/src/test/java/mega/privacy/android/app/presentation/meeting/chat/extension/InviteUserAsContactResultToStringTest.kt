package mega.privacy.android.app.presentation.meeting.chat.extension

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.InviteUserAsContactResult
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class InviteUserAsContactResultToStringTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `test that ContactInviteSent toString returns correctly`() {
        assertThat(InviteUserAsContactResult.ContactInviteSent.toString(context)).isEqualTo(
            context.getString(
                R.string.contact_invited
            )
        )
    }

    @Test
    fun `test that ContactAlreadyInvitedError toString returns correctly`() {
        val email = "a@b.c"
        assertThat(
            InviteUserAsContactResult.ContactAlreadyInvitedError(
                email
            ).toString(context)
        ).isEqualTo(
            context.getString(
                R.string.context_contact_already_invited,
                email
            )
        )
    }

    @Test
    fun `test that OwnEmailAsContactError toString returns correctly`() {
        assertThat(
            InviteUserAsContactResult.OwnEmailAsContactError.toString(context)
        ).isEqualTo(
            context.getString(
                R.string.error_own_email_as_contact
            )
        )
    }

    @Test
    fun `test that GeneralError toString returns correctly`() {
        assertThat(
            InviteUserAsContactResult.GeneralError.toString(context)
        ).isEqualTo(
            context.getString(
                R.string.general_error
            )
        )
    }
}

/**
 * fun InviteUserAsContactResult.toString(context: Context) = when (this) {
 *     is InviteUserAsContactResult.ContactInviteSent -> context.getString(R.string.contact_invited)
 *     is InviteUserAsContactResult.ContactAlreadyInvitedError -> context.getString(
 *         R.string.context_contact_already_invited,
 *         email
 *     )
 *
 *     is InviteUserAsContactResult.OwnEmailAsContactError -> context.getString(R.string.error_own_email_as_contact)
 *     is InviteUserAsContactResult.GeneralError -> context.getString(R.string.general_error)
 * }
 */