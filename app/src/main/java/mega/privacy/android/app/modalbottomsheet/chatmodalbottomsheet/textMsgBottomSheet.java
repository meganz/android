package mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.modalbottomsheet.UtilsModalBottomSheet;
import nz.mega.sdk.MegaApiAndroid;

public class textMsgBottomSheet extends BottomSheetDialogFragment implements View.OnClickListener {

    private Context context;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    private LinearLayout mainLinearLayout;
    private RelativeLayout titleLayout;
    private LinearLayout optionForward;
    private LinearLayout optionCopy;
    private DisplayMetrics outMetrics;

    private MegaApiAndroid megaApi;
    private DatabaseHandler dbH;
    private int heightDisplay;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = MegaApplication.getInstance().getMegaApi();
        }

        dbH = MegaApplication.getInstance().getDbH();
    }

    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_text_msg, null);

        mainLinearLayout = contentView.findViewById(R.id.bottom_sheet_text_msg);
        items_layout = contentView.findViewById(R.id.items_layout);

        titleLayout = contentView.findViewById(R.id.bottom_sheet_text_msg_title_layout);

        optionForward= contentView.findViewById(R.id.forward_layout);
        optionCopy = contentView.findViewById(R.id.copy_layout);

        optionForward.setOnClickListener(this);
        optionCopy.setOnClickListener(this);

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 48));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }


    @Override
    public void onClick(View view) {

    }
}
