package mega.privacy.android.app.lollipop;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.brandongogetap.stickyheaders.StickyLayoutManager;
import com.brandongogetap.stickyheaders.exposed.StickyHeaderHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.HeaderItemDecoration;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.components.TopSnappedStickyLayoutManager;
import mega.privacy.android.app.components.scrollBar.FastScroller;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.lollipop.adapters.AddContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaAddContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.PhoneContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.ShareContactsAdapter;
import mega.privacy.android.app.lollipop.adapters.ShareContactsHeaderAdapter;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.qrcode.QRCodeActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatListenerInterface;
import nz.mega.sdk.MegaChatPresenceConfig;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaEvent;
import nz.mega.sdk.MegaGlobalListenerInterface;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;
import nz.mega.sdk.MegaUser;
import nz.mega.sdk.MegaUserAlert;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.TimeUtils.*;
import static mega.privacy.android.app.utils.Util.*;


public class AddContactActivityLollipop extends PinActivityLollipop implements View.OnClickListener, RecyclerView.OnItemTouchListener, StickyHeaderHandler, TextWatcher, TextView.OnEditorActionListener, MegaRequestListenerInterface, MegaChatListenerInterface, MegaGlobalListenerInterface {

    private static final int SCAN_QR_FOR_ADD_CONTACTS = 1111;
    public static final String EXTRA_MEGA_CONTACTS = "mega_contacts";
    public static final String EXTRA_CONTACTS = "extra_contacts";
    public static final String EXTRA_NODE_HANDLE = "node_handle";
    public static final String EXTRA_CHAT_TITLE = "chatTitle";
    public static final String EXTRA_GROUP_CHAT = "groupChat";
    public static final String EXTRA_EKR = "EKR";
    public static final String EXTRA_CHAT_LINK = "chatLink";

    private DisplayMetrics outMetrics;
    private MegaApplication app;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private DatabaseHandler dbH = null;
    private int contactType = 0;
    private int multipleSelectIntent;
    private long nodeHandle = -1;
    private long[] nodeHandles;
    private long chatId = -1;

    private AddContactActivityLollipop addContactActivityLollipop;

    private Toolbar tB;
    private ActionBar aB;

    private RelativeLayout containerContacts;
    private RecyclerView recyclerViewList;
    private LinearLayoutManager linearLayoutManager;
    private StickyLayoutManager stickyLayoutManager;
    private ImageView emptyImageView;
    private TextView emptyTextView;
    private TextView emptySubTextView;
    private Button emptyInviteButton;
    private ProgressBar progressBar;
    private RecyclerView addedContactsRecyclerView;
    private RelativeLayout containerAddedContactsRecyclerView;
    private LinearLayoutManager mLayoutManager;
    private String inputString  = "";
    private String savedInputString = "";

//    Adapter list MEGA contacts
    private MegaContactsLollipopAdapter adapterMEGA;
//    Adapter list chips MEGA contacts
    private MegaAddContactsLollipopAdapter adapterMEGAContacts;

    private ArrayList<MegaUser> contactsMEGA;
    private ArrayList<MegaContactAdapter> visibleContactsMEGA = new ArrayList<>();
    private ArrayList<MegaContactAdapter> filteredContactMEGA = new ArrayList<>();
    private ArrayList<MegaContactAdapter> addedContactsMEGA = new ArrayList<>();
    private ArrayList<MegaContactAdapter> queryContactMEGA = new ArrayList<>();


//    Adapter list Phone contacts
    private PhoneContactsLollipopAdapter adapterPhone;
//    Adapter list chips Phone contacts
    private AddContactsLollipopAdapter adapterContacts;

    private ArrayList<PhoneContactInfo> phoneContacts = new ArrayList<>();
    private ArrayList<PhoneContactInfo> addedContactsPhone = new ArrayList<>();
    private ArrayList<PhoneContactInfo> filteredContactsPhone = new ArrayList<>();
    private ArrayList<PhoneContactInfo> queryContactsPhone = new ArrayList<>();

//    Adapter list Share contacts
    private ShareContactsHeaderAdapter adapterShareHeader;
//    Adapter list chips MEGA/Phone contacts
    private ShareContactsAdapter adapterShare;

    private ArrayList<ShareContactInfo> addedContactsShare = new ArrayList<>();
    private ArrayList<ShareContactInfo> shareContacts = new ArrayList<>();
    private ArrayList<ShareContactInfo> filteredContactsShare = new ArrayList<>();
    private ArrayList<ShareContactInfo> queryContactsShare = new ArrayList<>();

    private RelativeLayout relativeLayout;

    private ArrayList<String> savedaddedContacts = new ArrayList<>();

    private MenuItem sendInvitationMenuItem;
    private MenuItem scanQrMenuItem;
    private MenuItem inviteContactMenuItem;

    private boolean comesFromChat;

    private boolean comesFromRecent;
    public static final String FROM_RECENT = "comesFromRecent";

    private RelativeLayout headerContacts;
    private TextView textHeader;

    private boolean fromAchievements = false;
    private ArrayList<String> mailsFromAchievements;

    private MenuItem searchMenuItem;
    private SearchView.SearchAutoComplete searchAutoComplete;
    private boolean searchExpand = false;

    private FilterContactsTask filterContactsTask;
    private GetContactsTask getContactsTask;
    private GetPhoneContactsTask getPhoneContactsTask;
    private RecoverContactsTask recoverContactsTask;
    private QueryIfContactSouldBeAddedTask queryIfContactSouldBeAddedTask;

    private FastScroller fastScroller;

    private FloatingActionButton fabImageGroup;
    private FloatingActionButton fabButton;
    private EmojiEditText nameGroup;
    private boolean onNewGroup = false;
    private boolean isConfirmDeleteShown = false;
    private String confirmDeleteMail;

    private RelativeLayout mailError;
    private Drawable editTextBackground;
    private RelativeLayout typeContactLayout;
    private EditText typeContactEditText;
    private RelativeLayout scanQRButton;
    private RelativeLayout inviteContactButton;
    private RelativeLayout newGroupChatButton;
    private RelativeLayout newChatLinkButton;
    private boolean isConfirmAddShown = false;
    private String confirmAddMail;
    private boolean createNewGroup = false;
    private boolean createNewChatLink = false;
    private String title = "";

    private boolean queryPermissions = true;

    private LinearLayout addContactsLayout;
    private NestedScrollView newGroupLayout;
    private SwitchCompat ekrSwitch;
    private boolean isEKREnabled = false;
    private RelativeLayout getChatLinkLayout;
    private CheckBox getChatLinkBox;
    private LinearLayoutManager newGrouplinearLayoutManager;
    private RecyclerView newGroupRecyclerView;
    private TextView newGroupHeaderList;
    private boolean newGroup = false;
    private ArrayList<String> contactsNewGroup;

    MegaContactAdapter myContact;

    private boolean onlyCreateGroup;
    private boolean waitingForPhoneContacts;

    @Override
    public List<ShareContactInfo> getAdapterData() {
        if (inputString != null && !inputString.equals("")) {
            return queryContactsShare;
        }
        else {
            return filteredContactsShare;
        }
    }

    private class GetPhoneContactsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            getDeviceContacts();
            MegaContactAdapter contactMEGA;
            PhoneContactInfo contactPhone;
            boolean found;
            shareContacts.clear();

            ShareContactInfo lastItem = filteredContactsShare.get(filteredContactsShare.size() - 1);
            if (lastItem.isProgress()) {
               filteredContactsShare.remove(lastItem);
            }

