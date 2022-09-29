package mega.privacy.android.app.main;

import static mega.privacy.android.app.constants.StringsConstants.INVALID_CHARACTERS;
import static mega.privacy.android.app.utils.Constants.NODE_NAME_REGEX;
import static mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.databinding.FragmentImportFilesBinding;
import mega.privacy.android.app.main.adapters.ImportFilesAdapter;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.domain.entity.ShareTextInfo;

public class ImportFilesFragment extends Fragment implements ImportFilesAdapter.OnImportFilesAdapterFooterListener {

    public static final String THUMB_FOLDER = "ImportFilesThumb";

    private FragmentImportFilesBinding binding;

    private ImportFilesAdapter adapter;

    private FileExplorerActivityViewModel viewModel;

    public static ImportFilesFragment newInstance() {
        return new ImportFilesFragment();
    }

    public void changeActionBarElevation() {
        ((FileExplorerActivity) requireActivity()).changeActionBarElevation(
                binding.scrollContainerImport.canScrollVertically(SCROLLING_UP_DIRECTION),
                FileExplorerActivity.IMPORT_FRAGMENT);
    }

    @Override
    public void onResume() {
        super.onResume();
        changeActionBarElevation();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel = new ViewModelProvider((requireActivity())).get(FileExplorerActivityViewModel.class);
        binding = FragmentImportFilesBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupObservers();
        setupView();

        super.onViewCreated(view, savedInstanceState);
    }

    private void setupObservers() {
        viewModel.getFilesInfo().observe(getViewLifecycleOwner(), this::showFilesInfo);
        viewModel.getTextInfo().observe(getViewLifecycleOwner(), this::showImportingTextInfo);
    }

    /**
     * Shows the view when it is importing text.
     *
     * @param info ShareTextInfo containing all the required info to share the text.
     */
    private void showImportingTextInfo(ShareTextInfo info) {
        boolean setNames = true;

        if (adapter == null) {
            adapter = new ImportFilesAdapter(requireActivity(), info, getNameFiles());
            setNames = false;
        }

        String headerText;

        if (info.isUrl()) {
            headerText = StringResourcesUtils.getString(R.string.file_properties_shared_folder_public_link_name);
        } else {
            headerText = StringResourcesUtils.getQuantityString(R.plurals.general_num_files, 1);
        }

        setupAdapter(setNames, headerText);
    }

    /**
     * Shows the view when it is importing files.
     *
     * @param info List of ShareInfo containing all the required info to share the files.
     */
    private void showFilesInfo(List<ShareInfo> info) {
        boolean setNames = true;

        if (adapter == null) {
            adapter = new ImportFilesAdapter(requireActivity(), info, getNameFiles());
            setNames = false;
        }

        String headerText = StringResourcesUtils.getQuantityString(R.plurals.general_num_files, info.size());
        setupAdapter(setNames, headerText);
    }

    /**
     * Sets the adapter.
     *
     * @param setNames   True if should set the file names to the adapter, false otherwise.
     * @param headerText The text to show as the header of the content.
     */
    private void setupAdapter(boolean setNames, String headerText) {
        binding.contentText.setText(headerText);
        if (setNames) {
            adapter.setImportNameFiles(getNameFiles());
        }

        binding.fileListView.setAdapter(adapter);
        adapter.setFooterListener(this);
    }

    private void setupView() {
        binding.scrollContainerImport.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) ->
                changeActionBarElevation());

        binding.fileListView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    /**
     * Checks if all the files to import have a right name.
     * - If so, shows the fragment which the user chose to import.
     * - If not, shows an error warning.
     *
     * @param fragment The fragment chosen by the user.
     */
    private void confirmImport(int fragment) {
        if (adapter != null) {
            adapter.updateCurrentFocusPosition(binding.fileListView);
        }

        HashMap<String, String> nameFiles = getNameFiles();
        int emptyNames = 0;
        int wrongNames = 0;

        for (String name : nameFiles.values()) {
            if (name == null || name.trim().isEmpty()) {
                emptyNames++;
            } else if (NODE_NAME_REGEX.matcher(name).find()) {
                wrongNames++;
            }
        }

        if (wrongNames > 0 || emptyNames > 0) {
            String warning;

            if (emptyNames > 0 && wrongNames > 0) {
                warning = StringResourcesUtils.getString(R.string.general_incorrect_names);
            } else if (emptyNames > 0) {
                warning = StringResourcesUtils.getQuantityString(R.plurals.empty_names, emptyNames);
            } else {
                warning = StringResourcesUtils.getString(R.string.invalid_characters_defined, INVALID_CHARACTERS);
            }

            ((FileExplorerActivity) requireActivity()).showSnackbar(warning);
        } else {
            ((FileExplorerActivity) requireActivity()).chooseFragment(fragment);
        }
    }

    private HashMap<String, String> getNameFiles() {
        return viewModel.getFileNames().getValue();
    }

    @Override
    public void onClickCloudDriveButton() {
        confirmImport(FileExplorerActivity.CLOUD_FRAGMENT);
    }

    @Override
    public void onClickChatButton() {
        confirmImport(FileExplorerActivity.CHAT_FRAGMENT);
    }
}
