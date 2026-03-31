package mega.privacy.android.domain.usecase.avatar

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AvatarRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUserAvatarSecondaryColorUseCaseTest {
    private val avatarRepository = mock<AvatarRepository>()
    private val underTest = GetUserAvatarSecondaryColorUseCase(avatarRepository)
    private val userHandle = 123L
    private val secondaryColor = -44462

    @Test
    fun `test that invoke returns avatar secondary color when repository returns color`() =
        runTest {
            whenever(avatarRepository.getAvatarSecondaryColor(userHandle)).thenReturn(secondaryColor)
            val actual = underTest(userHandle)
            Truth.assertThat(actual).isEqualTo(secondaryColor)
        }
}
