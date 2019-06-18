package mega.privacy.android.app.lollipop.managerSections;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
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

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.RecentsItem;
import mega.privacy.android.app.components.HeaderItemDecoration;
import mega.privacy.android.app.components.TopSnappedStickyLayoutManager;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.AudioVideoPlayerLollipop;
import mega.privacy.android.app.lollipop.FullScreenImageViewerLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.PdfViewerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.RecentsAdapter;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.FileUtils;
import mega.privacy.android.app.utils.MegaApiUtils;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRecentActionBucket;
import nz.mega.sdk.MegaUser;

public class RecentsFragment extends Fragment implements View.OnClickListener, StickyHeaderHandler {

    private RecentsFragment recentsFragment;
    private Context context;
    private DisplayMetrics outMetrics;

    private DatabaseHandler dbH;
    private MegaApiAndroid megaApi;

    private ArrayList<MegaContactAdapter> visibleContacts = new ArrayList<>();
    private ArrayList<MegaRecentActionBucket> buckets;
    private ArrayList<RecentsItem> recentsItems = new ArrayList<>();
    private RecentsAdapter adapter;

    private RelativeLayout emptyLayout;
    private ImageView emptyImage;
    private TextView emptyText;
    private StickyLayoutManager stickyLayoutManager;
    private LinearLayout listLayout;
    private RecyclerView listView;
    private FastScroller fastScroller;

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

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }
        if (megaApi.getRootNode() == null) return null;

        dbH = DatabaseHandler.getDbHandler(context);

        buckets = megaApi.getRecentActions();

        View v = inflater.inflate(R.layout.fragment_recents, container, false);

        emptyLayout = (RelativeLayout) v.findViewById(R.id.empty_state_recents);

        emptyImage = (ImageView) v.findViewById(R.id.empty_image_recents);

        RelativeLayout.LayoutParams params;
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            params = new RelativeLayout.LayoutParams(Util.px2dp(200, outMetrics), Util.px2dp(200, outMetrics));
        }
        else {
            params = new RelativeLayout.LayoutParams(Util.px2dp(100, outMetrics), Util.px2dp(100, outMetrics));
        }
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        emptyImage.setLayoutParams(params);

        emptyText = (TextView) v.findViewById(R.id.empty_text_recents);

        String textToShow = String.format(context.getString(R.string.context_empty_recents)).toUpperCase();
        try {
            textToShow = textToShow.replace("[A]","<font color=\'#000000\'>");
            textToShow = textToShow.replace("[/A]","</font>");
            textToShow = textToShow.replace("[B]","<font color=\'#7a7a7a\'>");
            textToShow = textToShow.replace("[/B]","</font>");
        } catch (Exception e) {
        }
        Spanned result = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }
        emptyText.setText(result);

        listLayout  =(LinearLayout) v.findViewById(R.id.linear_layout_recycler);
        listView = (RecyclerView) v.findViewById(R.id.list_view_recents);
        fastScroller = (FastScroller) v.findViewById(R.id.fastscroll);

        if (buckets == null || buckets.isEmpty()) {
            emptyLayout.setVisibility(View.VISIBLE);
            listLayout.setVisibility(View.GONE);
            fastScroller.setVisibility(View.GONE);
        }
        else {
            emptyLayout.setVisibility(View.GONE);
            listLayout.setVisibility(View.VISIBLE);
            listView = (RecyclerView) v.findViewById(R.id.list_view_recents);
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
            for (int i=0; i<buckets.size(); i++) {
                RecentsItem item =  new RecentsItem(context, buckets.get(i));
                if (i == 0) {
                    previousDate = currentDate = item.getDate();
                    recentsItems.add(new RecentsItemHeader(currentDate));
                }
                else {
                    currentDate = item.getDate();
                    if (!currentDate.equals(previousDate)) {
                        recentsItems.add(new RecentsItemHeader(currentDate));
                        previousDate = currentDate;
                    }
                }
                recentsItems.add(item);
            }

            adapter = new RecentsAdapter(context, this, recentsItems);
            listView.setAdapter(adapter);
            listView.addItemDecoration(new HeaderItemDecoration(context, outMetrics));
            fastScroller.setRecyclerView(listView);
            setVisibleContacts();

            if (buckets.size() < Constants.MIN_ITEMS_SCROLLBAR) {
                fastScroller.setVisibility(View.GONE);
            }
            else {
                fastScroller.setVisibility(View.VISIBLE);
            }
        }

        return v;
    }

    public void checkScroll () {
        if (listView == null) return;

        if ((listView.canScrollVertically(-1) && listView.getVisibility() == View.VISIBLE)) {
            ((ManagerActivityLollipop) context).changeActionBarElevation(true);
        }
        else {
            ((ManagerActivityLollipop) context).changeActionBarElevation(false);
        }
    }

    public int onBackPressed() {
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

        for (int i=0;i<contacts.size();i++){
            if (contacts.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                MegaContactDB contactDB = dbH.findContactByHandle(contacts.get(i).getHandle()+"");
                String fullName = "";
                if(contactDB!=null){
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

    public void openFile (MegaNode node) {
        Intent intent = null;

        if (MimeTypeList.typeForName(node.getName()).isImage()) {
            intent = new Intent(context, FullScreenImageViewerLollipop.class);
            intent.putExtra("adapterType", Constants.RECENTS_ADAPTER);
            intent.putExtra("handle", node.getHandle());

            context.startActivity(intent);
            return;
        }

        String localPath = Util.getLocalFile(context, node.getName(), node.getSize(), Util.getDownloadLocation(context));
        boolean paramsSetSuccessfully = false;

        if (FileUtils.isAudioOrVideo(node)) {
            if (FileUtils.isInternalIntent(node)) {
                intent = new Intent(context, AudioVideoPlayerLollipop.class);
            }
            else {
                intent = new Intent(Intent.ACTION_VIEW);
            }

            intent.putExtra("adapterType", Constants.RECENTS_ADAPTER);
            intent.putExtra("FILENAME", node.getName());
            intent.putExtra("isPlayList", false);

            if (FileUtils.isLocalFile(context, node, megaApi, localPath)) {
                paramsSetSuccessfully = FileUtils.setLocalIntentParams(context, node, intent, localPath, false);
            }
            else {
                paramsSetSuccessfully = FileUtils.setStreamingIntentParams(context, node, megaApi, intent);
            }

            if (paramsSetSuccessfully) {
                intent.putExtra("HANDLE", node.getHandle());
                if (FileUtils.isOpusFile(node)){
                    intent.setDataAndType(intent.getData(), "audio/*");
                }
            }
        }
        else if (MimeTypeList.typeForName(node.getName()).isURL()) {
            intent = new Intent(Intent.ACTION_VIEW);

            if (FileUtils.isLocalFile(context, node, megaApi, localPath)){
                paramsSetSuccessfully = FileUtils.setURLIntentParams(context, node, intent, localPath);
            }
        }
        else if (MimeTypeList.typeForName(node.getName()).isPdf()) {
            intent = new Intent(context, PdfViewerActivityLollipop.class);
            intent.putExtra("inside", true);
            intent.putExtra("adapterType", Constants.RECENTS_ADAPTER);

            if (FileUtils.isLocalFile(context, node, megaApi, localPath)) {
                paramsSetSuccessfully = FileUtils.setLocalIntentParams(context, node, intent, localPath, false);
            }
            else {
                paramsSetSuccessfully = FileUtils.setStreamingIntentParams(context, node, megaApi, intent);
            }

            intent.putExtra("HANDLE", node.getHandle());
        }

        if (intent != null && !MegaApiUtils.isIntentAvailable(context, intent)){
            paramsSetSuccessfully = false;
            ((ManagerActivityLollipop)context).showSnackbar(Constants.SNACKBAR_TYPE, getString(R.string.intent_not_available), -1);
        }

        if (paramsSetSuccessfully) {
            context.startActivity(intent);
            return;
        }

        ArrayList<Long> handleList = new ArrayList<Long>();
        handleList.add(node.getHandle());
        NodeController nC = new NodeController(context);
        nC.prepareForDownload(handleList, true);
    }

    public void itemClick() {

    }

    @Override
    public void onClick(View v) {

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

    private static void log(String log) {
        Util.log("RecentsFragment",log);
    }
}
