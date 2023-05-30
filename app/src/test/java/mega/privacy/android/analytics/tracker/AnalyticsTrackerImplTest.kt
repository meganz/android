package mega.privacy.android.analytics.tracker

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import mega.privacy.android.analytics.event.ScreenView
import mega.privacy.android.analytics.event.TabSelected
import mega.privacy.android.domain.usecase.analytics.GetViewIdUseCase
import mega.privacy.android.domain.usecase.analytics.TrackEventUseCase
import mega.privacy.android.domain.usecase.analytics.TrackScreenViewUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.verifyNoMoreInteractions

@OptIn(ExperimentalCoroutinesApi::class)
internal class AnalyticsTrackerImplTest {
    private lateinit var underTest: AnalyticsTracker

    private val trackScreenViewUseCase = mock<TrackScreenViewUseCase>()
    private val trackEventUseCase = mock<TrackEventUseCase>()
    private val getViewIdUseCase = mock<GetViewIdUseCase>()

    @BeforeEach
    internal fun setUp() {
        Mockito.clearInvocations(
            trackScreenViewUseCase,
            trackEventUseCase,
            getViewIdUseCase,
        )

        underTest = AnalyticsTrackerImpl(
            appScope = TestScope(UnconfinedTestDispatcher()),
            trackScreenViewUseCase = trackScreenViewUseCase,
            trackEventUseCase = trackEventUseCase,
            getViewIdUseCase = getViewIdUseCase
        )
    }

    @Test
    internal fun `test that tracking screen view calls the correct use case`() {
        underTest.trackScreenView(mock {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        })
        verifyBlocking(trackScreenViewUseCase) { invoke(any()) }
    }

    @Test
    internal fun `test that tab selected generates new view id if it does not exist`() {
        getViewIdUseCase.stub {
            onBlocking { invoke() }.thenReturn("viewId")
        }
        val screen = mock<ScreenView> {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        }

        underTest.trackTabSelected(mock {
            on { screenView }.thenReturn(screen)
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(2)
        })

        verifyBlocking(getViewIdUseCase) { invoke() }

    }

    @Test
    internal fun `test that tab selected uses existing view id if it exists`() {
        val expectedViewId = "viewId"
        trackScreenViewUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(expectedViewId)
        }
        val screen = mock<ScreenView> {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        }
        underTest.trackScreenView(screen)

        underTest.trackTabSelected(mock {
            on { screenView }.thenReturn(screen)
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(2)
        })

        verifyBlocking(trackEventUseCase) { invoke(argThat { viewId == expectedViewId }) }

    }

    @Test
    internal fun `test that duplicate tab selections are ignored`() {
        trackScreenViewUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn("viewId")
        }
        val screen = mock<ScreenView> {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        }
        underTest.trackScreenView(screen)

        val tabSelected = mock<TabSelected> {
            on { screenView }.thenReturn(screen)
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(2)
        }
        underTest.trackTabSelected(tabSelected)
        underTest.trackTabSelected(tabSelected)

        verifyBlocking(trackEventUseCase, times(1)) { invoke(any()) }
        verifyNoMoreInteractions(trackEventUseCase)
    }

    @Test
    internal fun `test that unique tab selections are not ignored`() {
        trackScreenViewUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn("viewId")
        }
        val screen = mock<ScreenView> {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        }
        underTest.trackScreenView(screen)

        val tabSelected = mock<TabSelected> {
            on { screenView }.thenReturn(screen)
            on { name }.thenReturn("", "new")
            on { uniqueIdentifier }.thenReturn(2)
        }
        underTest.trackTabSelected(tabSelected)
        underTest.trackTabSelected(tabSelected)

        verifyBlocking(trackEventUseCase, times(2)) { invoke(any()) }
    }
}