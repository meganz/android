package mega.privacy.android.shared.nodes.dialog.removelink

import android.content.Context
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.ResultCount
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemovePublicLinkResultMapperTest {

    private val context: Context = mock()
    private val underTest = RemovePublicLinkResultMapper(context)

    @Test
    fun `test that the single-link success message is returned when one link is removed`() {
        whenever(context.getString(sharedResR.string.link_removed_success_message))
            .thenReturn(SINGLE_SUCCESS)

        val result = underTest(ResultCount(successCount = 1, errorCount = 0))

        assertThat(result).isEqualTo(SINGLE_SUCCESS)
    }

    @Test
    fun `test that the multiple-links success message is returned when more than one link is removed`() {
        whenever(context.getString(sharedResR.string.links_removed_success_message))
            .thenReturn(MULTIPLE_SUCCESS)

        val result = underTest(ResultCount(successCount = 2, errorCount = 0))

        assertThat(result).isEqualTo(MULTIPLE_SUCCESS)
    }

    @Test
    fun `test that the error message is returned when any removal fails`() {
        whenever(context.getString(sharedResR.string.public_link_node_removal_error_message))
            .thenReturn(ERROR)

        val result = underTest(ResultCount(successCount = 1, errorCount = 1))

        assertThat(result).isEqualTo(ERROR)
    }

    private companion object {
        const val SINGLE_SUCCESS = "Link removed"
        const val MULTIPLE_SUCCESS = "Links removed"
        const val ERROR = "Link removal failed, try again later"
    }
}
