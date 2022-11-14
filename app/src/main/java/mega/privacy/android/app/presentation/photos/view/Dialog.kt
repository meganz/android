package mega.privacy.android.app.presentation.photos.view

import android.app.Activity
import android.content.DialogInterface
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import mega.privacy.android.app.R

fun Fragment.showSortByDialog(
    context: Activity,
    checkedItem: Int,
    onClickListener: (DialogInterface, Int) -> Unit,
    onDismissListener: (DialogInterface) -> Unit = {},
) {
    val sortDialog: AlertDialog
    val dialogBuilder = MaterialAlertDialogBuilder(context)

    val stringsArray: List<String> = listOf(
        getString(R.string.sortby_date_newest),
        getString(R.string.sortby_date_oldest),
    )
    val itemsAdapter =
        ArrayAdapter(context, R.layout.checked_text_view_dialog_button, stringsArray)
    val listView = ListView(context)
    listView.adapter = itemsAdapter

    dialogBuilder.setSingleChoiceItems(
        itemsAdapter,
        checkedItem) { dialog, i ->
        onClickListener(dialog, i)
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
        onDismissListener(it)
    }
}