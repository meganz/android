package mega.privacy.android.data.mapper.camerauploads

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.model.MegaPreferences
import mega.privacy.android.domain.entity.settings.camerauploads.UploadOption
import mega.privacy.android.domain.exception.mapper.UnknownMapperParameterException
import org.junit.Before
import org.junit.Test

/**
 * Test class for [UploadOptionMapper]
 */
class UploadOptionMapperImplTest {
    private lateinit var underTest: UploadOptionMapper

    @Before
    fun setUp() {
        underTest = UploadOptionMapperImpl()
    }

    @Test
    fun `test that UploadOption can be mapped correctly`() {
        val expectedResults = HashMap<String?, UploadOption>().apply {
            put(null, UploadOption.PHOTOS)
            put(MegaPreferences.ONLY_PHOTOS.toString(), UploadOption.PHOTOS)
            put(MegaPreferences.ONLY_VIDEOS.toString(), UploadOption.VIDEOS)
            put(MegaPreferences.PHOTOS_AND_VIDEOS.toString(), UploadOption.PHOTOS_AND_VIDEOS)
        }

        expectedResults.forEach { (state, uploadOption) ->
            assertThat(underTest(state)).isEqualTo(uploadOption)
        }
    }

    @Test(expected = UnknownMapperParameterException::class)
    fun `test that a different state throws an error`() {
        underTest("-1")
    }
}