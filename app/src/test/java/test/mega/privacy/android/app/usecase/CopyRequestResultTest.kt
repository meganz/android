package test.mega.privacy.android.app.usecase

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.usecase.data.CopyRequestData
import mega.privacy.android.app.usecase.data.CopyRequestResult
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random
import mega.privacy.android.app.R

@RunWith(AndroidJUnit4::class)
class CopyRequestResultTest {
    private val mContext: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test(expected = RuntimeException::class)
    fun `test that getResultText should throw an exception if copy request data is 0`() {
        CopyRequestResult(
            context = mContext,
            CopyRequestData(count = 0, errorCount = 0)
        ).getResultText()
    }

    @Test
    fun `test that getResultText should return correct message when there is only 1 copy request and is successful `() {
        val underTest =
            CopyRequestResult(context = mContext, CopyRequestData(count = 1, errorCount = 0))
        val expected =
            mContext.resources.getQuantityString(R.plurals.general_copy_snackbar_success, 1)

        assertThat(underTest.getResultText()).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when more than 1 copy request and all are successful`() {
        val mockCopyCount = Random.nextInt(2, 10)
        val underTest =
            CopyRequestResult(
                context = mContext,
                CopyRequestData(count = mockCopyCount, errorCount = 0)
            )
        val count = underTest.data.count
        val expected = mContext.resources.getQuantityString(
            R.plurals.general_copy_snackbar_success,
            count,
            count
        )

        assertThat(underTest.getResultText()).isEqualTo(expected)
    }

    @Test
    fun `test that when there is only 1 copy request and failed should return correct message`() {
        val underTest =
            CopyRequestResult(context = mContext, CopyRequestData(count = 1, errorCount = 1))
        val expected =
            mContext.resources.getQuantityString(R.plurals.general_copy_snackbar_fail, 1)

        assertThat(underTest.getResultText()).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when there are more than 1 copy request and all failed`() {
        val mockCopyCount = Random.nextInt(2, 10)
        val underTest =
            CopyRequestResult(
                context = mContext,
                CopyRequestData(count = mockCopyCount, errorCount = mockCopyCount)
            )
        val count = underTest.data.count
        val expected = mContext.resources.getQuantityString(
            R.plurals.general_copy_snackbar_fail,
            count,
            count
        )

        assertThat(underTest.getResultText()).isEqualTo(expected)
    }

    @Test
    fun `test that when there are 1 copy success and 1 copy failed should return correct message`() {
        val underTest =
            CopyRequestResult(
                context = mContext,
                CopyRequestData(count = 2, errorCount = 1)
            )
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

        assertThat(underTest.getResultText()).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when there are 1 copy success and more than 1 copy failed`() {
        val mockCopyCount = Random.nextInt(5, 10)
        val underTest =
            CopyRequestResult(
                context = mContext,
                CopyRequestData(count = mockCopyCount, errorCount = mockCopyCount - 1)
            )
        val expected = "${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_success,
                1,
            )
        }${
            mContext.resources.getQuantityString(
                R.plurals.general_copy_snackbar_concat_fail,
                underTest.data.errorCount,
                underTest.data.errorCount
            )
        }"

        assertThat(underTest.getResultText()).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when there are more than 1 copy success and 1 copy failed`() {
        val mockCopyCount = Random.nextInt(2, 10)
        val underTest =
            CopyRequestResult(
                context = mContext,
                CopyRequestData(count = mockCopyCount, errorCount = 1)
            )
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

        assertThat(underTest.getResultText()).isEqualTo(expected)
    }

    @Test
    fun `test that getResultText should return correct message when there are more than 1 copy success and more than 1 copy failed`() {
        val mockCopyCount = Random.nextInt(5, 10)
        val mockErrorCount = mockCopyCount - 3
        val underTest =
            CopyRequestResult(
                context = mContext,
                CopyRequestData(count = mockCopyCount, errorCount = mockErrorCount)
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

        assertThat(underTest.getResultText()).isEqualTo(expected)
    }
}