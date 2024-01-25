package test.mega.privacy.android.app.presentation.movenode.mapper

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.MoveRequestResult.GeneralMovement
import mega.privacy.android.app.presentation.movenode.mapper.MoveRequestMessageMapper
import mega.privacy.android.domain.entity.node.MoveRequestResult
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class MoveRequestMessageMapperTest {
    private val mContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val generalMovementMessageMapper: MoveRequestMessageMapper =
        MoveRequestMessageMapper(mContext)

    @Test(expected = RuntimeException::class)
    fun `test that getResultText should throw an exception if move request data is 0`() {
        generalMovementMessageMapper(GeneralMovement(count = 0, errorCount = 0))
    }

    @Test
    fun `test that getResultText should return correct message when there is only 1 move request and is successful `() {
        val underTest = generalMovementMessageMapper(GeneralMovement(count = 1, errorCount = 0))
        val expected =
            mContext.resources.getQuantityString(R.plurals.general_move_node_snackbar_success, 1, 1)

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when more than 1 move request and all are successful`() {
        val mockMoveCount = Random.nextInt(2, 10)
        val underTest =
            generalMovementMessageMapper(GeneralMovement(count = mockMoveCount, errorCount = 0))
        val expected = mContext.resources.getQuantityString(
            R.plurals.general_move_node_snackbar_success,
            mockMoveCount,
            mockMoveCount
        )

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that when there is only 1 move request and failed should return correct message`() {
        val underTest =
            generalMovementMessageMapper(GeneralMovement(count = 1, errorCount = 1))
        val expected =
            mContext.resources.getQuantityString(R.plurals.general_move_node_snackbar_fail, 1, 1)

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when there are more than 1 move request and all failed`() {
        val mockMoveCount = Random.nextInt(2, 10)
        val underTest =
            generalMovementMessageMapper(
                GeneralMovement(
                    count = mockMoveCount,
                    errorCount = mockMoveCount
                )
            )
        val expected = mContext.resources.getQuantityString(
            R.plurals.general_move_node_snackbar_fail,
            mockMoveCount,
            mockMoveCount
        )

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that when there are 1 move success and 1 move failed should return correct message`() {
        val underTest =
            generalMovementMessageMapper(GeneralMovement(count = 2, errorCount = 1))
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.general_move_node_snackbar_concat_success,
                1,
                1
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.general_move_node_snackbar_concat_fail,
                1,
                1
            )
        }"

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when there are 1 move success and more than 1 move failed`() {
        val mockMoveCount = Random.nextInt(5, 10)
        val mockErrorCount = mockMoveCount - 1
        val underTest =
            generalMovementMessageMapper(
                GeneralMovement(
                    count = mockMoveCount,
                    errorCount = mockErrorCount
                )
            )
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.general_move_node_snackbar_concat_success,
                1,
                1
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.general_move_node_snackbar_concat_fail,
                mockErrorCount,
                mockErrorCount
            )
        }"

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when there are more than 1 move success and 1 move failed`() {
        val mockMoveCount = Random.nextInt(2, 10)
        val underTest =
            generalMovementMessageMapper(GeneralMovement(count = mockMoveCount, errorCount = 1))
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.general_move_node_snackbar_concat_success,
                mockMoveCount - 1,
                mockMoveCount - 1
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.general_move_node_snackbar_concat_fail,
                1,
                1
            )
        }"

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when there are more than 1 move success and more than 1 move failed`() {
        val mockMoveCount = Random.nextInt(5, 10)
        val mockErrorCount = mockMoveCount - 3
        val underTest =
            generalMovementMessageMapper(
                GeneralMovement(
                    count = mockMoveCount,
                    errorCount = mockErrorCount
                )
            )
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.general_move_node_snackbar_concat_success,
                mockMoveCount - mockErrorCount,
                mockMoveCount - mockErrorCount
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.general_move_node_snackbar_concat_fail,
                mockErrorCount,
                mockErrorCount
            )
        }"

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when there are more than 1 share success and more than 1 share failed`() {
        val mockShareCount = Random.nextInt(5, 10)
        val mockErrorCount = mockShareCount - 3
        val underTest =
            generalMovementMessageMapper(
                MoveRequestResult.ShareMovement(
                    count = mockShareCount,
                    errorCount = mockErrorCount,
                    nodes = listOf()
                )
            )
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.shared_items_cloud_drive_snackbar_sharing_folder_success,
                mockShareCount - mockErrorCount,
                mockShareCount - mockErrorCount
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.shared_items_cloud_drive_snackbar_sharing_folder_failed_concat,
                mockErrorCount,
                mockErrorCount
            )
        }"
        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that the correct message is returned when there is only 1 failed share request`() {
        val underTest =
            generalMovementMessageMapper(
                MoveRequestResult.ShareMovement(
                    count = 1,
                    errorCount = 1,
                    nodes = listOf()
                )
            )
        val expected =
            mContext.resources.getQuantityString(
                R.plurals.shared_items_cloud_drive_snackbar_sharing_folder_failed,
                1,
                1
            )

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that the correct message is returned when there is only 1 successful share request`() {
        val underTest =
            generalMovementMessageMapper(
                MoveRequestResult.ShareMovement(
                    count = 1,
                    errorCount = 0,
                    nodes = listOf()
                )
            )
        val expected =
            mContext.resources.getQuantityString(
                R.plurals.shared_items_cloud_drive_snackbar_sharing_folder_success,
                1,
                1
            )

        assertThat(underTest).isEqualTo(expected)
    }
}