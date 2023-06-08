package mega.privacy.android.analytics.tracker

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import mega.privacy.android.analytics.event.ScreenInfo
import mega.privacy.android.analytics.event.TabInfo
import mega.privacy.android.analytics.event.menu.MenuType
import mega.privacy.android.analytics.event.navigation.NavigationEventSource
import mega.privacy.android.domain.entity.analytics.NotificationEvent
import mega.privacy.android.domain.usecase.analytics.GetViewIdUseCase
import mega.privacy.android.domain.usecase.analytics.TrackEventUseCase
import mega.privacy.android.domain.usecase.analytics.TrackScreenViewUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argWhere
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
        val screen = mock<ScreenInfo> {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        }

        underTest.trackTabSelected(mock {
            on { screenInfo }.thenReturn(screen)
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
        val screen = mock<ScreenInfo> {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        }
        underTest.trackScreenView(screen)

        underTest.trackTabSelected(mock {
            on { screenInfo }.thenReturn(screen)
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
        val screen = mock<ScreenInfo> {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        }
        underTest.trackScreenView(screen)

        val tabInfo = mock<TabInfo> {
            on { screenInfo }.thenReturn(screen)
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(2)
        }
        underTest.trackTabSelected(tabInfo)
        underTest.trackTabSelected(tabInfo)

        verifyBlocking(trackEventUseCase, times(1)) { invoke(any()) }
        verifyNoMoreInteractions(trackEventUseCase)
    }

    @Test
    internal fun `test that unique tab selections are not ignored`() {
        trackScreenViewUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn("viewId")
        }
        val screen = mock<ScreenInfo> {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        }
        underTest.trackScreenView(screen)

        val tabInfo = mock<TabInfo> {
            on { screenInfo }.thenReturn(screen)
            on { name }.thenReturn("", "new")
            on { uniqueIdentifier }.thenReturn(2)
        }
        underTest.trackTabSelected(tabInfo)
        underTest.trackTabSelected(tabInfo)

        verifyBlocking(trackEventUseCase, times(2)) { invoke(any()) }
    }

    @Test
    internal fun `test that track dialog uses existing view id if it exists`() {
        val expectedViewId = "viewId"
        trackScreenViewUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(expectedViewId)
        }
        val screen = mock<ScreenInfo> {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        }
        underTest.trackScreenView(screen)

        underTest.trackDialogDisplayed(mock {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(2)
        })

        verifyBlocking(trackEventUseCase) { invoke(argThat { viewId == expectedViewId }) }
    }

    @Test
    internal fun `test that track dialog uses null view id if it does not exists`() {
        underTest.trackDialogDisplayed(mock {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(2)
        })

        verifyBlocking(trackEventUseCase) { invoke(argThat { viewId == null }) }
    }

    @Test
    internal fun `test that track dialog uses screen name if passed`() {
        val expected = "expected screen name"
        underTest.trackDialogDisplayed(
            mock {
                on { name }.thenReturn("")
                on { uniqueIdentifier }.thenReturn(2)
            },
            mock { on { name }.thenReturn(expected) }
        )

        verifyBlocking(trackEventUseCase) { invoke(argThat { data().containsValue(expected) }) }
    }

    @Test
    internal fun `test that track button press uses null view id if it does not exist`() {
        underTest.trackButtonPress(mock {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(5)
        })

        verifyBlocking(trackEventUseCase) { invoke(argThat { viewId == null }) }
    }

    @Test
    internal fun `test that track button press uses existing view id if it exists`() {
        val expectedViewId = "viewId"
        trackScreenViewUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(expectedViewId)
        }
        val screen = mock<ScreenInfo> {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        }
        underTest.trackScreenView(screen)

        underTest.trackButtonPress(mock {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(2)
        })

        verifyBlocking(trackEventUseCase) { invoke(argThat { viewId == expectedViewId }) }
    }

    @Test
    internal fun `test that track navigation uses null view id if it does not exist`() {
        underTest.trackNavigation(mock {
            on { uniqueIdentifier }.thenReturn(5)
            on { source }.thenReturn(NavigationEventSource.Other)
            on { destination }.thenReturn("")
        })

        verifyBlocking(trackEventUseCase) { invoke(argThat { viewId == null }) }
    }

    @Test
    internal fun `test that track navigation uses existing view id if it exists`() {
        val expectedViewId = "viewId"
        trackScreenViewUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(expectedViewId)
        }
        val screen = mock<ScreenInfo> {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        }
        underTest.trackScreenView(screen)

        underTest.trackNavigation(mock {
            on { uniqueIdentifier }.thenReturn(2)
            on { source }.thenReturn(NavigationEventSource.Other)
            on { destination }.thenReturn("")
        })

        verifyBlocking(trackEventUseCase) { invoke(argThat { viewId == expectedViewId }) }
    }

    @ParameterizedTest(name = "Source {0} passed to event use case")
    @EnumSource()
    internal fun `test that navigation source items are passed`(enumSource: NavigationEventSource) {
        underTest.trackNavigation(mock {
            on { uniqueIdentifier }.thenReturn(5)
            on { source }.thenReturn(enumSource)
            on { destination }.thenReturn("")
        })

        verifyBlocking(trackEventUseCase) { invoke(argThat { data().containsValue(enumSource.name.lowercase()) }) }
    }

    @Test
    internal fun `test that track notification passes a notification event`() {
        underTest.trackNotification(mock {
            on { uniqueIdentifier }.thenReturn(5)
            on { notificationName }.thenReturn("")
        })

        verifyBlocking(trackEventUseCase) { invoke(argWhere { it is NotificationEvent }) }
    }


    @Test
    internal fun `test that track menu item uses null view id if it does not exist`() {
        underTest.trackMenuItem(mock {
            on { uniqueIdentifier }.thenReturn(5)
            on { menuType }.thenReturn(MenuType.Toolbar)
            on { menuItemName }.thenReturn("")
        })

        verifyBlocking(trackEventUseCase) { invoke(argThat { viewId == null }) }
    }

    @Test
    internal fun `test that track menu item uses existing view id if it exists`() {
        val expectedViewId = "viewId"
        trackScreenViewUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(expectedViewId)
        }
        val screen = mock<ScreenInfo> {
            on { name }.thenReturn("")
            on { uniqueIdentifier }.thenReturn(12)
        }
        underTest.trackScreenView(screen)

        underTest.trackMenuItem(mock {
            on { uniqueIdentifier }.thenReturn(5)
            on { menuType }.thenReturn(MenuType.Toolbar)
            on { menuItemName }.thenReturn("")
        })

        verifyBlocking(trackEventUseCase) { invoke(argThat { viewId == expectedViewId }) }
    }

    @ParameterizedTest(name = "Menu type {0} passed to event use case")
    @EnumSource()
    internal fun `test that menu type items are passed`(enumSource: MenuType) {
        underTest.trackMenuItem(mock {
            on { uniqueIdentifier }.thenReturn(5)
            on { menuType }.thenReturn(enumSource)
            on { menuItemName }.thenReturn("")
        })

        verifyBlocking(trackEventUseCase) { invoke(argThat { data().containsValue(enumSource.name.lowercase()) }) }
    }

}