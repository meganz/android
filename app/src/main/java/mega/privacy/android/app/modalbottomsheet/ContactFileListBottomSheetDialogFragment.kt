package mega.privacy.android.app.modalbottomsheet

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import coil3.SingletonImageLoader
import coil3.asDrawable
import coil3.asImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import mega.privacy.android.app.MimeTypeList.Companion.typeForName
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.ActionNodeCallback
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.main.ContactFileListActivity
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.MegaApiUtils
import mega.privacy.android.app.utils.MegaNodeDialogUtil.showRenameNodeDialog
import mega.privacy.android.app.utils.MegaNodeUtil.manageEditTextFileIntent
import mega.privacy.android.app.utils.Util
import mega.privacy.android.domain.entity.node.thumbnail.ThumbnailRequest.Companion.fromHandle
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaShare
import timber.log.Timber

class ContactFileListBottomSheetDialogFragment : BaseBottomSheetDialogFragment(),
    View.OnClickListener {
    private var node: MegaNode? = null

    private var contactFileListActivity: ContactFileListActivity? = null
    private var contactInfoActivity: ContactInfoActivity? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        contentView = View.inflate(context, R.layout.bottom_sheet_contact_file_list, null)
        itemsLayout = contentView.findViewById(R.id.item_list_bottom_sheet_contact_file)

        if (requireActivity() is ContactFileListActivity) {
            contactFileListActivity = requireActivity() as ContactFileListActivity
        } else if (requireActivity() is ContactInfoActivity) {
            contactInfoActivity = requireActivity() as ContactInfoActivity
        }

        if (savedInstanceState != null) {
            val handle = savedInstanceState.getLong(Constants.HANDLE, MegaApiJava.INVALID_HANDLE)
            node = megaApi.getNodeByHandle(handle)
        } else if (requireActivity() is ContactFileListActivity) {
            node = contactFileListActivity?.selectedNode
        } else if (requireActivity() is ContactInfoActivity) {
            node = contactInfoActivity?.getSelectedNode()
        }

        return contentView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val node = node
        if (node == null) {
            Timber.w("Node NULL")
            return
        }

        val nodeThumb = contentView.findViewById<ImageView>(R.id.contact_file_list_thumbnail)
        val nodeName = contentView.findViewById<TextView>(R.id.contact_file_list_name_text)
        val nodeInfo = contentView.findViewById<TextView>(R.id.contact_file_list_info_text)
        val nodeIconLayout =
            contentView.findViewById<RelativeLayout>(R.id.contact_file_list_relative_layout_icon)
        val nodeIcon = contentView.findViewById<ImageView>(R.id.contact_file_list_icon)
        val optionDownload = contentView.findViewById<TextView>(R.id.download_option)
        val optionInfo = contentView.findViewById<TextView>(R.id.properties_option)
        val optionLeave = contentView.findViewById<TextView>(R.id.leave_option)
        val optionCopy = contentView.findViewById<TextView>(R.id.copy_option)
        val optionMove = contentView.findViewById<TextView>(R.id.move_option)
        val optionRename = contentView.findViewById<TextView>(R.id.rename_option)
        val optionRubbish = contentView.findViewById<TextView>(R.id.rubbish_bin_option)

        optionDownload.setOnClickListener(this)
        optionInfo.setOnClickListener(this)
        optionCopy.setOnClickListener(this)
        optionMove.setOnClickListener(this)
        optionRename.setOnClickListener(this)
        optionLeave.setOnClickListener(this)
        optionRubbish.setOnClickListener(this)

        val separatorInfo = contentView.findViewById<LinearLayout>(R.id.separator_info)
        val separatorDownload = contentView.findViewById<LinearLayout>(R.id.separator_download)
        val separatorModify = contentView.findViewById<LinearLayout>(R.id.separator_modify)

        nodeName.maxWidth = Util.scaleWidthPx(200, resources.displayMetrics)
        nodeInfo.maxWidth = Util.scaleWidthPx(200, resources.displayMetrics)

        nodeName.text = node.name

        val firstLevel = node.isInShare
        var parentHandle = MegaApiJava.INVALID_HANDLE
        if (requireActivity() is ContactFileListActivity) {
            parentHandle = contactFileListActivity?.getParentHandle() ?: MegaApiJava.INVALID_HANDLE
        }

        val accessLevel = megaApi.getAccess(node)

        optionInfo.setText(R.string.general_info)
        if (node.isFolder) {
            nodeThumb.setImageResource(mega.privacy.android.icon.pack.R.drawable.ic_folder_incoming_medium_solid)
            nodeInfo.text = MegaApiUtils.getMegaNodeFolderInfo(node, requireContext())

            if (!node.isTakenDown && (firstLevel || parentHandle == MegaApiJava.INVALID_HANDLE)) {
                when (accessLevel) {
                    MegaShare.ACCESS_FULL -> nodeIcon.setImageResource(R.drawable.ic_shared_fullaccess)
                    MegaShare.ACCESS_READ -> nodeIcon.setImageResource(R.drawable.ic_shared_read)
                    MegaShare.ACCESS_READWRITE -> nodeIcon.setImageResource(R.drawable.ic_shared_read_write)
                }
            } else {
                optionLeave.visibility = View.GONE
                nodeIconLayout.visibility = View.GONE
            }
        } else {
            val nodeSize = node.size
            nodeInfo.text = Util.getSizeString(nodeSize, requireContext())
            nodeIconLayout.visibility = View.GONE
            if (node.hasThumbnail()) {
                val placeholder = ContextCompat.getDrawable(
                    requireContext(),
                    typeForName(node.name).iconResourceId
                )?.asImage()
                val imageRequest = ImageRequest.Builder(requireContext())
                    .placeholder(placeholder)
                    .data(fromHandle(node.handle))
                    .size(resources.getDimensionPixelSize(R.dimen.default_thumbnail_size))
                    .transformations(
                        RoundedCornersTransformation(
                            resources.getDimension(R.dimen.thumbnail_corner_radius)
                        )
                    )
                    .target { image ->
                        nodeThumb.setImageDrawable(image.asDrawable(requireContext().resources))
                    }
                    .crossfade(true)
                    .build()

                SingletonImageLoader.get(requireContext()).enqueue(imageRequest)
            } else {
                nodeThumb.setImageResource(typeForName(node.name).iconResourceId)
            }
            optionLeave.visibility = View.GONE

            if (typeForName(node.name).isOpenableTextFile(node.size)
                && accessLevel >= MegaShare.ACCESS_READWRITE
            ) {
                val optionEdit = contentView.findViewById<LinearLayout>(R.id.edit_file_option)
                optionEdit.setVisibility(View.VISIBLE)
                optionEdit.setOnClickListener(this)
            }
        }

        when (accessLevel) {
            MegaShare.ACCESS_FULL -> {
                if (firstLevel || parentHandle == MegaApiJava.INVALID_HANDLE) {
                    optionRubbish.visibility = View.GONE
                    optionMove.visibility = View.GONE
                }
            }

            MegaShare.ACCESS_READ, MegaShare.ACCESS_READWRITE -> {
                optionMove.visibility = View.GONE
                optionRename.visibility = View.GONE
                optionRubbish.visibility = View.GONE
            }
        }

        if (node.isTakenDown) {
            optionDownload.visibility = View.GONE
            optionCopy.visibility = View.GONE
        }

        if (optionInfo.isGone || (optionDownload.isGone && optionCopy.isGone && optionMove.isGone && optionLeave.isGone && optionRename.isGone && optionRubbish.isGone)) {
            separatorInfo.visibility = View.GONE
        } else {
            separatorInfo.visibility = View.VISIBLE
        }

        if (optionDownload.isGone || (optionCopy.isGone && optionMove.isGone && optionRename.isGone && optionLeave.isGone && optionRubbish.isGone)) {
            separatorDownload.visibility = View.GONE
        } else {
            separatorDownload.visibility = View.VISIBLE
        }

        if ((optionCopy.isGone && optionMove.isGone && optionRename.isGone) || (optionLeave.isGone && optionRubbish.isGone)) {
            separatorModify.visibility = View.GONE
        } else {
            separatorModify.visibility = View.VISIBLE
        }

        super.onViewCreated(view, savedInstanceState)
    }

    override fun onClick(v: View) {
        val node = node
        if (node == null) {
            Timber.w("The selected node is NULL")
            return
        }

        val handleList = ArrayList<Long>()
        handleList.add(node.handle)

        val id = v.id
        if (id == R.id.download_option) {
            if (requireActivity() is ContactFileListActivity) {
                contactFileListActivity?.downloadFile(listOfNotNull(node))
            } else if (requireActivity() is ContactInfoActivity) {
                contactInfoActivity?.downloadFile(listOfNotNull(node))
            }
        } else if (id == R.id.properties_option) {
            val i = Intent(requireContext(), FileInfoActivity::class.java)
            i.putExtra(Constants.HANDLE, node.handle)
            i.putExtra("from", Constants.FROM_INCOMING_SHARES)
            i.putExtra(Constants.INTENT_EXTRA_KEY_FIRST_LEVEL, node.isInShare)
            i.putExtra(Constants.NAME, node.name)
            startActivity(i)
        } else if (id == R.id.leave_option) {
            if (requireActivity() is OnSharedFolderUpdatedCallBack) {
                (requireActivity() as OnSharedFolderUpdatedCallBack).showLeaveFolderDialog(
                    listOf(node.handle)
                )
            } else {
                Timber.w("The activity is not an instance of OnFolderLeaveCallBack")
            }
        } else if (id == R.id.rename_option) {
            showRenameNodeDialog(
                requireActivity(), node, activity as? SnackbarShower,
                activity as? ActionNodeCallback
            )
        } else if (id == R.id.move_option) {
            if (requireActivity() is ContactFileListActivity) {
                contactFileListActivity?.showMove(handleList)
            }
        } else if (id == R.id.copy_option) {
            if (requireActivity() is ContactFileListActivity) {
                contactFileListActivity?.showCopy(handleList)
            } else if (requireActivity() is ContactInfoActivity) {
                contactInfoActivity?.showCopy(handleList)
            }
        } else if (id == R.id.rubbish_bin_option) {
            if (requireActivity() is ContactFileListActivity) {
                contactFileListActivity?.askConfirmationMoveToRubbish(handleList)
            }
        } else if (id == R.id.edit_file_option) {
            manageEditTextFileIntent(requireContext(), node, Constants.CONTACT_FILE_ADAPTER)
        }

        setStateBottomSheetBehaviorHidden()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val handle = node?.handle ?: MegaApiJava.INVALID_HANDLE
        outState.putLong(Constants.HANDLE, handle)
    }
}
