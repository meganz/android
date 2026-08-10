package mega.privacy.android.app.presentation.notification.model.extensions

import android.content.Context
import android.content.res.Resources
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
class TitleTest {

    private val context = mock<Context>()
    private val resources = mock<Resources>()

    @BeforeEach
    fun cleanUp() {
        reset(context, resources)
        whenever(context.resources) doReturn resources
    }

    @Test
    fun `test that payment succeeded alert title is the localised resource formatted with the plan name`() {
        whenever(
            context.getString(sharedR.string.notifications_payment_succeeded_title, PLAN_NAME)
        ) doReturn EXPECTED

        val actual = paymentSucceededAlert(planName = PLAN_NAME).title()(context)

        assertThat(actual).isEqualTo(EXPECTED)
    }

    @Test
    fun `test that payment failed alert title is the localised resource formatted with the plan name`() {
        whenever(
            context.getString(sharedR.string.notifications_payment_failed_title, PLAN_NAME)
        ) doReturn EXPECTED

        val actual = paymentFailedAlert(planName = PLAN_NAME).title()(context)

        assertThat(actual).isEqualTo(EXPECTED)
    }

    @Test
    fun `test that payment reminder alert title uses the will expire plural when the plan has not expired yet`() {
        val nowInSeconds = System.currentTimeMillis() / 1000
        val daysRemaining = 5
        val endTimestamp = nowInSeconds + daysRemaining * SECONDS_IN_A_DAY + HALF_DAY_MARGIN
        whenever(
            resources.getQuantityString(
                sharedR.plurals.notifications_payment_reminder_will_expire,
                daysRemaining,
                daysRemaining,
            )
        ) doReturn EXPECTED

        val actual = paymentReminderAlert(endTimestamp = endTimestamp).title()(context)

        assertThat(actual).isEqualTo(EXPECTED)
    }

    @Test
    fun `test that payment reminder alert title uses the expired plural when the plan has already expired`() {
        val nowInSeconds = System.currentTimeMillis() / 1000
        val daysAgo = 3
        val endTimestamp = nowInSeconds - daysAgo * SECONDS_IN_A_DAY - HALF_DAY_MARGIN
        whenever(
            resources.getQuantityString(
                sharedR.plurals.notifications_payment_reminder_expired,
                daysAgo,
                daysAgo,
            )
        ) doReturn EXPECTED

        val actual = paymentReminderAlert(endTimestamp = endTimestamp).title()(context)

        assertThat(actual).isEqualTo(EXPECTED)
    }

    private fun paymentSucceededAlert(planName: String?) = PaymentSucceededAlert(
        id = ID,
        seen = false,
        createdTime = CREATED_TIME,
        isOwnChange = false,
        heading = SDK_HEADING,
        planName = planName,
    )

    private fun paymentFailedAlert(planName: String?) = PaymentFailedAlert(
        id = ID,
        seen = false,
        createdTime = CREATED_TIME,
        isOwnChange = false,
        heading = SDK_HEADING,
        planName = planName,
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
        const val HALF_DAY_MARGIN = 43200L
        const val EXPECTED = "expected"
        const val PLAN_NAME = "Pro I"
        const val SDK_HEADING = "hardcoded SDK heading"
    }
}
