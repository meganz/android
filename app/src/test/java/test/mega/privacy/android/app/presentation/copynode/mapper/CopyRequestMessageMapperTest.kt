package test.mega.privacy.android.app.presentation.copynode.mapper

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.copynode.CopyRequestResult
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapperImpl
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class CopyRequestMessageMapperTest {
    private val mContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val underTest: CopyRequestMessageMapper =
        CopyRequestMessageMapperImpl(mContext)

    @Test
    fun `test that mapper should return a message as if there were 0 successes if the result is null`() {
        val expected =
            mContext.resources.getQuantityString(R.plurals.general_copy_snackbar_fail, 0, 0)
        val actual = underTest(null)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that mapper should return a message as if there were 0 successes if the count is 0`() {
        val expected =
            mContext.resources.getQuantityString(R.plurals.general_copy_snackbar_fail, 0, 0)
        val actual = underTest(CopyRequestResult(count = 0, errorCount = 0))
        
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that mapper should return correct message when there is only 1 copy request and is successful `() {
        val actual = underTest(CopyRequestResult(count = 1, errorCount = 0))
        val expected =
            mContext.resources.getQuantityString(R.plurals.general_copy_snackbar_success, 1, 1)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that mapper should return correct message when more than 1 copy request and all are successful`() {
        val mockCopyCount = Random.nextInt(2, 10)
        val actual =
            underTest(CopyRequestResult(count = mockCopyCount, errorCount = 0))
        val expected = mContext.resources.getQuantityString(
            R.plurals.general_copy_snackbar_success,
            mockCopyCount,
            mockCopyCount
        )

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that when there is only 1 copy request and failed should return correct message`() {
        val actual = underTest(CopyRequestResult(count = 1, errorCount = 1))
        val expected =
            mContext.resources.getQuantityString(R.plurals.general_copy_snackbar_fail, 1, 1)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that mapper should return correct message when there are more than 1 copy request and all failed`() {
        val mockCopyCount = Random.nextInt(2, 10)
        val actual = underTest(
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

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that when there are 1 copy success and 1 copy failed should return correct message`() {
        val actual = underTest(CopyRequestResult(count = 2, errorCount = 1))
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_success,
                1,
                1
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_fail,
                1,
                1
            )
        }"

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that mapper should return correct message when there are 1 copy success and more than 1 copy failed`() {
        val mockCopyCount = Random.nextInt(5, 10)
        val actual =
            underTest(
                CopyRequestResult(
                    count = mockCopyCount,
                    errorCount = mockCopyCount - 1
                )
            )
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_success,
                1,
                1
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_fail,
                mockCopyCount - 1,
                mockCopyCount - 1
            )
        }"

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that mapper should return correct message when there are more than 1 copy success and 1 copy failed`() {
        val mockCopyCount = Random.nextInt(2, 10)
        val actual =
            underTest(CopyRequestResult(count = mockCopyCount, errorCount = 1))
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_success,
                mockCopyCount - 1,
                mockCopyCount - 1
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_fail,
                1,
                1
            )
        }"

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that mapper should return correct message when there are more than 1 copy success and more than 1 copy failed`() {
        val mockCopyCount = Random.nextInt(5, 10)
        val mockErrorCount = mockCopyCount - 3
        val actual = underTest(
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

        assertThat(actual).isEqualTo(expected)
    }
}