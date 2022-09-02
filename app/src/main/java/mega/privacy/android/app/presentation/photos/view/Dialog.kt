package mega.privacy.android.app.presentation.photos.view

import android.app.Activity
import android.content.DialogInterface
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.PhotosFragment
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.getCurrentSort
import mega.privacy.android.app.presentation.photos.timeline.model.Sort
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.setCurrentSort
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.showingSortByDialog

internal fun PhotosFragment.showSortByDialog(context: Activity) {
    val sortDialog: AlertDialog
    val dialogBuilder = MaterialAlertDialogBuilder(context)

    val stringsArray: List<String> = listOf(
        getString(R.string.sortby_date_newest),
        getString(R.string.sortby_date_oldest),
    )
    val sortArray: List<Sort> = listOf(Sort.NEWEST, Sort.OLDEST)
    val itemsAdapter =
        ArrayAdapter(context, R.layout.checked_text_view_dialog_button, stringsArray)
    val listView = ListView(context)
    listView.adapter = itemsAdapter

    dialogBuilder.setSingleChoiceItems(
        itemsAdapter,
        timelineViewModel.getCurrentSort().ordinal) { dialog, item ->
        itemsAdapter.getItem(item)?.let {
            timelineViewModel.setCurrentSort(sortArray[item])
            timelineViewModel.sortByOrder()
        }
        dialog.dismiss()
    }

    dialogBuilder.setNegativeButton(context.getString(R.string.general_cancel)) {
            dialog: DialogInterface,
            _,
        ->
        dialog.dismiss()
    }

    sortDialog = dialogBuilder.create()
    sortDialog.setTitle(R.string.action_sort_by)
    sortDialog.show()
    sortDialog.setOnDismissListener {
        timelineViewModel.showingSortByDialog(false)
    }
}