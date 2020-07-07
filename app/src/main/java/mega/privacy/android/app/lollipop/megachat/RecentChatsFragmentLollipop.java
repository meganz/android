package mega.privacy.android.app.lollipop.megachat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import mega.privacy.android.app.lollipop.AddContactActivityLollipop;
import mega.privacy.android.app.lollipop.InviteContactActivity;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.ContactsHorizontalAdapter;
import mega.privacy.android.app.lollipop.adapters.RotatableAdapter;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.lollipop.managerSections.RotatableFragment;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.utils.AskForDisplayOverDialog;
import mega.privacy.android.app.utils.PermissionUtils;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;

import static android.app.Activity.RESULT_OK;
import static mega.privacy.android.app.lollipop.AddContactActivityLollipop.FROM_RECENT;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ContactUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.PermissionUtils.*;
import static mega.privacy.android.app.utils.Util.*;

public class RecentChatsFragmentLollipop extends RotatableFragment implements View.OnClickListener, MegaContactGetter.MegaContactUpdater {

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";
    private static final String COLOR_START = "\'#000000\'";
    private static final String COLOR_END = "\'#7a7a7a\'";

    /** DURATION is the time duration of snack bar display, deisgned by designer
     *  MAX_LINES is the max line setting of the snack bar */
    public static final int DURATION = 4000;
    public static final int MAX_LINES = 3;

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
    private View bannerContainer;
    private ImageView collapseBtn;
    private ImageView closeBtn;
    private TextView inviteTitle;
    private FrameLayout invitationContainer;
    private RelativeLayout requestPermissionLayout;
    private Button dismissBtn;
    private Button allowBtn;
    private RelativeLayout contactsListLayout;
    private RecyclerView contactsList;
    private TextView moreContactsTitle;
    private TextView actionBarTitle, actionBarSubtitle;
    private View bannerDivider;

    private AppBarLayout appBarLayout;

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

    Button inviteButton;
    int chatStatus;

    float density;
    DisplayMetrics outMetrics;
    Display display;

    private ActionMode actionMode;

    private boolean isExplanationDialogShowing;
    private AlertDialog explanationDialog;
    private static final String KEY_DIALOG_IS_SHOWING = "dialog_is_showing";

    private AskForDisplayOverDialog askForDisplayOverDialog;

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
    public void reselectUnHandledSingleItem(int position) {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logDebug("onCreate");

        if (megaApi == null) {
            megaApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());

        if (megaChatApi == null) {
            megaChatApi = ((MegaApplication) ((Activity) context).getApplication()).getMegaChatApi();
        }

        grantedContactPermission = hasPermissions(context, Manifest.permission.READ_CONTACTS);
        contactGetter = new MegaContactGetter(context);
        contactGetter.setMegaContactUpdater(this);

        askForDisplayOverDialog = new AskForDisplayOverDialog(context);
    }

