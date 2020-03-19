package mega.privacy.android.app.modalbottomsheet;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ContactInfoActivityLollipop;

import static mega.privacy.android.app.utils.Constants.*;

public class ContactNicknameBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    private final static int MAX_HEIGHT = 48;
    public LinearLayout mainLinearLayout;
    public TextView titleText;
    public LinearLayout optionEditNickname;
    public LinearLayout optionRemoveNickname;
    protected Context context;
    private DisplayMetrics outMetrics;
    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;
    private int heightDisplay;
    private String nickname;
    private ContactInfoActivityLollipop contactInfoActivityLollipop = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            nickname = savedInstanceState.getString(EXTRA_USER_NICKNAME, null);
        } else if (context instanceof ContactInfoActivityLollipop) {
            contactInfoActivityLollipop = ((ContactInfoActivityLollipop) context);
            nickname = contactInfoActivityLollipop.getNickname();
        }
    }

    @Override
    public void onClick(View v) {
        if(contactInfoActivityLollipop == null) return;
        switch (v.getId()) {
            case R.id.edit_nickname_layout: {
                contactInfoActivityLollipop.showConfirmationSetNickname(nickname);
                break;
            }
            case R.id.remove_nickname_layout: {
                contactInfoActivityLollipop.addNickname(null, null);
                break;
            }
        }
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }


    @Override
    public void setupDialog(final Dialog dialog, int style) {
        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);
        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);

        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_nickname, null);
        mainLinearLayout = contentView.findViewById(R.id.nickname_bottom_sheet);
        items_layout = contentView.findViewById(R.id.items_layout);
        titleText = contentView.findViewById(R.id.nickname_title_text);
        optionEditNickname = contentView.findViewById(R.id.edit_nickname_layout);
        optionRemoveNickname = contentView.findViewById(R.id.remove_nickname_layout);
        optionEditNickname.setOnClickListener(this);
        optionRemoveNickname.setOnClickListener(this);
        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, getContext(), MAX_HEIGHT));
        mBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(EXTRA_USER_NICKNAME, nickname);
    }
}
