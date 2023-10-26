package test.mega.privacy.android.app.presentation.slideshow

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.slideshow.LegacySlideshowViewModel
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.entity.slideshow.SlideshowSpeed
import mega.privacy.android.domain.usecase.GetPhotosByIdsUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowOrderSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowRepeatSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowSpeedSettingUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByAlbumImportNodeUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodeHandleUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageByNodePublicLinkUseCase
import mega.privacy.android.domain.usecase.imageviewer.GetImageForChatMessageUseCase
import mega.privacy.android.domain.usecase.slideshow.GetChatPhotoByMessageIdUseCase
import mega.privacy.android.domain.usecase.slideshow.GetPhotoByAlbumImportNodeUseCase
import mega.privacy.android.domain.usecase.slideshow.GetPhotoByPublicLinkUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LegacySlideshowViewModelTest {
    private lateinit var underTest: LegacySlideshowViewModel

    private val getPhotosByIdsUseCase: GetPhotosByIdsUseCase = mock()
    private val monitorSlideshowOrderSettingUseCase: MonitorSlideshowOrderSettingUseCase = mock()
    private val monitorSlideshowSpeedSettingUseCase: MonitorSlideshowSpeedSettingUseCase = mock()
    private val monitorSlideshowRepeatSettingUseCase: MonitorSlideshowRepeatSettingUseCase = mock()
    private val getImageByNodeHandleUseCase: GetImageByNodeHandleUseCase = mock()
    private val getImageForChatMessageUseCase: GetImageForChatMessageUseCase = mock()
    private val getChatPhotoByMessageIdUseCase: GetChatPhotoByMessageIdUseCase = mock()
    private val getImageByNodePublicLinkUseCase: GetImageByNodePublicLinkUseCase = mock()
    private val getPhotoByPublicLinkUseCase: GetPhotoByPublicLinkUseCase = mock()
    private val getImageByAlbumImportNodeUseCase: GetImageByAlbumImportNodeUseCase = mock()
    private val getPhotoByAlbumImportNodeUseCase: GetPhotoByAlbumImportNodeUseCase = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        whenever(monitorSlideshowOrderSettingUseCase.invoke())
            .thenReturn(flowOf())

        whenever(monitorSlideshowSpeedSettingUseCase.invoke())
            .thenReturn(flowOf())

        whenever(monitorSlideshowRepeatSettingUseCase.invoke())
            .thenReturn(flowOf())
    }

    private fun createSUT() = LegacySlideshowViewModel(
        getPhotosByIdsUseCase = getPhotosByIdsUseCase,
        monitorSlideshowOrderSettingUseCase = monitorSlideshowOrderSettingUseCase,
        monitorSlideshowSpeedSettingUseCase = monitorSlideshowSpeedSettingUseCase,
        monitorSlideshowRepeatSettingUseCase = monitorSlideshowRepeatSettingUseCase,
        getImageByNodeHandleUseCase = getImageByNodeHandleUseCase,
        getImageForChatMessageUseCase = getImageForChatMessageUseCase,
        getChatPhotoByMessageIdUseCase = getChatPhotoByMessageIdUseCase,
        getImageByNodePublicLinkUseCase = getImageByNodePublicLinkUseCase,
        getPhotoByPublicLinkUseCase = getPhotoByPublicLinkUseCase,
        getImageByAlbumImportNodeUseCase = getImageByAlbumImportNodeUseCase,
        getPhotoByAlbumImportNodeUseCase = getPhotoByAlbumImportNodeUseCase,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    @Test
    fun `test that monitor order setting receives correct result`() = runTest {
        // given
        val expectedSetting = SlideshowOrder.Shuffle
        whenever(monitorSlideshowOrderSettingUseCase.invoke())
            .thenReturn(flowOf(expectedSetting))

        // when
        underTest = createSUT()

        // then
        underTest.state.drop(1).test {
            val actualSetting = awaitItem().order
            assertEquals(expectedSetting, actualSetting)
        }
    }

    @Test
    fun `test that monitor speed setting receives correct result`() = runTest {
        // given
        val expectedSetting = SlideshowSpeed.Normal
        whenever(monitorSlideshowSpeedSettingUseCase.invoke())
            .thenReturn(flowOf(expectedSetting))

        // when
        underTest = createSUT()

        // then
        underTest.state.drop(1).test {
            val actualSetting = awaitItem().speed
            assertEquals(expectedSetting, actualSetting)
        }
    }

    @Test
    fun `test that monitor repeat setting receives correct result`() = runTest {
        // given
        val expectedSetting = true
        whenever(monitorSlideshowRepeatSettingUseCase.invoke())
            .thenReturn(flowOf(expectedSetting))

        // when
        underTest = createSUT()

        // then
        underTest.state.drop(1).test {
            val actualSetting = awaitItem().repeat
            assertEquals(expectedSetting, actualSetting)
        }
    }
}
