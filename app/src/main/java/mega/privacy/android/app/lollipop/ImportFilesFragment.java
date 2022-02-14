package mega.privacy.android.app.lollipop;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.databinding.FragmentImportFilesBinding;
import mega.privacy.android.app.fragments.BaseFragment;
import mega.privacy.android.app.lollipop.adapters.ImportFilesAdapter;
import mega.privacy.android.app.utils.StringResourcesUtils;

import static android.text.TextUtils.isEmpty;
import static mega.privacy.android.app.utils.Constants.NODE_NAME_REGEX;
import static mega.privacy.android.app.utils.Constants.SCROLLING_UP_DIRECTION;

public class ImportFilesFragment extends BaseFragment implements ImportFilesAdapter.OnImportFilesAdapterFooterListener {

    public static final String THUMB_FOLDER = "ImportFilesThumb";

    private FragmentImportFilesBinding binding;

    private ImportFilesAdapter adapter;

    private List<ShareInfo> filePreparedInfos;

    public static ImportFilesFragment newInstance() {
        return new ImportFilesFragment();
    }

    public void changeActionBarElevation() {
        ((FileExplorerActivityLollipop) context).changeActionBarElevation(
                binding.scrollContainerImport.canScrollVertically(SCROLLING_UP_DIRECTION),
                FileExplorerActivityLollipop.IMPORT_FRAGMENT);
    }

    @Override
    public void onResume() {
        super.onResume();
        changeActionBarElevation();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        filePreparedInfos = ((FileExplorerActivityLollipop) context).getFilePreparedInfos();
        if (filePreparedInfos != null) {
            HashMap<String, String> nameFiles = ((FileExplorerActivityLollipop) context).getNameFiles();
            if (nameFiles == null || nameFiles.isEmpty()) {
                new GetNamesAsyncTask().execute();
            }
        }

        binding = FragmentImportFilesBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        new ListenScrollChangesHelper().addViewToListen(binding.scrollContainerImport,
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> changeActionBarElevation());

        binding.fileListView.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (filePreparedInfos != null) {
            binding.contentText.setText(getResources().getQuantityString(R.plurals.general_num_files, filePreparedInfos.size()));

            if (adapter == null) {
                adapter = new ImportFilesAdapter(context, this, filePreparedInfos, getNameFiles());
            }

            adapter.setImportNameFiles(getNameFiles());
            binding.fileListView.setAdapter(adapter);
            adapter.setFooterListener(this);
        }

        super.onViewCreated(view, savedInstanceState);
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
                warning = StringResourcesUtils.getString(R.string.invalid_characters_defined);
            }

            ((FileExplorerActivityLollipop) context).showSnackbar(warning);
        } else {
            ((FileExplorerActivityLollipop) context).chooseFragment(fragment);
        }
    }

    private HashMap<String, String> getNameFiles() {
        return ((FileExplorerActivityLollipop) context).getNameFiles();
    }

    @Override
    public void onClickCloudDriveButton() {
        confirmImport(FileExplorerActivityLollipop.CLOUD_FRAGMENT);
    }

    @Override
    public void onClickChatButton() {
        confirmImport(FileExplorerActivityLollipop.CHAT_FRAGMENT);
    }

    class GetNamesAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            HashMap<String, String> nameFiles = new HashMap<>();

            for (int i = 0; i < filePreparedInfos.size(); i++) {
                String name = filePreparedInfos.get(i).getTitle();
                if (isEmpty(name)) {
                    name = filePreparedInfos.get(i).getOriginalFileName();
                }
                nameFiles.put(name, name);
            }

            ((FileExplorerActivityLollipop) context).setNameFiles(nameFiles);
            return null;
        }
    }
}
