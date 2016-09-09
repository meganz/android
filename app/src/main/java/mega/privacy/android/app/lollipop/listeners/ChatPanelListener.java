package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.view.View;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Util;

public class ChatPanelListener implements View.OnClickListener {

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;

    public ChatPanelListener(Context context){
        log("UploadPanelListener created");
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        log("onClick ChatPanelListener");

        switch(v.getId()){

            case R.id.file_list_info_chat_layout:{
                log("click contact info");
                ((ManagerActivityLollipop)context).hideChatPanel();
//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
//                intent.setAction(Intent.ACTION_GET_CONTENT);
//                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//                intent.setType("*/*");
//                ((ManagerActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), Constants.REQUEST_CODE_GET);

                break;
            }

            case R.id.file_list_leave_chat_layout:{
                log("click leave chat");
                ((ManagerActivityLollipop)context).hideChatPanel();
//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
//                intent.setAction(Intent.ACTION_GET_CONTENT);
//                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//                intent.setType("*/*");
//                ((ManagerActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), Constants.REQUEST_CODE_GET);
                break;
            }

            case R.id.file_list_mute_chat_layout:{
                log("click mute chat");
                ((ManagerActivityLollipop)context).hideChatPanel();
//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
//                intent.setAction(Intent.ACTION_GET_CONTENT);
//                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//                intent.setType("*/*");
//                ((ManagerActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), Constants.REQUEST_CODE_GET);
                break;
            }

            case R.id.file_list_out_chat:{
                log("click out chat panel");
                ((ManagerActivityLollipop)context).hideChatPanel();
                break;
            }
        }
    }

    public static void log(String message) {
        Util.log("ChatPanelListener", message);
    }
}
