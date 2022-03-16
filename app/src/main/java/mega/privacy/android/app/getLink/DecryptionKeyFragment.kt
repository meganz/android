package mega.privacy.android.app.getLink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.databinding.FragmentDecryptionKeyBinding
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.utils.Constants

/**
 * Fragment of [GetLinkActivity] which shows info about the decryption key of a link.
 */
class DecryptionKeyFragment : Fragment(), Scrollable {

    private val viewModel: GetLinkViewModel by activityViewModels()

    private lateinit var binding: FragmentDecryptionKeyBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDecryptionKeyBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setupView() {
        binding.scrollViewDecryption.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }

        checkScroll()
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized) {
            return
        }

        val withElevation = binding.scrollViewDecryption
            .canScrollVertically(Constants.SCROLLING_UP_DIRECTION)

        viewModel.setElevation(withElevation)
    }
}