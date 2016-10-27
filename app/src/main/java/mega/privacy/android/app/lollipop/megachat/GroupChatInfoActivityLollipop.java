package mega.privacy.android.app.lollipop.megachat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.MegaLinearLayoutManager;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.components.SimpleDividerItemDecoration;
import mega.privacy.android.app.lollipop.MegaContactsLollipopAdapter;
import mega.privacy.android.app.lollipop.PinActivityLollipop;
import mega.privacy.android.app.lollipop.adapters.MegaListChatLollipopAdapter;
import mega.privacy.android.app.lollipop.adapters.MegaParticipantsChatLollipopAdapter;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;


public class GroupChatInfoActivityLollipop extends PinActivityLollipop implements MegaChatRequestListenerInterface, View.OnClickListener, MegaRequestListenerInterface, CompoundButton.OnCheckedChangeListener, AdapterView.OnItemClickListener {

    GroupChatInfoActivityLollipop groupChatInfoActivity;

    MegaUser user;
    long chatHandle;
    MegaChatRoom chat;

    private MegaApiAndroid megaApi = null;
    MegaChatApiAndroid megaChatApi = null;

    private Handler handler;

    Display display;
    DisplayMetrics outMetrics;
    float density;
    float scaleW;
    float scaleH;

    DatabaseHandler dbH = null;
    ChatPreferences chatPrefs = null;

    CoordinatorLayout fragmentContainer;
    TextView initialLetter;
    RoundedImageView avatarImageView;

    LinearLayout infoLayout;
    RelativeLayout avatarLayout;
    TextView infoTitleChatText;
    TextView infoNumParticipantsText;
    RelativeLayout infoTextContainerLayout;
    ImageView editImageView;

    LinearLayout notificationsLayout;
    Switch notificationsSwitch;
    TextView notificationsTitle;
    TextView notificationSelectedText;

    RelativeLayout messageSoundLayout;
    TextView messageSoundText;
    View dividerMessageSoundLayout;

    RelativeLayout ringtoneLayout;
    TextView ringtoneText;
    View dividerRingtoneLayout;

    RelativeLayout clearChatLayout;
    View dividerClearLayout;
    RelativeLayout leaveChatLayout;

    TextView participantsTitle;
    long participantsCount;

    RecyclerView recyclerView;
    MegaParticipantsChatLollipopAdapter adapter;
    ArrayList<MegaChatParticipant> participants;

    Toolbar toolbar;
    ActionBar aB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        log("onCreate");
        groupChatInfoActivity = this;
        if (megaApi == null) {
            MegaApplication app = (MegaApplication) getApplication();
            megaApi = app.getMegaApi();
        }

        if (megaChatApi == null) {
            MegaApplication app = (MegaApplication) getApplication();
            megaChatApi = app.getMegaChatApi();
        }

        handler = new Handler();

        display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        density = getResources().getDisplayMetrics().density;

        scaleW = Util.getScaleW(outMetrics, density);
        scaleH = Util.getScaleH(outMetrics, density);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            chatHandle = extras.getLong("handle", -1);
            if (chatHandle == -1) {
                finish();
                return;
            }

            chat = megaChatApi.getChatRoom(chatHandle);

            dbH = DatabaseHandler.getDbHandler(getApplicationContext());
            chatPrefs = dbH.findChatPreferencesByHandle(String.valueOf(chatHandle));

            setContentView(R.layout.activity_group_chat_properties);

            fragmentContainer = (CoordinatorLayout) findViewById(R.id.fragment_container_group_chat);
            toolbar = (Toolbar) findViewById(R.id.toolbar_group_chat_properties);
            setSupportActionBar(toolbar);
            aB = getSupportActionBar();