    @Override
    public void onFinish(List<MegaContactGetter.MegaContact> megaContacts) {
        if (!isAdded()) {
            return;
        }
        if (megaContacts.size() > 0) {
            logDebug("get " + megaContacts.size() + " matched contacts.");
            // change the settings, when have new matched contact.
            dbH.setShowInviteBanner("true");

            // At the end of the contacts list, add the 'Invite more' element, it's an empty MegaContact object.
            megaContacts.add(new MegaContactGetter.MegaContact());
            onContactsCountChange(megaContacts);
            expandContainer();
            bannerContainer.setVisibility(View.VISIBLE);
            requestPermissionLayout.setVisibility(View.GONE);
            contactsListLayout.setVisibility(View.VISIBLE);
            collapseBtn.setVisibility(View.VISIBLE);
            closeBtn.setVisibility(View.GONE);
            inviteTitle.setClickable(true);
            moreContactsTitle.setVisibility(View.GONE);

            adapter = new ContactsHorizontalAdapter((Activity) context, this, megaContacts);
            contactsList.setLayoutManager(new LinearLayoutManager(getContext(),  LinearLayoutManager.HORIZONTAL, false));
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
        logWarning(requestString + " failed, with error code: " + errorCode);
        noContacts();
    }

    @Override
    public void noContacts() {
        if(showInviteBanner()) {
            bannerContainer.setVisibility(View.VISIBLE);
            invitationContainer.setVisibility(View.GONE);
            inviteTitle.setText(R.string.no_local_contacts_on_mega);
            inviteTitle.setClickable(true);
            collapseBtn.setVisibility(View.INVISIBLE);
            closeBtn.setVisibility(View.VISIBLE);
            moreContactsTitle.setVisibility(View.VISIBLE);
        } else {
            bannerContainer.setVisibility(View.GONE);
            bannerDivider.setVisibility(View.GONE);
        }
    }

    public void onContactsCountChange(List<MegaContactGetter.MegaContact> megaContacts) {
        // The last element is the 'Invite more', don't include in the contacts count.
        int count = megaContacts.size() - 1;
        if (count > 0) {
            String title = context.getResources().getQuantityString(R.plurals.quantity_of_local_contact, count, count);
            inviteTitle.setText(title);
        } else {
            noContacts();
        }
    }

    public void checkScroll() {
        if (listView != null) {
            if (context instanceof ManagerActivityLollipop) {
                if(bannerContainer.getVisibility() == View.GONE) {
                    bannerDivider.setVisibility(View.GONE);
                    if (listView.canScrollVertically(-1) || (adapterList != null && adapterList.isMultipleSelect())) {
                        ((ManagerActivityLollipop) context).changeActionBarElevation(true);
                    } else {
                        ((ManagerActivityLollipop) context).changeActionBarElevation(false);
                    }
                } else {
                    ((ManagerActivityLollipop) context).changeActionBarElevation(false);
                    if (listView.canScrollVertically(-1) || (adapterList != null && adapterList.isMultipleSelect())) {
                        bannerDivider.setVisibility(View.GONE);
                        appBarLayout.setElevation(px2dp(4, outMetrics));
                    } else {
                        bannerDivider.setVisibility(View.VISIBLE);
                        appBarLayout.setElevation(0);
                    }
                }
            } else if (context instanceof ArchivedChatsActivity) {
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
        appBarLayout = v.findViewById(R.id.linear_layout_add);
        if(context instanceof ArchivedChatsActivity) {
            appBarLayout.setVisibility(View.GONE);
        } else {
            aB = ((AppCompatActivity) context).getSupportActionBar();
        }
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
        emptyTextViewInvite.setWidth(scaleWidthPx(236, outMetrics));
        emptyTextView = v.findViewById(R.id.empty_text_chat_recent);
        emptyImageView = v.findViewById(R.id.empty_image_view_recent);
        emptyImageView.setOnClickListener(this);

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            adjustLandscape();
            emptyImageView.setVisibility(View.GONE);
        } else {
            addMarginTop();
            emptyImageView.setImageResource(R.drawable.ic_empty_chat_list);
        }

        inviteButton = (Button) v.findViewById(R.id.invite_button);
        inviteButton.setOnClickListener(this);

        mainRelativeLayout = (RelativeLayout) v.findViewById(R.id.main_relative_layout);
        //auto scroll to bottom to show invite button.
        if(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            adjustLandscape();
        } else {
            addMarginTop();
        }

        if (context instanceof ManagerActivityLollipop) {
            setStatus();
            if (!emptyArchivedChats()) {
                listView.setPadding(0, scaleHeightPx(8, outMetrics), 0, scaleHeightPx(16, outMetrics));
            } else {
                listView.setPadding(0, scaleHeightPx(8, outMetrics), 0, scaleHeightPx(78, outMetrics));
            }
        } else {
            //Archived chats section
            listView.setPadding(0, scaleHeightPx(8, outMetrics), 0, 0);
        }

        this.setChats();

        if (megaChatApi.isSignalActivityRequired()) {
            megaChatApi.signalPresenceActivity();
        }
        setCustomisedActionBar();

        //Invitation bar
        bannerContainer = v.findViewById(R.id.invite_banner_container);
        collapseBtn = v.findViewById(R.id.collapse_btn);
        collapseBtn.setOnClickListener(this);
        closeBtn = v.findViewById(R.id.close_btn);
        closeBtn.setOnClickListener(this);
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
        moreContactsTitle = v.findViewById(R.id.more_contacts_title);
        bannerDivider = v.findViewById(R.id.invitation_banner_divider);
        moreContactsTitle.setOnClickListener(this);
        if(showInviteBanner()) {
            bannerContainer.setVisibility(View.VISIBLE);
        } else {
            bannerContainer.setVisibility(View.GONE);
        }
        if(askForDisplayOverDialog != null) {
            askForDisplayOverDialog.showDialog();
        }
        return v;
    }

    private boolean showInviteBanner() {
        String showInviteBannerString = dbH.getPreferences().getShowInviteBanner();
        if(!TextUtils.isEmpty(showInviteBannerString)) {
            return Boolean.parseBoolean(showInviteBannerString);
        } else {
            return true;
        }
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
        closeBtn.setVisibility(View.GONE);
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
                            ((ManagerActivityLollipop) context).showFabButton();
                        } else {
                            showNoConnectionScreen();
                        }
                    } else {
                        logDebug("Chats size: " + chats.size());

                        //Order by last interaction
                        sortChats(chats);

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

                        ((ManagerActivityLollipop) context).showFabButton();
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
                    sortChats(chats);

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

                    ((ManagerActivityLollipop) context).showFabButton();
                }
            } else {
                logDebug("Show chat screen connecting...");
                showConnectingChatScreen();
            }
        }
    }

    private void sortChats(ArrayList<MegaChatListItem> chatsToSort) {
        Collections.sort(chatsToSort, new Comparator<MegaChatListItem>() {

            public int compare(MegaChatListItem c1, MegaChatListItem c2) {
                long timestamp1 = c1.getLastTimestamp();
                long timestamp2 = c2.getLastTimestamp();

                long result = timestamp2 - timestamp1;
                return (int) result;
            }
        });
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
            emptyTextViewInvite.setText(getString(R.string.recent_chat_empty_text));
            emptyTextViewInvite.setVisibility(View.VISIBLE);
            inviteButton.setText(getString(R.string.new_chat_link_label));
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

    private void addMarginTop() {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        layoutParams.setMargins(0, scaleHeightPx(60, outMetrics), 0, 0);
        emptyLayoutContainer.setLayoutParams(layoutParams);
    }

    public void showConnectingChatScreen(){
        logDebug("showConnectingChatScreen");

        listView.setVisibility(View.GONE);
        if (context instanceof ManagerActivityLollipop) {
            ((ManagerActivityLollipop) context).hideFabButton();
        }

        emptyTextViewInvite.setText(getString(R.string.recent_chat_empty_text));
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
        emptyLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        switch (v.getId()) {
            case R.id.invite_button: {
                if (isOnline(context)) {
                    if (context instanceof ManagerActivityLollipop) {
                        Intent in = new Intent(context, AddContactActivityLollipop.class);
                        in.putExtra("contactType", CONTACT_TYPE_MEGA);
                        in.putExtra(FROM_RECENT, true);
                        ((ManagerActivityLollipop) context).startActivityForResult(in, REQUEST_CREATE_CHAT);
                    }
                    if (megaChatApi.isSignalActivityRequired()) {
                        megaChatApi.signalPresenceActivity();
                    }
                } else {
                    ((ManagerActivityLollipop) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
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
                    returnCall(context);
                }
                break;
            }
            case R.id.invite_title:
            case R.id.dismiss_button:
            case R.id.collapse_btn:
                if(moreContactsTitle.getVisibility() == View.VISIBLE) {
                    startActivityForResult(new Intent(context, InviteContactActivity.class), REQUEST_INVITE_CONTACT_FROM_DEVICE);
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
            case R.id.close_btn:
                dbH.setShowInviteBanner("false");
                bannerContainer.setVisibility(View.GONE);
                break;
            case R.id.allow_button:
                logDebug("request contact permission!");
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
                break;
            case R.id.more_contacts_title:
                logDebug("to InviteContactActivity");
                startActivityForResult(new Intent(context, InviteContactActivity.class), REQUEST_INVITE_CONTACT_FROM_DEVICE);
                break;
        }
    }

    public void showStateInfo() {

        StringBuilder builder = new StringBuilder();

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
                if (((ManagerActivityLollipop) context).getSearchQuery() != null && !((ManagerActivityLollipop) context).getSearchQuery().isEmpty()) {
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
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
                    onUnreadCountChange(indexToReplace, item.getUnreadCount() == 0);
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
        chatStatus = megaChatApi.getOnlineStatus();
        logDebug("Chat status --> getOnlineStatus with megaChatApi: " + chatStatus);
        onlineStatusUpdate(chatStatus);
    }

    public void onlineStatusUpdate(int status) {
        logDebug("Status: " + status);

        chatStatus = status;
        int initStatus = megaChatApi.getInitState();
        if (isAdded()) {
            if (aB != null) {
                switch (status) {
                    case MegaChatApi.STATUS_ONLINE: {
                        setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.online_status)));
                        break;
                    }
                    case MegaChatApi.STATUS_AWAY: {
                        setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.away_status)));
                        break;
                    }
                    case MegaChatApi.STATUS_BUSY: {
                        setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.busy_status)));
                        break;
                    }
                    case MegaChatApi.STATUS_OFFLINE: {
                        setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.offline_status)));
                        break;
                    }
                    case MegaChatApi.STATUS_INVALID: {
                        if (!isOnline(context)) {
                            setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.error_server_connection_problem)));
                        } else {
                            if (megaChatApi == null) {
                                setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.invalid_connection_state)));
                            } else if (megaChatApi.getConnectionState() == MegaChatApi.CONNECTING) {
                                setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.chat_connecting)));
                            } else if (megaChatApi.getConnectionState() == MegaChatApi.DISCONNECTED) {
                                setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.invalid_connection_state)));
                            } else {
                                if (initStatus == MegaChatApi.INIT_WAITING_NEW_SESSION || initStatus == MegaChatApi.INIT_NO_CACHE) {
                                    setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.chat_connecting)));
                                }
                            }
                        }
                        break;
                    }
                    default: {

                        if (!isOnline(context) || megaApi == null || megaApi.getRootNode() == null) {
                            setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.error_server_connection_problem)));
                        } else {
                            if (megaChatApi == null) {
                                setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.invalid_connection_state)));
                            } else if (megaChatApi.getConnectionState() == MegaChatApi.CONNECTING) {
                                setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.chat_connecting)));
                            } else if (megaChatApi.getConnectionState() == MegaChatApi.DISCONNECTED) {
                                setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.invalid_connection_state)));
                            } else {
                                if (initStatus == MegaChatApi.INIT_WAITING_NEW_SESSION || initStatus == MegaChatApi.INIT_NO_CACHE) {
                                    setCustomisedActionBarSubtitle(adjustForLargeFont(getString(R.string.chat_connecting)));
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

        String nickname = getNicknameContact(chat.getPeerHandle(i));
        if(nickname != null) return nickname;

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
        outState.putBoolean(KEY_DIALOG_IS_SHOWING, isExplanationDialogShowing);
    }

    @Override
    public void onPause() {
        logDebug("onPause");
        lastFirstVisiblePosition = ((LinearLayoutManager) listView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        MegaApplication.setRecentChatVisible(false);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(askForDisplayOverDialog != null) {
            askForDisplayOverDialog.recycle();
        }
        if(explanationDialog != null) {
            explanationDialog.cancel();
        }
        if(adapter != null) {
            adapter.dismissDialog();
        }
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
        if(aB == null) {
            aB = ((AppCompatActivity) context).getSupportActionBar();

        }
        if (aB != null && aB.getTitle() != null) {
            aB.setTitle(adjustForLargeFont(aB.getTitle().toString()));
        }

        if (context instanceof ManagerActivityLollipop) {
            if (((ManagerActivityLollipop) context).isSearchOpen()) {
                filterChats(((ManagerActivityLollipop) context).getSearchQuery());
            }
            ((ManagerActivityLollipop) context).invalidateOptionsMenu();
        }
        // if in ArchivedChatsActivity or user close the invitation banner, no need to load contacts.
        if(appBarLayout.getVisibility() != View.GONE) {
            refreshMegaContactsList();
        }
        setStatus();
        super.onResume();
    }

    public void refreshMegaContactsList() {
        grantedContactPermission = hasPermissions(context, Manifest.permission.READ_CONTACTS);
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
            isExplanationDialogShowing = savedInstanceState.getBoolean(KEY_DIALOG_IS_SHOWING);
            if(isExplanationDialogShowing) {
                showExplanationDialog();
            }
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
                        returnCall(context);
                    }
                }
                break;
            }
            case RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermissionsCall()) {
                        logDebug("RECORD_AUDIO -> returnTheCall");
                        returnCall(context);
                    }
                }
                break;
            }
            case REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    logDebug("REQUEST_READ_CONTACTS");
                    grantedContactPermission = true;
                } else {
                    logDebug("read contacts permission denied!");
                    showPermissionDeniedView();
                    grantedContactPermission = false;
                    boolean should = PermissionUtils.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_CONTACTS);
                    if (should) {
                        showExplanationDialog();
                    } else {
                        // the system request permission dialog can no longer show.
                        Snackbar snackbar = Snackbar.make(bannerContainer, getString(R.string.on_permanently_denied), Snackbar.LENGTH_LONG)
                                .setAction(getString(R.string.action_settings), PermissionUtils.toAppInfo(getContext()))
                                .setDuration(DURATION);
                        TextView snackbarTextView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
                        snackbarTextView.setMaxLines(MAX_LINES);
                        snackbar.show();
                    }
                }
                break;
            }
        }
    }

    private void showExplanationDialog() {
        isExplanationDialogShowing = true;
        explanationDialog = showAlert(getContext(), getString(R.string.explanation_for_contacts_permission), null, dialog -> isExplanationDialogShowing = false);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_INVITE_CONTACT_FROM_DEVICE && resultCode == RESULT_OK) {
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
        int archivedSize = 0;

        @Override
        protected Void doInBackground(String... strings) {
            ArrayList<MegaChatListItem> chatsToSearch = new ArrayList<>();
            chatsToSearch.addAll(chats);

            if (context instanceof ManagerActivityLollipop) {
                ArrayList<MegaChatListItem> archivedChats = megaChatApi.getArchivedChatListItems();
                if (archivedChats != null && !archivedChats.isEmpty()) {
                    archivedSize = archivedChats.size();
                    sortChats(archivedChats);
                    chatsToSearch.addAll(archivedChats);
                }
            }

            if (!chatsToSearch.isEmpty()) {
                if (filteredChats == null) {
                    filteredChats = new ArrayList<>();
                } else {
                    filteredChats.clear();
                }
                for (MegaChatListItem chat : chatsToSearch) {
                    if (getTitleChat(chat).toLowerCase().contains(strings[0].toLowerCase())) {
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

            if (shouldShowEmptyState()) {
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

        private boolean shouldShowEmptyState() {
            if (context instanceof ManagerActivityLollipop) {
                if ((adapterList.getItemCount() == 0 && archivedSize == 0) || (adapterList.getItemCount() == 1 && archivedSize > 0))
                    return true;
            } else if (context instanceof ArchivedChatsActivity) {
                if (adapterList.getItemCount() == 0)
                    return true;
            }
            return false;
        }
    }

    public void setCustomisedActionBar() {
        if (aB != null) {
            aB.setDisplayShowCustomEnabled(true);
            aB.setDisplayShowTitleEnabled(false);

            aB.setCustomView(R.layout.chat_action_bar);
            View v = aB.getCustomView();
            actionBarTitle = v.findViewById(R.id.ab_title);
            setCustomisedActionBarTitle(adjustForLargeFont(getString(R.string.section_chat).toUpperCase()));
            actionBarSubtitle = v.findViewById(R.id.ab_subtitle);
            setStatus();
            v.findViewById(R.id.ab_subtitle_container).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (context != null && context instanceof ManagerActivityLollipop) {
                        ((ManagerActivityLollipop) context).showPresenceStatusDialog();
                    }
                }
            });
        }
    }

    private void setCustomisedActionBarTitle(SpannableString title){
        if(actionBarTitle != null){
            actionBarTitle.setText(title);
        }
    }

    private void setCustomisedActionBarSubtitle(SpannableString subtitle){
        if(actionBarSubtitle != null){
            actionBarSubtitle.setText(subtitle);
        }
    }
}