package mega.privacy.android.app.main.megachat;

import static android.app.Activity.RESULT_OK;
import static mega.privacy.android.app.constants.EventConstants.EVENT_RINGING_STATUS_CHANGE;
import static mega.privacy.android.app.main.AddContactActivity.FROM_RECENT;
import static mega.privacy.android.app.utils.CallUtil.hintShown;
import static mega.privacy.android.app.utils.CallUtil.returnActiveCall;
import static mega.privacy.android.app.utils.CallUtil.shouldShowMeetingHint;
import static mega.privacy.android.app.utils.ChatUtil.createMuteNotificationsChatAlertDialog;
import static mega.privacy.android.app.utils.ChatUtil.getPositionFromChatId;
import static mega.privacy.android.app.utils.ChatUtil.isEnableChatNotifications;
import static mega.privacy.android.app.utils.ChatUtil.shouldMuteOrUnmuteOptionsBeShown;
import static mega.privacy.android.app.utils.ChatUtil.showConfirmationLeaveChats;
import static mega.privacy.android.app.utils.Constants.ACTION_CHAT_SHOW_MESSAGES;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.CONTACT_TYPE_MEGA;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.Constants.KEY_HINT_IS_SHOWING;
import static mega.privacy.android.app.utils.Constants.MIN_ITEMS_SCROLLBAR_CHAT;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_ENABLED;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA;
import static mega.privacy.android.app.utils.Constants.REQUEST_CREATE_CHAT;
import static mega.privacy.android.app.utils.Constants.REQUEST_INVITE_CONTACT_FROM_DEVICE;
import static mega.privacy.android.app.utils.Constants.REQUEST_READ_CONTACTS;
import static mega.privacy.android.app.utils.Constants.REQUEST_RECORD_AUDIO;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logWarning;
import static mega.privacy.android.app.utils.Util.adjustForLargeFont;
import static mega.privacy.android.app.utils.Util.isDarkMode;
import static mega.privacy.android.app.utils.Util.isOnline;
import static mega.privacy.android.app.utils.Util.noChangeRecyclerViewItemAnimator;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static mega.privacy.android.app.utils.Util.scaleWidthPx;
import static mega.privacy.android.app.utils.Util.showAlert;
import static mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions;
import static mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
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
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;
import com.jeremyliao.liveeventbus.LiveEventBus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.ChatDividerItemDecoration;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.main.AddContactActivity;
import mega.privacy.android.app.main.DrawerItem;
import mega.privacy.android.app.main.InviteContactActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.adapters.ContactsHorizontalAdapter;
import mega.privacy.android.app.main.adapters.RotatableAdapter;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.listeners.ChatNonContactNameListener;
import mega.privacy.android.app.main.managerSections.RotatableFragment;
import mega.privacy.android.app.main.megachat.chatAdapters.MegaListChatAdapter;
import mega.privacy.android.app.objects.PasscodeManagement;
import mega.privacy.android.app.usecase.chat.SearchChatsUseCase;
import mega.privacy.android.app.utils.AskForDisplayOverDialog;
import mega.privacy.android.app.utils.HighLightHintHelper;
import mega.privacy.android.app.utils.StringResourcesUtils;
import mega.privacy.android.app.utils.TextUtil;
import mega.privacy.android.app.utils.permission.PermissionUtils;
import mega.privacy.android.app.utils.TimeUtils;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.contacts.MegaContactGetter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;

import static mega.privacy.android.app.utils.Util.*;

@AndroidEntryPoint
public class RecentChatsFragment extends RotatableFragment implements View.OnClickListener, MegaContactGetter.MegaContactUpdater {

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

    /** DURATION is the time duration of snack bar display
     *  MAX_LINES is the max line setting of the snack bar */
    public static final int DURATION = 4000;
    public static final int MAX_LINES = 3;

    @Inject
    PasscodeManagement passcodeManagement;

    @Inject
    SearchChatsUseCase searchChatsUseCase;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    DatabaseHandler dbH;

    Context context;
    ActionBar aB;
    MegaListChatAdapter adapterList;
    RelativeLayout mainRelativeLayout;

