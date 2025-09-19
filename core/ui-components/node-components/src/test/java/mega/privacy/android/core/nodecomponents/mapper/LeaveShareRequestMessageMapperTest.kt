package mega.privacy.android.core.nodecomponents.mapper

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LeaveShareRequestMessageMapperTest {

    private val mContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val leaveShareRequestMessageMapper = LeaveShareRequestMessageMapper(mContext)

    @Test
    fun `test that getResultText should return correct message when there are 2 move request and all failed`() {
        testAllFailedScenario(2)
    }

    @Test
    fun `test that getResultText should return correct message when there are 4 move request and all failed`() {
        testAllFailedScenario(4)
    }

    @Test
    fun `test that getResultText should return correct message when there are 6 move request and all failed`() {
        testAllFailedScenario(6)
    }

    private fun testAllFailedScenario(value: Int) {
        val underTest =
            leaveShareRequestMessageMapper(
                MoveRequestResult.DeleteMovement(
                    count = value,
                    errorCount = value,
                    nodes = emptyList()
                )
            )
        val expected = mContext.getString(
            sharedResR.string.leave_shared_folder_failed_message
        )

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that when there are 1 move success and 1 move failed should return correct message`() {
        val underTest =
            leaveShareRequestMessageMapper(
                MoveRequestResult.DeleteMovement(
                    count = 2,
                    errorCount = 1,
                    nodes = emptyList()
                )
            )
        val expected = mContext.resources.getQuantityString(
            sharedResR.plurals.leave_shared_folder_partial_success_snackbar_message,
            2,
            2
        )

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when there is leave request and is successful `() {
        val underTest = leaveShareRequestMessageMapper(
            MoveRequestResult.DeleteMovement(
                count = 1,
                errorCount = 0,
                nodes = emptyList()
            )
        )
        val expected =
            mContext.resources.getQuantityString(
                sharedResR.plurals.shared_items_incoming_shares_snackbar_leaving_shares_success,
                1,
                1
            )

        assertThat(underTest).isEqualTo(expected)
    }
}