            aB.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white);
            aB.setHomeButtonEnabled(true);
            aB.setDisplayHomeAsUpEnabled(true);

            aB.setTitle(chat.getTitle());

            initialLetter = (TextView) findViewById(R.id.chat_contact_properties_toolbar_initial_letter);

            float scaleText;
            if (scaleH < scaleW) {
                scaleText = scaleH;
            } else {
                scaleText = scaleW;
            }

            //Info Layout
            avatarImageView = (RoundedImageView) findViewById(R.id.chat_group_properties_thumbnail);
            initialLetter = (TextView) findViewById(R.id.chat_group_properties_initial_letter);

            if (chat.getTitle().length() > 0){
                String chatTitle = chat.getTitle().trim();
                String firstLetter = chatTitle.charAt(0) + "";
                firstLetter = firstLetter.toUpperCase(Locale.getDefault());
                initialLetter.setText(firstLetter);
            }

            createGroupChatAvatar();

            infoLayout = (LinearLayout) findViewById(R.id.chat_group_contact_properties_info_layout);
            infoLayout.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams paramsInfo = (LinearLayout.LayoutParams) infoLayout.getLayoutParams();
            paramsInfo.leftMargin = Util.scaleWidthPx(16, outMetrics);
            paramsInfo.topMargin = Util.scaleHeightPx(2, outMetrics);
            infoLayout.setLayoutParams(paramsInfo);
            avatarLayout = (RelativeLayout) findViewById(R.id.chat_group_properties_avatar_layout);

            infoTextContainerLayout = (RelativeLayout) findViewById(R.id.chat_group_contact_properties_info_text_container);
            LinearLayout.LayoutParams paramsInfoText = (LinearLayout.LayoutParams) infoTextContainerLayout.getLayoutParams();
            paramsInfoText.leftMargin = Util.scaleWidthPx(16, outMetrics);
            infoTextContainerLayout.setLayoutParams(paramsInfoText);

            infoTitleChatText = (TextView) findViewById(R.id.chat_group_contact_properties_info_title);
            infoTitleChatText.setText(chat.getTitle());
            infoNumParticipantsText = (TextView) findViewById(R.id.chat_group_contact_properties_info_participants);
            participantsCount = chat.getPeerCount();
            infoNumParticipantsText.setText(participantsCount+ " "+ getString(R.string.participants_chat_label));

            editImageView = (ImageView) findViewById(R.id.chat_group_contact_properties_edit_icon);
            RelativeLayout.LayoutParams paramsEditIcon = (RelativeLayout.LayoutParams) editImageView.getLayoutParams();
            paramsEditIcon.leftMargin = Util.scaleWidthPx(15, outMetrics);
            editImageView.setLayoutParams(paramsEditIcon);

            //Notifications Layout

            notificationsLayout = (LinearLayout) findViewById(R.id.chat_group_contact_properties_notifications_layout);
            notificationsLayout.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams paramsNotifications = (LinearLayout.LayoutParams) notificationsLayout.getLayoutParams();
            paramsNotifications.leftMargin = Util.scaleWidthPx(72, outMetrics);
            notificationsLayout.setLayoutParams(paramsNotifications);

            notificationsTitle = (TextView) findViewById(R.id.chat_group_contact_properties_notifications_title);
            notificationSelectedText = (TextView) findViewById(R.id.chat_group_contact_properties_notifications_option);

            notificationsSwitch = (Switch) findViewById(R.id.chat_group_contact_properties_switch);
            notificationsSwitch.setOnCheckedChangeListener(this);
            LinearLayout.LayoutParams paramsSwitch = (LinearLayout.LayoutParams) notificationsSwitch.getLayoutParams();
            paramsSwitch.rightMargin = Util.scaleWidthPx(16, outMetrics);
            notificationsSwitch.setLayoutParams(paramsSwitch);

            //Chat message sound Layout

            messageSoundLayout = (RelativeLayout) findViewById(R.id.chat_group_contact_properties_messages_sound_layout);
            messageSoundLayout.setOnClickListener(this);
            LinearLayout.LayoutParams paramsSound = (LinearLayout.LayoutParams) messageSoundLayout.getLayoutParams();
            paramsSound.leftMargin = Util.scaleWidthPx(72, outMetrics);
            messageSoundLayout.setLayoutParams(paramsSound);

            messageSoundText = (TextView) findViewById(R.id.chat_group_contact_properties_messages_sound);

            dividerMessageSoundLayout = (View) findViewById(R.id.divider_message_sound_layout);
            LinearLayout.LayoutParams paramsDividerSound = (LinearLayout.LayoutParams) dividerMessageSoundLayout.getLayoutParams();
            paramsDividerSound.leftMargin = Util.scaleWidthPx(72, outMetrics);
            dividerMessageSoundLayout.setLayoutParams(paramsDividerSound);

            //Call ringtone Layout

            ringtoneLayout = (RelativeLayout) findViewById(R.id.chat_group_contact_properties_ringtone_layout);
            ringtoneLayout.setOnClickListener(this);
            LinearLayout.LayoutParams paramsRingtone = (LinearLayout.LayoutParams) ringtoneLayout.getLayoutParams();
            paramsRingtone.leftMargin = Util.scaleWidthPx(72, outMetrics);
            ringtoneLayout.setLayoutParams(paramsRingtone);

            ringtoneText = (TextView) findViewById(R.id.chat_group_contact_properties_ringtone);

            dividerRingtoneLayout = (View) findViewById(R.id.divider_ringtone_layout);
            LinearLayout.LayoutParams paramsRingtoneDivider = (LinearLayout.LayoutParams) dividerRingtoneLayout.getLayoutParams();
            paramsRingtoneDivider.leftMargin = Util.scaleWidthPx(72, outMetrics);
            dividerRingtoneLayout.setLayoutParams(paramsRingtoneDivider);

            //Clear chat Layout
            clearChatLayout = (RelativeLayout) findViewById(R.id.chat_group_contact_properties_clear_layout);
            clearChatLayout.setOnClickListener(this);
            LinearLayout.LayoutParams paramsClearChat = (LinearLayout.LayoutParams) clearChatLayout.getLayoutParams();
            paramsClearChat.leftMargin = Util.scaleWidthPx(72, outMetrics);
            clearChatLayout.setLayoutParams(paramsClearChat);

            dividerClearLayout = (View) findViewById(R.id.divider_clear_layout);
            LinearLayout.LayoutParams paramsClearDivider = (LinearLayout.LayoutParams) dividerClearLayout.getLayoutParams();
            paramsClearDivider.leftMargin = Util.scaleWidthPx(72, outMetrics);
            dividerClearLayout.setLayoutParams(paramsClearDivider);

            //Leave chat Layout
            leaveChatLayout = (RelativeLayout) findViewById(R.id.chat_group_contact_properties_leave_layout);
            leaveChatLayout.setOnClickListener(this);
            LinearLayout.LayoutParams paramsLeaveChat = (LinearLayout.LayoutParams) leaveChatLayout.getLayoutParams();
            paramsLeaveChat.leftMargin = Util.scaleWidthPx(72, outMetrics);
            leaveChatLayout.setLayoutParams(paramsLeaveChat);

            participantsTitle = (TextView) findViewById(R.id.chat_group_contact_properties_title_text);
            RelativeLayout.LayoutParams paramsPartTitle = (RelativeLayout.LayoutParams) participantsTitle.getLayoutParams();
            paramsPartTitle.leftMargin = Util.scaleWidthPx(72, outMetrics);
            participantsTitle.setLayoutParams(paramsPartTitle);

            recyclerView = (RecyclerView) findViewById(R.id.contacts_list_view);
            recyclerView.setPadding(0, 0, 0, Util.scaleHeightPx(85, outMetrics));
            recyclerView.setClipToPadding(false);
            recyclerView.addItemDecoration(new SimpleDividerItemDecoration(this, outMetrics));
            recyclerView.setHasFixedSize(true);
            MegaLinearLayoutManager linearLayoutManager = new MegaLinearLayoutManager(this);
            recyclerView.setLayoutManager(linearLayoutManager);