    RecyclerView listView;
    LinearLayoutManager mLayoutManager;
    FastScroller fastScroller;

    ArrayList<MegaChatListItem> chats = new ArrayList<>();

    CompositeDisposable filterDisposable = new CompositeDisposable();

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
    private ImageView actionBarSubtitleArrow;

    private AppBarLayout appBarLayout;

    private static boolean isExpand;
    private static boolean isFirstTime = true;

    private boolean grantedContactPermission;

    private MegaContactGetter contactGetter;

    private ContactsHorizontalAdapter adapter;

    //Empty screen
    private TextView emptyTextView;
    private LinearLayout emptyLayout;
    private TextView emptyTextViewInvite;
    private TextView emptyDescriptionText;
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

    private static final String SP_KEY_IS_HINT_SHOWN_RECENT_CHATS = "is_hint_shown_recent_chats";
    private boolean isHintShowing;

    private HighLightHintHelper highLightHintHelper;

    private final Observer<MegaChatCall> callRingingStatusObserver = call -> {
        if (megaChatApi.getNumCalls() == 0 || adapter == null) {
            logError("Calls not found");
            return;
        }

        int position = getPositionFromChatId(chats, call.getChatid());
        if (call.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT &&
                call.isRinging() && position != INVALID_POSITION) {
            adapterList.setLastMessage(position, null);
        }
    };

