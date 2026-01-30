package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.AccountRepository
import mega.privacy.android.domain.usecase.media.DoesAlbumsHaveLinksUseCase
import mega.privacy.android.domain.usecase.node.publiclink.DoesHaveLinksUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldShowCopyrightUseCaseTest {

    private lateinit var underTest: ShouldShowCopyrightUseCase

    private val accountRepository: AccountRepository = mock()
    private val doesHaveLinksUseCase: DoesHaveLinksUseCase = mock()
    private val doesAlbumsHaveLinksUseCase: DoesAlbumsHaveLinksUseCase = mock()

    @BeforeEach
    fun setup() {
        underTest = ShouldShowCopyrightUseCase(
            accountRepository = accountRepository,
            doesHaveLinksUseCase = doesHaveLinksUseCase,
            doesAlbumsHaveLinksUseCase = doesAlbumsHaveLinksUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            accountRepository,
            doesHaveLinksUseCase,
            doesAlbumsHaveLinksUseCase
        )
    }

    @Test
    fun `test that true is returned when the copyright should be shown and there are no public links yet`() =
        runTest {
            whenever(accountRepository.shouldShowCopyright()) doReturn true
            whenever(doesHaveLinksUseCase()) doReturn false
            whenever(doesAlbumsHaveLinksUseCase()) doReturn false

            val actual = underTest()

            assertThat(actual).isTrue()
        }

    @Test
    fun `test that false is returned when the copyright should be shown but there are links`() =
        runTest {
            whenever(accountRepository.shouldShowCopyright()) doReturn true
            whenever(doesHaveLinksUseCase()) doReturn true
            whenever(doesAlbumsHaveLinksUseCase()) doReturn false

            val actual = underTest()

            assertThat(actual).isFalse()
        }

    @Test
    fun `test that false is returned when the copyright should be shown but there are albums links`() =
        runTest {
            whenever(accountRepository.shouldShowCopyright()) doReturn true
            whenever(doesHaveLinksUseCase()) doReturn false
            whenever(doesAlbumsHaveLinksUseCase()) doReturn true

            val actual = underTest()

            assertThat(actual).isFalse()
        }

    @Test
    fun `test that false is returned when the copyright should not be shown and there are no links yet`() =
        runTest {
            whenever(accountRepository.shouldShowCopyright()) doReturn false
            whenever(doesHaveLinksUseCase()) doReturn false
            whenever(doesAlbumsHaveLinksUseCase()) doReturn false

            val actual = underTest()

            assertThat(actual).isFalse()
        }
}
