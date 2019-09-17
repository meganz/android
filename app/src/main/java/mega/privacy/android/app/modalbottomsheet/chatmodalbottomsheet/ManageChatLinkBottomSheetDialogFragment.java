package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;


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
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet;
import nz.mega.sdk.MegaChatApiAndroid;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ManageChatLinkBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;

    private BottomSheetBehavior mBehavior;
    private LinearLayout itemsLayout;

    public LinearLayout mainLinearLayout;
    public TextView titleSlidingPanel;

    public LinearLayout optionCopyLayout;
    public LinearLayout optionDeleteLayout;
    ////

    DisplayMetrics outMetrics;

    MegaChatApiAndroid megaChatApi;

    long chatId;

    private int heightDisplay;

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

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.manage_chat_link_bottom_sheet, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.manage_chat_link_bottom_sheet);
        itemsLayout = (LinearLayout) contentView.findViewById(R.id.manage_chat_link_items_layout);

        titleSlidingPanel = (TextView)  contentView.findViewById(R.id.manage_chat_link_title_text);
        optionCopyLayout = (LinearLayout) contentView.findViewById(R.id.manage_chat_link_copy_layout);
        optionDeleteLayout = (LinearLayout) contentView.findViewById(R.id.manage_chat_link_delete_layout);
        optionDeleteLayout.setOnClickListener(this);
        optionCopyLayout.setOnClickListener(this);

        dialog.setContentView(contentView);

        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());

        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(itemsLayout, heightDisplay, context, 48));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onClick(View v) {
        logDebug("onClick");

        switch(v.getId()){

            case R.id.manage_chat_link_copy_layout: {
                ((GroupChatInfoActivityLollipop) context).copyLink();
                break;
            }

            case R.id.manage_chat_link_delete_layout: {
                showConfirmationRemoveChatLink(context);
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
}