            if (filteredContactsPhone != null && !filteredContactsPhone.isEmpty()) {
                shareContacts.add(new ShareContactInfoHeader(true, false, true));
                for (int i=0; i<filteredContactsPhone.size(); i++) {
                    found = false;
                    contactPhone = filteredContactsPhone.get(i);
                    for (int j=0; j<filteredContactMEGA.size(); j++) {
                        contactMEGA = filteredContactMEGA.get(j);
                        if (contactPhone.getEmail().equals(getMegaContactMail(contactMEGA))){
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        shareContacts.add(new ShareContactInfo(contactPhone, null, null));
                    }
                    else {
                        filteredContactsPhone.remove(contactPhone);
                        i--;
                    }
                }

                for (int i=0; i<shareContacts.size(); i++) {
                    filteredContactsShare.add(shareContacts.get(i));
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            waitingForPhoneContacts = false;
            setShareAdapterContacts(filteredContactsShare);
        }
    }

    private class GetContactsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            if (contactType == CONTACT_TYPE_MEGA) {
                getVisibleMEGAContacts();
                if (newGroup) {
                    String mail;
                    MegaContactAdapter contact;
                    for (int i=0; i<contactsNewGroup.size(); i++) {
                        mail = contactsNewGroup.get(i);
                        for (int j=0; j<filteredContactMEGA.size(); j++) {
                            contact = filteredContactMEGA.get(j);
                            if ((contact.getMegaUser() != null && contact.getMegaUser().getEmail().equals(mail))
                                    || (contact.getMegaContactDB() != null && contact.getMegaContactDB().getMail().equals(mail))) {
                                addedContactsMEGA.add(contact);
                                filteredContactMEGA.remove(contact);
                                break;
                            }
                        }
                    }
                    adapterMEGAContacts.setContacts(addedContactsMEGA);
                }
            }
            else if (contactType == CONTACT_TYPE_DEVICE) {
                if (queryPermissions) {
                    getBothContacts();
                    addedContactsPhone.clear();
                    boolean found;
                    PhoneContactInfo contactPhone;
                    MegaContactAdapter contactMEGA;

                    if (filteredContactsPhone != null && !filteredContactsPhone.isEmpty()) {
                        for (int i = 0; i < filteredContactsPhone.size(); i++) {
                            found = false;
                            contactPhone = filteredContactsPhone.get(i);
                            for (int j = 0; j < visibleContactsMEGA.size(); j++) {
                                contactMEGA = visibleContactsMEGA.get(j);
                                if (contactPhone.getEmail().equals(getMegaContactMail(contactMEGA))) {
                                    found = true;
                                    break;
                                }
                            }
                            if (found) {
                                filteredContactsPhone.remove(contactPhone);
                                i--;
                            }
                        }
                    }
                }
            }
            else {
                getVisibleMEGAContacts();
                addedContactsPhone.clear();
                MegaContactAdapter contactMEGA;
                ShareContactInfo contact;
                shareContacts.clear();
                filteredContactsShare.clear();

                if (filteredContactMEGA != null && !filteredContactMEGA.isEmpty()) {
                    shareContacts.add(new ShareContactInfoHeader(true, true, false));
                    for (int i = 0; i<filteredContactMEGA.size(); i++) {
                        contactMEGA = filteredContactMEGA.get(i);
                        contact = new ShareContactInfo(null, contactMEGA, null);
                        shareContacts.add(contact);
                    }

                    for (int i=0; i<shareContacts.size(); i++) {
                        filteredContactsShare.add(shareContacts.get(i));
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void avoid) {
            logDebug("onPostExecute GetContactsTask");
            progressBar.setVisibility(View.GONE);
            if (searchExpand) {
                if (isAsyncTaskRunning(filterContactsTask)) {
                    filterContactsTask.cancel(true);
                }
                filterContactsTask = new FilterContactsTask();
                filterContactsTask.execute();
            }
            else {
                if (contactType == CONTACT_TYPE_MEGA) {
                    if (newGroup) {
                        setAddedAdapterContacts();
                    }
                    setMegaAdapterContacts(filteredContactMEGA, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
                }
                else if (contactType == CONTACT_TYPE_DEVICE) {
                    setPhoneAdapterContacts(filteredContactsPhone);
                }
                else {
                    setShareAdapterContacts(filteredContactsShare);
                    if (queryPermissions) {
                        waitingForPhoneContacts = true;
                        filteredContactsShare.add(new ShareContactInfo());
                        getPhoneContactsTask = new GetPhoneContactsTask();
                        getPhoneContactsTask.execute();
                    }
                }
                setTitleAB();
                setRecyclersVisibility();
                setSendInvitationVisibility();
                visibilityFastScroller();

                if (isConfirmAddShown) {
                    if (isAsyncTaskRunning(queryIfContactSouldBeAddedTask)) {
                        queryIfContactSouldBeAddedTask.cancel(true);
                    }
                    hideKeyboard(addContactActivityLollipop, 0);
                    queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask();
                    queryIfContactSouldBeAddedTask.execute(true);
                }
            }
        }
    }

    private class FilterContactsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (searchExpand) {
                if (searchAutoComplete != null) {
                    inputString = searchAutoComplete.getText().toString();
                }
            }
            else {
                inputString = typeContactEditText.getText().toString();
            }
            if (inputString != null && !inputString.equals("")){
                MegaContactAdapter contactMega;
                PhoneContactInfo contactPhone;
                ShareContactInfo contactShare;

                if (contactType == CONTACT_TYPE_MEGA) {
                    queryContactMEGA.clear();
                    for (int i=0; i<filteredContactMEGA.size(); i++){
                        contactMega = filteredContactMEGA.get(i);
                        if (getMegaContactMail(contactMega).toLowerCase().contains(inputString.toLowerCase())
                                || contactMega.getFullName().toLowerCase().contains(inputString.toLowerCase())) {
                            queryContactMEGA.add(contactMega);
                        }
                    }
                }
                else if (contactType == CONTACT_TYPE_DEVICE) {
                    queryContactsPhone.clear();
                    for (int i = 0; i<filteredContactsPhone.size(); i++) {
                        contactPhone = filteredContactsPhone.get(i);
                        if (contactPhone.getEmail().toLowerCase().contains(inputString.toLowerCase())
                                || contactPhone.getName().toLowerCase().contains(inputString.toLowerCase())) {
                            queryContactsPhone.add(contactPhone);
                        }
                    }
                }
                else {
                    queryContactsShare.clear();
                    int numMega = 0;
                    int numPhone = 0;
                    for (int i = 0; i<filteredContactsShare.size(); i++) {
                        contactShare = filteredContactsShare.get(i);
                        if (contactShare.isHeader()){
                            queryContactsShare.add(contactShare);
                        }
                        else {
                            if (contactShare.isMegaContact()){
                                if (getMegaContactMail(contactShare.getMegaContactAdapter()).toLowerCase().contains(inputString.toLowerCase())
                                        || contactShare.getMegaContactAdapter().getFullName().toLowerCase().contains(inputString.toLowerCase())) {
                                    queryContactsShare.add(contactShare);
                                    numMega++;
                                }
                            }
                            else {
                                if (contactShare.getPhoneContactInfo().getEmail().toLowerCase().contains(inputString.toLowerCase())
                                        || contactShare.getPhoneContactInfo().getName().toLowerCase().contains(inputString.toLowerCase())) {
                                    queryContactsShare.add(contactShare);
                                    numPhone++;
                                }
                            }
                        }
                    }
                    if (numMega == 0 && queryContactsShare.size() > 0) {
                        queryContactsShare.remove(0);
                    }
                    if (numPhone == 0 && (queryContactsShare.size()-1 >= 0)) {
                        queryContactsShare.remove(queryContactsShare.size()-1);
                    }
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void voids) {
            logDebug("onPostExecute FilterContactsTask");
            if (contactType == CONTACT_TYPE_MEGA) {
                if (inputString != null && !inputString.equals("")) {
                    setMegaAdapterContacts(queryContactMEGA, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
                }
                else {
                    setMegaAdapterContacts(filteredContactMEGA, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
                }
            }
            else if (contactType == CONTACT_TYPE_DEVICE) {
                if (inputString != null && !inputString.equals("")) {
                    setPhoneAdapterContacts(queryContactsPhone);
                }
                else {
                    setPhoneAdapterContacts(filteredContactsPhone);
                }
            }
            else {
                if (inputString != null && !inputString.equals("")) {
                    setShareAdapterContacts(queryContactsShare);
                }
                else {
                    setShareAdapterContacts(filteredContactsShare);
                }
            }
            visibilityFastScroller();

            if (isConfirmAddShown) {
                if (isAsyncTaskRunning(queryIfContactSouldBeAddedTask)) {
                    queryIfContactSouldBeAddedTask.cancel(true);
                }
                hideKeyboard(addContactActivityLollipop, 0);
                queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask();
                queryIfContactSouldBeAddedTask.execute(true);
            }
        }
    }

    private class RecoverContactsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if (contactType == CONTACT_TYPE_MEGA) {
                getVisibleMEGAContacts();
                String contactToAddMail = null;
                MegaContactAdapter contactToAdd, contact;
                for (int i=0; i<savedaddedContacts.size(); i++){
                    String mail = savedaddedContacts.get(i);
                    for (int j=0;j<filteredContactMEGA.size(); j++){
                        contact = filteredContactMEGA.get(j);
                        contactToAddMail = getMegaContactMail(contact);
                        if (contactToAddMail != null && contactToAddMail.equals(mail)){
                            if (!addedContactsMEGA.contains(contact)) {
                                addedContactsMEGA.add(contact);
                                filteredContactMEGA.remove(contact);
                            }
                            break;
                        }
                    }
                    if (contactToAddMail != null && !contactToAddMail.equals(mail)){
                        contactToAdd = new MegaContactAdapter(null, null, mail);
                        if (!addedContactsMEGA.contains(contactToAdd)) {
                            addedContactsMEGA.add(contactToAdd);
                        }
                    }
                }
            }
            else {
                getBothContacts();
                MegaContactAdapter contactMEGA;
                PhoneContactInfo contactPhone;
                ShareContactInfo contact = null;
                boolean found;
                shareContacts.clear();

                if (filteredContactMEGA != null && !filteredContactMEGA.isEmpty()) {
                    shareContacts.add(new ShareContactInfoHeader(true, true, false));
                    for (int i = 0; i<filteredContactMEGA.size(); i++) {
                        contactMEGA = filteredContactMEGA.get(i);
                        contact = new ShareContactInfo(null, contactMEGA, null);
                        shareContacts.add(contact);
                    }
                }
                if (filteredContactsPhone != null && !filteredContactsPhone.isEmpty()) {
                    shareContacts.add(new ShareContactInfoHeader(true, false, true));
                    for (int i=0; i<filteredContactsPhone.size(); i++) {
                        found = false;
                        contactPhone = filteredContactsPhone.get(i);
                        for (int j=0; j<filteredContactMEGA.size(); j++) {
                            contactMEGA = filteredContactMEGA.get(j);
                            if (contactPhone.getEmail().equals(getMegaContactMail(contactMEGA))){
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            shareContacts.add(new ShareContactInfo(contactPhone, null, null));
                        }
                        else {
                            filteredContactsPhone.remove(contactPhone);
                            i--;
                        }
                    }
                }
                filteredContactsShare.clear();
                for (int i=0; i<shareContacts.size(); i++) {
                    filteredContactsShare.add(shareContacts.get(i));
                }
                addedContactsShare.clear();
                String contactToAddMail = null;

                for (int i=0; i<savedaddedContacts.size(); i++){
                    String mail = savedaddedContacts.get(i);
                    logDebug("mail[" + i + "]: " + mail);
                    for (int j = 0; j< filteredContactsShare.size(); j++){
                        contact = filteredContactsShare.get(j);
                        if (contact.isMegaContact() && !contact.isHeader()){
                            contactToAddMail = getMegaContactMail(contact.getMegaContactAdapter());
                        }
                        else if (!contact.isHeader()){
                            contactToAddMail = contact.getPhoneContactInfo().getEmail();
                        }
                        else {
                            contactToAddMail = null;
                        }
                        if (contactToAddMail != null && contactToAddMail.equals(mail)){
                            if (!addedContactsShare.contains(contact)) {
                                addedContactsShare.add(contact);
                                filteredContactsShare.remove(contact);
                                if (contact.isMegaContact()){
                                    filteredContactMEGA.remove(contact.getMegaContactAdapter());
                                }
                                else {
                                    filteredContactsPhone.remove(contact.getPhoneContactInfo());
                                }
                            }
                            break;
                        }
                    }
                    if (contactToAddMail != null && !contactToAddMail.equals(mail)){
                        contact = new ShareContactInfo(null, null, mail);
                        if (!addedContactsShare.contains(contact)) {
                            addedContactsShare.add(contact);
                        }
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            logDebug("onPostExecute RecoverContactsTask");
            setAddedAdapterContacts();
            if (searchExpand) {
                if (isAsyncTaskRunning(filterContactsTask)) {
                    filterContactsTask.cancel(true);
                }
                filterContactsTask = new FilterContactsTask();
                filterContactsTask.execute();
            }
            else {
                if (contactType == CONTACT_TYPE_MEGA) {
                    if (onNewGroup) {
                        newGroup();
                    }
                    else {
                        setMegaAdapterContacts(filteredContactMEGA, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
                    }
                }
                else {
                    setShareAdapterContacts(filteredContactsShare);
                }
                setTitleAB();
                setRecyclersVisibility();
                visibilityFastScroller();

                if (isConfirmAddShown) {
                    if (isAsyncTaskRunning(queryIfContactSouldBeAddedTask)) {
                        queryIfContactSouldBeAddedTask.cancel(true);
                    }
                    hideKeyboard(addContactActivityLollipop, 0);
                    queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask();
                    queryIfContactSouldBeAddedTask.execute(true);
                }
            }
        }
    }

    private void getDeviceContacts() {
        if (queryPermissions) {
            if (phoneContacts != null) {
                phoneContacts.clear();
            }
            filteredContactsPhone.clear();
            phoneContacts = getPhoneContacts();
            if (phoneContacts != null) {
                for (int i = 0; i < phoneContacts.size(); i++) {
                    filteredContactsPhone.add(phoneContacts.get(i));
                }
            }
        }
    }

    private void getBothContacts() {
        getDeviceContacts();
        getVisibleMEGAContacts();
    }

    private class QueryIfContactSouldBeAddedTask extends AsyncTask<Boolean, Void, Integer> {

        ShareContactInfo shareContactInfo;
        PhoneContactInfo phoneContactInfo;
        boolean showDialog;
        final int isShareContact = 1;
        final int addContactShare = 2;
        final int isPhoneContact = 3;
        final int addContactPhone = 4;
        final int isAddedContact = 5;
        final int isMegaContact = 6;

        @Override
        protected Integer doInBackground(Boolean... booleans) {

            showDialog = booleans[0];

            if (contactType == CONTACT_TYPE_DEVICE) {
                for (int i=0; i<addedContactsPhone.size(); i++) {
                    if (addedContactsPhone.get(i).getEmail().equals(confirmAddMail)) {
                        return isAddedContact;
                    }
                }
                for (int i=0; i<filteredContactsPhone.size(); i++) {
                    if (filteredContactsPhone.get(i).getEmail().equals(confirmAddMail)) {
                        phoneContactInfo = filteredContactsPhone.get(i);
                        return isPhoneContact;
                    }
                }
                for (int i=0; i<visibleContactsMEGA.size(); i++) {
                    if (getMegaContactMail(visibleContactsMEGA.get(i)).equals(confirmAddMail)) {
                        return isMegaContact;
                    }
                }
                return addContactPhone;
            }
            else if (contactType == CONTACT_TYPE_BOTH) {
                for (int i=0; i<addedContactsShare.size(); i++) {
                    if (addedContactsShare.get(i).isMegaContact() && !addedContactsShare.get(i).isHeader()) {
                        if (getMegaContactMail(addedContactsShare.get(i).getMegaContactAdapter()).equals(confirmAddMail)) {
                            return isAddedContact;
                        }
                    }
                    else if (addedContactsShare.get(i).isPhoneContact() && !addedContactsShare.get(i).isHeader()) {
                        if (addedContactsShare.get(i).getPhoneContactInfo().getEmail().equals(confirmAddMail)) {
                            return isAddedContact;
                        }
                    }
                    else {
                        if (addedContactsShare.get(i).getMail().equals(confirmAddMail)) {
                            return isAddedContact;
                        }
                    }
                }

                for (int i=0; i<filteredContactsShare.size(); i++) {
                    if (filteredContactsShare.get(i).isMegaContact() && !filteredContactsShare.get(i).isHeader()) {
                        if (getMegaContactMail(filteredContactsShare.get(i).getMegaContactAdapter()).equals(confirmAddMail)) {
                            shareContactInfo = filteredContactsShare.get(i);
                            return isShareContact;
                        }
                    }
                    else if (filteredContactsShare.get(i).isPhoneContact() && !filteredContactsShare.get(i).isHeader()) {
                        if (filteredContactsShare.get(i).getPhoneContactInfo().getEmail().equals(confirmAddMail)) {
                            shareContactInfo = filteredContactsShare.get(i);
                            return isShareContact;
                        }
                    }
                }
                return addContactShare;
            }
            return 0;
        }

        void shareContact () {
            addShareContact(shareContactInfo);
            int position = filteredContactsShare.indexOf(shareContactInfo);
            if (shareContactInfo.isMegaContact()) {
                if (filteredContactMEGA.size() == 1) {
                    filteredContactsShare.remove(0);
                }
                filteredContactMEGA.remove(shareContactInfo.getMegaContactAdapter());
            } else if (shareContactInfo.isPhoneContact()) {
                filteredContactsPhone.remove(shareContactInfo.getPhoneContactInfo());
                if (filteredContactsPhone.size() == 0) {
                    filteredContactsShare.remove(filteredContactsShare.size() - 2);
                }
            }
            filteredContactsShare.remove(shareContactInfo);
            setShareAdapterContacts(filteredContactsShare);
        }

        void phoneContact () {
            addContact(phoneContactInfo);
            filteredContactsPhone.remove(phoneContactInfo);
            setPhoneAdapterContacts(filteredContactsPhone);
        }

        @Override
        protected void onPostExecute(final Integer type) {
            logDebug("onPostExecute QueryIfContactSouldBeAddedTask");
            if (showDialog) {
                AlertDialog.Builder builder = new AlertDialog.Builder(addContactActivityLollipop, R.style.AppCompatAlertDialogStyleAddContacts);
                builder.setCancelable(false);

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case DialogInterface.BUTTON_POSITIVE: {
                                if (contactType == CONTACT_TYPE_DEVICE) {
                                    if (type == isPhoneContact) {
                                        phoneContact();
                                    } else {
                                        addContact(new PhoneContactInfo(0, null, confirmAddMail, null));
                                    }
                                } else if (contactType == CONTACT_TYPE_BOTH) {
                                    if (type == isShareContact) {
                                        shareContact();
                                    } else {
                                        addShareContact(new ShareContactInfo(null, null, confirmAddMail));
                                    }
                                }
                                isConfirmAddShown = false;
                                break;
                            }

                            case DialogInterface.BUTTON_NEGATIVE: {
                                //No button clicked
                                isConfirmAddShown = false;
                                break;
                            }
                        }
                    }
                };

                switch (type) {
                    case isShareContact:
                    case addContactShare: {
                        builder.setMessage(getString(R.string.confirmation_share_contact, confirmAddMail));

                        builder.setPositiveButton(R.string.menu_add_contact, dialogClickListener)
                                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
                        break;
                    }
                    case isPhoneContact:
                    case addContactPhone: {
                        builder.setMessage(getString(R.string.confirmation_invite_contact, confirmAddMail));

                        builder.setPositiveButton(R.string.menu_add_contact, dialogClickListener)
                                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
                        break;
                    }
                    case isAddedContact: {
                        builder.setMessage(getString(R.string.confirmation_invite_contact_already_added, confirmAddMail));

                        builder.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
                        break;
                    }
                    case isMegaContact: {
                        builder.setMessage(getString(R.string.confirmation_not_invite_contact, confirmAddMail));

                        builder.setNegativeButton(R.string.general_cancel, dialogClickListener).show();
                    }
                }

                isConfirmAddShown = true;
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        isConfirmAddShown = false;
                    }
                });
            }
            else {
                switch (type) {
                    case isShareContact: {
                        shareContact();
                        break;
                    }
                    case addContactShare: {
                        addShareContact(new ShareContactInfo(null, null, confirmAddMail));
                        break;
                    }
                    case isPhoneContact: {
                        phoneContact();
                        break;
                    }
                    case addContactPhone: {
                        addContact(new PhoneContactInfo(0, null, confirmAddMail, null));
                        break;
                    }
                    case isAddedContact: {
                        showSnackbar(getString(R.string.contact_not_added));
                        break;
                    }
                    case isMegaContact: {
                        showSnackbar(getString(R.string.context_contact_already_exists, confirmAddMail));
                        break;
                    }
                }
            }
        }
    }

    private void setAddedAdapterContacts () {
        if (contactType == CONTACT_TYPE_MEGA) {
            if (adapterMEGAContacts == null){
                adapterMEGAContacts = new MegaAddContactsLollipopAdapter(addContactActivityLollipop, addedContactsMEGA);
            }
            else {
                adapterMEGAContacts.setContacts(addedContactsMEGA);
            }

            if (addedContactsMEGA.size() == 0){
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            }
            else {
                containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
            }

            addedContactsRecyclerView.setAdapter(adapterMEGAContacts);
        }
        else if (contactType == CONTACT_TYPE_DEVICE) {
            if (adapterContacts == null){
                adapterContacts = new AddContactsLollipopAdapter(this, addedContactsPhone);
            }
            else {
                adapterContacts.setContacts(addedContactsPhone);
            }

            if (addedContactsPhone.size() == 0){
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            }
            else {
                containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
            }

            addedContactsRecyclerView.setAdapter(adapterContacts);
        }
        else {
            if (adapterShare == null) {
                adapterShare = new ShareContactsAdapter(addContactActivityLollipop, addedContactsShare);
            }
            else {
                adapterShare.setContacts(addedContactsShare);
            }

            if (addedContactsShare.size() == 0){
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            }
            else {
                containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
            }

            addedContactsRecyclerView.setAdapter(adapterShare);
        }

        setSendInvitationVisibility();
    }

    private void setPhoneAdapterContacts (ArrayList<PhoneContactInfo> contacts) {
        if(queryPermissions && filteredContactsPhone!=null){
            if (filteredContactsPhone.size() == 0){
                headerContacts.setVisibility(View.GONE);
                String textToShow = String.format(getString(R.string.context_empty_contacts)).toUpperCase();
                try{
                    textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                }
                catch (Exception e){}
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);
            }
            else {
                emptyTextView.setText(R.string.contacts_list_empty_text_loading);
            }
        }
        else{
            if (!queryPermissions) {
                emptyTextView.setText(R.string.no_contacts_permissions);
            }
            logDebug("PhoneContactsTask: Phone contacts null");
            boolean hasReadContactsPermission = (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
            if (!hasReadContactsPermission) {
                logWarning("PhoneContactsTask: No read contacts permission");
            }
        }

        if (adapterPhone == null){
            adapterPhone = new PhoneContactsLollipopAdapter(addContactActivityLollipop, contacts);

            recyclerViewList.setAdapter(adapterPhone);

            adapterPhone.SetOnItemClickListener(new PhoneContactsLollipopAdapter.OnItemClickListener() {

                @Override
                public void onItemClick(View view, int position) {
                    itemClick(view, position);
                }
            });
        }
        else{
            adapterPhone.setContacts(contacts);
        }

        if(adapterPhone!=null){
            if (adapterPhone.getItemCount() == 0){
                headerContacts.setVisibility(View.GONE);
                if (contactType == CONTACT_TYPE_BOTH) {
                    if (adapterMEGA != null) {
                        if (adapterMEGA.getItemCount() == 0) {
                            setEmptyStateVisibility(true);
                        }
                        else {
                            setEmptyStateVisibility(false);
                        }
                    }
                }
                else {
                    setEmptyStateVisibility(true);
                }
            }
            else{
                headerContacts.setVisibility(View.VISIBLE);
                setEmptyStateVisibility(false);
            }
        }
    }

    private void setMegaAdapterContacts (ArrayList<MegaContactAdapter> contacts, int adapter) {
        if (onNewGroup){
            adapterMEGA = new MegaContactsLollipopAdapter(addContactActivityLollipop, null, contacts, newGroupRecyclerView, adapter);

            adapterMEGA.setPositionClicked(-1);
            newGroupRecyclerView.setAdapter(adapterMEGA);
        }
        else {
            if (adapterMEGA == null) {
                adapterMEGA = new MegaContactsLollipopAdapter(addContactActivityLollipop, null, contacts, recyclerViewList, adapter);
            } else {
                adapterMEGA.setAdapterType(adapter);
                adapterMEGA.setContacts(contacts);
            }

            adapterMEGA.setPositionClicked(-1);
            recyclerViewList.setAdapter(adapterMEGA);

            if (adapterMEGA.getItemCount() == 0) {
                String textToShow = getString(R.string.context_empty_contacts).toUpperCase();
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
                emptyTextView.setText(result);
                headerContacts.setVisibility(View.GONE);
                recyclerViewList.setVisibility(View.GONE);
                setEmptyStateVisibility(true);
            }
            else {
                headerContacts.setVisibility(View.VISIBLE);
                recyclerViewList.setVisibility(View.VISIBLE);
                setEmptyStateVisibility(false);
            }
        }
    }

    private void setShareAdapterContacts (ArrayList<ShareContactInfo> contacts) {
        if (adapterShareHeader == null){
            adapterShareHeader = new ShareContactsHeaderAdapter(addContactActivityLollipop, contacts);
            recyclerViewList.setAdapter(adapterShareHeader);
            adapterShareHeader.SetOnItemClickListener(new ShareContactsHeaderAdapter.OnItemClickListener() {

                @Override
                public void onItemClick(View view, int position) {
                    itemClick(view, position);
                }
            });
        }
        else{
            adapterShareHeader.setContacts(contacts);
        }

        if (adapterShareHeader.getItemCount() == 0){
            String textToShow = String.format(getString(R.string.context_empty_contacts)).toUpperCase();
            try{
                textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                textToShow = textToShow.replace("[/A]", "</font>");
                textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                textToShow = textToShow.replace("[/B]", "</font>");
            }
            catch (Exception e){}
            Spanned result = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
            } else {
                result = Html.fromHtml(textToShow);
            }
            emptyTextView.setText(result);
        }
        else {
            setEmptyStateVisibility(false);
        }
    }

    private final static boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            logDebug("isValid");
            return EMAIL_ADDRESS.matcher(target).matches();
        }
    }

    private void setSendInvitationVisibility() {
        if (fabButton != null) {
            if (contactType == CONTACT_TYPE_MEGA && !onNewGroup && (createNewGroup || createNewChatLink
                    || (comesFromChat && adapterMEGAContacts != null && adapterMEGAContacts.getItemCount() > 0))){
                fabButton.show();
            }
            else if (contactType == CONTACT_TYPE_DEVICE && adapterContacts != null && adapterContacts.getItemCount() > 0) {
                fabButton.show();
            }
            else if (contactType == CONTACT_TYPE_BOTH && adapterShare != null && adapterShare.getItemCount() > 0){
                fabButton.show();
            }
            else {
                fabButton.hide();
            }
        }
        if (sendInvitationMenuItem != null) {
            if (contactType == CONTACT_TYPE_MEGA && onNewGroup) {
                sendInvitationMenuItem.setVisible(true);
            }
            else {
                sendInvitationMenuItem.setVisible(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        logDebug("onCreateOptionsMenu");

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.activity_add_contact, menu);

        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchMenuItem = menu.findItem(R.id.action_search);
        searchMenuItem.setIcon(mutateIcon(this, R.drawable.ic_menu_search, R.color.black));

        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);

        searchAutoComplete = (SearchView.SearchAutoComplete) searchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        searchAutoComplete.setTextColor(ContextCompat.getColor(this, R.color.black));
        searchAutoComplete.setHintTextColor(ContextCompat.getColor(this, R.color.status_bar_login));
        searchAutoComplete.setHint(getString(R.string.hint_action_search));
        View v = searchView.findViewById(android.support.v7.appcompat.R.id.search_plate);
        v.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent));

        searchView.setIconifiedByDefault(true);

        searchAutoComplete.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard(addContactActivityLollipop, 0);
                    return true;
                }
                return false;
            }
        });

        ImageView closeIcon = (ImageView) searchView.findViewById(android.support.v7.appcompat.R.id.search_close_btn);
        closeIcon.setImageDrawable(mutateIcon(this, R.drawable.ic_close_white, R.color.black));

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                logDebug("onMenuItemActionExpand");
                searchExpand = true;
                typeContactEditText.getText().clear();
                if (isAsyncTaskRunning(filterContactsTask)) {
                    filterContactsTask.cancel(true);
                }
                filterContactsTask = new FilterContactsTask();
                filterContactsTask.execute();
                setSendInvitationVisibility();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                logDebug("onMenuItemActionCollapse");
                searchExpand = false;
                setSendInvitationVisibility();
                setTitleAB();
                if (isAsyncTaskRunning(filterContactsTask)){
                    filterContactsTask.cancel(true);
                }
                return true;
            }
        });
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                logDebug("onQueryTextChange searchView");
                if (isAsyncTaskRunning(filterContactsTask)){
                    filterContactsTask.cancel(true);
                }
                filterContactsTask = new FilterContactsTask();
                filterContactsTask.execute();
                return true;
            }

        });

        scanQrMenuItem = menu.findItem(R.id.action_scan_qr);
        scanQrMenuItem.setIcon(mutateIcon(this, R.drawable.ic_action_scan_qr, R.color.black));
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                && contactType != CONTACT_TYPE_MEGA) {
            scanQrMenuItem.setVisible(true);
        }
        else {
            scanQrMenuItem.setVisible(false);
        }

        inviteContactMenuItem = menu.findItem(R.id.action_invite_contact);
        inviteContactMenuItem.setIcon(mutateIcon(this, R.drawable.ic_add_contact, R.color.black));
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE
                && contactType == CONTACT_TYPE_MEGA && !createNewGroup && !comesFromChat && !onNewGroup) {
            inviteContactMenuItem.setVisible(true);
        }
        else {
            inviteContactMenuItem.setVisible(false);
        }

        sendInvitationMenuItem = menu.findItem(R.id.action_send_invitation);
        sendInvitationMenuItem.setIcon(mutateIcon(this, R.drawable.ic_send_white, R.color.accentColor));
        setSendInvitationVisibility();

        if (searchExpand && searchMenuItem != null) {
            searchMenuItem.expandActionView();
            if (searchView != null) {
                logDebug("searchView != null inputString: " + savedInputString);
                searchView.setQuery(savedInputString, false);
                if (recoverContactsTask != null && recoverContactsTask.getStatus() == AsyncTask.Status.FINISHED) {
                    filterContactsTask = new FilterContactsTask();
                    filterContactsTask.execute();
                }
            }
        }
        setSearchVisibility();

        if (!queryPermissions && contactType == CONTACT_TYPE_DEVICE) {
            searchMenuItem.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    private void setSearchVisibility() {
        if (searchMenuItem == null) {
            return;
        }

        boolean visible;

        if ((contactType == CONTACT_TYPE_MEGA && filteredContactMEGA.isEmpty())
                || (contactType ==  CONTACT_TYPE_DEVICE && filteredContactsPhone.isEmpty())
                || (contactType == CONTACT_TYPE_BOTH && filteredContactsShare.isEmpty())){
            visible = false;
        }
        else {
            visible = true;
        }

        if (searchMenuItem.isVisible() != visible) {
            searchMenuItem.setVisible(visible);
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        logDebug("onPrepareOptionsMenu");

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        logDebug("onOptionsItemSelected");

        int id = item.getItemId();
        switch(id) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            case R.id.action_scan_qr: {
                initScanQR();
                break;
            }
            case R.id.action_send_invitation: {
                if (contactType == CONTACT_TYPE_MEGA) {
                    setResultContacts(addedContactsMEGA, true);
                }
                else {
                    shareWith(addedContactsShare);
                }
                hideKeyboard(addContactActivityLollipop, 0);
                break;
            }
            case R.id.action_invite_contact: {
                toInviteContact();
                break;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshKeyboard() {

        String s = typeContactEditText.getText().toString();
        int imeOptions = typeContactEditText.getImeOptions();

        if (s != null) {
            if (s.length() == 0 && (!addedContactsMEGA.isEmpty() || !addedContactsPhone.isEmpty() || !addedContactsShare.isEmpty())){
                typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_SEND);
            }
            else {
                typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            }
        }
        else if (!addedContactsMEGA.isEmpty() || !addedContactsPhone.isEmpty() || !addedContactsShare.isEmpty()) {
            typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_SEND);
        }
        else {
            typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }

        int imeOptionsNew = typeContactEditText.getImeOptions();
        if (imeOptions != imeOptionsNew) {
            View view = getCurrentFocus();
            if (view != null) {
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.restartInput(view);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("fromAchievements", fromAchievements);
        outState.putStringArrayList("mailsFromAchievements", mailsFromAchievements);
        outState.putBoolean("searchExpand", searchExpand);
        if (searchExpand ) {
            if (searchAutoComplete != null) {
                outState.putString("inputString", searchAutoComplete.getText().toString());
            }
        }
        else {
            outState.putString("inputString", typeContactEditText.getText().toString());
        }
        outState.putBoolean("onNewGroup", onNewGroup);
        outState.putBoolean("isConfirmDeleteShown", isConfirmDeleteShown);
        outState.putString("confirmDeleteMail", confirmDeleteMail);
        outState.putBoolean(FROM_RECENT, comesFromRecent);
        if (isAsyncTaskRunning(queryIfContactSouldBeAddedTask)){
            isConfirmAddShown = true;
            queryIfContactSouldBeAddedTask.cancel(true);
        }
        outState.putBoolean("isConfirmAddShown", isConfirmAddShown);
        outState.putString("confirmAddMail", confirmAddMail);
        outState.putBoolean("createNewGroup", createNewGroup);
        outState.putBoolean("queryPermissions", queryPermissions);
        outState.putBoolean("createNewChatLink", createNewChatLink);
        outState.putBoolean("isEKREnabled", isEKREnabled);
        outState.putBoolean("newGroup", newGroup);
        outState.putBoolean("onlyCreateGroup", onlyCreateGroup);

        saveContactsAdded(outState);
    }

    private boolean isAsyncTaskRunning(AsyncTask asyncTask) {
        return asyncTask != null && asyncTask.getStatus() == AsyncTask.Status.RUNNING;
    }

    private void saveContactsAdded (Bundle outState) {

        boolean finished = true;

        if (isAsyncTaskRunning(getContactsTask)){
            getContactsTask.cancel(true);
            finished = false;
            if (contactType == CONTACT_TYPE_DEVICE) {
                outState.putParcelableArrayList("addedContactsPhone", null);
                outState.putParcelableArrayList("filteredContactsPhone", null);
                outState.putParcelableArrayList("phoneContacts", null);
            } else {
                outState.putStringArrayList("savedaddedContacts", null);
            }
        } else if (isAsyncTaskRunning(getPhoneContactsTask)) {
            getPhoneContactsTask.cancel(true);
            finished = false;
            outState.putStringArrayList("savedaddedContacts", null);
        } else if (isAsyncTaskRunning(filterContactsTask)) {
            filterContactsTask.cancel(true);
            finished = true;
        } else if (isAsyncTaskRunning(recoverContactsTask)) {
            recoverContactsTask.cancel(true);
            finished = false;
            if (contactType == CONTACT_TYPE_DEVICE) {
                outState.putParcelableArrayList("addedContactsPhone", addedContactsPhone);
                outState.putParcelableArrayList("filteredContactsPhone", filteredContactsPhone);
                outState.putParcelableArrayList("phoneContacts", phoneContacts);
            } else {
                outState.putStringArrayList("savedaddedContacts", savedaddedContacts);
            }
        }

        if (finished){
            savedaddedContacts.clear();
            if (contactType == CONTACT_TYPE_MEGA) {
                if (onNewGroup) {
                    createMyContact();
                    if (addedContactsMEGA.contains(myContact)) {
                        addedContactsMEGA.remove(myContact);
                    }
                }
                for (int i=0; i<addedContactsMEGA.size(); i++){
                    if (getMegaContactMail(addedContactsMEGA.get(i)) != null) {
                        savedaddedContacts.add(getMegaContactMail(addedContactsMEGA.get(i)));
                    }
                    else {
                        savedaddedContacts.add(addedContactsMEGA.get(i).getFullName());
                    }
                }
                outState.putStringArrayList("savedaddedContacts", savedaddedContacts);
            }
            else if (contactType == CONTACT_TYPE_DEVICE) {
                outState.putParcelableArrayList("addedContactsPhone", addedContactsPhone);
                outState.putParcelableArrayList("filteredContactsPhone", filteredContactsPhone);
                outState.putParcelableArrayList("phoneContacts", phoneContacts);
            }
            else {
                for (int i=0; i<addedContactsShare.size(); i++){
                    if (addedContactsShare.get(i).isMegaContact()) {
                        savedaddedContacts.add(getMegaContactMail(addedContactsShare.get(i).getMegaContactAdapter()));
                    }
                    else if (addedContactsShare.get(i).isPhoneContact()) {
                        savedaddedContacts.add(addedContactsShare.get(i).getPhoneContactInfo().getEmail());
                    }
                    else {
                        savedaddedContacts.add(addedContactsShare.get(i).getMail());
                    }
                }
                outState.putStringArrayList("savedaddedContacts", savedaddedContacts);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(megaApi != null) {
            megaApi.removeGlobalListener(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        logDebug("onCreate");

        super.onCreate(savedInstanceState);

        if (getIntent() != null){
            contactType = getIntent().getIntExtra("contactType", CONTACT_TYPE_MEGA);
            chatId = getIntent().getLongExtra("chatId", -1);
            newGroup = getIntent().getBooleanExtra("newGroup", false);
            comesFromRecent = getIntent().getBooleanExtra(FROM_RECENT, false);
            if (newGroup) {
                createNewGroup = true;
                contactsNewGroup = getIntent().getStringArrayListExtra("contactsNewGroup");
            }
            fromAchievements = getIntent().getBooleanExtra("fromAchievements", false);
            if (fromAchievements){
                mailsFromAchievements = getIntent().getStringArrayListExtra(EXTRA_CONTACTS);
            }
            comesFromChat = getIntent().getBooleanExtra("chat", false);
            if (comesFromChat) {
                title = getIntent().getStringExtra("aBtitle");
            }
            onlyCreateGroup = getIntent().getBooleanExtra("onlyCreateGroup", false);
            if (contactType == CONTACT_TYPE_MEGA || contactType == CONTACT_TYPE_BOTH){
                multipleSelectIntent = getIntent().getIntExtra("MULTISELECT", -1);
                if(multipleSelectIntent==0){
                    nodeHandle =  getIntent().getLongExtra(EXTRA_NODE_HANDLE, -1);
                }
                else if(multipleSelectIntent==1){
                    logDebug("Multiselect YES!");
                    nodeHandles=getIntent().getLongArrayExtra(EXTRA_NODE_HANDLE);
                }
            }
        }

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.dark_primary_color));

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);

        app = (MegaApplication)getApplication();
        megaApi = app.getMegaApi();
        megaApi.addGlobalListener(this);
        if(megaApi==null||megaApi.getRootNode()==null){
            logDebug("Refresh session - sdk");
            Intent intent = new Intent(this, LoginActivityLollipop.class);
            intent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return;
        }
        if(isChatEnabled()){
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) getApplication()).getMegaChatApi();
            }

            if(megaChatApi==null||megaChatApi.getInitState()== MegaChatApi.INIT_ERROR){
                logDebug("Refresh session - karere");
                Intent intent = new Intent(this, LoginActivityLollipop.class);
                intent.putExtra("visibleFragment",  LOGIN_FRAGMENT);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return;
            }
        }

        addContactActivityLollipop = this;

        if (megaChatApi != null){
            megaChatApi.addChatListener(this);
        }

        dbH = DatabaseHandler.getDbHandler(this);
        setContentView(R.layout.activity_add_contact);

        tB = (Toolbar) findViewById(R.id.add_contact_toolbar);
        if(tB==null){
            logWarning("Tb is Null");
            return;
        }

        tB.setVisibility(View.VISIBLE);
        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setHomeButtonEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setTitle("");
        aB.setSubtitle("");

        relativeLayout = (RelativeLayout) findViewById(R.id.relative_container_add_contact);

        fabButton = (FloatingActionButton) findViewById(R.id.fab_button_next);
        fabButton.setOnClickListener(this);

        mailError = (RelativeLayout) findViewById(R.id.add_contact_email_error);
        mailError.setVisibility(View.GONE);
        typeContactLayout = (RelativeLayout) findViewById(R.id.layout_type_mail);
        typeContactLayout.setVisibility(View.GONE);
        typeContactEditText = (EditText) findViewById(R.id.type_mail_edit_text);
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LinearLayout.LayoutParams params1 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px2dp(40, outMetrics));
            typeContactLayout.setLayoutParams(params1);
        }
        editTextBackground = typeContactEditText.getBackground().mutate().getConstantState().newDrawable();
        typeContactEditText.addTextChangedListener(this);
        typeContactEditText.setOnEditorActionListener(this);
        typeContactEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        typeContactEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (searchExpand) {
                        if (searchAutoComplete != null) {
                            searchAutoComplete.getText().clear();
                        }
                        if (searchMenuItem != null) {
                            searchMenuItem.collapseActionView();
                        }
                        if (isAsyncTaskRunning(filterContactsTask)) {
                            filterContactsTask.cancel(true);
                        }
                        filterContactsTask = new FilterContactsTask();
                        filterContactsTask.execute();
                    }
                }
            }
        });
        scanQRButton = (RelativeLayout) findViewById(R.id.layout_scan_qr);
        scanQRButton.setOnClickListener(this);
        scanQRButton.setVisibility(View.GONE);
        inviteContactButton = (RelativeLayout) findViewById(R.id.layout_invite_contact);
        inviteContactButton.setOnClickListener(this);
        inviteContactButton.setVisibility(View.GONE);
        newGroupChatButton = (RelativeLayout) findViewById(R.id.layout_group_chat);
        newGroupChatButton.setOnClickListener(this);
        newGroupChatButton.setVisibility(View.GONE);
        newChatLinkButton = (RelativeLayout) findViewById(R.id.layout_chat_link);
        newChatLinkButton.setOnClickListener(this);
        newChatLinkButton.setVisibility(View.GONE);
        addContactsLayout = (LinearLayout) findViewById(R.id.add_contacts_container);
        addedContactsRecyclerView = (RecyclerView) findViewById(R.id.contact_adds_recycler_view);
        containerAddedContactsRecyclerView = (RelativeLayout) findViewById(R.id.contacts_adds_container);
        containerAddedContactsRecyclerView.setVisibility(View.GONE);
        fabImageGroup = (FloatingActionButton) findViewById(R.id.image_group_floating_button);
        nameGroup = findViewById(R.id.name_group_edittext);
        nameGroup.setEmojiSize(px2dp(EMOJI_SIZE_SMALL, outMetrics));
        nameGroup.setFilters(new InputFilter[]{new InputFilter.LengthFilter(MAX_ALLOWED_CHARACTERS_AND_EMOJIS)});

        mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        addedContactsRecyclerView.setLayoutManager(mLayoutManager);
        addedContactsRecyclerView.setItemAnimator(new DefaultItemAnimator());

        headerContacts = (RelativeLayout) findViewById(R.id.header_list);
        textHeader = (TextView) findViewById(R.id.text_header_list);

        fastScroller = (FastScroller) findViewById(R.id.fastscroll);

        stickyLayoutManager = new TopSnappedStickyLayoutManager(addContactActivityLollipop, this);
        linearLayoutManager = new LinearLayoutManager(this);
        recyclerViewList = (RecyclerView) findViewById(R.id.add_contact_list);
        recyclerViewList.setClipToPadding(false);
        recyclerViewList.setHasFixedSize(true);
        recyclerViewList.addOnItemTouchListener(this);
        recyclerViewList.setItemAnimator(new DefaultItemAnimator());
        fastScroller.setRecyclerView(recyclerViewList);

        if (contactType == CONTACT_TYPE_MEGA) {
            if (!comesFromChat && !newGroup) {
                if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    inviteContactButton.setVisibility(View.VISIBLE);
                }
                newGroupChatButton.setVisibility(View.VISIBLE);
                newChatLinkButton.setVisibility(View.VISIBLE);
            }
            recyclerViewList.setLayoutManager(linearLayoutManager);
            headerContacts.setVisibility(View.VISIBLE);
            textHeader.setText(getString(R.string.section_contacts));
            recyclerViewList.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
        }
        else if(contactType == CONTACT_TYPE_DEVICE) {
            typeContactLayout.setVisibility(View.VISIBLE);
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                scanQRButton.setVisibility(View.VISIBLE);
            }
            recyclerViewList.setLayoutManager(linearLayoutManager);
            headerContacts.setVisibility(View.VISIBLE);
            textHeader.setText(getString(R.string.contacts_phone));
            recyclerViewList.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
        }
        else {
            typeContactLayout.setVisibility(View.VISIBLE);
            if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                scanQRButton.setVisibility(View.VISIBLE);
            }
            recyclerViewList.setLayoutManager(stickyLayoutManager);
            recyclerViewList.addItemDecoration(new HeaderItemDecoration(this, outMetrics));
            headerContacts.setVisibility(View.GONE);
        }

        containerContacts = (RelativeLayout) findViewById(R.id.container_list_contacts);

        emptyImageView = (ImageView) findViewById(R.id.add_contact_list_empty_image);
        emptyTextView = (TextView) findViewById(R.id.add_contact_list_empty_text);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            emptyImageView.setImageResource(R.drawable.ic_empty_contacts);
        }
        else {
            // auto scroll to the bottom to show the invite button
            final ScrollView scrollView = findViewById(R.id.scroller);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    scrollView.fullScroll(View.FOCUS_DOWN);
                }
            }, 100);
            emptyImageView.setImageResource(R.drawable.contacts_empty_landscape);
        }
        emptyTextView.setText(R.string.contacts_list_empty_text_loading_share);
        emptySubTextView = (TextView) findViewById(R.id.add_contact_list_empty_subtext);
        emptyInviteButton = (Button) findViewById(R.id.add_contact_list_empty_invite_button);
        emptyInviteButton.setText(R.string.contact_invite);

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) emptySubTextView.getLayoutParams();
            params1.setMargins(px2dp(34, outMetrics), 0, px2dp(34, outMetrics), 0);
            emptyTextView.setLayoutParams(params1);
            LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) emptyInviteButton.getLayoutParams();
            params2.setMargins(0, px2dp(5, outMetrics), 0, px2dp(32, outMetrics));
            emptyInviteButton.setLayoutParams(params2);
        }

        emptyInviteButton.setOnClickListener(this);

        progressBar = (ProgressBar) findViewById(R.id.add_contact_progress_bar);

        newGroupLayout = (NestedScrollView) findViewById(R.id.new_group_layout);
        newGroupLayout.setVisibility(View.GONE);
        ekrSwitch = (SwitchCompat) findViewById(R.id.ekr_switch);
        ekrSwitch.setOnClickListener(this);
        getChatLinkBox = (CheckBox) findViewById(R.id.get_chat_link_checkbox);
        getChatLinkLayout = (RelativeLayout) findViewById(R.id.get_chat_link_layout);
        newGroupHeaderList = (TextView) findViewById(R.id.new_group_text_header_list);
        newGroupRecyclerView = (RecyclerView) findViewById(R.id.new_group_add_contact_list);
        newGroupRecyclerView.setClipToPadding(false);
        newGroupRecyclerView.setHasFixedSize(true);
        newGroupRecyclerView.addOnItemTouchListener(this);
        newGroupRecyclerView.setItemAnimator(new DefaultItemAnimator());
        newGrouplinearLayoutManager = new LinearLayoutManager(this);
        newGroupRecyclerView.setLayoutManager(newGrouplinearLayoutManager);
        newGroupRecyclerView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));

        //Get MEGA contacts and phone contacts: first name, last name and email
        if (savedInstanceState != null) {
            onNewGroup = savedInstanceState.getBoolean("onNewGroup", onNewGroup);
            isConfirmDeleteShown = savedInstanceState.getBoolean("isConfirmDeleteShown", false);
            confirmDeleteMail = savedInstanceState.getString("confirmDeleteMail");
            comesFromRecent = savedInstanceState.getBoolean(FROM_RECENT, false);
            searchExpand = savedInstanceState.getBoolean("searchExpand", false);
            savedInputString = savedInstanceState.getString("inputString");
            isConfirmAddShown = savedInstanceState.getBoolean("isConfirmAddShown", false);
            confirmAddMail = savedInstanceState.getString("confirmAddMail");
            createNewGroup = savedInstanceState.getBoolean("createNewGroup", false);
            queryPermissions = savedInstanceState.getBoolean("queryPermissions", true);
            createNewChatLink = savedInstanceState.getBoolean("createNewChatLink", false);
            isEKREnabled = savedInstanceState.getBoolean("isEKREnabled", false);
            ekrSwitch.setChecked(isEKREnabled);
            onlyCreateGroup = savedInstanceState.getBoolean("onlyCreateGroup", false);

            if (contactType == CONTACT_TYPE_MEGA || contactType == CONTACT_TYPE_BOTH) {
                savedaddedContacts = savedInstanceState.getStringArrayList("savedaddedContacts");

                if (createNewGroup) {
                    setTitleAB();
                    inviteContactButton.setVisibility(View.GONE);
                    newGroupChatButton.setVisibility(View.GONE);
                    newChatLinkButton.setVisibility(View.GONE);
                }

                if (savedaddedContacts == null && (contactType == CONTACT_TYPE_MEGA || contactType == CONTACT_TYPE_BOTH)) {
                    setAddedAdapterContacts();
                    getContactsTask = new GetContactsTask();
                    getContactsTask.execute();
                }
                else {
                    recoverContactsTask = new RecoverContactsTask();
                    recoverContactsTask.execute();
                }
            }
            else if (contactType == CONTACT_TYPE_DEVICE){
                addedContactsPhone = savedInstanceState.getParcelableArrayList("addedContactsPhone");
                filteredContactsPhone = savedInstanceState.getParcelableArrayList("filteredContactsPhone");
                phoneContacts = savedInstanceState.getParcelableArrayList("phoneContacts");

                setAddedAdapterContacts();

                if (queryPermissions && filteredContactsPhone == null && phoneContacts == null) {
                    queryIfHasReadContactsPermissions();
                }
                else {
                    if (addedContactsPhone.size() == 0) {
                        containerAddedContactsRecyclerView.setVisibility(View.GONE);
                    }
                    else {
                        containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
                    }
                    addedContactsRecyclerView.setAdapter(adapterContacts);

                    if (phoneContacts != null && !phoneContacts.isEmpty()) {
                        if (filteredContactsPhone == null && (addedContactsPhone == null || addedContactsPhone.isEmpty())) {
                            for (int i = 0; i < phoneContacts.size(); i++) {
                                filteredContactsPhone.add(phoneContacts.get(i));
                            }
                        }
                        setPhoneAdapterContacts(filteredContactsPhone);
                    }
                    else if (addedContactsPhone != null && !addedContactsPhone.isEmpty()) {
                        setEmptyStateVisibility(true);
                    }
                    else {
                        setEmptyStateVisibility(true);

                        progressBar.setVisibility(View.VISIBLE);
                        getContactsTask = new GetContactsTask();
                        getContactsTask.execute();
                    }
                }
                setTitleAB();
                setRecyclersVisibility();
            }
        }
        else {
            isEKREnabled = false;
            ekrSwitch.setChecked(isEKREnabled);
            setAddedAdapterContacts();

            if (contactType == CONTACT_TYPE_MEGA) {
                progressBar.setVisibility(View.VISIBLE);
                getContactsTask = new GetContactsTask();
                getContactsTask.execute();
            } else {
                queryIfHasReadContactsPermissions();
            }
        }

        if (onlyCreateGroup) {
            createNewGroup = true;
            setTitleAB();
            inviteContactButton.setVisibility(View.GONE);
            newGroupChatButton.setVisibility(View.GONE);
            newChatLinkButton.setVisibility(View.GONE);

        }

        setGetChatLinkVisibility();

        if (comesFromRecent && !onNewGroup) {
            if (isAsyncTaskRunning(getContactsTask)) {
                getContactsTask.cancel(true);
            }
            createNewChatLink = true;
            newGroup();
        }
    }

    private void setEmptyStateVisibility (boolean visible) {
        if (visible) {
            emptyImageView.setVisibility(View.VISIBLE);
            emptyTextView.setVisibility(View.VISIBLE);
            if (contactType == CONTACT_TYPE_MEGA && (addedContactsMEGA == null || addedContactsMEGA.isEmpty())) {
                emptySubTextView.setVisibility(View.VISIBLE);
                emptyInviteButton.setVisibility(View.VISIBLE);
            }
            else {
                emptySubTextView.setVisibility(View.GONE);
                emptyInviteButton.setVisibility(View.GONE);
            }
        }
        else {
            emptyImageView.setVisibility(View.GONE);
            emptyTextView.setVisibility(View.GONE);
            emptySubTextView.setVisibility(View.GONE);
            emptyInviteButton.setVisibility(View.GONE);
        }
    }

    private void setGetChatLinkVisibility () {
        if (isEKREnabled) {
            getChatLinkLayout.setVisibility(View.GONE);
        } else {
            getChatLinkLayout.setVisibility(View.VISIBLE);
        }
    }

    private void queryIfHasReadContactsPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasReadContactsPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
            if (!hasReadContactsPermission) {
                logWarning("No read contacts permission");
                ActivityCompat.requestPermissions((AddContactActivityLollipop) this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        REQUEST_READ_CONTACTS);
                if (contactType == CONTACT_TYPE_DEVICE) {
                    return;
                }
            }
        }

        if (waitingForPhoneContacts) {
            filteredContactsShare.add(new ShareContactInfo());
            getPhoneContactsTask = new GetPhoneContactsTask();
            getPhoneContactsTask.execute();
            return;
        }

        setEmptyStateVisibility(true);
        progressBar.setVisibility(View.VISIBLE);
        getContactsTask = new GetContactsTask();
        getContactsTask.execute();

    }

    private void setTitleAB() {
        logDebug("setTitleAB");
        if (aB != null) {
            if (contactType == CONTACT_TYPE_MEGA){
                if (comesFromChat) {
                    aB.setTitle(title.toUpperCase());
                    if (addedContactsMEGA.size() > 0) {
                        aB.setSubtitle(getResources().getString(R.string.selected_items, addedContactsMEGA.size()));
                    }
                    else {
                        aB.setSubtitle(null);
                    }
                }
                else if (!createNewGroup && !createNewChatLink) {
                    aB.setTitle(getString(R.string.group_chat_start_conversation_label).toUpperCase());
                }
                else if ((createNewGroup || createNewChatLink) && !onNewGroup) {
                    if (onlyCreateGroup) {
                        aB.setTitle(getString(R.string.title_new_group));
                    }
                    else {
                        aB.setTitle(getString(R.string.group_chat_start_conversation_label));
                    }
                    if (addedContactsMEGA.size() > 0) {
                        aB.setSubtitle(getResources().getString(R.string.selected_items, addedContactsMEGA.size()));
                    }
                    else {
                        aB.setSubtitle(getString(R.string.add_participants_menu_item));
                    }
                }
            }
            else if (contactType == CONTACT_TYPE_DEVICE){
                aB.setTitle(getString(R.string.invite_contacts).toUpperCase());
                if (addedContactsPhone.size() > 0){
                    aB.setSubtitle(addedContactsPhone.size() + " " + getResources().getQuantityString(R.plurals.general_num_contacts, addedContactsPhone.size()));
                }
                else {
                    aB.setSubtitle(null);
                }
            }
            else {
                aB.setTitle(getString(R.string.share_with).toUpperCase());
                if (addedContactsShare.size() > 0){
                    aB.setSubtitle(addedContactsShare.size() + " " + getResources().getQuantityString(R.plurals.general_num_contacts, addedContactsShare.size()));
                }
                else {
                    aB.setSubtitle(null);
                }
            }
        }
    }

    private void setError(){
        logDebug("setError");
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(px2dp(18, outMetrics), px2dp(-10, outMetrics), px2dp(18, outMetrics), 0);
            typeContactEditText.setLayoutParams(params);
        }
        mailError.setVisibility(View.VISIBLE);
        PorterDuffColorFilter porterDuffColorFilter = new PorterDuffColorFilter(ContextCompat.getColor(this, R.color.login_warning), PorterDuff.Mode.SRC_ATOP);
        Drawable background = editTextBackground.mutate().getConstantState().newDrawable();
        background.setColorFilter(porterDuffColorFilter);
        typeContactEditText.setBackground(background);
    }

    private void quitError(){
        logDebug("quitError");
        if(mailError.getVisibility() != View.GONE){
            mailError.setVisibility(View.GONE);
        }
        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(px2dp(18, outMetrics), px2dp(0, outMetrics), px2dp(18, outMetrics), 0);
            typeContactEditText.setLayoutParams(params);
        }
        typeContactEditText.setBackground(editTextBackground);
    }

    private void addShareContact (ShareContactInfo contact) {
        logDebug("addShareContact");

        if (searchExpand && searchMenuItem != null) {
            searchMenuItem.collapseActionView();
        }
        if (!typeContactEditText.getText().toString().equals("")){
            typeContactEditText.getText().clear();
        }
        typeContactEditText.clearFocus();
        hideKeyboard(addContactActivityLollipop, 0);

        boolean found = false;
        for (int i=0; i<addedContactsShare.size(); i++) {
            found = false;
            if (getShareContactMail(addedContactsShare.get(i)).equals(getShareContactMail(contact))) {
                found = true;
                break;
            }
        }
        if (found){
            showSnackbar(getString(R.string.contact_not_added));
        }
        else {
            addedContactsShare.add(contact);
        }
        adapterShare.setContacts(addedContactsShare);
        if (adapterShare.getItemCount()-1 >= 0) {
            mLayoutManager.scrollToPosition(adapterShare.getItemCount() - 1);
        }
        setSendInvitationVisibility();
        containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
        setTitleAB();

        if (adapterShareHeader != null){
            if (adapterShareHeader.getItemCount() == 0){
                setEmptyStateVisibility(true);

                String textToShow = String.format(getString(R.string.context_empty_contacts)).toUpperCase();
                try{
                    textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                }
                catch (Exception e){}
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);
            }
            else {
                setEmptyStateVisibility(false);
            }
        }
        setRecyclersVisibility();
        refreshKeyboard();
    }

    private void addContactMEGA (MegaContactAdapter contact) {
        logDebug("Contact: " + contact.getFullName());

        if (searchExpand && searchMenuItem != null) {
            searchMenuItem.collapseActionView();
        }
        hideKeyboard(addContactActivityLollipop, 0);

        if (addedContactsMEGA.contains(contact)){
            showSnackbar(getString(R.string.contact_not_added));
        }
        else {
            addedContactsMEGA.add(contact);
        }
        adapterMEGAContacts.setContacts(addedContactsMEGA);
        if (adapterMEGAContacts.getItemCount()-1 >= 0) {
            mLayoutManager.scrollToPosition(adapterMEGAContacts.getItemCount() - 1);
        }
        setSendInvitationVisibility();
        containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
        setTitleAB();
        if (adapterMEGA != null){
            if (adapterMEGA.getItemCount() == 1){
                headerContacts.setVisibility(View.GONE);
                setEmptyStateVisibility(true);

                String textToShow = String.format(getString(R.string.context_empty_contacts)).toUpperCase();
                try{
                    textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                }
                catch (Exception e){}
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);
            }
        }
        setRecyclersVisibility();
        refreshKeyboard();
    }

    private void addContact (PhoneContactInfo contact){
        logDebug("Contact: " + contact.getName()+", Mail: " + contact.getEmail());

        if (searchExpand && searchMenuItem != null) {
            searchMenuItem.collapseActionView();
        }
        if (!typeContactEditText.getText().toString().equals("")){
            typeContactEditText.getText().clear();
        }
        typeContactEditText.clearFocus();
        hideKeyboard(addContactActivityLollipop, 0);

        boolean found = false;
        for (int i=0; i<addedContactsPhone.size(); i++) {
            found = false;
            if (addedContactsPhone.get(i).getEmail().equals(contact.getEmail())) {
                found = true;
                break;
            }
        }
        if (found){
            showSnackbar(getString(R.string.contact_not_added));
        }
        else {
            addedContactsPhone.add(contact);
        }

        adapterContacts.setContacts(addedContactsPhone);
        if (adapterContacts.getItemCount()-1 >= 0) {
            mLayoutManager.scrollToPosition(adapterContacts.getItemCount() - 1);
        }
        setSendInvitationVisibility();
        containerAddedContactsRecyclerView.setVisibility(View.VISIBLE);
        setTitleAB();

        if(adapterPhone!=null){
            if (adapterPhone.getItemCount() == 0){
                headerContacts.setVisibility(View.GONE);
                setEmptyStateVisibility(true);

                String textToShow = String.format(getString(R.string.context_empty_contacts)).toUpperCase();
                try{
                    textToShow = textToShow.replace("[A]", "<font color=\'#000000\'>");
                    textToShow = textToShow.replace("[/A]", "</font>");
                    textToShow = textToShow.replace("[B]", "<font color=\'#7a7a7a\'>");
                    textToShow = textToShow.replace("[/B]", "</font>");
                }
                catch (Exception e){}
                Spanned result = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    result = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
                } else {
                    result = Html.fromHtml(textToShow);
                }
                emptyTextView.setText(result);
            }
        }
        setRecyclersVisibility();
        refreshKeyboard();
    }

    public void deleteContact (int position){
        logDebug("Position: " + position);
        if (position < 0) {
            return;
        }
        if (contactType == CONTACT_TYPE_MEGA){
            if (position >= addedContactsMEGA.size()) {
                return;
            }
            MegaContactAdapter deleteContact = addedContactsMEGA.get(position);
            if (deleteContact.getMegaUser() != null || deleteContact.getMegaContactDB() != null) {
                addMEGAFilteredContact(deleteContact);
            }
            addedContactsMEGA.remove(deleteContact);
            setSendInvitationVisibility();
            adapterMEGAContacts.setContacts(addedContactsMEGA);
            if (addedContactsMEGA.size() == 0){
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            }
        }
        else if (contactType == CONTACT_TYPE_DEVICE){
            if (position >= addedContactsPhone.size()) {
                return;
            }
            PhoneContactInfo deleteContact = addedContactsPhone.get(position);
            if (deleteContact.getName() != null) {
                addFilteredContact(deleteContact);
            }
            addedContactsPhone.remove(deleteContact);
            setSendInvitationVisibility();
            adapterContacts.setContacts(addedContactsPhone);
            if (addedContactsPhone.size() == 0){
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            }
        }
        else {
            if (position >= addedContactsShare.size()) {
                return;
            }
            ShareContactInfo deleteContact = addedContactsShare.get(position);

            if (deleteContact.isPhoneContact()) {
                addFilteredContact(deleteContact.getPhoneContactInfo());
            }
            else if (deleteContact.isMegaContact()) {
                addMEGAFilteredContact(deleteContact.getMegaContactAdapter());
            }

            addedContactsShare.remove(deleteContact);
            setSendInvitationVisibility();
            adapterShare.setContacts(addedContactsShare);
            if (addedContactsShare.size() == 0){
                containerAddedContactsRecyclerView.setVisibility(View.GONE);
            }
        }
        setTitleAB();
        setRecyclersVisibility();
        refreshKeyboard();
        setSearchVisibility();
    }

    public void showSnackbar(String message) {
        hideKeyboard(addContactActivityLollipop, 0);
        showSnackbar(relativeLayout, message);
    }

    private void addMEGAFilteredContact (MegaContactAdapter contact) {

        filteredContactMEGA.add(contact);
        Collections.sort(filteredContactMEGA, new Comparator<MegaContactAdapter>(){

            public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
                String name1 = c1.getFullName();
                String name2 = c2.getFullName();
                int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                if (res == 0) {
                    res = name1.compareTo(name2);
                }
                return res;
            }
        });

        int index = filteredContactMEGA.indexOf(contact);
        if (searchExpand) {
            if (searchAutoComplete !=  null) {
                inputString = searchAutoComplete.getText().toString();
            }
        }
        else {
            inputString = typeContactEditText.getText().toString();
        }

        if (contactType == CONTACT_TYPE_BOTH) {
            int i = filteredContactMEGA.indexOf(contact);
            ShareContactInfo contactToAdd = new ShareContactInfo(null, contact, null);
            if (filteredContactMEGA.size() == 1) {
                filteredContactsShare.add(0, new ShareContactInfoHeader(true, true, false));
                filteredContactsShare.add(1, contactToAdd);
            }
            else {
                filteredContactsShare.add(i+1, contactToAdd);
            }
            if (inputString != null && !inputString.equals("")) {
                filterContactsTask = new FilterContactsTask();
                filterContactsTask.execute();
            }
            else {
                adapterShareHeader.setContacts(filteredContactsShare);
                if (index >= 0 && index < adapterShareHeader.getItemCount()) {
                    stickyLayoutManager.scrollToPosition(index);
                }
            }
            if (adapterShareHeader.getItemCount() != 0) {
                setEmptyStateVisibility(false);
            }
        }
        else {
            if (!onNewGroup) {
                if (inputString != null && !inputString.equals("")) {
                    filterContactsTask = new FilterContactsTask();
                    filterContactsTask.execute();
                }
                else {
                    adapterMEGA.setContacts(filteredContactMEGA);
                    if (index >= 0 && index < adapterMEGA.getItemCount()) {
                        linearLayoutManager.scrollToPosition(index);
                    }
                    if (adapterMEGA.getItemCount() != 0) {
                        headerContacts.setVisibility(View.VISIBLE);
                        setEmptyStateVisibility(false);
                    }
                }
            }
        }
    }

    private void addFilteredContact(PhoneContactInfo contact) {
        logDebug("addFilteredContact");
        filteredContactsPhone.add(contact);
        Collections.sort(filteredContactsPhone);
        int index = filteredContactsPhone.indexOf(contact);
        int position;

        logDebug("Size filteredContactsPhone: " + filteredContactsPhone.size());

        if (searchExpand) {
            if (searchAutoComplete != null) {
                inputString = searchAutoComplete.getText().toString();
            }
        }
        else {
            inputString = typeContactEditText.getText().toString();
        }

        if (contactType == CONTACT_TYPE_BOTH) {
            if (filteredContactsPhone.size() == 1){
                filteredContactsShare.add(filteredContactsShare.size(), new ShareContactInfoHeader(true, false, true));
                filteredContactsShare.add(filteredContactsShare.size(), new ShareContactInfo(contact, null, null));
                position = filteredContactsShare.size();
            }
            else {
                position = (adapterShareHeader.getItemCount()-filteredContactsPhone.size())+index;
                if (position > 0 && (position+1 <= filteredContactsShare.size())) {
                    filteredContactsShare.add(position + 1, new ShareContactInfo(contact, null, null));
                }
            }
            if (inputString != null && !inputString.equals("")) {
                filterContactsTask = new FilterContactsTask();
                filterContactsTask.execute();
            }
            else {
                adapterShareHeader.setContacts(filteredContactsShare);
                if (position >= 0 && position < adapterShareHeader.getItemCount()) {
                    stickyLayoutManager.scrollToPosition(position);
                }
            }
            if (adapterShareHeader.getItemCount() != 0) {
                setEmptyStateVisibility(false);
            }
        }
        else {
            if (inputString != null && !inputString.equals("")) {
                filterContactsTask = new FilterContactsTask();
                filterContactsTask.execute();
            }
            else {
                adapterPhone.setContacts(filteredContactsPhone);
                if (index >= 0 && index < adapterPhone.getItemCount()) {
                    linearLayoutManager.scrollToPosition(index);
                }
                if(adapterPhone!=null){
                    if (adapterPhone.getItemCount() != 0){
                        headerContacts.setVisibility(View.VISIBLE);
                        setEmptyStateVisibility(false);
                    }
                }
            }
        }
    }

    private void getVisibleMEGAContacts () {
        contactsMEGA = megaApi.getContacts();
        visibleContactsMEGA.clear();
        filteredContactMEGA.clear();
        addedContactsMEGA.clear();

        if(chatId!=-1){
            logDebug("Add participant to chat");
            if(megaChatApi!=null){
                MegaChatRoom chat = megaChatApi.getChatRoom(chatId);
                if(chat!=null){
                    long participantsCount = chat.getPeerCount();

                    for (int i=0;i<contactsMEGA.size();i++){
                        if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                            boolean found = false;

                            for(int j=0;j<participantsCount;j++) {

                                long peerHandle = chat.getPeerHandle(j);

                                if(peerHandle == contactsMEGA.get(i).getHandle()){
                                    found = true;
                                    break;
                                }
                            }

                            if(!found){
                                MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                                String fullName = "";
                                if(contactDB!=null){
                                    ContactController cC = new ContactController(this);
                                    fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                                }
                                else{
                                    //No name, ask for it and later refresh!!
                                    fullName = contactsMEGA.get(i).getEmail();
                                }

                                logDebug("Added to list: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
                                MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);
                                visibleContactsMEGA.add(megaContactAdapter);
                            }
                            else{
                                MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                                String fullName = "";
                                if(contactDB!=null){
                                    ContactController cC = new ContactController(this);
                                    fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                                }
                                else{
                                    //No name, ask for it and later refresh!!
                                    fullName = contactsMEGA.get(i).getEmail();
                                }

                                logDebug("Removed from list - already included on chat: " + fullName);
                            }
                        }
                    }
                }
                else{
                    for (int i=0;i<contactsMEGA.size();i++){
                        logDebug("Contact: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
                        if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                            MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                            String fullName = "";
                            if(contactDB!=null){
                                ContactController cC = new ContactController(this);
                                fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                            }
                            else{
                                //No name, ask for it and later refresh!!
                                fullName = contactsMEGA.get(i).getEmail();
                            }

                            MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);
                            visibleContactsMEGA.add(megaContactAdapter);
                        }
                    }
                }
            }
        }
        else if ((multipleSelectIntent == 0 && nodeHandle != -1) || (multipleSelectIntent == 1 && nodeHandles.length == 1)) {
            ArrayList<MegaShare> shared = new ArrayList<>();
            if (multipleSelectIntent == 0) {
                shared.addAll(megaApi.getOutShares(megaApi.getNodeByHandle(nodeHandle)));
            }
            else {
                shared.addAll(megaApi.getOutShares(megaApi.getNodeByHandle(nodeHandles[0])));
            }
            boolean found;
            for (int i=0;i<contactsMEGA.size();i++){
                found = false;
                logDebug("Contact: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
                if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                    MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                    String fullName = "";
                    if(contactDB!=null){
                        ContactController cC = new ContactController(this);
                        fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                    }
                    else {
                        //No name, ask for it and later refresh!!
                        fullName = contactsMEGA.get(i).getEmail();
                    }

                    MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);

                    for (int j=0; j<shared.size(); j++) {
                        if (getMegaContactMail(megaContactAdapter).equals(shared.get(j).getUser())){
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        visibleContactsMEGA.add(megaContactAdapter);
                    }

                }
            }
        }
        else{
            for (int i=0;i<contactsMEGA.size();i++){
                logDebug("Contact: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
                if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                    MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                    String fullName = "";
                    if(contactDB!=null){
                        ContactController cC = new ContactController(this);
                        fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                    }
                    else{
                        //No name, ask for it and later refresh!!
                        fullName = contactsMEGA.get(i).getEmail();
                    }

                    MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);
                    visibleContactsMEGA.add(megaContactAdapter);
                }
            }
        }

        Collections.sort(visibleContactsMEGA, new Comparator<MegaContactAdapter>(){

            public int compare(MegaContactAdapter c1, MegaContactAdapter c2) {
                String name1 = c1.getFullName();
                String name2 = c2.getFullName();
                int res = String.CASE_INSENSITIVE_ORDER.compare(name1, name2);
                if (res == 0) {
                    res = name1.compareTo(name2);
                }
                return res;
            }
        });

        long handle;
        for (int i= 0; i<visibleContactsMEGA.size(); i++){
            filteredContactMEGA.add(visibleContactsMEGA.get(i));
            if (contactType == CONTACT_TYPE_MEGA) {
                //Ask for presence info and last green
                handle = getMegaContactHandle(visibleContactsMEGA.get(i));
                if (handle != -1) {
                    int userStatus = megaChatApi.getUserOnlineStatus(handle);
                    if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
                        logDebug("Request last green for user");
                        megaChatApi.requestLastGreen(handle, null);
                    }
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    //Get the contacts explicitly added
    private ArrayList<PhoneContactInfo> getPhoneContacts() {
        logDebug("getPhoneContacts");
        ArrayList<PhoneContactInfo> contactList = new ArrayList<>();
        logDebug("inputString empty");
        String filter = ContactsContract.CommonDataKinds.Email.DATA + " NOT LIKE ''  AND " + ContactsContract.Contacts.IN_VISIBLE_GROUP + "=1";

        try {
            ContentResolver cr = getBaseContext().getContentResolver();
            String SORT_ORDER = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? ContactsContract.Contacts.SORT_KEY_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME;

            Cursor c = cr.query(
                    ContactsContract.Data.CONTENT_URI,
                    null,filter,
                    null, SORT_ORDER);

            while (c.moveToNext()){
                long id = c.getLong(c.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                String name = c.getString(c.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
                String emailAddress = c.getString(c.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                logDebug("ID: " + id + "___ NAME: " + name + "____ EMAIL: " + emailAddress);

                if ((!emailAddress.equalsIgnoreCase("")) && (emailAddress.contains("@")) && (!emailAddress.contains("s.whatsapp.net"))) {
                    PhoneContactInfo contactPhone = new PhoneContactInfo(id, name, emailAddress, null);
                    contactList.add(contactPhone);
                }
            }
            c.close();

            logDebug("contactList.size() = " + contactList.size());

            return contactList;

        } catch (Exception e) {
            logError("Exception", e);
        }

        return null;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        quitError();
        refreshKeyboard();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        logDebug("onTextChanged: " + s.toString() + "_ " + start + "__" + before + "__" + count);
        if (contactType == CONTACT_TYPE_DEVICE){
            if (s != null) {
                if (s.length() > 0) {
                    String temp = s.toString();
                    char last = s.charAt(s.length()-1);
                    if(last == ' '){
                        boolean isValid = isValidEmail(temp.trim());
                        if(isValid){
                            confirmAddMail = temp.trim();
                            queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask();
                            queryIfContactSouldBeAddedTask.execute(false);
                            typeContactEditText.getText().clear();
                        }
                        else{
                            setError();
                        }
                        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            hideKeyboard(addContactActivityLollipop, 0);
                        }
                    }
                    else{
                        logDebug("Last character is: " + last);
                    }
                }
            }
        }
        else if (contactType == CONTACT_TYPE_BOTH){
            if (s != null) {
                if (s.length() > 0) {
                    String temp = s.toString();
                    char last = s.charAt(s.length()-1);
                    if (last == ' '){
                        boolean isValid = isValidEmail(temp.trim());
                        if (isValid){
                            confirmAddMail = temp.trim();
                            queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask();
                            queryIfContactSouldBeAddedTask.execute(false);
                            typeContactEditText.getText().clear();
                        }
                        else {
                            setError();
                        }
                        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                            hideKeyboard(addContactActivityLollipop, 0);
                        }
                    }
                    else{
                        logDebug("Last character is: " + last);
                    }
                }
            }
        }

        if (isAsyncTaskRunning(filterContactsTask)){
            filterContactsTask.cancel(true);
        }
        filterContactsTask = new FilterContactsTask();
        filterContactsTask.execute();
        refreshKeyboard();
    }

    @Override
    public void afterTextChanged(Editable editable) {
        refreshKeyboard();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        refreshKeyboard();
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            String s = v.getText().toString();
            logDebug("s: " + s);
            if (s.isEmpty() || s.equals("null") || s.equals("")) {
                hideKeyboard(addContactActivityLollipop, 0);
            }
            else {
                if (contactType == CONTACT_TYPE_DEVICE) {
                    boolean isValid = isValidEmail(s.trim());
                    if (isValid) {
                        confirmAddMail = s.trim();
                        queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask();
                        queryIfContactSouldBeAddedTask.execute(false);
                        typeContactEditText.getText().clear();
                        hideKeyboard(addContactActivityLollipop, 0);
                    }
                    else {
                        setError();
                    }
                    if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        hideKeyboard(addContactActivityLollipop, 0);
                    }
                }
                else if (contactType == CONTACT_TYPE_BOTH){
                    boolean isValid = isValidEmail(s.trim());
                    if (isValid) {
                        confirmAddMail = s.trim();
                        queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask();
                        queryIfContactSouldBeAddedTask.execute(false);
                        typeContactEditText.getText().clear();
                        hideKeyboard(addContactActivityLollipop, 0);
                    }
                    else {
                        setError();
                    }
                    if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                        hideKeyboard(addContactActivityLollipop, 0);
                    }
                }
                if (isAsyncTaskRunning(filterContactsTask)){
                    filterContactsTask.cancel(true);
                }
                filterContactsTask = new FilterContactsTask();
                filterContactsTask.execute();
            }
            return true;
        }
        if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_SEND)) {
            if (contactType == CONTACT_TYPE_DEVICE){
                if (addedContactsPhone.isEmpty() || addedContactsPhone == null) {
                    hideKeyboard(addContactActivityLollipop, 0);
                }
                else {
                    inviteContacts(addedContactsPhone);
                }
            }
            else if (contactType == CONTACT_TYPE_MEGA){
                if (addedContactsMEGA.isEmpty() || addedContactsMEGA == null) {
                    hideKeyboard(addContactActivityLollipop, 0);
                }
                else {
                    setResultContacts(addedContactsMEGA, true);
                }
            }
            else {
                if (addedContactsShare.isEmpty() || addedContactsShare == null) {
                    hideKeyboard(addContactActivityLollipop, 0);
                }
                else {
                    shareWith(addedContactsShare);
                }
            }
            return true;
        }
        return false;
    }

    public void itemClick(String email, int adapter){

        logDebug("itemClick");

        if (contactType == CONTACT_TYPE_MEGA) {
            if (createNewGroup || createNewChatLink || comesFromChat) {
                if (adapter == MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT) {
                    if (searchExpand) {
                        if (searchAutoComplete != null) {
                            inputString = searchAutoComplete.getText().toString();
                        }
                    }
                    else {
                        inputString = typeContactEditText.getText().toString();
                    }
                    if (inputString != null && !inputString.equals("")) {
                        for (int i = 0; i < queryContactMEGA.size(); i++) {
                            if (getMegaContactMail(queryContactMEGA.get(i)).equals(email)) {
                                addContactMEGA(queryContactMEGA.get(i));
                                filteredContactMEGA.remove(queryContactMEGA.get(i));
                                queryContactMEGA.remove(i);
                                adapterMEGA.setContacts(queryContactMEGA);
                                break;
                            }
                        }
                    }
                    else {
                        for (int i = 0; i < filteredContactMEGA.size(); i++) {
                            if (getMegaContactMail(filteredContactMEGA.get(i)).equals(email)) {
                                addContactMEGA(filteredContactMEGA.get(i));
                                filteredContactMEGA.remove(i);
                                adapterMEGA.setContacts(filteredContactMEGA);
                                break;
                            }
                        }
                    }

                    if (addedContactsMEGA.size() == 0) {
                        setSendInvitationVisibility();
                    }
                }
                else {
                    for (int i = 0; i < addedContactsMEGA.size(); i++) {
                        if (getMegaContactMail(addedContactsMEGA.get(i)).equals(email)) {
                            showConfirmationDeleteFromChat(addedContactsMEGA.get(i));
                            break;
                        }
                    }
                }
            }
            else {
                ArrayList<String> contacts = new ArrayList<>();
                contacts.add(email);
                startConversation(contacts, true, null);
            }
        }
        setSearchVisibility();
    }

    private void showConfirmationDeleteFromChat (final MegaContactAdapter contact) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyleAddContacts);

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case DialogInterface.BUTTON_POSITIVE: {
                        addMEGAFilteredContact(contact);
                        addedContactsMEGA.remove(contact);
                        newGroupHeaderList.setText(getResources().getQuantityString(R.plurals.subtitle_of_group_chat, addedContactsMEGA.size(), addedContactsMEGA.size()));
                        adapterMEGA.setContacts(addedContactsMEGA);
                        adapterMEGAContacts.setContacts(addedContactsMEGA);

                        break;
                    }

                    case DialogInterface.BUTTON_NEGATIVE: {
                        //No button clicked
                        isConfirmDeleteShown = false;
                        break;
                    }
                }
            }
        };

        confirmDeleteMail = getMegaContactMail(contact);
        builder.setMessage(getString(R.string.confirmation_delete_contact, contact.getFullName()));
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                isConfirmDeleteShown = false;
            }
        });
        builder.setPositiveButton(R.string.context_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
        isConfirmDeleteShown = true;
    }

    private void itemClick(View view, int position) {
        logDebug("on item click");
        if (searchExpand) {
            if (searchAutoComplete !=  null) {
                inputString = searchAutoComplete.getText().toString();
            }
        }
        else {
            inputString = typeContactEditText.getText().toString();
        }
        if (contactType == CONTACT_TYPE_DEVICE){

            if(adapterPhone==null){
                return;
            }

            final PhoneContactInfo contact = adapterPhone.getItem(position);
            if(contact == null) {
                return;
            }

            if (inputString != null && !inputString.equals("")) {
                for (int i=0; i<queryContactsPhone.size(); i++) {
                    if (queryContactsPhone.get(i).getEmail().equals(contact.getEmail())) {
                        filteredContactsPhone.remove(queryContactsPhone.get(i));
                        queryContactsPhone.remove(i);
                        adapterPhone.setContacts(queryContactsPhone);
                    }
                }
            }
            else {
                for (int i = 0; i < filteredContactsPhone.size(); i++) {
                    if (filteredContactsPhone.get(i).getEmail().equals(contact.getEmail())) {
                        filteredContactsPhone.remove(i);
                        adapterPhone.setContacts(filteredContactsPhone);
                        break;
                    }
                }
            }
            addContact(contact);
        }
        else if (contactType == CONTACT_TYPE_BOTH){
            if(adapterShareHeader==null){
                return;
            }

            final ShareContactInfo contact = adapterShareHeader.getItem(position);
            if(contact == null || contact.isHeader() || contact.isProgress()) {
                return;
            }

            if (contact.isMegaContact()) {
                if (filteredContactMEGA.size() == 1) {
                    filteredContactsShare.remove(0);
                }
                filteredContactMEGA.remove(contact.getMegaContactAdapter());
            }
            else if (contact.isPhoneContact()) {
                filteredContactsPhone.remove(contact.getPhoneContactInfo());
                if (filteredContactsPhone.size() == 0) {
                    filteredContactsShare.remove(filteredContactsShare.size() - 2);
                }
            }
            filteredContactsShare.remove(contact);
            if (inputString != null && !inputString.equals("")) {
                filterContactsTask = new FilterContactsTask();
                filterContactsTask.execute();
            }
            else {
                adapterShareHeader.setContacts(filteredContactsShare);
            }

            addShareContact(contact);
        }
        setSearchVisibility();
    }

    public String getShareContactMail(ShareContactInfo contact) {
        String mail = null;

        if (contact.isMegaContact() && !contact.isHeader()) {
            if (contact.getMegaContactAdapter().getMegaUser() != null && contact.getMegaContactAdapter().getMegaUser().getEmail() != null) {
                mail = contact.getMegaContactAdapter().getMegaUser().getEmail();
            } else if (contact.getMegaContactAdapter().getMegaContactDB() != null && contact.getMegaContactAdapter().getMegaContactDB().getMail() != null) {
                mail = contact.getMegaContactAdapter().getMegaContactDB().getMail();
            }
        }
        else if (contact.isPhoneContact() && !contact.isHeader()){
            mail = contact.getPhoneContactInfo().getEmail();
        }
        else{
            mail = contact.getMail();
        }

        return mail;
    }

    private String getMegaContactMail (MegaContactAdapter contact) {
        String mail = null;
        if (contact != null) {
            if (contact.getMegaUser() != null && contact.getMegaUser().getEmail() != null) {
                mail = contact.getMegaUser().getEmail();
            }
            else if (contact.getMegaContactDB() != null && contact.getMegaContactDB().getMail() != null) {
                mail = contact.getMegaContactDB().getMail();
            }
        }
        return mail;
    }

    private long getMegaContactHandle (MegaContactAdapter contact) {
        long handle = -1;
        if (contact != null) {
            if (contact.getMegaUser() != null && contact.getMegaUser().getHandle() != -1) {
                handle = contact.getMegaUser().getHandle();
            }
            else if (contact.getMegaContactDB() != null && contact.getMegaContactDB().getMail() != null) {
                handle = Long.parseLong(contact.getMegaContactDB().getHandle());
            }
        }
        return handle;
    }

    private void toInviteContact() {
        Intent in = new Intent(this, InviteContactActivity.class);
        in.putExtra("contactType", CONTACT_TYPE_DEVICE);
        startActivityForResult(in, REQUEST_INVITE_CONTACT_FROM_DEVICE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_scan_qr: {
                logDebug("Scan QR code pressed");
                initScanQR();
                break;
            }
            case R.id.add_contact_list_empty_invite_button:
            case R.id.layout_invite_contact: {
                toInviteContact();
                break;
            }
            case R.id.layout_group_chat: {
                logDebug("New group chat pressed");
                createNewGroup = true;
                setNextLayout();
                break;
            }
            case R.id.ekr_switch: {
                isEKREnabled = ekrSwitch.isChecked();
                setGetChatLinkVisibility();
                break;
            }
            case R.id.layout_chat_link: {
                createNewChatLink = true;
                newGroup();
                break;
            }
            case R.id.fab_button_next: {
                if (contactType == CONTACT_TYPE_DEVICE){
                    inviteContacts(addedContactsPhone);
                }
                else if (contactType == CONTACT_TYPE_MEGA){
                    if (onlyCreateGroup && addedContactsMEGA.isEmpty()) {
                        showSnackbar(getString(R.string.error_creating_group_and_attaching_file));
                        break;
                    }

                    setResultContacts(addedContactsMEGA, true);
                }
                else {
                    shareWith(addedContactsShare);
                }
                hideKeyboard(this, 0);
                break;
            }
        }
    }

    private void setNextLayout() {
        inviteContactMenuItem.setVisible(false);
        if (visibleContactsMEGA != null && !visibleContactsMEGA.isEmpty()) {
            setSendInvitationVisibility();
            setTitleAB();
            inviteContactButton.setVisibility(View.GONE);
            newGroupChatButton.setVisibility(View.GONE);
            newChatLinkButton.setVisibility(View.GONE);
        }
        else {
            setResultContacts(addedContactsMEGA, true);
        }
    }

    private void initScanQR() {
        Intent intent = new Intent(this, QRCodeActivity.class);
        intent.putExtra("inviteContacts", true);
        startActivityForResult(intent, SCAN_QR_FOR_ADD_CONTACTS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        logDebug("onActivityResult");
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == SCAN_QR_FOR_ADD_CONTACTS && resultCode == Activity.RESULT_OK && intent != null) {
            String mail = intent.getStringExtra("mail");

            if (mail != null && !mail.equals("")) {
                confirmAddMail = mail;
                queryIfContactSouldBeAddedTask = new QueryIfContactSouldBeAddedTask();
                queryIfContactSouldBeAddedTask.execute(true);
            }
        }
        else if (requestCode == REQUEST_INVITE_CONTACT_FROM_DEVICE && resultCode == RESULT_OK) {
            logDebug("REQUEST_INVITE_CONTACT_FROM_DEVICE OK");

            if (intent == null) {
                logWarning("Return.....");
                return;
            }

            final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivityLollipop.EXTRA_CONTACTS);

            if (contactsData != null){
                ContactController cC = new ContactController(this);
                cC.inviteMultipleContacts(contactsData);
            }
        }
    }

    @Override
    public void onBackPressed() {
        retryConnectionsAndSignalPresence();

        if (onNewGroup) {
            if (addedContactsMEGA.contains(myContact)) {
                addedContactsMEGA.remove(myContact);
            }
            if (comesFromRecent) {
                finish();
            } else {
                returnToAddContacts();
                createMyContact();
            }
        } else if (createNewGroup && (newGroup || onlyCreateGroup)) {
            finish();
        } else if ((createNewGroup || createNewChatLink) && (!newGroup || !onlyCreateGroup)) {
            returnToStartConversation();
        } else {
            super.onBackPressed();
        }
    }

    private void setResultContacts(ArrayList<MegaContactAdapter> addedContacts, boolean megaContacts){
        logDebug("setResultContacts");
        ArrayList<String> contactsSelected = new ArrayList<String>();
        String contactEmail;

        if (addedContacts != null) {
            for (int i = 0; i < addedContacts.size(); i++) {
                if (addedContacts.get(i).getMegaUser() != null && addedContacts.get(i).getMegaContactDB() != null) {
                    contactEmail = addedContacts.get(i).getMegaUser().getEmail();
                }
                else {
                    contactEmail = addedContacts.get(i).getFullName();
                }
                if (contactEmail != null) {
                    contactsSelected.add(contactEmail);
                }
            }
        }
        logDebug("Contacts selected: " + contactsSelected.size());

        if (comesFromChat) {
            addParticipants(contactsSelected);
        }
        else if (onNewGroup) {
            String chatTitle = "";
            if (nameGroup != null && nameGroup.getText().length() > 0) {
                chatTitle = nameGroup.getText().toString();
                startConversation(contactsSelected, megaContacts, chatTitle);
            }
            else {
                startConversation(contactsSelected, megaContacts, null);
            }
        }
        else {
            newGroup();
        }
        hideKeyboard(addContactActivityLollipop, 0);
    }

    private void addParticipants(ArrayList<String> contacts) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contacts);

        setResult(RESULT_OK, intent);
        hideKeyboard(addContactActivityLollipop, 0);
        finish();
    }

    private void returnToStartConversation() {
        createNewGroup = false;
        createNewChatLink = false;
        aB.setSubtitle(null);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            inviteContactButton.setVisibility(View.VISIBLE);
        } else {
            inviteContactMenuItem.setVisible(true);
        }
        newGroupChatButton.setVisibility(View.VISIBLE);
        newChatLinkButton.setVisibility(View.VISIBLE);
        filteredContactMEGA.clear();
        filteredContactMEGA.addAll(visibleContactsMEGA);
        setMegaAdapterContacts(filteredContactMEGA, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
        addedContactsMEGA.clear();
        setRecyclersVisibility();
        setSendInvitationVisibility();
        setSearchVisibility();
        containerAddedContactsRecyclerView.setVisibility(View.GONE);
    }

    private void returnToAddContacts() {
        onNewGroup = false;
        setTitleAB();
        textHeader.setText(getString(R.string.contacts_mega));
        setRecyclersVisibility();
        addContactsLayout.setVisibility(View.VISIBLE);
        if (addedContactsMEGA.size() == 0) {
            containerAddedContactsRecyclerView.setVisibility(View.GONE);
        }
        setMegaAdapterContacts(filteredContactMEGA, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_ADD_CONTACT);
        newGroupLayout.setVisibility(View.GONE);
        if (createNewChatLink) {
            findViewById(R.id.ekr_layout).setVisibility(View.VISIBLE);
        }
        visibilityFastScroller();
        setSendInvitationVisibility();
        setSearchVisibility();
        if (createNewChatLink || visibleContactsMEGA == null || visibleContactsMEGA.isEmpty()) {
            onBackPressed();
        }
    }

    private void createMyContact () {
        if (myContact == null) {
            MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(megaApi.getMyUserHandle() + ""));
            String myFullName =  megaChatApi.getMyFullname();

            if(myFullName!=null){
                if(myFullName.trim().isEmpty()){
                    myFullName =  megaChatApi.getMyEmail();
                }
            }
            else{
                myFullName =  megaChatApi.getMyEmail();
            }

            myContact = new MegaContactAdapter(contactDB, megaApi.getMyUser(), getString(R.string.chat_me_text_bracket, myFullName));
        }
    }

    private void newGroup () {
        logDebug("newGroup");

        if (isAsyncTaskRunning(filterContactsTask)) {
            filterContactsTask.cancel(true);
        }
        onNewGroup = true;
        searchExpand = false;
        if (aB != null) {
            if (createNewChatLink) {
                aB.setTitle(getString(R.string.new_chat_link_label).toUpperCase());
            }
            else {
                aB.setTitle(getString(R.string.title_new_group).toUpperCase());
            }
            aB.setSubtitle(getString(R.string.subtitle_new_group));
        }

        createMyContact();

        addedContactsMEGA.add(myContact);
        newGroupHeaderList.setText(getResources().getQuantityString(R.plurals.subtitle_of_group_chat, addedContactsMEGA.size(), addedContactsMEGA.size()));

        if (searchMenuItem != null) {
            searchMenuItem.setVisible(false);
        }
        if (inviteContactMenuItem != null) {
            inviteContactMenuItem.setVisible(false);
        }
        addContactsLayout.setVisibility(View.GONE);
        newGroupLayout.setVisibility(View.VISIBLE);
        if (createNewChatLink) {
            findViewById(R.id.ekr_layout).setVisibility(View.GONE);
        }
        setSendInvitationVisibility();
        setMegaAdapterContacts(addedContactsMEGA, MegaContactsLollipopAdapter.ITEM_VIEW_TYPE_LIST_GROUP_CHAT);
        visibilityFastScroller();
        setRecyclersVisibility();
        if (isConfirmDeleteShown) {
            for (int i=0; i<addedContactsMEGA.size(); i++) {
                if (getMegaContactMail(addedContactsMEGA.get(i)).equals(confirmDeleteMail)) {
                    showConfirmationDeleteFromChat(addedContactsMEGA.get(i));
                    break;
                }
            }
        }
    }

    private void startConversation (ArrayList<String> contacts, boolean megaContacts, String chatTitle) {
        logDebug("startConversation");
        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contacts);

        intent.putExtra(EXTRA_MEGA_CONTACTS, megaContacts);

        if((getChatLinkBox.isChecked() || createNewChatLink) && (chatTitle == null || chatTitle.trim().isEmpty())){
            new AlertDialog.Builder(this, R.style.AppCompatAlertDialogStyleNormal)
                    .setTitle(getString(R.string.enter_group_name))
                    .setMessage(getString(R.string.alert_enter_group_name))
                    .setPositiveButton("OK",null)
                    .show();
            return;
        }

        if(chatTitle!=null){
            intent.putExtra(EXTRA_CHAT_TITLE, chatTitle);
        }

        if (createNewChatLink) {
            intent.putExtra(EXTRA_GROUP_CHAT, onNewGroup);
            intent.putExtra(EXTRA_CHAT_LINK, true);
        }
        else if(onNewGroup){
            intent.putExtra(EXTRA_EKR, isEKREnabled);
            intent.putExtra(EXTRA_GROUP_CHAT, onNewGroup);
            intent.putExtra(EXTRA_CHAT_LINK, getChatLinkBox.isChecked());
        }

        setResult(RESULT_OK, intent);
        hideKeyboard(addContactActivityLollipop, 0);
        finish();
    }

    private void shareWith (ArrayList<ShareContactInfo> addedContacts){
        logDebug("shareWith");

        ArrayList<String> contactsSelected = new ArrayList<>();
        if (addedContacts != null){
            for (int i=0; i<addedContacts.size(); i++){
                contactsSelected.add(getShareContactMail(addedContacts.get(i)));
            }
        }

        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);
        if(multipleSelectIntent==0){
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandle);
            intent.putExtra("MULTISELECT", 0);
        }
        else if(multipleSelectIntent==1){
            intent.putExtra(EXTRA_NODE_HANDLE, nodeHandles);
            intent.putExtra("MULTISELECT", 1);
        }

        intent.putExtra(EXTRA_MEGA_CONTACTS, false);
        setResult(RESULT_OK, intent);
        hideKeyboard(addContactActivityLollipop, 0);
        finish();
    }

    private void inviteContacts(ArrayList<PhoneContactInfo> addedContacts){
        logDebug("inviteContacts");

        String contactEmail = null;
        ArrayList<String> contactsSelected = new ArrayList<>();
        if (addedContacts != null) {
            for (int i=0;i<addedContacts.size();i++) {
                contactEmail = addedContacts.get(i).getEmail();
                if (fromAchievements){
                    if (contactEmail != null && !mailsFromAchievements.contains(contactEmail)) {
                        contactsSelected.add(contactEmail);
                    }
                }
                else {
                    if (contactEmail != null) {
                        contactsSelected.add(contactEmail);
                    }
                }
            }
        }

        Intent intent = new Intent();
        intent.putStringArrayListExtra(EXTRA_CONTACTS, contactsSelected);
        for(int i=0; i<contactsSelected.size();i++){
            logDebug("setResultContacts: " + contactsSelected.get(i));
        }

        intent.putExtra(EXTRA_MEGA_CONTACTS, false);
        setResult(RESULT_OK, intent);
        hideKeyboard(addContactActivityLollipop, 0);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        logDebug("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_READ_CONTACTS: {
                logDebug("REQUEST_READ_CONTACTS");
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    boolean hasReadContactsPermissions = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED);
                    if (hasReadContactsPermissions && contactType == CONTACT_TYPE_DEVICE) {
                        filteredContactsPhone.clear();
                        setEmptyStateVisibility(true);

                        progressBar.setVisibility(View.VISIBLE);
                        new GetContactsTask().execute();
                    }
                    else if (hasReadContactsPermissions && contactType == CONTACT_TYPE_BOTH) {
                        progressBar.setVisibility(View.VISIBLE);
                        emptyTextView.setText(R.string.contacts_list_empty_text_loading_share);

                        getContactsTask = new GetContactsTask();
                        getContactsTask.execute();
                    }
                }
                else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    boolean hasReadContactsPermissions = (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED);
                    queryPermissions = false;
                    supportInvalidateOptionsMenu();
                    if (hasReadContactsPermissions && contactType == CONTACT_TYPE_DEVICE) {
                        logWarning("Permission denied");
                        setTitleAB();
                        filteredContactsPhone.clear();
                        setEmptyStateVisibility(true);
                        emptyTextView.setText(R.string.no_contacts_permissions);

                        progressBar.setVisibility(View.GONE);
                    }
                }
                break;
            }
        }
    }

    private void setRecyclersVisibility () {
        if (contactType == CONTACT_TYPE_MEGA) {
            if (filteredContactMEGA.size() > 0){
                containerContacts.setVisibility(View.VISIBLE);
            }
            else {
                if (onNewGroup) {
                    containerContacts.setVisibility(View.VISIBLE);
                }
                else {
                    containerContacts.setVisibility(View.GONE);
                }
            }
        }
        else if (contactType ==  CONTACT_TYPE_DEVICE) {
            if (filteredContactsPhone.size() > 0){
                containerContacts.setVisibility(View.VISIBLE);
            }
            else {
                containerContacts.setVisibility(View.GONE);
            }
        }
        else {
            if (filteredContactsShare.size() >= 2){
                containerContacts.setVisibility(View.VISIBLE);
            }
            else {
                containerContacts.setVisibility(View.GONE);
            }
        }
    }

    private void visibilityFastScroller(){
        fastScroller.setRecyclerView(recyclerViewList);
        if (contactType == CONTACT_TYPE_MEGA) {
            if(adapterMEGA == null){
                fastScroller.setVisibility(View.GONE);
            }
            else{
                if(adapterMEGA.getItemCount() < 20){
                    fastScroller.setVisibility(View.GONE);
                }
                else{
                    fastScroller.setVisibility(View.VISIBLE);
                }
            }
        }
        else if (contactType == CONTACT_TYPE_DEVICE) {
            if(adapterPhone == null){
                fastScroller.setVisibility(View.GONE);
            }
            else{
                if(adapterPhone.getItemCount() < 20){
                    fastScroller.setVisibility(View.GONE);
                }
                else{
                    fastScroller.setVisibility(View.VISIBLE);
                }
            }
        }
        else {
            if(adapterShareHeader == null){
                fastScroller.setVisibility(View.GONE);
            }
            else{
                if(adapterShareHeader.getItemCount() < 20){
                    fastScroller.setVisibility(View.GONE);
                }
                else{
                    fastScroller.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onUsersUpdate(MegaApiJava api, ArrayList<MegaUser> users) {

    }

    @Override
    public void onUserAlertsUpdate(MegaApiJava api, ArrayList<MegaUserAlert> userAlerts) {

    }

    @Override
    public void onNodesUpdate(MegaApiJava api, ArrayList<MegaNode> nodeList) {

    }

    @Override
    public void onReloadNeeded(MegaApiJava api) {

    }

    @Override
    public void onAccountUpdate(MegaApiJava api) {

    }

    @Override
    public void onContactRequestsUpdate(MegaApiJava api, ArrayList<MegaContactRequest> requests) {
        if (requests != null) {
            for (int i = 0; i < requests.size(); i++) {
                MegaContactRequest cr = requests.get(i);
                if (cr != null) {
                    if ((cr.getStatus() == MegaContactRequest.STATUS_ACCEPTED) && (cr.isOutgoing())) {
                        logDebug("ACCEPT OPR: " + cr.getSourceEmail() + " cr.isOutgoing: " + cr.isOutgoing() + " cr.getStatus: " + cr.getStatus());
                        getContactsTask = new GetContactsTask();
                        getContactsTask.execute();
                    }
                }
            }
        }
    }

    @Override
    public void onEvent(MegaApiJava api, MegaEvent event) {

    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
        return false;
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent e) {

    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {
        logDebug("onRequestStart: " + request.getRequestString());
    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT){
            logDebug("MegaRequest.TYPE_INVITE_CONTACT finished: " + request.getNumber());

            if(request.getNumber()==MegaContactRequest.INVITE_ACTION_REMIND){
                showSnackbar(getString(R.string.context_contact_invitation_resent));
            }
            else{
                if (e.getErrorCode() == MegaError.API_OK){
                    logDebug("OK INVITE CONTACT: " + request.getEmail());
                    if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD){
                        showSnackbar(getString(R.string.context_contact_request_sent, request.getEmail()));
                    }
                    else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_DELETE){
                        showSnackbar(getString(R.string.context_contact_invitation_deleted));
                    }
                }
                else{
                    logWarning("ERROR: " + e.getErrorCode() + "___" + e.getErrorString());
                    if(e.getErrorCode()==MegaError.API_EEXIST){
                        showSnackbar(getString(R.string.context_contact_already_exists, request.getEmail()));
                    }
                    else if(request.getNumber()==MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode()==MegaError.API_EARGS){
                        showSnackbar(getString(R.string.error_own_email_as_contact));
                    }
                    else{
                        showSnackbar(getString(R.string.general_error));
                    }
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }


    @Override
    public void onChatListItemUpdate(MegaChatApiJava api, MegaChatListItem item) {

    }

    @Override
    public void onChatInitStateUpdate(MegaChatApiJava api, int newState) {

    }

    @Override
    public void onChatOnlineStatusUpdate(MegaChatApiJava api, long userhandle, int status, boolean inProgress) {

    }

    @Override
    public void onChatPresenceConfigUpdate(MegaChatApiJava api, MegaChatPresenceConfig config) {

    }

    @Override
    public void onChatConnectionStateUpdate(MegaChatApiJava api, long chatid, int newState) {

    }

    @Override
    public void onChatPresenceLastGreen(MegaChatApiJava api, long userhandle, int lastGreen) {
        logDebug("onChatPresenceLastGreen");
        int state = megaChatApi.getUserOnlineStatus(userhandle);
        if(state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID){
            String formattedDate = lastGreenDate(this, lastGreen);
            if(userhandle != megaChatApi.getMyUserHandle()){
                logDebug("Status last green for the user: " + userhandle);
//                Replace on visible MEGA contacts (all my visible contacts)
                ListIterator<MegaContactAdapter> itrReplace = visibleContactsMEGA.listIterator();
                while (itrReplace.hasNext()) {
                    MegaContactAdapter contactToUpdate = itrReplace.next();
                    if (contactToUpdate != null) {
                        if (getMegaContactHandle(contactToUpdate) == userhandle) {
                            contactToUpdate.setLastGreen(formattedDate);
                            break;
                        }
                    } else {
                        break;
                    }
                }
//                Replace on list adapter (filtered or search filtered MEGA contacts)
                int indexToReplace = -1;
                if(adapterMEGA!=null && adapterMEGA.getContacts() != null){
                    ListIterator<MegaContactAdapter> itrReplace2 = adapterMEGA.getContacts().listIterator();
                    while (itrReplace2.hasNext()) {
                        MegaContactAdapter contactToUpdate = itrReplace2.next();
                        if (contactToUpdate != null) {
                            if (getMegaContactHandle(contactToUpdate) == userhandle) {
                                contactToUpdate.setLastGreen(formattedDate);
                                indexToReplace = itrReplace2.nextIndex() -1;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (indexToReplace != -1) {
                        adapterMEGA.updateContactStatus(indexToReplace);
                    }
                }
//                Replace on filtered MEGA contacts (without search)
                if (filteredContactMEGA != null && filteredContactMEGA.size() > 0) {
                    ListIterator<MegaContactAdapter> itrReplace3 = filteredContactMEGA.listIterator();
                    while (itrReplace3.hasNext()) {
                        MegaContactAdapter contactToUpdate = itrReplace3.next();
                        if (contactToUpdate != null) {
                            if (getMegaContactHandle(contactToUpdate) == userhandle) {
                                contactToUpdate.setLastGreen(formattedDate);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
//                Replace, if exist, on search filtered MEGA contacts
                if (queryContactMEGA != null && queryContactMEGA.size() > 0) {
                    ListIterator<MegaContactAdapter> itrReplace4 = queryContactMEGA.listIterator();
                    while (itrReplace4.hasNext()) {
                        MegaContactAdapter contactToUpdate = itrReplace4.next();
                        if (contactToUpdate != null) {
                            if (getMegaContactHandle(contactToUpdate) == userhandle) {
                                contactToUpdate.setLastGreen(formattedDate);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                }
//                Replace, if exist, on added adapter and added MEGA contacts
                if (addedContactsMEGA != null && addedContactsMEGA.size() > 0) {
                    ListIterator<MegaContactAdapter> itrReplace5 = addedContactsMEGA.listIterator();
                    while (itrReplace5.hasNext()) {
                        MegaContactAdapter contactToUpdate = itrReplace5.next();
                        if (contactToUpdate != null) {
                            if (getMegaContactHandle(contactToUpdate) == userhandle) {
                                contactToUpdate.setLastGreen(formattedDate);
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (adapterMEGAContacts != null) {
                        adapterMEGAContacts.setContacts(addedContactsMEGA);
                    }
                }
            }
            logDebug("Date last green: " + formattedDate);
        }
    }
}
