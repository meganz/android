package mega.privacy.android.app.lollipop;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.components.ListenScrollChangesHelper;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.adapters.ImportFilesAdapter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ImportFilesFragment extends Fragment implements View.OnClickListener {

    public static final String THUMB_FOLDER = "ImportFilesThumb";

    MegaApiAndroid megaApi;

    Context context;

    NestedScrollView scrollView;
    ImportFilesAdapter adapter;

    TextView contentText;
    LinearLayoutManager mLayoutManager;
    RecyclerView recyclerView;
    RelativeLayout cloudDriveButton;
    RelativeLayout incomingButton;
    RelativeLayout chatButton;
    RelativeLayout showMoreLayout;
    TextView showMoreText;
    ImageView showMoreIcon;

    boolean itemsVisibles = false;

    private List<ShareInfo> filePreparedInfos;
    HashMap<String, String> nameFiles = new HashMap<>();

    public static ImportFilesFragment newInstance() {
        logDebug("newInstance");
        ImportFilesFragment fragment = new ImportFilesFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        filePreparedInfos = ((FileExplorerActivityLollipop) context).getFilePreparedInfos();
    }

    public void changeActionBarElevation () {
        if (scrollView != null) {
            if (scrollView.canScrollVertically(-1)) {
                ((FileExplorerActivityLollipop) context).changeActionBarElevation(true);
            }
            else {
                ((FileExplorerActivityLollipop) context).changeActionBarElevation(false);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        changeActionBarElevation();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);

        if (filePreparedInfos != null) {
            nameFiles = ((FileExplorerActivityLollipop) context).getNameFiles();
            if (nameFiles == null || nameFiles.size() <= 0) {
                new GetNamesAsyncTask().execute();
            }
        }

        View v = inflater.inflate(R.layout.fragment_importfile, container, false);

        scrollView = (NestedScrollView) v.findViewById(R.id.scroll_container_import);
        new ListenScrollChangesHelper().addViewToListen(scrollView, new ListenScrollChangesHelper.OnScrollChangeListenerCompat() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                changeActionBarElevation();
            }
        });

        contentText = (TextView) v.findViewById(R.id.content_text);
        recyclerView = (RecyclerView) v.findViewById(R.id.file_list_view);
        recyclerView.addItemDecoration(new SimpleDividerItemDecoration(context, metrics));
        mLayoutManager = new LinearLayoutManager(context);
        recyclerView.setLayoutManager(mLayoutManager);
        cloudDriveButton = (RelativeLayout) v.findViewById(R.id.cloud_drive_layout);
        cloudDriveButton.setOnClickListener(this);
        incomingButton = (RelativeLayout) v.findViewById(R.id.incoming_layout);
        incomingButton.setOnClickListener(this);
        ArrayList<MegaNode> inShares = megaApi.getInShares();
        if (inShares == null || inShares.size() <= 0) {
            incomingButton.setVisibility(View.GONE);
        }
        else {
            incomingButton.setVisibility(View.VISIBLE);
        }

        chatButton = (RelativeLayout) v.findViewById(R.id.chat_layout);
        chatButton.setOnClickListener(this);
        chatButton.setVisibility(View.VISIBLE);

        showMoreLayout = (RelativeLayout) v.findViewById(R.id.show_more_layout);
        showMoreLayout.setOnClickListener(this);
        showMoreText  = (TextView) v.findViewById(R.id.show_more_text);
        showMoreIcon = (ImageView) v.findViewById(R.id.show_more_image);

        if (filePreparedInfos != null) {

            if (filePreparedInfos.size() <= 4) {
                showMoreLayout.setVisibility(View.GONE);
            }
            else {
                showMoreLayout.setVisibility(View.VISIBLE);
            }

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
                        logDebug("Item click listener trigger!!");
                        itemClick(view, position);
                    }
                });
            }
            adapter.setImportNameFiles(nameFiles);
            if (showMoreLayout.getVisibility() == View.VISIBLE) {
                itemsVisibles = false;
                adapter.setItemsVisibility(itemsVisibles);
            }
            else {
                itemsVisibles = true;
                adapter.setItemsVisibility(itemsVisibles);
            }
            recyclerView.setAdapter(adapter);
        }

        return v;
    }

    public void itemClick(View view, int position) {
        logDebug("Position: " + position);

        if (view.getId() == R.id.edit_icon_layout) {
            if (adapter != null) {
                ShareInfo info = (ShareInfo) adapter.getItem(position);
                if (info != null) {
                    File file =  new File(info.getFileAbsolutePath());
                    if (file != null) {
                        ((FileExplorerActivityLollipop) context).showRenameDialog(file, nameFiles.get(info.getTitle()));
                    }
                }
            }
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        ((FileExplorerActivityLollipop) context).setNameFiles(nameFiles);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cloud_drive_layout: {
                ((FileExplorerActivityLollipop) context).chooseFragment(FileExplorerActivityLollipop.CLOUD_FRAGMENT);
                break;
            }
            case R.id.incoming_layout: {
                ((FileExplorerActivityLollipop) context).chooseFragment(FileExplorerActivityLollipop.INCOMING_FRAGMENT);
                break;
            }
            case R.id.chat_layout: {
                ((FileExplorerActivityLollipop) context).chooseFragment(FileExplorerActivityLollipop.CHAT_FRAGMENT);
                break;
            }
            case R.id.show_more_layout: {
                if (!itemsVisibles) {
                    itemsVisibles = true;
                    if (adapter != null) {
                        adapter.setItemsVisibility(itemsVisibles);
                    }
                    showMoreText.setText(getString(R.string.general_show_less));
                    showMoreIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_expand));
                }
                else {
                    itemsVisibles = false;
                    if (adapter != null) {
                        adapter.setItemsVisibility(itemsVisibles);
                    }
                    showMoreText.setText(getString(R.string.general_show_more));
                    showMoreIcon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_collapse_acc));
                }
                break;
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    public void setNameFiles (HashMap<String, String> nameFiles) {
        this.nameFiles = nameFiles;
    }

    public HashMap<String, String> getNameFiles() {
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
}
