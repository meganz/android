package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.brandongogetap.stickyheaders.StickyLayoutManager;
import com.brandongogetap.stickyheaders.exposed.StickyHeader;
import com.brandongogetap.stickyheaders.exposed.StickyHeaderHandler;

import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.BucketSaved;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.RecentsItem;
import mega.privacy.android.app.components.HeaderItemDecoration;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.TopSnappedStickyLayoutManager;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MultipleBucketAdapter;
import mega.privacy.android.app.lollipop.adapters.RecentsAdapter;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.FileUtils;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.TimeUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRecentActionBucket;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.Constants.RECENTS_ADAPTER;
import static mega.privacy.android.app.utils.FileUtils.*;

public class RecentsFragment extends Fragment implements StickyHeaderHandler {

    private RecentsFragment recentsFragment;
    private Context context;
    private DisplayMetrics outMetrics;

    private DatabaseHandler dbH;
    private MegaApiAndroid megaApi;

    private ArrayList<MegaContactAdapter> visibleContacts = new ArrayList<>();
    private ArrayList<MegaRecentActionBucket> buckets;
    private MegaRecentActionBucket bucketSelected;
    private ArrayList<RecentsItem> recentsItems = new ArrayList<>();
    private RecentsAdapter adapter;

    private RelativeLayout emptyLayout;
    private ImageView emptyImage;
    private TextView emptyText;
    private StickyLayoutManager stickyLayoutManager;
    private LinearLayout listLayout;
    private RecyclerView listView;
    private FastScroller fastScroller;
    private RecyclerView multipleBucketView;
    private MultipleBucketAdapter multipleBucketAdapter;
    private LinearLayoutManager linearLayoutManager;
    private GridLayoutManager gridLayoutManager;
    private SimpleDividerItemDecoration simpleDividerItemDecoration;
    private LinearLayout headerView;
    private TextView folderNameText;
    private ImageView actionImage;
    private TextView dateText;

    public static RecentsFragment newInstance() {
        log("newInstance");
        RecentsFragment fragment = new RecentsFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        recentsFragment = this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        Display display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }
        if (megaApi.getRootNode() == null) return null;

        dbH = DatabaseHandler.getDbHandler(context);

        buckets = megaApi.getRecentActions();

        View v = inflater.inflate(R.layout.fragment_recents, container, false);

        emptyLayout = (RelativeLayout) v.findViewById(R.id.empty_state_recents);

        emptyImage = (ImageView) v.findViewById(R.id.empty_image_recents);

        RelativeLayout.LayoutParams params;
        int size;
        if (Util.isScreenInPortrait(context)) {
            size = Util.px2dp(200, outMetrics);
        } else {
            size = Util.px2dp(100, outMetrics);
        }
        params = new RelativeLayout.LayoutParams(size, size);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        emptyImage.setLayoutParams(params);

        emptyText = (TextView) v.findViewById(R.id.empty_text_recents);

        String textToShow = String.format(context.getString(R.string.context_empty_recents)).toUpperCase();
        try {
            textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
            textToShow = textToShow.replace("[/B]", "</font>");
        } catch (Exception e) {
        }
        Spanned result = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        emptyText.setText(result);

        headerView = (LinearLayout) v.findViewById(R.id.header_info_layout);
        folderNameText = (TextView) v.findViewById(R.id.folder_name_text);
        actionImage = (ImageView) v.findViewById(R.id.action_image);
        dateText = (TextView) v.findViewById(R.id.date_text);

