package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetHandleFromContactLinkUseCaseTest {
    private lateinit var underTest: GetHandleFromContactLinkUseCase
    private val nodeRepository: NodeRepository = mock()

    @BeforeAll
    fun setup() {
        underTest = GetHandleFromContactLinkUseCase(nodeRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(nodeRepository)
    }

    @Test
    fun `test that getHandleFromContactLink returns invalid handle when link is invalid`() =
        runTest {
            val link = "https://mega.nz/B!86YkxIDC"
            whenever(nodeRepository.getInvalidHandle()).thenReturn(-1)
            Truth.assertThat(underTest(link)).isEqualTo(-1L)
        }

    @Test
    fun `test that getHandleFromContactLink returns correctly handle when link is invalid`() =
        runTest {
            val link = "https://mega.nz/C!86YkxIDC"
            whenever(nodeRepository.convertBase64ToHandle("86YkxIDC")).thenReturn(1L)
            Truth.assertThat(underTest(link)).isEqualTo(1L)
        }
}