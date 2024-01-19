package test.mega.privacy.android.app.presentation.movenode.mapper

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.movenode.mapper.LeaveShareRequestMessageMapper
import mega.privacy.android.domain.entity.node.MoveRequestResult
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class LeaveShareRequestMessageMapperTest {

    private val mContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val leaveShareRequestMessageMapper = LeaveShareRequestMessageMapper(mContext)

    @Test
    fun `test that getResultText should return correct message when there are more than 1 move request and all failed`() {
        val mockLeaveShareCount = Random.nextInt(2, 10)
        val underTest =
            leaveShareRequestMessageMapper(
                MoveRequestResult.DeleteMovement(
                    count = mockLeaveShareCount,
                    errorCount = mockLeaveShareCount,
                    nodes = emptyList()
                )
            )
        val expected = mContext.resources.getQuantityString(
            R.plurals.shared_items_incoming_shares_snackbar_leaving_shares_fail,
            mockLeaveShareCount,
            mockLeaveShareCount
        )

        Truth.assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that when there are 1 move success and 1 move failed should return correct message`() {
        val underTest =
            leaveShareRequestMessageMapper(
                MoveRequestResult.DeleteMovement(
                    count = 2,
                    errorCount = 1,
                    nodes = listOf()
                )
            )
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.shared_items_incoming_shares_snackbar_leaving_shares_success_concat,
                2,
                2
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.shared_items_incoming_shares_snackbar_leaving_shares_fail_concat,
                1,
                1
            )
        }"

        Truth.assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when there is leave request and is successful `() {
        val underTest = leaveShareRequestMessageMapper(
            MoveRequestResult.DeleteMovement(
                count = 1,
                errorCount = 0,
                nodes = listOf()
            )
        )
        val expected =
            mContext.resources.getQuantityString(
                R.plurals.shared_items_incoming_shares_snackbar_leaving_shares_success,
                1,
                1
            )

        Truth.assertThat(underTest).isEqualTo(expected)
    }
}