package mega.privacy.android.app.lollipop;

import android.os.AsyncTask;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.databinding.FragmentImportFilesBinding;
import mega.privacy.android.app.fragments.BaseFragment;
import mega.privacy.android.app.lollipop.adapters.ImportFilesAdapter;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ImportFilesFragment extends BaseFragment {

    public static final String THUMB_FOLDER = "ImportFilesThumb";

    private FragmentImportFilesBinding binding;

    private LinearLayoutManager mLayoutManager;
    public ImportFilesAdapter adapter;

    boolean areItemsVisible = false;

    private List<ShareInfo> filePreparedInfos;
    HashMap<String, String> nameFiles = new HashMap<>();

    public static ImportFilesFragment newInstance() {
        return new ImportFilesFragment();
    }

    public void changeActionBarElevation() {
        ((FileExplorerActivityLollipop) context).changeActionBarElevation(
                binding.scrollContainerImport.canScrollVertically(-1),
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
            nameFiles = ((FileExplorerActivityLollipop) context).getNameFiles();
            if (nameFiles == null || nameFiles.size() <= 0) {
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

        binding.fileListView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
        mLayoutManager = new LinearLayoutManager(context);
        binding.fileListView.setLayoutManager(mLayoutManager);
        binding.cloudDriveLayout.setOnClickListener(v ->
                ((FileExplorerActivityLollipop) context).chooseFragment(FileExplorerActivityLollipop.CLOUD_FRAGMENT));

        binding.chatLayout.setOnClickListener(v ->
                ((FileExplorerActivityLollipop) context).chooseFragment(FileExplorerActivityLollipop.CHAT_FRAGMENT));
        binding.chatLayout.setVisibility(VISIBLE);

        binding.showMoreLayout.setOnClickListener(v -> showMoreClick());

        if (filePreparedInfos != null) {
            binding.showMoreLayout.setVisibility(filePreparedInfos.size() <= 4 ? GONE : VISIBLE);
            binding.contentText.setText(getResources().getQuantityString(R.plurals.general_num_files, filePreparedInfos.size()));

            if (adapter == null) {
                adapter = new ImportFilesAdapter(context, this, filePreparedInfos, nameFiles);
                adapter.SetOnItemClickListener(this::itemClick);
            }

            adapter.setImportNameFiles(nameFiles);
            areItemsVisible = binding.showMoreLayout.getVisibility() != VISIBLE;
            adapter.setItemsVisibility(areItemsVisible);
            binding.fileListView.setAdapter(adapter);
        }

        super.onViewCreated(view, savedInstanceState);
    }

    private void showMoreClick() {
        areItemsVisible = !areItemsVisible;

        if (adapter != null) {
            adapter.setItemsVisibility(areItemsVisible);
        }

        if (areItemsVisible) {
            binding.showMoreText.setText(getString(R.string.general_show_less));
            binding.showMoreImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_expand));
        } else {
            binding.showMoreText.setText(getString(R.string.general_show_more));
            binding.showMoreImage.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_collapse_acc));
        }
    }

    public void itemClick(View view, int position) {
        if (view.getId() == R.id.edit_icon_layout && adapter != null) {
            ShareInfo info = (ShareInfo) adapter.getItem(position);

            if (info != null) {
                File file = new File(info.getFileAbsolutePath());

                if (file != null) {
                    ((FileExplorerActivityLollipop) context).showRenameDialog(file, nameFiles.get(info.getTitle()));
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        ((FileExplorerActivityLollipop) context).setNameFiles(nameFiles);
    }

    public void setNameFiles(HashMap<String, String> nameFiles) {
        this.nameFiles = nameFiles;
    }

    public HashMap<String, String> getNameFiles() {
        return nameFiles;
    }

    class GetNamesAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            for (int i = 0; i < filePreparedInfos.size(); i++) {
                nameFiles.put(filePreparedInfos.get(i).getTitle(), filePreparedInfos.get(i).getTitle());
            }
            return null;
        }
    }
}
