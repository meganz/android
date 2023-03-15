package test.mega.privacy.android.app.presentation.copynode.mapper

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapperImpl

@RunWith(AndroidJUnit4::class)
class CopyRequestMessageMapperTest {
    private val mContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val copyRequestMessageMapper: CopyRequestMessageMapper =
        CopyRequestMessageMapperImpl(mContext)

    @Test(expected = RuntimeException::class)
    fun `test that mapper should throw an exception if copy request data is 0`() {
        copyRequestMessageMapper(CopyRequestResult(count = 0, errorCount = 0))
    }

    @Test
    fun `test that mapper should return correct message when there is only 1 copy request and is successful `() {
        val underTest = copyRequestMessageMapper(CopyRequestResult(count = 1, errorCount = 0))
        val expected =
            mContext.resources.getQuantityString(R.plurals.general_copy_snackbar_success, 1)

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that mapper should return correct message when more than 1 copy request and all are successful`() {
        val mockCopyCount = Random.nextInt(2, 10)
        val underTest =
            copyRequestMessageMapper(CopyRequestResult(count = mockCopyCount, errorCount = 0))
        val expected = mContext.resources.getQuantityString(
            R.plurals.general_copy_snackbar_success,
            mockCopyCount,
            mockCopyCount
        )

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that when there is only 1 copy request and failed should return correct message`() {
        val underTest = copyRequestMessageMapper(CopyRequestResult(count = 1, errorCount = 1))
        val expected =
            mContext.resources.getQuantityString(R.plurals.general_copy_snackbar_fail, 1)

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that mapper should return correct message when there are more than 1 copy request and all failed`() {
        val mockCopyCount = Random.nextInt(2, 10)
        val underTest = copyRequestMessageMapper(
            CopyRequestResult(
                count = mockCopyCount,
                errorCount = mockCopyCount
            )
        )
        val expected = mContext.resources.getQuantityString(
            R.plurals.general_copy_snackbar_fail,
            mockCopyCount,
            mockCopyCount
        )

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that when there are 1 copy success and 1 copy failed should return correct message`() {
        val underTest = copyRequestMessageMapper(CopyRequestResult(count = 2, errorCount = 1))
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_success,
                1,
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_fail,
                1,
            )
        }"

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that mapper should return correct message when there are 1 copy success and more than 1 copy failed`() {
        val mockCopyCount = Random.nextInt(5, 10)
        val underTest =
            copyRequestMessageMapper(
                CopyRequestResult(
                    count = mockCopyCount,
                    errorCount = mockCopyCount - 1
                )
            )
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_success,
                1,
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_fail,
                mockCopyCount - 1,
                mockCopyCount - 1
            )
        }"

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that mapper should return correct message when there are more than 1 copy success and 1 copy failed`() {
        val mockCopyCount = Random.nextInt(2, 10)
        val underTest =
            copyRequestMessageMapper(CopyRequestResult(count = mockCopyCount, errorCount = 1))
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_success,
                mockCopyCount - 1,
                mockCopyCount - 1
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_fail,
                1
            )
        }"

        assertThat(underTest).isEqualTo(expected)
    }

    @Test
    fun `test that mapper should return correct message when there are more than 1 copy success and more than 1 copy failed`() {
        val mockCopyCount = Random.nextInt(5, 10)
        val mockErrorCount = mockCopyCount - 3
        val underTest = copyRequestMessageMapper(
            CopyRequestResult(
                count = mockCopyCount,
                errorCount = mockErrorCount
            )
        )
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_success,
                mockCopyCount - mockErrorCount,
                mockCopyCount - mockErrorCount
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_fail,
                mockErrorCount,
                mockErrorCount
            )
        }"

        assertThat(underTest).isEqualTo(expected)
    }
}