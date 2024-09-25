package mega.privacy.android.domain.usecase.user

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AvatarRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorUserAvatarUpdatesUseCaseTest {

    private lateinit var underTest: MonitorUserAvatarUpdatesUseCase

    private val avatarRepository = mock<AvatarRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = MonitorUserAvatarUpdatesUseCase(
            avatarRepository = avatarRepository,
        )
    }

    @Test
    fun `test that use case emits correctly`() = runTest {
        whenever(avatarRepository.monitorUserAvatarUpdates())
            .thenReturn(flowOf(1L))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(1L)
            awaitComplete()
        }
    }
}