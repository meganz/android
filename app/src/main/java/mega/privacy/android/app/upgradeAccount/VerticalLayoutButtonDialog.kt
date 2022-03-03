package mega.privacy.android.app.upgradeAccount

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import mega.privacy.android.app.databinding.DialogButtonVerticalLayoutBinding

/**
 * Customised dialog that buttons are vertical layout
 * @param context Context
 * @param title dialog title
 * @param message dialog message
 * @param positiveButtonTitle title for positive button
 * @param onPositiveButtonClicked clicked listener of positive button
 * @param onDismissClicked clicked listener of dismiss button
 */
class VerticalLayoutButtonDialog(
    context: Context,
    private val title: String,
    private val message: String,
    private val positiveButtonTitle: String,
    private val onPositiveButtonClicked: (dialog: Dialog) -> Unit,
    private val onDismissClicked: (dialog: Dialog) -> Unit
): Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        val binding = DialogButtonVerticalLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding) {
            dialogTitle.text = title
            dialogMessage.text = message
            buttonPositive.text = positiveButtonTitle
            buttonPositive.setOnClickListener {
                onPositiveButtonClicked(this@VerticalLayoutButtonDialog)
            }
            buttonDismiss.setOnClickListener {
                onDismissClicked(this@VerticalLayoutButtonDialog)
            }
        }
    }
}