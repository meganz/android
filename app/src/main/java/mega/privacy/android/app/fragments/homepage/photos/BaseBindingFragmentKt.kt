package mega.privacy.android.app.fragments.homepage.photos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.MaterialToolbar
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.BaseFragment

abstract class BaseBindingFragmentKt<VM : ViewModel, VB : ViewDataBinding> : BaseFragment() {

    protected abstract val viewModel: VM

    private var _binding: VB? = null

    protected val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setDefaultToolBar()
        init()
        subscribeObservers()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = initBinding(inflater, container, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    abstract fun subscribeObservers()

    abstract fun bindToolbar(): MaterialToolbar?

    abstract fun bindTitle(): Int?

    protected fun bindToolbarNavigationIcon(): Int{
        return R.drawable.ic_arrow_back_black
    }

    private fun getToolbarNavigationIcon(resDrawable: Int?): Int {
        return resDrawable ?: R.drawable.ic_arrow_back_black
    }

    private fun getToolbarNavigationTitle(resTitle: Int?): String {
        return if (resTitle == null) {
            ""
        } else {
            getString(resTitle)
        }
    }

    private fun setDefaultToolBar() {
        val toolbar = bindToolbar()
        val resTitle = bindTitle()
        val resNavigationIcon = bindToolbarNavigationIcon()
        bindToolbar()
        toolbar?.let {
            it.title = getToolbarNavigationTitle(resTitle)
            it.setNavigationIcon(getToolbarNavigationIcon(resNavigationIcon))
            it.setNavigationOnClickListener {
                goBack()
            }
        }
    }

    /**
     *
     */
    abstract fun initBinding(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): VB

    abstract fun init()

    protected fun goBack() {
        findNavController().popBackStack()
    }
}