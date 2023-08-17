package mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.repository.MegaNodeRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetContactVerificationWarningUseCaseTest {
    private val megaNodeRepository: MegaNodeRepository = mock()
    private lateinit var underTest: GetContactVerificationWarningUseCase

    @Before
    fun setUp() {
        underTest = GetContactVerificationWarningUseCase(megaNodeRepository = megaNodeRepository)
    }

    @Test
    fun `test that when warning is disable invoke returns false`() = runTest {
        whenever(megaNodeRepository.getContactVerificationEnabledWarning()).thenReturn(false)
        val value = underTest()
        Truth.assertThat(value).isFalse()
    }

    @Test
    fun `test that when warning is disable invoke returns true`() = runTest {
        whenever(megaNodeRepository.getContactVerificationEnabledWarning()).thenReturn(true)
        val value = underTest()
        Truth.assertThat(value).isTrue()
    }
}