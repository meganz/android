package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import org.junit.Before
import org.junit.Test

/**
 * Test class for [UploadOptionIntMapper]
 */
class UploadOptionIntMapperImplTest {
    private lateinit var underTest: UploadOptionIntMapper

    @Before
    fun setUp() {
        underTest = UploadOptionIntMapperImpl()
    }

    @Test
    fun `test that the MegaPreferences value can be mapped correctly`() {
        val expectedResults = HashMap<UploadOption, Int>().apply {
            put(UploadOption.PHOTOS, MegaPreferences.ONLY_PHOTOS)
            put(UploadOption.VIDEOS, MegaPreferences.ONLY_VIDEOS)
            put(UploadOption.PHOTOS_AND_VIDEOS, MegaPreferences.PHOTOS_AND_VIDEOS)
        }

        expectedResults.forEach { (option, intVersion) ->
            assertThat(underTest(option)).isEqualTo(intVersion)
        }
    }
}