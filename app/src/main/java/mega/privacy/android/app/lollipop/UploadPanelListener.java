package mega.privacy.android.app.lollipop;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import mega.privacy.android.app.R;
import mega.privacy.android.app.utils.Util;

public class UploadPanelListener implements View.OnClickListener {

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;

    public UploadPanelListener(Context context){
        log("UploadPanelListener created");
        this.context = context;
    }

    @Override
    public void onClick(View v) {
        log("onClick UploadPanelListener");

        switch(v.getId()){

            case R.id.file_list_upload_audio_layout:{
                log("click upload audio");
                ((ManagerActivityLollipop)context).hideUploadPanel();
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("*/*");
                ((ManagerActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), ManagerActivityLollipop.REQUEST_CODE_GET);

                break;
            }

            case R.id.file_list_upload_video_layout:{
                log("click upload video");
                ((ManagerActivityLollipop)context).hideUploadPanel();
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("*/*");
                ((ManagerActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), ManagerActivityLollipop.REQUEST_CODE_GET);
                break;
            }

            case R.id.file_list_upload_image_layout:{
                log("click upload image");
                ((ManagerActivityLollipop)context).hideUploadPanel();
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                intent.setType("*/*");
                ((ManagerActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), ManagerActivityLollipop.REQUEST_CODE_GET);
                break;
            }

            case R.id.file_list_upload_from_system_layout:{
                log("click upload from_system");
                ((ManagerActivityLollipop)context).hideUploadPanel();
                Intent intent = new Intent();
                intent.setAction(FileStorageActivityLollipop.Mode.PICK_FILE.getAction());
                intent.putExtra(FileStorageActivityLollipop.EXTRA_FROM_SETTINGS, false);
                intent.setClass(((ManagerActivityLollipop)context), FileStorageActivityLollipop.class);
                ((ManagerActivityLollipop)context).startActivityForResult(intent, ManagerActivityLollipop.REQUEST_CODE_GET_LOCAL);
                break;
            }

            case R.id.file_list_out_upload:{
                log("click file_list_out_upload");
                ((ManagerActivityLollipop)context).hideUploadPanel();
                break;
            }
        }
    }

    public static void log(String message) {
        Util.log("UploadPanelListener", message);
    }
}
