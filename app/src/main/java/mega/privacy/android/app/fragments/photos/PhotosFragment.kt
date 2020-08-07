package mega.privacy.android.app.fragments.photos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.databinding.FragmentPhotosBinding
import javax.inject.Inject

@AndroidEntryPoint
class PhotosFragment : Fragment() {

    private val viewModel by viewModels<PhotosViewModel>()
    private lateinit var binding: FragmentPhotosBinding

    private var isItemsInited = false

    @Inject
    lateinit var listAdapter: PhotosGridAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPhotosBinding.inflate(inflater, container, false).apply {
            viewmodel = viewModel
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = viewLifecycleOwner
        setupListAdapter()
//        setupNavigation()

//        if (!isItemsInited) {
//            isItemsInited = true
            viewModel.loadPhotos(PhotoQuery(searchDate = LongArray(0)))
//        }
    }

    private fun setupListAdapter() {
//        val adapter = PhotosGridAdapter(viewModel)
        binding.photoList.adapter = listAdapter
    }

//    private fun setupNavigation() {
//
//    }
}