package mega.privacy.android.app.modalbottomsheet

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.view.LayoutInflater
import android.widget.TextView
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.BottomSheetSortByBinding
import mega.privacy.android.app.lollipop.ManagerActivityLollipop
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_ASC
import nz.mega.sdk.MegaApiJava.ORDER_DEFAULT_DESC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_ASC
import nz.mega.sdk.MegaApiJava.ORDER_MODIFICATION_DESC
import nz.mega.sdk.MegaApiJava.ORDER_SIZE_ASC
import nz.mega.sdk.MegaApiJava.ORDER_SIZE_DESC

class SortByBottomSheetDialogFragment : BaseBottomSheetDialogFragment() {
    @SuppressLint("SetTextI18n", "RestrictedApi")
    override fun setupDialog(dialog: Dialog, style: Int) {
        super.setupDialog(dialog, style)

        val binding = BottomSheetSortByBinding.inflate(LayoutInflater.from(context), null, false)

        val sortByName = getString(R.string.sortby_name)
        val sortByAsc = getString(R.string.sortby_name_ascending)
        val sortByDesc = getString(R.string.sortby_name_descending)
        binding.sortByNameAsc.text = "$sortByName     $sortByAsc"
        binding.sortByNameDesc.text = "$sortByName     $sortByDesc"

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
        mainLinearLayout = binding.root
        items_layout = binding.root
        dialog.setContentView(contentView)
        setBottomSheetBehavior(HEIGHT_HEADER_LOW, false)
    }

    private fun setSelectedColor(text: TextView) =
        text.setTextColor(ContextCompat.getColor(context, R.color.accentColor))

    private fun setCloudOrder(managerActivity: ManagerActivityLollipop, order: Int) {
        managerActivity.refreshCloudOrder(order)
        setStateBottomSheetBehaviorHidden()
    }
}
