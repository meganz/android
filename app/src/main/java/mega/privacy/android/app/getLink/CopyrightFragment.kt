package mega.privacy.android.app.getLink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.databinding.FragmentCopyrightBinding
import mega.privacy.android.app.fragments.BaseFragment

class CopyrightFragment : BaseFragment() {

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
        binding.agreeButton.setOnClickListener {
            viewModel.updateShowCopyRight(false)
            requireActivity().onBackPressed()
        }

        binding.disagreeButton.setOnClickListener {
            viewModel.updateShowCopyRight(true)
            activity?.finish()
        }
    }
}