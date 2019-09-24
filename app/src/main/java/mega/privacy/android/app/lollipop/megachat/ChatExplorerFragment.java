package mega.privacy.android.app.lollipop.megachat;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ListIterator;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MegaContactAdapter;
import mega.privacy.android.app.MegaContactDB;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ContactController;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChipChatExplorerAdapter;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatExplorerAdapter;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

public class ChatExplorerFragment extends Fragment {

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

    ChatExplorerFragment chatExplorerFragment;

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    DatabaseHandler dbH;

    Context context;
    ActionBar aB;
    RecyclerView listView;
    MegaListChatExplorerAdapter adapterList;
    RelativeLayout mainRelativeLayout;

    LinearLayoutManager mLayoutManager;

    ArrayList<MegaChatListItem> chats;
    ArrayList<MegaChatListItem> archievedChats;
    ArrayList<MegaContactAdapter> contacts;
    ArrayList<ChatExplorerListItem> items;
    ArrayList<ChatExplorerListItem> addedItems;
    ArrayList<String> addedItemsSaved;

    int lastFirstVisiblePosition;

    //Empty screen
    TextView emptyTextView;
    LinearLayout emptyLayout;
    TextView emptyTextViewInvite;
    ImageView emptyImageView;
    int chatStatus;
    Button inviteButton;
    RelativeLayout contentLayout;
    ProgressBar progressBar;

    boolean chatEnabled = true;
    float density;
    DisplayMetrics outMetrics;
    Display display;

    AppBarLayout addLayout;
    Button newGroupButton;
    RecyclerView addedList;
    MegaChipChatExplorerAdapter adapterAdded;
    LinearLayoutManager addedLayoutManager;

