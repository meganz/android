package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.widget.TextView
import kotlinx.android.synthetic.main.bottom_sheet_sort_by.view.*
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetSortByBinding
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import mega.privacy.android.app.utils.ColorUtils
import nz.mega.sdk.MegaApiJava.*
import java.util.*

class SortByBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {
    @SuppressLint("SetTextI18n", "RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        val binding = BottomSheetSortByBinding.inflate(LayoutInflater.from(context), null, false)

        val sortByName = getString(R.string.sortby_name)
        val sortByAsc = getString(R.string.sortby_name_ascending).toLowerCase(Locale.ROOT)
        val sortByDesc = getString(R.string.sortby_name_descending).toLowerCase(Locale.ROOT)
        binding.sortByNameAsc.text = "$sortByName ($sortByAsc)"
        binding.sortByNameDesc.text = "$sortByName ($sortByDesc)"

        val managerActivity = requireActivity() as ManagerActivityLollipop
        when (managerActivity.orderCloud) {
            ORDER_DEFAULT_ASC -> setSelectedColor(binding.sortByNameAsc)
            ORDER_DEFAULT_DESC -> setSelectedColor(binding.sortByNameDesc)
            ORDER_MODIFICATION_DESC -> setSelectedColor(binding.sortByNewestDate)
            ORDER_MODIFICATION_ASC -> setSelectedColor(binding.sortByOldestDate)
            ORDER_SIZE_DESC -> setSelectedColor(binding.sortByLargestSize)
            ORDER_SIZE_ASC -> setSelectedColor(binding.sortBySmallestSize)
        }

        binding.sortByNameAsc.setOnClickListener {
            setCloudOrder(managerActivity, ORDER_DEFAULT_ASC)
        }
        binding.sortByNameDesc.setOnClickListener {
            setCloudOrder(managerActivity, ORDER_DEFAULT_DESC)
        }
        binding.sortByNewestDate.setOnClickListener {
            setCloudOrder(managerActivity, ORDER_MODIFICATION_DESC)
        }
        binding.sortByOldestDate.setOnClickListener {
            setCloudOrder(managerActivity, ORDER_MODIFICATION_ASC)
        }
        binding.sortByLargestSize.setOnClickListener {
            setCloudOrder(managerActivity, ORDER_SIZE_DESC)
        }
        binding.sortBySmallestSize.setOnClickListener {
            setCloudOrder(managerActivity, ORDER_SIZE_ASC)
        }

        contentView = binding.root
        mainLinearLayout = binding.root.linear_layout
        items_layout = binding.root.linear_layout
        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false)
    }

    private fun setSelectedColor(text: TextView) {
        val colorSecondary = ColorUtils.getThemeColor(context, R.attr.colorSecondary)
        text.setTextColor(colorSecondary)

        var icon = text.compoundDrawablesRelative[0] ?: return
        icon = icon.mutate()
        icon.colorFilter = PorterDuffColorFilter(colorSecondary, PorterDuff.Mode.SRC_IN)
        text.setCompoundDrawablesRelative(icon, null, null, null)
    }

    private fun setCloudOrder(managerActivity: ManagerActivityLollipop, order: Int) {
        managerActivity.refreshCloudOrder(order)
        setStateBottomSheetBehaviorHidden()
    }
}
