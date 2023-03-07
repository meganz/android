package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaApiJava
import org.junit.Before
import org.junit.Test

/**
 * SortOrder Int mapper test
 */
class SortOrderIntMapperTest {
    private lateinit var underTest: SortOrderIntMapper

    @Before
    fun setUp() {
        underTest = SortOrderIntMapperImpl()
    }

    private val sortOrderIntMap = mapOf(
        SortOrder.ORDER_NONE to MegaApiJava.ORDER_NONE,
        SortOrder.ORDER_DEFAULT_ASC to MegaApiJava.ORDER_DEFAULT_ASC,
        SortOrder.ORDER_DEFAULT_DESC to MegaApiJava.ORDER_DEFAULT_DESC,
        SortOrder.ORDER_SIZE_ASC to MegaApiJava.ORDER_SIZE_ASC,
        SortOrder.ORDER_SIZE_DESC to MegaApiJava.ORDER_SIZE_DESC,
        SortOrder.ORDER_CREATION_ASC to MegaApiJava.ORDER_CREATION_ASC,
        SortOrder.ORDER_CREATION_DESC to MegaApiJava.ORDER_CREATION_DESC,
        SortOrder.ORDER_MODIFICATION_ASC to MegaApiJava.ORDER_MODIFICATION_ASC,
        SortOrder.ORDER_MODIFICATION_DESC to MegaApiJava.ORDER_MODIFICATION_DESC,
        SortOrder.ORDER_ALPHABETICAL_ASC to MegaApiJava.ORDER_ALPHABETICAL_ASC,
        SortOrder.ORDER_ALPHABETICAL_DESC to MegaApiJava.ORDER_ALPHABETICAL_DESC,
        SortOrder.ORDER_PHOTO_ASC to MegaApiJava.ORDER_PHOTO_ASC,
        SortOrder.ORDER_PHOTO_DESC to MegaApiJava.ORDER_PHOTO_DESC,
        SortOrder.ORDER_VIDEO_ASC to MegaApiJava.ORDER_VIDEO_ASC,
        SortOrder.ORDER_VIDEO_DESC to MegaApiJava.ORDER_VIDEO_DESC,
        SortOrder.ORDER_LINK_CREATION_ASC to MegaApiJava.ORDER_LINK_CREATION_ASC,
        SortOrder.ORDER_LINK_CREATION_DESC to MegaApiJava.ORDER_LINK_CREATION_DESC,
        SortOrder.ORDER_LABEL_ASC to MegaApiJava.ORDER_LABEL_ASC,
        SortOrder.ORDER_LABEL_DESC to MegaApiJava.ORDER_LABEL_DESC,
        SortOrder.ORDER_FAV_ASC to MegaApiJava.ORDER_FAV_ASC,
        SortOrder.ORDER_FAV_DESC to MegaApiJava.ORDER_FAV_DESC)

    @Test
    fun `test that sort order is mapped correctly`() {
        sortOrderIntMap.forEach { (key, value) ->
            assertThat(underTest(key)).isEqualTo(value)
        }
    }
}