    SearchTask searchTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logDebug("onCreate");

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());

        if(isChatEnabled()){
            chatEnabled=true;
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
                if (context instanceof ChatExplorerActivity) {
                    megaChatApi.addChatListener((ChatExplorerActivity) context);
                }
                else if (context instanceof FileExplorerActivityLollipop) {
                    megaChatApi.addChatListener((FileExplorerActivityLollipop) context);
                }
            }
        }
        else{
            logWarning("Chat not enabled!");
            chatEnabled=false;
        }

        chatExplorerFragment = this;
    }


    public static ChatExplorerFragment newInstance() {
        logDebug("newInstance");
        ChatExplorerFragment fragment = new ChatExplorerFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        logDebug("onCreateView");

        display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        View v = inflater.inflate(R.layout.chat_recent_explore, container, false);

        contentLayout = (RelativeLayout) v.findViewById(R.id.content_layout_chat_explorer);
        progressBar = (ProgressBar) v.findViewById(R.id.progressbar_chat_explorer);

        addLayout = (AppBarLayout) v.findViewById(R.id.linear_layout_add);
        addedList = (RecyclerView) v.findViewById(R.id.contact_adds_recycler_view);
        newGroupButton = (Button) v.findViewById(R.id.new_group_button);
        if (context instanceof ChatExplorerActivity || context instanceof FileExplorerActivityLollipop) {
            addLayout.setVisibility(View.VISIBLE);
            addedLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
            addedList.setLayoutManager(addedLayoutManager);
            addedList.setHasFixedSize(true);
            addedList.setItemAnimator(new DefaultItemAnimator());
            addedList.setClipToPadding(false);

            if (context instanceof ChatExplorerActivity) {
                newGroupButton.setOnClickListener((ChatExplorerActivity) context);
            }
            else if (context instanceof FileExplorerActivityLollipop) {
                newGroupButton.setOnClickListener((FileExplorerActivityLollipop) context);
            }
            setFirstLayoutVisibility(View.VISIBLE);
        }
        else {
            addLayout.setVisibility(View.GONE);
        }

        listView = (RecyclerView) v.findViewById(R.id.chat_recent_list_view);

        listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
        mLayoutManager = new LinearLayoutManager(context);
        listView.setLayoutManager(mLayoutManager);
        listView.setHasFixedSize(true);
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setClipToPadding(false);
        listView.setPadding(0,scaleHeightPx(8, outMetrics),0, 0);
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (context instanceof FileExplorerActivityLollipop) {
                    if (listView.canScrollVertically(-1)) {
                        ((FileExplorerActivityLollipop) context).changeActionBarElevation(true);
                        if (addLayout.getVisibility() == View.VISIBLE) {
                            addLayout.setElevation(px2dp(4, outMetrics));
                        }
                    } else {
                        ((FileExplorerActivityLollipop) context).changeActionBarElevation(false);
                        if (addLayout.getVisibility() == View.VISIBLE) {
                            addLayout.setElevation(0);
                        }
                    }
                }
                else if (context instanceof ChatExplorerActivity && addLayout != null && addLayout.getVisibility() == View.VISIBLE) {
                    if (listView.canScrollVertically(-1)) {
                        addLayout.setElevation(px2dp(4, outMetrics));
                    } else {
                        addLayout.setElevation(0);
                    }
                }
            }
        });

        emptyLayout = (LinearLayout) v.findViewById(R.id.linear_empty_layout_chat_recent);
        emptyTextViewInvite = (TextView) v.findViewById(R.id.empty_text_chat_recent_invite);
        emptyTextViewInvite.setWidth(scaleWidthPx(236, outMetrics));
        emptyTextView = (TextView) v.findViewById(R.id.empty_text_chat_recent);

        String textToShow = String.format(context.getString(R.string.chat_explorer_empty));
        try{
            textToShow = textToShow.replace("[A]", "<font color=\'#7a7a7a\'>");
            textToShow = textToShow.replace("[/A]", "</font>");
            textToShow = textToShow.replace("[B]", "<font color=\'#000000\'>");
            textToShow = textToShow.replace("[/B]", "</font>");
        }
        catch (Exception e){}
        Spanned resultB = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            resultB = Html.fromHtml(textToShow,Html.FROM_HTML_MODE_LEGACY);
        } else {
            resultB = Html.fromHtml(textToShow);
        }
        emptyTextView.setText(resultB);

        LinearLayout.LayoutParams emptyTextViewParams1 = (LinearLayout.LayoutParams)emptyTextViewInvite.getLayoutParams();
        emptyTextViewParams1.setMargins(0, scaleHeightPx(50, outMetrics), 0, scaleHeightPx(24, outMetrics));
        emptyTextViewInvite.setLayoutParams(emptyTextViewParams1);

        LinearLayout.LayoutParams emptyTextViewParams2 = (LinearLayout.LayoutParams)emptyTextView.getLayoutParams();
        emptyTextViewParams2.setMargins(scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics), scaleWidthPx(20, outMetrics), scaleHeightPx(20, outMetrics));
        emptyTextView.setLayoutParams(emptyTextViewParams2);

        emptyImageView = (ImageView) v.findViewById(R.id.empty_image_view_recent);
        emptyImageView.setImageResource(R.drawable.ic_empty_chat_list);

        mainRelativeLayout = (RelativeLayout) v.findViewById(R.id.main_relative_layout);
        inviteButton = (Button) v.findViewById(R.id.invite_button);
        inviteButton.setVisibility(View.GONE);

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        if (savedInstanceState != null) {
            addedItemsSaved = savedInstanceState.getStringArrayList("addedItemsSaved");
        }
        else {
            addedItemsSaved = new ArrayList<>();
        }

        this.setChats();

        return v;
    }

    void setFirstLayoutVisibility (int visibility) {
        newGroupButton.setVisibility(visibility);
        if (visibility == View.VISIBLE) {
            addedList.setVisibility(View.GONE);
        }
        else if (visibility == View.GONE) {
            addedList.setVisibility(View.VISIBLE);
        }
    }

    public MegaContactAdapter getContact(MegaChatListItem chat) {
        long handle = chat.getPeerHandle();
        String userHandleEncoded = MegaApiAndroid.userHandleToBase64(handle);
        MegaUser user = megaApi.getContact(userHandleEncoded);

//        Maybe the contact is not my contact already
        if (user == null) {
            logDebug("Chat ID " + chat.getChatId() + " with PeerHandle: " + handle + " is NULL");
            return null;
        }

        MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(handle+""));
        String fullName = "";
        if(contactDB!=null){
            ContactController cC = new ContactController(context);
            fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), user.getEmail());
        }
        else{
            fullName = user.getEmail();
        }

        if (handle != -1) {
            int userStatus = megaChatApi.getUserOnlineStatus(handle);
            if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
                logDebug("Request last green for user");
                megaChatApi.requestLastGreen(handle, null);
            }
        }

        return new MegaContactAdapter(contactDB, user, fullName);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (megaChatApi != null) {
            if (context instanceof ChatExplorerActivity) {
                megaChatApi.removeChatListener((ChatExplorerActivity) context);
            }
            else if (context instanceof FileExplorerActivityLollipop) {
                megaChatApi.removeChatListener((FileExplorerActivityLollipop) context);
            }
        }
    }

    void sortByAlphabetical ( ) {
        Collections.sort(items, new Comparator<ChatExplorerListItem> (){

            public int compare(ChatExplorerListItem c1, ChatExplorerListItem c2) {
                String n1 = c1.getTitle();
                String n2 = c2.getTitle();

                int res = String.CASE_INSENSITIVE_ORDER.compare(n1, n2);
                if (res == 0) {
                    res = n1.compareTo(n2);
                }
                return res;
            }
        });
    }

    public void setChats(){
        logDebug("setChats");

        contentLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        new RecoverItemsTask().execute();
    }

    void getVisibleMEGAContacts () {
        ArrayList<MegaUser> contactsMEGA = megaApi.getContacts();
        for (int i=0;i<contactsMEGA.size();i++){
            logDebug("Contact: " + contactsMEGA.get(i).getEmail() + "_" + contactsMEGA.get(i).getVisibility());
            if (contactsMEGA.get(i).getVisibility() == MegaUser.VISIBILITY_VISIBLE){

                MegaContactDB contactDB = dbH.findContactByHandle(String.valueOf(contactsMEGA.get(i).getHandle()+""));
                String fullName = "";
                if(contactDB!=null){
                    ContactController cC = new ContactController(context);
                    fullName = cC.getFullName(contactDB.getName(), contactDB.getLastName(), contactsMEGA.get(i).getEmail());
                }
                else{
                    fullName = contactsMEGA.get(i).getEmail();
                }

                MegaContactAdapter megaContactAdapter = new MegaContactAdapter(contactDB, contactsMEGA.get(i), fullName);
                contacts.add(megaContactAdapter);
            }
        }
    }

    public ArrayList<ChatExplorerListItem> getAddedChats () {

        if (addedItems != null) {
            return addedItems;
        }

        return null;
    }

    public void itemClick(int position) {
        logDebug("Position: " + position);
        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        if (adapterList != null && adapterList.getItemCount() > 0) {
            ChatExplorerListItem item = adapterList.getItem(position);

            if (adapterList.isSearchEnabled()) {
                if (context instanceof ChatExplorerActivity) {
                    ((ChatExplorerActivity) context).collapseSearchView();
                }
                else {
                    ((FileExplorerActivityLollipop) context).collapseSearchView();
                }
                adapterList.setItems(items);
                int togglePossition = adapterList.getPosition(item);
                adapterList.toggleSelection(togglePossition);
            }
            else {
                adapterList.toggleSelection(position);
            }


            if (item != null && !addedItems.contains(item)) {
                addedItems.add(item);
                adapterAdded.setItems(addedItems);
                setFirstLayoutVisibility(View.GONE);

                if(context instanceof  ChatExplorerActivity){
                    ((ChatExplorerActivity)context).showFabButton(true);
                    ((ChatExplorerActivity)context).setToolbarSubtitle(getString(R.string.selected_items, addedItems.size()));
                }
                else if (context instanceof FileExplorerActivityLollipop){
                    ((FileExplorerActivityLollipop)context).showFabButton(true);
                    ((FileExplorerActivityLollipop)context).setToolbarSubtitle(getString(R.string.selected_items, addedItems.size()));
                }
            }
            else if (addedItems.contains(item)) {
                deleteItem(item);
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        aB = ((AppCompatActivity)activity).getSupportActionBar();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
        aB = ((AppCompatActivity)context).getSupportActionBar();
    }

    public String getParticipantFullName(MegaChatRoom chat, long i){
        String participantFirstName = chat.getPeerFirstname(i);
        String participantLastName = chat.getPeerLastname(i);

        if(participantFirstName==null){
            participantFirstName="";
        }
        if(participantLastName == null){
            participantLastName="";
        }

        if (participantFirstName.trim().length() <= 0){
            logDebug("Participant: " + participantFirstName);
            return participantLastName;
        }
        else{
            logDebug("Participant: " + participantLastName);
            return participantFirstName + " " + participantLastName;
        }
    }

//    @Override
//    public void onChatRoomUpdate(MegaChatApiJava api, MegaChatRoom item) {
//        log("onChatRoomUpdate");
//
////        MegaChatRoom chatUpdated = megaChatApi.getChatRoom(chat.getChatId());
////        if(chatUpdated!=null){
////            log("chat updated: "+chat.getTitle());
////            log("unread count: "+chat.getUnreadCount());
////            log("change type: "+chat.getChanges());
////        }
//
//        if(item.hasChanged(MegaChatRoom.CHANGE_TYPE_UNREAD_COUNT)){
//            log("CHANGE_TYPE_UNREAD_COUNT for the chat: "+item.getChatId());
//            if(item!=null){
//
//                if (adapterList == null || adapterList.getItemCount()==0){
//                    setChats();
//                }
//                else{
//                    long chatHandleToUpdate = item.getChatId();
//                    int indexToReplace = -1;
//                    ListIterator<MegaChatListItem> itrReplace = chats.listIterator();
//                    while (itrReplace.hasNext()) {
//                        MegaChatListItem chat = itrReplace.next();
//                        if(chat!=null){
//                            if(chat.getChatId()==chatHandleToUpdate){
//                                indexToReplace = itrReplace.nextIndex()-1;
//                                break;
//                            }
//                        }
//                        else{
//                            break;
//                        }
//                    }
//                    if(indexToReplace!=-1){
//                        log("Index to replace: "+indexToReplace);
//                        MegaChatListItem chatToReplace = megaChatApi.getChatListItem(item.getChatId());
//                        log("Unread count: "+chatToReplace.getUnreadCount());
//                        chats.set(indexToReplace, chatToReplace);
//
//                        adapterList.modifyChat(chats, indexToReplace);
//                        adapterList.setPositionClicked(-1);
//
//                        if (adapterList.getItemCount() == 0){
//                            log("adapterList.getItemCount() == 0");
//                            listView.setVisibility(View.GONE);
//                            emptyLayout.setVisibility(View.VISIBLE);
//                        }
//                        else{
//                            log("adapterList.getItemCount() NOT = 0");
//                            listView.setVisibility(View.VISIBLE);
//                            emptyLayout.setVisibility(View.GONE);
//                        }
//                    }
//                }
//            }
//        }
//    }

//    @Override
//    public void onViewStateRestored(Bundle savedInstanceState) {
//        log("onViewStateRestored");
//        super.onViewStateRestored(savedInstanceState);
//
//        if(savedInstanceState != null)
//        {
//            Parcelable savedRecyclerLayoutState = savedInstanceState.getParcelable(BUNDLE_RECYCLER_LAYOUT);
//            listView.getLayoutManager().onRestoreInstanceState(savedRecyclerLayoutState);
//        }
//    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        logDebug("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if(listView.getLayoutManager()!=null){
            outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, listView.getLayoutManager().onSaveInstanceState());
        }

        if (addedItems != null && !addedItems.isEmpty()) {
            if (addedItemsSaved == null) {
                addedItemsSaved = new ArrayList<>();
            }
            else {
                addedItemsSaved.clear();
            }
            for (ChatExplorerListItem item : addedItems) {
                addedItemsSaved.add(item.getId());
            }
            outState.putStringArrayList("addedItemsSaved", addedItemsSaved);
        }
    }

    @Override
    public void onPause() {
        logDebug("onPause");
        lastFirstVisiblePosition = ((LinearLayoutManager)listView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        super.onPause();
    }

    @Override
    public void onResume() {
        logDebug("lastFirstVisiblePosition: " + lastFirstVisiblePosition);
        if(lastFirstVisiblePosition>0){
            (listView.getLayoutManager()).scrollToPosition(lastFirstVisiblePosition);
        }else{
            (listView.getLayoutManager()).scrollToPosition(0);
        }
        lastFirstVisiblePosition=0;
        super.onResume();
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

    public long getChatIdFrom(){
        if(context instanceof  ChatExplorerActivity){
            return ((ChatExplorerActivity)context).chatIdFrom;
        }
        return -1;
    }

    public long getMegaContactHandle (MegaContactAdapter contact) {
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

    public void updateLastGreenContact (long userhandle, String formattedDate) {

        if(adapterList!=null && adapterList.getItems() != null){
            ListIterator<ChatExplorerListItem> itrReplace = adapterList.getItems().listIterator();
            while (itrReplace.hasNext()) {
                ChatExplorerListItem itemToUpdate = itrReplace.next();
                if (itemToUpdate != null) {
                    if (itemToUpdate.getContact() != null) {
                        if (getMegaContactHandle(itemToUpdate.getContact()) == userhandle) {
                            itemToUpdate.getContact().setLastGreen(formattedDate);
                            adapterList.updateItemContactStatus(itrReplace.nextIndex());
                            items = adapterList.getItems();
                            break;
                        }
                    }
                    else {
                        continue;
                    }
                } else {
                    break;
                }
            }
        }

        if(adapterAdded!=null && adapterAdded.getItems() != null){
            ListIterator<ChatExplorerListItem> itrReplace = adapterAdded.getItems().listIterator();
            while (itrReplace.hasNext()) {
                ChatExplorerListItem itemToUpdate = itrReplace.next();
                if (itemToUpdate != null) {
                    if (itemToUpdate.getContact() != null) {
                        if (getMegaContactHandle(itemToUpdate.getContact()) == userhandle) {
                            itemToUpdate.getContact().setLastGreen(formattedDate);
                            items = adapterAdded.getItems();
                            break;
                        }
                    }
                    else {
                        continue;
                    }
                } else {
                    break;
                }
            }
        }
    }

    public void deleteItemPosition(int position) {
        ChatExplorerListItem item = adapterAdded.getItem(position);
        if (adapterList != null) {
            int positionItem = adapterList.getPosition(item);
            if (positionItem != -1) {
                adapterList.toggleSelection(positionItem);
            }
        }
        deleteItem(item);
    }

    public void deleteItem(ChatExplorerListItem item) {
        if (item != null) {
            addedItems.remove(item);
            adapterAdded.setItems(addedItems);

            if (addedItems.size() > 0) {
                setFirstLayoutVisibility(View.GONE);
                if(context instanceof  ChatExplorerActivity){
                    ((ChatExplorerActivity)context).showFabButton(true);
                    ((ChatExplorerActivity)context).setToolbarSubtitle(getString(R.string.selected_items, addedItems.size()));
                }
                else if (context instanceof FileExplorerActivityLollipop){
                    ((FileExplorerActivityLollipop)context).showFabButton(true);
                    ((FileExplorerActivityLollipop)context).setToolbarSubtitle(getString(R.string.selected_items, addedItems.size()));
                }
            }
            else {
                setFirstLayoutVisibility(View.VISIBLE);
                if(context instanceof  ChatExplorerActivity){
                    ((ChatExplorerActivity)context).showFabButton(false);
                    ((ChatExplorerActivity)context).setToolbarSubtitle(null);
                }
                else if (context instanceof FileExplorerActivityLollipop){
                    ((FileExplorerActivityLollipop)context).showFabButton(false);
                    ((FileExplorerActivityLollipop)context).setToolbarSubtitle(null);
                }
            }
        }
    }

    class RecoverItemsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            if(isAdded()){
                if (items != null) {
                    items.clear();
                }
                else {
                    items = new ArrayList<>();
                }

                if (addedItems != null) {
                    addedItems.clear();
                }
                else {
                    addedItems = new ArrayList<>();
                }

                if(chats!=null){
                    chats.clear();
                }
                else{
                    chats = new ArrayList<>();
                }

                if (archievedChats != null) {
                    archievedChats.clear();
                }
                else {
                    archievedChats =  new ArrayList<>();
                }

                if (contacts != null) {
                    contacts.clear();
                }
                else {
                    contacts = new ArrayList<>();
                }

                chats = megaChatApi.getActiveChatListItems();
                archievedChats = megaChatApi.getArchivedChatListItems();
                getVisibleMEGAContacts();

                for (MegaChatListItem chat : chats) {
                    if (chat.isGroup()) {
                        items.add(new ChatExplorerListItem(chat));
                    }
                    else {
                        items.add(new ChatExplorerListItem(chat, getContact(chat)));
                    }
                }

                for (MegaChatListItem archieved : archievedChats) {
                    if (archieved.isGroup()) {
                        items.add(new ChatExplorerListItem(archieved));
                    }
                    else {
                        items.add(new ChatExplorerListItem(archieved, getContact(archieved)));
                    }
                }

                for (MegaContactAdapter contact : contacts) {
                    if (contact.getMegaUser() != null) {
                        MegaChatRoom chat = megaChatApi.getChatRoomByUser(contact.getMegaUser().getHandle());
                        if (chat == null) {
                            if (contact.getMegaUser() != null) {
                                long handle = contact.getMegaUser().getHandle();
                                if (handle != -1) {
                                    int userStatus = megaChatApi.getUserOnlineStatus(handle);
                                    if (userStatus != MegaChatApi.STATUS_ONLINE && userStatus != MegaChatApi.STATUS_BUSY && userStatus != MegaChatApi.STATUS_INVALID) {
                                        logDebug("Request last green for user");
                                        if (context instanceof ChatExplorerActivity) {
                                            megaChatApi.requestLastGreen(handle, (ChatExplorerActivity) context);
                                        }
                                        else if (context instanceof FileExplorerActivityLollipop) {
                                            megaChatApi.requestLastGreen(handle, (FileExplorerActivityLollipop) context);
                                        }
                                    }
                                }
                            }
                            items.add(new ChatExplorerListItem(contact));
                        }
                    }
                }

                logDebug("Items number: " + items.size());

                //Order by title
                sortByAlphabetical();

                if (adapterList == null){
                    logWarning("AdapterList is NULL");
                    adapterList = new MegaListChatExplorerAdapter(context, chatExplorerFragment, items, listView);
                }
                else{
                    adapterList.setItems(items);
                }

                if (addedItemsSaved != null && !addedItemsSaved.isEmpty()) {
                    for (String id : addedItemsSaved) {
                        for (ChatExplorerListItem item : items) {
                            if (item.getId().equals(id)) {
                                addedItems.add(item);
                                int position = adapterList.getPosition(item);
                                if (position != -1) {
                                    adapterList.toggleSelection(position);
                                }
                                break;
                            }
                        }
                    }
                }

                if (adapterAdded == null) {
                    adapterAdded = new MegaChipChatExplorerAdapter(context, chatExplorerFragment, addedItems);
                }
                else {
                    adapterAdded.setItems(addedItems);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {

            addedList.setAdapter(adapterAdded);

            listView.setAdapter(adapterList);

            if (adapterAdded.getItemCount() == 0) {
                setFirstLayoutVisibility(View.VISIBLE);

                if(context instanceof  ChatExplorerActivity){
                    ((ChatExplorerActivity)context).setToolbarSubtitle(null);
                }
                else if (context instanceof FileExplorerActivityLollipop){
                    ((FileExplorerActivityLollipop)context).setToolbarSubtitle(null);
                }
            }
            else {
                setFirstLayoutVisibility(View.GONE);

                if(context instanceof  ChatExplorerActivity){
                    ((ChatExplorerActivity)context).showFabButton(true);
                    ((ChatExplorerActivity)context).setToolbarSubtitle(getString(R.string.selected_items, addedItems.size()));
                }
                else if (context instanceof FileExplorerActivityLollipop){
                    ((FileExplorerActivityLollipop) context).showFabButton(true);
                    ((FileExplorerActivityLollipop) context).setToolbarSubtitle(getString(R.string.selected_items, addedItems.size()));
                }
            }

            if (context instanceof ChatExplorerActivity) {
                ((ChatExplorerActivity) context).isPendingToOpenSearchView();
            }
            else if (context instanceof FileExplorerActivityLollipop) {
                ((FileExplorerActivityLollipop) context).isPendingToOpenSearchView();
            }
            contentLayout.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            setListVisibility();
        }
    }

    void setListVisibility () {
        if (adapterList.getItemCount() == 0){
            logDebug("adapterList.getItemCount() == 0");
            listView.setVisibility(View.GONE);
            addLayout.setVisibility(View.GONE);
            emptyLayout.setVisibility(View.VISIBLE);
        }
        else{
            logDebug("adapterList.getItemCount() NOT = 0");
            listView.setVisibility(View.VISIBLE);
            if (!adapterList.isSearchEnabled()) {
                addLayout.setVisibility(View.VISIBLE);
            }
            emptyLayout.setVisibility(View.GONE);
        }
    }

    class SearchTask extends AsyncTask<String, Void, Void> {

        ArrayList<ChatExplorerListItem> searchItems = new ArrayList<>();
        SparseBooleanArray searchSelectedItems = new SparseBooleanArray();

        @Override
        protected Void doInBackground(String... strings) {
            String s = strings[0];
            boolean areAddedItems = false;

            if (addedItems != null && !addedItems.isEmpty()) {
                areAddedItems = true;
            }

            for (ChatExplorerListItem item : items) {
                if (item.getTitle().toLowerCase().contains(s.toLowerCase())) {
                    searchItems.add(item);
                    if (areAddedItems && addedItems.contains(item)) {
                        searchSelectedItems.put(searchItems.indexOf(item), true);
                    }
                }
            }

            adapterList.setSearchSelectedItems(searchSelectedItems);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            adapterList.setItems(searchItems);
            setListVisibility();
        }
    }

    public void search (String s) {
        if (searchTask != null && searchTask.getStatus() != AsyncTask.Status.FINISHED) {
            searchTask.cancel(true);
        }
        searchTask = new SearchTask();
        searchTask.execute(s);
    }

    public void enableSearch(boolean enable) {
        if (enable) {
            search("");
            if (addLayout.getVisibility() == View.VISIBLE) {
                addLayout.setVisibility(View.GONE);
            }
            if (adapterList != null && !adapterList.isSearchEnabled()) {
                adapterList.setSearchEnabled(enable);
            }
            if(context instanceof  ChatExplorerActivity){
                ((ChatExplorerActivity)context).showFabButton(false);
            }
            else if (context instanceof FileExplorerActivityLollipop){
                ((FileExplorerActivityLollipop)context).showFabButton(false);
            }
        }
        else{
            if (addLayout.getVisibility() == View.GONE) {
                addLayout.setVisibility(View.VISIBLE);
            }

            if (adapterList != null && adapterList.isSearchEnabled()) {
                adapterList.setSearchEnabled(enable);
            }

            if (adapterAdded != null && adapterAdded.getItemCount() == 0) {
                setFirstLayoutVisibility(View.VISIBLE);
            }
            else {
                setFirstLayoutVisibility(View.GONE);

                if(context instanceof  ChatExplorerActivity){
                    ((ChatExplorerActivity)context).showFabButton(true);
                }
                else if (context instanceof FileExplorerActivityLollipop){
                    ((FileExplorerActivityLollipop)context).showFabButton(true);
                }
            }
        }
    }
}
