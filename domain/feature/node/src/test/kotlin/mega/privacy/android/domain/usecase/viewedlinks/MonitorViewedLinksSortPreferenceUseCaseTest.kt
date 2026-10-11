package mega.privacy.android.domain.usecase.viewedlinks

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.viewedlinks.ViewedLinksSortField
import mega.privacy.android.domain.repository.ViewedLinksRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MonitorViewedLinksSortPreferenceUseCaseTest {

    private val repository: ViewedLinksRepository = mock()
    private lateinit var underTest: MonitorViewedLinksSortPreferenceUseCase

    @BeforeEach
    fun setUp() {
        reset(repository)
        underTest = MonitorViewedLinksSortPreferenceUseCase(repository)
    }

    @Test
    fun `test that invoke emits the repository sort preference`() = runTest {
        val expected = ViewedLinksSortField.Name to SortDirection.Ascending
        whenever(repository.monitorSortPreference()).thenReturn(flowOf(expected))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            awaitComplete()
        }
    }
}
