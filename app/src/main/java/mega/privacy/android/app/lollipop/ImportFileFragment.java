package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.ImportFilesAdapter;
import mega.privacy.android.app.utils.Util;

public class ImportFileFragment extends Fragment implements View.OnClickListener {

    public static final String THUMB_FOLDER = "ImportFilesThumb";

    Context context;

    ImportFilesAdapter adapter;

    TextView contentText;
    LinearLayoutManager mLayoutManager;
    RecyclerView recyclerView;
    RelativeLayout cloudDriveButton;
    RelativeLayout incomingButton;
    RelativeLayout chatButton;

    private List<ShareInfo> filePreparedInfos;
    HashMap<String, String> nameFiles = new HashMap<>();

    public static ImportFileFragment newInstance() {
        log("newInstance");
        ImportFileFragment fragment = new ImportFileFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        filePreparedInfos = ((FileExplorerActivityLollipop) context).getFilePreparedInfos();
        if (filePreparedInfos != null) {
            new GetNamesAsyncTask().execute();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        View v = inflater.inflate(R.layout.fragment_importfile, container, false);

        contentText = (TextView) v.findViewById(R.id.content_text);
        recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, metrics));
        mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);
        cloudDriveButton = (RelativeLayout) v.findViewById(R.id.cloud_drive_layout);
        cloudDriveButton.setOnClickListener(this);
        incomingButton = (RelativeLayout) v.findViewById(R.id.incoming_layout);
        incomingButton.setOnClickListener(this);
        chatButton = (RelativeLayout) v.findViewById(R.id.chat_layout);
        chatButton.setOnClickListener(this);

        if (filePreparedInfos != null) {
            if (filePreparedInfos.size() == 1) {
                contentText.setText("File");
            }
            else if (filePreparedInfos.size() > 1) {
                contentText.setText("Files");
            }
            if (adapter == null) {
                adapter = new ImportFilesAdapter(context, this, filePreparedInfos, nameFiles);

                adapter.SetOnItemClickListener(new ImportFilesAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        log("item click listener trigger!!");
                        itemClick(view, position);
                    }
                });
            }
            adapter.setImportNameFiles(nameFiles);
            recyclerView.setAdapter(adapter);
        }

        return v;
    }

    public void itemClick(View view, int position) {
        log("itemClick");
        ((MegaApplication) ((Activity) context).getApplication()).sendSignalPresenceActivity();

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cloud_drive_layout: {
                break;
            }
            case R.id.incoming_layout: {
                break;
            }
            case R.id.chat_layout: {
                break;
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    public HashMap<String, String> getNames () {
        return nameFiles;
    }

    class GetNamesAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            for (int i=0; i<filePreparedInfos.size(); i++) {
                nameFiles.put(filePreparedInfos.get(i).getTitle(), filePreparedInfos.get(i).getTitle());
            }
            return null;
        }
    }

    private static void log(String log) {
        Util.log("ImportFileFragment", log);
    }
}
