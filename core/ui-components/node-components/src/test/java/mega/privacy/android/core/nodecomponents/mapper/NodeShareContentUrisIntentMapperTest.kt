package mega.privacy.android.core.nodecomponents.mapper

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.node.NodeShareContentUri
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NodeShareContentUrisIntentMapperTest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val underTest = NodeShareContentUrisIntentMapper(context)

    @Test
    fun `test that invoke does not set EXTRA_TITLE when there is a single link`() {
        val intent = underTest(
            title = "subject",
            content = NodeShareContentUri.RemoteContentUris(links = listOf("https://mega.nz/a")),
        )

        assertThat(intent.hasExtra(Intent.EXTRA_TITLE)).isFalse()
    }

    @Test
    fun `test that invoke sets EXTRA_TITLE with the link count when there are multiple links`() {
        val links = listOf("https://mega.nz/a", "https://mega.nz/b", "https://mega.nz/c")
        val intent = underTest(
            title = "subject",
            content = NodeShareContentUri.RemoteContentUris(links = links),
        )

        val expected = context.resources.getQuantityString(
            sharedResR.plurals.general_share_link_count_title,
            links.size,
            links.size,
        )
        assertThat(intent.getStringExtra(Intent.EXTRA_TITLE)).isEqualTo(expected)
    }

    @Test
    fun `test that invoke joins the links into EXTRA_TEXT when content is remote`() {
        val links = listOf("https://mega.nz/a", "https://mega.nz/b")
        val intent = underTest(
            title = "subject",
            content = NodeShareContentUri.RemoteContentUris(links = links),
        )

        assertThat(intent.getStringExtra(Intent.EXTRA_TEXT))
            .isEqualTo(links.joinToString(separator = "\n\n"))
    }

    @Test
    fun `test that invoke sets EXTRA_SUBJECT with the given title`() {
        val intent = underTest(
            title = "my subject",
            content = NodeShareContentUri.RemoteContentUris(links = listOf("https://mega.nz/a")),
        )

        assertThat(intent.getStringExtra(Intent.EXTRA_SUBJECT)).isEqualTo("my subject")
    }
}