//            recyclerView.addOnItemTouchListener(this);
            recyclerView.setItemAnimator(new DefaultItemAnimator());

            participants = new ArrayList<>();

            for(int i=0;i<participantsCount;i++){
                long peerHandle = chat.getPeerHandle(i);
                int peerPrivilege = chat.getPeerPrivilege(i);

//                String userHandleEncoded = MegaApiAndroid.userHandleToBase64(peerHandle);
//                user = megaApi.getContact(userHandleEncoded);

                MegaChatParticipant participant = new MegaChatParticipant(peerHandle, null, null, null, peerPrivilege);
                participants.add(participant);
            }

            if (adapter == null){
                adapter = new MegaParticipantsChatLollipopAdapter(this, participants, recyclerView);
            }
            else{
                adapter.setParticipants(participants);
            }

            adapter.setPositionClicked(-1);
            recyclerView.setAdapter(adapter);

        }
    }

    public void createGroupChatAvatar(){
        log("createGroupChatAvatar()");

        Bitmap defaultAvatar = Bitmap.createBitmap(Constants.DEFAULT_AVATAR_WIDTH_HEIGHT,Constants.DEFAULT_AVATAR_WIDTH_HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(defaultAvatar);
        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(getResources().getColor(R.color.lollipop_primary_color));

        int radius;
        if (defaultAvatar.getWidth() < defaultAvatar.getHeight())
            radius = defaultAvatar.getWidth()/2;
        else
            radius = defaultAvatar.getHeight()/2;

        c.drawCircle(defaultAvatar.getWidth()/2, defaultAvatar.getHeight()/2, radius, p);
        avatarImageView.setImageBitmap(defaultAvatar);

        Display display = getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics ();
        display.getMetrics(outMetrics);
        float density  = getResources().getDisplayMetrics().density;

        int avatarTextSize = getAvatarTextSize(density);
        log("DENSITY: " + density + ":::: " + avatarTextSize);

        initialLetter.setTextColor(Color.WHITE);
        initialLetter.setVisibility(View.VISIBLE);
        initialLetter.setTextSize(24);
    }

    private int getAvatarTextSize (float density){
        float textSize = 0.0f;

        if (density > 3.0){
            textSize = density * (DisplayMetrics.DENSITY_XXXHIGH / 72.0f);
        }
        else if (density > 2.0){
            textSize = density * (DisplayMetrics.DENSITY_XXHIGH / 72.0f);
        }
        else if (density > 1.5){
            textSize = density * (DisplayMetrics.DENSITY_XHIGH / 72.0f);
        }
        else if (density > 1.0){
            textSize = density * (72.0f / DisplayMetrics.DENSITY_HIGH / 72.0f);
        }
        else if (density > 0.75){
            textSize = density * (72.0f / DisplayMetrics.DENSITY_MEDIUM / 72.0f);
        }
        else{
            textSize = density * (72.0f / DisplayMetrics.DENSITY_LOW / 72.0f);
        }

        return (int)textSize;
    }


    @Override
    public void onClick(View view) {

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {

    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {

    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }
}
