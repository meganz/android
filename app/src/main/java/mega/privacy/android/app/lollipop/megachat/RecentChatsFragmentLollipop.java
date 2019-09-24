package mega.privacy.android.app.lollipop.megachat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ChatDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.lollipop.InviteContactActivity;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.ContactsHorizontalAdapter;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.lollipop.managerSections.RotatableFragment;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;

import static android.app.Activity.RESULT_OK;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class RecentChatsFragmentLollipop extends RotatableFragment implements View.OnClickListener, MegaContactGetter.MegaContactUpdater {

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";
    private static final String COLOR_START = "\'#000000\'";
    private static final String COLOR_END = "\'#7a7a7a\'";

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    DatabaseHandler dbH;

    Context context;
    ActionBar aB;
    MegaListChatLollipopAdapter adapterList;
    RelativeLayout mainRelativeLayout;

    RecyclerView listView;
    LinearLayoutManager mLayoutManager;
    FastScroller fastScroller;

    ArrayList<MegaChatListItem> chats;

    FilterChatsTask filterChatsTask;

    int lastFirstVisiblePosition;

    int numberOfClicks = 0;

    private ScrollView emptyLayoutContainer;

    //Invite bar
    private ImageView collapseBtn;
    private TextView inviteTitle;
    private FrameLayout invitationContainer;
    private RelativeLayout requestPermissionLayout;
    private Button dismissBtn;
    private Button allowBtn;
    private RelativeLayout contactsListLayout;
    private RecyclerView contactsList;
    private ImageView moreContacts;
    private ImageView moreContactsTitle;

    public static final int CONTACTS_COUNT = 4;

    private static boolean isExpand;
    private static boolean isFirstTime = true;

    private boolean grantedContactPermission;

    private MegaContactGetter contactGetter;

    private ContactsHorizontalAdapter adapter;

    //Empty screen
    private TextView emptyTextView;
    private RelativeLayout emptyLayout;
    private TextView emptyTextViewInvite;
    private ImageView emptyImageView;

    //Call
    RelativeLayout callInProgressLayout;
    Chronometer callInProgressChrono;

    Button inviteButton;
    int chatStatus;

    float density;
    DisplayMetrics outMetrics;
    Display display;

    private ActionMode actionMode;

    public static RecentChatsFragmentLollipop newInstance() {
        logDebug("newInstance");
        RecentChatsFragmentLollipop fragment = new RecentChatsFragmentLollipop();
        return fragment;
    }

    @Override
    protected RotatableAdapter getAdapter() {
        return adapterList;
    }

    public void activateActionMode() {
        logDebug("activateActionMode");
        if (!adapterList.isMultipleSelect()) {
            adapterList.setMultipleSelect(true);
            actionMode = ((AppCompatActivity) context).startSupportActionMode(new ActionBarCallBack());
        }
    }

    @Override
    public void multipleItemClick(int position) {
        adapterList.toggleSelection(position);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logDebug("onCreate");

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());

        if (isChatEnabled()) {
            if (megaChatApi == null) {
                megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
            }
        } else {
            logWarning("Chat not enabled!");
        }
        grantedContactPermission = Util.checkPermissionGranted(Manifest.permission.READ_CONTACTS, context);
        contactGetter = new MegaContactGetter(context);
        contactGetter.setMegaContactUpdater(this);
    }

    /*
        Just disable scroll
     */
    private class DisableScrollLayoutManager extends GridLayoutManager {

        public DisableScrollLayoutManager(Context context, int spanCount, int orientation, boolean reverseLayout) {
            super(context, spanCount, orientation, reverseLayout);
        }

        @Override
        public boolean canScrollHorizontally() {
            return false;
        }

        @Override
        public boolean canScrollVertically() {
            return false;
        }
    }

    @Override
    public void onFinish(List<MegaContactGetter.MegaContact> megaContacts) {
        if (!isAdded()) {
            return;
        }
        if (megaContacts.size() > 0) {
            onContactsCountChange(megaContacts);
            expandContainer();
            requestPermissionLayout.setVisibility(View.GONE);
            contactsListLayout.setVisibility(View.VISIBLE);
            collapseBtn.setVisibility(View.VISIBLE);
            inviteTitle.setClickable(true);
            moreContactsTitle.setVisibility(View.GONE);

            adapter = new ContactsHorizontalAdapter((Activity) context, this, megaContacts);
            contactsList.setLayoutManager(new DisableScrollLayoutManager(getContext(), CONTACTS_COUNT, GridLayoutManager.VERTICAL, false));
            contactsList.setAdapter(adapter);
        } else {
            noContacts();
        }
    }

    private void expandContainer() {
        if (isExpand || isFirstTime) {
            invitationContainer.setVisibility(View.VISIBLE);
            collapseBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_expand));
            isFirstTime = false;
            isExpand = true;
        } else {
            invitationContainer.setVisibility(View.GONE);
            collapseBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_collapse_acc));
            isExpand = false;
        }
    }

    @Override
    public void onException(int errorCode, String requestString) {
        logDebug(requestString + " failed, with error code: " + errorCode);
        noContacts();
    }

    @Override
    public void noContacts() {
        invitationContainer.setVisibility(View.GONE);
        inviteTitle.setText(R.string.no_local_contacts_on_mega);
        inviteTitle.setClickable(true);
        collapseBtn.setVisibility(View.INVISIBLE);
        moreContactsTitle.setVisibility(View.VISIBLE);
    }

    public void onContactsCountChange(List<MegaContactGetter.MegaContact> megaContacts) {
        int count = megaContacts.size();
        if (count > 0) {
            String title = context.getResources().getQuantityString(R.plurals.quantity_of_local_contact, count, count);
            inviteTitle.setText(title);
        } else {
            noContacts();
        }
    }

    public void checkScroll() {
        if (listView != null) {
            if (context instanceof ArchivedChatsActivity) {
                if (listView.canScrollVertically(-1) || (adapterList != null && adapterList.isMultipleSelect())) {
                    ((ArchivedChatsActivity) context).changeActionBarElevation(true);
                } else {
                    ((ArchivedChatsActivity) context).changeActionBarElevation(false);
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        logDebug("onCreateView");

        display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        View v = inflater.inflate(R.layout.chat_recent_tab, container, false);

        emptyLayoutContainer = v.findViewById(R.id.scroller);
        listView = (RecyclerView) v.findViewById(R.id.chat_recent_list_view);
        fastScroller = (FastScroller) v.findViewById(R.id.fastscroll_chat);
        listView.setPadding(0, 0, 0, scaleHeightPx(85, outMetrics));
        listView.setClipToPadding(false);
        listView.addItemDecoration(new ChatDividerItemDecoration(context, outMetrics));
        mLayoutManager = new LinearLayoutManager(context);
        listView.setLayoutManager(mLayoutManager);
        listView.setHasFixedSize(true);
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });
//        listView.setClipToPadding(false);

        emptyLayout = v.findViewById(R.id.linear_empty_layout_chat_recent);
        emptyTextViewInvite = v.findViewById(R.id.empty_text_chat_recent_invite);
        emptyTextViewInvite.setWidth(Util.scaleWidthPx(236, outMetrics));
        emptyTextView = v.findViewById(R.id.empty_text_chat_recent);
        emptyImageView = v.findViewById(R.id.empty_image_view_recent);
        emptyImageView.setOnClickListener(this);

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            adjustLandscape();
            emptyImageView.setImageResource(R.drawable.chat_empty_landscape);
        } else {
            addMarginTop();
            emptyImageView.setImageResource(R.drawable.ic_empty_chat_list);
        }

        inviteButton = (Button) v.findViewById(R.id.invite_button);
        inviteButton.setOnClickListener(this);

        callInProgressLayout = (RelativeLayout) v.findViewById(R.id.call_in_progress_layout);
        callInProgressLayout.setOnClickListener(this);
        callInProgressChrono = (Chronometer) v.findViewById(R.id.call_in_progress_chrono);
        callInProgressLayout.setVisibility(View.GONE);
        callInProgressChrono.setVisibility(View.GONE);

        mainRelativeLayout = (RelativeLayout) v.findViewById(R.id.main_relative_layout);
        //auto scroll to bottom to show invite button.
        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            adjustLandscape();
        } else {
            addMarginTop();
        }
        if(isChatEnabled()){
            logDebug("Chat ENABLED");

            if (context instanceof ManagerActivityLollipop) {
                setStatus();
                if (!emptyArchivedChats()) {
                    listView.setPadding(0, scaleHeightPx(8, outMetrics), 0, scaleHeightPx(16, outMetrics));
                } else {
                    listView.setPadding(0, scaleHeightPx(8, outMetrics), 0, scaleHeightPx(78, outMetrics));
                }
            } else {
                //Archived chats section
                aB.setSubtitle(null);
                listView.setPadding(0, scaleHeightPx(8, outMetrics), 0, 0);
            }

            this.setChats();

            if (megaChatApi.isSignalActivityRequired()) {
                megaChatApi.signalPresenceActivity();
            }
        } else {
            logDebug("Chat DISABLED");
            if (isOnline(context)) {
                showDisableChatScreen();
            } else {
                showNoConnectionScreen();
            }
        }

        //Invitation bar
        collapseBtn = v.findViewById(R.id.collapse_btn);
        collapseBtn.setOnClickListener(this);
        inviteTitle = v.findViewById(R.id.invite_title);
        inviteTitle.setOnClickListener(this);
        requestPermissionLayout = v.findViewById(R.id.request_permission_layout);
        dismissBtn = v.findViewById(R.id.dismiss_button);
        dismissBtn.setOnClickListener(this);
        allowBtn = v.findViewById(R.id.allow_button);
        allowBtn.setOnClickListener(this);
        invitationContainer = v.findViewById(R.id.contacts_list_container);
        contactsListLayout = v.findViewById(R.id.contacts_list_layout);
        contactsList = v.findViewById(R.id.contacts_list);
        moreContacts = v.findViewById(R.id.more_contacts);
        moreContacts.setOnClickListener(this);
        moreContactsTitle = v.findViewById(R.id.more_contacts_title);
        moreContactsTitle.setOnClickListener(this);
        return v;
    }

    private void adjustLandscape() {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(0,0,0,0);
        emptyLayoutContainer.setLayoutParams(layoutParams);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                emptyLayoutContainer.fullScroll(View.FOCUS_DOWN);
            }
        },100);
    }

    private void showPermissionGrantedView() {
        requestPermissionLayout.setVisibility(View.GONE);
        contactsListLayout.setVisibility(View.VISIBLE);
        collapseBtn.setVisibility(View.INVISIBLE);
        inviteTitle.setText(R.string.get_registered_contacts);
        inviteTitle.setClickable(false);
        moreContactsTitle.setVisibility(View.GONE);
        invitationContainer.setVisibility(View.GONE);

        loadMegaContacts();
    }


    private void showPermissionDeniedView() {
        expandContainer();
        collapseBtn.setVisibility(View.VISIBLE);
        inviteTitle.setClickable(true);
        inviteTitle.setText(R.string.see_local_contacts_on_mega);
        moreContactsTitle.setVisibility(View.GONE);
        contactsListLayout.setVisibility(View.GONE);
        requestPermissionLayout.setVisibility(View.VISIBLE);
    }

    public void setChats(){
        logDebug("setChats");

        if (listView == null) {
            logWarning("listView is null - do not update");
            return;
        }

        if (isAdded()) {
            if (isChatEnabled()) {

                int initState = megaChatApi.getInitState();
                logDebug("Init state is: " + initState);

                if ((initState == MegaChatApi.INIT_ONLINE_SESSION)) {
                    logDebug("Connected state is: " + megaChatApi.getConnectionState());

                    if (megaChatApi.getConnectionState() == MegaChatApi.CONNECTED) {
                        if (chats != null) {
                            chats.clear();
                        } else {
                            chats = new ArrayList<MegaChatListItem>();
                        }

                        if (context instanceof ManagerActivityLollipop) {
                            chats = megaChatApi.getChatListItems();
                        } else {
                            chats = megaChatApi.getArchivedChatListItems();
                        }

                        if ((chats == null || chats.isEmpty()) && emptyArchivedChats()) {
                            if (isOnline(context)) {
                                showEmptyChatScreen();
                            } else {
                                showNoConnectionScreen();
                            }
                        } else {
                            logDebug("Chats size: " + chats.size());

                            //Order by last interaction
                            Collections.sort(chats, new Comparator<MegaChatListItem>() {

                                public int compare(MegaChatListItem c1, MegaChatListItem c2) {
                                    long timestamp1 = c1.getLastTimestamp();
                                    long timestamp2 = c2.getLastTimestamp();

                                    long result = timestamp2 - timestamp1;
                                    return (int) result;
                                }
                            });

                            if (adapterList == null) {
                                logWarning("AdapterList is NULL");
                                adapterList = new MegaListChatLollipopAdapter(context, this, chats, listView, MegaListChatLollipopAdapter.ADAPTER_RECENT_CHATS);
                            } else {
                                adapterList.setChats(chats);
                            }

                            listView.setAdapter(adapterList);
                            fastScroller.setRecyclerView(listView);
                            visibilityFastScroller();

                            adapterList.setPositionClicked(-1);

                            listView.setVisibility(View.VISIBLE);
                            emptyLayout.setVisibility(View.GONE);
                        }
                    } else {
                        logDebug("Show chat screen connecting...");
                        showConnectingChatScreen();
                    }
                } else if (initState == MegaChatApi.INIT_OFFLINE_SESSION) {
                    logDebug("Init with OFFLINE session");
                    if (chats != null) {
                        chats.clear();
                    } else {
                        chats = new ArrayList<MegaChatListItem>();
                    }

                    if (context instanceof ManagerActivityLollipop) {
                        chats = megaChatApi.getChatListItems();
                    } else {
                        chats = megaChatApi.getArchivedChatListItems();
                    }

                    if (chats == null || chats.isEmpty()) {
                        showNoConnectionScreen();
                    } else {
                        logDebug("Chats no: " + chats.size());

                        //Order by last interaction
                        Collections.sort(chats, new Comparator<MegaChatListItem>() {

                            public int compare(MegaChatListItem c1, MegaChatListItem c2) {
                                long timestamp1 = c1.getLastTimestamp();
                                long timestamp2 = c2.getLastTimestamp();

                                long result = timestamp2 - timestamp1;
                                return (int) result;
                            }
                        });

                        if (listView == null) {
                            logWarning("INIT_OFFLINE_SESSION: listView is null");
                        } else if (listView != null) {
                            listView.setVisibility(View.VISIBLE);
                        }
                        if (emptyLayout != null) {
                            emptyLayout.setVisibility(View.GONE);
                        }

                        if (adapterList == null) {
                            logWarning("AdapterList is NULL");
                            adapterList = new MegaListChatLollipopAdapter(context, this, chats, listView, MegaListChatLollipopAdapter.ADAPTER_RECENT_CHATS);
                            if (listView != null) {
                                listView.setAdapter(adapterList);
                            }
                        } else {
                            adapterList.setChats(chats);
                        }

                        fastScroller.setRecyclerView(listView);
                        visibilityFastScroller();
                        adapterList.setPositionClicked(-1);
                    }
                } else {
                    logDebug("Show chat screen connecting...");
                    showConnectingChatScreen();
                }
            } else {
                if (isOnline(context)) {
                    showDisableChatScreen();
                } else {
                    showNoConnectionScreen();
                }
            }
        }
    }


    public void showCallLayout() {
        if (isChatEnabled() && context instanceof ManagerActivityLollipop && megaChatApi != null && participatingInACall(megaChatApi)) {
            logDebug("showCallLayout");

            if (callInProgressLayout != null && callInProgressLayout.getVisibility() != View.VISIBLE) {
                callInProgressLayout.setVisibility(View.VISIBLE);
            }
            if (callInProgressChrono != null && callInProgressChrono.getVisibility() != View.VISIBLE) {
                long chatId = getChatCallInProgress(megaChatApi);
                if ((megaChatApi != null) && chatId != -1) {
                    MegaChatCall call = megaChatApi.getChatCall(chatId);
                    if (call != null) {
                        callInProgressChrono.setVisibility(View.VISIBLE);
                        callInProgressChrono.setBase(SystemClock.elapsedRealtime() - (call.getDuration() * 1000));
                        callInProgressChrono.start();
                        callInProgressChrono.setFormat("%s");
                    }
                }
            }
        } else {

            if (callInProgressChrono != null) {
                callInProgressChrono.stop();
                callInProgressChrono.setVisibility(View.GONE);
            }
            if (callInProgressLayout != null) {
                callInProgressLayout.setVisibility(View.GONE);
            }
        }
    }

    public void showEmptyChatScreen() {
        logDebug("showEmptyChatScreen");

        listView.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.VISIBLE);
        String textToShow, colorStart, colorEnd;
        Spanned result;

        if (context instanceof ArchivedChatsActivity) {
            textToShow = context.getString(R.string.recent_chat_empty).toUpperCase();
            colorStart = COLOR_END;
            colorEnd = COLOR_START;
            result = getSpannedMessageForEmptyChat(textToShow, colorStart, colorEnd);

            if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                emptyTextView.setVisibility(View.GONE);
            } else {
                emptyTextView.setVisibility(View.VISIBLE);
            }

            emptyTextViewInvite.setVisibility(View.GONE);
            inviteButton.setVisibility(View.GONE);
            emptyTextView.setText(result);


        } else {
            textToShow = context.getString(R.string.context_empty_chat_recent);
            colorStart = COLOR_START;
            colorEnd = COLOR_END;
            result = getSpannedMessageForEmptyChat(textToShow, colorStart, colorEnd);

            emptyTextViewInvite.setText(result);
            emptyTextViewInvite.setVisibility(View.VISIBLE);
            inviteButton.setText(getString(R.string.contact_invite));
            inviteButton.setVisibility(View.VISIBLE);
        }
    }

    private Spanned getSpannedMessageForEmptyChat(String originalMessage, String colorStart, String colorEnd){
        String textToShow = originalMessage;
        Spanned result;
        try {
            textToShow = textToShow.replace("[A]", "<font color=" + colorStart + ">");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=" + colorEnd + ">");
            textToShow = textToShow.replace("[/B]", "</font>");
        } catch (Exception e) {
            logError(e.getStackTrace().toString());
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);
        }

        return result;
    }

    public void showDisableChatScreen() {
        logDebug("showDisableChatScreen");

        listView.setVisibility(View.GONE);
        if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop) context).hideFabButton();
        }

        String textToShow = String.format(context.getString(R.string.recent_chat_empty_enable_chat));

        try {
            textToShow = textToShow.replace("[A]", "<br />");
            textToShow = textToShow.replace("[B]", "<font color=" + COLOR_START + ">");
            textToShow = textToShow.replace("[/B]", "</font>");
            textToShow = textToShow.replace("[C]", "<font color=" + COLOR_END + ">");
            textToShow = textToShow.replace("[/C]", "</font>");

        } catch (Exception e) {
        }
        Spanned result = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);

        }
        emptyTextViewInvite.setText(result);
        emptyTextViewInvite.setVisibility(View.VISIBLE);

        inviteButton.setText(getString(R.string.recent_chat_enable_chat_button));
        inviteButton.setVisibility(View.VISIBLE);

        emptyTextView.setText(R.string.recent_chat_enable_chat);
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            adjustLandscape();
            emptyTextView.setVisibility(View.GONE);
        } else {
            addMarginTop();
            emptyTextView.setVisibility(View.VISIBLE);
        }
        emptyLayout.setVisibility(View.VISIBLE);
    }

    private void addMarginTop() {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(0, Util.scaleHeightPx(60, outMetrics), 0, 0);
        emptyLayoutContainer.setLayoutParams(layoutParams);
    }

    public void showConnectingChatScreen(){
        logDebug("showConnectingChatScreen");

        listView.setVisibility(View.GONE);
        if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop) context).hideFabButton();
        }

        String textToShow = String.format(context.getString(R.string.context_empty_chat_recent));

        try {
            textToShow = textToShow.replace("[A]", "<font color=" + COLOR_START + ">");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=" + COLOR_END + ">");
            textToShow = textToShow.replace("[/B]", "</font>");
        } catch (Exception e) {
        }
        Spanned result = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(textToShow);

        }

        emptyTextViewInvite.setText(result);
        emptyTextViewInvite.setVisibility(View.INVISIBLE);

        inviteButton.setVisibility(View.GONE);

        String textToShowB = String.format(context.getString(R.string.recent_chat_loading_conversations));
        try {
            textToShowB = textToShowB.replace("[A]", "<font color=" + COLOR_END + ">");
            textToShowB = textToShowB.replace("[/A]", "</font>");
            textToShowB = textToShowB.replace("[B]", "<font color=" + COLOR_START + ">");
            textToShowB = textToShowB.replace("[/B]", "</font>");
        } catch (Exception e) {
        }
        Spanned resultB = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resultB = Html.fromHtml(textToShowB, Html.FROM_HTML_MODE_LEGACY);
        } else {
            resultB = Html.fromHtml(textToShowB);
        }
        emptyTextView.setText(resultB);
        emptyTextView.setVisibility(View.VISIBLE);
    }

    public void showNoConnectionScreen() {
        logDebug("showNoConnectionScreen");

        listView.setVisibility(View.GONE);
        if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop) context).hideFabButton();
        }

        emptyTextViewInvite.setText(getString(R.string.error_server_connection_problem));
        emptyTextViewInvite.setVisibility(View.VISIBLE);
        inviteButton.setVisibility(View.GONE);

        emptyTextView.setText(R.string.recent_chat_empty_no_connection_text);
        if (isChatEnabled()) {
            emptyTextView.setVisibility(View.GONE);
        } else {
            emptyTextView.setVisibility(View.VISIBLE);
        }

        emptyLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        switch (v.getId()) {
            case R.id.invite_button: {
                if (isChatEnabled()) {
                    if (isOnline(context)) {
                        ((ManagerActivityLollipop) context).addContactFromPhone();
                        if (megaChatApi.isSignalActivityRequired()) {
                            megaChatApi.signalPresenceActivity();
                        }
                    } else {
                        ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                    }
                } else {
                    if (isOnline(context)) {
                        if (megaApi != null) {
                            if (megaApi.isLoggedIn() == 0) {
                                ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_enable_chat_before_login), -1);
                            } else {
                                ChatController chatController = new ChatController(context);
                                logDebug("Enable Chat");
                                chatController.enableChat();
                                getActivity().invalidateOptionsMenu();
                                ((ManagerActivityLollipop) context).enableChat();
                            }
                        } else {
                            ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_enable_chat_before_login), -1);
                        }
                    } else {
                        ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                        showNoConnectionScreen();
                    }
                }

                break;
            }
            case R.id.empty_image_view_chat: {
                numberOfClicks++;
                logDebug("Number of clicks: " + numberOfClicks);
                if (numberOfClicks >= 5) {
                    numberOfClicks = 0;
                    showStateInfo();
                }

                break;
            }
            case R.id.call_in_progress_layout: {
                logDebug("call_in_progress_layout");
                if (checkPermissionsCall()) {
                    returnCall(context, megaChatApi);
                }
                break;
            }
            case R.id.invite_title:
            case R.id.dismiss_button:
            case R.id.collapse_btn:
                if(moreContactsTitle.getVisibility() == View.VISIBLE) {
                    startActivityForResult(new Intent(context, InviteContactActivity.class), Constants.REQUEST_INVITE_CONTACT_FROM_DEVICE);
                } else {
                    if (invitationContainer.getVisibility() == View.VISIBLE) {
                        invitationContainer.setVisibility(View.GONE);
                        collapseBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_collapse_acc));
                        isExpand = false;
                    } else {
                        invitationContainer.setVisibility(View.VISIBLE);
                        collapseBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_expand));
                        isExpand = true;
                    }
                }
                break;
            case R.id.allow_button:
                logDebug("request contact permission!");
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, Constants.REQUEST_READ_CONTACTS);
                break;
            case R.id.more_contacts_title:
            case R.id.more_contacts:
                logDebug("to InviteContactActivity");
                startActivityForResult(new Intent(context, InviteContactActivity.class), Constants.REQUEST_INVITE_CONTACT_FROM_DEVICE);
                break;
        }
    }

    public void showStateInfo() {

        StringBuilder builder = new StringBuilder();

        if (isChatEnabled()) {
            if (megaChatApi != null) {
                builder.append("INIT STATE: " + megaChatApi.getInitState());
                builder.append("\nCONNECT STATE: " + megaChatApi.getConnectionState());
                if (isOnline(context)) {
                    builder.append("\nNetwork OK");
                } else {
                    builder.append("\nNo network connection");
                }
            } else {
                builder.append("MegaChatApi: false");
            }
        } else {
            builder.append("Chat is disabled");
            if (megaChatApi != null) {
                builder.append("\nINIT STATE: " + megaChatApi.getInitState());
                builder.append("\nCONNECT STATE: " + megaChatApi.getConnectionState());
                if (isOnline(context)) {
                    builder.append("\nNetwork OK");
                } else {
                    builder.append("\nNo network connection");
                }
            }
        }

        Toast.makeText(context, builder, Toast.LENGTH_LONG).show();
    }

    public boolean showSelectMenuItem() {
        if (adapterList != null) {
            return adapterList.isMultipleSelect();
        }

        return false;
    }

    /*
     * Clear all selected items
     */
    public void clearSelections() {
        logDebug("clearSelections");
        if (adapterList.isMultipleSelect()) {
            adapterList.clearSelections();
        }
    }

    @Override
    protected void updateActionModeTitle() {
        if (actionMode == null || getActivity() == null) {
            return;
        }
        List<MegaChatListItem> chats = adapterList.getSelectedChats();

        actionMode.setTitle(chats.size() + "");

        try {
            actionMode.invalidate();
        } catch (NullPointerException e) {
            e.printStackTrace();
            logError("Invalidate error", e);
        }
    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        logDebug("hideMultipleSelect");
        adapterList.setMultipleSelect(false);

        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void selectAll() {
        if (adapterList != null) {
            if (adapterList.isMultipleSelect()) {
                adapterList.selectAll();
            } else {
                adapterList.setMultipleSelect(true);
                adapterList.selectAll();

                actionMode = ((AppCompatActivity) context).startSupportActionMode(new ActionBarCallBack());
            }

            updateActionModeTitle();
        }
    }

    public void itemClick(int position) {
        logDebug("Position: " + position);
        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }
        if (adapterList.isMultipleSelect()) {
            adapterList.toggleSelection(position);
            List<MegaChatListItem> chats = adapterList.getSelectedChats();
            if (chats.size() > 0) {
                updateActionModeTitle();
            }
        } else {
            logDebug("Open chat: Position: " + position + ", Chat ID: " + chats.get(position).getChatId());
            Intent intent = new Intent(context, ChatActivityLollipop.class);
            intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intent.putExtra("CHAT_ID", adapterList.getChatAt(position).getChatId());
            this.startActivity(intent);
            if (context instanceof ManagerActivityLollipop) {
                if (((ManagerActivityLollipop) context).searchQuery != null && !((ManagerActivityLollipop) context).searchQuery.isEmpty()) {
                    closeSearch();
                    ((ManagerActivityLollipop) context).closeSearchView();
                }
            } else if (context instanceof ArchivedChatsActivity) {
                if (((ArchivedChatsActivity) context).querySearch != null && !((ArchivedChatsActivity) context).querySearch.isEmpty()) {
                    closeSearch();
                    ((ArchivedChatsActivity) context).closeSearchView();
                }
            }
        }
    }
    /////END Multiselect/////

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity) activity).getSupportActionBar();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity) context).getSupportActionBar();
    }

    public void listItemUpdate(MegaChatListItem item) {
        if (item == null) {
            logWarning("Item is null");
            return;
        }

        logDebug("Chat ID: " + item.getChatId());

        if (!isAdded()) {
            logDebug("return!");
            return;
        }

        if (listView == null) {
            logWarning("listView is null - do not update");
            return;
        }

        if (context instanceof ManagerActivityLollipop) {
            if (!(((ManagerActivityLollipop) context).getDrawerItem() == ManagerActivityLollipop.DrawerItem.CHAT)) {
                logWarning("Not CHAT shown!");
                return;
            }
        }

        if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_STATUS)) {
            logDebug("Change status: MegaChatListItem.CHANGE_TYPE_STATUS");
        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_OWN_PRIV)) {

            logDebug("Change status: MegaChatListItem.CHANGE_TYPE_OWN_PRIV");
        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_PARTICIPANTS)) {

            logDebug("Change participants");
            MegaChatRoom chatToCheck = megaChatApi.getChatRoom(item.getChatId());
            updateCacheForNonContacts(chatToCheck);
        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_UNREAD_COUNT)) {

            logDebug(" Change unread count");
            if (adapterList == null || adapterList.getItemCount() == 0) {
                setChats();
            } else {
                long chatHandleToUpdate = item.getChatId();
                int indexToReplace = -1;
                ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                while (itrReplace.hasNext()) {
                    MegaChatListItem chat = itrReplace.next();
                    if (chat != null) {
                        if (chat.getChatId() == chatHandleToUpdate) {
                            indexToReplace = itrReplace.nextIndex() - 1;
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (indexToReplace != -1) {
                    logDebug("Index to replace: " + indexToReplace);
                    chats.set(indexToReplace, item);
                    if (item.getUnreadCount() == 0) {
                        logDebug("No unread count");
                        onUnreadCountChange(indexToReplace, false);
                        onLastMessageChange(indexToReplace);
                    } else {
                        onUnreadCountChange(indexToReplace, true);
                    }
                }
            }

        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_LAST_TS)) {
            logDebug("Change last ts: " + item.getChanges());

            long chatHandleToUpdate = item.getChatId();
            int indexToReplace = -1;
            if (chats != null && !chats.isEmpty()) {
                ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                while (itrReplace.hasNext()) {
                    MegaChatListItem chat = itrReplace.next();
                    if (chat != null) {
                        if (chat.getChatId() == chatHandleToUpdate) {
                            indexToReplace = itrReplace.nextIndex() - 1;
                            break;
                        }
                    } else {
                        break;
                    }
                }
            }

            if (indexToReplace != -1) {
                logDebug("Index to replace: " + indexToReplace);
                chats.set(indexToReplace, item);
                if (indexToReplace == 0) {
                    onLastTsChange(indexToReplace, false);
                } else {
                    onLastTsChange(indexToReplace, true);
                }
            }

        } else if ((item.hasChanged(MegaChatListItem.CHANGE_TYPE_TITLE))) {

            logDebug("Change title ");
            if (adapterList == null || adapterList.getItemCount() == 0) {
                setChats();
            } else {
                long chatHandleToUpdate = item.getChatId();
                int indexToReplace = -1;
                ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                while (itrReplace.hasNext()) {
                    MegaChatListItem chat = itrReplace.next();
                    if (chat != null) {
                        if (chat.getChatId() == chatHandleToUpdate) {
                            indexToReplace = itrReplace.nextIndex() - 1;
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (indexToReplace != -1) {
                    logDebug("Index to replace: " + indexToReplace);
                    chats.set(indexToReplace, item);
                    onTitleChange(indexToReplace);
                }
            }

        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_LAST_MSG)) {

            logDebug("Change last message: ");
            if (adapterList == null || adapterList.getItemCount() == 0) {
                setChats();
            } else {
                long chatHandleToUpdate = item.getChatId();
                int indexToReplace = -1;
                ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                while (itrReplace.hasNext()) {
                    MegaChatListItem chat = itrReplace.next();
                    if (chat != null) {
                        if (chat.getChatId() == chatHandleToUpdate) {
                            indexToReplace = itrReplace.nextIndex() - 1;
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (indexToReplace != -1) {
                    logDebug("Index to replace: " + indexToReplace);
                    chats.set(indexToReplace, item);
                    onLastMessageChange(indexToReplace);
                }
            }

        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_CLOSED)) {

            logDebug("Change closed: MegaChatListItem.CHANGE_TYPE_CLOSED");
            logDebug("Own privilege: " + item.getOwnPrivilege());
            if (adapterList.getItemCount() != 0) {
                long chatHandleToRemove = item.getChatId();
                int indexToRemove = -1;
                ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                while (itrReplace.hasNext()) {
                    MegaChatListItem chat = itrReplace.next();
                    if (chat != null) {
                        if (chat.getChatId() == chatHandleToRemove) {
                            indexToRemove = itrReplace.nextIndex() - 1;
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (indexToRemove != -1) {
                    logDebug("Index to replace: " + indexToRemove);
                    chats.remove(indexToRemove);

                    adapterList.removeChat(chats, indexToRemove);
                    adapterList.setPositionClicked(-1);

                    if (adapterList.getItemCount() == 0 && emptyArchivedChats()) {
                        logDebug("adapterList.getItemCount() == 0");
                        listView.setVisibility(View.GONE);
                        emptyLayout.setVisibility(View.VISIBLE);
                    } else {
                        listView.setVisibility(View.VISIBLE);
                        emptyLayout.setVisibility(View.GONE);
                    }
                }
            }

        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_ARCHIVE)) {
            logDebug("Change: MegaChatListItem.CHANGE_TYPE_ARCHIVE");
            if (context instanceof ManagerActivityLollipop) {
                if (item.isArchived()) {
                    logDebug("New archived element:remove from list");
                    if (adapterList == null || adapterList.getItemCount()==0){
                        setChats();
                    } else {
                        long chatHandleToRemove = item.getChatId();
                        int indexToRemove = -1;
                        ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                        while (itrReplace.hasNext()) {
                            MegaChatListItem chat = itrReplace.next();
                            if (chat != null) {
                                if (chat.getChatId() == chatHandleToRemove) {
                                    indexToRemove = itrReplace.nextIndex() - 1;
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                        if (indexToRemove != -1) {
                            logDebug("Index to replace: " + indexToRemove);
                            chats.remove(indexToRemove);

                            adapterList.removeChat(chats, indexToRemove);
                            adapterList.setPositionClicked(-1);

                            if (adapterList.getItemCount() == 0 && emptyArchivedChats()) {
                                logDebug("adapterList.getItemCount() == 0");
                                listView.setVisibility(View.GONE);
                                emptyLayout.setVisibility(View.VISIBLE);
                            } else {
                                listView.setVisibility(View.VISIBLE);
                                emptyLayout.setVisibility(View.GONE);
                            }

                            if (chats.isEmpty()) {
                                ((ManagerActivityLollipop) context).invalidateOptionsMenu();
                            }
                        }
                    }
                } else {
                    logDebug("New unarchived element: refresh chat list");
                    setChats();
                    if (chats.size() == 1) {
                        ((ManagerActivityLollipop) context).invalidateOptionsMenu();
                    }
                }
                //Update last position
                if (adapterList != null) {
                    adapterList.notifyItemChanged(chats.size() + 1);
                }

                if (!emptyArchivedChats()) {
                    listView.setPadding(0, scaleHeightPx(8, outMetrics), 0, scaleHeightPx(16, outMetrics));
                } else {
                    listView.setPadding(0, scaleHeightPx(8, outMetrics), 0, scaleHeightPx(78, outMetrics));
                }

                checkScroll();
            } else if (context instanceof ArchivedChatsActivity) {
                if (item.isArchived()) {
                    logDebug("New archived element: refresh chat list");
                    setChats();
                } else {
                    logDebug("New unarchived element: remove from Archive list");
                    if (adapterList.getItemCount() != 0) {

                        long chatHandleToRemove = item.getChatId();
                        int indexToRemove = -1;
                        ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                        while (itrReplace.hasNext()) {
                            MegaChatListItem chat = itrReplace.next();
                            if (chat != null) {
                                if (chat.getChatId() == chatHandleToRemove) {
                                    indexToRemove = itrReplace.nextIndex() - 1;
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                        if (indexToRemove != -1) {
                            logDebug("Index to replace: " + indexToRemove);
                            chats.remove(indexToRemove);

                            adapterList.removeChat(chats, indexToRemove);
                            adapterList.setPositionClicked(-1);

                            if (adapterList.getItemCount() == 0) {
                                logDebug("adapterList.getItemCount() == 0");
                                showEmptyChatScreen();
                                ((ArchivedChatsActivity) context).invalidateOptionsMenu();
                            } else {
                                listView.setVisibility(View.VISIBLE);
                                emptyLayout.setVisibility(View.GONE);
                            }
                        }
                    }
                }
                checkScroll();
            }

        } else if (item.hasChanged(MegaChatListItem.CHANGE_TYPE_CALL) || item.hasChanged(MegaChatListItem.CHANGE_TYPE_CHAT_MODE)) {
            logDebug("Change: MegaChatListItem.CHANGE_TYPE_CALL or CHANGE_TYPE_CHAT_MODE");
            if (adapterList == null || adapterList.getItemCount() == 0) {
                setChats();
            } else {
                long chatHandleToUpdate = item.getChatId();
                int indexToReplace = -1;
                ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                while (itrReplace.hasNext()) {
                    MegaChatListItem chat = itrReplace.next();
                    if (chat != null) {
                        if (chat.getChatId() == chatHandleToUpdate) {
                            indexToReplace = itrReplace.nextIndex() - 1;
                            break;
                        }
                    } else {
                        break;
                    }
                }
                if (indexToReplace != -1) {
                    logDebug("Index to replace: " + indexToReplace);
                    chats.set(indexToReplace, item);
                    adapterList.notifyItemChanged(indexToReplace);
                }
            }
        } else {
            logDebug("Other change: " + item.getChanges());

            if (item != null) {
                logDebug("New chat");
                setChats();
                MegaChatRoom chatToCheck = megaChatApi.getChatRoom(item.getChatId());
                updateCacheForNonContacts(chatToCheck);
            } else {
                logError("The chat is NULL");
            }
        }
    }

    boolean emptyArchivedChats() {
        ArrayList<MegaChatListItem> archivedChats = megaChatApi.getArchivedChatListItems();

        if (archivedChats == null || archivedChats.isEmpty()) {
            return true;
        }

        return false;
    }

    public void setStatus() {
        logDebug("setStatus");
        if (isChatEnabled()) {
            chatStatus = megaChatApi.getOnlineStatus();
            logDebug("Chat status --> getOnlineStatus with megaChatApi: " + chatStatus);

            onlineStatusUpdate(chatStatus);
        }
    }

    public void onlineStatusUpdate(int status) {
        logDebug("Status: " + status);

        chatStatus = status;

        if (isAdded()) {
            if (aB != null) {
                switch (status) {
                    case MegaChatApi.STATUS_ONLINE: {
                        aB.setSubtitle(adjustForLargeFont(getString(R.string.online_status)));
                        break;
                    }
                    case MegaChatApi.STATUS_AWAY: {
                        aB.setSubtitle(adjustForLargeFont(getString(R.string.away_status)));
                        break;
                    }
                    case MegaChatApi.STATUS_BUSY: {
                        aB.setSubtitle(adjustForLargeFont(getString(R.string.busy_status)));
                        break;
                    }
                    case MegaChatApi.STATUS_OFFLINE: {
                        aB.setSubtitle(adjustForLargeFont(getString(R.string.offline_status)));
                        break;
                    }
                    case MegaChatApi.STATUS_INVALID: {
                        if (!isOnline(context)) {
                            aB.setSubtitle(adjustForLargeFont(getString(R.string.error_server_connection_problem)));
                        } else {
                            if (megaChatApi == null) {
                                aB.setSubtitle(adjustForLargeFont(getString(R.string.invalid_connection_state)));
                            } else if (megaChatApi.getConnectionState() == MegaChatApi.CONNECTING) {
                                aB.setSubtitle(adjustForLargeFont(getString(R.string.chat_connecting)));
                            } else if (megaChatApi.getConnectionState() == MegaChatApi.DISCONNECTED) {
                                aB.setSubtitle(adjustForLargeFont(getString(R.string.invalid_connection_state)));
                            } else {
                                aB.setSubtitle(null);
                            }
                        }
                        break;
                    }
                    default: {

                        if (!isOnline(context) || megaApi == null || megaApi.getRootNode() == null) {
                            aB.setSubtitle(adjustForLargeFont(getString(R.string.error_server_connection_problem)));
                        } else {
                            if (megaChatApi == null) {
                                aB.setSubtitle(adjustForLargeFont(getString(R.string.invalid_connection_state)));
                            } else if (megaChatApi.getConnectionState() == MegaChatApi.CONNECTING) {
                                aB.setSubtitle(adjustForLargeFont(getString(R.string.chat_connecting)));
                            } else if (megaChatApi.getConnectionState() == MegaChatApi.DISCONNECTED) {
                                aB.setSubtitle(adjustForLargeFont(getString(R.string.invalid_connection_state)));
                            } else {
                                int initStatus = megaChatApi.getInitState();
                                if (initStatus == MegaChatApi.INIT_WAITING_NEW_SESSION || initStatus == MegaChatApi.INIT_NO_CACHE) {
                                    aB.setSubtitle(adjustForLargeFont(getString(R.string.chat_connecting)));
                                } else {
                                    aB.setSubtitle(null);
                                }
                            }
                        }
                        break;
                    }
                }
            } else {
                logWarning("aB is NULL");
            }
        } else {
            logWarning("RecentChats not added");
        }
    }

    public void contactStatusUpdate(long userHandle, int status) {
        logDebug("User Handle: " + userHandle + ", Status: " + status);

        long chatHandleToUpdate = -1;
        MegaChatRoom chatToUpdate = megaChatApi.getChatRoomByUser(userHandle);
        if (chatToUpdate != null) {
            chatHandleToUpdate = chatToUpdate.getChatId();
            logDebug("Update chat: " + chatHandleToUpdate);
            if (chatHandleToUpdate != -1) {
                logDebug("The user has a one to one chat: " + chatHandleToUpdate);

                int indexToReplace = -1;
                if (chats != null) {
                    ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
                    while (itrReplace.hasNext()) {
                        MegaChatListItem chat = itrReplace.next();
                        if (chat != null) {
                            if (chat.getChatId() == chatHandleToUpdate) {
                                indexToReplace = itrReplace.nextIndex() - 1;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (indexToReplace != -1) {
                        logDebug("Index to replace: " + indexToReplace);
                        onStatusChange(indexToReplace, userHandle, status);
                    }
                } else {
                    logWarning("No chat list loaded");
                }
            }
        }
    }

    public void onStatusChange(int position, long userHandle, int status) {
        logDebug("Position: " + position + ", User Handle: " + userHandle + " with new presence: " + status);

        adapterList.updateContactStatus(position, userHandle, status);
    }

    public void onTitleChange(int position) {
        logDebug("Position: " + position);
        adapterList.setTitle(position, null);
        interactionUpdate(position);
    }

    public void onUnreadCountChange(int position, boolean updateOrder) {
        logDebug("Position: " + position + ", Update order: " + updateOrder);
        adapterList.setPendingMessages(position, null);

        if (updateOrder) {
            interactionUpdate(position);
        }
    }

    public void onLastTsChange(int position, boolean updateOrder) {
        logDebug("Position: " + position + ", Update order: " + updateOrder);

        adapterList.setTs(position, null);

        if (updateOrder) {
            interactionUpdate(position);
        }
    }

    public void onLastMessageChange(int position) {
        logDebug("Position: " + position);

        adapterList.setLastMessage(position, null);

//        if(updateOrder){
//            interactionUpdate(position);
//        }
    }

    public void showMuteIcon(MegaChatListItem item) {
        logDebug("Chat ID: " + item.getChatId());

        long chatHandleToUpdate = item.getChatId();
        int indexToReplace = -1;
        ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
        while (itrReplace.hasNext()) {
            MegaChatListItem chat = itrReplace.next();
            if (chat != null) {
                if (chat.getChatId() == chatHandleToUpdate) {
                    indexToReplace = itrReplace.nextIndex() - 1;
                    break;
                }
            } else {
                break;
            }
        }
        if (indexToReplace != -1) {
            logDebug("Index to replace: " + indexToReplace);
            if (adapterList != null) {
                adapterList.showMuteIcon(indexToReplace);
            }
        }
    }

    public void refreshNode(MegaChatListItem item) {
        logDebug("Chat ID: " + item.getChatId());
        ChatUtil.showCallLayout(context, megaChatApi, callInProgressLayout, callInProgressChrono);

        //elements of adapter
        long chatHandleToUpdate = item.getChatId();
        int indexToUpdate = -1;
        if (chats != null) {
            ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
            while (itrReplace.hasNext()) {
                MegaChatListItem chat = itrReplace.next();
                if (chat != null) {
                    if (chat.getChatId() == chatHandleToUpdate) {
                        indexToUpdate = itrReplace.nextIndex() - 1;
                        break;
                    }
                } else {
                    break;
                }
            }
            if (indexToUpdate != -1) {
                logDebug("Index to replace: " + indexToUpdate);
                if (adapterList != null) {
                    adapterList.notifyItemChanged(indexToUpdate);
                }
            }
        }
    }

    public void interactionUpdate(int position) {
        logDebug("Position: " + position);
        MegaChatListItem chat = chats.remove(position);
        chats.add(0, chat);
        adapterList.notifyItemMoved(position, 0);
        if (lastFirstVisiblePosition == position) {
            logDebug("Interaction - change lastFirstVisiblePosition");
            lastFirstVisiblePosition = 0;
        }

        if (adapterList.isMultipleSelect()) {
            adapterList.updateMultiselectionPosition(position);
        }
    }

    public String getParticipantFullName(MegaChatRoom chat, long i) {
        String participantFirstName = chat.getPeerFirstname(i);
        String participantLastName = chat.getPeerLastname(i);

        if (participantFirstName == null) {
            participantFirstName = "";
        }
        if (participantLastName == null) {
            participantLastName = "";
        }

        if (participantFirstName.trim().length() <= 0) {
            return participantLastName;
        } else {
            return participantFirstName + " " + participantLastName;
        }
    }

    public void updateCacheForNonContacts(MegaChatRoom chatToCheck) {
        if (chatToCheck != null) {
            long peers = chatToCheck.getPeerCount();
            for (int i = 0; i < peers; i++) {
//                    long peerHandle = chatToCheck.getPeerHandle(i);
                String fullName = getParticipantFullName(chatToCheck, i);
                if (fullName != null) {
                    if (fullName.trim().length() <= 0) {
                        logDebug("Ask for name!");
                        ChatNonContactNameListener listener = new ChatNonContactNameListener(context);
                        megaChatApi.getUserFirstname(chatToCheck.getPeerHandle(i), chatToCheck.getAuthorizationToken(), listener);
                        megaChatApi.getUserLastname(chatToCheck.getPeerHandle(i), chatToCheck.getAuthorizationToken(), listener);
                    } else {
                        logDebug("Exists name!");
                    }
                } else {
                    logDebug("Ask for name!");
                    ChatNonContactNameListener listener = new ChatNonContactNameListener(context);
                    megaChatApi.getUserFirstname(chatToCheck.getPeerHandle(i), chatToCheck.getAuthorizationToken(), listener);
                    megaChatApi.getUserLastname(chatToCheck.getPeerHandle(i), chatToCheck.getAuthorizationToken(), listener);
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if (listView.getLayoutManager() != null) {
            outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, listView.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    public void onPause() {
        logDebug("onPause");
        lastFirstVisiblePosition = ((LinearLayoutManager) listView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        MegaApplication.setRecentChatVisible(false);
        super.onPause();
    }

    @Override
    public void onResume() {
        logDebug("onResume: lastFirstVisiblePosition " + lastFirstVisiblePosition);
        if (lastFirstVisiblePosition > 0) {
            (listView.getLayoutManager()).scrollToPosition(lastFirstVisiblePosition);
        } else {
            (listView.getLayoutManager()).scrollToPosition(0);
        }
        lastFirstVisiblePosition = 0;

        if (aB != null && aB.getTitle() != null) {
            aB.setTitle(adjustForLargeFont(aB.getTitle().toString()));
        }
        ChatUtil.showCallLayout(context, megaChatApi, callInProgressLayout, callInProgressChrono);

        if (context instanceof ManagerActivityLollipop) {
            String searchQuery = ((ManagerActivityLollipop) context).searchQuery;
            if (searchQuery != null && ((ManagerActivityLollipop) context).searchExpand) {
                filterChats(searchQuery);
            }
            ((ManagerActivityLollipop) context).invalidateOptionsMenu();
        }
        refreshMegaContactsList();
        super.onResume();
    }

    public void refreshMegaContactsList() {
        grantedContactPermission = Util.checkPermissionGranted(Manifest.permission.READ_CONTACTS, context);
        if (grantedContactPermission) {
            showPermissionGrantedView();
        } else {
            showPermissionDeniedView();
        }
    }

    public int getItemCount(){
        if(adapterList != null){
            return adapterList.getItemCount();
        }
        return 0;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        logDebug("onActivityCreated");
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable(BUNDLE_RECYCLER_LAYOUT);
            listView.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
        }
    }

    public void visibilityFastScroller() {
        if (chats == null) {
            fastScroller.setVisibility(View.GONE);
        } else {
            if (chats.size() < MIN_ITEMS_SCROLLBAR_CHAT) {
                fastScroller.setVisibility(View.GONE);
            } else {
                fastScroller.setVisibility(View.VISIBLE);
            }
        }
    }

    public void filterChats(String s) {
        if (adapterList != null && adapterList.isMultipleSelect()) {
            hideMultipleSelect();
        }

        if (filterChatsTask != null && filterChatsTask.getStatus() != AsyncTask.Status.FINISHED) {
            filterChatsTask.cancel(true);
        }
        filterChatsTask = new FilterChatsTask();
        filterChatsTask.execute(s);
    }

    public void closeSearch() {
        if (filterChatsTask != null && filterChatsTask.getStatus() != AsyncTask.Status.FINISHED) {
            filterChatsTask.cancel(true);
        }

        if (adapterList == null) {
            return;
        }

        adapterList.setChats(chats);

        if (adapterList.getItemCount() == 0 && emptyArchivedChats()) {
            logDebug("adapterList.getItemCount() == 0");
            listView.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        } else {
            logDebug("adapterList.getItemCount() NOT = 0");
            listView.setVisibility(View.VISIBLE);
            emptyLayout.setVisibility(View.GONE);

        }
    }

    public boolean checkPermissionsCall() {
        logDebug("checkPermissionsCall() ");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            boolean hasCameraPermission = (ContextCompat.checkSelfPermission(((ManagerActivityLollipop) context), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED);
            if (!hasCameraPermission) {
                ActivityCompat.requestPermissions(((ManagerActivityLollipop) context), new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA);
                return false;
            }

            boolean hasRecordAudioPermission = (ContextCompat.checkSelfPermission(((ManagerActivityLollipop) context), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED);
            if (!hasRecordAudioPermission) {
                ActivityCompat.requestPermissions(((ManagerActivityLollipop) context), new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO);
                return false;
            }

            return true;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        logDebug("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermissionsCall()) {
                        logDebug("REQUEST_CAMERA -> returnTheCall");
                        returnCall(context, megaChatApi);
                    }
                }
                break;
            }
            case RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermissionsCall()) {
                        logDebug("RECORD_AUDIO -> returnTheCall");
                        returnCall(context, megaChatApi);
                    }
                }
                break;
            }
            case Constants.REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    logDebug("REQUEST_READ_CONTACTS");
                    grantedContactPermission = true;
                } else {
                    logDebug("read contacts permission denied!");
                    showPermissionDeniedView();
                    grantedContactPermission = false;
                }
                break;
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == Constants.REQUEST_INVITE_CONTACT_FROM_DEVICE && resultCode == RESULT_OK) {
            logDebug("onActivityResult REQUEST_INVITE_CONTACT_FROM_DEVICE OK");
        }
        refreshMegaContactsList();
    }

    /////Multiselect/////
    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ArrayList<MegaChatListItem> chats = adapterList.getSelectedChats();

            if (megaChatApi.isSignalActivityRequired()) {
                megaChatApi.signalPresenceActivity();
            }

            switch (item.getItemId()) {
                case R.id.cab_menu_select_all: {
                    selectAll();
                    actionMode.invalidate();
                    break;
                }
                case R.id.cab_menu_unselect_all: {
                    clearSelections();
                    hideMultipleSelect();
                    actionMode.invalidate();
                    break;
                }
                case R.id.cab_menu_mute: {
                    clearSelections();
                    hideMultipleSelect();
                    ChatController chatC = new ChatController(context);
                    chatC.muteChats(chats);
//                    setChats();
                    break;
                }
                case R.id.cab_menu_unmute: {
                    clearSelections();
                    hideMultipleSelect();
                    ChatController chatC = new ChatController(context);
                    chatC.unmuteChats(chats);
//                    setChats();
                    break;
                }
                case R.id.cab_menu_archive:
                case R.id.cab_menu_unarchive: {
                    clearSelections();
                    hideMultipleSelect();
                    ChatController chatC = new ChatController(context);
                    chatC.archiveChats(chats);
                    break;
                }
                case R.id.cab_menu_delete: {
                    clearSelections();
                    hideMultipleSelect();
                    //Delete
                    Toast.makeText(context, "Not yet implemented! Delete: " + chats.size() + " chats", Toast.LENGTH_SHORT).show();
                    break;
                }
                case R.id.chat_list_leave_chat_layout: {
                    //Leave group chat
                    ((ManagerActivityLollipop) context).showConfirmationLeaveChats(chats);
                    clearSelections();
                    hideMultipleSelect();
                    break;
                }
            }
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.recent_chat_action, menu);
            adapterList.setMultipleSelect(true);
            if (context instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop) context).hideFabButton();
                ((ManagerActivityLollipop) context).showHideBottomNavigationView(true);
                ((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ACCENT);
                checkScroll();
            } else if (context instanceof ArchivedChatsActivity) {
                ((ArchivedChatsActivity) context).changeStatusBarColor(1);
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            clearSelections();
            adapterList.setMultipleSelect(false);
            if (context instanceof ManagerActivityLollipop) {
                ((ManagerActivityLollipop) context).showFabButton();
                ((ManagerActivityLollipop) context).showHideBottomNavigationView(false);
                ((ManagerActivityLollipop) context).changeStatusBarColor(COLOR_STATUS_BAR_ZERO_DELAY);
                checkScroll();
            } else if (context instanceof ArchivedChatsActivity) {
                ((ArchivedChatsActivity) context).changeStatusBarColor(0);
            }
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            List<MegaChatListItem> selected = adapterList.getSelectedChats();
            boolean showMute = false;
            boolean showUnmute = false;
            boolean showLeaveChat = false;

            if (context instanceof ManagerActivityLollipop) {
                if (selected.size() != 0) {

                    menu.findItem(R.id.cab_menu_archive).setVisible(false);
                    menu.findItem(R.id.cab_menu_delete).setVisible(false);

                    menu.findItem(R.id.cab_menu_archive).setVisible(true);
                    menu.findItem(R.id.cab_menu_unarchive).setVisible(false);

                    MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);

                    if (!emptyArchivedChats() && selected.size() == adapterList.getItemCount() - 1) {
                        menu.findItem(R.id.cab_menu_select_all).setVisible(false);
                        unselect.setTitle(getString(R.string.action_unselect_all));
                        unselect.setVisible(true);
                    } else if (selected.size() == adapterList.getItemCount()) {
                        menu.findItem(R.id.cab_menu_select_all).setVisible(false);
                        unselect.setTitle(getString(R.string.action_unselect_all));
                        unselect.setVisible(true);
                    } else {
                        menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                        unselect.setTitle(getString(R.string.action_unselect_all));
                        unselect.setVisible(true);
                    }

                    for (int i = 0; i < selected.size(); i++) {
                        MegaChatListItem chat = selected.get(i);
                        if (chat != null) {
                            if (chat.isGroup() && (chat.getOwnPrivilege() == MegaChatRoom.PRIV_RO || chat.getOwnPrivilege() == MegaChatRoom.PRIV_STANDARD
                                    || chat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR)) {
                                logDebug("Chat Group permissions: " + chat.getOwnPrivilege());
                                showLeaveChat = true;
                            } else {
                                showLeaveChat = false;
                                break;
                            }
                        }
                    }

                    for (int i = 0; i < selected.size(); i++) {
                        MegaChatListItem chat = selected.get(i);
                        if (chat != null) {

                            String chatHandle = String.valueOf(chat.getChatId());
                            if (dbH.areNotificationsEnabled(chatHandle)) {
                                logDebug("Chat UNMUTED");
                                showUnmute = true;
                                break;
                            }
                        }
                    }

                    for (int i = 0; i < selected.size(); i++) {
                        MegaChatListItem chat = selected.get(i);
                        if (chat != null) {

                            String chatHandle = String.valueOf(chat.getChatId());
                            if (!(dbH.areNotificationsEnabled(chatHandle))) {
                                logDebug("Chat MUTED");
                                showMute = true;
                                break;
                            }
                        }
                    }

                    menu.findItem(R.id.cab_menu_mute).setVisible(showUnmute);
                    menu.findItem(R.id.cab_menu_unmute).setVisible(showMute);
                    menu.findItem(R.id.chat_list_leave_chat_layout).setVisible(showLeaveChat);
                    if (showLeaveChat) {
                        menu.findItem(R.id.chat_list_leave_chat_layout).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                    }

                } else {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                    menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
                    menu.findItem(R.id.cab_menu_mute).setVisible(false);
                    menu.findItem(R.id.cab_menu_unmute).setVisible(false);

                    menu.findItem(R.id.chat_list_leave_chat_layout).setVisible(false);

                }
            } else if (context instanceof ArchivedChatsActivity) {
                menu.findItem(R.id.cab_menu_delete).setVisible(false);
                menu.findItem(R.id.cab_menu_mute).setVisible(showUnmute);
                menu.findItem(R.id.cab_menu_unmute).setVisible(showMute);
                menu.findItem(R.id.chat_list_leave_chat_layout).setVisible(showLeaveChat);

                if (selected.size() != 0) {
                    MenuItem unselect = menu.findItem(R.id.cab_menu_unselect_all);
                    if (selected.size() == adapterList.getItemCount()) {
                        menu.findItem(R.id.cab_menu_select_all).setVisible(false);
                        unselect.setTitle(getString(R.string.action_unselect_all));
                        unselect.setVisible(true);
                    } else {
                        menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                        unselect.setTitle(getString(R.string.action_unselect_all));
                        unselect.setVisible(true);
                    }

                    menu.findItem(R.id.cab_menu_unarchive).setVisible(true);
                    menu.findItem(R.id.cab_menu_archive).setVisible(false);
                } else {
                    menu.findItem(R.id.cab_menu_select_all).setVisible(true);
                    menu.findItem(R.id.cab_menu_unselect_all).setVisible(false);
                }
            }

            return false;
        }
    }

    private void loadMegaContacts() {
        contactGetter.getMegaContacts(megaApi, MegaContactGetter.DAY);
    }

    class FilterChatsTask extends AsyncTask<String, Void, Void> {

        ArrayList<MegaChatListItem> filteredChats;

        @Override
        protected Void doInBackground(String... strings) {
            if (chats != null && !chats.isEmpty()) {
                if (filteredChats == null) {
                    filteredChats = new ArrayList<>();
                } else {
                    filteredChats.clear();
                }
                for (MegaChatListItem chat : chats) {
                    if (chat.getTitle().toLowerCase().contains(strings[0].toLowerCase())) {
                        filteredChats.add(chat);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (adapterList == null) {
                return;
            }
            adapterList.setChats(filteredChats);

            if (adapterList.getItemCount() == 0) {
                logDebug("adapterList.getItemCount() == 0");
                listView.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.VISIBLE);
                inviteButton.setVisibility(View.GONE);
                String textToShow = String.format(context.getString(R.string.recent_chat_empty));

                try {
                    textToShow = textToShow.replace("[A]", "<font color=" + COLOR_START + ">");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=" + COLOR_END + ">");
                    textToShow = textToShow.replace("[/B]", "</font>");
                } catch (Exception e) {
                }
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow, Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }

                emptyTextViewInvite.setText(result);
                emptyTextViewInvite.setVisibility(View.VISIBLE);
            } else {
                logDebug("adapterList.getItemCount() NOT = 0");
                listView.setVisibility(View.VISIBLE);
                emptyLayout.setVisibility(View.GONE);
            }
        }
    }
}