package mega.privacy.android.app.gallery.fragment.newImages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import mega.privacy.android.app.fragments.BaseFragment

abstract class BaseBindingFragmentKt<VM : ViewModel, VB : ViewBinding> : BaseFragment() {

    protected abstract val viewModel: VM

    private var _binding: VB? = null

    protected val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        subscribeObservers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = initBinding(inflater,container,savedInstanceState)

        return binding.root
    }

    abstract fun subscribeObservers()

    /**
     *
     */
    abstract fun initBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ):VB

    abstract fun init()

    protected fun goBack(){
        findNavController().popBackStack()
    }
}