    public static RecentChatsFragment newInstance() {
        logDebug("newInstance");
        RecentChatsFragment fragment = new RecentChatsFragment();
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
            bannerContainer.setVisibility(View.GONE);
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

        highLightHintHelper = new HighLightHintHelper(requireActivity());
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
        checkScroll();
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
            contactsListLayout.setVisibility(View.GONE);
        } else {
            bannerContainer.setVisibility(View.GONE);
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
        if (listView == null) {
            return;
        }

        if (context instanceof ManagerActivity) {
            if(bannerContainer.getVisibility() == View.GONE) {
                ((ManagerActivity) context).changeAppBarElevation(
                        listView.canScrollVertically(-1)
                                || (adapterList != null && adapterList.isMultipleSelect()));
            } else if (listView.canScrollVertically(-1)
                    || (adapterList != null && adapterList.isMultipleSelect())
                    || contactsListLayout.getVisibility() == View.VISIBLE) {
                appBarLayout.setElevation(getResources().getDimension(R.dimen.toolbar_elevation));

                ((ManagerActivity) context).changeAppBarElevation(isDarkMode(context));
            } else {
                appBarLayout.setElevation(0);
                // Reset the AppBar elevation whatever in the light and dark mode
                ((ManagerActivity) context).changeAppBarElevation(false);
            }
        } else if (context instanceof ArchivedChatsActivity) {
            boolean withElevation = listView.canScrollVertically(-1) || (adapterList != null && adapterList.isMultipleSelect());
            Util.changeActionBarElevation((ArchivedChatsActivity) context, ((ArchivedChatsActivity) context).findViewById(R.id.app_bar_layout_chat_explorer), withElevation);
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
        listView.setItemAnimator(noChangeRecyclerViewItemAnimator());
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkScroll();
            }
        });

        emptyLayout = v.findViewById(R.id.linear_empty_layout_chat_recent);
        emptyDescriptionText = v.findViewById(R.id.empty_description_text_recent);
        emptyDescriptionText.setText(TextUtil.formatEmptyScreenText(context, StringResourcesUtils.getString(R.string.context_empty_chat_recent)));

        emptyTextViewInvite = v.findViewById(R.id.empty_text_chat_recent_invite);
        emptyTextView = v.findViewById(R.id.empty_text_chat_recent);
        emptyImageView = v.findViewById(R.id.empty_image_view_recent);
        emptyImageView.setOnClickListener(this);

        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            adjustLandscape();
            emptyImageView.setVisibility(View.GONE);
        } else {
            addMarginTop();
            emptyImageView.setImageResource(R.drawable.empty_chat_message_portrait);
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

        if (context instanceof ManagerActivity) {
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
        moreContactsTitle.setOnClickListener(this);
        if(showInviteBanner()) {
            bannerContainer.setVisibility(View.VISIBLE);
        } else {
            bannerContainer.setVisibility(View.GONE);
        }

        LiveEventBus.get(EVENT_RINGING_STATUS_CHANGE, MegaChatCall.class).observe(this, callRingingStatusObserver);

        // Workaround: wait for R.id.action_menu_open_meeting initialized.
        new Handler().postDelayed(() -> {
            if (shouldShowMeetingHint(context, SP_KEY_IS_HINT_SHOWN_RECENT_CHATS) || isHintShowing) {
                highLightHintHelper.showHintForMeetingIcon(R.id.action_menu_open_meeting, () -> {
                    hintShown(context, SP_KEY_IS_HINT_SHOWN_RECENT_CHATS);
                    highLightHintHelper.dismissPopupWindow();
                    isHintShowing = false;
                    askForDisplayOver();
                    return null;
                });

                isHintShowing = true;
            } else {
                askForDisplayOver();
            }
        }, 300);

        return v;
    }

    private void askForDisplayOver() {
        if (askForDisplayOverDialog != null) {
            askForDisplayOverDialog.showDialog();
        }
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
        requestPermissionLayout.setVisibility(View.VISIBLE);
    }

    public void setChats() {
        logDebug("setChats");

        if (listView == null) {
            logWarning("listView is null - do not update");
            return;
        }

        if (isAdded()) {
            int initState = megaChatApi.getInitState();
            logDebug("Init state is: " + initState);

            if (initState == MegaChatApi.INIT_ONLINE_SESSION || initState == MegaChatApi.INIT_OFFLINE_SESSION) {
                if (chats != null) {
                    chats.clear();
                } else {
                    chats = new ArrayList<>();
                }

                if (context instanceof ManagerActivity) {
                    chats = megaChatApi.getChatListItems();
                } else {
                    chats = megaChatApi.getArchivedChatListItems();
                }

                if ((chats == null || chats.isEmpty()) && emptyArchivedChats()) {
                    if (isOnline(context) && initState != MegaChatApi.INIT_OFFLINE_SESSION) {
                        showEmptyChatScreen();
                        showFab();
                    } else {
                        showNoConnectionScreen();
                    }
                } else {
                    logDebug("Chats size: " + chats.size());

                    //Order by last interaction
                    sortChats(chats);

                    if (adapterList == null) {
                        logWarning("AdapterList is NULL");
                        adapterList = new MegaListChatAdapter(context, this, chats,
                                listView, MegaListChatAdapter.ADAPTER_RECENT_CHATS);
                    } else {
                        adapterList.setChats(chats);
                    }

                    if (listView != null) {
                        listView.setVisibility(View.VISIBLE);

                        if (listView.getAdapter() == null) {
                            listView.setAdapter(adapterList);
                            fastScroller.setRecyclerView(listView);
                        }

                        visibilityFastScroller();
                    }

                    if (emptyLayout != null) {
                        emptyLayout.setVisibility(View.GONE);
                    }

                    adapterList.setPositionClicked(-1);
                    showFab();
                }
            } else {
                showConnectingChatScreen();
            }
        }
    }

    private void showFab() {
        if (adapterList != null && adapterList.isMultipleSelect()) {
            return;
        }

        if (context instanceof ManagerActivity) {
            ((ManagerActivity) context).showFabButton();
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
        Spanned result = TextUtil.formatEmptyRecentChatsScreenText(context, StringResourcesUtils.getString(R.string.recent_chat_empty).toUpperCase());

        if (context instanceof ArchivedChatsActivity) {
            emptyTextView.setVisibility(context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ?
                    View.GONE : View.VISIBLE);

            emptyTextViewInvite.setVisibility(View.GONE);
            emptyDescriptionText.setVisibility(View.GONE);
            inviteButton.setVisibility(View.GONE);
            emptyTextView.setText(result);
        } else {
            emptyTextViewInvite.setText(result);
            emptyTextViewInvite.setVisibility(View.VISIBLE);
            emptyDescriptionText.setVisibility(View.VISIBLE);
            inviteButton.setVisibility(View.VISIBLE);
        }
    }

    private void addMarginTop() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp2px(108, outMetrics), dp2px(52, outMetrics), dp2px(108, outMetrics), dp2px(12, outMetrics));
        emptyImageView.setLayoutParams(lp);
    }

    public void showConnectingChatScreen(){
        logDebug("showConnectingChatScreen");

        listView.setVisibility(View.GONE);
        if (context instanceof ManagerActivity) {
            ((ManagerActivity) context).hideFabButton();
        }

        emptyTextViewInvite.setVisibility(View.INVISIBLE);
        emptyDescriptionText.setVisibility(View.INVISIBLE);

        inviteButton.setVisibility(View.GONE);

        String textToShow = context.getString(R.string.recent_chat_loading_conversations).toUpperCase();
        emptyTextView.setText(TextUtil.formatEmptyRecentChatsScreenText(context, textToShow));
        emptyTextView.setVisibility(View.VISIBLE);
    }

    public void showNoConnectionScreen() {
        logDebug("showNoConnectionScreen");

        listView.setVisibility(View.GONE);
        if (context instanceof ManagerActivity) {
            ((ManagerActivity) context).hideFabButton();
        }

        emptyTextViewInvite.setText(getString(R.string.error_server_connection_problem));
        emptyTextViewInvite.setVisibility(View.VISIBLE);
        emptyDescriptionText.setVisibility(View.GONE);
        inviteButton.setVisibility(View.GONE);
        emptyLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        switch (v.getId()) {
            case R.id.invite_button: {
                if (isOnline(context)) {
                    if (context instanceof ManagerActivity) {
                        Intent in = new Intent(context, AddContactActivity.class);
                        in.putExtra("contactType", CONTACT_TYPE_MEGA);
                        in.putExtra(FROM_RECENT, true);
                        ((ManagerActivity) context).startActivityForResult(in, REQUEST_CREATE_CHAT);
                    }
                    if (megaChatApi.isSignalActivityRequired()) {
                        megaChatApi.signalPresenceActivity();
                    }
                } else {
                    ((ManagerActivity) context).showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
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
                    returnActiveCall(context, passcodeManagement);
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
                bannerContainer.setVisibility(View.GONE);
            }

            new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
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
            Intent intent = new Intent(context, ChatActivity.class);
            intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intent.putExtra(CHAT_ID, adapterList.getChatAt(position).getChatId());
            this.startActivity(intent);
            if (context instanceof ManagerActivity) {
                if (((ManagerActivity) context).getSearchQuery() != null && !((ManagerActivity) context).getSearchQuery().isEmpty()) {
                    closeSearch();
                    ((ManagerActivity) context).closeSearchView();
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
    public void onAttach(@NonNull Context context) {
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

        if (context instanceof ManagerActivity) {
            if (!(((ManagerActivity) context).getDrawerItem() == DrawerItem.CHAT)) {
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
                int indexToReplace = getPositionFromChatId(chats, item.getChatId());
                if (indexToReplace != INVALID_POSITION) {
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
            int indexToReplace = getPositionFromChatId(chats, item.getChatId());
            if (indexToReplace != INVALID_POSITION) {
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
                int indexToReplace = getPositionFromChatId(chats, item.getChatId());
                if (indexToReplace != INVALID_POSITION) {
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
                int indexToReplace = getPositionFromChatId(chats, item.getChatId());
                if (indexToReplace != INVALID_POSITION) {
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
                int indexToRemove = getPositionFromChatId(chats, item.getChatId());
                if (indexToRemove != INVALID_POSITION) {
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
            if (context instanceof ManagerActivity) {
                if (item.isArchived()) {
                    logDebug("New archived element:remove from list");
                    if (adapterList == null || adapterList.getItemCount()==0){
                        setChats();
                    } else {
                        int indexToRemove = getPositionFromChatId(chats, item.getChatId());
                        if (indexToRemove != INVALID_POSITION) {
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
                                ((ManagerActivity) context).invalidateOptionsMenu();
                            }
                        }
                    }
                } else {
                    logDebug("New unarchived element: refresh chat list");
                    setChats();
                    if (chats.size() == 1) {
                        ((ManagerActivity) context).invalidateOptionsMenu();
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
                        int indexToRemove = getPositionFromChatId(chats, item.getChatId());
                        if (indexToRemove != INVALID_POSITION) {
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
                int indexToReplace = getPositionFromChatId(chats, item.getChatId());
                if (indexToReplace != INVALID_POSITION) {
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

    private boolean emptyArchivedChats() {
        ArrayList<MegaChatListItem> archivedChats = megaChatApi.getArchivedChatListItems();
        return archivedChats == null || archivedChats.isEmpty();
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
    }

    public void notifyPushChanged(){
        if (adapterList == null || adapterList.getItemCount() == 0) {
            setChats();
        } else if (chats != null && !chats.isEmpty()) {
            for (MegaChatListItem chat : chats) {
                int pos = chats.indexOf(chat);
                if (pos != INVALID_POSITION) {
                    adapterList.updateMuteIcon(pos);
                }
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

    public void updateCacheForNonContacts(MegaChatRoom chatToCheck) {
        if (chatToCheck != null) {
            long peers = chatToCheck.getPeerCount();
            for (int i = 0; i < peers; i++) {
                String fullName = new ChatController(context).getParticipantFullName(chatToCheck.getPeerHandle(i));
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
        outState.putBoolean(KEY_HINT_IS_SHOWING, isHintShowing);
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
        filterDisposable.clear();

        if(askForDisplayOverDialog != null) {
            askForDisplayOverDialog.recycle();
        }
        if(explanationDialog != null) {
            explanationDialog.cancel();
        }
        if(adapter != null) {
            adapter.dismissDialog();
        }
        if (context instanceof ManagerActivity) {
            ((ManagerActivity) context).setSearchQuery("");
        }

        // Avoid window leak.
        highLightHintHelper.dismissPopupWindow();
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

        if (context instanceof ManagerActivity) {
            if (((ManagerActivity) context).isSearchOpen()) {
                filterChats(((ManagerActivity) context).getSearchQuery(), false);
            }
            ((ManagerActivity) context).invalidateOptionsMenu();
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

            isHintShowing = savedInstanceState.getBoolean(KEY_HINT_IS_SHOWING);
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

    public void filterChats(String query, Boolean archivedChats) {
        if (adapterList != null && adapterList.isMultipleSelect()) {
            hideMultipleSelect();
        }
        filterDisposable.clear();

        Disposable disposable = searchChatsUseCase.search(query, archivedChats)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((filteredChats, throwable) -> {
                    if (throwable == null) {
                        if (adapterList == null) return;
                        adapterList.setChats(new ArrayList(filteredChats));

                        if (filteredChats.isEmpty()) {
                            listView.setVisibility(View.GONE);
                            emptyLayout.setVisibility(View.VISIBLE);
                            inviteButton.setVisibility(View.GONE);
                            Spanned result = TextUtil.formatEmptyRecentChatsScreenText(context, StringResourcesUtils.getString(R.string.recent_chat_empty).toUpperCase());
                            emptyTextViewInvite.setText(result);
                            emptyTextViewInvite.setVisibility(View.VISIBLE);
                        } else {
                            listView.setVisibility(View.VISIBLE);
                            emptyLayout.setVisibility(View.GONE);
                        }
                    } else {
                        logError(throwable.getMessage());
                    }
                });

        filterDisposable.add(disposable);
    }

    public void closeSearch() {
        filterDisposable.clear();

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

        boolean hasCameraPermission = hasPermissions(((ManagerActivity) context), Manifest.permission.CAMERA);
        if (!hasCameraPermission) {
            requestPermission(((ManagerActivity) context), REQUEST_CAMERA, Manifest.permission.CAMERA);
            return false;
        }

        boolean hasRecordAudioPermission = hasPermissions(((ManagerActivity) context), Manifest.permission.RECORD_AUDIO);
        if (!hasRecordAudioPermission) {
            requestPermission(((ManagerActivity) context), REQUEST_RECORD_AUDIO, Manifest.permission.RECORD_AUDIO);
            return false;
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
                        returnActiveCall(context, passcodeManagement);
                    }
                }
                break;
            }
            case REQUEST_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (checkPermissionsCall()) {
                        logDebug("RECORD_AUDIO -> returnTheCall");
                        returnActiveCall(context, passcodeManagement);
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

    // Multiselect
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
                    break;
                }
                case R.id.cab_menu_unselect_all: {
                    clearSelections();
                    hideMultipleSelect();
                    break;
                }
                case R.id.cab_menu_mute:
                    if (context instanceof ManagerActivity && chats != null && !chats.isEmpty()) {
                        createMuteNotificationsChatAlertDialog((Activity) context, chats);
                        clearSelections();
                        hideMultipleSelect();
                    }
                    break;

                case R.id.cab_menu_unmute:
                    if (context instanceof ManagerActivity && chats != null && !chats.isEmpty()) {
                        MegaApplication.getPushNotificationSettingManagement().controlMuteNotifications(context, NOTIFICATIONS_ENABLED, chats);
                        clearSelections();
                        hideMultipleSelect();
                    }
                    break;

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
                    showConfirmationLeaveChats(context, chats, ((ManagerActivity) context));
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
            if (context instanceof ManagerActivity) {
                ((ManagerActivity) context).hideFabButton();
                ((ManagerActivity) context).showHideBottomNavigationView(true);
            }
            checkScroll();
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            clearSelections();
            adapterList.setMultipleSelect(false);
            if (context instanceof ManagerActivity) {
                ((ManagerActivity) context).showFabButton();
                ((ManagerActivity) context).showHideBottomNavigationView(false);
            }
            checkScroll();

            if(showInviteBanner()) {
                bannerContainer.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            List<MegaChatListItem> selected = adapterList.getSelectedChats();
            boolean showMute = false;
            boolean showUnmute = false;
            boolean showLeaveChat = false;

            if (context instanceof ManagerActivity) {
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

                    boolean allChatsAreMuted = true;
                    boolean allChatAreUnmuted = true;
                    for (MegaChatListItem chat : selected) {
                        if (chat != null) {
                            if (!shouldMuteOrUnmuteOptionsBeShown(context, megaChatApi.getChatRoom(chat.getChatId()))) {
                                allChatsAreMuted = false;
                                allChatAreUnmuted = false;
                            } else if (isEnableChatNotifications(chat.getChatId())) {
                                allChatsAreMuted = false;
                            } else {
                                allChatAreUnmuted = false;
                            }
                        }
                    }

                    if (allChatsAreMuted) {
                        showUnmute = true;
                        showMute = false;
                    } else if (allChatAreUnmuted) {
                        showUnmute = false;
                        showMute = true;
                    } else {
                        showMute = false;
                        showUnmute = false;
                    }

                    menu.findItem(R.id.cab_menu_mute).setVisible(showMute);
                    menu.findItem(R.id.cab_menu_unmute).setVisible(showUnmute);
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
                menu.findItem(R.id.cab_menu_mute).setVisible(showMute);
                menu.findItem(R.id.cab_menu_unmute).setVisible(showUnmute);
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
        contactGetter.getMegaContacts(megaApi, TimeUtils.DAY, context);
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
            actionBarSubtitleArrow = v.findViewById(R.id.ab_subtitle_arrow);
            setStatus();
            v.findViewById(R.id.ab_subtitle_container).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (context != null && context instanceof ManagerActivity
                        && megaChatApi.getConnectionState() == MegaChatApi.CONNECTED
                        && isOnline(context)) {
                        ((ManagerActivity) context).showPresenceStatusDialog();
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

    private void setCustomisedActionBarSubtitle(SpannableString subtitle) {
        if(actionBarSubtitle != null) {
            actionBarSubtitle.setText(subtitle);
        }
        if (actionBarSubtitleArrow != null) {
            boolean showArrow = megaChatApi.getConnectionState() == MegaChatApi.CONNECTED
                && isOnline(context);
            actionBarSubtitleArrow.setVisibility(showArrow ? View.VISIBLE : View.GONE);
        }
    }
}