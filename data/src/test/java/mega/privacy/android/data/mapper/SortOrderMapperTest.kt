package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.SortOrder
import nz.mega.sdk.MegaApiJava
import org.junit.Test

/**
 * SortOrder mapper test
 */
class SortOrderMapperTest {
    private val underTest = ::toSortOrder

    private val sortOrderMap = mapOf(MegaApiJava.ORDER_NONE to SortOrder.ORDER_NONE,
        MegaApiJava.ORDER_DEFAULT_ASC to SortOrder.ORDER_DEFAULT_ASC,
        MegaApiJava.ORDER_DEFAULT_DESC to SortOrder.ORDER_DEFAULT_DESC,
        MegaApiJava.ORDER_SIZE_ASC to SortOrder.ORDER_SIZE_ASC,
        MegaApiJava.ORDER_SIZE_DESC to SortOrder.ORDER_SIZE_DESC,
        MegaApiJava.ORDER_CREATION_ASC to SortOrder.ORDER_CREATION_ASC,
        MegaApiJava.ORDER_CREATION_DESC to SortOrder.ORDER_CREATION_DESC,
        MegaApiJava.ORDER_MODIFICATION_ASC to SortOrder.ORDER_MODIFICATION_ASC,
        MegaApiJava.ORDER_MODIFICATION_DESC to SortOrder.ORDER_MODIFICATION_DESC,
        MegaApiJava.ORDER_ALPHABETICAL_ASC to SortOrder.ORDER_ALPHABETICAL_ASC,
        MegaApiJava.ORDER_ALPHABETICAL_DESC to SortOrder.ORDER_ALPHABETICAL_DESC,
        MegaApiJava.ORDER_PHOTO_ASC to SortOrder.ORDER_PHOTO_ASC,
        MegaApiJava.ORDER_PHOTO_DESC to SortOrder.ORDER_PHOTO_DESC,
        MegaApiJava.ORDER_VIDEO_ASC to SortOrder.ORDER_VIDEO_ASC,
        MegaApiJava.ORDER_VIDEO_DESC to SortOrder.ORDER_VIDEO_DESC,
        MegaApiJava.ORDER_LINK_CREATION_ASC to SortOrder.ORDER_LINK_CREATION_ASC,
        MegaApiJava.ORDER_LINK_CREATION_DESC to SortOrder.ORDER_LINK_CREATION_DESC,
        MegaApiJava.ORDER_LABEL_ASC to SortOrder.ORDER_LABEL_ASC,
        MegaApiJava.ORDER_LABEL_DESC to SortOrder.ORDER_LABEL_DESC,
        MegaApiJava.ORDER_FAV_ASC to SortOrder.ORDER_FAV_ASC,
        MegaApiJava.ORDER_FAV_DESC to SortOrder.ORDER_FAV_DESC)

    @Test
    fun `test that mapper returns correct value when input is in the mapping range`() {
        sortOrderMap.forEach { (key, value) ->
            assertThat(underTest(key)).isEqualTo(value)
        }
    }

    @Test
    fun `test that mapper returns null when input is outside the mapping range`() {
        val expected = null
        assertThat(underTest(-1)).isEqualTo(expected)
    }
}