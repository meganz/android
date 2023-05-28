package mega.privacy.android.domain.usecase.analytics

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.analytics.ScreenViewEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argWhere
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
internal class TrackScreenViewUseCaseTest {
    private lateinit var underTest: TrackScreenViewUseCase

    private val getViewIdUseCase = mock<GetViewIdUseCase>()
    private val trackEventUseCase = mock<TrackEventUseCase>()

    @BeforeEach
    internal fun setUp() {
        underTest = TrackScreenViewUseCase(
            getViewIdUseCase = getViewIdUseCase,
            trackEventUseCase = trackEventUseCase
        )
    }

    @Test
    internal fun `test that view id is returned`() = runTest {
        val expected = "ViewId"
        getViewIdUseCase.stub {
            onBlocking { invoke() }.thenReturn(expected)
        }

        assertThat(underTest(mock())).isEqualTo(expected)
    }

    @Test
    internal fun `test that track event is called with screen view event`() = runTest {
        getViewIdUseCase.stub {
            onBlocking { invoke() }.thenReturn("expected")
        }
        underTest(mock())
        verify(trackEventUseCase).invoke(argWhere { it is ScreenViewEvent })
    }

}