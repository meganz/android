package mega.privacy.android.app.presentation.versions.mapper

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.domain.exception.VersionsNotDeletedException
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class VersionHistoryRemoveMessageMapperTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val mapper = VersionHistoryRemoveMessageMapper(context)

    @Test
    fun `test that message shown is as expected when version not deleted exception is thrown`() {
        val expected = context.getString(R.string.version_history_deleted_erroneously)
            .plus(System.lineSeparator())
            .plus(
                context.resources.getQuantityString(
                    R.plurals.versions_deleted_succesfully,
                    1,
                    1
                )
            ).plus(System.lineSeparator())
            .plus(
                context.resources.getQuantityString(
                    R.plurals.versions_not_deleted,
                    2,
                    2
                )
            )
        val result = mapper(VersionsNotDeletedException(3, 2))
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that message shown is as expected when any exception is thrown`() {
        val expected = context.getString(R.string.general_text_error)
        val result = mapper(Throwable())
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that message shown is as expected when no exception is thrown`() {
        val expected = context.getString(R.string.version_history_deleted)
        val result = mapper(null)
        assertThat(result).isEqualTo(expected)
    }
}