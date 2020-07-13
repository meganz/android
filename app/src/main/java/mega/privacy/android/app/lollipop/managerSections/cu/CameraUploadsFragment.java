package mega.privacy.android.app.lollipop.managerSections.cu;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.databinding.FragmentCameraUploadsBinding;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.CameraUploadsAdapter;

import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR;
import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR_GRID;

public class CameraUploadsFragment extends Fragment {
  public static final int TYPE_CAMERA = 0;
  public static final int TYPE_MEDIA = 1;

  private static final String ARG_TYPE = "type";

  private static final int SPAN_LARGE_GRID = 3;
  private static final int SPAN_SMALL_GRID = 7;
  private static final int MARGIN_LARGE_GRID = 6;
  private static final int MARGIN_SMALL_GRID = 3;

  private FragmentCameraUploadsBinding binding;
  private CameraUploadsAdapter adapter;

  private CuViewModel viewModel;

  public static CameraUploadsFragment newInstance(int type) {
    CameraUploadsFragment fragment = new CameraUploadsFragment();

    Bundle args = new Bundle();
    args.putInt(ARG_TYPE, type);
    fragment.setArguments(args);

    return fragment;
  }

  public int getItemCount() {
    return adapter.getItemCount();
  }

  public void setOrderBy(int orderBy) {
    viewModel.setOrderBy(orderBy);
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    binding = FragmentCameraUploadsBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    CuViewModelFactory viewModelFactory =
        new CuViewModelFactory(((MegaApplication) requireActivity().getApplication()).getMegaApi(),
            DatabaseHandler.getDbHandler(requireContext()));
    viewModel = new ViewModelProvider(requireActivity(), viewModelFactory).get(CuViewModel.class);

    boolean smallGrid = ((ManagerActivityLollipop) requireActivity()).isSmallGridCameraUploads;
    int spanCount = smallGrid ? SPAN_SMALL_GRID : SPAN_LARGE_GRID;

    binding.cuList.setHasFixedSize(true);
    GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), spanCount);
    binding.cuList.setLayoutManager(layoutManager);

    DisplayMetrics displayMetrics = new DisplayMetrics();
    requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    int gridMargin = smallGrid ? MARGIN_SMALL_GRID : MARGIN_LARGE_GRID;
    int gridWidth = displayMetrics.widthPixels / spanCount - gridMargin * 2;

    adapter = new CameraUploadsAdapter(spanCount, smallGrid, gridWidth, gridMargin);
    layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
      @Override public int getSpanSize(int position) {
        return adapter.getSpanSize(position);
      }
    });

    binding.cuList.setAdapter(adapter);
    binding.scroller.setRecyclerView(binding.cuList);

    viewModel.cuNodes().observe(getViewLifecycleOwner(), nodes -> {
      boolean showScroller =
          nodes.size() >= (smallGrid ? MIN_ITEMS_SCROLLBAR_GRID : MIN_ITEMS_SCROLLBAR);
      binding.scroller.setVisibility(showScroller ? View.VISIBLE : View.GONE);
      adapter.setNodes(nodes);
    });
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }
}
