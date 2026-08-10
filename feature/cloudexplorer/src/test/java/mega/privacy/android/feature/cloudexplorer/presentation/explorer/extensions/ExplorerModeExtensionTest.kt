package mega.privacy.android.feature.cloudexplorer.presentation.explorer.extensions

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExplorerModeExtensionTest {

    @Test
    fun `test that every explorer mode maps to a title`() {
        ExplorerMode.entries.forEach { mode ->
            assertThat(mode.titleStringId).isAnyOf(
                sharedR.string.video_section_video_selected_top_bar_title,
                sharedR.string.cloud_explorer_select_destination_title,
                sharedR.string.home_pinned_choose_files_and_folders,
            )
        }
    }

    @ParameterizedTest(name = "{0} title")
    @MethodSource("titleCases")
    fun `test that the explorer mode maps to the expected title`(
        mode: ExplorerMode,
        expected: Int,
    ) {
        assertThat(mode.titleStringId).isEqualTo(expected)
    }

    @ParameterizedTest(name = "{0} action")
    @MethodSource("actionCases")
    fun `test that the explorer mode maps to the expected action label`(
        mode: ExplorerMode,
        expected: Int,
    ) {
        assertThat(mode.actionStringId).isEqualTo(expected)
    }

    companion object {
        @JvmStatic
        private fun titleCases() = listOf(
            Arguments.of(
                ExplorerMode.ShareFilesToChat,
                sharedR.string.video_section_video_selected_top_bar_title,
            ),
            Arguments.of(
                ExplorerMode.AddVideosToPlaylist,
                sharedR.string.video_section_video_selected_top_bar_title,
            ),
            Arguments.of(
                ExplorerMode.Copy,
                sharedR.string.cloud_explorer_select_destination_title,
            ),
            Arguments.of(
                ExplorerMode.Move,
                sharedR.string.cloud_explorer_select_destination_title,
            ),
            Arguments.of(
                ExplorerMode.PinToHome,
                sharedR.string.home_pinned_choose_files_and_folders,
            ),
        )

        @JvmStatic
        private fun actionCases() = listOf(
            Arguments.of(ExplorerMode.ShareFilesToMega, sharedR.string.general_upload_label),
            Arguments.of(ExplorerMode.ShareTextToMega, sharedR.string.general_upload_label),
            Arguments.of(ExplorerMode.ShareURLToMega, sharedR.string.general_upload_label),
            Arguments.of(ExplorerMode.SaveScannedDocument, sharedR.string.general_upload_label),
            Arguments.of(ExplorerMode.ShareFilesToChat, sharedR.string.context_send),
            Arguments.of(ExplorerMode.Move, sharedR.string.general_move),
            Arguments.of(ExplorerMode.Copy, sharedR.string.general_copy),
            Arguments.of(
                ExplorerMode.SelectCUFolder,
                sharedR.string.cloud_explorer_use_this_folder_button,
            ),
            Arguments.of(
                ExplorerMode.SelectSyncFolder,
                sharedR.string.cloud_explorer_use_this_folder_button,
            ),
            Arguments.of(
                ExplorerMode.SelectStopBackupDestination,
                sharedR.string.cloud_explorer_use_this_folder_button,
            ),
            Arguments.of(ExplorerMode.Import, sharedR.string.general_action_save),
            Arguments.of(ExplorerMode.AlbumImport, sharedR.string.general_action_save),
            Arguments.of(
                ExplorerMode.AddVideosToPlaylist,
                sharedR.string.video_to_playlist_add_button,
            ),
            Arguments.of(
                ExplorerMode.PinToHome,
                sharedR.string.general_dialog_choose_button,
            ),
        )
    }
}
