package mega.privacy.android.app.presentation.meeting.chat.extension

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.model.messages.InviteMultipleUsersAsContactResult
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InviteMultiUserAsContactResultToStringTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun `test that SomeAlreadyRequestedSomeSent toString works properly `() {
        val expectedSent = 2
        val expectedAlreadyRequested = 1
        assertThat(
            InviteMultipleUsersAsContactResult.SomeAlreadyRequestedSomeSent(
                alreadyRequested = expectedAlreadyRequested,
                sent = expectedSent
            ).toString(context)
        ).isEqualTo(
            context.getString(
                R.string.number_existing_invite_contact_request,
                expectedAlreadyRequested
            ) + context.resources.getQuantityString(
                R.plurals.number_correctly_invite_contact_request, expectedSent, expectedSent
            )
        )
    }

    @Test
    fun `test that AllSent toString works properly`() {
        val expectedSent = 2
        assertThat(
            InviteMultipleUsersAsContactResult.AllSent(
                sent = expectedSent
            ).toString(context)
        ).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.number_correctly_invite_contact_request, expectedSent, expectedSent
            )
        )
    }

    @Test
    fun `test that SomeFailedSomeSent toString works properly`() {
        val expectedSent = 2
        val expectedNotSent = 1
        assertThat(
            InviteMultipleUsersAsContactResult.SomeFailedSomeSent(
                sent = expectedSent,
                notSent = expectedNotSent
            ).toString(context)
        ).isEqualTo(
            context.resources.getQuantityString(
                R.plurals.contact_snackbar_invite_contact_requests_sent,
                expectedSent,
                expectedSent
            ) + context.resources.getQuantityString(
                R.plurals.contact_snackbar_invite_contact_requests_not_sent,
                expectedNotSent,
                expectedNotSent
            )
        )
    }
}