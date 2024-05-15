package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetSortByBinding
import mega.privacy.android.app.fragments.homepage.SortByHeaderViewModel
import mega.privacy.android.app.fragments.homepage.model.SortOrderState
import mega.privacy.android.app.utils.ColorUtils
import mega.privacy.android.app.utils.Constants.ORDER_CAMERA
import mega.privacy.android.app.utils.Constants.ORDER_CLOUD
import mega.privacy.android.app.utils.Constants.ORDER_FAVOURITES
import mega.privacy.android.app.utils.Constants.ORDER_OFFLINE
import mega.privacy.android.app.utils.Constants.ORDER_OTHERS
import mega.privacy.android.app.utils.Constants.ORDER_VIDEO_PLAYLIST
import mega.privacy.android.data.mapper.SortOrderIntMapper
import mega.privacy.android.domain.entity.SortOrder
import java.util.Locale
import javax.inject.Inject

/**
 * A [BaseBottomSheetDialogFragment] that displays a list of Sort Options
 */
@AndroidEntryPoint
class SortByBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {

    companion object {
        private const val ORDER_TYPE = "ORDER_TYPE"

        /**
         * Specify behavior when this Fragment is initialized
         */
        @JvmStatic
        fun newInstance(orderType: Int): SortByBottomSheetDialogFragment {
            val fragment = SortByBottomSheetDialogFragment()
            val args = Bundle()

            args.putInt(ORDER_TYPE, orderType)
            fragment.arguments = args

            return fragment
        }
    }

    private lateinit var binding: BottomSheetSortByBinding

    private var orderType: Int = ORDER_CLOUD

    /**
     * SortByHeaderViewModel
     */
    val sortByHeaderViewModel: SortByHeaderViewModel by activityViewModels()

    /**
     * SortOrderIntMapper
     */
    @Inject
    lateinit var sortOrderIntMapper: SortOrderIntMapper


    /**
     * onCreateView()
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = BottomSheetSortByBinding.inflate(LayoutInflater.from(context), null, false)
        contentView = binding.root
        itemsLayout = binding.linearLayout
        return contentView
    }

    /**
     * onViewCreated()
     */
    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        sortByHeaderViewModel.refreshData()
        val sortByName = getString(R.string.sortby_name)
        val sortByAsc = getString(R.string.sortby_name_ascending).lowercase(Locale.ROOT)
        val sortByDesc = getString(R.string.sortby_name_descending).lowercase(Locale.ROOT)
        binding.sortByNameAsc.text = "$sortByName ($sortByAsc)"
        binding.sortByNameDesc.text = "$sortByName ($sortByDesc)"

        orderType = arguments?.getInt(ORDER_TYPE) ?: ORDER_CLOUD

