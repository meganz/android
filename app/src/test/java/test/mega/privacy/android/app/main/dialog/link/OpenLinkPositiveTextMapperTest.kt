package test.mega.privacy.android.app.main.dialog.link

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mega.privacy.android.app.R
import mega.privacy.android.app.main.dialog.link.OpenLinkPositiveTextMapper
import mega.privacy.android.domain.entity.RegexPatternType
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class OpenLinkPositiveTextMapperTest {
    private val underTest: OpenLinkPositiveTextMapper = OpenLinkPositiveTextMapper()

    private val result = mapOf(
        RegexPatternType.CHAT_LINK to R.string.action_open_chat_link,
        RegexPatternType.CONTACT_LINK to R.string.action_open_contact_link,
    )

    @ParameterizedTest(name = "linkType: {0}")
    @EnumSource(RegexPatternType::class)
    fun `test that return correctly when linkType is not null`(linkType: RegexPatternType) {
        Truth.assertThat(underTest(linkType))
            .isEqualTo(result[linkType] ?: R.string.context_open_link)
    }
}