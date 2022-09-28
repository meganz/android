package mega.privacy.android.app.fragments.recent;

import static mega.privacy.android.app.components.dragger.DragToExitSupport.observeDragSupportEvents;
import static mega.privacy.android.app.components.dragger.DragToExitSupport.putThumbnailLocation;
import static mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_HIDE_RECENT_ACTIVITY;
import static mega.privacy.android.app.utils.Constants.EVENT_NODES_CHANGE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_HANDLE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST;
import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR;
import static mega.privacy.android.app.utils.Constants.NODE_HANDLES;
import static mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.VIEWER_FROM_RECETS;
import static mega.privacy.android.app.utils.ContactUtil.getContactDB;
import static mega.privacy.android.app.utils.ContactUtil.getContactNameDB;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.FileUtil.isAudioOrVideo;
import static mega.privacy.android.app.utils.FileUtil.isInternalIntent;
import static mega.privacy.android.app.utils.FileUtil.isLocalFile;
import static mega.privacy.android.app.utils.FileUtil.isOpusFile;
import static mega.privacy.android.app.utils.FileUtil.setLocalIntentParams;
import static mega.privacy.android.app.utils.FileUtil.setStreamingIntentParams;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageTextFileIntent;
import static mega.privacy.android.app.utils.MegaNodeUtil.manageURLNode;
import static mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped;
import static mega.privacy.android.app.utils.SharedPreferenceConstants.HIDE_RECENT_ACTIVITY;
import static mega.privacy.android.app.utils.SharedPreferenceConstants.USER_INTERFACE_PREFERENCES;
import static mega.privacy.android.app.utils.TextUtil.formatEmptyScreenText;
import static mega.privacy.android.app.utils.Util.getMediaIntent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;

import com.brandongogetap.stickyheaders.StickyLayoutManager;
import com.brandongogetap.stickyheaders.exposed.StickyHeader;
import com.brandongogetap.stickyheaders.exposed.StickyHeaderHandler;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.RecentsItem;
import mega.privacy.android.app.components.HeaderItemDecoration;
import mega.privacy.android.app.components.TopSnappedStickyLayoutManager;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.PdfViewerActivity;
import mega.privacy.android.app.main.adapters.RecentsAdapter;
import mega.privacy.android.app.utils.StringResourcesUtils;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRecentActionBucket;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;


public class RecentsFragment extends Fragment implements StickyHeaderHandler {

    private MegaApiAndroid megaApi;

    private ArrayList<MegaContactAdapter> visibleContacts = new ArrayList<>();
    private ArrayList<MegaRecentActionBucket> buckets;
    private MegaRecentActionBucket bucketSelected;
    private ArrayList<RecentsItem> recentsItems;
    private RecentsAdapter adapter;

    private ScrollView emptyLayout;
    private TextView emptyText;
    private Button showActivityButton;
    private Spanned emptySpanned;
    private Spanned activityHiddenSpanned;

    private StickyLayoutManager stickyLayoutManager;
    private RecyclerView listView;
    private FastScroller fastScroller;

    private SelectedBucketViewModel selectedBucketModel;

