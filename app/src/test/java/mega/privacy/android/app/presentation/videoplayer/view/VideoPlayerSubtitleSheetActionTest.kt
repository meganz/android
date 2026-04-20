package mega.privacy.android.app.presentation.videoplayer.view

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.videoplayer.model.SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM
import mega.privacy.android.app.presentation.videoplayer.model.SUBTITLE_SELECTED_STATE_MATCHED_ITEM
import mega.privacy.android.app.presentation.videoplayer.model.SUBTITLE_SELECTED_STATE_OFF
import mega.privacy.android.domain.entity.mediaplayer.SubtitleFileInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VideoPlayerSubtitleSheetActionTest {

    private val testSubtitleFileInfo = SubtitleFileInfo(
        id = 1L,
        name = "subtitle.srt",
        url = "https://example.com/subtitle.srt",
        parentName = "parent",
        isMarkedSensitive = false,
        isSensitiveInherited = false,
    )

    @Test
    fun `test that buildSubtitleSheetRows returns Off and AddFromCloud when both params are null`() {
        val rows = buildSubtitleSheetRows(
            subtitleFileName = null,
            matchedSubtitle = null,
        )

        assertThat(rows).hasSize(2)
        assertThat(rows[0]).isEqualTo(VideoPlayerSubtitleSheetAction.Off)
        assertThat(rows[1]).isEqualTo(VideoPlayerSubtitleSheetAction.AddFromCloud)
    }

    @Test
    fun `test that buildSubtitleSheetRows includes AddedSubtitle when subtitleFileName is not null`() {
        val rows = buildSubtitleSheetRows(
            subtitleFileName = "my_subtitle.srt",
            matchedSubtitle = null,
        )

        assertThat(rows).hasSize(3)
        assertThat(rows[0]).isEqualTo(VideoPlayerSubtitleSheetAction.Off)
        assertThat(rows[1]).isEqualTo(VideoPlayerSubtitleSheetAction.AddedSubtitle("my_subtitle.srt"))
        assertThat(rows[2]).isEqualTo(VideoPlayerSubtitleSheetAction.AddFromCloud)
    }

    @Test
    fun `test that buildSubtitleSheetRows includes AutoMatched when matchedSubtitle is not null`() {
        val rows = buildSubtitleSheetRows(
            subtitleFileName = null,
            matchedSubtitle = testSubtitleFileInfo,
        )

        assertThat(rows).hasSize(3)
        assertThat(rows[0]).isEqualTo(VideoPlayerSubtitleSheetAction.Off)
        assertThat(rows[1]).isEqualTo(
            VideoPlayerSubtitleSheetAction.AutoMatched(
                testSubtitleFileInfo
            )
        )
        assertThat(rows[2]).isEqualTo(VideoPlayerSubtitleSheetAction.AddFromCloud)
    }

    @Test
    fun `test that buildSubtitleSheetRows includes all rows when both params are not null`() {
        val rows = buildSubtitleSheetRows(
            subtitleFileName = "my_subtitle.srt",
            matchedSubtitle = testSubtitleFileInfo,
        )

        assertThat(rows).hasSize(4)
        assertThat(rows[0]).isEqualTo(VideoPlayerSubtitleSheetAction.Off)
        assertThat(rows[1]).isEqualTo(VideoPlayerSubtitleSheetAction.AddedSubtitle("my_subtitle.srt"))
        assertThat(rows[2]).isEqualTo(
            VideoPlayerSubtitleSheetAction.AutoMatched(
                testSubtitleFileInfo
            )
        )
        assertThat(rows[3]).isEqualTo(VideoPlayerSubtitleSheetAction.AddFromCloud)
    }

    @Test
    fun `test that handleSubtitleSheetAction calls onOffClicked when action is Off`() {
        val onOffClicked = mock<() -> Unit>()
        val onAddedSubtitleClicked = mock<() -> Unit>()
        val onAutoMatch = mock<(SubtitleFileInfo) -> Unit>()
        val onToSelectSubtitle = mock<() -> Unit>()

        handleSubtitleSheetAction(
            VideoPlayerSubtitleSheetAction.Off,
            onOffClicked,
            onAddedSubtitleClicked,
            onAutoMatch,
            onToSelectSubtitle,
        )

        verify(onOffClicked).invoke()
        verifyNoInteractions(onAddedSubtitleClicked)
        verifyNoInteractions(onAutoMatch)
        verifyNoInteractions(onToSelectSubtitle)
    }

    @Test
    fun `test that handleSubtitleSheetAction calls onAddedSubtitleClicked when action is AddedSubtitle`() {
        val onOffClicked = mock<() -> Unit>()
        val onAddedSubtitleClicked = mock<() -> Unit>()
        val onAutoMatch = mock<(SubtitleFileInfo) -> Unit>()
        val onToSelectSubtitle = mock<() -> Unit>()

        handleSubtitleSheetAction(
            VideoPlayerSubtitleSheetAction.AddedSubtitle("file.srt"),
            onOffClicked,
            onAddedSubtitleClicked,
            onAutoMatch,
            onToSelectSubtitle,
        )

        verify(onAddedSubtitleClicked).invoke()
        verifyNoInteractions(onOffClicked)
        verifyNoInteractions(onAutoMatch)
        verifyNoInteractions(onToSelectSubtitle)
    }

    @Test
    fun `test that handleSubtitleSheetAction calls onAutoMatch when action is AutoMatched`() {
        val onOffClicked = mock<() -> Unit>()
        val onAddedSubtitleClicked = mock<() -> Unit>()
        val onAutoMatch = mock<(SubtitleFileInfo) -> Unit>()
        val onToSelectSubtitle = mock<() -> Unit>()

        handleSubtitleSheetAction(
            VideoPlayerSubtitleSheetAction.AutoMatched(testSubtitleFileInfo),
            onOffClicked,
            onAddedSubtitleClicked,
            onAutoMatch,
            onToSelectSubtitle,
        )

        verify(onAutoMatch).invoke(testSubtitleFileInfo)
        verifyNoInteractions(onOffClicked)
        verifyNoInteractions(onAddedSubtitleClicked)
        verifyNoInteractions(onToSelectSubtitle)
    }

    @Test
    fun `test that handleSubtitleSheetAction calls onToSelectSubtitle when action is AddFromCloud`() {
        val onOffClicked = mock<() -> Unit>()
        val onAddedSubtitleClicked = mock<() -> Unit>()
        val onAutoMatch = mock<(SubtitleFileInfo) -> Unit>()
        val onToSelectSubtitle = mock<() -> Unit>()

        handleSubtitleSheetAction(
            VideoPlayerSubtitleSheetAction.AddFromCloud,
            onOffClicked,
            onAddedSubtitleClicked,
            onAutoMatch,
            onToSelectSubtitle,
        )

        verify(onToSelectSubtitle).invoke()
        verifyNoInteractions(onOffClicked)
        verifyNoInteractions(onAddedSubtitleClicked)
        verifyNoInteractions(onAutoMatch)
    }

    @Test
    fun `test that isRadioSelected returns true for Off when state is SUBTITLE_SELECTED_STATE_OFF`() {
        assertThat(
            VideoPlayerSubtitleSheetAction.Off.isRadioSelected(SUBTITLE_SELECTED_STATE_OFF)
        ).isTrue()
    }

    @Test
    fun `test that isRadioSelected returns false for Off when state is not SUBTITLE_SELECTED_STATE_OFF`() {
        assertThat(
            VideoPlayerSubtitleSheetAction.Off.isRadioSelected(SUBTITLE_SELECTED_STATE_MATCHED_ITEM)
        ).isFalse()
    }

    @Test
    fun `test that isRadioSelected returns true for AddedSubtitle when state is SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM`() {
        assertThat(
            VideoPlayerSubtitleSheetAction.AddedSubtitle("file.srt")
                .isRadioSelected(SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM)
        ).isTrue()
    }

    @Test
    fun `test that isRadioSelected returns false for AddedSubtitle when state is not SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM`() {
        assertThat(
            VideoPlayerSubtitleSheetAction.AddedSubtitle("file.srt")
                .isRadioSelected(SUBTITLE_SELECTED_STATE_OFF)
        ).isFalse()
    }

    @Test
    fun `test that isRadioSelected returns true for AutoMatched when state is SUBTITLE_SELECTED_STATE_MATCHED_ITEM`() {
        assertThat(
            VideoPlayerSubtitleSheetAction.AutoMatched(testSubtitleFileInfo)
                .isRadioSelected(SUBTITLE_SELECTED_STATE_MATCHED_ITEM)
        ).isTrue()
    }

    @Test
    fun `test that isRadioSelected returns false for AutoMatched when state is not SUBTITLE_SELECTED_STATE_MATCHED_ITEM`() {
        assertThat(
            VideoPlayerSubtitleSheetAction.AutoMatched(testSubtitleFileInfo)
                .isRadioSelected(SUBTITLE_SELECTED_STATE_OFF)
        ).isFalse()
    }

    @Test
    fun `test that isRadioSelected always returns false for AddFromCloud`() {
        assertThat(
            VideoPlayerSubtitleSheetAction.AddFromCloud
                .isRadioSelected(SUBTITLE_SELECTED_STATE_OFF)
        ).isFalse()
        assertThat(
            VideoPlayerSubtitleSheetAction.AddFromCloud
                .isRadioSelected(SUBTITLE_SELECTED_STATE_MATCHED_ITEM)
        ).isFalse()
        assertThat(
            VideoPlayerSubtitleSheetAction.AddFromCloud
                .isRadioSelected(SUBTITLE_SELECTED_STATE_ADD_SUBTITLE_ITEM)
        ).isFalse()
    }
}
