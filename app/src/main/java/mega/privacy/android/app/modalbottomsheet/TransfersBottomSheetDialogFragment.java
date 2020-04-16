package mega.privacy.android.app.modalbottomsheet;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import mega.privacy.android.app.DatabaseHandler;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.AccountController;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaTransfer;

import static mega.privacy.android.app.utils.LogUtil.*;

public class TransfersBottomSheetDialogFragment extends BottomSheetDialogFragment implements View.OnClickListener {

    Context context;
    AccountController aC;

    private BottomSheetBehavior mBehavior;
    private LinearLayout items_layout;

    public LinearLayout mainLinearLayout;
    public TextView titleText;
    public LinearLayout optionTransferManager;
    public LinearLayout optionPauseAll;
    public LinearLayout optionClear;
    public LinearLayout optionCancel;
    public ImageView iconPause;
    public TextView textPause;

    DisplayMetrics outMetrics;
    private int heightDisplay;

    MegaApiAndroid megaApi;
    DatabaseHandler dbH;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (megaApi == null){
            megaApi = ((MegaApplication) ((Activity)context).getApplication()).getMegaApi();
        }

        aC = new AccountController(context);

        dbH = DatabaseHandler.getDbHandler(getActivity());
    }
    @Override
    public void setupDialog(final Dialog dialog, int style) {

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        heightDisplay = outMetrics.heightPixels;

        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.bottom_sheet_transfers, null);

        mainLinearLayout = (LinearLayout) contentView.findViewById(R.id.transfers_bottom_sheet);
        items_layout = (LinearLayout) contentView.findViewById(R.id.items_layout);

        titleText = (TextView) contentView.findViewById(R.id.transfers_title_text);

        optionTransferManager= (LinearLayout) contentView.findViewById(R.id.transfers_manager_option_layout);
        optionPauseAll = (LinearLayout) contentView.findViewById(R.id.transfers_pause_layout);
        iconPause = (ImageView) contentView.findViewById(R.id.transfers_option_pause);
        textPause = (TextView) contentView.findViewById(R.id.transfers_option_pause_text);
        optionClear = (LinearLayout) contentView.findViewById(R.id.transfers_clear_layout);
        optionCancel = (LinearLayout) contentView.findViewById(R.id.transfers_cancel_layout);

        if(megaApi.areTransfersPaused(MegaTransfer.TYPE_DOWNLOAD)||megaApi.areTransfersPaused(MegaTransfer.TYPE_UPLOAD)){
            logDebug("Show PLAY button");
            iconPause.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_play));
            textPause.setText(getString(R.string.option_to_resume_transfers));
        }
        else{
            logDebug("Show PAUSE button");
            iconPause.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_pause));
            textPause.setText(getString(R.string.option_to_pause_transfers));
        }

        optionTransferManager.setOnClickListener(this);
        optionPauseAll.setOnClickListener(this);
        optionClear.setOnClickListener(this);
        optionCancel.setOnClickListener(this);

        dialog.setContentView(contentView);
        mBehavior = BottomSheetBehavior.from((View) mainLinearLayout.getParent());
        mBehavior.setPeekHeight(UtilsModalBottomSheet.getPeekHeight(items_layout, heightDisplay, context, 48));
        mBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

    }


    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.transfers_manager_option_layout:{
                logDebug("Option manager layout");
                ((ManagerActivityLollipop) getActivity()).selectDrawerItemLollipop(ManagerActivityLollipop.DrawerItem.TRANSFERS);
                dismissAllowingStateLoss();
                break;
            }
            case R.id.transfers_pause_layout:{
                logDebug("Option pause/play");
                ((ManagerActivityLollipop) context).changeTransfersStatus();
                break;
            }
            case R.id.transfers_clear_layout:{
                logDebug("Option clear transfers");
                ((ManagerActivityLollipop) context).showConfirmationClearCompletedTransfers();
                break;
            }
            case R.id.transfers_cancel_layout:{
                logDebug("Option cancel ALL transfers");
                ((ManagerActivityLollipop) context).showConfirmationCancelAllTransfers();
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
        this.context = activity;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }
}
