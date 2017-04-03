package mega.privacy.android.app.modalbottomsheet;


import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatRoom;

public class MessageNotSentBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;

    private BottomSheetBehavior mBehavior;

    public LinearLayout mainLinearLayout;
    public TextView titleSlidingPanel;

    public LinearLayout optionRetryLayout;
    public LinearLayout optionDeleteLayout;
    ////

    DisplayMetrics outMetrics;

    MegaChatApiAndroid megaChatApi;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaChatApi == null){
            megaChatApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaChatApi();
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
        log("onClick");
        MegaChatRoom selectedChat = null;
        AndroidMegaChatMessage selectedMessage = null;
        if(context instanceof ChatActivityLollipop){
            selectedMessage = ((ChatActivityLollipop) context).getSelectedMessage();
            selectedChat = ((ChatActivityLollipop) context).getChatRoom();
        }

        switch(v.getId()){

            case R.id.msg_not_sent_retry_layout: {
                log("retry option click");
                if(selectedMessage!=null&&selectedChat!=null){
                    ((ChatActivityLollipop) context).removeMsgNotSent();
                    megaChatApi.removeUnsentMessage(selectedChat.getChatId(), selectedMessage.getMessage().getRowId());
                    ((ChatActivityLollipop) context).sendMessage(selectedMessage.getMessage().getContent());
                }
                else{
                    log("onClick: Chat or message are NULL");
                }

                break;
            }

            case R.id.msg_not_sent_delete_layout: {
                log("delete option click");
                if(selectedMessage!=null&&selectedChat!=null){
                    ((ChatActivityLollipop) context).removeMsgNotSentAndUpdate();
                    megaChatApi.removeUnsentMessage(selectedChat.getChatId(), selectedMessage.getMessage().getRowId());
                }
                else{
                    log("onClick: Chat or message are NULL");
                }

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
