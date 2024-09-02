package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import de.palm.composestateevents.EventEffect
import mega.privacy.android.app.main.controllers.ChatController
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.NodeAttachmentBottomSheetViewModel.Companion.CHAT_ID
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.NodeAttachmentBottomSheetViewModel.Companion.MESSAGE_ID
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.nodeattachment.view.NodeAttachmentBottomSheetContent
import mega.privacy.android.app.presentation.chat.NodeAttachmentHistoryViewModel
import mega.privacy.android.app.presentation.extensions.isDarkMode
import mega.privacy.android.app.presentation.transfers.starttransfer.StartDownloadViewModel
import mega.privacy.android.app.utils.Constants.IMPORT_ONLY_OPTION
import mega.privacy.android.core.ui.mapper.FileTypeIconMapper
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.GetThemeMode
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import javax.inject.Inject

@AndroidEntryPoint
internal class NodeAttachmentBottomSheetDialogFragment : BottomSheetDialogFragment() {

    private val viewModel: NodeAttachmentBottomSheetViewModel by viewModels()
    private val nodeAttachmentHistoryViewModel: NodeAttachmentHistoryViewModel by activityViewModels()
    private val startDownloadViewModel: StartDownloadViewModel by activityViewModels()
    private val chatController: ChatController by lazy { ChatController(requireActivity()) }

    @Inject
    lateinit var getThemeMode: GetThemeMode

    @Inject
    lateinit var fileTypeIconMapper: FileTypeIconMapper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val themeMode by getThemeMode()
                    .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                OriginalTempTheme(isDark = themeMode.isDarkMode()) {
                    NodeAttachmentBottomSheetContent(
                        uiState = uiState,
                        fileTypeIconMapper = fileTypeIconMapper,
                        onAvailableOfflineChecked = { checked, nodeId ->
                            if (checked) {
                                startDownloadViewModel.onSaveOfflineClicked(
                                    chatId = viewModel.chatId,
                                    messageId = viewModel.messageId
                                )
                            } else {
                                nodeAttachmentHistoryViewModel.removeChatNodeFromOffline(nodeId)
                            }
                            dismissAllowingStateLoss()
                        },
                        onSaveToDeviceClicked = {
                            startDownloadViewModel.onDownloadClicked(
                                chatId = viewModel.chatId,
                                messageId = viewModel.messageId
                            )
                            dismissAllowingStateLoss()
                        },
                        onImportClicked = {
                            /** ChatController usages should be removed when NodeAttachmentHistoryActivity.java is refactored */
                            chatController.importNode(
                                viewModel.messageId,
                                viewModel.chatId,
                                IMPORT_ONLY_OPTION
                            )
                            dismissAllowingStateLoss()
                        }
                    )
                }

                EventEffect(
                    event = uiState.errorEvent,
                    onConsumed = viewModel::onErrorEventConsumed
                ) { onBackPressed() }
            }
        }
    }

    private fun onBackPressed() {
        activity?.onBackPressedDispatcher?.onBackPressed()
    }

    companion object {
        @JvmStatic
        fun newInstance(chatId: Long, messageId: Long) =
            NodeAttachmentBottomSheetDialogFragment().apply {
                arguments = bundleOf(
                    CHAT_ID to chatId,
                    MESSAGE_ID to messageId
                )
            }
    }
}