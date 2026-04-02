@file:OptIn(ExperimentalCoroutinesApi::class)

package mega.privacy.android.app.presentation.imagepreview.slideshow

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.IMAGE_NODE_FETCHER_SOURCE
import mega.privacy.android.app.presentation.imagepreview.ImagePreviewViewModel.Companion.PARAMS_CURRENT_IMAGE_NODE_ID_VALUE
import mega.privacy.android.app.presentation.imagepreview.fetcher.ImageNodeFetcher
import mega.privacy.android.app.presentation.imagepreview.model.ImagePreviewFetcherSource
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.slideshow.SlideshowOrder
import mega.privacy.android.domain.usecase.MonitorSlideshowOrderSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowRepeatSettingUseCase
import mega.privacy.android.domain.usecase.MonitorSlideshowSpeedSettingUseCase
import mega.privacy.android.domain.usecase.file.CheckFileUriUseCase
import mega.privacy.android.domain.usecase.imagepreview.ClearImageResultUseCase
import mega.privacy.android.domain.usecase.imagepreview.GetImageFromFileUseCase
import mega.privacy.android.domain.usecase.imagepreview.GetImageUseCase
import mega.privacy.android.domain.usecase.node.AddImageTypeUseCase
import mega.privacy.android.domain.usecase.slideshow.MonitorSecureSlideshowTutorialShownUseCase
import mega.privacy.android.domain.usecase.slideshow.SetSecureSlideshowTutorialShownUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SlideshowViewModelTest {

    private val savedStateHandle = mock<SavedStateHandle>()
    private val imageNodeFetchers =
        mutableMapOf<ImagePreviewFetcherSource, ImageNodeFetcher>()
    private val addImageTypeUseCase: AddImageTypeUseCase = mock()
    private val getImageUseCase: GetImageUseCase = mock()
    private val getImageFromFileUseCase: GetImageFromFileUseCase = mock()
    private val monitorSlideshowOrderSettingUseCase: MonitorSlideshowOrderSettingUseCase = mock()
    private val monitorSlideshowSpeedSettingUseCase: MonitorSlideshowSpeedSettingUseCase = mock()
    private val monitorSlideshowRepeatSettingUseCase: MonitorSlideshowRepeatSettingUseCase = mock()
    private val monitorSecureSlideshowTutorialShownUseCase: MonitorSecureSlideshowTutorialShownUseCase =
        mock()
    private val setSecureSlideshowTutorialShownUseCase: SetSecureSlideshowTutorialShownUseCase =
        mock()
    private val checkUri: CheckFileUriUseCase = mock()
    private val clearImageResultUseCase: ClearImageResultUseCase = mock()

    private fun initViewModel() = SlideshowViewModel(
        savedStateHandle = savedStateHandle,
        imageNodeFetchers = imageNodeFetchers,
        addImageTypeUseCase = addImageTypeUseCase,
        getImageUseCase = getImageUseCase,
        getImageFromFileUseCase = getImageFromFileUseCase,
        monitorSlideshowOrderSettingUseCase = monitorSlideshowOrderSettingUseCase,
        monitorSlideshowSpeedSettingUseCase = monitorSlideshowSpeedSettingUseCase,
        monitorSlideshowRepeatSettingUseCase = monitorSlideshowRepeatSettingUseCase,
        monitorSecureSlideshowTutorialShownUseCase = monitorSecureSlideshowTutorialShownUseCase,
        setSecureSlideshowTutorialShownUseCase = setSecureSlideshowTutorialShownUseCase,
        checkUri = checkUri,
        clearImageResultUseCase = clearImageResultUseCase,
    )

    @Test
    fun `test that duplicate image nodes are deduplicated in state`() = runTest {
        val duplicateId = NodeId(123L)
        val imageType = mock<StaticImageFileTypeInfo>()
        val imageNode1 = mock<ImageNode> {
            on { id } doReturn duplicateId
            on { type } doReturn imageType
            on { hasThumbnail } doReturn true
        }
        val imageNode2 = mock<ImageNode> {
            on { id } doReturn duplicateId
            on { type } doReturn imageType
            on { hasThumbnail } doReturn true
        }
        val fetcher = mock<ImageNodeFetcher>()
        whenever(fetcher.monitorImageNodes(any())) doReturn flowOf(
            listOf(imageNode1, imageNode2)
        )
        whenever(monitorSlideshowOrderSettingUseCase()) doReturn flowOf(SlideshowOrder.Newest)
        whenever(monitorSlideshowSpeedSettingUseCase()) doReturn flowOf()
        whenever(monitorSlideshowRepeatSettingUseCase()) doReturn flowOf()
        whenever(monitorSecureSlideshowTutorialShownUseCase()) doReturn flowOf(true)
        whenever(savedStateHandle.get<ImagePreviewFetcherSource>(IMAGE_NODE_FETCHER_SOURCE))
            .thenReturn(ImagePreviewFetcherSource.DEFAULT)
        whenever(savedStateHandle.get<Long>(PARAMS_CURRENT_IMAGE_NODE_ID_VALUE))
            .thenReturn(123L)
        imageNodeFetchers[ImagePreviewFetcherSource.DEFAULT] = fetcher

        val underTest = initViewModel()
        advanceUntilIdle()

        underTest.state.test {
            val state = expectMostRecentItem()
            assertThat(state.imageNodes).hasSize(1)
        }
    }
}