        listLayout = (LinearLayout) v.findViewById(R.id.linear_layout_recycler);
        listView = (RecyclerView) v.findViewById(R.id.list_view_recents);
        fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);
        multipleBucketView = (RecyclerView) v.findViewById(R.id.multiple_bucket_view);
        multipleBucketView.setClipToPadding(false);
        multipleBucketView.setItemAnimator(new DefaultItemAnimator());
        multipleBucketView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });
        stickyLayoutManager = new TopSnappedStickyLayoutManager(context, this);
        listView.setLayoutManager(stickyLayoutManager);
        listView.setClipToPadding(false);
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });

        String previousDate = "";
        String currentDate;
        BucketSaved bucketSaved = ((ManagerActivityLollipop) context).getBucketSaved();
        for (int i = 0; i < buckets.size(); i++) {
            if (bucketSaved != null && bucketSaved.isTheSameBucket(buckets.get(i))) {
                setBucketSelected(buckets.get(i));
            }
            RecentsItem item = new RecentsItem(context, buckets.get(i));
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

        if (bucketSaved == null || getBucketSelected() == null) {
            ((ManagerActivityLollipop) context).setDeepBrowserTreeRecents(0);
        } else if (getBucketSelected() != null) {
            openMultipleBucket(getBucketSelected());
        }

        adapter = new RecentsAdapter(context, this, recentsItems);
        listView.setAdapter(adapter);
        listView.addItemDecoration(new HeaderItemDecoration(context, outMetrics));
        setVisibleContacts();
        setRecentsView();

        return v;
    }

    private void setRecentsView() {
        if (((ManagerActivityLollipop) context).getDeepBrowserTreeRecents() == 0) {
            if (buckets == null || buckets.isEmpty()) {
                emptyLayout.setVisibility(View.VISIBLE);
                headerView.setVisibility(View.GONE);
                listLayout.setVisibility(View.GONE);
                fastScroller.setVisibility(View.GONE);
            } else {
                emptyLayout.setVisibility(View.GONE);
                listLayout.setVisibility(View.VISIBLE);
                headerView.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
                multipleBucketView.setVisibility(View.GONE);
                fastScroller.setRecyclerView(listView);
                if (buckets.size() < Constants.MIN_ITEMS_SCROLLBAR) {
                    fastScroller.setVisibility(View.GONE);
                } else {
                    fastScroller.setVisibility(View.VISIBLE);
                }
            }
            ((ManagerActivityLollipop) context).showTabCloud(true);
        } else {
            emptyLayout.setVisibility(View.GONE);
            listLayout.setVisibility(View.VISIBLE);
            if (!isBucketSelectedMedia()) {
                headerView.setVisibility(View.VISIBLE);
                setHeaderContent();
            } else {
                headerView.setVisibility(View.GONE);
            }
            listView.setVisibility(View.GONE);
            multipleBucketView.setVisibility(View.VISIBLE);
            fastScroller.setRecyclerView(multipleBucketView);
            if (isBucketSelectedMedia() && getBucketSelected().getNodes() != null && getBucketSelected().getNodes().size() >= Constants.MIN_ITEMS_SCROLLBAR) {
                fastScroller.setVisibility(View.VISIBLE);
            } else {
                fastScroller.setVisibility(View.GONE);
            }
            ((ManagerActivityLollipop) context).showTabCloud(false);
        }
        ((ManagerActivityLollipop) context).setToolbarTitle();
        checkScroll();
    }

    private void setHeaderContent() {
        if (getBucketSelected() == null) return;

        MegaNode folder = megaApi.getNodeByHandle(getBucketSelected().getParentHandle());
        if (folder == null) return;

        folderNameText.setText(folder.getName());
        if (getBucketSelected().isUpdate()) {
            actionImage.setImageResource(R.drawable.ic_versions_small);
        } else {
            actionImage.setImageResource(R.drawable.ic_recents_up);
        }
        dateText.setText(TimeUtils.formatBucketDate(context, getBucketSelected().getTimestamp()));
    }

    public void checkScroll() {
        if (((ManagerActivityLollipop) context).getDeepBrowserTreeRecents() == 0) {
            if (listView == null) return;

            if ((listView.canScrollVertically(-1) && listView.getVisibility() == View.VISIBLE)) {
                ((ManagerActivityLollipop) context).changeActionBarElevation(true);
            } else {
                ((ManagerActivityLollipop) context).changeActionBarElevation(false);
            }
        } else {
            if (multipleBucketView == null) return;

            if ((multipleBucketView.canScrollVertically(-1) && multipleBucketView.getVisibility() == View.VISIBLE)) {
                ((ManagerActivityLollipop) context).changeActionBarElevation(true);
            } else {
                ((ManagerActivityLollipop) context).changeActionBarElevation(false);
            }
        }
    }

    public int onBackPressed() {
        if (((ManagerActivityLollipop) context).getDeepBrowserTreeRecents() > 0) {
            ((ManagerActivityLollipop) context).setDeepBrowserTreeRecents(0);
            setBucketSelected(null);
            setRecentsView();
            return 1;
        }

        return 0;
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

                MegaContactDB contactDB = dbH.findContactByHandle(contacts.get(i).getHandle() + "");
                String fullName = "";
                if (contactDB != null) {
                    ContactController cC = new ContactController(context);
                    fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contacts.get(i).getEmail());
                }

                MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contacts.get(i), fullName);
                visibleContacts.add(megaContactAdapter);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    private long[] getBucketNodeHandles(boolean areImages, boolean areVideos) {
        if (getBucketSelected() == null || getBucketSelected().getNodes() == null || getBucketSelected().getNodes().size() == 0)
            return null;

        MegaNode node;
        MegaNodeList list = getBucketSelected().getNodes();
        ArrayList<Long> nodeHandlesList = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            node = list.get(i);
            if (node == null) continue;

            if (areImages) {
                if (MimeTypeList.typeForName(node.getName()).isImage()) {
                    nodeHandlesList.add(node.getHandle());
                }
            } else if (areVideos) {
                if (FileUtils.isAudioOrVideo(node) && FileUtils.isInternalIntent(node)) {
                    nodeHandlesList.add(node.getHandle());
                }
            } else {
                nodeHandlesList.add(node.getHandle());
            }
        }

        long[] nodeHandles = new long[nodeHandlesList.size()];
        for (int i = 0; i < nodeHandlesList.size(); i++) {
            nodeHandles[i] = nodeHandlesList.get(i);
        }

        return nodeHandles;
    }

    public void openFile(MegaNode node, boolean isMedia) {
        Intent intent = null;

        if (MimeTypeList.typeForName(node.getName()).isImage()) {
            intent = new Intent(context, FullScreenImageViewerLollipop.class);
            intent.putExtra("adapterType", RECENTS_ADAPTER);
            intent.putExtra("handle", node.getHandle());
            if (isMedia) {
                intent.putExtra("nodeHandles", getBucketNodeHandles(true, false));
            }

            ((ManagerActivityLollipop) context).overridePendingTransition(0, 0);
            context.startActivity(intent);
            return;
        }

        String localPath = getLocalFile(context, node.getName(), node.getSize(), getDownloadLocation(context));
        boolean paramsSetSuccessfully = false;

        if (FileUtils.isAudioOrVideo(node)) {
            if (FileUtils.isInternalIntent(node)) {
                intent = new Intent(context, AudioVideoPlayerLollipop.class);
            } else {
                intent = new Intent(Intent.ACTION_VIEW);
            }

            intent.putExtra("adapterType", RECENTS_ADAPTER);
            intent.putExtra("FILENAME", node.getName());
            if (isMedia) {
                intent.putExtra("nodeHandles", getBucketNodeHandles(false, true));
                intent.putExtra("isPlayList", true);
            } else {
                intent.putExtra("isPlayList", false);
            }

            if (FileUtils.isLocalFile(context, node, megaApi, localPath)) {
                paramsSetSuccessfully = FileUtils.setLocalIntentParams(context, node, intent, localPath, false);
            } else {
                paramsSetSuccessfully = FileUtils.setStreamingIntentParams(context, node, megaApi, intent);
            }

            if (paramsSetSuccessfully) {
                intent.putExtra("HANDLE", node.getHandle());
                if (FileUtils.isOpusFile(node)) {
                    intent.setDataAndType(intent.getData(), "audio/*");
                }
            }
        } else if (MimeTypeList.typeForName(node.getName()).isURL()) {
            intent = new Intent(Intent.ACTION_VIEW);

            if (FileUtils.isLocalFile(context, node, megaApi, localPath)) {
                paramsSetSuccessfully = FileUtils.setURLIntentParams(context, node, intent, localPath);
            }
        } else if (MimeTypeList.typeForName(node.getName()).isPdf()) {
            intent = new Intent(context, PdfViewerActivityLollipop.class);
            intent.putExtra("inside", true);
            intent.putExtra("adapterType", RECENTS_ADAPTER);

            if (FileUtils.isLocalFile(context, node, megaApi, localPath)) {
                paramsSetSuccessfully = FileUtils.setLocalIntentParams(context, node, intent, localPath, false);
            } else {
                paramsSetSuccessfully = FileUtils.setStreamingIntentParams(context, node, megaApi, intent);
            }

            intent.putExtra("HANDLE", node.getHandle());
        }

        if (intent != null && !MegaApiUtils.isIntentAvailable(context, intent)) {
            paramsSetSuccessfully = false;
            ((ManagerActivityLollipop) context).showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.intent_not_available), -1);
        }

        if (paramsSetSuccessfully) {
            ((ManagerActivityLollipop) context).overridePendingTransition(0, 0);
            context.startActivity(intent);
            return;
        }

        ArrayList<Long> handleList = new ArrayList<Long>();
        handleList.add(node.getHandle());
        NodeController nC = new NodeController(context);
        nC.prepareForDownload(handleList, true);
    }

    public void openMultipleBucket(MegaRecentActionBucket bucket) {
        setBucketSelected(bucket);
        MegaNodeList nodeList = bucket.getNodes();
        if (nodeList == null) return;

        ((ManagerActivityLollipop) context).setDeepBrowserTreeRecents(1);
        setRecentsView();

        multipleBucketAdapter = new MultipleBucketAdapter(context, this, getNodes(nodeList), isBucketSelectedMedia());
        if (isBucketSelectedMedia()) {
            int numCells;
            if (Util.isScreenInPortrait(context)) {
                numCells = 4;
            } else {
                numCells = 6;
            }
            gridLayoutManager = new GridLayoutManager(context, numCells, GridLayoutManager.VERTICAL, false);
            multipleBucketView.setLayoutManager(gridLayoutManager);
            if (simpleDividerItemDecoration != null) {
                multipleBucketView.removeItemDecoration(simpleDividerItemDecoration);
            }
        } else {
            linearLayoutManager = new LinearLayoutManager(context);
            multipleBucketView.setLayoutManager(linearLayoutManager);
            if (simpleDividerItemDecoration == null) {
                simpleDividerItemDecoration = new SimpleDividerItemDecoration(context, outMetrics);
            }
            multipleBucketView.addItemDecoration(simpleDividerItemDecoration);
        }
        multipleBucketView.setAdapter(multipleBucketAdapter);
    }

    private ArrayList<MegaNode> getNodes(MegaNodeList list) {
        ArrayList<MegaNode> nodes = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            nodes.add(list.get(i));
        }

        return nodes;
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

    public void setBucketSelected(MegaRecentActionBucket bucketSelected) {
        this.bucketSelected = bucketSelected;
        if (bucketSelected == null) {
            ((ManagerActivityLollipop) context).setBucketSaved(null);
        } else {
            ((ManagerActivityLollipop) context).setBucketSaved(new BucketSaved(bucketSelected));
        }
    }

    public MegaRecentActionBucket getBucketSelected() {
        return bucketSelected;
    }

    public boolean isBucketSelectedMedia() {
        if (bucketSelected == null) return false;

        return bucketSelected.isMedia();
    }

    private static void log(String log) {
        Util.log("RecentsFragment", log);
    }
}
