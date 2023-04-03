package mega.privacy.android.domain.usecase.achievements

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.verification.OptInVerification
import mega.privacy.android.domain.entity.verification.Unblock
import mega.privacy.android.domain.repository.VerificationRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class IsAddPhoneRewardEnabledUseCaseTest {
    private lateinit var underTest: IsAddPhoneRewardEnabledUseCase

    private val verificationRepository = mock<VerificationRepository>()

    suspend operator fun invoke() = with(verificationRepository.getSmsPermissions()) {
        any { it is OptInVerification } && any { it is Unblock }
    }

    @Before
    fun setUp() {
        underTest = IsAddPhoneRewardEnabledUseCase(
            verificationRepository = verificationRepository
        )
    }

    @Test
    fun `test that add phone rewards are enabled if repository returns both SMS permission types`() =
        runTest {
            whenever(verificationRepository.getSmsPermissions()).thenReturn(
                listOf(
                    OptInVerification,
                    Unblock
                )
            )
            assertThat(underTest()).isTrue()
        }

    @Test
    fun `test that add phone rewards are disabled if repository returns one SMS permission type`() =
        runTest {
            whenever(verificationRepository.getSmsPermissions()).thenReturn(
                listOf(
                    OptInVerification,
                )
            )
            assertThat(underTest()).isFalse()
        }

    @Test
    fun `test that add phone rewards are disabled if repository returns no SMS permission type`() =
        runTest {
            whenever(verificationRepository.getSmsPermissions()).thenReturn(emptyList())
            assertThat(underTest()).isFalse()
        }
}
