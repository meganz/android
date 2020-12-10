package mega.privacy.android.app.psa

import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.view.View
import mega.privacy.android.app.R
import mega.privacy.android.app.databinding.PsaLayoutBinding
import mega.privacy.android.app.lollipop.WebViewActivityLollipop

/**
 * The view holder for normal PSA view, implementing the display logic of PSA.
 */
class PsaViewHolder(
    psaLayout: View,
    private val psaManager: PsaManager
) {
    private val binding = PsaLayoutBinding.bind(psaLayout)
    private var bound = false

    /**
     * Bind view for the PSA.
     *
     * @param psa the PSA to display
     */
    fun bind(psa: Psa) {
        bound = true
        binding.root.visibility = View.VISIBLE
        if (!TextUtils.isEmpty(psa.imageUrl)) {
            binding.image.visibility = View.VISIBLE
            binding.image.setImageURI(Uri.parse(psa.imageUrl))
        }

        binding.title.text = psa.title
        binding.text.text = psa.text

        if (!TextUtils.isEmpty(psa.positiveText) && !TextUtils.isEmpty(psa.positiveLink)) {
            binding.leftButton.text = psa.positiveText
            binding.leftButton.setOnClickListener {
                val context = binding.root.context
                val intent = Intent(context, WebViewActivityLollipop::class.java)
                intent.data = Uri.parse(psa.positiveLink)
                context.startActivity(intent)
                dismissPsa(psa.id)
            }

            binding.rightButton.visibility = View.VISIBLE
            binding.rightButton.setOnClickListener { dismissPsa(psa.id) }
        } else {
            binding.leftButton.setText(R.string.general_dismiss)
            binding.leftButton.setOnClickListener { dismissPsa(psa.id) }
        }
    }

    /**
     * Toggle visibility of the PSA view.
     *
     * @param shouldShow if the PSA view should be visible
     */
    fun toggleVisible(shouldShow: Boolean) {
        binding.root.visibility = if (shouldShow && bound) View.VISIBLE else View.GONE
    }

    /**
     * Hide PSA view and dismiss it in server.
     *
     * @param id the id of the PSA
     */
    private fun dismissPsa(id: Int) {
        psaManager.dismissPsa(id)
        binding.root.visibility = View.GONE
        bound = false
    }
}
