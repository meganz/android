package mega.privacy.android.data.mapper.viewedlinks

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.database.entity.ViewedLinkRawItem
import mega.privacy.android.domain.entity.node.RecentlyViewedLinkType
import mega.privacy.android.domain.entity.node.ViewedLink
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ViewedLinkRawItemMapperTest {

    private lateinit var underTest: ViewedLinkRawItemMapper

    private val recentlyViewedLinkTypeIdMapper = mock<RecentlyViewedLinkTypeIdMapper>()

    @BeforeAll
    fun setUp() {
        underTest = ViewedLinkRawItemMapper(
            recentlyViewedLinkTypeIdMapper = recentlyViewedLinkTypeIdMapper,
        )
    }

    @Test
    fun `test that ViewedLinkRawItem is mapped to ViewedLink correctly`() {
        whenever(recentlyViewedLinkTypeIdMapper(1)).thenReturn(RecentlyViewedLinkType.FileLink)
        val raw = ViewedLinkRawItem(
            nodeHandle = 123L,
            typeId = 1,
            nodeName = "document.pdf",
            lastAccessedTimestamp = 1_000_000L,
            linkUrl = "https://mega.nz/file/abc123",
        )

        val result = underTest(raw)

        assertThat(result).isEqualTo(
            ViewedLink(
                nodeHandle = 123L,
                name = "document.pdf",
                linkUrl = "https://mega.nz/file/abc123",
                type = RecentlyViewedLinkType.FileLink,
                accessedTimestamp = 1_000_000L,
            )
        )
    }
}
