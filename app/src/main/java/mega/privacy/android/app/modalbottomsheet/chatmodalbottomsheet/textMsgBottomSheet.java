package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.controllers.NodeController;
import mega.privacy.android.app.lollipop.megachat.AndroidMegaChatMessage;
import mega.privacy.android.app.lollipop.megachat.ChatActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.ContactAttachmentActivityLollipop;
import mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaChatApiAndroid;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaHandleList;

import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logWarning;

public class textMsgBottomSheet extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    MegaHandleList handleList;
    AndroidMegaChatMessage message = null;
    long chatId;
    long messageId;
    String email=null;

    int position;

    private BottomSheetBehavior mBehavior;

    private LinearLayout mainLinearLayout;
    private LinearLayout reactionsLayout;
    private LinearLayout itemsLayout;
    private LinearLayout optionForward;
    private LinearLayout optionCopy;
    private DisplayMetrics outMetrics;

    static ManagerActivityLollipop.DrawerItem drawerItem = null;
    private int heightDisplay;
    private MegaApiAndroid megaApi;
    private MegaChatApiAndroid megaChatApi;
    private DatabaseHandler dbH;
    NodeController nC;
    ChatController chatC;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        logDebug("onCreate");

        if (megaApi == null) {
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        if (megaChatApi == null) {
            megaChatApi = MegaApplication.getInstance().getMegaChatApi();
        }

        if (savedInstanceState != null) {
            logDebug("Bundle is NOT NULL");
            chatId = savedInstanceState.getLong("chatId", -1);
            messageId = savedInstanceState.getLong("messageId", -1);
            logDebug("Chat ID: " + chatId + ", Message ID: " + messageId);
            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);

            if (messageMega != null) {
                message = new AndroidMegaChatMessage(messageMega);
            }
        } else {
            logWarning("Bundle NULL");

            if (context instanceof ChatActivityLollipop) {
                chatId = ((ChatActivityLollipop) context).idChat;
                messageId = ((ChatActivityLollipop) context).selectedMessageId;
            }
            logDebug("Chat ID: " + chatId + ", Message ID: " + messageId);
            MegaChatMessage messageMega = megaChatApi.getMessage(chatId, messageId);

            if (messageMega != null) {
                message = new AndroidMegaChatMessage(messageMega);
            }
        }

        nC = new NodeController(context);
        chatC = new ChatController(context);
        dbH = MegaApplication.getInstance().getDbH();
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_text_msg, null);

        mainLinearLayout = contentView.findViewById(R.id.bottom_sheet_text_msg);

        reactionsLayout = contentView.findViewById(R.id.reaction_layout);

        itemsLayout = contentView.findViewById(R.id.items_layout);
        optionForward= contentView.findViewById(R.id.forward_layout);
        optionCopy = contentView.findViewById(R.id.copy_layout);

        optionForward.setOnClickListener(this);
        optionCopy.setOnClickListener(this);

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(itemsLayout, heightDisplay, context, 48));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);



    }


    @Override
    public void onClick(View view) {

    }
}
