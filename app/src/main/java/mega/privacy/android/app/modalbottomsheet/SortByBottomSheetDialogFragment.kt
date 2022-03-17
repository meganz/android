package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetSortByBinding
import mega.privacy.android.app.globalmanagement.SortOrderManagement
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.callManager
import nz.mega.sdk.MegaApiJava.*
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SortByBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private const val ORDER_TYPE = "ORDER_TYPE"

        @JvmStatic
        fun newInstance(orderType: Int): SortByBottomSheetDialogFragment {
            val fragment = SortByBottomSheetDialogFragment()
            val args = Bundle()

            args.putInt(ORDER_TYPE, orderType)
            fragment.arguments = args

            return fragment
        }
    }

    @Inject
    lateinit var sortOrderManagement: SortOrderManagement

    private lateinit var binding: BottomSheetSortByBinding

    private var oldOrder: Int = ORDER_DEFAULT_ASC
    private var orderType: Int = ORDER_CLOUD
    private var isIncomingRootOrder: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetSortByBinding.inflate(LayoutInflater.from(context), null, false)
        contentView = binding.root
        itemsLayout = binding.linearLayout
        return contentView
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sortByName = getString(R.string.sortby_name)
        val sortByAsc = getString(R.string.sortby_name_ascending).lowercase(Locale.ROOT)
        val sortByDesc = getString(R.string.sortby_name_descending).lowercase(Locale.ROOT)
        binding.sortByNameAsc.text = "$sortByName ($sortByAsc)"
        binding.sortByNameDesc.text = "$sortByName ($sortByDesc)"

        orderType = arguments?.getInt(ORDER_TYPE)!!

        oldOrder = when (orderType) {
            ORDER_CLOUD -> sortOrderManagement.getOrderCloud()
            ORDER_CAMERA -> sortOrderManagement.getOrderCamera()
            ORDER_OTHERS -> sortOrderManagement.getOrderOthers()
            ORDER_OFFLINE -> sortOrderManagement.getOrderOffline()
            else -> ORDER_DEFAULT_ASC
        }

        when (orderType) {
            ORDER_CAMERA -> {
                binding.sortByNameAsc.isVisible = false
                binding.sortByNameDesc.isVisible = false
                binding.sortByNameSeparator.isVisible = false
                binding.sortByLargestSize.isVisible = false
                binding.sortBySmallestSize.isVisible = false
                binding.sortBySizeSeparator.isVisible = false
                binding.sortByFavoritesType.isVisible = false
                binding.sortByLabelType.isVisible = false
                binding.sortByPhotosMediaType.isVisible = true
                binding.sortByVideosMediaType.isVisible = true
            }
            ORDER_OTHERS -> {
                binding.sortByNameSeparator.isVisible = false
                binding.sortByLargestSize.isVisible = false
                binding.sortBySmallestSize.isVisible = false
                binding.sortBySizeSeparator.isVisible = false
                binding.sortByNewestDate.isVisible = false
                binding.sortByOldestDate.isVisible = false
            }
            ORDER_OFFLINE -> {
                binding.sortByDateSeparator.isVisible = false
                binding.sortByFavoritesType.isVisible = false
                binding.sortByLabelType.isVisible = false
            }
        }

        when (oldOrder) {
            ORDER_DEFAULT_ASC -> setSelectedColor(binding.sortByNameAsc)
            ORDER_DEFAULT_DESC -> setSelectedColor(binding.sortByNameDesc)
            ORDER_CREATION_ASC -> setSelectedColor(binding.sortByNewestDate)
            ORDER_MODIFICATION_DESC -> setSelectedColor(binding.sortByNewestDate)
            ORDER_CREATION_DESC -> setSelectedColor(binding.sortByOldestDate)
            ORDER_MODIFICATION_ASC -> setSelectedColor(binding.sortByOldestDate)
            ORDER_SIZE_DESC -> setSelectedColor(binding.sortByLargestSize)
            ORDER_SIZE_ASC -> setSelectedColor(binding.sortBySmallestSize)
            ORDER_FAV_ASC -> setSelectedColor(binding.sortByFavoritesType)
            ORDER_LABEL_ASC -> setSelectedColor(binding.sortByLabelType)
            ORDER_PHOTO_DESC -> setSelectedColor(binding.sortByPhotosMediaType)
            ORDER_VIDEO_DESC -> setSelectedColor(binding.sortByVideosMediaType)
        }

        binding.sortByNameAsc.setOnClickListener {
            setNewOrder(ORDER_DEFAULT_ASC)
        }

        binding.sortByNameDesc.setOnClickListener {
            setNewOrder(ORDER_DEFAULT_DESC)
        }

        binding.sortByNewestDate.setOnClickListener {
            setNewOrder(ORDER_MODIFICATION_DESC)
        }

        binding.sortByOldestDate.setOnClickListener {
            setNewOrder(ORDER_MODIFICATION_ASC)
        }

        binding.sortByLargestSize.setOnClickListener {
            setNewOrder(ORDER_SIZE_DESC)
        }

        binding.sortBySmallestSize.setOnClickListener {
            setNewOrder(ORDER_SIZE_ASC)
        }

        binding.sortByFavoritesType.setOnClickListener {
            setNewOrder(ORDER_FAV_ASC)
        }

        binding.sortByLabelType.setOnClickListener {
            setNewOrder(ORDER_LABEL_ASC)
        }

        binding.sortByPhotosMediaType.setOnClickListener {
            setNewOrder(ORDER_PHOTO_DESC)
        }

        binding.sortByVideosMediaType.setOnClickListener {
            setNewOrder(ORDER_VIDEO_DESC)
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun setSelectedColor(text: TextView) {
        val colorSecondary = ColorUtils.getThemeColor(requireContext(), R.attr.colorSecondary)
        text.setTextColor(colorSecondary)

        var icon = text.compoundDrawablesRelative[0] ?: return
        icon = icon.mutate()
        icon.colorFilter = PorterDuffColorFilter(colorSecondary, PorterDuff.Mode.SRC_IN)
        text.setCompoundDrawablesRelative(icon, null, null, null)
    }

    private fun setNewOrder(order: Int) {
        if (oldOrder == order) {
            return
        }

        when (orderType) {
            ORDER_CLOUD -> {
                sortOrderManagement.setOrderCloud(order)
                LiveEventBus.get(EVENT_ORDER_CHANGE, Triple::class.java)
                    .post(
                        Triple(
                            order,
                            sortOrderManagement.getOrderOthers(),
                            sortOrderManagement.getOrderOffline()
                        )
                    )

                if (requireActivity() is ManagerActivity) {
                    (requireActivity() as ManagerActivity).refreshCloudOrder(order)
                } else if (requireActivity() is FileExplorerActivity) {
                    updateFileExplorerOrder(order)
                }
            }
            ORDER_CAMERA -> {
                sortOrderManagement.setOrderCamera(order)

                callManager { manager ->
                    manager.refreshCUNodes()

                    if(manager.isInMDPage) {
                        manager.mdFragment.loadPhotos()
                    }

                    if(manager.isInAlbumContentPage) {
                        manager.albumContentFragment.loadPhotos()
                    }
                }
            }
            ORDER_OTHERS -> {
                sortOrderManagement.setOrderOthers(order)
                LiveEventBus.get(EVENT_ORDER_CHANGE, Triple::class.java)
                    .post(
                        Triple(
                            sortOrderManagement.getOrderCloud(),
                            order,
                            sortOrderManagement.getOrderOffline()
                        )
                    )

                if (requireActivity() is ManagerActivity) {
                    (requireActivity() as ManagerActivity).refreshOthersOrder()
                } else if (requireActivity() is FileExplorerActivity) {
                    updateFileExplorerOrder(order)
                }
            }
            ORDER_OFFLINE -> {
                sortOrderManagement.setOrderOffline(order)
                LiveEventBus.get(EVENT_ORDER_CHANGE, Triple::class.java)
                    .post(
                        Triple(
                            sortOrderManagement.getOrderCloud(),
                            sortOrderManagement.getOrderOthers(),
                            order
                        )
                    )

                callManager { manager ->
                    manager.refreshOthersOrder()
                }
            }
        }

        setStateBottomSheetBehaviorHidden()
    }

    private fun updateFileExplorerOrder(order: Int) {
        (requireActivity() as FileExplorerActivity).refreshOrderNodes(order)

        requireActivity().sendBroadcast(
            Intent(BROADCAST_ACTION_INTENT_UPDATE_ORDER).putExtra(
                IS_CLOUD_ORDER,
                orderType == ORDER_CLOUD
            ).putExtra(NEW_ORDER, order)
        )
    }
}
