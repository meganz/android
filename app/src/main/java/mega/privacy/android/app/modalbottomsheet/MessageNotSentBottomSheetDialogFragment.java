package mega.privacy.android.app.modalbottomsheet;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.components.RoundedImageView;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.MegaChatParticipant;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatRoom;

public class MessageNotSentBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
//    MegaChatListItem chat = null;
    MegaChatParticipant selectedParticipant;
    MegaChatRoom selectedChat;

    private BottomSheetBehavior mBehavior;

    public LinearLayout mainLinearLayout;
    public TextView titleSlidingPanel;

    public LinearLayout optionRetryLayout;
    public LinearLayout optionDeleteLayout;
    ////

    DisplayMetrics outMetrics;

    MegaApiAndroid megaApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        if(context instanceof GroupChatInfoActivityLollipop){
            selectedParticipant = ((GroupChatInfoActivityLollipop) context).getSelectedParticipant();
        }

        if(context instanceof GroupChatInfoActivityLollipop){
            selectedChat = ((GroupChatInfoActivityLollipop) context).getChat();
        }

    }
    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.msg_not_sent_bottom_sheet, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.msg_not_sent_bottom_sheet);

        titleSlidingPanel = (TextView)  contentView.findViewById(R.id.msg_not_sent_title_text);
        optionRetryLayout = (LinearLayout) contentView.findViewById(R.id.msg_not_sent_retry_layout);
        optionDeleteLayout = (LinearLayout) contentView.findViewById(R.id.msg_not_sent_delete_layout);

        optionRetryLayout.setOnClickListener(this);
        optionDeleteLayout.setOnClickListener(this);

        dialog.setContentView(contentView);
    }


    @Override
    public void onClick(View v) {

        MegaChatParticipant selectedParticipant = null;
        MegaChatRoom selectedChat = null;
        if(context instanceof GroupChatInfoActivityLollipop){
            selectedParticipant = ((GroupChatInfoActivityLollipop) context).getSelectedParticipant();
            selectedChat = ((GroupChatInfoActivityLollipop) context).getChat();
        }

        switch(v.getId()){

            case R.id.msg_not_sent_retry_layout: {
                log("retry option click");
                ((ChatActivityLollipop) context).showSnackbar("Not yet implemented");
//                dismissAllowingStateLoss();
                break;
            }

            case R.id.msg_not_sent_delete_layout: {
                log("delete option click");
                ((ChatActivityLollipop) context).showSnackbar("Not yet implemented");
                break;
            }
        }
//        dismiss();
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    private static void log(String log) {
        Util.log("MessageNotSentBottomSheetDialogFragment", log);
    }
}
