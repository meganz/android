package mega.privacy.android.app.modalbottomsheet

import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import coil.load
import coil.transform.RoundedCornersTransformation
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.extensions.collectFlow
import mega.privacy.android.app.databinding.BottomSheetManageTransferBinding
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.main.managerSections.CompletedTransfersFragment
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.transfer.CompletedTransfer
import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaTransfer

/**
 * Manage transfer bottom sheet dialog fragment
 *
 */
internal class ManageTransferBottomSheetDialogFragment : BaseBottomSheetDialogFragment(),
    View.OnClickListener {
    private lateinit var managerActivity: ManagerActivity
    private var transfer: CompletedTransfer? = null
    private var handle: Long = 0
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
        viewLifecycleOwner.collectFlow(viewModel.uiState) {
            transfer = it.transfer
            handleCompletedTransfer()
        }
    }

    private fun handleCompletedTransfer() {
        val transfer = transfer ?: return
        val thumbnail = contentView.findViewById<ImageView>(R.id.manage_transfer_thumbnail)
        val type = contentView.findViewById<ImageView>(R.id.manage_transfer_small_icon)
        val stateIcon = contentView.findViewById<ImageView>(R.id.manage_transfer_completed_image)
        val name = contentView.findViewById<TextView>(R.id.manage_transfer_filename)
        val location = contentView.findViewById<TextView>(R.id.manage_transfer_location)
        val viewInFolderOption = contentView.findViewById<LinearLayout>(R.id.option_view_layout)
        viewInFolderOption.setOnClickListener(this)
        val getLinkOption = contentView.findViewById<LinearLayout>(R.id.option_get_link_layout)
        getLinkOption.setOnClickListener(this)
        val clearOption = contentView.findViewById<LinearLayout>(R.id.option_clear_layout)
        clearOption.setOnClickListener(this)
        val retryOption = contentView.findViewById<LinearLayout>(R.id.option_retry_layout)
        retryOption.setOnClickListener(this)
        name.text = transfer.fileName
        if (transfer.type == MegaTransfer.TYPE_DOWNLOAD) {
            type.setImageResource(R.drawable.ic_download_transfers)
            getLinkOption.visibility = View.GONE
        } else if (transfer.type == MegaTransfer.TYPE_UPLOAD) {
            type.setImageResource(R.drawable.ic_upload_transfers)
        }
        location.setTextColor(ContextCompat.getColor(requireContext(), R.color.grey_054_white_054))
        when (transfer.state) {
            MegaTransfer.STATE_COMPLETED -> {
                location.text = transfer.path
                stateIcon.setColorFilter(
                    ContextCompat.getColor(
                        requireContext(),
                        R.color.green_500_300
                    ), PorterDuff.Mode.SRC_IN
                )
                stateIcon.setImageResource(R.drawable.ic_transfers_completed)
                retryOption.visibility = View.GONE
                stateIcon.isVisible = true
            }

            MegaTransfer.STATE_FAILED -> {
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

            MegaTransfer.STATE_CANCELLED -> {
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
        if (getLinkOption.visibility == View.GONE && retryOption.visibility == View.GONE || viewInFolderOption.visibility == View.GONE) {
            contentView.findViewById<View>(R.id.separator_get_link).visibility =
                View.GONE
        }
        handle = transfer.handle
        val thumbParams = thumbnail.layoutParams as FrameLayout.LayoutParams
        thumbnail.load(ThumbnailRequest(NodeId(handle))) {
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
    }

    override fun onClick(v: View) {
        val transfer = transfer ?: return
        val id = v.id
        if (id == R.id.option_view_layout) {
            if (transfer.type == MegaTransfer.TYPE_UPLOAD && !Util.isOnline(requireContext())) {
                managerActivity.showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
            } else {
                managerActivity.openTransferLocation(transfer)
            }
        } else if (id == R.id.option_get_link_layout) {
            if (!Util.isOnline(requireContext())) {
                managerActivity.showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
            } else {
                managerActivity.showGetLinkActivity(handle)
            }
        } else if (id == R.id.option_clear_layout) {
            viewModel.completedTransferRemoved(transfer, true)
        } else if (id == R.id.option_retry_layout) {
            if (!Util.isOnline(requireContext())) {
                managerActivity.showSnackbar(
                    Constants.SNACKBAR_TYPE,
                    getString(R.string.error_server_connection_problem),
                    MegaChatApiJava.MEGACHAT_INVALID_HANDLE
                )
            } else {
                (parentFragment as? CompletedTransfersFragment)?.retrySingleTransfer(transfer)
            }
        }
        setStateBottomSheetBehaviorHidden()
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