        sortByHeaderViewModel.setOrderType(orderType)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                sortByHeaderViewModel.oldOrder.collectLatest { oldOrder ->
                    if (orderType == ORDER_VIDEO_PLAYLIST) {
                        when (oldOrder) {
                            SortOrder.ORDER_SIZE_DESC,
                            SortOrder.ORDER_SIZE_ASC,
                            SortOrder.ORDER_FAV_ASC,
                            SortOrder.ORDER_LABEL_ASC,
                            SortOrder.ORDER_DEFAULT_ASC,
                            -> setSelectedColor(binding.sortByNameAsc)

                            SortOrder.ORDER_DEFAULT_DESC -> setSelectedColor(binding.sortByNameDesc)
                            SortOrder.ORDER_MODIFICATION_DESC -> setSelectedColor(binding.sortByNewestDate)
                            SortOrder.ORDER_MODIFICATION_ASC -> setSelectedColor(binding.sortByOldestDate)

                            else -> {}
                        }
                    } else {
                        when (oldOrder) {
                            SortOrder.ORDER_DEFAULT_ASC -> setSelectedColor(binding.sortByNameAsc)
                            SortOrder.ORDER_DEFAULT_DESC -> setSelectedColor(binding.sortByNameDesc)
                            SortOrder.ORDER_CREATION_ASC -> setSelectedColor(binding.sortByNewestDate)
                            SortOrder.ORDER_MODIFICATION_DESC -> setSelectedColor(binding.sortByNewestDate)
                            SortOrder.ORDER_CREATION_DESC -> setSelectedColor(binding.sortByOldestDate)
                            SortOrder.ORDER_MODIFICATION_ASC -> setSelectedColor(binding.sortByOldestDate)
                            SortOrder.ORDER_SIZE_DESC -> setSelectedColor(binding.sortByLargestSize)
                            SortOrder.ORDER_SIZE_ASC -> setSelectedColor(binding.sortBySmallestSize)
                            SortOrder.ORDER_FAV_ASC -> setSelectedColor(binding.sortByFavoritesType)
                            SortOrder.ORDER_LABEL_ASC -> setSelectedColor(binding.sortByLabelType)
                            else -> {}
                        }
                    }
                }
            }
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

            ORDER_FAVOURITES -> {
                binding.sortByFavoritesType.isVisible = false
            }

            ORDER_VIDEO_PLAYLIST -> {
                binding.sortByLargestSize.isVisible = false
                binding.sortBySmallestSize.isVisible = false
                binding.sortByFavoritesType.isVisible = false
                binding.sortByLabelType.isVisible = false
            }
        }

        binding.sortByNameAsc.setOnClickListener {
            setNewOrder(SortOrder.ORDER_DEFAULT_ASC)
        }

        binding.sortByNameDesc.setOnClickListener {
            setNewOrder(SortOrder.ORDER_DEFAULT_DESC)
        }

        binding.sortByNewestDate.setOnClickListener {
            setNewOrder(SortOrder.ORDER_MODIFICATION_DESC)
        }

        binding.sortByOldestDate.setOnClickListener {
            setNewOrder(SortOrder.ORDER_MODIFICATION_ASC)
        }

        binding.sortByLargestSize.setOnClickListener {
            setNewOrder(SortOrder.ORDER_SIZE_DESC)
        }

        binding.sortBySmallestSize.setOnClickListener {
            setNewOrder(SortOrder.ORDER_SIZE_ASC)
        }

        binding.sortByFavoritesType.setOnClickListener {
            setNewOrder(SortOrder.ORDER_FAV_ASC)
        }

        binding.sortByLabelType.setOnClickListener {
            setNewOrder(SortOrder.ORDER_LABEL_ASC)
        }

        super.onViewCreated(view, savedInstanceState)
    }

    private fun setSelectedColor(text: TextView) {
        val colorSecondary = ColorUtils.getThemeColor(
            requireContext(),
            com.google.android.material.R.attr.colorSecondary
        )
        text.setTextColor(colorSecondary)

        var icon = text.compoundDrawablesRelative[0] ?: return
        icon = icon.mutate()
        icon.colorFilter = PorterDuffColorFilter(colorSecondary, PorterDuff.Mode.SRC_IN)
        text.setCompoundDrawablesRelative(icon, null, null, null)
    }

    private fun setNewOrder(order: SortOrder) {
        lifecycleScope.launch {
            if (sortByHeaderViewModel.oldOrder.value == order) {
                return@launch
            }

            when (orderType) {
                ORDER_FAVOURITES,
                ORDER_CLOUD,
                ORDER_VIDEO_PLAYLIST,
                -> {
                    sortByHeaderViewModel.setOrderCloud(order).join()
                    sortByHeaderViewModel.updateWhenOrderChanged(
                        SortOrderState(
                            order,
                            sortByHeaderViewModel.othersSortOrder.value,
                            sortByHeaderViewModel.offlineSortOrder.value,
                        )
                    )
                }

                ORDER_CAMERA -> {
                    sortByHeaderViewModel.setOrderCamera(order).join()
                }

                ORDER_OTHERS -> {
                    sortByHeaderViewModel.setOrderOthers(order).join()
                    sortByHeaderViewModel.updateWhenOrderChanged(
                        SortOrderState(
                            sortByHeaderViewModel.cloudSortOrder.value,
                            order,
                            sortByHeaderViewModel.offlineSortOrder.value,
                        )
                    )
                }

                ORDER_OFFLINE -> {
                    sortByHeaderViewModel.setOrderOffline(order).join()
                    sortByHeaderViewModel.updateWhenOrderChanged(
                        SortOrderState(
                            sortByHeaderViewModel.cloudSortOrder.value,
                            sortByHeaderViewModel.othersSortOrder.value,
                            order
                        )
                    )
                }
            }

            setStateBottomSheetBehaviorHidden()
        }
    }

    /**
     * onDestroy()
     */
    override fun onDestroy() {
        super.onDestroy()
        sortByHeaderViewModel.resetOlderOrder()
    }
}
