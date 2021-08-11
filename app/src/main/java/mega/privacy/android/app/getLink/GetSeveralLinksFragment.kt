package mega.privacy.android.app.getLink

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import mega.privacy.android.app.databinding.FragmentGetSeveralLinksBinding

class GetSeveralLinksFragment: Fragment() {

    private val viewModel: GetSeveralLinksViewModel by activityViewModels()

    private lateinit var binding: FragmentGetSeveralLinksBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGetSeveralLinksBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupView()
        setupObservers()
        super.onViewCreated(view, savedInstanceState)
    }

    private fun setupView() {

    }

    private fun setupObservers() {

    }
}