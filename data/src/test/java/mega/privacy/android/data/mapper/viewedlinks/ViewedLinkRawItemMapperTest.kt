package mega.privacy.android.data.mapper.viewedlinks

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.database.entity.ViewedLinkRawItem
import mega.privacy.android.data.mapper.continuewhereleftoff.RecentlyUsedTypeIdMapper
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.domain.entity.node.ViewedLink
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ViewedLinkRawItemMapperTest {

    private lateinit var underTest: ViewedLinkRawItemMapper

    private val recentlyUsedTypeIdMapper = mock<RecentlyUsedTypeIdMapper>()

    @BeforeAll
    fun setUp() {
        underTest = ViewedLinkRawItemMapper(
            recentlyUsedTypeIdMapper = recentlyUsedTypeIdMapper,
        )
    }

    @Test
    fun `test that ViewedLinkRawItem is mapped to ViewedLink correctly`() {
        whenever(recentlyUsedTypeIdMapper(5)).thenReturn(RecentlyUsedType.FileLink)
        val raw = ViewedLinkRawItem(
            nodeHandle = 123L,
            typeId = 5,
            fileName = "document.pdf",
            lastAccessedTimestamp = 1_000_000L,
            linkUrl = "https://mega.nz/file/abc123",
        )

        val result = underTest(raw)

        assertThat(result).isEqualTo(
            ViewedLink(
                nodeHandle = 123L,
                name = "document.pdf",
                linkUrl = "https://mega.nz/file/abc123",
                type = RecentlyUsedType.FileLink,
                accessedTimestamp = 1_000_000L,
            )
        )
    }
}
