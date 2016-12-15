package mega.privacy.android.app.modalbottomsheet;


import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StyleRes;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.view.LayoutInflater;
import android.view.View;

import mega.privacy.android.app.R;

public class ChatModalBottomSheetDialog extends BottomSheetDialog {

    public ChatModalBottomSheetDialog(@NonNull Context context) {
        super(context);
    }

    protected ChatModalBottomSheetDialog(@NonNull Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    public ChatModalBottomSheetDialog(@NonNull Context context, @StyleRes int theme) {
        super(context, theme);
    }

    @Override
    public void show() {
        super.show();
//        final View view = findViewById(R.id.chat_item_bottom_sheet);
//        final View sheetView = findViewById(R.id.chat_item_bottom_sheet);

        LayoutInflater inflater = getLayoutInflater();
        final View dialogLayout = inflater.inflate(R.layout.chat_item_bottom_sheet, null);

        dialogLayout.post(new Runnable() {
            @Override
            public void run() {
                BottomSheetBehavior behavior = BottomSheetBehavior.from(dialogLayout);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });


//        final View view = findViewById(R.id.title_chat_activity_bottom_sheet);
//        view.post(new Runnable() {
//            @Override
//            public void run() {
//                BottomSheetBehavior behavior = BottomSheetBehavior.from(view);
//                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
//
//            }
//        });
    }
}
