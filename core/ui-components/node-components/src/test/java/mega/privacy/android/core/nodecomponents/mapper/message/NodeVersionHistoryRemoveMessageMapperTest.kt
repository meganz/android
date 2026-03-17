package mega.privacy.android.core.nodecomponents.mapper.message

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.shared.nodes.R as NodesR
import mega.privacy.android.domain.exception.VersionsNotDeletedException
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeVersionHistoryRemoveMessageMapperTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val mapper = NodeVersionHistoryRemoveMessageMapper(context)

    @Test
    fun `test that message shown is as expected when version not deleted exception is thrown`() {
        val expected = context.getString(NodesR.string.version_history_deleted_erroneously)
            .plus(System.lineSeparator())
            .plus(
                context.resources.getQuantityString(
                    NodesR.plurals.versions_deleted_succesfully,
                    1,
                    1
                )
            ).plus(System.lineSeparator())
            .plus(
                context.resources.getQuantityString(
                    NodesR.plurals.versions_not_deleted,
                    2,
                    2
                )
            )
        val result = mapper(VersionsNotDeletedException(3, 2))
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that message shown is as expected when any exception is thrown`() {
        val expected = context.getString(NodesR.string.general_text_error)
        val result = mapper(Throwable())
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that message shown is as expected when no exception is thrown`() {
        val expected = context.getString(NodesR.string.version_history_deleted)
        val result = mapper(null)
        assertThat(result).isEqualTo(expected)
    }
}