    private final Observer<Boolean> nodeChangeObserver = forceUpdate -> {
        if (forceUpdate) {
            buckets = megaApi.getRecentActions();
            reloadItems(buckets);
            refreshRecentsActions();
            setVisibleContacts();
            setRecentsView();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) requireContext()).getApplication()).getMegaApi();
        }
        if (megaApi.getRootNode() == null) return null;

        recentsItems = new ArrayList<>();

        buckets = megaApi.getRecentActions();

        View v = inflater.inflate(R.layout.fragment_recents, container, false);

        emptyLayout = v.findViewById(R.id.empty_state_recents);

        emptyText = v.findViewById(R.id.empty_text_recents);

        showActivityButton = v.findViewById(R.id.show_activity_button);
        showActivityButton.setOnClickListener(v1 -> showRecentActivity());

        emptySpanned = formatEmptyScreenText(requireContext(),
                StringResourcesUtils.getString(R.string.context_empty_recents));

        activityHiddenSpanned = formatEmptyScreenText(requireContext(),
                StringResourcesUtils.getString(R.string.recents_activity_hidden));


        listView = v.findViewById(R.id.list_view_recents);
        fastScroller = v.findViewById(R.id.fastscroll);

        stickyLayoutManager = new TopSnappedStickyLayoutManager(requireContext(), this);
        listView.setLayoutManager(stickyLayoutManager);
        listView.setClipToPadding(false);
        listView.setItemAnimator(new DefaultItemAnimator());

        fillRecentItems(buckets);
        setRecentsView();
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        LiveEventBus.get(EVENT_NODES_CHANGE, Boolean.class).observeForever(nodeChangeObserver);
        LiveEventBus.get(EVENT_UPDATE_HIDE_RECENT_ACTIVITY, Boolean.class)
                .observe(getViewLifecycleOwner(), this::setRecentsView);

        selectedBucketModel = new ViewModelProvider(requireActivity()).get(SelectedBucketViewModel.class);

        observeDragSupportEvents(getViewLifecycleOwner(), listView, VIEWER_FROM_RECETS);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        LiveEventBus.get(EVENT_NODES_CHANGE, Boolean.class).removeObserver(nodeChangeObserver);
    }

    public SelectedBucketViewModel getSelectedBucketModel() {
        return selectedBucketModel;
    }

    public void fillRecentItems(ArrayList<MegaRecentActionBucket> buckets) {
        this.buckets = buckets;
        reloadItems(buckets);

        adapter = new RecentsAdapter(requireContext(), this, recentsItems);
        listView.setAdapter(adapter);
        listView.addItemDecoration(new HeaderItemDecoration(requireContext()));
        setVisibleContacts();
    }

    private void reloadItems(ArrayList<MegaRecentActionBucket> buckets) {
        recentsItems.clear();
        String previousDate = "";
        String currentDate;

        for (int i = 0; i < buckets.size(); i++) {
            RecentsItem item = new RecentsItem(requireContext(), buckets.get(i));
            if (i == 0) {
                previousDate = currentDate = item.getDate();
                recentsItems.add(new RecentsItemHeader(currentDate));
            } else {
                currentDate = item.getDate();
                if (!currentDate.equals(previousDate)) {
                    recentsItems.add(new RecentsItemHeader(currentDate));
                    previousDate = currentDate;
                }
            }
            recentsItems.add(item);
        }
    }

    public void refreshRecentsActions() {
        if (adapter != null) {
            adapter.setItems(recentsItems);
        }
        setRecentsView();
    }

    private void setRecentsView() {
        boolean hideRecentActivity = requireContext()
                .getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
                .getBoolean(HIDE_RECENT_ACTIVITY, false);

        setRecentsView(hideRecentActivity);
        ((ManagerActivity) requireContext()).setToolbarTitle();
    }

    /**
     * Sets the recent view. Hide it if the setting to hide it is enabled, and shows it if the
     * setting is disabled.
     *
     * @param hideRecentActivity True if the setting to hide the recent activity is enabled,
     *                           false otherwise.
     */
    private void setRecentsView(boolean hideRecentActivity) {
        if (hideRecentActivity) {
            hideRecentActivity();
        } else {
            showActivity();
        }
    }

    /**
     * Shows the recent activity.
     */
    private void showActivity() {
        if (buckets == null || buckets.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            fastScroller.setVisibility(View.GONE);
            showActivityButton.setVisibility(View.GONE);
            emptyText.setText(emptySpanned);
        } else {
            emptyLayout.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
            fastScroller.setRecyclerView(listView);
            if (buckets.size() < MIN_ITEMS_SCROLLBAR) {
                fastScroller.setVisibility(View.GONE);
            } else {
                fastScroller.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Hides the recent activity.
     */
    private void hideRecentActivity() {
        emptyLayout.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        fastScroller.setVisibility(View.GONE);
        showActivityButton.setVisibility(View.VISIBLE);
        emptyText.setText(activityHiddenSpanned);
    }

    /**
     * Disables the setting to hide recent activity and updates the UI by showing it.
     */
    private void showRecentActivity() {
        LiveEventBus.get(EVENT_UPDATE_HIDE_RECENT_ACTIVITY, Boolean.class).post(false);
        requireContext().getSharedPreferences(USER_INTERFACE_PREFERENCES, Context.MODE_PRIVATE)
                .edit().putBoolean(HIDE_RECENT_ACTIVITY, false).apply();
    }

    public String findUserName(String mail) {
        for (MegaContactAdapter contact : visibleContacts) {
            if (contact.getMegaUser().getEmail().equals(mail)) {
                return contact.getFullName();
            }
        }
        return "";
    }

    public void setVisibleContacts() {
        visibleContacts.clear();
        ArrayList<MegaUser> contacts = megaApi.getContacts();
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                long contactHandle = contacts.get(i).getHandle();
                MegaContactDB contactDB = getContactDB(contactHandle);
                if (contactDB == null) break;
                String fullName = getContactNameDB(contactDB);
                if (fullName == null) {
                    fullName = contacts.get(i).getEmail();
                }
                MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contacts.get(i), fullName);
                visibleContacts.add(megaContactAdapter);
            }
        }
    }

    private long[] getBucketNodeHandles(boolean areImages) {
        if (bucketSelected == null || bucketSelected.getNodes() == null || bucketSelected.getNodes().size() == 0)
            return null;

        MegaNode node;
        MegaNodeList list = bucketSelected.getNodes();
        ArrayList<Long> nodeHandlesList = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            node = list.get(i);
            if (node == null) continue;

//          Group handles by type of file
            if (areImages && MimeTypeList.typeForName(node.getName()).isImage()) {
//              only images on the one hand
                nodeHandlesList.add(node.getHandle());
            } else if (!areImages && isAudioOrVideo(node) && isInternalIntent(node)) {
//              only videos or audios on the other
                nodeHandlesList.add(node.getHandle());
            }
        }

        long[] nodeHandles = new long[nodeHandlesList.size()];
        for (int i = 0; i < nodeHandlesList.size(); i++) {
            nodeHandles[i] = nodeHandlesList.get(i);
        }

        return nodeHandles;
    }

    public void openFile(int index, MegaNode node, boolean isMedia) {
        Intent intent;

        if (MimeTypeList.typeForName(node.getName()).isImage()) {
            long[] bucket = getBucketNodeHandles(true);
            if (isMedia && bucket != null && bucket.length > 0) {
                intent = ImageViewerActivity.getIntentForChildren(
                        requireContext(),
                        bucket
                );
            } else {
                intent = ImageViewerActivity.getIntentForSingleNode(
                        requireContext(),
                        node.getHandle(),
                        false
                );
            }

            putThumbnailLocation(intent, listView, index, VIEWER_FROM_RECETS, adapter);
            startActivity(intent);
            ((ManagerActivity) requireContext()).overridePendingTransition(0, 0);
            return;
        }

        String localPath = getLocalFile(node);
        boolean paramsSetSuccessfully;

        if (isAudioOrVideo(node)) {
            if (isInternalIntent(node)) {
                intent = getMediaIntent(requireContext(), node.getName());
            } else {
                intent = new Intent(Intent.ACTION_VIEW);
            }

            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, RECENTS_ADAPTER);
            intent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.getName());
            if (isMedia) {
                intent.putExtra(NODE_HANDLES, getBucketNodeHandles(false));
                intent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, true);
            } else {
                intent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false);
            }

            if (isLocalFile(node, megaApi, localPath)) {
                paramsSetSuccessfully = setLocalIntentParams(requireContext(), node, intent, localPath,
                        false, (ManagerActivity) requireActivity());
            } else {
                paramsSetSuccessfully = setStreamingIntentParams(requireContext(), node, megaApi, intent,
                        (ManagerActivity) requireActivity());
            }

            if (paramsSetSuccessfully && isOpusFile(node)) {
                intent.setDataAndType(intent.getData(), "audio/*");
            }

            launchIntent(intent, paramsSetSuccessfully, node, index);
        } else if (MimeTypeList.typeForName(node.getName()).isURL()) {
            manageURLNode(requireContext(), megaApi, node);
        } else if (MimeTypeList.typeForName(node.getName()).isPdf()) {
            intent = new Intent(requireContext(), PdfViewerActivity.class);
            intent.putExtra(INTENT_EXTRA_KEY_INSIDE, true);
            intent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, RECENTS_ADAPTER);

            if (isLocalFile(node, megaApi, localPath)) {
                paramsSetSuccessfully = setLocalIntentParams(requireContext(), node, intent, localPath,
                        false, (ManagerActivity) requireActivity());
            } else {
                paramsSetSuccessfully = setStreamingIntentParams(requireContext(), node, megaApi, intent,
                        (ManagerActivity) requireActivity());
            }

            launchIntent(intent, paramsSetSuccessfully, node, index);
        } else if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())) {
            manageTextFileIntent(requireContext(), node, RECENTS_ADAPTER);
        } else {
            Timber.d("itemClick:isFile:otherOption");
            onNodeTapped(requireContext(), node, ((ManagerActivity) requireActivity())::saveNodeByTap, (ManagerActivity) requireActivity(), (ManagerActivity) requireActivity());
        }
    }

    /**
     * Launch corresponding intent to open the file based on its type.
     *
     * @param intent                Intent to launch activity.
     * @param paramsSetSuccessfully true, if the param is set for the intent successfully; false, otherwise.
     * @param node                  The node to open.
     * @param position              Thumbnail's position in the list.
     */
    private void launchIntent(Intent intent, boolean paramsSetSuccessfully, MegaNode node, int position) {
        if (intent != null && !isIntentAvailable(requireContext(), intent)) {
            paramsSetSuccessfully = false;
            ((ManagerActivity) requireContext()).showSnackbar(SNACKBAR_TYPE, getString(R.string.intent_not_available), -1);
        }

        if (intent != null && paramsSetSuccessfully) {
            intent.putExtra(INTENT_EXTRA_KEY_HANDLE, node.getHandle());
            putThumbnailLocation(intent, listView, position, VIEWER_FROM_RECETS, adapter);

            requireContext().startActivity(intent);
            ((ManagerActivity) requireContext()).overridePendingTransition(0, 0);
        }
    }

    public void setBucketSelected(MegaRecentActionBucket bucketSelected) {
        this.bucketSelected = bucketSelected;
    }

    @Override
    public List<RecentsItem> getAdapterData() {
        return recentsItems;
    }

    public class RecentsItemHeader extends RecentsItem implements StickyHeader {

        public RecentsItemHeader(String date) {
            super(date);
        }
    }
}
