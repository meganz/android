package mega.privacy.android.data.mapper.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mega.privacy.android.domain.entity.chat.RichPreview
import nz.mega.sdk.MegaChatRichPreview
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RichPreviewMapperTest {

    private lateinit var underTest: RichPreviewMapper

    @BeforeAll
    fun setup() {
        underTest = RichPreviewMapper()
    }

    @Test
    fun `test that rich preview mapper returns correctly`() {
        val megaChatRichPreview = mock<MegaChatRichPreview> {
            on { title }.thenReturn("title")
            on { description }.thenReturn("description")
            on { image }.thenReturn("image")
            on { imageFormat }.thenReturn("imageFormat")
            on { icon }.thenReturn(null)
            on { iconFormat }.thenReturn(null)
            on { url }.thenReturn("url")
            on { domainName }.thenReturn("domainName")
        }
        val richPreview = RichPreview(
            title = megaChatRichPreview.title.orEmpty(),
            description = megaChatRichPreview.description.orEmpty(),
            image = megaChatRichPreview.image,
            imageFormat = megaChatRichPreview.imageFormat,
            icon = megaChatRichPreview.icon,
            iconFormat = megaChatRichPreview.iconFormat,
            url = megaChatRichPreview.url,
            domainName = megaChatRichPreview.domainName
        )
        Truth.assertThat(underTest.invoke(megaChatRichPreview)).isEqualTo(richPreview)
    }
}