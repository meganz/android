package mega.privacy.android.app.lollipop.megachat;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.FileExplorerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatRoom;

public class ChatExplorerFragment extends Fragment{

    private static final String BUNDLE_RECYCLER_LAYOUT = "classname.recycler.layout";

    MegaApiAndroid megaApi;
    MegaChatApiAndroid megaChatApi;

    DatabaseHandler dbH;

    Context context;
    ActionBar aB;
    RecyclerView listView;
    MegaListChatLollipopAdapter adapterList;
    RelativeLayout mainRelativeLayout;

    LinearLayoutManager mLayoutManager;

    ArrayList<MegaChatListItem> chats;

    int lastFirstVisiblePosition;

    //Empty screen
    TextView emptyTextView;
    LinearLayout emptyLayout;
    TextView emptyTextViewInvite;
    ImageView emptyImageView;
    int chatStatus;
    Button inviteButton;

    boolean chatEnabled = true;
    float density;
    DisplayMetrics outMetrics;
    Display display;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        dbH = DatabaseHandler.getDbHandler(getActivity());

        if(Util.isChatEnabled()){
            chatEnabled=true;
            if (megaChatApi == null){
                megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
            }
        }
        else{
            log("Chat not enabled!");
            chatEnabled=false;
        }
    }


    public static ChatExplorerFragment newInstance() {
        log("newInstance");
        ChatExplorerFragment fragment = new ChatExplorerFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        log("onCreateView");

        display = ((Activity) context).getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        View v = inflater.inflate(R.layout.chat_recent_tab, container, false);

        listView = (RecyclerView) v.findViewById(R.id.chat_recent_list_view);

        listView.addItemDecoration(new SimpleDividerItemDecoration(context, outMetrics));
        mLayoutManager = new LinearLayoutManager(context);
        listView.setLayoutManager(mLayoutManager);
        listView.setHasFixedSize(true);
        listView.setItemAnimator(new DefaultItemAnimator());
        listView.setClipToPadding(false);
        listView.setPadding(0,Util.scaleHeightPx(8, outMetrics),0, 0);
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (listView.canScrollVertically(-1)){
                    ((FileExplorerActivityLollipop) context).changeActionBarElevation(true);
                }
                else {
                    ((FileExplorerActivityLollipop) context).changeActionBarElevation(false);
                }
            }
        });

        emptyLayout = (LinearLayout) v.findViewById(R.id.linear_empty_layout_chat_recent);
        emptyTextViewInvite = (TextView) v.findViewById(R.id.empty_text_chat_recent_invite);
        emptyTextViewInvite.setWidth(Util.scaleWidthPx(236, outMetrics));
        emptyTextView = (TextView) v.findViewById(R.id.empty_text_chat_recent);

        String textToShow = String.format(context.getString(R.string.recent_chat_empty));
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
        emptyTextViewParams1.setMargins(0, Util.scaleHeightPx(50, outMetrics), 0, Util.scaleHeightPx(24, outMetrics));
        emptyTextViewInvite.setLayoutParams(emptyTextViewParams1);

        LinearLayout.LayoutParams emptyTextViewParams2 = (LinearLayout.LayoutParams)emptyTextView.getLayoutParams();
        emptyTextViewParams2.setMargins(Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics), Util.scaleWidthPx(20, outMetrics), Util.scaleHeightPx(20, outMetrics));
        emptyTextView.setLayoutParams(emptyTextViewParams2);

        emptyImageView = (ImageView) v.findViewById(R.id.empty_image_view_recent);
        emptyImageView.setImageResource(R.drawable.ic_empty_chat_list);

        mainRelativeLayout = (RelativeLayout) v.findViewById(R.id.main_relative_layout);
        inviteButton = (Button) v.findViewById(R.id.invite_button);
        inviteButton.setVisibility(View.GONE);

        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        this.setChats();

        return v;
    }

    public void setChats(){
        log("setChats");

        if(isAdded()){
            if(chats!=null){
                chats.clear();
            }
            else{
                chats = new ArrayList<MegaChatListItem>();
            }

            int initState = megaChatApi.getInitState();
            log("Init state is: "+initState);
            chats = megaChatApi.getActiveChatListItems();

            log("chats no: "+chats.size());

            //Order by last interaction
            Collections.sort(chats, new Comparator<MegaChatListItem> (){

                public int compare(MegaChatListItem c1, MegaChatListItem c2) {
                    long timestamp1 = c1.getLastTimestamp();
                    long timestamp2 = c2.getLastTimestamp();

                    long result = timestamp2 - timestamp1;
                    return (int)result;
                }
            });

            if (adapterList == null){
                log("adapterList is NULL");
                adapterList = new MegaListChatLollipopAdapter(context, this, chats, listView, MegaListChatLollipopAdapter.ADAPTER_RECENT_CHATS);
            }
            else{
                adapterList.setChats(chats);
            }

            listView.setAdapter(adapterList);
            adapterList.setPositionClicked(-1);

            if (adapterList.getItemCount() == 0){
                log("adapterList.getItemCount() == 0");
                listView.setVisibility(View.GONE);
                emptyLayout.setVisibility(View.VISIBLE);
            }
            else{
                log("adapterList.getItemCount() NOT = 0");
                listView.setVisibility(View.VISIBLE);
                emptyLayout.setVisibility(View.GONE);
            }
        }
    }

    public ArrayList<MegaChatListItem> getSelectedChats() {
        log("getSelectedChats");

        if(adapterList!=null){
            return adapterList.getSelectedChats();
        }
        return null;
    }

    /*
     * Clear all selected items
     */
    public void clearSelections() {
        log("clearSelections");
        adapterList.clearSelections();
    }

    public void selectAll() {
        if (adapterList != null) {
            adapterList.selectAll();
        }
    }

    public void itemClick(int position) {
        log("itemClick");
        if(megaChatApi.isSignalActivityRequired()){
            megaChatApi.signalPresenceActivity();
        }

        adapterList.toggleSelection(position);
        List<MegaChatListItem> chats = adapterList.getSelectedChats();

        if(context instanceof  ChatExplorerActivity){
            if (chats.size() > 0){
                log("Show FAB button to send");
                ((ChatExplorerActivity)context).showFabButton(true);
            }
            else{
                ((ChatExplorerActivity)context).showFabButton(false);
            }
        }
        else{
            if (chats.size() > 0){
                log("Show FAB button to send");
                ((FileExplorerActivityLollipop)context).showFabButton(true);
            }
            else{
                ((FileExplorerActivityLollipop)context).showFabButton(false);
            }
        }
    }
    /////END Multiselect/////

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
            log("Participant1: "+participantFirstName);
            return participantLastName;
        }
        else{
            log("Participant2: "+participantLastName);
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
        log("onSaveInstanceState");
        super.onSaveInstanceState(outState);
        if(listView.getLayoutManager()!=null){
            outState.putParcelable(BUNDLE_RECYCLER_LAYOUT, listView.getLayoutManager().onSaveInstanceState());
        }
    }

    @Override
    public void onPause() {
        log("onPause");
        lastFirstVisiblePosition = ((LinearLayoutManager)listView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        super.onPause();
    }

    @Override
    public void onResume() {
        log("onResume: lastFirstVisiblePosition " +lastFirstVisiblePosition);
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
        log("onActivityCreated");
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

    private static void log(String log) {
        Util.log("ChatExplorerFragment", log);
    }
}
