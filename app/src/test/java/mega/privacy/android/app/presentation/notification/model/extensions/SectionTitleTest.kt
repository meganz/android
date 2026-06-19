package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.PaymentFailedAlert
import mega.privacy.android.domain.entity.PaymentReminderAlert
import mega.privacy.android.domain.entity.PaymentSucceededAlert
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SectionTitleTest {

    private val context = mock<Context>()

    @BeforeEach
    fun cleanUp() {
        reset(context)
        whenever(context.getString(sharedR.string.notifications_payment_info_section)) doReturn PAYMENT_INFO
        whenever(context.getString(sharedR.string.notifications_payment_reminder_section)) doReturn REMINDER_EXPIRING
        whenever(context.getString(sharedR.string.notifications_payment_reminder_expired_section)) doReturn REMINDER_EXPIRED
    }

    @Test
    fun `test that payment succeeded alert section title uses the payment info resource and not the SDK heading`() {
        val actual = paymentSucceededAlert().sectionTitle()(context)

        assertThat(actual).isEqualTo(PAYMENT_INFO)
    }

    @Test
    fun `test that payment failed alert section title uses the payment info resource and not the SDK heading`() {
        val actual = paymentFailedAlert().sectionTitle()(context)

        assertThat(actual).isEqualTo(PAYMENT_INFO)
    }

    @Test
    fun `test that payment reminder alert section title uses the expiring soon resource when the plan has not expired yet`() {
        val notExpired = System.currentTimeMillis() / 1000 + 5 * SECONDS_IN_A_DAY

        val actual = paymentReminderAlert(endTimestamp = notExpired).sectionTitle()(context)

        assertThat(actual).isEqualTo(REMINDER_EXPIRING)
    }

    @Test
    fun `test that payment reminder alert section title uses the expired resource when the plan has already expired`() {
        val expired = System.currentTimeMillis() / 1000 - 3 * SECONDS_IN_A_DAY

        val actual = paymentReminderAlert(endTimestamp = expired).sectionTitle()(context)

        assertThat(actual).isEqualTo(REMINDER_EXPIRED)
    }

    private fun paymentSucceededAlert() = PaymentSucceededAlert(
        id = ID,
        seen = false,
        createdTime = CREATED_TIME,
        isOwnChange = false,
        heading = SDK_HEADING,
        planName = PLAN_NAME,
    )

    private fun paymentFailedAlert() = PaymentFailedAlert(
        id = ID,
        seen = false,
        createdTime = CREATED_TIME,
        isOwnChange = false,
        heading = SDK_HEADING,
        planName = PLAN_NAME,
    )

    private fun paymentReminderAlert(endTimestamp: Long) = PaymentReminderAlert(
        id = ID,
        seen = false,
        createdTime = CREATED_TIME,
        isOwnChange = false,
        heading = SDK_HEADING,
        endTimestamp = endTimestamp,
    )

    private companion object {
        const val ID = 1L
        const val CREATED_TIME = 0L
        const val SECONDS_IN_A_DAY = 86400L
        const val PAYMENT_INFO = "payment info section"
        const val REMINDER_EXPIRING = "reminder expiring section"
        const val REMINDER_EXPIRED = "reminder expired section"
        const val PLAN_NAME = "Pro I"
        const val SDK_HEADING = "hardcoded SDK heading"
    }
}
