package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import nz.mega.sdk.MegaError
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FolderLoginStatusMapperTest {
    private val underTest: FolderLoginStatusMapper = FolderLoginStatusMapper()

    @ParameterizedTest(name = "test {0} is mapped correctly")
    @MethodSource("provideParameters")
    fun `test mapped correctly`(expected: FolderLoginStatus, raw: MegaError) {
        val actual = underTest(raw)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(
            FolderLoginStatus.SUCCESS,
            mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }),
        Arguments.of(
            FolderLoginStatus.API_INCOMPLETE,
            mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EINCOMPLETE) }),
        Arguments.of(
            FolderLoginStatus.INCORRECT_KEY,
            mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EARGS) }),
    )
}