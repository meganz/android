package mega.privacy.android.app.getLink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.databinding.FragmentCopyrightBinding
import mega.privacy.android.app.interfaces.Scrollable
import mega.privacy.android.app.utils.Constants

/**
 * Fragment of [GetLinkActivity] which informs the user about Copyright.
 */
class CopyrightFragment : Fragment(), Scrollable {

    private val viewModel: GetLinkViewModel by activityViewModels()

    private lateinit var binding: FragmentCopyrightBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentCopyrightBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setupView() {
        binding.scrollViewCopyright.setOnScrollChangeListener { _, _, _, _, _ ->
            checkScroll()
        }

        checkScroll()

        binding.agreeButton.setOnClickListener {
            viewModel.updateShowCopyRight(false)
            requireActivity().onBackPressed()
        }

        binding.disagreeButton.setOnClickListener {
            viewModel.updateShowCopyRight(true)
            requireActivity().finish()
        }
    }

    override fun checkScroll() {
        if (!this::binding.isInitialized) {
            return
        }

        val withElevation = binding.scrollViewCopyright
            .canScrollVertically(Constants.SCROLLING_UP_DIRECTION)

        viewModel.setElevation(withElevation)
    }
}