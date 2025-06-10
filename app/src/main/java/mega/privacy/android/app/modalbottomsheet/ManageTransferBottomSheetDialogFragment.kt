package mega.privacy.android.app.modalbottomsheet

import android.content.Intent
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import coil.load
import coil.transform.RoundedCornersTransformation
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.BottomSheetManageTransferBinding
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.managerSections.CompletedTransfersFragment
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.Util
import mega.privacy.android.app.utils.Util.showSnackbar
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.domain.entity.transfer.TransferType
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import java.io.File

/**
 * Manage transfer bottom sheet dialog fragment
 *
 */
internal class ManageTransferBottomSheetDialogFragment : BaseBottomSheetDialogFragment(),
    View.OnClickListener {
    private lateinit var managerActivity: ManagerActivity
    private var transfer: CompletedTransfer? = null
    private var parentFileUri: Uri? = null
    private var fileUri: Uri? = null
    private val viewModel by viewModels<ManageTransferSheetViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = BottomSheetManageTransferBinding.inflate(layoutInflater)
        contentView = binding.root
        itemsLayout = contentView.findViewById(R.id.item_list_bottom_sheet_contact_file)
        managerActivity = requireActivity() as ManagerActivity
        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.collectFlow(viewModel.uiState) { state ->
            state.transfer?.let {
                transfer = state.transfer
                parentFileUri = state.parentUri
                fileUri = state.fileUri
                handleCompletedTransfer(state)
            }
        }
    }

    private fun handleCompletedTransfer(uiState: ManageTransferSheetUiState) {
        val transfer = transfer ?: run {
            Timber.w("Transfer is not initialized")
            return
        }

        val thumbnail = contentView.findViewById<ImageView>(R.id.manage_transfer_thumbnail)
        val type = contentView.findViewById<ImageView>(R.id.manage_transfer_small_icon)
        val stateIcon = contentView.findViewById<ImageView>(R.id.manage_transfer_completed_image)
        val name = contentView.findViewById<TextView>(R.id.manage_transfer_filename)
        val location = contentView.findViewById<TextView>(R.id.manage_transfer_location)
        val viewInFolderOption = contentView.findViewById<TextView>(R.id.option_view)
        viewInFolderOption.setOnClickListener(this)

        if ((transfer.isContentUriDownload && fileUri != null) || transfer.isContentUriDownload.not()) {
            viewInFolderOption.visibility = View.VISIBLE
        } else {
            viewInFolderOption.visibility = View.GONE
        }

        val openWith = contentView.findViewById<TextView>(R.id.option_open_with)
        openWith.setOnClickListener(this)
        val openWithSeparator = contentView.findViewById<View>(R.id.separator_open_with)
        if (transfer.type == TransferType.DOWNLOAD && fileUri != null) {
            openWith.visibility = View.VISIBLE
            openWithSeparator.visibility =
                if (viewInFolderOption.isGone) View.GONE else View.VISIBLE
        } else {
            openWith.visibility = View.GONE
            openWithSeparator.visibility = View.GONE
        }

        val getLinkOption = contentView.findViewById<TextView>(R.id.option_get_link)
        getLinkOption.text = resources.getQuantityString(
            sharedR.plurals.label_share_links,
            1
        )
        val getLinkOptionSeparator = contentView.findViewById<View>(R.id.separator_get_link)
        getLinkOption.setOnClickListener(this)

        if (uiState.iAmNodeOwner) {
            getLinkOption.visibility = View.VISIBLE
            getLinkOptionSeparator.visibility = View.VISIBLE
        } else {
            getLinkOption.visibility = View.GONE
            getLinkOptionSeparator.visibility = View.GONE
        }

        val clearOption = contentView.findViewById<TextView>(R.id.option_clear)
        clearOption.setOnClickListener(this)
        val retryOption = contentView.findViewById<TextView>(R.id.option_retry)
        val retryOptionSeparator = contentView.findViewById<View>(R.id.separator_retry)
        retryOption.setOnClickListener(this)
        name.text = transfer.fileName
        if (transfer.type.isDownloadType()) {
            type.setImageResource(R.drawable.ic_download_transfers)
        } else if (transfer.type.isUploadType()) {
            type.setImageResource(R.drawable.ic_upload_transfers)
        }
        location.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_054_white_054))
        when (transfer.state) {
            TransferState.STATE_COMPLETED -> {
                location.text = uiState.parentFilePath ?: transfer.path
                stateIcon.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.green_500_300
                    ), PorterDuff.Mode.SRC_IN
                )
                stateIcon.setImageResource(R.drawable.ic_transfers_completed)
                retryOption.visibility = View.GONE
                retryOptionSeparator.visibility = View.GONE
                stateIcon.isVisible = true
            }

            TransferState.STATE_FAILED -> {
                location.setTextColor(
                    ColorUtils.getThemeColor(
                        requireContext(),
                        com.google.android.material.R.attr.colorError
                    )
                )
                location.text = String.format(
                    "%s: %s",
                    getString(R.string.failed_label),
                    transfer.error
                )
                stateIcon.isVisible = false
                viewInFolderOption.visibility = View.GONE
                getLinkOption.visibility = View.GONE
            }

            TransferState.STATE_CANCELLED -> {
                location.setText(R.string.transfer_cancelled)
                stateIcon.isVisible = false
                viewInFolderOption.visibility = View.GONE
                getLinkOption.visibility = View.GONE
            }

            else -> {
                location.setText(R.string.transfer_unknown)
                stateIcon.clearColorFilter()
                stateIcon.setImageResource(R.drawable.ic_queue)
                stateIcon.isVisible = true
            }
        }
        if (getLinkOption.isGone && retryOption.isGone || viewInFolderOption.isGone) {
            getLinkOptionSeparator.visibility = View.GONE
        }
        val thumbParams = thumbnail.layoutParams as FrameLayout.LayoutParams
        thumbnail.load(ThumbnailRequest(NodeId(transfer.handle))) {
            size(Util.dp2px(Constants.THUMB_SIZE_DP.toFloat()))
            transformations(
                RoundedCornersTransformation(
                    Util.dp2px(Constants.THUMB_CORNER_RADIUS_DP).toFloat()
                )
            )
            listener(
                onSuccess = { _, _ ->
                    thumbParams.width = Util.dp2px(Constants.THUMB_SIZE_DP.toFloat())
                    thumbParams.height = thumbParams.width
                    thumbnail.layoutParams = thumbParams
                },
                onError = { _, _ ->
                    thumbParams.width = Util.dp2px(Constants.ICON_SIZE_DP.toFloat())
                    thumbParams.height = thumbParams.width
                    thumbnail.layoutParams = thumbParams
                    thumbnail.setImageResource(MimeTypeList.typeForName(transfer.fileName).iconResourceId)
                },
            )
        }
        calculatePeekHeight()
    }

    override fun onClick(v: View) {
        val transfer = transfer ?: run {
            Timber.w("Transfer is not initialized")
            return
        }

        val id = v.id
        if (id == R.id.option_view) {
            if (transfer.type.isUploadType() && !Util.isOnline(requireContext())) {
                managerActivity.showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
            } else {
                managerActivity.openTransferLocation(
                    transfer,
                    parentFileUri?.toString() ?: transfer.path
                )
            }
        } else if (id == R.id.option_get_link) {
            if (!Util.isOnline(requireContext())) {
                managerActivity.showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
            } else {
                managerActivity.showGetLinkActivity(transfer.handle)
            }
        } else if (id == R.id.option_clear) {
            viewModel.completedTransferRemoved(transfer, true)
        } else if (id == R.id.option_retry) {
            if (!Util.isOnline(requireContext())) {
                managerActivity.showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
            } else {
                (parentFragment as? CompletedTransfersFragment)?.retrySingleTransfer(transfer)
            }
        } else if (id == R.id.option_open_with) {
            openFileWith()
        }
        setStateBottomSheetBehaviorHidden()
    }

    private fun openFileWith() {
        val transfer = transfer ?: run {
            Timber.w("Transfer is not initialized")
            return
        }

        with(transfer) {
            var uri: Uri = originalPath.toUri()

            if (originalPath.startsWith(File.separator)) {
                val localFile = File(originalPath)
                if (FileUtil.isFileAvailable(localFile)) {
                    try {
                        FileProvider.getUriForFile(
                            requireActivity(),
                            Constants.AUTHORITY_STRING_FILE_PROVIDER,
                            localFile
                        )?.let { uri = it }
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                } else {
                    showSnackbar(requireActivity(), getString(R.string.corrupt_video_dialog_text))
                    return
                }
            } else {
                uri = fileUri ?: uri
            }

            Intent(Intent.ACTION_VIEW).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                try {
                    setDataAndType(uri, typeForName(fileName).type)
                    val chooserTitle = resources.getString(
                        sharedR.string.open_with_os_dialog_title,
                        fileName
                    )
                    val intent = Intent.createChooser(this, chooserTitle)
                    if (MegaApiUtils.isIntentAvailable(requireActivity(), intent)) {
                        startActivity(intent)
                        return
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }

        showSnackbar(requireActivity(), getString(R.string.intent_not_available))
    }

    companion object {
        const val TRANSFER_ID = "TRANSFER_ID"
        const val TAG = "ManageTransferBottomSheetDialogFragment"

        fun newInstance(completedTransferId: Int) =
            ManageTransferBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(TRANSFER_ID, completedTransferId)
                }
            }
    }
}