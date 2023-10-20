package mega.privacy.android.app.main.megachat;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static mega.privacy.android.app.activities.GiphyPickerActivity.GIF_DATA;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_CLOSE_CHAT_AFTER_IMPORT;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_FIRST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_LAST_NAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_NICKNAME;
import static mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_RETENTION_TIME;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_CHAT_TRANSFER_START;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_ERROR_COPYING_NODES;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_RETRY_PENDING_MESSAGE;
import static mega.privacy.android.app.constants.BroadcastConstants.BROADCAST_ACTION_UPDATE_HISTORY_BY_RT;
import static mega.privacy.android.app.constants.BroadcastConstants.ERROR_MESSAGE_TEXT;
import static mega.privacy.android.app.constants.BroadcastConstants.EXTRA_USER_HANDLE;
import static mega.privacy.android.app.constants.BroadcastConstants.PENDING_MESSAGE_ID;
import static mega.privacy.android.app.constants.BroadcastConstants.RETENTION_TIME;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_COMPOSITION_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_ON_HOLD_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CALL_STATUS_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_CHAT_OPEN_INVITE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_SESSION_ON_HOLD_CHANGE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_WAITING_FOR_OTHERS;
import static mega.privacy.android.app.globalmanagement.TransfersManagement.isServiceRunning;
import static mega.privacy.android.app.main.megachat.MapsActivity.EDITING_MESSAGE;
import static mega.privacy.android.app.main.megachat.MapsActivity.LATITUDE;
import static mega.privacy.android.app.main.megachat.MapsActivity.LONGITUDE;
import static mega.privacy.android.app.main.megachat.MapsActivity.MSG_ID;
import static mega.privacy.android.app.main.megachat.MapsActivity.SNAPSHOT;
import static mega.privacy.android.app.main.megachat.MapsActivity.getAddresses;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_ACTION_IN;
import static mega.privacy.android.app.meeting.activity.MeetingActivity.MEETING_CHAT_ID;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.isBottomSheetDialogShown;
import static mega.privacy.android.app.modalbottomsheet.ModalBottomSheetUtil.openWith;
import static mega.privacy.android.app.providers.FileProviderActivity.FROM_MEGA_APP;
import static mega.privacy.android.app.utils.AlertDialogUtil.dismissAlertDialogIfExists;
import static mega.privacy.android.app.utils.AlertsAndWarnings.showOverDiskQuotaPaywallWarning;
import static mega.privacy.android.app.utils.CacheFolderManager.buildVoiceClipFile;
import static mega.privacy.android.app.utils.CallUtil.activateChrono;
import static mega.privacy.android.app.utils.CallUtil.callStatusToString;
import static mega.privacy.android.app.utils.CallUtil.checkIfCanJoinOneToOneCall;
import static mega.privacy.android.app.utils.CallUtil.getAnotherCallOnHold;
import static mega.privacy.android.app.utils.CallUtil.getCallInProgress;
import static mega.privacy.android.app.utils.CallUtil.getCallsParticipating;
import static mega.privacy.android.app.utils.CallUtil.isMeetingEnded;
import static mega.privacy.android.app.utils.CallUtil.isSessionOnHold;
import static mega.privacy.android.app.utils.CallUtil.isStatusConnected;
import static mega.privacy.android.app.utils.CallUtil.openMeetingRinging;
import static mega.privacy.android.app.utils.CallUtil.participatingInACall;
import static mega.privacy.android.app.utils.CallUtil.returnCall;
import static mega.privacy.android.app.utils.CallUtil.showConfirmationInACall;
import static mega.privacy.android.app.utils.ChatUtil.AUDIOFOCUS_DEFAULT;
import static mega.privacy.android.app.utils.ChatUtil.STREAM_MUSIC_DEFAULT;
import static mega.privacy.android.app.utils.ChatUtil.StatusIconLocation;
import static mega.privacy.android.app.utils.ChatUtil.abandonAudioFocus;
import static mega.privacy.android.app.utils.ChatUtil.areDrawablesIdentical;
import static mega.privacy.android.app.utils.ChatUtil.converterShortCodes;
import static mega.privacy.android.app.utils.ChatUtil.createAttachmentPendingMessage;
import static mega.privacy.android.app.utils.ChatUtil.createMuteNotificationsAlertDialogOfAChat;
import static mega.privacy.android.app.utils.ChatUtil.getAudioFocus;
import static mega.privacy.android.app.utils.ChatUtil.getNameContactAttachment;
import static mega.privacy.android.app.utils.ChatUtil.getRequest;
import static mega.privacy.android.app.utils.ChatUtil.getTitleChat;
import static mega.privacy.android.app.utils.ChatUtil.isEnableChatNotifications;
import static mega.privacy.android.app.utils.ChatUtil.isGeolocation;
import static mega.privacy.android.app.utils.ChatUtil.isItSameMsg;
import static mega.privacy.android.app.utils.ChatUtil.isMsgImage;
import static mega.privacy.android.app.utils.ChatUtil.isVoiceClip;
import static mega.privacy.android.app.utils.ChatUtil.lockOrientationLandscape;
import static mega.privacy.android.app.utils.ChatUtil.lockOrientationPortrait;
import static mega.privacy.android.app.utils.ChatUtil.lockOrientationReverseLandscape;
import static mega.privacy.android.app.utils.ChatUtil.lockOrientationReversePortrait;
import static mega.privacy.android.app.utils.ChatUtil.manageTextFileIntent;
import static mega.privacy.android.app.utils.ChatUtil.setContactStatus;
import static mega.privacy.android.app.utils.ChatUtil.shareMsgFromChat;
import static mega.privacy.android.app.utils.ChatUtil.shareNodesFromChat;
import static mega.privacy.android.app.utils.ChatUtil.shouldMuteOrUnmuteOptionsBeShown;
import static mega.privacy.android.app.utils.ChatUtil.showConfirmationClearChat;
import static mega.privacy.android.app.utils.ChatUtil.showConfirmationLeaveChat;
import static mega.privacy.android.app.utils.ChatUtil.showShareChatLinkDialog;
import static mega.privacy.android.app.utils.ChatUtil.unlockOrientation;
import static mega.privacy.android.app.utils.Constants.ACTION_CHAT_NOTIFICATION_MESSAGE;
import static mega.privacy.android.app.utils.Constants.ACTION_CHAT_OPEN;
import static mega.privacy.android.app.utils.Constants.ACTION_CHAT_SHOW_MESSAGES;
import static mega.privacy.android.app.utils.Constants.ACTION_CHECK_COMPRESSING_MESSAGE;
import static mega.privacy.android.app.utils.Constants.ACTION_JOIN_OPEN_CHAT_LINK;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_CHAT_LINK;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_FOLDER_LINK;
import static mega.privacy.android.app.utils.Constants.ACTION_OPEN_MEGA_LINK;
import static mega.privacy.android.app.utils.Constants.ACTION_UPDATE_ATTACHMENT;
import static mega.privacy.android.app.utils.Constants.APP_DATA_VOICE_CLIP;
import static mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CALL_IN_PROGRESS;
import static mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_PLAY_VOICE_CLIP;
import static mega.privacy.android.app.utils.Constants.AUTHORITY_STRING_FILE_PROVIDER;
import static mega.privacy.android.app.utils.Constants.BROADCAST_ACTION_INTENT_VOICE_CLIP_DOWNLOADED;
import static mega.privacy.android.app.utils.Constants.CHAT_ID;
import static mega.privacy.android.app.utils.Constants.CHAT_LINK_EXTRA;
import static mega.privacy.android.app.utils.Constants.CHECK_LINK_TYPE_CHAT_LINK;
import static mega.privacy.android.app.utils.Constants.CHECK_LINK_TYPE_UNKNOWN_LINK;
import static mega.privacy.android.app.utils.Constants.CONTACT_TYPE_MEGA;
import static mega.privacy.android.app.utils.Constants.DISMISS_ACTION_SNACKBAR;
import static mega.privacy.android.app.utils.Constants.EXTRA_RESULT_TRANSFER;
import static mega.privacy.android.app.utils.Constants.EXTRA_TRANSFER_TYPE;
import static mega.privacy.android.app.utils.Constants.FORWARD_ONLY_OPTION;
import static mega.privacy.android.app.utils.Constants.FROM_CHAT;
import static mega.privacy.android.app.utils.Constants.HANDLE;
import static mega.privacy.android.app.utils.Constants.ID_MESSAGES;
import static mega.privacy.android.app.utils.Constants.IMPORT_ONLY_OPTION;
import static mega.privacy.android.app.utils.Constants.IMPORT_TO_SHARE_OPTION;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CHAT_ID;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_CONTACT_TYPE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_FILE_NAME;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_INSIDE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_IS_PLAYLIST;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_MSG_ID;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_SCREEN_POSITION;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_KEY_TOOL_BAR_TITLE;
import static mega.privacy.android.app.utils.Constants.INTENT_EXTRA_PENDING_MESSAGE_ID;
import static mega.privacy.android.app.utils.Constants.INVALID_ID;
import static mega.privacy.android.app.utils.Constants.INVALID_POSITION;
import static mega.privacy.android.app.utils.Constants.INVALID_VALUE;
import static mega.privacy.android.app.utils.Constants.INVITE_CONTACT_TYPE;
import static mega.privacy.android.app.utils.Constants.LINK_IS_FOR_MEETING;
import static mega.privacy.android.app.utils.Constants.LOCATION_PERMISSION_REQUEST_CODE;
import static mega.privacy.android.app.utils.Constants.LOGIN_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.MAX_REACTIONS_PER_MESSAGE;
import static mega.privacy.android.app.utils.Constants.MAX_REACTIONS_PER_USER;
import static mega.privacy.android.app.utils.Constants.MESSAGE_ID;
import static mega.privacy.android.app.utils.Constants.NOTIFICATIONS_ENABLED;
import static mega.privacy.android.app.utils.Constants.OPENED_FROM_CHAT;
import static mega.privacy.android.app.utils.Constants.REACTION_ERROR_DEFAULT_VALUE;
import static mega.privacy.android.app.utils.Constants.REACTION_ERROR_TYPE_USER;
import static mega.privacy.android.app.utils.Constants.RECORD_VOICE_CLIP;
import static mega.privacy.android.app.utils.Constants.REQUEST_ADD_PARTICIPANTS;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA_SHOW_PREVIEW;
import static mega.privacy.android.app.utils.Constants.REQUEST_CAMERA_TAKE_PICTURE;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_GET_FILES;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_CHAT;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FILE;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_IMPORT_FOLDER;
import static mega.privacy.android.app.utils.Constants.REQUEST_CODE_SEND_LOCATION;
import static mega.privacy.android.app.utils.Constants.REQUEST_READ_STORAGE;
import static mega.privacy.android.app.utils.Constants.REQUEST_SEND_CONTACTS;
import static mega.privacy.android.app.utils.Constants.REQUEST_STORAGE_VOICE_CLIP;
import static mega.privacy.android.app.utils.Constants.REQUEST_WRITE_STORAGE_TAKE_PICTURE;
import static mega.privacy.android.app.utils.Constants.RICH_WARNING_TRUE;
import static mega.privacy.android.app.utils.Constants.SELECTED_CHATS;
import static mega.privacy.android.app.utils.Constants.SELECTED_USERS;
import static mega.privacy.android.app.utils.Constants.SENT_REQUESTS_TYPE;
import static mega.privacy.android.app.utils.Constants.SHOW_SNACKBAR;
import static mega.privacy.android.app.utils.Constants.SNACKBAR_TYPE;
import static mega.privacy.android.app.utils.Constants.TOUR_FRAGMENT;
import static mega.privacy.android.app.utils.Constants.TYPE_END_RECORD;
import static mega.privacy.android.app.utils.Constants.TYPE_ERROR_RECORD;
import static mega.privacy.android.app.utils.Constants.TYPE_START_RECORD;
import static mega.privacy.android.app.utils.Constants.TYPE_VOICE_CLIP;
import static mega.privacy.android.app.utils.Constants.VISIBLE_FRAGMENT;
import static mega.privacy.android.app.utils.FileUtil.getFileFromContentUri;
import static mega.privacy.android.app.utils.FileUtil.getLocalFile;
import static mega.privacy.android.app.utils.FileUtil.isFileAvailable;
import static mega.privacy.android.app.utils.GiphyUtil.getGiphySrc;
import static mega.privacy.android.app.utils.LinksUtil.isMEGALinkAndRequiresTransferSession;
import static mega.privacy.android.app.utils.MegaApiUtils.isIntentAvailable;
import static mega.privacy.android.app.utils.MegaNodeUtil.existsMyChatFilesFolder;
import static mega.privacy.android.app.utils.MegaNodeUtil.getMyChatFilesFolder;
import static mega.privacy.android.app.utils.MegaNodeUtil.onNodeTapped;
import static mega.privacy.android.app.utils.OnSingleClickListener.setOnSingleClickListener;
import static mega.privacy.android.app.utils.StringResourcesUtils.getTranslatedErrorString;
import static mega.privacy.android.app.utils.TextUtil.isTextEmpty;
import static mega.privacy.android.app.utils.TimeUtils.DATE;
import static mega.privacy.android.app.utils.TimeUtils.TIME;
import static mega.privacy.android.app.utils.TimeUtils.lastGreenDate;
import static mega.privacy.android.app.utils.Util.adjustForLargeFont;
import static mega.privacy.android.app.utils.Util.calculateDateFromTimestamp;
import static mega.privacy.android.app.utils.Util.dp2px;
import static mega.privacy.android.app.utils.Util.getMediaIntent;
import static mega.privacy.android.app.utils.Util.isScreenInPortrait;
import static mega.privacy.android.app.utils.Util.mutateIcon;
import static mega.privacy.android.app.utils.Util.scaleHeightPx;
import static mega.privacy.android.app.utils.Util.showErrorAlertDialog;
import static mega.privacy.android.app.utils.Util.toCDATA;
import static mega.privacy.android.app.utils.permission.PermissionUtils.checkMandatoryCallPermissions;
import static mega.privacy.android.app.utils.permission.PermissionUtils.hasPermissions;
import static mega.privacy.android.app.utils.permission.PermissionUtils.requestCallPermissions;
import static mega.privacy.android.app.utils.permission.PermissionUtils.requestPermission;
import static mega.privacy.android.data.facade.FileFacadeKt.INTENT_EXTRA_NODE_HANDLE;
import static nz.mega.sdk.MegaApiJava.INVALID_HANDLE;
import static nz.mega.sdk.MegaChatApi.INIT_ANONYMOUS;
import static nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.util.Patterns;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.view.ActionMode;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.primitives.Longs;
import com.jeremyliao.liveeventbus.LiveEventBus;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import kotlin.Unit;
import mega.privacy.android.app.BuildConfig;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.MimeTypeList;
import mega.privacy.android.app.R;
import mega.privacy.android.app.ShareInfo;
import mega.privacy.android.app.activities.GiphyPickerActivity;
import mega.privacy.android.app.activities.PasscodeActivity;
import mega.privacy.android.app.arch.extensions.ViewExtensionsKt;
import mega.privacy.android.app.components.BubbleDrawable;
import mega.privacy.android.app.components.ChatManagement;
import mega.privacy.android.app.components.MarqueeTextView;
import mega.privacy.android.app.components.NpaLinearLayoutManager;
import mega.privacy.android.app.components.attacher.MegaAttacher;
import mega.privacy.android.app.components.saver.NodeSaver;
import mega.privacy.android.app.components.twemoji.EmojiEditText;
import mega.privacy.android.app.components.twemoji.EmojiKeyboard;
import mega.privacy.android.app.components.twemoji.EmojiManager;
import mega.privacy.android.app.components.twemoji.EmojiTextView;
import mega.privacy.android.app.components.voiceClip.OnBasketAnimationEnd;
import mega.privacy.android.app.components.voiceClip.OnRecordListener;
import mega.privacy.android.app.components.voiceClip.RecordButton;
import mega.privacy.android.app.components.voiceClip.RecordView;
import mega.privacy.android.app.fcm.ChatAdvancedNotificationBuilder;
import mega.privacy.android.app.generalusecase.FilePrepareUseCase;
import mega.privacy.android.app.globalmanagement.ActivityLifecycleHandler;
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler;
import mega.privacy.android.app.imageviewer.ImageViewerActivity;
import mega.privacy.android.app.interfaces.AttachNodeToChatListener;
import mega.privacy.android.app.interfaces.ChatManagementCallback;
import mega.privacy.android.app.interfaces.ChatRoomToolbarBottomSheetDialogActionListener;
import mega.privacy.android.app.interfaces.SnackbarShower;
import mega.privacy.android.app.interfaces.StoreDataBeforeForward;
import mega.privacy.android.app.listeners.CreateChatListener;
import mega.privacy.android.app.listeners.ExportListener;
import mega.privacy.android.app.listeners.GetAttrUserListener;
import mega.privacy.android.app.listeners.GetPeerAttributesListener;
import mega.privacy.android.app.listeners.InviteToChatRoomListener;
import mega.privacy.android.app.listeners.LoadPreviewListener;
import mega.privacy.android.app.listeners.RemoveFromChatRoomListener;
import mega.privacy.android.app.main.AddContactActivity;
import mega.privacy.android.app.main.FileExplorerActivity;
import mega.privacy.android.app.main.ManagerActivity;
import mega.privacy.android.app.main.adapters.RotatableAdapter;
import mega.privacy.android.app.main.controllers.ChatController;
import mega.privacy.android.app.main.controllers.ContactController;
import mega.privacy.android.app.main.listeners.AudioFocusListener;
import mega.privacy.android.app.main.listeners.ChatLinkInfoListener;
import mega.privacy.android.app.main.listeners.MultipleForwardChatProcessor;
import mega.privacy.android.app.main.megachat.chatAdapters.MegaChatAdapter;
import mega.privacy.android.app.mediaplayer.service.AudioPlayerService;
import mega.privacy.android.app.meeting.activity.MeetingActivity;
import mega.privacy.android.app.meeting.fragments.MeetingHasEndedDialogFragment;
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway;
import mega.privacy.android.app.meeting.listeners.HangChatCallListener;
import mega.privacy.android.app.meeting.listeners.SetCallOnHoldListener;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ChatRoomToolbarBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.GeneralChatMessageBottomSheet;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.InfoReactionsBottomSheet;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.MessageNotSentBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.PendingMessageBottomSheetDialogFragment;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.ReactionsBottomSheet;
import mega.privacy.android.app.modalbottomsheet.chatmodalbottomsheet.SendAttachmentChatBottomSheetDialogFragment;
import mega.privacy.android.app.namecollision.data.NameCollision;
import mega.privacy.android.app.namecollision.usecase.CheckNameCollisionUseCase;
import mega.privacy.android.app.objects.GifData;
import mega.privacy.android.app.objects.PasscodeManagement;
import mega.privacy.android.app.presentation.chat.ChatViewModel;
import mega.privacy.android.app.presentation.chat.ContactInvitation;
import mega.privacy.android.app.presentation.chat.dialog.AddParticipantsNoContactsDialogFragment;
import mega.privacy.android.app.presentation.chat.dialog.AddParticipantsNoContactsLeftToAddDialogFragment;
import mega.privacy.android.app.presentation.contactinfo.ContactInfoActivity;
import mega.privacy.android.app.presentation.copynode.mapper.CopyRequestMessageMapper;
import mega.privacy.android.app.presentation.extensions.StorageStateExtensionsKt;
import mega.privacy.android.app.presentation.filelink.FileLinkComposeActivity;
import mega.privacy.android.app.presentation.folderlink.FolderLinkComposeActivity;
import mega.privacy.android.app.presentation.login.LoginActivity;
import mega.privacy.android.app.presentation.meeting.ScheduledMeetingInfoActivity;
import mega.privacy.android.app.presentation.meeting.UsersInWaitingRoomDialogFragment;
import mega.privacy.android.app.presentation.meeting.WaitingRoomActivity;
import mega.privacy.android.app.presentation.meeting.WaitingRoomManagementViewModel;
import mega.privacy.android.app.presentation.pdfviewer.PdfViewerActivity;
import mega.privacy.android.app.psa.PsaWebBrowser;
import mega.privacy.android.app.usecase.GetAvatarUseCase;
import mega.privacy.android.app.usecase.GetNodeUseCase;
import mega.privacy.android.app.usecase.GetPublicNodeUseCase;
import mega.privacy.android.app.usecase.LegacyCopyNodeUseCase;
import mega.privacy.android.app.usecase.LegacyGetPublicLinkInformationUseCase;
import mega.privacy.android.app.usecase.call.EndCallUseCase;
import mega.privacy.android.app.usecase.call.GetCallStatusChangesUseCase;
import mega.privacy.android.app.usecase.call.GetCallUseCase;
import mega.privacy.android.app.usecase.call.GetParticipantsChangesUseCase;
import mega.privacy.android.app.usecase.chat.GetChatChangesUseCase;
import mega.privacy.android.app.utils.AlertDialogUtil;
import mega.privacy.android.app.utils.AlertsAndWarnings;
import mega.privacy.android.app.utils.CallUtil;
import mega.privacy.android.app.utils.ChatUtil;
import mega.privacy.android.app.utils.ColorUtils;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.ContactUtil;
import mega.privacy.android.app.utils.FileUtil;
import mega.privacy.android.app.utils.MegaProgressDialogUtil;
import mega.privacy.android.app.utils.TextUtil;
import mega.privacy.android.app.utils.TimeUtils;
import mega.privacy.android.app.utils.Util;
import mega.privacy.android.app.utils.permission.PermissionUtils;
import mega.privacy.android.data.model.chat.AndroidMegaChatMessage;
import mega.privacy.android.data.model.chat.AndroidMegaRichLinkMessage;
import mega.privacy.android.domain.entity.StorageState;
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting;
import mega.privacy.android.domain.entity.chat.FileGalleryItem;
import mega.privacy.android.domain.entity.chat.PendingMessage;
import mega.privacy.android.domain.entity.chat.PendingMessageState;
import mega.privacy.android.domain.entity.contacts.ContactLink;
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus;
import mega.privacy.android.domain.usecase.GetPushToken;
import mega.privacy.android.domain.usecase.GetThemeMode;
import mega.privacy.android.domain.usecase.permisison.HasMediaPermissionUseCase;
import nz.mega.documentscanner.DocumentScannerActivity;
import nz.mega.sdk.MegaApiAndroid;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaChatApi;
import nz.mega.sdk.MegaChatApiJava;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaChatContainsMeta;
import nz.mega.sdk.MegaChatError;
import nz.mega.sdk.MegaChatGeolocation;
import nz.mega.sdk.MegaChatGiphy;
import nz.mega.sdk.MegaChatListItem;
import nz.mega.sdk.MegaChatMessage;
import nz.mega.sdk.MegaChatPeerList;
import nz.mega.sdk.MegaChatRequest;
import nz.mega.sdk.MegaChatRequestListenerInterface;
import nz.mega.sdk.MegaChatRoom;
import nz.mega.sdk.MegaChatRoomListenerInterface;
import nz.mega.sdk.MegaContactRequest;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaHandleList;
import nz.mega.sdk.MegaNode;
import nz.mega.sdk.MegaNodeList;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaUser;
import timber.log.Timber;

@AndroidEntryPoint
public class ChatActivity extends PasscodeActivity
        implements MegaChatRequestListenerInterface, MegaRequestListenerInterface,
        MegaChatRoomListenerInterface, View.OnClickListener,
        StoreDataBeforeForward<ArrayList<AndroidMegaChatMessage>>, ChatManagementCallback,
        SnackbarShower, AttachNodeToChatListener, HangChatCallListener.OnCallHungUpCallback,
        SetCallOnHoldListener.OnCallOnHoldCallback, LoadPreviewListener.OnPreviewLoadedCallback,
        LoadPreviewListener.OnChatPreviewLoadedCallback, ChatRoomToolbarBottomSheetDialogActionListener {

    private static final int MAX_NAMES_PARTICIPANTS = 3;
    private static final int MIN_LINES_TO_EXPAND_INPUT_TEXT = 4;
    private static final int INVALID_LAST_SEEN_ID = 0;

    public MegaChatAdapter.ViewHolderMessageChat holder_imageDrag;
    public int position_imageDrag = -1;
    private static final String PLAYING = "isAnyPlaying";
    private static final String ID_VOICE_CLIP_PLAYING = "idMessageVoicePlaying";
    private static final String PROGRESS_PLAYING = "progressVoicePlaying";
    private static final String MESSAGE_HANDLE_PLAYING = "messageHandleVoicePlaying";
    private static final String USER_HANDLE_PLAYING = "userHandleVoicePlaying";
    private final static String JOIN_CALL_DIALOG = "isJoinCallDialogShown";
    private final static String ONLY_ME_IN_CALL_DIALOG = "isOnlyMeInCallDialogShown";
    private final static String END_CALL_FOR_ALL_DIALOG = "isEndCallForAllDialogShown";

    private static final String LAST_MESSAGE_SEEN = "LAST_MESSAGE_SEEN";
    private static final String GENERAL_UNREAD_COUNT = "GENERAL_UNREAD_COUNT";
    private static final String SELECTED_ITEMS = "selectedItems";
    private static final String OPENING_AND_JOINING_ACTION = "OPENING_AND_JOINING_ACTION";
    private static final String ERROR_REACTION_DIALOG = "ERROR_REACTION_DIALOG";
    private static final String TYPE_ERROR_REACTION = "TYPE_ERROR_REACTION";
    private static final String NUM_MSGS_RECEIVED_AND_UNREAD = "NUM_MSGS_RECEIVED_AND_UNREAD";
    private final static int NUMBER_MESSAGES_TO_LOAD = 32;
    private final static int MAX_NUMBER_MESSAGES_TO_LOAD_NOT_SEEN = 256;
    private final static int NUMBER_MESSAGES_BEFORE_LOAD = 8;
    public static final int REPEAT_INTERVAL = 40;

    private final static int ROTATION_PORTRAIT = 0;
    private final static int ROTATION_LANDSCAPE = 1;
    private final static int ROTATION_REVERSE_PORTRAIT = 2;
    private final static int ROTATION_REVERSE_LANDSCAPE = 3;
    private final static int MAX_LINES_INPUT_TEXT_COLLAPSED = 5;
    private final static int MAX_LINES_INPUT_TEXT_EXPANDED = Integer.MAX_VALUE;

    private final static int HINT_LAND = 550;
    private final static int HINT_PORT = 250;
    private final static boolean IS_LOW = true;
    private final static boolean IS_HIGH = false;

    public static int MEGA_FILE_LINK = 1;
    public static int MEGA_FOLDER_LINK = 2;
    public static int MEGA_CHAT_LINK = 3;

    private final static int SHOW_WRITING_LAYOUT = 1;
    private final static int SHOW_JOIN_LAYOUT = 2;
    private final static int SHOW_NOTHING_LAYOUT = 3;
    private final static int SHOW_JOINING_OR_LEFTING_LAYOUT = 4;
    private final static int INITIAL_PRESENCE_STATUS = -55;
    private final static int RECORD_BUTTON_SEND = 1;
    private final static int RECORD_BUTTON_ACTIVATED = 2;
    private final static int RECORD_BUTTON_DEACTIVATED = 3;

    private final static int PADDING_BUBBLE = 25;
    private final static int CORNER_RADIUS_BUBBLE = 30;
    private final static int MARGIN_BUTTON_DEACTIVATED = 20;
    private final static int MARGIN_BUTTON_ACTIVATED = 24;
    private final static int DURATION_BUBBLE = 4000;
    private int MIN_FIRST_AMPLITUDE = 2;
    private int MIN_SECOND_AMPLITUDE;
    private int MIN_THIRD_AMPLITUDE;
    private int MIN_FOURTH_AMPLITUDE;
    private int MIN_FIFTH_AMPLITUDE;
    private int MIN_SIXTH_AMPLITUDE;
    private final static int NOT_SOUND = 0;
    private final static int FIRST_RANGE = 1;
    private final static int SECOND_RANGE = 2;
    private final static int THIRD_RANGE = 3;
    private final static int FOURTH_RANGE = 4;
    private final static int FIFTH_RANGE = 5;
    private final static int SIXTH_RANGE = 6;

    @Inject
    GetThemeMode getThemeMode;

    @Inject
    FilePrepareUseCase filePrepareUseCase;
    @Inject
    PasscodeManagement passcodeManagement;
    @Inject
    GetAvatarUseCase getAvatarUseCase;
    @Inject
    LegacyGetPublicLinkInformationUseCase legacyGetPublicLinkInformationUseCase;
    @Inject
    GetPublicNodeUseCase getPublicNodeUseCase;
    @Inject
    GetChatChangesUseCase getChatChangesUseCase;
    @Inject
    EndCallUseCase endCallUseCase;
    @Inject
    GetCallStatusChangesUseCase getCallStatusChangesUseCase;
    @Inject
    GetNodeUseCase getNodeUseCase;
    @Inject
    CheckNameCollisionUseCase checkNameCollisionUseCase;
    @Inject
    LegacyCopyNodeUseCase legacyCopyNodeUseCase;
    @Inject
    GetParticipantsChangesUseCase getParticipantsChangesUseCase;
    @Inject
    GetCallUseCase getCallUseCase;
    @Inject
    GetPushToken getPushToken;
    @Inject
    ActivityLifecycleHandler activityLifecycleHandler;
    @Inject
    MegaChatRequestHandler chatRequestHandler;
    @Inject
    RTCAudioManagerGateway rtcAudioManagerGateway;
    @Inject
    CopyRequestMessageMapper copyRequestMessageMapper;

    @Inject
    NotificationManagerCompat notificationManager;

    @Inject
    HasMediaPermissionUseCase hasMediaPermissionUseCase;

    private ChatViewModel viewModel;

    private WaitingRoomManagementViewModel waitingRoomManagementViewModel;

    private int currentRecordButtonState;
    private String mOutputFilePath;
    private boolean getMoreHistory;
    private boolean isLoadingHistory;
    private AlertDialog errorOpenChatDialog;
    private long numberToLoad;
    private ArrayList<Integer> recoveredSelectedPositions = null;

    private AlertDialog chatAlertDialog;
    private AlertDialog errorReactionsDialog;
    private boolean isInputTextExpanded;
    private boolean errorReactionsDialogIsShown;
    private long typeErrorReaction = REACTION_ERROR_DEFAULT_VALUE;
    private AlertDialog dialogCall;
    private AlertDialog dialogOnlyMeInCall;
    private AlertDialog endCallForAllDialog;

    private RelativeLayout editMsgLayout;
    private RelativeLayout cancelEdit;
    private EmojiTextView editMsgText;

    AlertDialog dialog;
    AlertDialog statusDialog;
    private UsersInWaitingRoomDialogFragment usersInWaitingRoomDialogFragment;

    boolean retryHistory = false;
    boolean isStartAndRecordVoiceClip = false;

    private long lastIdMsgSeen = MEGACHAT_INVALID_HANDLE;
    private long generalUnreadCount;
    private boolean lastSeenReceived;
    private int positionToScroll = INVALID_VALUE;
    private int positionNewMessagesLayout = INVALID_VALUE;

    Handler handlerReceive;
    Handler handlerSend;
    Handler handlerKeyboard;
    Handler handlerEmojiKeyboard;

    private TextView emptyTextView;
    private ImageView emptyImageView;
    private RelativeLayout emptyLayout;

    boolean pendingMessagesLoaded = false;

    public boolean activityVisible = false;
    boolean setAsRead = false;

    boolean isOpeningChat = true;
    public int selectedPosition = INVALID_POSITION;
    public long selectedMessageId = -1;
    MegaChatRoom chatRoom;

    public long idChat = MEGACHAT_INVALID_HANDLE;

    boolean noMoreNoSentMessages = false;

    public int showRichLinkWarning = RICH_WARNING_TRUE;

    private final MegaAttacher nodeAttacher = new MegaAttacher(this);
    private final NodeSaver nodeSaver = new NodeSaver(this, this, this,
            AlertsAndWarnings.showSaveToDeviceConfirmDialog(this));

    ChatController chatC;
    boolean scrollingUp = false;

    long myUserHandle;

    ActionBar aB;
    Toolbar tB;
    LinearLayout toolbarElementsInside;

    private EmojiTextView titleToolbar;
    private MarqueeTextView individualSubtitleToobar;
    private EmojiTextView groupalSubtitleToolbar;
    private ImageView iconStateToolbar;
    private ImageView muteIconToolbar;
    private LinearLayout subtitleCall;
    private TextView subtitleChronoCall;
    private LinearLayout participantsLayout;
    private ImageView privateIconToolbar;

    private boolean editingMessage = false;
    private MegaChatMessage messageToEdit = null;

    private CoordinatorLayout fragmentContainer;
    private LinearLayout writingContainerLayout;
    private ConstraintLayout inputTextContainer;

    private RelativeLayout joinChatLinkLayout;
    private Button joinButton;

    private RelativeLayout chatRelativeLayout;
    private RelativeLayout userTypingLayout;
    private TextView userTypingText;
    private RelativeLayout joiningLeavingLayout;
    private TextView joiningLeavingText;

    boolean sendIsTyping = true;
    long userTypingTimeStamp = -1;
    private ImageButton keyboardTwemojiButton;

    private EmojiKeyboard emojiKeyboard;
    private RelativeLayout rLKeyboardTwemojiButton;

    private RelativeLayout returnCallOnHoldButton;
    private ImageView returnCallOnHoldButtonIcon;
    private TextView returnCallOnHoldButtonText;

    private RelativeLayout callInProgressLayout;
    private long chatIdBanner;
    private TextView callInProgressText;
    private TextView startOrJoinMeetingBanner;
    private Chronometer callInProgressChrono;
    private boolean startVideo = false;
    private boolean shouldCallRing = true;
    private RelativeLayout chatRoomOptions;
    private EmojiEditText textChat;
    private ImageButton sendIcon;
    private RelativeLayout expandCollapseInputTextLayout;
    private ConstraintLayout writeMsgLayout;
    private ConstraintLayout inputTextLayout;
    private ConstraintLayout writeMsgAndExpandLayout;
    private LinearLayout editMsgLinearLayout;

    private ImageButton expandCollapseInputTextIcon;

    RelativeLayout messagesContainerLayout;

    RelativeLayout observersLayout;
    TextView observersNumberText;

    RecyclerView listView;
    NpaLinearLayoutManager mLayoutManager;

    ChatActivity chatActivity;

    private MenuItem importIcon;
    private MenuItem callMenuItem;
    private MenuItem videoMenuItem;
    private MenuItem selectMenuItem;
    private MenuItem inviteMenuItem;
    private MenuItem clearHistoryMenuItem;
    private MenuItem contactInfoMenuItem;
    private MenuItem leaveMenuItem;
    private MenuItem endCallForAllMenuItem;
    private MenuItem archiveMenuItem;
    private MenuItem muteMenuItem;
    private MenuItem unMuteMenuItem;
    private MenuItem editIcon;
    private MenuItem copyIcon;
    private MenuItem deleteIcon;
    private MenuItem forwardIcon;
    private MenuItem downloadIcon;
    private MenuItem shareIcon;
    private MenuItem inviteIcon;
    private MenuItem startConversationIcon;

    private boolean showDelete = true;
    private boolean showCopy = true;
    private boolean showForward = true;
    private boolean allNodeAttachments = true;
    private boolean allNodeImages = true;
    private boolean allNodeNonContacts = true;

    String intentAction;
    MegaChatAdapter adapter;
    int stateHistory;

    private ArrayList<AndroidMegaChatMessage> messages = new ArrayList<>();
    private ArrayList<AndroidMegaChatMessage> bufferMessages = new ArrayList<>();
    private ArrayList<AndroidMegaChatMessage> bufferSending = new ArrayList<>();
    private ArrayList<MessageVoiceClip> messagesPlaying = new ArrayList<>();
    private ArrayList<RemovedMessage> removedMessages = new ArrayList<>();

    /**
     * Temporarily stores the voice clip message being played during a screen rotation
     * so that it continues to play after the rotation is over.
     */
    private MessageVoiceClip currentMessagePlaying = null;

    private ConstraintLayout unreadMsgsLayout;
    private RelativeLayout unreadBadgeLayout;
    private ImageView unreadBadgeImage;
    private TextView unreadBadgeText;
    private ArrayList<Long> msgsReceived = new ArrayList<>();

    boolean isTurn = false;
    Handler handler;

    private AlertDialog locationDialog;
    private boolean isLocationDialogShown = false;
    private boolean isJoinCallDialogShown = false;
    private boolean isOnlyMeInCallDialogShown = false;
    private boolean isEndCallForAllDialogShown = false;


    /*Voice clips*/
    private String outputFileVoiceNotes = null;
    private String outputFileName = "";
    private RelativeLayout recordLayout;
    private RelativeLayout recordButtonLayout;
    private RecordButton recordButton;
    private MediaRecorder myAudioRecorder = null;
    private LinearLayout bubbleLayout;
    private RecordView recordView;
    private FrameLayout fragmentVoiceClip;
    private boolean isShareLinkDialogDismissed = false;
    private RelativeLayout recordingLayout;
    private TextView recordingChrono;
    private RelativeLayout firstBar, secondBar, thirdBar, fourthBar, fifthBar, sixthBar;
    private int currentAmplitude = -1;
    private Handler handlerVisualizer = new Handler();

    private ActionMode actionMode;

    private AppRTCAudioManager rtcAudioManager = null;
    private boolean speakerWasActivated = true;

    // Data being stored when My Chat Files folder does not exist
    private ArrayList<AndroidMegaChatMessage> preservedMessagesSelected;
    private ArrayList<MegaChatMessage> preservedMsgSelected;
    private ArrayList<MegaChatMessage> preservedMsgToImport;
    private boolean isForwardingFromNC;

    private ArrayList<Intent> preservedIntents = new ArrayList<>();
    private boolean isWaitingForMoreFiles;
    private boolean isAskingForMyChatFiles;
    // The flag to indicate whether forwarding message is on going
    private boolean isForwardingMessage = false;
    private int typeImport = IMPORT_ONLY_OPTION;
    private ExportListener exportListener;
    private BottomSheetDialogFragment bottomSheetDialogFragment;

    private MegaNode myChatFilesFolder;
    private TextUtils.TruncateAt typeEllipsize = TextUtils.TruncateAt.END;

    private boolean openingAndJoining;

    private AudioFocusRequest request;
    private AudioManager mAudioManager;
    private AudioFocusListener audioFocusListener;

    private int lastVisibleItemPosition = INVALID_POSITION;

    private ActivityResultLauncher<Intent> sendGifLauncher = null;
    private ActivityResultLauncher<Intent> sendContactLauncher = null;
    private ActivityResultLauncher<Intent> scanDocumentLauncher = null;
    private ActivityResultLauncher<Intent> takePictureLauncher = null;

    private ActivityResultLauncher<String[]> permissionsRequest = null;

    /**
     * Current contact online status.
     */
    private int contactOnlineStatus;

    private CompositeDisposable internalComposite = new CompositeDisposable();

    @Override
    public void storedUnhandledData(ArrayList<AndroidMegaChatMessage> preservedData) {
        this.preservedMessagesSelected = preservedData;
    }

    @Override
    public void handleStoredData() {
        if (preservedMessagesSelected != null && !preservedMessagesSelected.isEmpty()) {
            forwardMessages(preservedMessagesSelected);
            preservedMessagesSelected = null;
        } else if (preservedMsgSelected != null && !preservedMsgSelected.isEmpty()) {
            chatC.proceedWithForwardOrShare(this, myChatFilesFolder, preservedMsgSelected,
                    preservedMsgToImport, idChat, typeImport);
            isForwardingFromNC = false;
            preservedMsgSelected = null;
            preservedMsgToImport = null;
        }
    }

    @Override
    public void storedUnhandledData(ArrayList<MegaChatMessage> messagesSelected, ArrayList<MegaChatMessage> messagesToImport) {
        isForwardingFromNC = true;
        preservedMsgSelected = messagesSelected;
        preservedMsgToImport = messagesToImport;
        preservedMessagesSelected = null;
    }

    public void setExportListener(ExportListener exportListener) {
        this.exportListener = exportListener;
    }

    private final Observer<MegaChatCall> callStatusObserver = call -> {
        if (call.getChatid() != getCurrentChatid()) {
            Timber.d("Different chat");
            updateCallBanner();
            return;
        }

        switch (call.getStatus()) {
            case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
            case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:
            case MegaChatCall.CALL_STATUS_IN_PROGRESS:
            case MegaChatCall.CALL_STATUS_DESTROYED:

                updateCallBanner();
                if (call.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                    cancelRecording();
                } else if (call.getStatus() == MegaChatCall.CALL_STATUS_DESTROYED) {
                    if (dialogCall != null) {
                        dialogCall.dismiss();
                    }
                }

                if (call.getStatus() == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION &&
                        call.getTermCode() == MegaChatCall.TERM_CODE_TOO_MANY_PARTICIPANTS) {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.call_error_too_many_participants), MEGACHAT_INVALID_HANDLE);
                }
                break;
        }
    };

    private final Observer<MegaChatCall> callCompositionChangeObserver = call -> {
        if (call.getChatid() != getCurrentChatid() && call.getCallCompositionChange() == 0) {
            Timber.d("Different chat or no changes");
            return;
        }

        if (call.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
            updateCallBanner();
        }
    };

    private final Observer<Pair> waitingForOthersBannerObserver = result -> {
        Long chatId = (Long) result.first;

        if (chatId != getCurrentChatid()) {
            Timber.d("Different chat");
            return;
        }

        Boolean onlyMeInTheCall = (Boolean) result.second;
        if (onlyMeInTheCall) {
            showOnlyMeInTheCallDialog();
        } else {
            hideDialogCall();
        }
    };

    private final Observer<MegaChatCall> callOnHoldObserver = call -> updateCallBanner();

    private final Observer<Pair> sessionOnHoldObserver = sessionAndCall -> updateCallBanner();

    /**
     * Method for finding out if the selected message is deleted.
     *
     * @param messageSelected The message selected.
     * @return True if it's removed. False, otherwise.
     */
    public boolean hasMessagesRemovedOrPending(MegaChatMessage messageSelected) {
        return ChatUtil.isMsgRemovedOrHasRejectedOrManualSendingStatus(removedMessages, messageSelected);
    }

    @Override
    public void confirmLeaveChat(long chatId) {
        stopReproductions();
        setJoiningOrLeaving(R.string.leaving_label);
        megaChatApi.leaveChat(chatId, new RemoveFromChatRoomListener(this, this));
    }

    @Override
    public void confirmLeaveChats(@NotNull List<? extends MegaChatListItem> chats) {
        // No option available to leave more than one chat here.
    }

    @Override
    public void leaveChatSuccess() {
        viewModel.setIsJoiningOrLeaving(false, null);
    }

    @Override
    public void onCallHungUp(long callId) {
        Timber.d("The call has been successfully hung up");
        MegaChatCall call = megaChatApi.getChatCall(idChat);
        if (call == null || (call.getStatus() != MegaChatCall.CALL_STATUS_USER_NO_PRESENT ||
                call.getStatus() == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION))
            return;

        answerCall(chatRoom.getChatId(), false, true, false);
    }

    @Override
    public void onCallOnHold(long chatId, boolean isOnHold) {
        if (!isOnHold)
            return;

        MegaChatCall callInThisChat = megaChatApi.getChatCall(idChat);
        if (callInThisChat == null || (callInThisChat.getStatus() != MegaChatCall.CALL_STATUS_USER_NO_PRESENT ||
                callInThisChat.getStatus() == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION))
            return;

        Timber.d("Active call on hold. Answer call.");
        answerCall(idChat, false, true, false);
    }

    @Override
    public void onPreviewLoaded(MegaChatRequest request, boolean alreadyExist) {
        long chatId = request.getChatHandle();
        boolean isFromOpenChatPreview = request.getFlag();
        int type = request.getParamType();
        String link = request.getLink();
        boolean waitingRoom = MegaChatApi.hasChatOptionEnabled(MegaChatApi.CHAT_OPTION_WAITING_ROOM, request.getPrivilege());

        if (type == LINK_IS_FOR_MEETING) {
            Timber.d("It's a meeting link");
            boolean linkInvalid = TextUtil.isTextEmpty(link) && chatId == MEGACHAT_INVALID_HANDLE;
            if (linkInvalid) {
                Timber.e("Invalid link");
                return;
            }

            if (isMeetingEnded(request)) {
                Timber.d("It's a meeting, open dialog: Meeting has ended");
                new MeetingHasEndedDialogFragment(new MeetingHasEndedDialogFragment.ClickCallback() {
                    @Override
                    public void onViewMeetingChat() {
                        Timber.d("Chat link");
                        loadChatLink(link);
                    }

                    @Override
                    public void onLeave() {
                    }
                }, false).show(getSupportFragmentManager(),
                        MeetingHasEndedDialogFragment.TAG);
            } else {
                CallUtil.checkMeetingInProgress(ChatActivity.this, ChatActivity.this, chatId, isFromOpenChatPreview, link, request.getText(), alreadyExist, request.getUserHandle(), passcodeManagement, waitingRoom);
            }
        } else {
            Timber.d("It's a chat link");
            loadChatLink(link);
        }
    }

    @Override
    public void onErrorLoadingPreview(int errorCode) {
    }

    @Override
    public void onPreviewLoaded(@NonNull MegaChatRequest request, int errorCode) {
        if (errorCode == MegaChatError.ERROR_OK || errorCode == MegaChatError.ERROR_EXIST) {
            if (idChat != MEGACHAT_INVALID_HANDLE && megaChatApi.getChatRoom(idChat) != null) {
                Timber.d("Close previous chat");
                megaChatApi.closeChatRoom(idChat, ChatActivity.this);
            }

            idChat = request.getChatHandle();
            viewModel.setChatId(idChat);

            composite.clear();
            checkChatChanges();

            if (idChat != MEGACHAT_INVALID_HANDLE) {
                dbH.setLastPublicHandle(idChat);
                dbH.setLastPublicHandleTimeStamp();
            }

            MegaApplication.setOpenChatId(idChat);

            initAndShowChat();

            if (errorCode == MegaChatError.ERROR_OK && openingAndJoining) {
                if (!isAlreadyJoining(idChat)) {
                    addJoiningChatId(idChat);
                    megaChatApi.autojoinPublicChat(idChat, ChatActivity.this);
                }

                openingAndJoining = false;
            } else if (errorCode == MegaChatError.ERROR_EXIST) {
                if (megaChatApi.getChatRoom(idChat).isActive()) {
                    //I'm already participant
                    viewModel.setIsJoiningOrLeaving(false, null);
                    openingAndJoining = false;
                }
                if (!isAlreadyJoining(idChat) && !isAlreadyJoining(request.getUserHandle())) {
                    addJoiningChatId(idChat);
                    addJoiningChatId(request.getUserHandle());
                    megaChatApi.autorejoinPublicChat(idChat, request.getUserHandle(), this);
                }
            }

            supportInvalidateOptionsMenu();
        } else {
            String text;
            if (errorCode == MegaChatError.ERROR_NOENT) {
                text = getString(R.string.invalid_chat_link);
            } else {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), MEGACHAT_INVALID_HANDLE);
                text = getString(R.string.error_chat_link);
            }

            emptyScreen(text);
        }
    }

    /**
     * Method for answering a call
     *
     * @param chatId  Chat ID
     * @param video   True, video ON. False, video OFF.
     * @param audio   True, audio ON. False, audio OFF.
     * @param speaker True, speaker ON. False, speaker OFF.
     */
    private void answerCall(long chatId, Boolean video, Boolean audio, Boolean speaker) {
        var enableAudio = audio;
        var hasAudioPermissions = hasPermissions(this, Manifest.permission.RECORD_AUDIO);
        if (enableAudio) {
            enableAudio = hasAudioPermissions;
        }

        if (!hasAudioPermissions) {
            openMeetingRinging(
                    this,
                    chatId,
                    passcodeManagement
            );
            return;
        }

        var enableVideo = video;
        if (enableVideo) {
            enableVideo = hasPermissions(this, Manifest.permission.CAMERA);
        }

        callInProgressLayout.setEnabled(false);
        viewModel.onAnswerCall(chatId, enableVideo, enableAudio);
    }

    /**
     * Method for detecting when the keyboard is opened or closed
     */
    public void setKeyboardVisibilityListener() {
        final View parentView = ((ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        if (parentView == null) {
            Timber.w("Cannot set the keyboard visibility listener. Parent view is NULL.");
            return;
        }

        parentView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {

            private boolean alreadyOpen;
            private final int defaultKeyboardHeightDP = 100;
            private final Rect rect = new Rect();

            @Override
            public void onGlobalLayout() {
                int estimatedKeyboardDP = defaultKeyboardHeightDP + 48;
                int estimatedKeyboardHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, estimatedKeyboardDP, parentView.getResources().getDisplayMetrics());
                parentView.getWindowVisibleDisplayFrame(rect);
                int heightDiff = parentView.getRootView().getHeight() - (rect.bottom - rect.top);
                boolean isShown = heightDiff >= estimatedKeyboardHeight;
                if (isShown == alreadyOpen)
                    return;

                alreadyOpen = isShown;

                if (emojiKeyboard != null) {
                    emojiKeyboard.updateStatusLetterKeyboard(alreadyOpen);
                }
            }
        });
    }

    @Override
    public void onScanDocumentOptionClicked() {
        String[] saveDestinations = {
                getString(R.string.section_chat)
        };
        Intent intent = DocumentScannerActivity.getIntent(this, saveDestinations);
        scanDocumentLauncher.launch(intent);
    }

    @Override
    public void onRecordVoiceClipClicked() {
        isStartAndRecordVoiceClip = true;
        recordView.startRecord();
    }

    @Override
    public void onSendFilesSelected(ArrayList<FileGalleryItem> files) {
        filePrepareUseCase.prepareFilesFromGallery(files)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((shareInfo, throwable) -> {
                    if (throwable == null) {
                        onIntentProcessed(shareInfo);
                    }
                });
    }

    @Override
    public void onTakePictureOptionClicked() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, AUTHORITY_STRING_FILE_PROVIDER, photoFile);

                mOutputFilePath = photoFile.getAbsolutePath();
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                takePictureLauncher.launch(intent);
            }
        }
    }

    @Override
    public void onSendFileOptionClicked() {
        hideKeyboard();

        if (isBottomSheetDialogShown(bottomSheetDialogFragment)) {
            bottomSheetDialogFragment.dismiss();
        }

        bottomSheetDialogFragment = new SendAttachmentChatBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    @Override
    public void onStartCallOptionClicked(boolean videoOn) {
        if (recordView.isRecordingNow())
            return;

        startVideo = videoOn;
        shouldCallRing = true;
        startCall();
    }

    @Override
    public void onSendGIFOptionClicked() {
        Intent intent = new Intent(this, GiphyPickerActivity.class);
        sendGifLauncher.launch(intent);
    }

    @Override
    public void onSendContactOptionClicked() {
        ArrayList<MegaUser> contacts = megaApi.getContacts();

        if (contacts == null || contacts.isEmpty()) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.no_contacts_invite), MEGACHAT_INVALID_HANDLE);
            return;
        }

        Intent in = new Intent(this, AddContactActivity.class);
        in.putExtra(INTENT_EXTRA_KEY_CONTACT_TYPE, CONTACT_TYPE_MEGA);
        in.putExtra(INTENT_EXTRA_KEY_CHAT, true);
        in.putExtra(INTENT_EXTRA_KEY_TOOL_BAR_TITLE, getString(R.string.send_contacts));
        sendContactLauncher.launch(in);
    }

    @Override
    public void onSendLocationOptionClicked() {
        if (MegaApplication.isEnabledGeoLocation()) {
            getLocationPermission();
        } else {
            showSendLocationDialog();
        }
    }

    private class UserTyping {
        MegaChatParticipant participantTyping;
        long timeStampTyping;

        public UserTyping(MegaChatParticipant participantTyping) {
            this.participantTyping = participantTyping;
        }

        public MegaChatParticipant getParticipantTyping() {
            return participantTyping;
        }

        public void setParticipantTyping(MegaChatParticipant participantTyping) {
            this.participantTyping = participantTyping;
        }

        public long getTimeStampTyping() {
            return timeStampTyping;
        }

        public void setTimeStampTyping(long timeStampTyping) {
            this.timeStampTyping = timeStampTyping;
        }
    }

    private final BroadcastReceiver historyTruncatedByRetentionTimeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                long msgId = intent.getLongExtra(MESSAGE_ID, MEGACHAT_INVALID_HANDLE);
                if (msgId != MEGACHAT_INVALID_HANDLE) {
                    updateHistoryByRetentionTime(msgId);
                }
            }
        }
    };

    private BroadcastReceiver voiceclipDownloadedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                long nodeHandle = intent.getLongExtra(INTENT_EXTRA_NODE_HANDLE, 0);
                int resultTransfer = intent.getIntExtra(EXTRA_RESULT_TRANSFER, 0);
                if (adapter != null) {
                    adapter.finishedVoiceClipDownload(nodeHandle, resultTransfer);
                }
            }
        }
    };

    private BroadcastReceiver userNameReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null
                    || intent.getLongExtra(EXTRA_USER_HANDLE, INVALID_HANDLE) == INVALID_HANDLE) {
                return;
            }

            if (intent.getAction().equals(ACTION_UPDATE_NICKNAME)
                    || intent.getAction().equals(ACTION_UPDATE_FIRST_NAME)
                    || intent.getAction().equals(ACTION_UPDATE_LAST_NAME)) {
                updateUserNameInChat();
            }
        }
    };

    private BroadcastReceiver closeChatReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;

            if (intent.getAction().equals(ACTION_CLOSE_CHAT_AFTER_IMPORT)
                    || intent.getAction().equals(ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS)) {
                finish();
            }
        }
    };

    private final BroadcastReceiver chatUploadStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !BROADCAST_ACTION_CHAT_TRANSFER_START.equals(intent.getAction())) {
                return;
            }

            long pendingMessageId = intent.getLongExtra(PENDING_MESSAGE_ID, MEGACHAT_INVALID_HANDLE);
            if (pendingMessageId != MEGACHAT_INVALID_HANDLE) {
                PendingMessage pendingMessage = dbH.findPendingMessageById(pendingMessageId);

                if (pendingMessage == null || pendingMessage.getChatId() != idChat) {
                    Timber.e("pendingMessage is null or is not the same chat, cannot update it.");
                    return;
                }

                updatePendingMessage(pendingMessage);
            }
        }
    };

    private final BroadcastReceiver errorCopyingNodesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !BROADCAST_ACTION_ERROR_COPYING_NODES.equals(intent.getAction())) {
                return;
            }

            removeProgressDialog();
            showSnackbar(SNACKBAR_TYPE, intent.getStringExtra(ERROR_MESSAGE_TEXT), MEGACHAT_INVALID_HANDLE);
        }
    };

    private final BroadcastReceiver retryPendingMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !BROADCAST_ACTION_RETRY_PENDING_MESSAGE.equals(intent.getAction())) {
                return;
            }

            long pendingMsgId = intent.getLongExtra(INTENT_EXTRA_PENDING_MESSAGE_ID, MEGACHAT_INVALID_HANDLE);
            long chatId = intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);

            if (pendingMsgId == MEGACHAT_INVALID_HANDLE || chatId != idChat) {
                Timber.w("pendingMsgId is not valid or is not the same chat. Cannot retry");
                return;
            }

            retryPendingMessage(pendingMsgId);
        }
    };

    ArrayList<UserTyping> usersTyping;
    List<UserTyping> usersTypingSync;

    public void openMegaLink(String url, boolean isFile) {
        Timber.d("url: %s, isFile: %s", url, isFile);
        Intent openLink;

        if (isFile) {
            openLink = new Intent(this, FileLinkComposeActivity.class);
            openLink.setAction(ACTION_OPEN_MEGA_LINK);
        } else {
            openLink = new Intent(this, FolderLinkComposeActivity.class);
            openLink.setAction(ACTION_OPEN_MEGA_FOLDER_LINK);
        }

        openLink.putExtra(OPENED_FROM_CHAT, true);
        openLink.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        openLink.setData(Uri.parse(url));
        startActivity(openLink);
    }

    public void showMessageInfo(int positionInAdapter) {
        Timber.d("showMessageInfo");
        int position = positionInAdapter - 1;

        if (position < messages.size()) {
            AndroidMegaChatMessage androidM = messages.get(position);
            StringBuilder messageToShow = new StringBuilder("");
            String token = getPushToken.invoke();
            if (token != null) {
                messageToShow.append("FCM TOKEN: " + token);
            }
            messageToShow.append("\nCHAT ID: " + MegaApiJava.userHandleToBase64(idChat));
            messageToShow.append("\nMY USER HANDLE: " + MegaApiJava.userHandleToBase64(megaChatApi.getMyUserHandle()));
            if (androidM != null) {
                MegaChatMessage m = androidM.getMessage();
                if (m != null) {
                    messageToShow.append("\nMESSAGE TYPE: " + m.getType());
                    messageToShow.append("\nMESSAGE TIMESTAMP: " + m.getTimestamp());
                    messageToShow.append("\nMESSAGE USERHANDLE: " + MegaApiJava.userHandleToBase64(m.getUserHandle()));
                    messageToShow.append("\nMESSAGE ID: " + MegaApiJava.userHandleToBase64(m.getMsgId()));
                    messageToShow.append("\nMESSAGE TEMP ID: " + MegaApiJava.userHandleToBase64(m.getTempId()));
                }
            }
            Toast.makeText(this, messageToShow, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Opens Group info if a group chat conversation or a contact info if a 1to1 conversation.
     */
    public void showGroupOrContactInfoActivity() {
        Timber.d("showGroupInfoActivity");
        if (chatRoom == null)
            return;

        ChatScheduledMeeting schedMeet = viewModel.getMeeting();
        if (chatRoom.isMeeting() && schedMeet != null && viewModel.isPendingMeeting() && chatRoom.isActive()) {
            Intent i = new Intent(this, ScheduledMeetingInfoActivity.class);
            i.putExtra(Constants.CHAT_ID, schedMeet.getChatId());
            i.putExtra(Constants.SCHEDULED_MEETING_ID, schedMeet.getSchedId());
            startActivity(i);
        } else {
            Intent i = new Intent(this,
                    chatRoom.isGroup() ? GroupChatInfoActivity.class : ContactInfoActivity.class);
            i.putExtra(HANDLE, chatRoom.getChatId());
            i.putExtra(ACTION_CHAT_OPEN, true);
            startActivity(i);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        waitingRoomManagementViewModel = new ViewModelProvider(this).get(WaitingRoomManagementViewModel.class);

        if (shouldRefreshSessionDueToKarere()) {
            return;
        }

        if (viewModel.isConnected() && megaChatApi.getInitState() != INIT_ANONYMOUS) {
            shouldRefreshSessionDueToSDK();
        }

        handler = new Handler();

        chatActivity = this;
        chatC = new ChatController(chatActivity);

        collectFlows();

        registerReceiver(historyTruncatedByRetentionTimeReceiver, new IntentFilter(BROADCAST_ACTION_UPDATE_HISTORY_BY_RT));
        registerReceiver(voiceclipDownloadedReceiver, new IntentFilter(BROADCAST_ACTION_INTENT_VOICE_CLIP_DOWNLOADED));

        IntentFilter contactUpdateFilter = new IntentFilter(BROADCAST_ACTION_INTENT_FILTER_CONTACT_UPDATE);
        contactUpdateFilter.addAction(ACTION_UPDATE_NICKNAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_FIRST_NAME);
        contactUpdateFilter.addAction(ACTION_UPDATE_LAST_NAME);
        registerReceiver(userNameReceiver, contactUpdateFilter);

        LiveEventBus.get(EVENT_CALL_STATUS_CHANGE, MegaChatCall.class).observe(this, callStatusObserver);
        LiveEventBus.get(EVENT_CALL_COMPOSITION_CHANGE, MegaChatCall.class).observe(this, callCompositionChangeObserver);
        LiveEventBus.get(EVENT_UPDATE_WAITING_FOR_OTHERS, Pair.class).observe(this, waitingForOthersBannerObserver);

        internalComposite.add(getParticipantsChangesUseCase.checkIfIAmAloneOnAnyCall()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    long chatId = result.component1();
                    if (chatRoom != null && chatId == chatRoom.getChatId()) {
                        boolean onlyMeInTheCall = result.component2();
                        boolean waitingForOthers = result.component3();
                        long millisecondsOnlyMeInCallDialog =
                                TimeUnit.MILLISECONDS.toSeconds(MegaApplication.getChatManagement().getMillisecondsOnlyMeInCallDialog());

                        boolean hideDialogCall = MegaApplication.getChatManagement().getHasEndCallDialogBeenIgnored() || !onlyMeInTheCall || (waitingForOthers && millisecondsOnlyMeInCallDialog <= 0);

                        if (hideDialogCall) {
                            hideDialogCall();
                        } else {
                            showOnlyMeInTheCallDialog();
                        }
                    }

                }, Timber::e));

        LiveEventBus.get(EVENT_CALL_ON_HOLD_CHANGE, MegaChatCall.class).observe(this, callOnHoldObserver);
        LiveEventBus.get(EVENT_SESSION_ON_HOLD_CHANGE, Pair.class).observe(this, sessionOnHoldObserver);

        internalComposite.add(getCallStatusChangesUseCase.callCannotBeRecovered()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result) -> {
                    if (result) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.calls_chat_screen_unable_to_reconnect_the_call), MEGACHAT_INVALID_HANDLE);
                    }
                }));

        IntentFilter closeChatFilter = new IntentFilter(ACTION_CLOSE_CHAT_AFTER_IMPORT);
        closeChatFilter.addAction(ACTION_CLOSE_CHAT_AFTER_OPEN_TRANSFERS);
        registerReceiver(closeChatReceiver, closeChatFilter);

        registerReceiver(chatUploadStartedReceiver,
                new IntentFilter(BROADCAST_ACTION_CHAT_TRANSFER_START));

        registerReceiver(errorCopyingNodesReceiver,
                new IntentFilter(BROADCAST_ACTION_ERROR_COPYING_NODES));

        registerReceiver(retryPendingMessageReceiver,
                new IntentFilter(BROADCAST_ACTION_RETRY_PENDING_MESSAGE));

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        permissionsRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    if (checkMandatoryCallPermissions(chatActivity)) {
                        enableCallMenuItems(false);
                        viewModel.onCallTap(startVideo, shouldCallRing);
                    } else {
                        showSnackbar(Constants.NOT_CALL_PERMISSIONS_SNACKBAR_TYPE,
                                fragmentContainer,
                                getString(R.string.allow_acces_calls_subtitle_microphone),
                                MEGACHAT_INVALID_HANDLE
                        );
                    }
                });

        sendGifLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result != null && result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            sendGiphyMessageFromGifData(data.getParcelableExtra(GIF_DATA));
                        }
                    }
                });

        sendContactLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result != null && result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            final ArrayList<String> contactsData = data.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS);
                            if (contactsData != null) {
                                for (int i = 0; i < contactsData.size(); i++) {
                                    MegaUser user = megaApi.getContact(contactsData.get(i));
                                    if (user != null) {
                                        MegaHandleList handleList = MegaHandleList.createInstance();
                                        handleList.addMegaHandle(user.getHandle());
                                        retryContactAttachment(handleList);
                                    }
                                }
                            }
                        }
                    }
                });

        scanDocumentLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result != null && result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            Intent fileIntent = new Intent();
                            fileIntent.setAction(FileExplorerActivity.ACTION_UPLOAD_TO_CHAT);
                            fileIntent.putExtra(Intent.EXTRA_STREAM, data.getData());
                            fileIntent.setType(data.getType());

                            internalComposite.add(filePrepareUseCase.prepareFiles(fileIntent)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe((shareInfo, throwable) -> {
                                        if (throwable == null) {
                                            onIntentProcessed(shareInfo);
                                        }
                                    }));
                        }
                    }
                });

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result != null && result.getResultCode() == Activity.RESULT_OK) {
                        onCaptureImageResult();
                    }
                });

        setContentView(R.layout.activity_chat);
        //Set toolbar
        tB = findViewById(R.id.toolbar_chat);

        setSupportActionBar(tB);
        aB = getSupportActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setDisplayShowHomeEnabled(true);
        aB.setTitle(null);
        aB.setSubtitle(null);
        tB.setOnClickListener(this);

        int range = 32000 / 6;
        MIN_SECOND_AMPLITUDE = range;
        MIN_THIRD_AMPLITUDE = range * SECOND_RANGE;
        MIN_FOURTH_AMPLITUDE = range * THIRD_RANGE;
        MIN_FIFTH_AMPLITUDE = range * FOURTH_RANGE;
        MIN_SIXTH_AMPLITUDE = range * FIFTH_RANGE;

        toolbarElementsInside = tB.findViewById(R.id.toolbar_elements_inside);
        titleToolbar = tB.findViewById(R.id.title_toolbar);
        iconStateToolbar = tB.findViewById(R.id.state_icon_toolbar);
        privateIconToolbar = tB.findViewById(R.id.private_icon_toolbar);
        muteIconToolbar = tB.findViewById(R.id.mute_icon_toolbar);

        individualSubtitleToobar = tB.findViewById(R.id.individual_subtitle_toolbar);
        groupalSubtitleToolbar = tB.findViewById(R.id.groupal_subtitle_toolbar);

        subtitleCall = tB.findViewById(R.id.subtitle_call);
        subtitleChronoCall = tB.findViewById(R.id.chrono_call);
        participantsLayout = tB.findViewById(R.id.ll_participants);

        chatRoomOptions = findViewById(R.id.more_options_rl);
        chatRoomOptions.setOnClickListener(this);

        textChat = findViewById(R.id.input_text_chat);
        textChat.setVisibility(View.VISIBLE);
        textChat.setEnabled(true);

        expandCollapseInputTextLayout = findViewById(R.id.expand_input_text_rl);
        expandCollapseInputTextIcon = findViewById(R.id.expand_input_text_icon);
        writeMsgLayout = findViewById(R.id.write_msg_rl);
        editMsgLinearLayout = findViewById(R.id.edit_msg_rl);

        editMsgLayout = findViewById(R.id.edit_msg_layout);
        editMsgText = findViewById(R.id.edit_msg_text);
        cancelEdit = findViewById(R.id.cancel_edit);
        cancelEdit.setOnClickListener(this);
        hideEditMsgLayout();

        expandCollapseInputTextLayout.setOnClickListener(this);
        expandCollapseInputTextIcon.setOnClickListener(this);
        expandCollapseInputTextLayout.setVisibility(View.GONE);

        emptyLayout = findViewById(R.id.empty_messages_layout);
        emptyTextView = findViewById(R.id.empty_text_chat_recent);
        emptyImageView = findViewById(R.id.empty_image_view_chat);

        chatRelativeLayout = findViewById(R.id.relative_chat_layout);

        initEmptyScreen(null);

        fragmentContainer = findViewById(R.id.fragment_container_chat);
        writingContainerLayout = findViewById(R.id.writing_container_layout_chat_layout);

        inputTextContainer = findViewById(R.id.input_text_container);
        inputTextLayout = findViewById(R.id.input_text_layout);
        writeMsgAndExpandLayout = findViewById(R.id.write_msg_and_expand_rl);


        titleToolbar.setText("");
        individualSubtitleToobar.setText("");
        individualSubtitleToobar.setVisibility(View.GONE);
        groupalSubtitleToolbar.setText("");
        setGroupalSubtitleToolbarVisibility(false);
        subtitleCall.setVisibility(View.GONE);
        subtitleChronoCall.setVisibility(View.GONE);
        participantsLayout.setVisibility(View.GONE);
        iconStateToolbar.setVisibility(View.GONE);
        privateIconToolbar.setVisibility(View.GONE);

        muteIconToolbar.setVisibility(View.GONE);

        Drawable upArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_arrow_back_white)
                .mutate();
        upArrow.setColorFilter(getResources().getColor(R.color.grey_087_white_087),
                PorterDuff.Mode.SRC_IN);
        aB.setHomeAsUpIndicator(upArrow);

        joinChatLinkLayout = findViewById(R.id.join_chat_layout_chat_layout);
        joinButton = findViewById(R.id.join_button);
        setOnSingleClickListener(joinButton, this);

        joiningLeavingLayout = findViewById(R.id.joining_leaving_layout_chat_layout);
        joiningLeavingText = findViewById(R.id.joining_leaving_text_chat_layout);

        unreadMsgsLayout = findViewById(R.id.new_messages_icon);
        unreadMsgsLayout.setVisibility(View.GONE);
        unreadBadgeLayout = findViewById(R.id.badge_rl);
        unreadBadgeText = findViewById(R.id.badge_text);
        unreadBadgeImage = findViewById(R.id.badge_image);

        rLKeyboardTwemojiButton = findViewById(R.id.emoji_rl);

        keyboardTwemojiButton = findViewById(R.id.emoji_icon);

        checkExpandOrCollapseInputText();

        callInProgressLayout = findViewById(R.id.call_in_progress_layout);
        callInProgressLayout.setVisibility(View.GONE);
        callInProgressText = findViewById(R.id.call_in_progress_text);
        callInProgressChrono = findViewById(R.id.call_in_progress_chrono);
        callInProgressChrono.setVisibility(View.GONE);

        returnCallOnHoldButton = findViewById(R.id.call_on_hold_layout);
        returnCallOnHoldButtonIcon = findViewById(R.id.call_on_hold_icon);
        returnCallOnHoldButtonText = findViewById(R.id.call_on_hold_text);

        returnCallOnHoldButton.setOnClickListener(this);
        returnCallOnHoldButton.setVisibility(View.GONE);

        startOrJoinMeetingBanner = findViewById(R.id.start_or_join_meeting_banner);
        startOrJoinMeetingBanner.setVisibility(View.GONE);
        setOnSingleClickListener(startOrJoinMeetingBanner, this);

        /*Recording views*/
        recordingLayout = findViewById(R.id.recording_layout);
        recordingChrono = findViewById(R.id.recording_time);
        recordingChrono.setText(new SimpleDateFormat("mm:ss").format(0));
        firstBar = findViewById(R.id.first_bar);
        secondBar = findViewById(R.id.second_bar);
        thirdBar = findViewById(R.id.third_bar);
        fourthBar = findViewById(R.id.fourth_bar);
        fifthBar = findViewById(R.id.fifth_bar);
        sixthBar = findViewById(R.id.sixth_bar);

        initRecordingItems(IS_LOW);
        recordingLayout.setVisibility(View.GONE);

        enableButton(rLKeyboardTwemojiButton, keyboardTwemojiButton);

        unreadMsgsLayout.setOnClickListener(this);

        sendIcon = findViewById(R.id.send_icon);
        sendIcon.setOnClickListener(this);
        sendIcon.setEnabled(true);

        //Voice clip elements
        fragmentVoiceClip = findViewById(R.id.fragment_voice_clip);
        recordLayout = findViewById(R.id.layout_button_layout);
        recordButtonLayout = findViewById(R.id.record_button_layout);
        recordButton = findViewById(R.id.record_button);
        recordButton.setEnabled(true);
        recordButton.setHapticFeedbackEnabled(true);
        recordView = findViewById(R.id.record_view);
        recordView.setVisibility(View.GONE);
        bubbleLayout = findViewById(R.id.bubble_layout);
        BubbleDrawable myBubble = new BubbleDrawable(BubbleDrawable.CENTER, ContextCompat.getColor(this, R.color.grey_800_white));
        myBubble.setCornerRadius(CORNER_RADIUS_BUBBLE);
        myBubble.setPointerAlignment(BubbleDrawable.RIGHT);
        myBubble.setPointerWidth(dp2px(8, getOutMetrics()));
        myBubble.setPointerHeight(dp2px(5, getOutMetrics()));
        myBubble.setPointerMarginEnd(dp2px(15, getOutMetrics()));
        myBubble.setPadding(PADDING_BUBBLE, PADDING_BUBBLE, PADDING_BUBBLE, PADDING_BUBBLE);
        bubbleLayout.setBackground(myBubble);
        bubbleLayout.setVisibility(View.GONE);
        recordButton.setRecordView(recordView);
        myAudioRecorder = new MediaRecorder();

        //Input text:
        handlerKeyboard = new Handler();
        handlerEmojiKeyboard = new Handler();

        emojiKeyboard = findViewById(R.id.emojiView);
        emojiKeyboard.initEmoji(this, textChat, keyboardTwemojiButton);
        emojiKeyboard.setListenerActivated(true);
        observersLayout = findViewById(R.id.observers_layout);
        observersNumberText = findViewById(R.id.observers_text);

        textChat.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s != null && !s.toString().trim().isEmpty()) {
                    showSendIcon();
                    controlExpandableInputText(textChat.getLineCount());
                } else {
                    refreshTextInput();
                }

                if (getCurrentFocus() == textChat) {
                    // is only executed if the EditText was directly changed by the user
                    if (sendIsTyping) {
                        sendIsTyping = false;
                        megaChatApi.sendTypingNotification(chatRoom.getChatId());

                        int interval = 4000;
                        Runnable runnable = new Runnable() {
                            public void run() {
                                sendIsTyping = true;
                            }
                        };
                        handlerSend = new Handler();
                        handlerSend.postDelayed(runnable, interval);
                    }

                    megaChatApi.signalPresenceActivity();
                } else {
                    if (chatRoom != null) {
                        megaChatApi.sendStopTypingNotification(chatRoom.getChatId());
                    }
                }
            }
        });

        textChat.setOnLongClickListener(v -> {
            showLetterKB();
            return false;
        });

        textChat.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                showLetterKB();
            }
            return false;
        });

        textChat.setMediaListener(path -> uploadPictureOrVoiceClip(path));

        /*
         *If the recording button (an arrow) is clicked, the recording will be sent to the chat
         */
        recordButton.setOnRecordClickListener(v -> {
            recordButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
            sendRecording();
        });

        /*
         *Events of the recording
         */
        recordView.setOnRecordListener(new OnRecordListener() {
            @Override
            public void onStart() {
                if (participatingInACall()) {
                    showSnackbar(SNACKBAR_TYPE, getApplicationContext().getString(R.string.not_allowed_recording_voice_clip), -1);
                    return;
                }

                if (!isAllowedToRecord())
                    return;

                audioFocusListener = new AudioFocusListener(chatActivity);
                request = getRequest(audioFocusListener, AUDIOFOCUS_DEFAULT);
                if (getAudioFocus(mAudioManager, audioFocusListener, request, AUDIOFOCUS_DEFAULT, STREAM_MUSIC_DEFAULT)) {
                    prepareRecording();
                }
            }

            @Override
            public void onLessThanSecond() {
                if (!isAllowedToRecord()) return;
                showBubble();
            }

            @Override
            public void onCancel() {
                recordButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                cancelRecording();
            }

            @Override
            public void onLock() {
                recordButtonStates(RECORD_BUTTON_SEND);
            }

            @Override
            public void onFinish(long recordTime) {
                recordButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                sendRecording();
            }

            @Override
            public void finishedSound() {
                if (!isAllowedToRecord()) return;
                startRecording();
            }

            @Override
            public void changeTimer(CharSequence time) {
                if (recordingLayout != null && recordingChrono != null && recordingLayout.getVisibility() == View.VISIBLE) {
                    recordingChrono.setText(time);
                }
            }
        });

        recordView.setOnBasketAnimationEndListener(new OnBasketAnimationEnd() {
            @Override
            public void onAnimationEnd() {
                recordButton.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                cancelRecording();
            }

            @Override
            public void deactivateRecordButton() {
                hideChatOptions();
                recordView.setVisibility(View.VISIBLE);
                recordLayout.setVisibility(View.VISIBLE);
                recordButtonLayout.setVisibility(View.VISIBLE);
                recordButton.activateOnTouchListener(false);
                recordButtonDeactivated(true);
                placeRecordButton(RECORD_BUTTON_DEACTIVATED);
            }
        });


        emojiKeyboard.setOnPlaceButtonListener(() -> {
            if (sendIcon.getVisibility() != View.VISIBLE) {
                recordLayout.setVisibility(View.VISIBLE);
                recordButtonLayout.setVisibility(View.VISIBLE);
            }
            recordView.setVisibility(View.INVISIBLE);
            recordButton.activateOnTouchListener(true);
            placeRecordButton(RECORD_BUTTON_DEACTIVATED);
        });

        listView = findViewById(R.id.messages_chat_list_view);
        listView.setClipToPadding(false);

        listView.setNestedScrollingEnabled(false);
        ((SimpleItemAnimator) listView.getItemAnimator()).setSupportsChangeAnimations(false);

        mLayoutManager = new NpaLinearLayoutManager(this);
        mLayoutManager.setStackFromEnd(true);
        listView.setLayoutManager(mLayoutManager);

        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                checkScroll();
                int currentLastVisibleItemPosition = mLayoutManager.findLastVisibleItemPosition();

                if (lastVisibleItemPosition == INVALID_POSITION) {
                    lastVisibleItemPosition = currentLastVisibleItemPosition;
                }

                if (!messages.isEmpty() && currentLastVisibleItemPosition < messages.size() - 1) {
                    showScrollToLastMsgButton();
                } else {
                    hideScrollToLastMsgButton();
                }

                if (stateHistory != MegaChatApi.SOURCE_NONE) {
                    scrollingUp = dy > 0;

                    if (!scrollingUp && mLayoutManager.findFirstVisibleItemPosition() <= NUMBER_MESSAGES_BEFORE_LOAD && getMoreHistory) {
                        askForMoreMessages();
                        positionToScroll = INVALID_VALUE;
                    }
                }
            }
        });

        messagesContainerLayout = findViewById(R.id.message_container_chat_layout);

        userTypingLayout = findViewById(R.id.user_typing_layout);
        userTypingLayout.setVisibility(View.GONE);
        userTypingText = findViewById(R.id.user_typing_text);

        initAfterIntent(getIntent(), savedInstanceState);

        Timber.d("FINISH on Create");
    }

    /**
     * Show waiting room dialog
     */
    private void showWaitingRoomDialog() {
        if (usersInWaitingRoomDialogFragment != null) {
            usersInWaitingRoomDialogFragment.dismissAllowingStateLoss();
        }

        usersInWaitingRoomDialogFragment = UsersInWaitingRoomDialogFragment.Companion.newInstance();
        usersInWaitingRoomDialogFragment.show(getSupportFragmentManager(), usersInWaitingRoomDialogFragment.getTag());
    }

    private boolean isAllowedToRecord() {
        Timber.d("isAllowedToRecord ");
        if (participatingInACall()) return false;
        return checkPermissionsVoiceClip();
    }

    private void showLetterKB() {
        if (emojiKeyboard == null || emojiKeyboard.getLetterKeyboardShown()) {
            return;
        }

        emojiKeyboard.showLetterKeyboard();
    }

    /**
     * Collecting Flows from ViewModels
     */
    private void collectFlows() {
        ViewExtensionsKt.collectFlow(this, viewModel.getState(), Lifecycle.State.STARTED, chatState -> {

            if (chatState.getOpenWaitingRoomScreen()) {
                viewModel.setOpenWaitingRoomConsumed();
                Intent intentWaitingRoom = new Intent(chatActivity, WaitingRoomActivity.class);
                intentWaitingRoom.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentWaitingRoom.putExtra(WaitingRoomActivity.EXTRA_CHAT_ID, chatState.getChatId());
                startActivity(intentWaitingRoom);
            }


            if (chatState.getError() != null) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.call_error), MEGACHAT_INVALID_HANDLE);
            } else if (chatState.isCallAnswered()) {
                callInProgressLayout.setEnabled(true);
            }

            if (chatState.isPushNotificationSettingsUpdatedEvent()) {
                if (chatRoom == null) {
                    chatRoom = megaChatApi.getChatRoom(idChat);
                }
                if (chatRoom != null) {
                    muteIconToolbar.setVisibility(isEnableChatNotifications(chatRoom.getChatId()) ? View.GONE : View.VISIBLE);
                }
                viewModel.onConsumePushNotificationSettingsUpdateEvent();
            }

            ChatScheduledMeeting schedMeet = chatState.getScheduledMeeting();
            if (schedMeet != null) {
                adapter.notifyItemChanged(0);
                updateCallBanner();
            }

            ScheduledMeetingStatus schedMeetStatus = chatState.getScheduledMeetingStatus();

            if (chatRoom != null && chatRoom.isActive() && !chatRoom.isArchived() && (schedMeetStatus instanceof ScheduledMeetingStatus.NotStarted ||
                    schedMeetStatus instanceof ScheduledMeetingStatus.NotJoined)) {
                startOrJoinMeetingBanner.setText(schedMeetStatus instanceof ScheduledMeetingStatus.NotStarted ?
                        R.string.meetings_chat_room_start_scheduled_meeting_option :
                        R.string.meetings_chat_room_join_scheduled_meeting_option);

                startOrJoinMeetingBanner.setVisibility(View.VISIBLE);
                callInProgressLayout.setVisibility(View.GONE);
            } else {
                startOrJoinMeetingBanner.setVisibility(View.GONE);
            }

            if (schedMeet != null) {
                setTitle(null);
            }

            long callChatId = chatState.getCurrentCallChatId();

            if (callChatId != MEGACHAT_INVALID_HANDLE) {
                Timber.d("Open call with chat Id " + callChatId);
                viewModel.removeCurrentCall();
                Intent intentMeeting = new Intent(chatActivity, MeetingActivity.class);
                intentMeeting.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intentMeeting.setAction(MEETING_ACTION_IN);
                intentMeeting.putExtra(MEETING_CHAT_ID, callChatId);
                intentMeeting.putExtra(MeetingActivity.MEETING_AUDIO_ENABLE, chatState.getCurrentCallAudioStatus());
                intentMeeting.putExtra(MeetingActivity.MEETING_VIDEO_ENABLE, chatState.getCurrentCallVideoStatus());
                startActivity(intentMeeting);
            }

            if (chatState.getTitleChatArchivedEvent() != null) {
                viewModel.onChatArchivedEventConsumed();
                closeChat(true);
                finish();
            }

            if (chatState.getJoiningOrLeavingAction() != null) {
                setJoiningOrLeavingView(chatState.getJoiningOrLeavingAction());
            }

            if (chatState.getSnackbarMessage() != null) {
                showSnackbar(SNACKBAR_TYPE, getString(chatState.getSnackbarMessage()), MEGACHAT_INVALID_HANDLE);
                viewModel.onSnackbarMessageConsumed();
            }

            ContactInvitation contactInvitation = chatState.getContactInvitation();
            if (contactInvitation != null) {
                if (contactInvitation.isSent()) {
                    String text = getString(R.string.contact_already_invited, converterShortCodes(contactInvitation.getName()));
                    showSnackbar(SENT_REQUESTS_TYPE, text, MEGACHAT_INVALID_HANDLE);
                } else {
                    String text = getString(R.string.user_is_not_contact, converterShortCodes(contactInvitation.getName()));
                    showSnackbar(INVITE_CONTACT_TYPE, text, MEGACHAT_INVALID_HANDLE, contactInvitation.getEmail());
                }
                viewModel.onContactInvitationConsumed();
            }

            return Unit.INSTANCE;
        });
        ViewExtensionsKt.collectFlow(this, viewModel.getMonitorConnectivityEvent(), Lifecycle.State.STARTED, isConnected -> {
            if (isConnected && megaApi.getRootNode() == null) {
                showConfirmationConnect();
            }
            return Unit.INSTANCE;
        });
        viewModel.onPendingMessageLoaded().observe(this, this::onPendingMessageLoaded);
        ViewExtensionsKt.collectFlow(this, waitingRoomManagementViewModel.getState(), Lifecycle.State.STARTED, waitingRoomManagementState -> {
            if (waitingRoomManagementState.getShowParticipantsInWaitingRoomDialog()) {
                showWaitingRoomDialog();
            } else if (usersInWaitingRoomDialogFragment != null) {
                usersInWaitingRoomDialogFragment.dismissAllowingStateLoss();
            }

            return Unit.INSTANCE;
        });
    }

    public void checkScroll() {
        if (listView == null) return;

        Util.changeToolBarElevation(this, tB, listView.canScrollVertically(-1) || adapter.isMultipleSelect());
        setStatusIcon();
    }

    public void initAfterIntent(Intent newIntent, Bundle savedInstanceState) {
        if (newIntent != null) {
            Timber.d("Intent is not null");
            intentAction = newIntent.getAction();
            if (intentAction != null) {
                if (intentAction.equals(ACTION_OPEN_CHAT_LINK) || intentAction.equals(ACTION_JOIN_OPEN_CHAT_LINK)) {
                    String link = newIntent.getDataString();
                    megaChatApi.openChatPreview(link, new LoadPreviewListener(ChatActivity.this, ChatActivity.this, ChatActivity.this, CHECK_LINK_TYPE_CHAT_LINK));

                    if (intentAction.equals(ACTION_JOIN_OPEN_CHAT_LINK) && MegaApplication.getChatManagement().isAlreadyJoining(idChat)) {
                        openingAndJoining = true;
                        setJoiningOrLeaving(R.string.joining_label);
                    }
                } else {
                    long newIdChat = newIntent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);

                    if (idChat != newIdChat && newIdChat != MEGACHAT_INVALID_HANDLE) {
                        megaChatApi.closeChatRoom(idChat, this);
                        idChat = newIdChat;
                        viewModel.setChatId(idChat);
                    }

                    composite.clear();
                    checkChatChanges();
                    myUserHandle = megaChatApi.getMyUserHandle();

                    if (savedInstanceState != null) {

                        Timber.d("Bundle is NOT NULL");
                        selectedMessageId = savedInstanceState.getLong("selectedMessageId", -1);
                        Timber.d("Handle of the message: %s", selectedMessageId);
                        selectedPosition = savedInstanceState.getInt("selectedPosition", -1);
                        mOutputFilePath = savedInstanceState.getString("mOutputFilePath");
                        isShareLinkDialogDismissed = savedInstanceState.getBoolean("isShareLinkDialogDismissed", false);
                        isLocationDialogShown = savedInstanceState.getBoolean("isLocationDialogShown", false);
                        isJoinCallDialogShown = savedInstanceState.getBoolean(JOIN_CALL_DIALOG, false);
                        isOnlyMeInCallDialogShown = savedInstanceState.getBoolean(ONLY_ME_IN_CALL_DIALOG, false);
                        isEndCallForAllDialogShown = savedInstanceState.getBoolean(END_CALL_FOR_ALL_DIALOG, false);
                        recoveredSelectedPositions = savedInstanceState.getIntegerArrayList(SELECTED_ITEMS);
                        lastIdMsgSeen = savedInstanceState.getLong(LAST_MESSAGE_SEEN, MEGACHAT_INVALID_HANDLE);
                        isTurn = lastIdMsgSeen != MEGACHAT_INVALID_HANDLE;

                        generalUnreadCount = savedInstanceState.getLong(GENERAL_UNREAD_COUNT, 0);

                        long[] longArray = savedInstanceState.getLongArray(NUM_MSGS_RECEIVED_AND_UNREAD);
                        if (longArray != null && longArray.length > 0) {
                            for (int i = 0; i < longArray.length; i++) {
                                msgsReceived.add(longArray[i]);
                            }
                        }

                        boolean isPlaying = savedInstanceState.getBoolean(PLAYING, false);
                        if (isPlaying) {
                            long idMessageVoicePlaying = savedInstanceState.getLong(ID_VOICE_CLIP_PLAYING, -1);
                            long messageHandleVoicePlaying = savedInstanceState.getLong(MESSAGE_HANDLE_PLAYING, -1);
                            long userHandleVoicePlaying = savedInstanceState.getLong(USER_HANDLE_PLAYING, -1);
                            int progressVoicePlaying = savedInstanceState.getInt(PROGRESS_PLAYING, 0);

                            if (!messagesPlaying.isEmpty()) {
                                for (MessageVoiceClip m : messagesPlaying) {
                                    m.getMediaPlayer().release();
                                    m.setMediaPlayer(null);
                                }
                                messagesPlaying.clear();
                            }

                            MessageVoiceClip messagePlaying = new MessageVoiceClip(idMessageVoicePlaying, userHandleVoicePlaying, messageHandleVoicePlaying);
                            messagePlaying.setProgress(progressVoicePlaying);
                            messagePlaying.setPlayingWhenTheScreenRotated(true);
                            messagesPlaying.add(messagePlaying);

                        }

                        openingAndJoining = savedInstanceState.getBoolean(OPENING_AND_JOINING_ACTION, false);
                        errorReactionsDialogIsShown = savedInstanceState.getBoolean(ERROR_REACTION_DIALOG, false);
                        typeErrorReaction = savedInstanceState.getLong(TYPE_ERROR_REACTION, REACTION_ERROR_DEFAULT_VALUE);
                        if (errorReactionsDialogIsShown && typeErrorReaction != REACTION_ERROR_DEFAULT_VALUE) {
                            createLimitReactionsAlertDialog(typeErrorReaction);
                        }

                        nodeSaver.restoreState(savedInstanceState);
                    }

                    String text = null;
                    if (intentAction.equals(ACTION_CHAT_SHOW_MESSAGES)) {
                        Timber.d("ACTION_CHAT_SHOW_MESSAGES");
                        isOpeningChat = true;

                        int errorCode = newIntent.getIntExtra("PUBLIC_LINK", 1);
                        if (savedInstanceState == null) {
                            text = newIntent.getStringExtra(SHOW_SNACKBAR);
                            if (text == null) {
                                if (errorCode != 1) {
                                    if (errorCode == MegaChatError.ERROR_OK) {
                                        text = getString(R.string.chat_link_copied_clipboard);
                                    } else {
                                        Timber.d("initAfterIntent:publicLinkError:errorCode");
                                        text = getString(R.string.general_error) + ": " + errorCode;
                                    }
                                }
                            }
                        } else if (errorCode != 1 && errorCode == MegaChatError.ERROR_OK && !isShareLinkDialogDismissed) {
                            text = getString(R.string.chat_link_copied_clipboard);
                        }
                    }
                    initEmptyScreen(text);
                    initAndShowChat();
                }
            }
        } else {
            Timber.w("INTENT is NULL");
        }
    }

    private void initializeInputText() {
        hideKeyboard();
        setChatSubtitle();
        ChatItemPreferences chatPrefs = dbH.findChatPreferencesByHandle(Long.toString(idChat));

        if (chatPrefs != null) {
            String written = chatPrefs.getWrittenText();
            if (!TextUtils.isEmpty(written)) {
                String editedMsgId = chatPrefs.getEditedMsgId();
                editingMessage = !isTextEmpty(editedMsgId);
                messageToEdit = editingMessage ? megaChatApi.getMessage(idChat, Long.parseLong(editedMsgId)) : null;

                if (editingMessage) {
                    editMsgUI(written);
                } else {
                    hideEditMsgLayout();
                    textChat.setText(written);
                    textChat.post(() -> controlExpandableInputText(textChat.getLineCount()));
                    showSendIcon();
                }

                return;
            }
        } else {
            chatPrefs = new ChatItemPreferences(Long.toString(idChat), "");
            dbH.setChatItemPreferences(chatPrefs);
        }
        refreshTextInput();
    }

    private CharSequence transformEmojis(String textToTransform, float sizeText) {
        CharSequence text = textToTransform == null ? "" : textToTransform;
        String resultText = converterShortCodes(text.toString());
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(resultText);
        EmojiManager.getInstance().replaceWithImages(this, spannableStringBuilder, sizeText, sizeText);
        int maxWidth;
        if (isScreenInPortrait(this)) {
            maxWidth = HINT_PORT;
        } else {
            maxWidth = HINT_LAND;
        }
        CharSequence textF = TextUtils.ellipsize(spannableStringBuilder, textChat.getPaint(), dp2px(maxWidth, getOutMetrics()), typeEllipsize);
        return textF;
    }

    private void refreshTextInput() {
        recordButtonStates(RECORD_BUTTON_DEACTIVATED);
        collapseInputText();
        sendIcon.setVisibility(View.GONE);
        sendIcon.setEnabled(false);
        emojiKeyboard.changeKeyboardIcon();
        sendIcon.setImageDrawable(ColorUtils.tintIcon(chatActivity, R.drawable.ic_send_white, R.color.grey_054_white_054));

        if (chatRoom != null) {
            megaChatApi.sendStopTypingNotification(chatRoom.getChatId());
            String title;
            setSizeInputText(true);
            if (chatRoom.hasCustomTitle()) {
                title = getString(R.string.type_message_hint_with_customized_title, getTitleChat(chatRoom));
            } else {
                title = getString(R.string.type_message_hint_with_default_title, getTitleChat(chatRoom));
            }
            textChat.setHint(transformEmojis(title, textChat.getTextSize()));
            controlExpandableInputText(1);
        }
    }

    public void updateUserNameInChat() {
        if (chatRoom != null && chatRoom.isGroup()) {
            setChatSubtitle();
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void updateTitle() {
        initializeInputText();
        setTitle(null);
    }

    /**
     * Set chat room title
     */
    private void setTitle(String newTitle) {
        String title = viewModel.getSchedTitle();
        if (title == null) {
            if (newTitle != null) {
                title = newTitle;
            } else {
                title = getTitleChat(chatRoom);
            }
        }

        titleToolbar.setText(title);
    }

    /**
     * Opens a new chat conversation, checking if the id is valid and if the ChatRoom exists.
     * If an error occurred opening the chat, an error dialog is shown.
     *
     * @return True if the chat was successfully opened, false otherwise
     */
    private boolean initChat() {
        if (idChat == MEGACHAT_INVALID_HANDLE) {
            Timber.e("Chat ID -1 error");
            return false;
        }

        //Recover chat
        Timber.d("Recover chat with id: %s", idChat);
        chatRoom = megaChatApi.getChatRoom(idChat);
        if (chatRoom == null) {
            Timber.e("Chatroom is NULL - finish activity!!");
            finish();
        }

        if (adapter != null) {
            adapter.updateChatRoom(chatRoom);
        }

        megaChatApi.closeChatRoom(idChat, this);
        if (megaChatApi.openChatRoom(idChat, this)) {
            viewModel.setChatInitialised(true);
            MegaApplication.setClosedChat(false);
            return true;
        }

        Timber.e("Error openChatRoom");
        if (errorOpenChatDialog == null) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle(getString(R.string.chat_error_open_title))
                    .setMessage(getString(R.string.chat_error_open_message))
                    .setPositiveButton(getString(R.string.general_ok), (dialog, whichButton) -> finish());
            errorOpenChatDialog = builder.create();
            errorOpenChatDialog.show();
        }

        return false;
    }

    /**
     * Shows the chat with the empty state and requests messages.
     *
     * @param textSnackbar if there is a chat link involved in the action, it it indicates the "Copy chat link" dialog has to be shown.
     *                     If not, a simple Snackbar has to be shown with this text.
     */
    private void initEmptyScreen(String textSnackbar) {
        String textToShowB = getString(R.string.chat_loading_messages);

        try {
            textToShowB = textToShowB.replace("[A]", "<font color=\'"
                    + ColorUtils.getColorHexString(this, R.color.grey_300_grey_600)
                    + "\'>");
            textToShowB = textToShowB.replace("[/A]", "</font>");
            textToShowB = textToShowB.replace("[B]", "<font color=\'"
                    + ColorUtils.getColorHexString(this, R.color.grey_900_grey_100)
                    + "\'>");
            textToShowB = textToShowB.replace("[/B]", "</font>");
        } catch (Exception e) {
            Timber.w(e, "Exception formatting string");
        }

        emptyScreen(HtmlCompat.fromHtml(textToShowB, HtmlCompat.FROM_HTML_MODE_LEGACY));

        if (!isTextEmpty(textSnackbar)) {
            String chatLink = getIntent().getStringExtra(CHAT_LINK_EXTRA);

            if (!isTextEmpty(chatLink) && !isShareLinkDialogDismissed) {
                showShareChatLinkDialog(this, chatRoom, chatLink);
            } else {
                showSnackbar(SNACKBAR_TYPE, textSnackbar, MEGACHAT_INVALID_HANDLE);
            }
        }
    }

    /**
     * Opens a new chat conversation.
     * If it went well, requests messages.
     */
    private void initAndShowChat() {
        boolean resultInit = initChat();
        if (!resultInit) {
            return;
        }

        initEmptyScreen(null);

        initializeInputText();
        checkIfIsAlreadyJoiningOrLeaving();

        int chatConnection = megaChatApi.getChatConnectionState(idChat);
        Timber.d("Chat connection (%d) is: %d", idChat, chatConnection);

        //Create always a new adapter to avoid showing messages of a previous conversation.
        createAdapter();

        setPreviewersView();
        setTitle(null);
        setChatSubtitle();
        privateIconToolbar.setVisibility((!chatRoom.isGroup() || !chatRoom.isPublic()) ? View.VISIBLE : View.GONE);
        muteIconToolbar.setVisibility(isEnableChatNotifications(chatRoom.getChatId()) ? View.GONE : View.VISIBLE);
        isOpeningChat = true;

        loadHistory();
        Timber.d("On create: stateHistory: %s", stateHistory);
        if (isLocationDialogShown) {
            showSendLocationDialog();
        }
        if (isOnlyMeInCallDialogShown) {
            showOnlyMeInTheCallDialog();
        }
        if (isEndCallForAllDialogShown) {
            showEndCallForAllDialog();
        }

        updateCallBanner();

        if (isJoinCallDialogShown) {
            MegaChatCall callInThisChat = megaChatApi.getChatCall(chatRoom.getChatId());
            if (callInThisChat != null && !callInThisChat.isOnHold() && chatRoom.isGroup()) {
                ArrayList<Long> numCallsParticipating = getCallsParticipating();
                if (numCallsParticipating == null || numCallsParticipating.isEmpty())
                    return;

                if (numCallsParticipating.size() == 1) {
                    MegaChatCall anotherCallActive = getAnotherActiveCall(chatRoom.getChatId());
                    if (anotherCallActive != null) {
                        showJoinCallDialog(callInThisChat.getChatid(), anotherCallActive, false);
                        return;
                    }

                    MegaChatCall anotherCallOnHold = getAnotherCallOnHold(chatRoom.getChatId());
                    if (anotherCallOnHold != null) {
                        showJoinCallDialog(callInThisChat.getChatid(), anotherCallOnHold, false);
                    }
                } else {
                    for (int i = 0; i < numCallsParticipating.size(); i++) {
                        MegaChatCall call = megaChatApi.getChatCall(numCallsParticipating.get(i));
                        if (call != null && !call.isOnHold()) {
                            showJoinCallDialog(callInThisChat.getChatid(), call, true);
                        }
                    }
                }
            }
        }
    }

    private void emptyScreen(CharSequence text) {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            emptyImageView.setImageResource(R.drawable.empty_chat_message_landscape);
        } else {
            emptyImageView.setImageResource(R.drawable.empty_chat_message_portrait);
        }

        emptyTextView.setText(text);
        emptyTextView.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.VISIBLE);

        chatRelativeLayout.setVisibility(View.GONE);
    }

    public void removeChatLink() {
        Timber.d("removeChatLink");
        megaChatApi.removeChatLink(idChat, this);
    }

    /**
     * Requests to load for first time chat messages.
     * It controls if it is the real first time, or the device was rotated with the "isTurn" flag.
     */
    public void loadHistory() {
        if (chatRoom == null) {
            return;
        }

        long unreadCount = chatRoom.getUnreadCount();
        lastSeenReceived = unreadCount == 0;

        if (lastSeenReceived) {
            Timber.d("loadMessages:unread is 0");

            if (!isTurn) {
                lastIdMsgSeen = MEGACHAT_INVALID_HANDLE;
                generalUnreadCount = 0;
            }
        } else {
            if (!isTurn) {
                lastIdMsgSeen = megaChatApi.getLastMessageSeenId(idChat);
                generalUnreadCount = unreadCount;
            } else {
                Timber.d("Do not change lastSeenId --> rotating screen");
            }

            if (lastIdMsgSeen != -1) {
                Timber.d("lastSeenId: %s", lastIdMsgSeen);
            } else {
                Timber.e("Error:InvalidLastMessage");
            }
        }

        askForMoreMessages();
    }

    /**
     * Sets the visibility of the groupalSubtitleToolbar view.
     * If it is visible some attributes of the layout should be updated due to the marquee behaviour.
     * <p>
     * This method should be used always the visibility of groupalSubtitleToolbar
     * changes instead of change the visibility directly.
     *
     * @param visible true if visible, false otherwise
     */
    private void setGroupalSubtitleToolbarVisibility(boolean visible) {
        if (subtitleCall.getVisibility() == View.VISIBLE) {
            visible = false;
        }

        groupalSubtitleToolbar.setVisibility(visible ? View.VISIBLE : View.GONE);

        if (visible) {
            groupalSubtitleToolbar.setSelected(true);
            groupalSubtitleToolbar.setHorizontallyScrolling(true);
            groupalSubtitleToolbar.setFocusable(true);
            groupalSubtitleToolbar.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            groupalSubtitleToolbar.setMarqueeRepeatLimit(-1);
            groupalSubtitleToolbar.setSingleLine(true);
            groupalSubtitleToolbar.setHorizontallyScrolling(true);
        }
    }

    private void setSubtitleVisibility() {
        if (chatRoom == null) {
            chatRoom = megaChatApi.getChatRoom(idChat);
        }

        if (chatRoom == null)
            return;

        boolean isGroup = chatRoom.isGroup();
        individualSubtitleToobar.setVisibility(isGroup ? View.GONE : View.VISIBLE);
        setGroupalSubtitleToolbarVisibility(isGroup);

        if (chatRoom.isGroup()) {
            iconStateToolbar.setVisibility(View.GONE);
        }
    }

    private void setPreviewGroupalSubtitle() {
        long participants = chatRoom.getPeerCount();

        if (!chatRoom.isPreview() && chatRoom.isActive()) {
            participants++;
        }

        setGroupalSubtitleToolbarVisibility(participants > 0);
        if (participants > 0) {
            groupalSubtitleToolbar.setText(adjustForLargeFont(getResources()
                    .getQuantityString(R.plurals.subtitle_of_group_chat, (int) participants, (int) participants)));
        }
    }

    public void setChatSubtitle() {
        Timber.d("setChatSubtitle");
        if (chatRoom == null) {
            return;
        }

        setSubtitleVisibility();

        if (chatC.isInAnonymousMode() && megaChatApi.getChatConnectionState(idChat) == MegaChatApi.CHAT_CONNECTION_ONLINE) {
            Timber.d("Is preview");
            setPreviewGroupalSubtitle();
            tB.setOnClickListener(this);
            setBottomLayout(SHOW_JOIN_LAYOUT);

        } else if (megaChatApi.getConnectionState() != MegaChatApi.CONNECTED || megaChatApi.getChatConnectionState(idChat) != MegaChatApi.CHAT_CONNECTION_ONLINE) {
            Timber.d("Chat not connected ConnectionState: %d ChatConnectionState: %d", megaChatApi.getConnectionState(), megaChatApi.getChatConnectionState(idChat));
            tB.setOnClickListener(this);
            if (chatRoom.isPreview()) {
                Timber.d("Chat not connected: is preview");
                setPreviewGroupalSubtitle();
                setBottomLayout(SHOW_NOTHING_LAYOUT);
            } else {
                Timber.d("Chat not connected: is not preview");
                if (chatRoom.isGroup()) {
                    groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.invalid_connection_state)));
                } else {
                    individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.invalid_connection_state)));
                }

                int permission = chatRoom.getOwnPrivilege();
                Timber.d("Check permissions");
                if ((permission == MegaChatRoom.PRIV_RO) || (permission == MegaChatRoom.PRIV_RM)) {
                    setBottomLayout(SHOW_NOTHING_LAYOUT);
                } else {
                    setBottomLayout(SHOW_WRITING_LAYOUT);
                }
            }
        } else {
            Timber.d("Karere connection state: %s", megaChatApi.getConnectionState());
            Timber.d("Chat connection state: %s", megaChatApi.getChatConnectionState(idChat));

            int permission = chatRoom.getOwnPrivilege();
            if (chatRoom.isGroup()) {
                tB.setOnClickListener(this);
                if (chatRoom.isPreview()) {
                    Timber.d("Is preview");
                    setPreviewGroupalSubtitle();
                    setBottomLayout(openingAndJoining ? SHOW_JOINING_OR_LEFTING_LAYOUT : SHOW_JOIN_LAYOUT);
                } else {
                    Timber.d("Check permissions group chat");
                    if (permission == MegaChatRoom.PRIV_RO) {
                        Timber.d("Permission RO");
                        setBottomLayout(SHOW_NOTHING_LAYOUT);

                        if (chatRoom.isArchived()) {
                            Timber.d("Chat is archived");
                            groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.archived_chat)));
                        } else {
                            groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.observer_permission_label_participants_panel)));
                        }
                    } else if (permission == MegaChatRoom.PRIV_RM) {
                        Timber.d("Permission RM");
                        setBottomLayout(SHOW_NOTHING_LAYOUT);

                        if (chatRoom.isArchived()) {
                            Timber.d("Chat is archived");
                            groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.archived_chat)));
                        } else if (!chatRoom.isActive()) {
                            groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.inactive_chat)));
                        } else {
                            groupalSubtitleToolbar.setText(null);
                            setGroupalSubtitleToolbarVisibility(false);
                        }
                    } else {
                        Timber.d("Permission: %s", permission);

                        setBottomLayout(SHOW_WRITING_LAYOUT);

                        if (chatRoom.isArchived()) {
                            Timber.d("Chat is archived");
                            groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.archived_chat)));
                        } else if (chatRoom.hasCustomTitle()) {
                            setCustomSubtitle();
                        } else {
                            long participantsLabel = chatRoom.getPeerCount() + 1; //Add one to include me
                            groupalSubtitleToolbar.setText(adjustForLargeFont(getResources().getQuantityString(R.plurals.subtitle_of_group_chat, (int) participantsLabel, participantsLabel)));
                        }
                    }
                }
            } else {
                Timber.d("Check permissions one to one chat");
                if (permission == MegaChatRoom.PRIV_RO) {
                    Timber.d("Permission RO");

                    if (megaApi != null) {
                        if (megaApi.getRootNode() != null) {
                            long chatHandle = chatRoom.getChatId();
                            MegaChatRoom chat = megaChatApi.getChatRoom(chatHandle);
                            long userHandle = chat.getPeerHandle(0);
                            String userHandleEncoded = MegaApiAndroid.userHandleToBase64(userHandle);
                            MegaUser user = megaApi.getContact(userHandleEncoded);

                            if (user != null && user.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                                tB.setOnClickListener(this);
                            } else {
                                tB.setOnClickListener(null);
                            }
                        }
                    } else {
                        tB.setOnClickListener(null);
                    }
                    setBottomLayout(SHOW_NOTHING_LAYOUT);

                    if (chatRoom.isArchived()) {
                        Timber.d("Chat is archived");
                        individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.archived_chat)));
                    } else {
                        individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.observer_permission_label_participants_panel)));
                    }
                } else if (permission == MegaChatRoom.PRIV_RM) {
                    tB.setOnClickListener(this);

                    Timber.d("Permission RM");
                    setBottomLayout(SHOW_NOTHING_LAYOUT);

                    if (chatRoom.isArchived()) {
                        Timber.d("Chat is archived");
                        individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.archived_chat)));
                    } else if (!chatRoom.isActive()) {
                        individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.inactive_chat)));
                    } else {
                        individualSubtitleToobar.setText(null);
                        individualSubtitleToobar.setVisibility(View.GONE);
                    }
                } else {
                    tB.setOnClickListener(this);

                    long userHandle = chatRoom.getPeerHandle(0);
                    setStatus(userHandle);
                    setBottomLayout(SHOW_WRITING_LAYOUT);
                }
            }
        }
    }

    /**
     * Updates the views that have to be shown at the bottom of the UI.
     *
     * @param show indicates which layout has to be shown at the bottom of the UI
     */
    public void setBottomLayout(int show) {
        if (viewModel.getStorageState() == StorageState.PayWall) {
            show = SHOW_NOTHING_LAYOUT;
        } else if (viewModel.isJoiningOrLeaving()) {
            show = SHOW_JOINING_OR_LEFTING_LAYOUT;
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) messagesContainerLayout.getLayoutParams();

        switch (show) {
            case SHOW_JOINING_OR_LEFTING_LAYOUT:
                writingContainerLayout.setVisibility(View.GONE);
                joinChatLinkLayout.setVisibility(View.GONE);
                params.addRule(RelativeLayout.ABOVE, R.id.joining_leaving_layout_chat_layout);
                messagesContainerLayout.setLayoutParams(params);
                fragmentVoiceClip.setVisibility(View.GONE);
                joiningLeavingLayout.setVisibility(View.VISIBLE);
                break;

            case SHOW_JOIN_LAYOUT:
                writingContainerLayout.setVisibility(View.GONE);
                joinChatLinkLayout.setVisibility(View.VISIBLE);
                params.addRule(RelativeLayout.ABOVE, R.id.join_chat_layout_chat_layout);
                messagesContainerLayout.setLayoutParams(params);
                fragmentVoiceClip.setVisibility(View.GONE);
                joiningLeavingLayout.setVisibility(View.GONE);
                break;

            case SHOW_NOTHING_LAYOUT:
                writingContainerLayout.setVisibility(View.GONE);
                joinChatLinkLayout.setVisibility(View.GONE);
                fragmentVoiceClip.setVisibility(View.GONE);
                joiningLeavingLayout.setVisibility(View.GONE);
                break;

            default:
                writingContainerLayout.setVisibility(View.VISIBLE);
                joinChatLinkLayout.setVisibility(View.GONE);
                params.addRule(RelativeLayout.ABOVE, R.id.writing_container_layout_chat_layout);
                messagesContainerLayout.setLayoutParams(params);
                fragmentVoiceClip.setVisibility(View.VISIBLE);
                joiningLeavingLayout.setVisibility(View.GONE);
        }
    }

    /**
     * When the group chat has a custom title, the subtitle has to contain the participants' names.
     * It sets the custom subtitle. The subtitle would contain the participant's names following these rules:
     * - If the group has four or less participants: all their names.
     * - If the group has more than four participants: the names of the three first participants and X more,
     * which "X" is the number of the rest of participants
     */
    private void setCustomSubtitle() {
        Timber.d("setCustomSubtitle");
        long participantsCount = chatRoom.getPeerCount();

        if (participantsCount == 0 && !chatRoom.isPreview()) {
            groupalSubtitleToolbar.setText(R.string.bucket_word_me);
            return;
        }

        StringBuilder customSubtitle = new StringBuilder();

        for (int i = 0; i < participantsCount; i++) {
            if ((i == 1 || i == 2) && areMoreParticipants(i)) {
                customSubtitle.append(", ");
            }
            String participantName = chatC.getParticipantFirstName(chatRoom.getPeerHandle(i));
            if (isTextEmpty(participantName)) {
                sendGetPeerAttributesRequest(participantsCount);
                return;
            } else if (i == 0 && !areMoreParticipants(i)) {
                if (!chatRoom.isPreview()) {
                    customSubtitle.append(participantName)
                            .append(", ").append(getString(R.string.bucket_word_me));
                    groupalSubtitleToolbar.setText(adjustForLargeFont(customSubtitle.toString()));
                } else {
                    groupalSubtitleToolbar.setText(adjustForLargeFont(participantName));
                }
            } else if (areMoreParticipantsThanMaxAllowed(i)) {
                String firstNames = customSubtitle.append(participantName).toString();
                groupalSubtitleToolbar.setText(adjustForLargeFont(getString(R.string.custom_subtitle_of_group_chat, firstNames, participantsCount - 2)));
                break;
            } else {
                customSubtitle.append(participantName);

                if (i == participantsCount - 1) {
                    if (!chatRoom.isPreview()) {
                        customSubtitle.append(", ").append(getString(R.string.bucket_word_me));
                    }
                    groupalSubtitleToolbar.setText(adjustForLargeFont(customSubtitle.toString()));
                }
            }
        }

        if (isTextEmpty(groupalSubtitleToolbar.getText().toString())) {
            groupalSubtitleToolbar.setText(null);
            setGroupalSubtitleToolbarVisibility(false);
        }
    }

    /**
     * Checks if there are more participants in the group chat after the current position.
     *
     * @param position position to check
     * @return True if there are more participants after the current position, false otherwise.
     */
    private boolean areMoreParticipants(long position) {
        return chatRoom.getPeerCount() > position;
    }

    /**
     * Checks if there only three participants in the group chat.
     *
     * @param position position to check
     * @return True if there are three participants, false otherwise.
     */
    private boolean areSameParticipantsAsMaxAllowed(long position) {
        return chatRoom.getPeerCount() == MAX_NAMES_PARTICIPANTS && position == 2;
    }

    /**
     * Checks if there are more than three participants in the group chat.
     *
     * @param position position to check
     * @return True if there are more than three participants, false otherwise.
     */
    private boolean areMoreParticipantsThanMaxAllowed(long position) {
        return chatRoom.getPeerCount() > MAX_NAMES_PARTICIPANTS && position == 2;
    }

    /**
     * Requests the attributes of the participants when they unavailable.
     *
     * @param participantsCount number of participants in the group chat.
     */
    private void sendGetPeerAttributesRequest(long participantsCount) {
        MegaHandleList handleList = MegaHandleList.createInstance();

        for (int i = 0; i < participantsCount; i++) {
            handleList.addMegaHandle(chatRoom.getPeerHandle(i));

            if (areMoreParticipantsThanMaxAllowed(i) || areSameParticipantsAsMaxAllowed(i))
                break;
        }

        if (handleList.size() > 0) {
            megaChatApi.loadUserAttributes(chatRoom.getChatId(), handleList, new GetPeerAttributesListener(this));
        }
    }

    /**
     * Updates the custom subtitle when the request for load the participants' attributes finishes.
     *
     * @param chatId     identifier of the chat received in the request
     * @param handleList list of the participants' handles
     */
    public void updateCustomSubtitle(long chatId, MegaHandleList handleList) {
        if (handleList == null || handleList.size() == 0 || (chatRoom != null && chatId != chatRoom.getChatId()))
            return;

        chatRoom = megaChatApi.getChatRoom(chatId);

        if (chatRoom == null)
            return;

        for (int i = 0; i < handleList.size(); i++) {
            chatC.setNonContactAttributesInDB(handleList.get(i));
        }

        setCustomSubtitle();
    }

    public void setLastGreen(String date) {
        individualSubtitleToobar.setText(date);
        individualSubtitleToobar.isMarqueeIsNecessary(this);
        if (subtitleCall.getVisibility() != View.VISIBLE && groupalSubtitleToolbar.getVisibility() != View.VISIBLE) {
            individualSubtitleToobar.setVisibility(View.VISIBLE);
        }
    }

    public void requestLastGreen(int state) {
        Timber.d("State: %s", state);

        if (chatRoom != null && !chatRoom.isGroup() && !chatRoom.isArchived()) {
            if (state == INITIAL_PRESENCE_STATUS) {
                state = megaChatApi.getUserOnlineStatus(chatRoom.getPeerHandle(0));
            }

            if (state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID) {
                Timber.d("Request last green for user");
                megaChatApi.requestLastGreen(chatRoom.getPeerHandle(0), this);
            }
        }
    }

    public void setStatus(long userHandle) {
        iconStateToolbar.setVisibility(View.GONE);

        if (megaChatApi.getConnectionState() != MegaChatApi.CONNECTED) {
            Timber.w("Chat not connected");
            individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.invalid_connection_state)));
        } else if (chatRoom.isArchived()) {
            Timber.d("Chat is archived");
            individualSubtitleToobar.setText(adjustForLargeFont(getString(R.string.archived_chat)));
        } else if (!chatRoom.isGroup()) {
            contactOnlineStatus = megaChatApi.getUserOnlineStatus(userHandle);
            setStatusIcon();
        }
    }

    /**
     * Set status icon image resource depends on online state and toolbar's elevation.
     */
    private void setStatusIcon() {
        if (chatRoom == null || chatRoom.isGroup() || listView == null || adapter == null
                || iconStateToolbar == null || individualSubtitleToobar == null) {
            return;
        }

        boolean withElevation = listView.canScrollVertically(-1) || adapter.isMultipleSelect();
        StatusIconLocation where = withElevation ? StatusIconLocation.APPBAR : StatusIconLocation.STANDARD;
        setContactStatus(contactOnlineStatus, iconStateToolbar, individualSubtitleToobar, where);
    }

    public int compareTime(AndroidMegaChatMessage message, AndroidMegaChatMessage previous) {
        return compareTime(message.getMessage().getTimestamp(), previous.getMessage().getTimestamp());
    }

    public int compareTime(long timeStamp, AndroidMegaChatMessage previous) {
        return compareTime(timeStamp, previous.getMessage().getTimestamp());
    }

    public int compareTime(long timeStamp, long previous) {
        if (previous != -1) {

            Calendar cal = calculateDateFromTimestamp(timeStamp);
            Calendar previousCal = calculateDateFromTimestamp(previous);

            TimeUtils tc = new TimeUtils(TIME);

            int result = tc.compare(cal, previousCal);
            Timber.d("RESULTS compareTime: %s", result);
            return result;
        } else {
            Timber.w("return -1");
            return -1;
        }
    }

    public int compareDate(AndroidMegaChatMessage message, AndroidMegaChatMessage previous) {
        return compareDate(message.getMessage().getTimestamp(), previous.getMessage().getTimestamp());
    }

    public int compareDate(long timeStamp, AndroidMegaChatMessage previous) {
        return compareDate(timeStamp, previous.getMessage().getTimestamp());
    }

    public int compareDate(long timeStamp, long previous) {
        Timber.d("compareDate");

        if (previous != -1) {
            Calendar cal = calculateDateFromTimestamp(timeStamp);
            Calendar previousCal = calculateDateFromTimestamp(previous);

            TimeUtils tc = new TimeUtils(DATE);

            int result = tc.compare(cal, previousCal);
            Timber.d("RESULTS compareDate: %s", result);
            return result;
        } else {
            Timber.w("return -1");
            return -1;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Timber.d("onCreateOptionsMenu");
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_action, menu);

        callMenuItem = menu.findItem(R.id.cab_menu_call_chat);
        videoMenuItem = menu.findItem(R.id.cab_menu_video_chat);
        selectMenuItem = menu.findItem(R.id.cab_menu_select_messages);
        inviteMenuItem = menu.findItem(R.id.cab_menu_invite_chat);
        clearHistoryMenuItem = menu.findItem(R.id.cab_menu_clear_history_chat);
        contactInfoMenuItem = menu.findItem(R.id.cab_menu_contact_info_chat);
        leaveMenuItem = menu.findItem(R.id.cab_menu_leave_chat);
        endCallForAllMenuItem = menu.findItem(R.id.cab_menu_end_call_for_all);
        archiveMenuItem = menu.findItem(R.id.cab_menu_archive_chat);
        muteMenuItem = menu.findItem(R.id.cab_menu_mute_chat);
        unMuteMenuItem = menu.findItem(R.id.cab_menu_unmute_chat);

        checkEndCallForAllMenuItem();

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Timber.d("onPrepareOptionsMenu");

        if (chatRoom != null && !viewModel.isJoiningOrLeaving()) {
            if (isEnableChatNotifications(chatRoom.getChatId())) {
                unMuteMenuItem.setVisible(false);
                muteMenuItem.setVisible(true);
            } else {
                muteMenuItem.setVisible(false);
                unMuteMenuItem.setVisible(true);
            }

            if (!shouldMuteOrUnmuteOptionsBeShown(this, chatRoom)) {
                unMuteMenuItem.setVisible(false);
                muteMenuItem.setVisible(false);
            }

            checkSelectOption();
            enableCallMenuItems(false);
            if (chatRoom.isGroup()) {
                videoMenuItem.setVisible(false);
            }

            inviteMenuItem.setVisible(false);

            if (chatRoom.isPreview() || !isStatusConnected(this, idChat)) {
                leaveMenuItem.setVisible(false);
                clearHistoryMenuItem.setVisible(false);
                contactInfoMenuItem.setVisible(false);
                archiveMenuItem.setVisible(false);
            } else {
                if (megaChatApi != null && (megaChatApi.getNumCalls() <= 0 || (!megaChatApi.hasCallInChatRoom(chatRoom.getChatId())))) {
                    enableCallMenuItems(true);
                    if (chatRoom.isGroup() || chatRoom.isMeeting()) {
                        videoMenuItem.setVisible(false);
                    }
                }

                archiveMenuItem.setVisible(true);
                if (chatRoom.isArchived()) {
                    archiveMenuItem.setTitle(getString(R.string.general_unarchive));
                } else {
                    archiveMenuItem.setTitle(getString(R.string.general_archive));
                }

                int permission = chatRoom.getOwnPrivilege();
                Timber.d("Permission in the chat: %s", permission);
                if (chatRoom.isGroup()) {
                    leaveMenuItem.setVisible(false);

                    if (permission == MegaChatRoom.PRIV_MODERATOR) {
                        inviteMenuItem.setVisible(chatRoom.isActive());

                        int lastMessageIndex = messages.size() - 1;
                        if (lastMessageIndex >= 0) {
                            AndroidMegaChatMessage lastMessage = messages.get(lastMessageIndex);
                            if (!lastMessage.isUploading()) {
                                if (lastMessage.getMessage().getType() == MegaChatMessage.TYPE_TRUNCATE) {
                                    Timber.d("Last message is TRUNCATE");
                                    clearHistoryMenuItem.setVisible(false);
                                } else {
                                    Timber.d("Last message is NOT TRUNCATE");
                                    clearHistoryMenuItem.setVisible(true);
                                }
                            } else {
                                Timber.d("Last message is UPLOADING");
                                clearHistoryMenuItem.setVisible(true);
                            }
                        } else {
                            clearHistoryMenuItem.setVisible(false);
                        }
                    } else {
                        inviteMenuItem.setVisible(chatRoom.isActive() && chatRoom.isOpenInvite());
                        clearHistoryMenuItem.setVisible(false);
                        if (permission == MegaChatRoom.PRIV_RM) {
                            Timber.d("Group chat PRIV_RM");
                            callMenuItem.setVisible(false);
                            videoMenuItem.setVisible(false);
                        } else if (permission == MegaChatRoom.PRIV_RO) {
                            Timber.d("Group chat PRIV_RO");
                            callMenuItem.setVisible(false);
                            videoMenuItem.setVisible(false);
                        } else if (permission == MegaChatRoom.PRIV_STANDARD) {
                            Timber.d("Group chat PRIV_STANDARD");
                        } else {
                            Timber.d("Permission: %s", permission);
                        }
                    }

                    contactInfoMenuItem.setTitle(getString(R.string.general_info));
                    contactInfoMenuItem.setVisible(true);
                } else {
                    if (permission == MegaChatRoom.PRIV_RO) {
                        clearHistoryMenuItem.setVisible(false);
                        contactInfoMenuItem.setVisible(false);
                        callMenuItem.setVisible(false);
                        videoMenuItem.setVisible(false);
                    } else {
                        clearHistoryMenuItem.setVisible(true);
                        contactInfoMenuItem.setTitle(getString(R.string.general_info));
                        contactInfoMenuItem.setVisible(true);
                    }
                    leaveMenuItem.setVisible(false);
                }
            }

        } else {
            Timber.w("Chatroom NULL on create menu");
            muteMenuItem.setVisible(false);
            unMuteMenuItem.setVisible(false);
            leaveMenuItem.setVisible(false);
            callMenuItem.setVisible(false);
            videoMenuItem.setVisible(false);
            selectMenuItem.setVisible(false);
            clearHistoryMenuItem.setVisible(false);
            inviteMenuItem.setVisible(false);
            contactInfoMenuItem.setVisible(false);
            archiveMenuItem.setVisible(false);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    void ifAnonymousModeLogin(boolean pendingJoin) {
        if (chatC.isInAnonymousMode()) {
            Intent loginIntent = new Intent(this, LoginActivity.class);
            if (pendingJoin && getIntent() != null && getIntent().getDataString() != null) {
                String link = getIntent().getDataString();

                MegaApplication.getChatManagement().setPendingJoinLink(link);
                loginIntent.putExtra(VISIBLE_FRAGMENT, LOGIN_FRAGMENT);
                loginIntent.setAction(ACTION_JOIN_OPEN_CHAT_LINK);
                loginIntent.setData(Uri.parse(link));
            } else {
                loginIntent.putExtra(VISIBLE_FRAGMENT, TOUR_FRAGMENT);
            }

            loginIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(loginIntent);
            chatRequestHandler.setIsLoggingRunning(true);
        }

        closeChat(true);
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Timber.d("onOptionsItemSelected");

        if (viewModel.getStorageState() == StorageState.PayWall &&
                (item.getItemId() == R.id.cab_menu_call_chat || item.getItemId() == R.id.cab_menu_video_chat)) {
            showOverDiskQuotaPaywallWarning();
            return false;
        }

        if (viewModel.isJoiningOrLeaving() && item.getItemId() != android.R.id.home) {
            return false;
        }

        int itemId = item.getItemId();// Respond to the action bar's Up/Home button
        if (itemId == android.R.id.home) {
            if (emojiKeyboard != null) {
                emojiKeyboard.hideBothKeyboard(this);
            }

            if (handlerEmojiKeyboard != null) {
                handlerEmojiKeyboard.removeCallbacksAndMessages(null);
            }
            if (handlerKeyboard != null) {
                handlerKeyboard.removeCallbacksAndMessages(null);
            }
            ifAnonymousModeLogin(false);
        } else if (itemId == R.id.cab_menu_call_chat) {
            if (recordView.isRecordingNow()) return super.onOptionsItemSelected(item);

            if (participatingInACall()) {
                showConfirmationInACall(this, getString(R.string.ongoing_call_content), passcodeManagement);
                return super.onOptionsItemSelected(item);
            }

            startVideo = false;
            checkCallInThisChat();
        } else if (itemId == R.id.cab_menu_video_chat) {
            if (recordView.isRecordingNow()) return super.onOptionsItemSelected(item);

            if (participatingInACall()) {
                showConfirmationInACall(this, getString(R.string.ongoing_call_content), passcodeManagement);
                return super.onOptionsItemSelected(item);
            }
            startVideo = true;
            checkCallInThisChat();
        } else if (itemId == R.id.cab_menu_select_messages) {
            activateActionMode();
        } else if (itemId == R.id.cab_menu_invite_chat) {
            if (recordView.isRecordingNow())
                return super.onOptionsItemSelected(item);

            chooseAddParticipantDialog();
        } else if (itemId == R.id.cab_menu_contact_info_chat) {
            if (recordView.isRecordingNow()) return super.onOptionsItemSelected(item);
            showGroupOrContactInfoActivity();
        } else if (itemId == R.id.cab_menu_clear_history_chat) {
            if (recordView.isRecordingNow()) return super.onOptionsItemSelected(item);

            Timber.d("Clear history selected!");
            stopReproductions();
            showConfirmationClearChat(this, chatRoom);
        } else if (itemId == R.id.cab_menu_leave_chat) {
            if (recordView.isRecordingNow()) return super.onOptionsItemSelected(item);

            Timber.d("Leave selected!");
            showConfirmationLeaveChat(chatActivity, chatRoom.getChatId(), chatActivity);
        } else if (itemId == R.id.cab_menu_end_call_for_all) {
            Timber.d("End call for all selected");
            showEndCallForAllDialog();
        } else if (itemId == R.id.cab_menu_archive_chat) {
            if (recordView.isRecordingNow()) return super.onOptionsItemSelected(item);

            Timber.d("Archive/unarchive selected!");
            ChatController chatC = new ChatController(chatActivity);
            chatC.archiveChat(chatRoom);
        } else if (itemId == R.id.cab_menu_mute_chat) {
            createMuteNotificationsAlertDialogOfAChat(this, chatRoom.getChatId());
        } else if (itemId == R.id.cab_menu_unmute_chat) {
            MegaApplication.getPushNotificationSettingManagement().controlMuteNotificationsOfAChat(this, NOTIFICATIONS_ENABLED, chatRoom.getChatId());
        }
        return super.onOptionsItemSelected(item);
    }

    /*
     *Prepare recording
     */
    private void prepareRecording() {
        recordView.playSound(TYPE_START_RECORD);
        stopReproductions();
    }


    /*
     * Start recording
     */
    public void startRecording() {
        AudioPlayerService.pauseAudioPlayer(this);

        long timeStamp = System.currentTimeMillis() / 1000;
        outputFileName = "/note_voice" + getVoiceClipName(timeStamp);
        File vcFile = buildVoiceClipFile(outputFileName);
        outputFileVoiceNotes = vcFile.getAbsolutePath();
        if (outputFileVoiceNotes == null) return;
        if (myAudioRecorder == null) myAudioRecorder = new MediaRecorder();
        try {
            myAudioRecorder.reset();
            myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            myAudioRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            myAudioRecorder.setAudioEncodingBitRate(50000);
            myAudioRecorder.setAudioSamplingRate(44100);
            myAudioRecorder.setAudioChannels(1);
            myAudioRecorder.setOutputFile(outputFileVoiceNotes);
            myAudioRecorder.prepare();

        } catch (IOException e) {
            controlErrorRecording();
            e.printStackTrace();
            return;
        }
        myAudioRecorder.start();
        setRecordingNow(true);
        if (isStartAndRecordVoiceClip) {
            recordView.startAndLockRecordingTime();
        } else {
            recordView.startRecordingTime();
        }

        handlerVisualizer.post(updateVisualizer);
        initRecordingItems(IS_LOW);
        recordingLayout.setVisibility(View.VISIBLE);
    }

    private void initRecordingItems(boolean isLow) {
        changeColor(firstBar, isLow);
        changeColor(secondBar, isLow);
        changeColor(thirdBar, isLow);
        changeColor(fourthBar, isLow);
        changeColor(fifthBar, isLow);
        changeColor(sixthBar, isLow);

    }

    public static String getVoiceClipName(long timestamp) {
        Timber.d("timestamp: %s", timestamp);
        //Get date time:
        try {
            Calendar calendar = Calendar.getInstance();
            TimeZone tz = TimeZone.getDefault();
            calendar.setTimeInMillis(timestamp * 1000L);
            calendar.add(Calendar.MILLISECOND, tz.getOffset(calendar.getTimeInMillis()));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            return sdf.format(calendar.getTime()) + ".m4a";

        } catch (Exception e) {
            Timber.e(e, "Error getting the voice clip name");
        }

        return null;
    }

    private void controlErrorRecording() {
        destroyAudioRecorderElements();
        textChat.requestFocus();
    }

    private void hideRecordingLayout() {
        if (recordingLayout == null || recordingLayout.getVisibility() == View.GONE) return;
        recordingChrono.setText("00:00");
        recordingLayout.setVisibility(View.GONE);
    }

    private void destroyAudioRecorderElements() {
        abandonAudioFocus(audioFocusListener, mAudioManager, request);
        handlerVisualizer.removeCallbacks(updateVisualizer);

        hideRecordingLayout();
        outputFileVoiceNotes = null;
        outputFileName = null;
        setRecordingNow(false);

        if (myAudioRecorder == null) return;
        myAudioRecorder.reset();
        myAudioRecorder.release();
        myAudioRecorder = null;
    }

    /*
     * Cancel recording and reset the audio recorder
     */
    public void cancelRecording() {
        if (!isRecordingNow() || myAudioRecorder == null)
            return;

        hideRecordingLayout();
        handlerVisualizer.removeCallbacks(updateVisualizer);

        try {
            myAudioRecorder.stop();
            myAudioRecorder.reset();
            myAudioRecorder = null;
            abandonAudioFocus(audioFocusListener, mAudioManager, request);
            ChatController.deleteOwnVoiceClip(this, outputFileName);
            outputFileVoiceNotes = null;
            setRecordingNow(false);
            textChat.requestFocus();

        } catch (RuntimeException stopException) {
            Timber.e(stopException, "Error canceling a recording");
            ChatController.deleteOwnVoiceClip(this, outputFileName);
            controlErrorRecording();

        }
    }

    /*
     * Stop the Record and send it to the chat
     */
    private void sendRecording() {
        Timber.d("sendRecording");

        if ((!recordView.isRecordingNow()) || (myAudioRecorder == null)) return;
        hideRecordingLayout();
        handlerVisualizer.removeCallbacks(updateVisualizer);

        try {
            myAudioRecorder.stop();
            recordView.playSound(TYPE_END_RECORD);
            abandonAudioFocus(audioFocusListener, mAudioManager, request);
            setRecordingNow(false);
            uploadPictureOrVoiceClip(outputFileVoiceNotes);
            outputFileVoiceNotes = null;
            textChat.requestFocus();
        } catch (RuntimeException ex) {
            controlErrorRecording();
        }
    }

    /**
     * Hide chat options while recording
     */
    private void hideChatOptions() {
        textChat.setVisibility(View.INVISIBLE);
        sendIcon.setVisibility(View.GONE);
        chatRoomOptions.setVisibility(View.GONE);
        disableButton(rLKeyboardTwemojiButton, keyboardTwemojiButton);
        emojiKeyboard.changeKeyboardIcon();
    }

    private void disableButton(final RelativeLayout layout, final ImageButton button) {
        Timber.d("disableButton");
        layout.setOnClickListener(null);
        button.setOnClickListener(null);
        button.setVisibility(View.INVISIBLE);
    }

    /**
     * Show chat options when not being recorded
     */
    private void showChatOptions() {
        textChat.setVisibility(View.VISIBLE);
        chatRoomOptions.setVisibility(View.VISIBLE);
        enableButton(rLKeyboardTwemojiButton, keyboardTwemojiButton);
    }

    private void enableButton(RelativeLayout layout, ImageButton button) {
        Timber.d("enableButton");
        layout.setOnClickListener(this);
        button.setOnClickListener(this);
        button.setVisibility(View.VISIBLE);
    }

    /**
     * Method that displays the send icon.
     */
    private void showSendIcon() {
        if (recordView.isRecordingNow())
            return;

        sendIcon.setEnabled(true);
        if (editingMessage) {
            sendIcon.setImageResource(mega.privacy.android.core.R.drawable.ic_select_folder);
        } else {
            sendIcon.setImageDrawable(ColorUtils.tintIcon(chatActivity, R.drawable.ic_send_white,
                    ColorUtils.getThemeColor(this, android.R.attr.colorAccent)));
        }

        textChat.setHint(" ");
        setSizeInputText(false);
        sendIcon.setVisibility(View.VISIBLE);
        emojiKeyboard.changeKeyboardIcon();
        currentRecordButtonState = 0;
        recordLayout.setVisibility(View.GONE);
        recordButtonLayout.setVisibility(View.GONE);
    }

    /*
     *Record button deactivated or ready to send
     */
    private void recordButtonDeactivated(boolean isDeactivated) {
        if (textChat != null && textChat.getText() != null && !isTextEmpty(textChat.getText().toString()) && isDeactivated) {
            showSendIcon();
        } else {
            recordButtonLayout.setBackground(null);
            emojiKeyboard.changeKeyboardIcon();
            sendIcon.setVisibility(View.GONE);
            recordButton.setVisibility(View.VISIBLE);

            if (isDeactivated) {
                recordButton.activateOnClickListener(false);
                recordButton.setImageResource(R.drawable.ic_record_voice_clip);
                return;
            }

            recordButton.activateOnTouchListener(false);
            recordButton.activateOnClickListener(true);
            recordButton.setImageDrawable(ColorUtils.tintIcon(chatActivity, R.drawable.ic_send_white,
                    ColorUtils.getThemeColor(this, android.R.attr.colorAccent)));
        }
    }

    /*
     *Update the record button view depending on the state the recording is in
     */
    private void recordButtonStates(int recordButtonState) {
        if (currentRecordButtonState == recordButtonState)
            return;

        currentRecordButtonState = recordButtonState;
        recordLayout.setVisibility(View.VISIBLE);
        recordButtonLayout.setVisibility(View.VISIBLE);

        if (currentRecordButtonState == RECORD_BUTTON_SEND || currentRecordButtonState == RECORD_BUTTON_ACTIVATED) {
            recordView.setVisibility(View.VISIBLE);
            hideChatOptions();
            if (recordButtonState == RECORD_BUTTON_SEND) {
                recordButtonDeactivated(false);
            } else {
                recordButtonLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.recv_bg_mic));
                recordButton.activateOnTouchListener(true);
                recordButton.activateOnClickListener(false);
                recordButton.setImageResource(R.drawable.ic_record_voice_clip);
            }

        } else if (currentRecordButtonState == RECORD_BUTTON_DEACTIVATED) {
            showChatOptions();
            recordView.setVisibility(View.GONE);
            recordButton.activateOnTouchListener(true);
            recordButtonDeactivated(true);
        }
        placeRecordButton(currentRecordButtonState);
    }

    public void showBubble() {
        Timber.d("showBubble");
        recordView.playSound(TYPE_ERROR_RECORD);
        bubbleLayout.setAlpha(1);
        bubbleLayout.setVisibility(View.VISIBLE);
        bubbleLayout.animate().alpha(0).setDuration(DURATION_BUBBLE);
        cancelRecording();
    }

    /**
     * Place the record button
     *
     * @param recordButtonState RECORD_BUTTON_SEND, RECORD_BUTTON_ACTIVATED or RECORD_BUTTON_DEACTIVATED
     */
    public void placeRecordButton(int recordButtonState) {
        recordView.recordButtonTranslation(recordButtonLayout, 0, 0);

        int size = dp2px(48, getOutMetrics());
        int margin = 0;

        if (recordButtonState == RECORD_BUTTON_ACTIVATED) {
            size = dp2px(80, getOutMetrics());
            margin = dp2px(-15, getOutMetrics());
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) recordButtonLayout.getLayoutParams();
        params.addRule(RelativeLayout.ALIGN_PARENT_END);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        params.height = size;
        params.width = size;
        params.setMargins(0, 0, margin, margin);
        recordButtonLayout.setLayoutParams(params);
    }

    public boolean isRecordingNow() {
        return recordView.isRecordingNow();
    }

    /*
     * Know if you're recording right now
     */
    public void setRecordingNow(boolean recordingNow) {
        if (recordView == null) return;

        recordView.setRecordingNow(recordingNow);
        if (recordView.isRecordingNow()) {

            int screenRotation = getWindowManager().getDefaultDisplay().getRotation();
            switch (screenRotation) {
                case ROTATION_PORTRAIT: {
                    lockOrientationPortrait(this);
                    break;
                }
                case ROTATION_LANDSCAPE: {
                    lockOrientationLandscape(this);
                    break;
                }
                case ROTATION_REVERSE_PORTRAIT: {
                    lockOrientationReversePortrait(this);
                }
                case ROTATION_REVERSE_LANDSCAPE: {
                    lockOrientationReverseLandscape(this);
                    break;
                }
                default: {
                    unlockOrientation(this);
                    break;
                }
            }
            if (emojiKeyboard != null) emojiKeyboard.setListenerActivated(false);

            if (!isStartAndRecordVoiceClip) {
                recordButtonStates(RECORD_BUTTON_ACTIVATED);
            }

            return;
        }

        unlockOrientation(this);
        recordButtonStates(RECORD_BUTTON_DEACTIVATED);
        if (emojiKeyboard != null) emojiKeyboard.setListenerActivated(true);
    }

    /**
     * Method to make the correct action in the call of this chat.
     * If a call already exists, I will return to it or join it.
     * If it does not exist, I will initiate a call.
     */
    private void checkCallInThisChat() {
        stopReproductions();
        hideKeyboard();

        if (megaChatApi == null)
            return;

        MegaChatCall callInThisChat = megaChatApi.getChatCall(chatRoom.getChatId());
        if (callInThisChat != null) {
            Timber.d("There is a call in this chat");
            if (participatingInACall()) {
                MegaChatCall currentCallInProgress = getCallInProgress();
                if (callInThisChat.isOnHold() ||
                        (currentCallInProgress != null && currentCallInProgress.getChatid() == chatRoom.getChatId())) {
                    Timber.d("I'm participating in the call of this chat");
                    returnCall(this, chatRoom.getChatId(), passcodeManagement);
                    return;
                }

                Timber.d("I'm participating in another call from another chat");
                ArrayList<Long> numCallsParticipating = getCallsParticipating();
                if (numCallsParticipating == null || numCallsParticipating.isEmpty())
                    return;

                if (numCallsParticipating.size() == 1) {
                    MegaChatCall anotherOnHoldCall = getAnotherOnHoldCall(chatRoom.getChatId());
                    if (anotherOnHoldCall != null) {
                        showJoinCallDialog(chatRoom.getChatId(), anotherOnHoldCall, false);
                    }
                } else {
                    for (int i = 0; i < numCallsParticipating.size(); i++) {
                        MegaChatCall call = megaChatApi.getChatCall(numCallsParticipating.get(i));
                        if (call != null && !call.isOnHold()) {
                            showJoinCallDialog(callInThisChat.getChatid(), call, true);
                        }
                    }
                }

                return;
            }

            if (callInThisChat.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT ||
                    callInThisChat.getStatus() == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION) {
                Timber.d("The call in this chat is In progress, but I do not participate");
                answerCall(chatRoom.getChatId(), startVideo, !chatRoom.isMeeting(), startVideo);
            }
            return;
        }

        if (StorageStateExtensionsKt.getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning();
            return;
        }

        if (!participatingInACall()) {
            Timber.d("There is not a call in this chat and I am NOT in another call");
            shouldCallRing = true;
            startCall();
        }
    }

    /**
     * Start a call
     */
    private void startCall() {
        requestCallPermissions(permissionsRequest);
    }

    private void enableCallMenuItems(Boolean enable) {
        enable = enable && viewModel.shouldEnableCallOption();
        int idColor = enable ? R.color.grey_087_white_087 : R.color.grey_054_white_054;
        callMenuItem.setEnabled(enable);
        callMenuItem.setIcon(mutateIcon(this, R.drawable.ic_phone_white, idColor));
        videoMenuItem.setEnabled(enable);
        videoMenuItem.setIcon(mutateIcon(this, R.drawable.ic_videocam_white, idColor));
    }

    /**
     * Method to control the visibility of the menu item: End call for all
     */
    private void checkEndCallForAllMenuItem() {
        endCallForAllMenuItem.setVisible(false);
        if (chatRoom == null || viewModel.isJoiningOrLeaving() || (!chatRoom.isGroup() && !chatRoom.isMeeting()))
            return;

        Disposable callSubscription = getCallUseCase.isThereACallAndIAmModerator(chatRoom.getChatId())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((shouldBeVisible) -> {
                    if (endCallForAllMenuItem.isVisible() != shouldBeVisible) {
                        endCallForAllMenuItem.setVisible(shouldBeVisible);
                    }

                    if (!shouldBeVisible && endCallForAllDialog != null) {
                        endCallForAllDialog.dismiss();
                    }

                }, (error) -> Timber.e("Error " + error));

        composite.add(callSubscription);
    }

    /**
     * Method to show the End call for all dialog
     */
    private void showEndCallForAllDialog() {

        if (AlertDialogUtil.isAlertDialogShown(endCallForAllDialog))
            return;

        endCallForAllDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.meetings_chat_screen_dialog_title_end_call_for_all))
                .setMessage(getString(R.string.meetings_chat_screen_dialog_description_end_call_for_all))
                .setNegativeButton(R.string.meetings_chat_screen_dialog_negative_button_end_call_for_all, (dialogInterface, i) -> AlertDialogUtil.dismissAlertDialogIfExists(endCallForAllDialog))
                .setPositiveButton(R.string.meetings_chat_screen_dialog_positive_button_end_call_for_all, (dialogInterface, i) -> {
                    AlertDialogUtil.dismissAlertDialogIfExists(endCallForAllDialog);
                    viewModel.endCallForAll();
                })
                .show();
    }

    private boolean checkPermissions(int requestCode, String... permissions) {
        boolean hasPermission = hasPermissions(this, permissions);
        if (!hasPermission) {
            requestPermission(this, requestCode, permissions);
            return false;
        }

        return true;
    }

    private boolean checkPermissionsVoiceClip() {
        Timber.d("checkPermissionsVoiceClip()");
        return checkPermissions(RECORD_VOICE_CLIP, Manifest.permission.RECORD_AUDIO);
    }

    private boolean checkPermissionsTakePicture() {
        Timber.d("checkPermissionsTakePicture");
        return checkPermissions(REQUEST_CAMERA_TAKE_PICTURE, Manifest.permission.CAMERA)
                && checkPermissions(REQUEST_WRITE_STORAGE_TAKE_PICTURE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    private boolean checkPermissionsReadStorage() {
        Timber.d("checkPermissionsReadStorage");
        String[] PERMISSIONS = new String[]{
                PermissionUtils.getImagePermissionByVersion(),
                PermissionUtils.getAudioPermissionByVersion(),
                PermissionUtils.getVideoPermissionByVersion(),
                PermissionUtils.getReadExternalStoragePermission()
        };
        return checkPermissions(REQUEST_READ_STORAGE, PERMISSIONS);
    }

    private boolean checkPermissionWriteStorage(int code) {
        Timber.d("checkPermissionsWriteStorage :%s", code);
        return checkPermissions(code, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Timber.d("onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length == 0) return;

        if (nodeSaver.handleRequestPermissionsResult(requestCode)) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CAMERA_TAKE_PICTURE:
            case REQUEST_WRITE_STORAGE_TAKE_PICTURE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && checkPermissionsTakePicture()) {
                    onTakePictureOptionClicked();
                }
                break;

            case RECORD_VOICE_CLIP:
            case REQUEST_STORAGE_VOICE_CLIP:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && checkPermissionsVoiceClip()) {
                    cancelRecording();
                }
                break;

            case REQUEST_CAMERA_SHOW_PREVIEW:
                if (isBottomSheetDialogShown(bottomSheetDialogFragment) && bottomSheetDialogFragment instanceof ChatRoomToolbarBottomSheetDialogFragment) {
                    ((ChatRoomToolbarBottomSheetDialogFragment) bottomSheetDialogFragment).getViewModel().updatePermissionsGranted(Manifest.permission.CAMERA, grantResults[0] == PackageManager.PERMISSION_GRANTED);
                }
                break;

            case REQUEST_READ_STORAGE:
                if (isBottomSheetDialogShown(bottomSheetDialogFragment) && bottomSheetDialogFragment instanceof ChatRoomToolbarBottomSheetDialogFragment) {
                    ((ChatRoomToolbarBottomSheetDialogFragment) bottomSheetDialogFragment).getViewModel().updatePermissionsGranted(Manifest.permission.READ_EXTERNAL_STORAGE, hasMediaPermissionUseCase.invoke());
                }
                break;

            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED && hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
                    intent.putExtra(EDITING_MESSAGE, editingMessage);
                    if (messageToEdit != null) {
                        intent.putExtra(MSG_ID, messageToEdit.getMsgId());
                    }
                    startActivityForResult(intent, REQUEST_CODE_SEND_LOCATION);
                }
                break;
            }
        }
    }

    public void chooseAddParticipantDialog() {
        Timber.d("chooseAddContactDialog");

        if (megaApi != null && megaApi.getRootNode() != null) {
            ArrayList<MegaUser> contacts = megaApi.getContacts();
            if (contacts == null || contacts.isEmpty() || contacts.stream().noneMatch(it -> it.getVisibility() == MegaUser.VISIBILITY_VISIBLE)) {
                var dialog = AddParticipantsNoContactsDialogFragment.newInstance();
                dialog.show(getSupportFragmentManager(), dialog.getTag());
            } else if (ChatUtil.areAllMyContactsChatParticipants(chatRoom)) {
                var dialog = AddParticipantsNoContactsLeftToAddDialogFragment.newInstance();
                dialog.show(getSupportFragmentManager(), dialog.getTag());
            } else {
                Intent in = new Intent(this, AddContactActivity.class);
                in.putExtra("contactType", CONTACT_TYPE_MEGA);
                in.putExtra("chat", true);
                in.putExtra("chatId", idChat);
                in.putExtra("aBtitle", getString(R.string.add_participants_menu_item));
                startActivityForResult(in, REQUEST_ADD_PARTICIPANTS);
            }
        } else {
            Timber.w("Online but not megaApi");
            showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
        }
    }

    public void chooseContactsDialog() {
        Timber.d("chooseContactsDialog");

        if (megaApi != null && megaApi.getRootNode() != null) {
            ArrayList<MegaUser> contacts = megaApi.getContacts();
            if (contacts == null) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.no_contacts_invite), -1);
            } else {
                if (contacts.isEmpty()) {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.no_contacts_invite), -1);
                } else {
                    Intent in = new Intent(this, AddContactActivity.class);
                    in.putExtra("contactType", CONTACT_TYPE_MEGA);
                    in.putExtra("chat", true);
                    in.putExtra("aBtitle", getString(R.string.send_contacts));
                    startActivityForResult(in, REQUEST_SEND_CONTACTS);
                }
            }
        } else {
            Timber.w("Online but not megaApi");
            showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
        }
    }

    public void disablePinScreen() {
        Timber.d("disablePinScreen");
        passcodeManagement.setShowPasscodeScreen(false);
    }

    public void showProgressForwarding() {
        Timber.d("showProgressForwarding");

        statusDialog = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.general_forwarding));
        statusDialog.show();
    }

    private void stopReproductions() {
        if (adapter != null) {
            adapter.stopAllReproductionsInProgress();
        }
    }

    /**
     * Method to import a node that will later be shared.
     *
     * @param messagesSelected List of messages to be imported and shared.
     * @param listener         The listener to retrieve all the links of the nodes to be exported.
     */
    public void importNodeToShare(ArrayList<AndroidMegaChatMessage> messagesSelected, ExportListener listener) {
        if (viewModel.getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning();
            return;
        }

        this.typeImport = IMPORT_TO_SHARE_OPTION;
        this.exportListener = listener;
        controlStoredUnhandledData(messagesSelected);
    }

    private void controlStoredUnhandledData(ArrayList<AndroidMegaChatMessage> messagesSelected) {
        storedUnhandledData(messagesSelected);
        checkIfIsNeededToAskForMyChatFilesFolder();
    }

    public void forwardMessages(ArrayList<AndroidMegaChatMessage> messagesSelected) {
        if (viewModel.getStorageState() == StorageState.PayWall) {
            showOverDiskQuotaPaywallWarning();
            return;
        }

        //Prevent trigger multiple forwarding messages screens in multiple clicks
        if (isForwardingMessage) {
            Timber.d("Forwarding message is on going");
            return;
        }

        this.typeImport = FORWARD_ONLY_OPTION;
        isForwardingMessage = true;
        controlStoredUnhandledData(messagesSelected);
    }

    public void proceedWithAction() {
        if (typeImport == IMPORT_TO_SHARE_OPTION) {
            stopReproductions();
            chatC.setExportListener(exportListener);
            chatC.prepareMessagesToShare(preservedMessagesSelected, idChat);
        } else if (isForwardingMessage) {
            stopReproductions();
            chatC.prepareAndroidMessagesToForward(preservedMessagesSelected, idChat);
        } else {
            startUploadService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Timber.d("resultCode: %s", resultCode);

        if (nodeSaver.handleActivityResult(this, requestCode, resultCode, intent)) {
            return;
        }

        if (requestCode == REQUEST_ADD_PARTICIPANTS && resultCode == RESULT_OK) {
            if (intent == null) {
                Timber.w("Return.....");
                return;
            }

            final List<String> contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS);
            if (contactsData != null) {
                new InviteToChatRoomListener(this).inviteToChat(chatRoom.getChatId(), contactsData);
            }
        } else if (requestCode == REQUEST_CODE_SELECT_IMPORT_FOLDER && resultCode == RESULT_OK) {
            if (!viewModel.isConnected() || megaApi == null) {
                removeProgressDialog();
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                return;
            }

            final long toHandle = intent.getLongExtra("IMPORT_TO", 0);

            final long[] importMessagesHandles = intent.getLongArrayExtra("HANDLES_IMPORT_CHAT");

            importNodes(toHandle, importMessagesHandles);
        } else if (requestCode == REQUEST_SEND_CONTACTS && resultCode == RESULT_OK) {
            final ArrayList<String> contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS);
            if (contactsData != null) {
                for (int i = 0; i < contactsData.size(); i++) {
                    MegaUser user = megaApi.getContact(contactsData.get(i));
                    if (user != null) {
                        MegaHandleList handleList = MegaHandleList.createInstance();
                        handleList.addMegaHandle(user.getHandle());
                        retryContactAttachment(handleList);
                    }
                }
            }
        } else if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK) {
            nodeAttacher.handleSelectFileResult(intent, idChat, this, this);
        } else if (requestCode == REQUEST_CODE_GET_FILES && resultCode == RESULT_OK) {
            if (intent == null) {
                Timber.w("Return.....");
                return;
            }

            if (intent.getBooleanExtra(FROM_MEGA_APP, false)) {
                nodeAttacher.handleSelectFileResult(intent, idChat, this, this);
                return;
            }

            intent.setAction(Intent.ACTION_GET_CONTENT);
            try {
                statusDialog = MegaProgressDialogUtil.createProgressDialog(this, getResources().getQuantityString(R.plurals.upload_prepare, 1));
                statusDialog.show();
            } catch (Exception e) {
                return;
            }

            internalComposite.add(filePrepareUseCase.prepareFiles(intent)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((shareInfo, throwable) -> {
                        if (throwable == null) {
                            onIntentProcessed(shareInfo);
                        }
                    }));
        } else if (requestCode == REQUEST_CODE_SELECT_CHAT) {
            isForwardingMessage = false;
            if (resultCode != RESULT_OK) return;
            if (!viewModel.isConnected()) {
                removeProgressDialog();

                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), -1);
                return;
            }

            showProgressForwarding();

            long[] idMessages = intent.getLongArrayExtra(ID_MESSAGES);
            if (idMessages != null) Timber.d("Send %d messages", idMessages.length);

            long[] chatHandles = intent.getLongArrayExtra(SELECTED_CHATS);
            if (chatHandles != null) Timber.d("Send to %d chats", chatHandles.length);

            long[] contactHandles = intent.getLongArrayExtra(SELECTED_USERS);
            if (contactHandles != null) Timber.d("Send to %d contacts", contactHandles.length);

            if (idMessages != null) {
                ArrayList<MegaChatRoom> chats = new ArrayList<>();
                ArrayList<MegaUser> users = new ArrayList<>();

                if (contactHandles != null && contactHandles.length > 0) {
                    for (int i = 0; i < contactHandles.length; i++) {
                        MegaUser user = megaApi.getContact(MegaApiAndroid.userHandleToBase64(contactHandles[i]));
                        if (user != null) {
                            users.add(user);
                        }
                    }
                    if (chatHandles != null && chatHandles.length > 0) {
                        for (int i = 0; i < chatHandles.length; i++) {
                            MegaChatRoom chatRoom = megaChatApi.getChatRoom(chatHandles[i]);
                            if (chatRoom != null) {
                                chats.add(chatRoom);
                            }
                        }
                    }
                    CreateChatListener listener = new CreateChatListener(
                            CreateChatListener.SEND_MESSAGES, chats, users, this, this, idMessages,
                            idChat);

                    if (users != null && !users.isEmpty()) {
                        for (MegaUser user : users) {
                            MegaChatPeerList peers = MegaChatPeerList.createInstance();
                            peers.addPeer(user.getHandle(), MegaChatPeerList.PRIV_STANDARD);
                            megaChatApi.createChat(false, peers, listener);
                        }
                    }

                } else if (chatHandles != null && chatHandles.length > 0) {
                    int countChat = chatHandles.length;
                    Timber.d("Selected: %d chats to send", countChat);

                    MultipleForwardChatProcessor forwardChatProcessor = new MultipleForwardChatProcessor(this, chatHandles, idMessages, idChat);
                    forwardChatProcessor.forward(chatRoom);
                } else {
                    Timber.e("Error on sending to chat");
                }
            }
        } else if (requestCode == REQUEST_CODE_SEND_LOCATION && resultCode == RESULT_OK) {
            if (intent == null) {
                return;
            }
            byte[] byteArray = intent.getByteArrayExtra(SNAPSHOT);

            if (byteArray == null) return;
            Bitmap snapshot = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
            String encodedSnapshot = Base64.encodeToString(byteArray, Base64.DEFAULT);
            Timber.d("Info bitmap: %d %d %d", snapshot.getByteCount(), snapshot.getWidth(), snapshot.getHeight());

            float latitude = (float) intent.getDoubleExtra(LATITUDE, 0);
            float longitude = (float) intent.getDoubleExtra(LONGITUDE, 0);
            editingMessage = intent.getBooleanExtra(EDITING_MESSAGE, false);
            if (editingMessage) {
                long msgId = intent.getLongExtra(MSG_ID, -1);
                if (msgId != -1) {
                    messageToEdit = megaChatApi.getMessage(idChat, msgId);
                }
            }

            if (editingMessage && messageToEdit != null) {
                Timber.d("Edit Geolocation - tempId: %d id: %d", messageToEdit.getTempId(), messageToEdit.getMsgId());
                if (messageToEdit.getTempId() != -1) {
                    MegaChatMessage editedMsg = megaChatApi.editGeolocation(idChat, messageToEdit.getTempId(), longitude, latitude, encodedSnapshot);
                    modifyLocationReceived(new AndroidMegaChatMessage(editedMsg), true);
                } else if (messageToEdit.getMsgId() != -1) {
                    MegaChatMessage editedMsg = megaChatApi.editGeolocation(idChat, messageToEdit.getMsgId(), longitude, latitude, encodedSnapshot);
                    modifyLocationReceived(new AndroidMegaChatMessage(editedMsg), false);
                }
                editingMessage = false;
                messageToEdit = null;
            } else {
                Timber.d("Send location [longLatitude]: %s [longLongitude]: %s", latitude, longitude);
                sendLocationMessage(longitude, latitude, encodedSnapshot);
            }
        } else {
            Timber.e("Error onActivityResult");
        }

        super.onActivityResult(requestCode, resultCode, intent);
    }

    public void importNodes(final long toHandle, final long[] importMessagesHandles) {
        Timber.d("importNode: %d -> %d", toHandle, importMessagesHandles.length);
        statusDialog = MegaProgressDialogUtil.createProgressDialog(this, getString(R.string.general_importing));
        statusDialog.show();

        internalComposite.add(checkNameCollisionUseCase.checkMessagesToImport(importMessagesHandles, idChat, toHandle)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((result, throwable) -> {
                    if (throwable == null) {
                        ArrayList<NameCollision> collisions = result.getFirst();

                        if (!collisions.isEmpty()) {
                            dismissAlertDialogIfExists(statusDialog);
                            nameCollisionActivityContract.launch(collisions);
                        }

                        List<MegaNode> nodesWithoutCollision = result.getSecond();

                        if (!nodesWithoutCollision.isEmpty()) {
                            internalComposite.add(legacyCopyNodeUseCase.copy(nodesWithoutCollision, toHandle)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe((copyResult, copyThrowable) -> {
                                        dismissAlertDialogIfExists(statusDialog);

                                        if (copyThrowable != null) {
                                            manageCopyMoveException(copyThrowable);
                                        }

                                        showSnackbar(SNACKBAR_TYPE, copyThrowable == null
                                                        ? copyRequestMessageMapper.invoke(copyResult)
                                                        : getString(R.string.import_success_error),
                                                MEGACHAT_INVALID_HANDLE);
                                    }));
                        }
                    } else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.import_success_error), MEGACHAT_INVALID_HANDLE);
                    }
                }));
    }

    public void retryNodeAttachment(long nodeHandle) {
        megaChatApi.attachNode(idChat, nodeHandle, this);
    }

    public void retryContactAttachment(MegaHandleList handleList) {
        Timber.d("retryContactAttachment");
        MegaChatMessage contactMessage = megaChatApi.attachContacts(idChat, handleList);
        if (contactMessage != null) {
            AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(contactMessage);
            sendMessageToUI(androidMsgSent);
        }
    }

    /**
     * Retries the sending of a pending message.
     *
     * @param idMessage The identifier of the pending message.
     */
    public void retryPendingMessage(long idMessage) {
        retryPendingMessage(dbH.findPendingMessageById(idMessage));
    }

    /**
     * Retries the sending of a pending message.
     *
     * @param pendMsg The pending message.
     */
    private void retryPendingMessage(PendingMessage pendMsg) {
        if (pendMsg == null) {
            Timber.e("Pending message does not exist");
            showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.messages_forwarded_error_not_available,
                    1, 1), MEGACHAT_INVALID_HANDLE);
            return;
        }

        if (pendMsg.getNodeHandle() != INVALID_HANDLE) {
            removePendingMsg(pendMsg);
            retryNodeAttachment(pendMsg.getNodeHandle());
        } else {
            // Retry to send
            File f = new File(pendMsg.getFilePath());
            if (!f.exists()) {
                showSnackbar(SNACKBAR_TYPE, getResources().getQuantityString(R.plurals.messages_forwarded_error_not_available,
                        1, 1), MEGACHAT_INVALID_HANDLE);
                return;
            }

            // Remove the old message from the UI and DB
            removePendingMsg(pendMsg);
            PendingMessage pMsgSingle = createAttachmentPendingMessage(idChat,
                    f.getAbsolutePath(), f.getName(), false);

            long idMessageDb = pMsgSingle.getId();
            if (idMessageDb == INVALID_ID) {
                Timber.e("Error when adding pending msg to the database");
                return;
            }

            if (!isLoadingHistory) {
                sendMessageToUI(new AndroidMegaChatMessage(pMsgSingle, true));
            }

            checkIfServiceCanStart(new Intent(this, ChatUploadService.class)
                    .putExtra(ChatUploadService.EXTRA_ID_PEND_MSG, idMessageDb)
                    .putExtra(ChatUploadService.EXTRA_CHAT_ID, idChat));
        }
    }

    /**
     * Method to update the input text size
     *
     * @param isEmpty True, if text is empty. False, otherwise
     */
    private void setSizeInputText(boolean isEmpty) {
        textChat.setMinLines(1);
        if (isEmpty) {
            textChat.setMaxLines(1);
        } else {
            int maxLines;
            if (textChat.getMaxLines() < (isInputTextExpanded ? MAX_LINES_INPUT_TEXT_EXPANDED : MAX_LINES_INPUT_TEXT_COLLAPSED) &&
                    textChat.getLineCount() == textChat.getMaxLines()) {
                maxLines = textChat.getLineCount() + 1;
            } else {
                maxLines = isInputTextExpanded ? MAX_LINES_INPUT_TEXT_EXPANDED : MAX_LINES_INPUT_TEXT_COLLAPSED;
            }
            textChat.setEllipsize(null);
            textChat.setMaxLines(maxLines);
        }
    }

    /**
     * Method to hang chat call
     *
     * @param callId The call ID
     */
    private void endCall(long callId) {
        if (megaChatApi != null) {
            megaChatApi.hangChatCall(callId, new HangChatCallListener(this, this));
        }
    }

    /**
     * Dialogue to allow you to end or stay on a group call or meeting when you are left alone on the call
     */
    private void showOnlyMeInTheCallDialog() {
        if (AlertDialogUtil.isAlertDialogShown(dialogOnlyMeInCall)) {
            return;
        }

        boolean isRequestSent = false;
        if (chatRoom != null) {
            MegaChatCall call = megaChatApi.getChatCall(chatRoom.getChatId());
            if (call != null) {
                isRequestSent = MegaApplication.getChatManagement().isRequestSent(call.getCallId());
            }
        }

        dialogOnlyMeInCall = new MaterialAlertDialogBuilder(this)
                .setTitle(isRequestSent ?
                        getString(R.string.calls_call_screen_dialog_title_only_you_in_the_call) :
                        getString(R.string.calls_chat_screen_dialog_title_only_you_in_the_call))
                .setMessage(getString(R.string.calls_call_screen_dialog_description_only_you_in_the_call))
                .setPositiveButton(getString(R.string.calls_call_screen_button_to_end_call), (dialog, which) -> {
                    viewModel.checkEndCall();
                    hideDialogCall();
                })
                .setNegativeButton(getString(R.string.calls_call_screen_button_to_stay_alone_in_call), (dialog, which) -> {
                    viewModel.checkStayOnCall();
                    hideDialogCall();
                })
                .setOnDismissListener(dialog -> isOnlyMeInCallDialogShown = false)
                .setCancelable(false)
                .create();

        dialogOnlyMeInCall.show();
    }

    /**
     * Hide dialog related to calls
     */
    private void hideDialogCall() {
        if (AlertDialogUtil.isAlertDialogShown(dialogOnlyMeInCall)) {
            dialogOnlyMeInCall.dismiss();
        }
    }

    /**
     * Dialog to allow joining a group call when another one is active.
     *
     * @param callInThisChat        The chat ID of the group call.
     * @param anotherCall           The in progress call.
     * @param existsMoreThanOneCall If
     */
    public void showJoinCallDialog(long callInThisChat, MegaChatCall anotherCall, boolean existsMoreThanOneCall) {
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.join_call_dialog, null);
        TextView joinCallDialogTitle = dialogLayout.findViewById(R.id.title_call_dialog);
        joinCallDialogTitle.setText(chatRoom.isGroup() ? R.string.title_join_call : R.string.title_join_one_to_one_call);
        joinCallDialogTitle.setVisibility(View.VISIBLE);
        TextView description = dialogLayout.findViewById(R.id.description_call_dialog);
        description.setText(getString(R.string.text_join_another_call));
        description.setVisibility(View.VISIBLE);

        final Button holdJoinButton = dialogLayout.findViewById(R.id.first_button);
        final Button endJoinButton = dialogLayout.findViewById(R.id.second_button);
        final Button cancelButton = dialogLayout.findViewById(R.id.cancel_button);

        holdJoinButton.setText(chatRoom.isGroup() ? R.string.hold_and_join_call_incoming : R.string.hold_and_answer_call_incoming);
        endJoinButton.setText(chatRoom.isGroup() ? R.string.end_and_join_call_incoming : R.string.end_and_answer_call_incoming);
        holdJoinButton.setVisibility(existsMoreThanOneCall ? View.GONE : View.VISIBLE);
        endJoinButton.setVisibility(View.VISIBLE);
        cancelButton.setVisibility(View.VISIBLE);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
        builder.setView(dialogLayout);
        dialogCall = builder.create();
        isJoinCallDialogShown = true;
        dialogCall.show();

        View.OnClickListener clickListener = v -> {
            int id = v.getId();
            if (id == R.id.first_button) {
                if (anotherCall.isOnHold()) {
                    MegaChatCall callInChat = megaChatApi.getChatCall(callInThisChat);
                    if (callInChat != null && (callInChat.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT ||
                            callInChat.getStatus() == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION)) {
                        answerCall(callInThisChat, false, true, false);
                    }
                } else {
                    megaChatApi.setCallOnHold(anotherCall.getChatid(), true, new SetCallOnHoldListener(this, this, this));
                }
            } else if (id == R.id.second_button) {
                endCall(anotherCall.getCallId());
            } else if (id == R.id.cancel_button) {
            }
            if (dialogCall != null) {
                dialogCall.dismiss();
            }
        };

        dialogCall.setOnDismissListener(dialog -> isJoinCallDialogShown = false);
        holdJoinButton.setOnClickListener(clickListener);
        endJoinButton.setOnClickListener(clickListener);
        cancelButton.setOnClickListener(clickListener);
    }

    public void controlCamera() {
        stopReproductions();
        openCameraApp();
    }

    @Override
    public void onBackPressed() {
        Timber.d("onBackPressed");
        PsaWebBrowser psaWebBrowser = getPsaWebBrowser();
        if (psaWebBrowser != null && psaWebBrowser.consumeBack()) return;

        retryConnectionsAndSignalPresence();
        if (emojiKeyboard != null && emojiKeyboard.getEmojiKeyboardShown()) {
            emojiKeyboard.hideBothKeyboard(this);
        } else {
            if (handlerEmojiKeyboard != null) {
                handlerEmojiKeyboard.removeCallbacksAndMessages(null);
            }
            if (handlerKeyboard != null) {
                handlerKeyboard.removeCallbacksAndMessages(null);
            }
            ifAnonymousModeLogin(false);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        Timber.d("onClick");

        if (viewModel.isJoiningOrLeaving() && v.getId() != androidx.appcompat.R.id.home)
            return;
        MegaChatCall callInThisChat;
        int id = v.getId();
        if (id == androidx.appcompat.R.id.home) {
            onBackPressed();
        } else if (id == R.id.call_on_hold_layout) {
            callInThisChat = megaChatApi.getChatCall(chatRoom.getChatId());
            if (callInThisChat == null)
                return;

            if (callInThisChat.getStatus() == MegaChatCall.CALL_STATUS_IN_PROGRESS) {
                if (callInThisChat.isOnHold()) {
                    returnCall(this, chatRoom.getChatId(), passcodeManagement);
                }

            } else {
                ArrayList<Long> numCallsParticipating = getCallsParticipating();
                if (numCallsParticipating == null || numCallsParticipating.isEmpty())
                    return;

                if (numCallsParticipating.size() == 1) {
                    MegaChatCall anotherCall = getAnotherActiveCall(chatRoom.getChatId());
                    if (anotherCall == null) {
                        anotherCall = getAnotherOnHoldCall(chatRoom.getChatId());
                    }
                    if (anotherCall != null) {
                        showJoinCallDialog(callInThisChat.getChatid(), anotherCall, false);
                    }
                } else {
                    for (int i = 0; i < numCallsParticipating.size(); i++) {
                        MegaChatCall call = megaChatApi.getChatCall(numCallsParticipating.get(i));
                        if (call != null && !call.isOnHold()) {
                            showJoinCallDialog(callInThisChat.getChatid(), call, true);
                        }
                    }
                }
            }
        } else if (id == R.id.call_in_progress_layout) {
            if (chatIdBanner == MEGACHAT_INVALID_HANDLE)
                return;

            MegaChatCall callBanner = megaChatApi.getChatCall(chatIdBanner);

            if (chatRoom != null && chatRoom.isActive() && !chatRoom.isArchived() && chatRoom.isMeeting() &&
                    callBanner != null && callBanner.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT) {
                startVideo = false;
                shouldCallRing = false;
                startCall();
            } else {
                if (!checkIfCanJoinOneToOneCall(chatIdBanner)) {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.call_error_too_many_participants), MEGACHAT_INVALID_HANDLE);
                    return;
                }

                if (callBanner == null || callBanner.getStatus() == MegaChatCall.CALL_STATUS_USER_NO_PRESENT ||
                        callBanner.getStatus() == MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION) {
                    startVideo = false;
                    checkCallInThisChat();
                } else {
                    returnCall(this, chatIdBanner, passcodeManagement);
                }
            }
        } else if (id == R.id.expand_input_text_rl || id == R.id.expand_input_text_icon) {
            isInputTextExpanded = !isInputTextExpanded;
            checkExpandOrCollapseInputText();
        } else if (id == R.id.send_icon) {
            String text = textChat.getText().toString();
            if (text.trim().isEmpty()) return;
            if (editingMessage) {
                editMessage(text);
                finishMultiselectionMode();
                checkActionMode();
                hideEditMsgLayout();
            } else {
                sendMessage(text);
            }
            textChat.setText("", TextView.BufferType.EDITABLE);
            controlExpandableInputText(1);
        } else if (id == R.id.more_options_rl) {
            showChatRoomToolbarByPanel();
        } else if (id == R.id.cancel_edit) {
            editingMessage = false;
            messageToEdit = null;
            hideEditMsgLayout();
            textChat.setText(null);
            textChat.post(() -> {
                controlExpandableInputText(textChat.getLineCount());
            });
            showSendIcon();
            refreshTextInput();
        } else if (id == R.id.emoji_rl || id == R.id.emoji_icon) {
            Timber.d("Emoji icon clicked");
            if (emojiKeyboard == null) return;
            changeKeyboard();
        } else if (id == R.id.toolbar_chat) {
            Timber.d("toolbar_chat");
            if (recordView.isRecordingNow()) return;

            showGroupOrContactInfoActivity();
        } else if (id == R.id.new_messages_icon) {
            goToEnd();
        } else if (id == R.id.join_button) {
            if (chatC.isInAnonymousMode()) {
                ifAnonymousModeLogin(true);
            } else if (!isAlreadyJoining(idChat)) {
                addJoiningChatId(idChat);
                setJoiningOrLeaving(R.string.joining_label);
                megaChatApi.autojoinPublicChat(idChat, this);
            }
        } else if (id == R.id.start_or_join_meeting_banner) {
            startVideo = false;
            shouldCallRing = false;
            startCall();
        }
    }

    /**
     * Method to show the letter keyboard or emoji keyboard
     */
    private void changeKeyboard() {
        Drawable currentDrawable = keyboardTwemojiButton.getDrawable();
        Drawable emojiDrawableLight = AppCompatResources.getDrawable(this, R.drawable.ic_emoji_unchecked);
        Drawable emojiDrawableDark = AppCompatResources.getDrawable(this, R.drawable.ic_emoji_checked);
        Drawable keyboardDrawable = AppCompatResources.getDrawable(this, R.drawable.ic_keyboard_white);
        if ((areDrawablesIdentical(currentDrawable, emojiDrawableLight) ||
                areDrawablesIdentical(currentDrawable, emojiDrawableDark)) &&
                !emojiKeyboard.getEmojiKeyboardShown()) {
            if (emojiKeyboard.getLetterKeyboardShown()) {
                emojiKeyboard.hideLetterKeyboard();
                handlerKeyboard.postDelayed(() -> {
                    emojiKeyboard.showEmojiKeyboard();
                }, 250);
            } else {
                emojiKeyboard.showEmojiKeyboard();
            }
        } else if (areDrawablesIdentical(currentDrawable, keyboardDrawable) && !emojiKeyboard.getLetterKeyboardShown()) {
            showLetterKB();
        }
    }

    public void sendFromCloud() {
        attachFromCloud();
    }

    public void sendFromFileSystem() {
        attachPhotoVideo();
    }

    void getLocationPermission() {
        if (!hasPermissions(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION);
        } else {
            Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
            intent.putExtra(EDITING_MESSAGE, editingMessage);
            if (messageToEdit != null) {
                intent.putExtra(MSG_ID, messageToEdit.getMsgId());
            }
            startActivityForResult(intent, REQUEST_CODE_SEND_LOCATION);
        }
    }

    void showSendLocationDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.title_activity_maps)
                .setMessage(R.string.explanation_send_location)
                .setPositiveButton(getString(R.string.button_continue),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                megaApi.enableGeolocation(chatActivity);
                            }
                        })
                .setNegativeButton(R.string.general_cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                try {
                                    locationDialog.dismiss();
                                    isLocationDialogShown = false;
                                } catch (Exception e) {
                                }
                            }
                        });

        locationDialog = builder.create();
        locationDialog.setCancelable(false);
        locationDialog.setCanceledOnTouchOutside(false);
        locationDialog.show();
        isLocationDialogShown = true;
    }

    public void attachFromCloud() {
        Timber.d("attachFromCloud");
        if (megaApi != null && megaApi.getRootNode() != null) {
            Intent intent = new Intent(this, FileExplorerActivity.class);
            intent.setAction(FileExplorerActivity.ACTION_MULTISELECT_FILE);
            startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
        } else {
            Timber.w("Online but not megaApi");
            showErrorAlertDialog(getString(R.string.error_server_connection_problem), false, this);
        }
    }

    public void attachPhotoVideo() {
        Timber.d("attachPhotoVideo");

        disablePinScreen();

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.putExtra(FROM_MEGA_APP, true);
        intent.setType("*/*");

        startActivityForResult(Intent.createChooser(intent, null), REQUEST_CODE_GET_FILES);
    }

    public void sendMessage(String text) {
        Timber.d("sendMessage: ");
        MegaChatMessage msgSent = megaChatApi.sendMessage(idChat, text);
        AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(msgSent);
        sendMessageToUI(androidMsgSent);
    }

    public void sendLocationMessage(float longLongitude, float longLatitude, String encodedSnapshot) {
        Timber.d("sendLocationMessage");
        MegaChatMessage locationMessage = megaChatApi.sendGeolocation(idChat, longLongitude, longLatitude, encodedSnapshot);
        if (locationMessage == null) return;
        AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(locationMessage);
        sendMessageToUI(androidMsgSent);
    }

    /**
     * Sends a Giphy message from MegaChatGiphy object.
     *
     * @param giphy Giphy to send.
     */
    public void sendGiphyMessageFromMegaChatGiphy(MegaChatGiphy giphy) {
        if (giphy == null) {
            Timber.w("MegaChatGiphy is null");
            return;
        }
        sendGiphyMessage(giphy.getMp4Src(), giphy.getWebpSrc(), giphy.getMp4Size(), giphy.getWebpSize(),
                giphy.getWidth(), giphy.getHeight(), giphy.getTitle());

    }

    /**
     * Sends a Giphy message from GifData object.
     *
     * @param gifData Giphy to send.
     */
    public void sendGiphyMessageFromGifData(GifData gifData) {
        if (gifData == null) {
            Timber.w("GifData is null");
            return;
        }

        sendGiphyMessage(gifData.getMp4Url(), gifData.getWebpUrl(), gifData.getMp4Size(), gifData.getWebpSize(),
                gifData.getWidth(), gifData.getHeight(), gifData.getTitle());
    }

    /**
     * Sends a Giphy message.
     *
     * @param srcMp4   Source location of the mp4
     * @param srcWebp  Source location of the webp
     * @param sizeMp4  Size in bytes of the mp4
     * @param sizeWebp Size in bytes of the webp
     * @param width    Width of the giphy
     * @param height   Height of the giphy
     * @param title    Title of the giphy
     */
    public void sendGiphyMessage(String srcMp4, String srcWebp, long sizeMp4, long sizeWebp, int width, int height, String title) {
        MegaChatMessage giphyMessage = megaChatApi.sendGiphy(idChat, getGiphySrc(srcMp4), getGiphySrc(srcWebp),
                sizeMp4, sizeWebp, width, height, title);
        if (giphyMessage == null) return;

        AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(giphyMessage);
        sendMessageToUI(androidMsgSent);
    }

    public void hideNewMessagesLayout() {
        Timber.d("hideNewMessagesLayout");

        int position = positionNewMessagesLayout;

        positionNewMessagesLayout = INVALID_VALUE;
        lastIdMsgSeen = MEGACHAT_INVALID_HANDLE;
        generalUnreadCount = 0;
        lastSeenReceived = true;

        if (adapter != null) {
            adapter.notifyItemChanged(position);
        }
    }

    public void openCameraApp() {
        Timber.d("openCameraApp()");
        if (checkPermissionsTakePicture()) {
            onTakePictureOptionClicked();
        }
    }

    public void sendMessageToUI(AndroidMegaChatMessage androidMsgSent) {
        Timber.d("sendMessageToUI");

        if (positionNewMessagesLayout != -1) {
            hideNewMessagesLayout();
        }

        int infoToShow = -1;

        int index = messages.size() - 1;
        if (androidMsgSent != null) {
            if (androidMsgSent.isUploading()) {
                Timber.d("Is uploading: ");

            } else if (androidMsgSent.getMessage() != null) {
                Timber.d("Sent message with id temp: %s", androidMsgSent.getMessage().getTempId());
                Timber.d("State of the message: %s", androidMsgSent.getMessage().getStatus());
            }

            if (index == -1) {
                //First element
                Timber.d("First element!");
                messages.add(androidMsgSent);
                messages.get(0).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
            } else {
                //Not first element - Find where to add in the queue
                Timber.d("NOT First element!");

                AndroidMegaChatMessage msg = messages.get(index);
                if (!androidMsgSent.isUploading()) {
                    while (msg.isUploading()) {
                        index--;
                        if (index == -1) {
                            break;
                        }
                        msg = messages.get(index);
                    }
                }

                while (!msg.isUploading() && msg.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL) {
                    index--;
                    if (index == -1) {
                        break;
                    }
                    msg = messages.get(index);
                }

                index++;
                Timber.d("Add in position: %s", index);
                messages.add(index, androidMsgSent);
                infoToShow = adjustInfoToShow(index);
            }

            if (adapter == null) {
                createAdapter();
            } else {
                //Increment header position
                index++;
                adapter.addMessage(messages, index);
                mLayoutManager.scrollToPositionWithOffset(index, scaleHeightPx(infoToShow == AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL ? 50 : 20, getOutMetrics()));
            }
        } else {
            Timber.e("Error sending message!");
        }
    }

    /**
     * Method for copying a message.
     *
     * @param message The message.
     * @return The copied text.
     */
    public String copyMessage(AndroidMegaChatMessage message) {
        return chatC.createSingleManagementString(message, chatRoom);
    }

    /**
     * Method for copying a text to the clipboard.
     *
     * @param text The text.
     */
    public void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
        clipboard.setPrimaryClip(clip);
        showSnackbar(SNACKBAR_TYPE, getString(R.string.messages_copied_clipboard), -1);
    }


    public void editMessage(ArrayList<AndroidMegaChatMessage> messagesSelected) {
        if (messagesSelected.isEmpty() || messagesSelected.get(0) == null)
            return;

        editingMessage = true;
        MegaChatMessage msg = messagesSelected.get(0).getMessage();
        MegaChatContainsMeta meta = msg.getContainsMeta();
        messageToEdit = msg;

        if (msg.getType() == MegaChatMessage.TYPE_CONTAINS_META && meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION) {
            onSendLocationOptionClicked();
            finishMultiselectionMode();
            checkActionMode();
        } else {
            editMsgUI(messageToEdit.getContent());
        }
    }

    /**
     * Method to hide the UI for editing a message
     */
    private void hideEditMsgLayout() {
        editMsgLayout.setVisibility(View.GONE);
        cancelEdit.setVisibility(View.GONE);
    }

    /**
     * Method to show the UI for editing a message
     *
     * @param written text to be edited
     */
    private void editMsgUI(String written) {
        editMsgLayout.setVisibility(View.VISIBLE);
        cancelEdit.setVisibility(View.VISIBLE);
        editMsgText.setText(written);
        textChat.setText(written);
        textChat.setSelection(textChat.getText().length());
        textChat.post(() -> {
            controlExpandableInputText(textChat.getLineCount());
        });
        setSizeInputText(false);
        showSendIcon();

        handlerKeyboard.postDelayed(this::showLetterKB, 250);
        checkExpandOrCollapseInputText();
    }

    public void editMessage(String text) {
        if (messageToEdit.getContent().equals(text)) return;
        MegaChatMessage msgEdited = megaChatApi.editMessage(idChat,
                messageToEdit.getMsgId() != MEGACHAT_INVALID_HANDLE ? messageToEdit.getMsgId() : messageToEdit.getTempId(),
                text);

        if (msgEdited != null) {
            Timber.d("Edited message: status: %s", msgEdited.getStatus());
            AndroidMegaChatMessage androidMsgEdited = new AndroidMegaChatMessage(msgEdited);
            modifyMessageReceived(androidMsgEdited, false);
        } else {
            Timber.w("Message cannot be edited!");
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_editing_message), MEGACHAT_INVALID_HANDLE);
        }
    }

    public void editMessageMS(String text, MegaChatMessage messageToEdit) {
        Timber.d("editMessageMS: ");
        MegaChatMessage msgEdited = null;

        if (messageToEdit.getMsgId() != -1) {
            msgEdited = megaChatApi.editMessage(idChat, messageToEdit.getMsgId(), text);
        } else {
            msgEdited = megaChatApi.editMessage(idChat, messageToEdit.getTempId(), text);
        }

        if (msgEdited != null) {
            Timber.d("Edited message: status: %s", msgEdited.getStatus());
            AndroidMegaChatMessage androidMsgEdited = new AndroidMegaChatMessage(msgEdited);
            modifyMessageReceived(androidMsgEdited, false);
        } else {
            Timber.w("Message cannot be edited!");
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_editing_message), -1);
        }
    }

    public void activateActionMode() {
        if (!adapter.isMultipleSelect()) {
            adapter.setMultipleSelect(true);
            actionMode = startSupportActionMode(new ActionBarCallBack());
            updateActionModeTitle();
        }
    }

    private void reDoTheSelectionAfterRotation() {
        if (recoveredSelectedPositions == null || adapter == null)
            return;

        if (recoveredSelectedPositions.size() > 0) {
            activateActionMode();

            for (int position : recoveredSelectedPositions) {
                AndroidMegaChatMessage msg = adapter.getMessageAtMessagesPosition(position);
                if (msg != null) {
                    adapter.toggleSelection(msg.getMessage().getMsgId());
                }
            }
        }

        updateActionModeTitle();
    }

    public void activateActionModeWithItem(int positionInAdapter) {
        Timber.d("activateActionModeWithItem");

        activateActionMode();
        if (adapter.isMultipleSelect()) {
            itemClick((positionInAdapter + 1), null);
        }
    }

    //Multiselect
    private class ActionBarCallBack implements ActionMode.Callback {

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            ArrayList<AndroidMegaChatMessage> messagesSelected = adapter.getSelectedMessages();

            if (viewModel.getStorageState() == StorageState.PayWall) {
                showOverDiskQuotaPaywallWarning();
                return false;
            }
            finishMultiselectionMode();

            int itemId = item.getItemId();
            if (itemId == R.id.chat_cab_menu_edit) {
                editMessage(messagesSelected);
            } else if (itemId == R.id.chat_cab_menu_share) {
                Timber.d("Share option");
                if (!messagesSelected.isEmpty()) {
                    if (messagesSelected.size() == 1) {
                        shareMsgFromChat(chatActivity, messagesSelected.get(0), idChat);
                    } else {
                        shareNodesFromChat(chatActivity, messagesSelected, idChat);
                    }
                }
            } else if (itemId == R.id.chat_cab_menu_invite) {
                ContactController cC = new ContactController(chatActivity);
                if (messagesSelected.size() == 1) {
                    cC.inviteContact(messagesSelected.get(0).getMessage().getUserEmail(0));
                } else {
                    ArrayList<String> contactEmails = new ArrayList<>();
                    for (AndroidMegaChatMessage message : messagesSelected) {
                        contactEmails.add(message.getMessage().getUserEmail(0));
                    }
                    cC.inviteMultipleContacts(contactEmails);
                }
            } else if (itemId == R.id.chat_cab_menu_start_conversation) {
                if (messagesSelected.size() == 1) {
                    startConversation(messagesSelected.get(0).getMessage().getUserHandle(0));
                } else {
                    ArrayList<Long> contactHandles = new ArrayList<>();
                    for (AndroidMegaChatMessage message : messagesSelected) {
                        contactHandles.add(message.getMessage().getUserHandle(0));
                    }
                    startGroupConversation(contactHandles);
                }
            } else if (itemId == R.id.chat_cab_menu_forward) {
                Timber.d("Forward message");
                forwardMessages(messagesSelected);
            } else if (itemId == R.id.chat_cab_menu_copy) {
                String text;
                if (messagesSelected.size() == 1) {
                    MegaChatMessage msg = messagesSelected.get(0).getMessage();
                    text = isGeolocation(msg) ? msg.getContainsMeta().getTextMessage() : copyMessage(messagesSelected.get(0));
                } else {
                    text = copyMessages(messagesSelected);
                }
                copyToClipboard(text);
            } else if (itemId == R.id.chat_cab_menu_delete) {//Delete
                showConfirmationDeleteMessages(messagesSelected, chatRoom);
            } else if (itemId == R.id.chat_cab_menu_download) {
                Timber.d("chat_cab_menu_download ");
                ArrayList<MegaNodeList> list = new ArrayList<>();
                for (int i = 0; i < messagesSelected.size(); i++) {
                    MegaNodeList megaNodeList = messagesSelected.get(i).getMessage().getMegaNodeList();
                    list.add(megaNodeList);
                }
                PermissionUtils.checkNotificationsPermission(chatActivity);
                nodeSaver.saveNodeLists(list, false, false, false, true);
            } else if (itemId == R.id.chat_cab_menu_import) {
                finishMultiselectionMode();
                chatC.importNodesFromAndroidMessages(messagesSelected, IMPORT_ONLY_OPTION);
            } else if (itemId == R.id.chat_cab_menu_offline) {
                PermissionUtils.checkNotificationsPermission(chatActivity);
                finishMultiselectionMode();
                chatC.saveForOfflineWithAndroidMessages(messagesSelected, chatRoom,
                        ChatActivity.this);
            }
            return false;
        }

        public String copyMessages(ArrayList<AndroidMegaChatMessage> messagesSelected) {
            Timber.d("copyMessages");
            ChatController chatC = new ChatController(chatActivity);
            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < messagesSelected.size(); i++) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append(chatC.createSingleManagementString(messagesSelected.get(i), chatRoom));
            }
            return builder.toString();
        }


        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            Timber.d("onCreateActionMode");
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.messages_chat_action, menu);

            importIcon = menu.findItem(R.id.chat_cab_menu_import);
            editIcon = menu.findItem(R.id.chat_cab_menu_edit);
            copyIcon = menu.findItem(R.id.chat_cab_menu_copy);
            deleteIcon = menu.findItem(R.id.chat_cab_menu_delete);
            forwardIcon = menu.findItem(R.id.chat_cab_menu_forward);
            downloadIcon = menu.findItem(R.id.chat_cab_menu_download);
            shareIcon = menu.findItem(R.id.chat_cab_menu_share);
            inviteIcon = menu.findItem(R.id.chat_cab_menu_invite);
            startConversationIcon = menu.findItem(R.id.chat_cab_menu_start_conversation);

            ColorUtils.changeStatusBarColorForElevation(ChatActivity.this, true);
            // No App bar in this activity, control tool bar instead.
            tB.setElevation(getResources().getDimension(R.dimen.toolbar_elevation));

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode arg0) {
            Timber.d("onDestroyActionMode");
            adapter.setMultipleSelect(false);
            editingMessage = false;
            recoveredSelectedPositions = null;
            clearSelections();

            // No App bar in this activity, control tool bar instead.
            boolean withElevation = listView.canScrollVertically(-1);
            ColorUtils.changeStatusBarColorForElevation(ChatActivity.this, withElevation);
            if (!withElevation) {
                tB.setElevation(0);
            }
        }

        /**
         * Show appropriate options when the node is available
         */
        private void showOptionsForAvailableNode() {
            shareIcon.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            downloadIcon.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            importIcon.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            shareIcon.setVisible(true);
            downloadIcon.setVisible(true);
            importIcon.setVisible(true);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            List<AndroidMegaChatMessage> selected = adapter.getSelectedMessages();
            forwardIcon.setVisible(false);
            shareIcon.setVisible(false);
            importIcon.setVisible(false);
            downloadIcon.setVisible(false);
            inviteIcon.setVisible(false);
            startConversationIcon.setVisible(false);
            editIcon.setVisible(false);
            copyIcon.setVisible(false);
            deleteIcon.setVisible(false);

            if (selected.isEmpty())
                return false;

            if ((chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RM || chatRoom.getOwnPrivilege() == MegaChatRoom.PRIV_RO) && !chatRoom.isPreview()) {
                Timber.d("Chat without permissions || without preview");
                boolean showCopy = true;
                for (int i = 0; i < selected.size(); i++) {
                    MegaChatMessage msg = selected.get(i).getMessage();
                    if (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT
                            || msg.getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT
                            || msg.getType() == MegaChatMessage.TYPE_VOICE_CLIP
                            || (msg.getType() == MegaChatMessage.TYPE_CONTAINS_META && msg.getContainsMeta() != null
                            && msg.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GIPHY)) {
                        showCopy = false;
                        break;
                    }
                }

                copyIcon.setVisible(showCopy);
                return false;
            }

            Timber.d("Chat with permissions or preview");
            if (selected.size() == 1) {
                Timber.d("One message selected");
                MegaChatMessage chatMessage = selected.get(0).getMessage();
                if (selected.get(0).isUploading() || chatMessage == null) {
                    Timber.d("Message is uploading or null");
                    return false;
                }

                int typeMessage = chatMessage.getType();

                boolean isMyOwnMsg = chatMessage.getUserHandle() == myUserHandle;

                boolean isRemovedMsg = ChatUtil.isMsgRemovedOrHasRejectedOrManualSendingStatus(removedMessages, chatMessage);
                boolean shouldForwardOptionVisible = !isRemovedMsg && viewModel.isConnected() && !chatC.isInAnonymousMode();
                forwardIcon.setVisible(shouldForwardOptionVisible);

                boolean shouldDeleteOptionVisible = !isRemovedMsg && isMyOwnMsg &&
                        chatMessage.isDeletable();
                deleteIcon.setVisible(shouldDeleteOptionVisible);

                switch (typeMessage) {
                    case MegaChatMessage.TYPE_NODE_ATTACHMENT:
                        MegaNodeList nodeList = chatMessage.getMegaNodeList();
                        boolean isOnlineNotAnonymousAndNotRemoved = viewModel.isConnected() && !chatC.isInAnonymousMode() && !isRemovedMsg;
                        if (nodeList != null && nodeList.size() > 0 && isOnlineNotAnonymousAndNotRemoved) {
                            if (isMyOwnMsg) {
                                internalComposite.add(getNodeUseCase.checkNodeAvailable(nodeList.get(0).getHandle())
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe((result, throwable) -> {
                                            if (throwable != null) {
                                                Timber.e(throwable);
                                            } else {
                                                if (result) {
                                                    showOptionsForAvailableNode();
                                                } else {
                                                    editIcon.setVisible(false);
                                                    copyIcon.setVisible(false);
                                                    forwardIcon.setVisible(false);
                                                    downloadIcon.setVisible(false);
                                                    shareIcon.setVisible(false);
                                                    inviteIcon.setVisible(false);
                                                    startConversationIcon.setVisible(false);
                                                    importIcon.setVisible(false);
                                                }
                                            }
                                        }));
                            } else {
                                showOptionsForAvailableNode();
                            }
                        }
                        break;

                    case MegaChatMessage.TYPE_CONTACT_ATTACHMENT:
                        Timber.d("Message selected is a contact attachment");
                        if (viewModel.isConnected()) {
                            String userEmail = chatMessage.getUserEmail(0);
                            long messageUserHandle = chatMessage.getUserHandle(0);
                            MegaUser contact = megaApi.getContact(userEmail);
                            if (contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) {
                                long chatRoomPeerHandle = chatRoom.getPeerHandle(0);
                                startConversationIcon.setVisible(!isRemovedMsg && (chatRoom.isGroup() || messageUserHandle != chatRoomPeerHandle));
                            } else {
                                inviteIcon.setVisible(!isRemovedMsg && messageUserHandle != myUserHandle);
                            }
                        }
                        break;

                    default:
                        Timber.d("Other type: %s", chatMessage.getType());
                        MegaChatMessage messageSelected = megaChatApi.getMessage(idChat, chatMessage.getMsgId());
                        if (messageSelected == null) {
                            messageSelected = megaChatApi.getMessage(idChat, chatMessage.getTempId());
                            if (messageSelected == null) {
                                return false;
                            }
                        }

                        copyIcon.setVisible((typeMessage != MegaChatMessage.TYPE_CONTAINS_META && typeMessage != MegaChatMessage.TYPE_VOICE_CLIP) || (messageSelected.getContainsMeta() != null && messageSelected.getContainsMeta().getType() != MegaChatContainsMeta.CONTAINS_META_GIPHY));

                        forwardIcon.setVisible(!isRemovedMsg && viewModel.isConnected() && !chatC.isInAnonymousMode() && typeMessage != MegaChatMessage.TYPE_TRUNCATE &&
                                typeMessage != MegaChatMessage.TYPE_ALTER_PARTICIPANTS && typeMessage != MegaChatMessage.TYPE_CHAT_TITLE && typeMessage != MegaChatMessage.TYPE_PRIV_CHANGE &&
                                typeMessage != MegaChatMessage.TYPE_CALL_ENDED & typeMessage != MegaChatMessage.TYPE_CALL_STARTED);

                        editIcon.setVisible(messageSelected.getUserHandle() == myUserHandle && !isRemovedMsg && messageSelected.isEditable());
                        break;
                }

                return false;
            }

            Timber.d("Many items selected");
            boolean isUploading = false;
            boolean isRemoved = false;
            boolean someNodeIsAttachmentAndSent = false;

            showDelete = true;
            showCopy = true;
            showForward = true;
            allNodeAttachments = true;
            allNodeImages = true;
            allNodeNonContacts = true;
            editIcon.setVisible(false);
            startConversationIcon.setVisible(false);

            for (int i = 0; i < selected.size(); i++) {
                MegaChatMessage chatMessage = selected.get(i).getMessage();
                int typeMessage = chatMessage.getType();
                boolean isMyOwnMsg = chatMessage.getUserHandle() == myUserHandle;

                isRemoved = ChatUtil.isMsgRemovedOrHasRejectedOrManualSendingStatus(removedMessages, chatMessage);

                if (typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT && isMyOwnMsg) {
                    someNodeIsAttachmentAndSent = true;
                }

                if (!isUploading) {
                    if (selected.get(i).isUploading()) {
                        isUploading = true;
                    }
                }

                if (showCopy
                        && (typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT
                        || typeMessage == MegaChatMessage.TYPE_CONTACT_ATTACHMENT
                        || typeMessage == MegaChatMessage.TYPE_VOICE_CLIP
                        || (typeMessage == MegaChatMessage.TYPE_CONTAINS_META && chatMessage.getContainsMeta() != null
                        && chatMessage.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GIPHY))) {
                    showCopy = false;
                }

                if (isRemoved || (showDelete &&
                        (chatMessage.getUserHandle() != myUserHandle ||
                                ((typeMessage == MegaChatMessage.TYPE_NORMAL ||
                                        typeMessage == MegaChatMessage.TYPE_NODE_ATTACHMENT ||
                                        typeMessage == MegaChatMessage.TYPE_CONTACT_ATTACHMENT ||
                                        typeMessage == MegaChatMessage.TYPE_CONTAINS_META ||
                                        typeMessage == MegaChatMessage.TYPE_VOICE_CLIP) && !chatMessage.isDeletable())))) {
                    showDelete = false;
                }

                if (showForward &&
                        (typeMessage == MegaChatMessage.TYPE_TRUNCATE ||
                                typeMessage == MegaChatMessage.TYPE_ALTER_PARTICIPANTS ||
                                typeMessage == MegaChatMessage.TYPE_CHAT_TITLE ||
                                typeMessage == MegaChatMessage.TYPE_PRIV_CHANGE ||
                                typeMessage == MegaChatMessage.TYPE_CALL_ENDED ||
                                typeMessage == MegaChatMessage.TYPE_CALL_STARTED)) {
                    showForward = false;
                }

                if (allNodeAttachments &&
                        typeMessage != MegaChatMessage.TYPE_NODE_ATTACHMENT) {
                    allNodeAttachments = false;
                    allNodeImages = false;
                }

                if (allNodeImages && !isMsgImage(selected.get(i))) {
                    allNodeImages = false;
                }

                if (allNodeNonContacts) {
                    if (typeMessage != MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                        allNodeNonContacts = false;
                    } else {
                        MegaUser contact = megaApi.getContact(chatMessage.getUserEmail(0));
                        if ((contact != null && contact.getVisibility() == MegaUser.VISIBILITY_VISIBLE) ||
                                chatMessage.getUserHandle(0) == myUserHandle) {
                            allNodeNonContacts = false;
                        }
                    }
                }
            }

            boolean isNotUploadingNotAnonymousNotRemoved = !isUploading && !chatC.isInAnonymousMode() && !isRemoved;
            boolean isOnlineNotUploadingNotAnonymousNotRemoved = viewModel.isConnected() && isNotUploadingNotAnonymousNotRemoved;

            if (someNodeIsAttachmentAndSent) {
                internalComposite.add(getNodeUseCase.checkNodesAvailable(selected)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((result, throwable) -> {
                            if (throwable != null) {
                                Timber.e(throwable);
                            } else {
                                if (result) {
                                    if (allNodeAttachments && isOnlineNotUploadingNotAnonymousNotRemoved) {
                                        showOptionsForAvailableNode();
                                    }

                                    deleteIcon.setVisible(showDelete && isNotUploadingNotAnonymousNotRemoved);
                                    copyIcon.setVisible(showCopy && isNotUploadingNotAnonymousNotRemoved);
                                    inviteIcon.setVisible(allNodeNonContacts && isOnlineNotUploadingNotAnonymousNotRemoved);
                                    forwardIcon.setVisible(showForward && isOnlineNotUploadingNotAnonymousNotRemoved);
                                } else {
                                    editIcon.setVisible(false);
                                    copyIcon.setVisible(false);
                                    forwardIcon.setVisible(false);
                                    downloadIcon.setVisible(false);
                                    shareIcon.setVisible(false);
                                    inviteIcon.setVisible(false);
                                    startConversationIcon.setVisible(false);
                                    importIcon.setVisible(false);
                                    deleteIcon.setVisible(showDelete && isNotUploadingNotAnonymousNotRemoved);

                                }
                            }
                        }));

            } else {
                if (allNodeAttachments && isOnlineNotUploadingNotAnonymousNotRemoved) {
                    showOptionsForAvailableNode();
                }

                deleteIcon.setVisible(showDelete && isNotUploadingNotAnonymousNotRemoved);
                copyIcon.setVisible(showCopy && isNotUploadingNotAnonymousNotRemoved);
                inviteIcon.setVisible(allNodeNonContacts && isOnlineNotUploadingNotAnonymousNotRemoved);
                forwardIcon.setVisible(showForward && isOnlineNotUploadingNotAnonymousNotRemoved);
            }

            return false;
        }
    }

    public boolean showSelectMenuItem() {
        if (adapter != null) {
            return adapter.isMultipleSelect();
        }
        return false;
    }

    public void downloadNodeList(MegaNodeList nodeList) {
        PermissionUtils.checkNotificationsPermission(this);
        nodeSaver.saveNodeLists(Collections.singletonList(nodeList), false, false, false, true);
    }

    public void showConfirmationDeleteMessages(final ArrayList<AndroidMegaChatMessage> messagesToDelete, final MegaChatRoom chat) {
        Timber.d("showConfirmationDeleteMessages");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        stopReproductions();
                        chatC.deleteAndroidMessages(messagesToDelete, chat);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);

        if (messages.size() == 1) {
            builder.setMessage(R.string.confirmation_delete_one_message);
        } else {
            builder.setMessage(R.string.confirmation_delete_several_messages);
        }
        builder.setPositiveButton(R.string.context_remove, dialogClickListener).setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    public void showConfirmationDeleteMessage(final long messageId, final long chatId) {
        Timber.d("showConfirmationDeleteMessage");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        ChatController cC = new ChatController(chatActivity);
                        cC.deleteMessageById(messageId, chatId);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);

        builder.setMessage(R.string.confirmation_delete_one_message);
        builder.setPositiveButton(R.string.context_remove, dialogClickListener)
                .setNegativeButton(R.string.general_cancel, dialogClickListener).show();
    }

    /*
     * Clear all selected items
     */
    private void clearSelections() {
        if (adapter.isMultipleSelect()) {
            adapter.clearSelections();
        }
        updateActionModeTitle();
    }

    public void updateActionModeTitle() {
        try {
            if (actionMode != null) {
                if (adapter.getSelectedItemCount() == 0) {
                    actionMode.setTitle(getString(R.string.select_message_title));
                } else {
                    actionMode.setTitle(adapter.getSelectedItemCount() + "");
                }
                actionMode.invalidate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Timber.e(e, "Invalidate error");
        }
    }

    /*
     * Disable selection
     */
    public void hideMultipleSelect() {
        if (adapter != null) {
            adapter.setMultipleSelect(false);
        }

        if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void finishMultiselectionMode() {
        clearSelections();
        hideMultipleSelect();
    }

    private void checkActionMode() {
        if (adapter.isMultipleSelect() && actionMode != null) {
            actionMode.invalidate();
        } else {
            editingMessage = false;
        }
    }

    public void selectAll() {
        if (adapter != null) {
            if (adapter.isMultipleSelect()) {
                adapter.selectAll();
            } else {
                adapter.setMultipleSelect(true);
                adapter.selectAll();

                actionMode = startSupportActionMode(new ActionBarCallBack());
            }

            new Handler(Looper.getMainLooper()).post(() -> updateActionModeTitle());
        }
    }

    /**
     * Method for displaying feedback dialogue in a message.
     *
     * @param chatId      The chat ID.
     * @param megaMessage The message.
     * @param reaction    The reaction.
     */
    public void openInfoReactionBottomSheet(long chatId, AndroidMegaChatMessage megaMessage, String reaction) {
        if (chatRoom.getChatId() != chatId)
            return;

        showReactionBottomSheet(megaMessage, messages.indexOf(megaMessage), reaction);
    }

    public boolean isMultiselectOn() {
        return adapter != null && adapter.isMultipleSelect();
    }

    public void openReactionBottomSheet(long chatId, AndroidMegaChatMessage megaMessage) {
        if (chatRoom.getChatId() != chatId)
            return;

        int positionInMessages = messages.indexOf(megaMessage);
        showReactionBottomSheet(megaMessage, positionInMessages, null);
    }

    public void itemLongClick(int positionInAdapter) {
        int positionInMessages = positionInAdapter - 1;
        if (positionInMessages >= messages.size())
            return;

        AndroidMegaChatMessage m = messages.get(positionInMessages);

        if (adapter.isMultipleSelect() || m == null || m.isUploading() || m.getMessage().getStatus() == MegaChatMessage.STATUS_SERVER_REJECTED || m.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL)
            return;

        int type = m.getMessage().getType();
        switch (type) {
            case MegaChatMessage.TYPE_NODE_ATTACHMENT:
            case MegaChatMessage.TYPE_CONTACT_ATTACHMENT:
            case MegaChatMessage.TYPE_VOICE_CLIP:
            case MegaChatMessage.TYPE_NORMAL:
                showGeneralChatMessageBottomSheet(m, positionInMessages);
                break;

            case MegaChatMessage.TYPE_CONTAINS_META:
                MegaChatContainsMeta meta = m.getMessage().getContainsMeta();
                if (meta == null || meta.getType() == MegaChatContainsMeta.CONTAINS_META_INVALID)
                    return;

                if (meta.getType() == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW
                        || meta.getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION
                        || meta.getType() == MegaChatContainsMeta.CONTAINS_META_GIPHY) {
                    showGeneralChatMessageBottomSheet(m, positionInMessages);
                }
                break;
        }
    }

    private boolean isSelectableMessage(AndroidMegaChatMessage message) {
        if (message.getMessage().getStatus() == MegaChatMessage.STATUS_SERVER_REJECTED || message.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL)
            return false;

        int type = message.getMessage().getType();
        switch (type) {
            case MegaChatMessage.TYPE_NODE_ATTACHMENT:
            case MegaChatMessage.TYPE_CONTACT_ATTACHMENT:
            case MegaChatMessage.TYPE_VOICE_CLIP:
            case MegaChatMessage.TYPE_NORMAL:
            case MegaChatMessage.TYPE_CONTAINS_META:
            case MegaChatMessage.TYPE_PUBLIC_HANDLE_CREATE:
            case MegaChatMessage.TYPE_PUBLIC_HANDLE_DELETE:
            case MegaChatMessage.TYPE_SET_PRIVATE_MODE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Method to control when to click on an attached node
     *
     * @param node               The MegaNode
     * @param msgId              msg ID
     * @param screenPosition     position in screen
     * @param positionInMessages position in array messages
     */
    private void nodeAttachmentClicked(MegaNode node, long msgId, int[] screenPosition, int positionInMessages) {
        if (MimeTypeList.typeForName(node.getName()).isImage()) {
            if (node.hasPreview()) {
                Timber.d("Show full screen viewer");
                showFullScreenViewer(msgId);
            } else {
                Timber.d("Image without preview - open with");
                openWith(this, node);
            }
        } else if (MimeTypeList.typeForName(node.getName()).isVideoMimeType() || MimeTypeList.typeForName(node.getName()).isAudio()) {
            String mimeType = MimeTypeList.typeForName(node.getName()).getType();
            Intent mediaIntent;
            boolean internalIntent;
            boolean opusFile = false;
            if (MimeTypeList.typeForName(node.getName()).isVideoNotSupported() || MimeTypeList.typeForName(node.getName()).isAudioNotSupported()) {
                mediaIntent = new Intent(Intent.ACTION_VIEW);
                internalIntent = false;
                String[] s = node.getName().split("\\.");
                if (s.length > 1 && s[s.length - 1].equals("opus")) {
                    opusFile = true;
                }
            } else {
                mediaIntent = getMediaIntent(this, node.getName());
                internalIntent = true;
            }

            mediaIntent.putExtra(INTENT_EXTRA_KEY_SCREEN_POSITION, screenPosition);
            mediaIntent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, FROM_CHAT);
            mediaIntent.putExtra(INTENT_EXTRA_KEY_IS_PLAYLIST, false);
            mediaIntent.putExtra(INTENT_EXTRA_KEY_MSG_ID, msgId);
            mediaIntent.putExtra(INTENT_EXTRA_KEY_CHAT_ID, idChat);
            mediaIntent.putExtra(INTENT_EXTRA_KEY_FILE_NAME, node.getName());

            String localPath = getLocalFile(node);

            if (localPath != null) {
                File mediaFile = new File(localPath);
                if (localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                    Uri mediaFileUri = FileProvider.getUriForFile(this, AUTHORITY_STRING_FILE_PROVIDER, mediaFile);
                    if (mediaFileUri == null) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), MEGACHAT_INVALID_HANDLE);
                    } else {
                        mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                    }
                } else {
                    Uri mediaFileUri = Uri.fromFile(mediaFile);
                    if (mediaFileUri == null) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), MEGACHAT_INVALID_HANDLE);
                    } else {
                        mediaIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                    }
                }
                mediaIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                if (viewModel.isConnected()) {
                    if (megaApi.httpServerIsRunning() == 0) {
                        Timber.d("megaApi.httpServerIsRunning() == 0");
                        megaApi.httpServerStart();
                        mediaIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
                    } else {
                        Timber.w("ERROR:httpServerAlreadyRunning");
                    }

                    String url = megaApi.httpServerGetLocalLink(node);
                    if (url != null && Uri.parse(url) != null) {
                        mediaIntent.setDataAndType(Uri.parse(url), mimeType);
                    } else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), MEGACHAT_INVALID_HANDLE);
                    }
                } else {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem) + ". " + getString(R.string.no_network_connection_on_play_file), MEGACHAT_INVALID_HANDLE);
                }
            }
            mediaIntent.putExtra("HANDLE", node.getHandle());

            if (opusFile) {
                mediaIntent.setDataAndType(mediaIntent.getData(), "audio/*");
            }

            if (internalIntent || isIntentAvailable(this, mediaIntent)) {
                startActivity(mediaIntent);
            } else {
                openWith(this, node);
            }
            overridePendingTransition(0, 0);

            if (adapter != null) {
                adapter.setNodeAttachmentVisibility(false, holder_imageDrag, positionInMessages);
            }
        } else if (MimeTypeList.typeForName(node.getName()).isPdf()) {
            String mimeType = MimeTypeList.typeForName(node.getName()).getType();
            Intent pdfIntent = new Intent(this, PdfViewerActivity.class);
            pdfIntent.putExtra(INTENT_EXTRA_KEY_INSIDE, true);
            pdfIntent.putExtra(INTENT_EXTRA_KEY_ADAPTER_TYPE, FROM_CHAT);
            pdfIntent.putExtra(INTENT_EXTRA_KEY_MSG_ID, msgId);
            pdfIntent.putExtra(INTENT_EXTRA_KEY_CHAT_ID, idChat);

            String localPath = getLocalFile(node);

            if (localPath != null) {
                File mediaFile = new File(localPath);
                if (localPath.contains(Environment.getExternalStorageDirectory().getPath())) {
                    Uri mediaFileUri = FileProvider.getUriForFile(this, AUTHORITY_STRING_FILE_PROVIDER, mediaFile);
                    if (mediaFileUri == null) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), MEGACHAT_INVALID_HANDLE);
                    } else {
                        pdfIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                    }
                } else {
                    Uri mediaFileUri = Uri.fromFile(mediaFile);
                    if (mediaFileUri == null) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), MEGACHAT_INVALID_HANDLE);
                    } else {
                        pdfIntent.setDataAndType(mediaFileUri, MimeTypeList.typeForName(node.getName()).getType());
                    }
                }
                pdfIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else if (viewModel.isConnected()) {
                if (megaApi.httpServerIsRunning() == 0) {
                    megaApi.httpServerStart();
                    pdfIntent.putExtra(INTENT_EXTRA_KEY_NEED_STOP_HTTP_SERVER, true);
                } else {
                    Timber.e("ERROR:httpServerAlreadyRunning");
                }

                String url = megaApi.httpServerGetLocalLink(node);
                if (url != null && Uri.parse(url) != null) {
                    pdfIntent.setDataAndType(Uri.parse(url), mimeType);
                } else {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_text_error), MEGACHAT_INVALID_HANDLE);
                }
            } else {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem) + ". " + getString(R.string.no_network_connection_on_play_file), MEGACHAT_INVALID_HANDLE);
            }
            pdfIntent.putExtra("HANDLE", node.getHandle());

            if (isIntentAvailable(this, pdfIntent)) {
                startActivity(pdfIntent);
            } else {
                openWith(this, node);
            }
            overridePendingTransition(0, 0);
        } else if (MimeTypeList.typeForName(node.getName()).isOpenableTextFile(node.getSize())) {
            manageTextFileIntent(this, msgId, idChat);
        } else {
            onNodeTapped(this, node, this::saveNodeByTap, this, this, false);
        }
    }

    public void itemClick(int positionInAdapter, int[] screenPosition) {
        if (messages == null || messages.isEmpty()) {
            Timber.e("Messages null or empty");
            return;
        }
        int positionInMessages = positionInAdapter - 1;
        if (positionInMessages < messages.size()) {
            AndroidMegaChatMessage m = messages.get(positionInMessages);
            if (adapter.isMultipleSelect()) {
                Timber.d("isMultipleSelect");
                if (!m.isUploading()) {
                    Timber.d("isMultipleSelect - iNOTsUploading");
                    if (m.getMessage() != null) {
                        MegaChatContainsMeta meta = m.getMessage().getContainsMeta();
                        if (meta != null && meta.getType() == MegaChatContainsMeta.CONTAINS_META_INVALID) {
                        } else {
                            Timber.d("Message id: %s", m.getMessage().getMsgId());
                            Timber.d("Timestamp: %s", m.getMessage().getTimestamp());
                            if (isSelectableMessage(m)) {
                                adapter.toggleSelection(m.getMessage().getMsgId());
                                List<AndroidMegaChatMessage> messages = adapter.getSelectedMessages();
                                if (!messages.isEmpty()) {
                                    updateActionModeTitle();
                                }
                            }
                        }
                    }
                }
            } else {
                if (m != null) {
                    if (m.isUploading()) {
                        showUploadingAttachmentBottomSheet(m, positionInMessages);
                    } else {
                        if ((m.getMessage().getStatus() == MegaChatMessage.STATUS_SERVER_REJECTED) || (m.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                            if (m.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()) {
                                if (!(m.getMessage().isManagementMessage())) {
                                    Timber.d("selected message handle: %s", m.getMessage().getTempId());
                                    Timber.d("selected message rowId: %s", m.getMessage().getRowId());
                                    if ((m.getMessage().getStatus() == MegaChatMessage.STATUS_SERVER_REJECTED) || (m.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL)) {
                                        Timber.d("show not sent message panel");
                                        showMsgNotSentPanel(m, positionInMessages);
                                    }
                                }
                            }
                        } else {
                            if (m.getMessage().getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT) {
                                MegaNodeList nodeList = m.getMessage().getMegaNodeList();
                                if (nodeList.size() == 1) {
                                    if (m.getMessage().getUserHandle() == myUserHandle) {
                                        internalComposite.add(getNodeUseCase.get(nodeList.get(0).getHandle())
                                                .subscribeOn(Schedulers.io())
                                                .observeOn(AndroidSchedulers.mainThread())
                                                .subscribe((result, throwable) -> {
                                                    if (throwable == null) {
                                                        MegaNode node = chatC.authorizeNodeIfPreview(nodeList.get(0), chatRoom);
                                                        nodeAttachmentClicked(node, m.getMessage().getMsgId(), screenPosition, positionInMessages);
                                                    } else {
                                                        showSnackbar(SNACKBAR_TYPE, getString(R.string.error_file_not_available), MEGACHAT_INVALID_HANDLE);
                                                    }
                                                }));
                                    } else {
                                        MegaNode node = chatC.authorizeNodeIfPreview(nodeList.get(0), chatRoom);
                                        nodeAttachmentClicked(node, m.getMessage().getMsgId(), screenPosition, positionInMessages);
                                    }
                                }
                            } else if (m.getMessage().getType() == MegaChatMessage.TYPE_CONTACT_ATTACHMENT) {
                                Timber.d("show contact attachment panel");
                                if (!viewModel.isConnected()) {
                                    //No shown - is not possible to know is it already contact or not - megaApi not working
                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), MEGACHAT_INVALID_HANDLE);
                                } else if (!chatC.isInAnonymousMode()) {
                                    if (m.getMessage().getUsersCount() < 1)
                                        return;

                                    if (m.getMessage().getUsersCount() > 1) {
                                        ContactUtil.openContactAttachmentActivity(this, idChat, m.getMessage().getMsgId());
                                        return;
                                    }

                                    if (m.getMessage().getUserHandle(0) != megaChatApi.getMyUserHandle()) {
                                        String email = m.getMessage().getUserEmail(0);
                                        showContactClickResult(email, getNameContactAttachment(m.getMessage()));
                                    }
                                }
                            } else if (m.getMessage().getType() == MegaChatMessage.TYPE_CONTAINS_META) {
                                MegaChatContainsMeta meta = m.getMessage().getContainsMeta();
                                if (meta == null || meta.getType() == MegaChatContainsMeta.CONTAINS_META_INVALID)
                                    return;
                                String url = null;
                                if (meta.getType() == MegaChatContainsMeta.CONTAINS_META_RICH_PREVIEW) {
                                    url = meta.getRichPreview().getUrl();

                                    if (isMEGALinkAndRequiresTransferSession(this, url)) {
                                        return;
                                    }
                                } else if (meta.getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION) {
                                    url = m.getMessage().getContent();
                                    MegaChatGeolocation location = meta.getGeolocation();
                                    if (location != null) {
                                        float latitude = location.getLatitude();
                                        float longitude = location.getLongitude();
                                        List<Address> addresses = getAddresses(this, latitude, longitude);
                                        if (addresses != null && !addresses.isEmpty()) {
                                            String address = addresses.get(0).getAddressLine(0);
                                            if (address != null) {
                                                url = "geo:" + latitude + "," + longitude + "?q=" + Uri.encode(address);
                                            }
                                        }
                                    }
                                }

                                if (url == null) return;

                                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

                                if (isIntentAvailable(this, browserIntent)) {
                                    startActivity(browserIntent);
                                } else {
                                    showSnackbar(SNACKBAR_TYPE, getString(R.string.intent_not_available_location), MEGACHAT_INVALID_HANDLE);
                                }
                            } else if (m.getMessage().getType() == MegaChatMessage.TYPE_NORMAL) {
                                AndroidMegaRichLinkMessage richLinkMessage = m.getRichLinkMessage();
                                if (richLinkMessage != null) {
                                    String url = richLinkMessage.getUrl();
                                    if (richLinkMessage.isChat()) {
                                        Timber.d("Link clicked");
                                        megaChatApi.checkChatLink(url, new LoadPreviewListener(ChatActivity.this, ChatActivity.this, CHECK_LINK_TYPE_UNKNOWN_LINK));
                                    } else {
                                        openMegaLink(url, richLinkMessage.getNode() != null ? richLinkMessage.getNode().isFile() : richLinkMessage.isFile());
                                    }
                                }

                            }
                        }
                    }
                }
            }
        } else {
            Timber.d("DO NOTHING: Position (%d) is more than size in messages (size: %d)", positionInMessages, messages.size());
        }
    }

    /**
     * Opens a contact link message.
     *
     * @param contactLinkResult All the data of the contact link.
     */
    public void openContactLinkMessage(ContactLink contactLinkResult) {
        String email = contactLinkResult.getEmail();
        if (email == null) {
            Timber.d("Email is null");
            return;
        }

        if (email.equals(megaApi.getMyEmail())) {
            Timber.d("Contact is my own contact");
            return;
        }

        showContactClickResult(email, contactLinkResult.getFullName());
    }

    /**
     * Shows the final result of a contact link or attachment message click.
     *
     * @param email Email of the contact.
     * @param name  Name of the contact.
     */
    private void showContactClickResult(String email, String name) {
        MegaUser contact = megaApi.getContact(email);

        if (contact == null || contact.getVisibility() != MegaUser.VISIBILITY_VISIBLE) {
            viewModel.checkContactRequestSent(email, name);
        } else if (!chatRoom.isGroup() && email != null && email.equals(megaChatApi.getUserEmailFromCache(0))) {
            showGroupOrContactInfoActivity();
        } else {
            ContactUtil.openContactInfoActivity(this, email);
        }
    }

    public void loadChatLink(String link) {
        Timber.d("Open new chat room");
        Intent intentOpenChat = new Intent(this, ChatActivity.class);
        intentOpenChat.setAction(ACTION_OPEN_CHAT_LINK);
        intentOpenChat.setData(Uri.parse(link));
        this.startActivity(intentOpenChat);
    }

    public void showFullScreenViewer(long msgId) {
        int position = 0;
        long currentNodeHandle = INVALID_HANDLE;
        List<Long> messageIds = new ArrayList<>();

        for (int i = 0; i < messages.size(); i++) {
            AndroidMegaChatMessage androidMessage = messages.get(i);
            if (!androidMessage.isUploading()) {
                MegaChatMessage message = androidMessage.getMessage();
                if (message.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT) {
                    messageIds.add(message.getMsgId());
                    if (message.getMsgId() == msgId) {
                        currentNodeHandle = message.getMegaNodeList().get(0).getHandle();
                        position = i;
                    }
                }
            }
        }

        Intent intent = ImageViewerActivity.getIntentForChatMessages(
                this,
                idChat,
                Longs.toArray(messageIds),
                currentNodeHandle
        );

        startActivity(intent);
        overridePendingTransition(0, 0);

        if (adapter != null) {
            adapter.setNodeAttachmentVisibility(false, holder_imageDrag, position);
        }
    }

    @Override
    public void onChatRoomUpdate(MegaChatApiJava api, MegaChatRoom chat) {
        this.chatRoom = chat;
        if (adapter != null) {
            adapter.updateChatRoom(chatRoom);
        }

        if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_CLOSED)) {
            Timber.d("CHANGE_TYPE_CLOSED for the chat: %s", chat.getChatId());
            int permission = chat.getOwnPrivilege();
            Timber.d("Permissions for the chat: %s", permission);

            if (chat.isPreview()) {
                if (permission == MegaChatRoom.PRIV_RM) {
                    //Show alert to user
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.alert_invalid_preview), -1);
                }
            } else {
                //Hide field to write
                viewModel.setIsJoiningOrLeaving(false, null);
                setChatSubtitle();
                supportInvalidateOptionsMenu();
            }
        } else if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_STATUS)) {
            Timber.d("CHANGE_TYPE_STATUS for the chat: %s", chat.getChatId());
            if (!(chatRoom.isGroup())) {
                long userHandle = chatRoom.getPeerHandle(0);
                setStatus(userHandle);
            }
        } else if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_PARTICIPANTS)) {
            Timber.d("CHANGE_TYPE_PARTICIPANTS for the chat: %s", chat.getChatId());
            setChatSubtitle();
        } else if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_OWN_PRIV)) {
            Timber.d("CHANGE_TYPE_OWN_PRIV for the chat: %s", chat.getChatId());
            setChatSubtitle();
            supportInvalidateOptionsMenu();
            viewModel.chatRoomUpdated(chat.isWaitingRoom(), chat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR);
        } else if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_WAITING_ROOM)) {
            viewModel.chatRoomUpdated(chat.isWaitingRoom(), chat.getOwnPrivilege() == MegaChatRoom.PRIV_MODERATOR);
        } else if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_TITLE)) {
            updateTitle();
        } else if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_USER_STOP_TYPING)) {
            Timber.d("CHANGE_TYPE_USER_STOP_TYPING for the chat: %s", chat.getChatId());

            long userHandleTyping = chat.getUserTyping();

            if (userHandleTyping == megaChatApi.getMyUserHandle()) {
                return;
            }

            if (usersTypingSync == null) {
                return;
            }

            //Find the item
            boolean found = false;
            for (UserTyping user : usersTypingSync) {
                if (user.getParticipantTyping().getHandle() == userHandleTyping) {
                    Timber.d("Found user typing!");
                    usersTypingSync.remove(user);
                    found = true;
                    break;
                }
            }

            if (!found) {
                Timber.d("CHANGE_TYPE_USER_STOP_TYPING: Not found user typing");
            } else {
                updateUserTypingFromNotification();
            }

        } else if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_USER_TYPING)) {
            Timber.d("CHANGE_TYPE_USER_TYPING for the chat: %s", chat.getChatId());
            if (chat != null) {

                long userHandleTyping = chat.getUserTyping();

                if (userHandleTyping == megaChatApi.getMyUserHandle()) {
                    return;
                }

                if (usersTyping == null) {
                    usersTyping = new ArrayList<>();
                    usersTypingSync = Collections.synchronizedList(usersTyping);
                }

                //Find if any notification arrives previously
                if (usersTypingSync.size() <= 0) {
                    Timber.d("No more users writing");
                    MegaChatParticipant participantTyping = new MegaChatParticipant(userHandleTyping);
                    UserTyping currentUserTyping = new UserTyping(participantTyping);

                    String nameTyping = chatC.getParticipantFirstName(userHandleTyping);

                    Timber.d("userHandleTyping: %s", userHandleTyping);

                    if (isTextEmpty(nameTyping)) {
                        nameTyping = getString(R.string.transfer_unknown);
                    }

                    participantTyping.setFirstName(nameTyping);

                    userTypingTimeStamp = System.currentTimeMillis() / 1000;
                    currentUserTyping.setTimeStampTyping(userTypingTimeStamp);

                    usersTypingSync.add(currentUserTyping);

                    String userTyping = getResources().getQuantityString(R.plurals.user_typing, 1, toCDATA(usersTypingSync.get(0).getParticipantTyping().getFirstName()));
                    userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                    userTyping = userTyping.replace("[/A]", "</font>");
                    userTypingText.setText(HtmlCompat.fromHtml(userTyping, HtmlCompat.FROM_HTML_MODE_LEGACY));

                    userTypingLayout.setVisibility(View.VISIBLE);
                } else {
                    Timber.d("More users writing or the same in different timestamp");

                    //Find the item
                    boolean found = false;
                    for (UserTyping user : usersTypingSync) {
                        if (user.getParticipantTyping().getHandle() == userHandleTyping) {
                            Timber.d("Found user typing!");
                            userTypingTimeStamp = System.currentTimeMillis() / 1000;
                            user.setTimeStampTyping(userTypingTimeStamp);
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        Timber.d("It's a new user typing");
                        MegaChatParticipant participantTyping = new MegaChatParticipant(userHandleTyping);
                        UserTyping currentUserTyping = new UserTyping(participantTyping);

                        String nameTyping = chatC.getParticipantFirstName(userHandleTyping);
                        if (isTextEmpty(nameTyping)) {
                            nameTyping = getString(R.string.transfer_unknown);
                        }

                        participantTyping.setFirstName(nameTyping);

                        userTypingTimeStamp = System.currentTimeMillis() / 1000;
                        currentUserTyping.setTimeStampTyping(userTypingTimeStamp);

                        usersTypingSync.add(currentUserTyping);

                        //Show the notification
                        String userTyping;
                        int size = usersTypingSync.size();
                        switch (size) {
                            case 1:
                                userTyping = getResources().getQuantityString(R.plurals.user_typing, 1, usersTypingSync.get(0).getParticipantTyping().getFirstName());
                                userTyping = toCDATA(userTyping);
                                break;

                            case 2:
                                userTyping = getResources().getQuantityString(R.plurals.user_typing, 2, usersTypingSync.get(0).getParticipantTyping().getFirstName() + ", " + usersTypingSync.get(1).getParticipantTyping().getFirstName());
                                userTyping = toCDATA(userTyping);
                                break;

                            default:
                                String names = usersTypingSync.get(0).getParticipantTyping().getFirstName() + ", " + usersTypingSync.get(1).getParticipantTyping().getFirstName();
                                userTyping = String.format(getString(R.string.more_users_typing), toCDATA(names));
                                break;
                        }

                        userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                        userTyping = userTyping.replace("[/A]", "</font>");

                        userTypingText.setText(HtmlCompat.fromHtml(userTyping, HtmlCompat.FROM_HTML_MODE_LEGACY));
                        userTypingLayout.setVisibility(View.VISIBLE);
                    }
                }

                int interval = 5000;
                IsTypingRunnable runnable = new IsTypingRunnable(userTypingTimeStamp, userHandleTyping);
                handlerReceive = new Handler();
                handlerReceive.postDelayed(runnable, interval);
            }
        } else if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_ARCHIVE)) {
            Timber.d("CHANGE_TYPE_ARCHIVE for the chat: %s", chat.getChatId());
            setChatSubtitle();
        } else if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_CHAT_MODE)) {
            Timber.d("CHANGE_TYPE_CHAT_MODE for the chat: %s", chat.getChatId());
        } else if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_UPDATE_PREVIEWERS)) {
            Timber.d("CHANGE_TYPE_UPDATE_PREVIEWERS for the chat: %s", chat.getChatId());
            setPreviewersView();
        } else if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_RETENTION_TIME)) {
            Timber.d("CHANGE_TYPE_RETENTION_TIME for the chat: %s", chat.getChatId());
            Intent intentRetentionTime = new Intent(ACTION_UPDATE_RETENTION_TIME);
            intentRetentionTime.putExtra(RETENTION_TIME, chat.getRetentionTime());
            intentRetentionTime.setPackage(getApplicationContext().getPackageName());
            MegaApplication.getInstance().sendBroadcast(intentRetentionTime);
        } else if (chat.hasChanged(MegaChatRoom.CHANGE_TYPE_OPEN_INVITE)) {
            if (chat.isGroup()) {
                int permission = chat.getOwnPrivilege();
                boolean visibility = chat.isActive() && (permission == MegaChatRoom.PRIV_MODERATOR || chat.isOpenInvite());
                inviteMenuItem.setVisible(visibility);
            }

            LiveEventBus.get(EVENT_CHAT_OPEN_INVITE, MegaChatRoom.class).post(chat);
        }
    }

    void setPreviewersView() {
        if (chatRoom != null && chatRoom.getNumPreviewers() > 0) {
            observersNumberText.setText(chatRoom.getNumPreviewers() + "");
            observersLayout.setVisibility(View.VISIBLE);
        } else {
            observersLayout.setVisibility(View.GONE);
        }
    }

    private class IsTypingRunnable implements Runnable {

        long timeStamp;
        long userHandleTyping;

        public IsTypingRunnable(long timeStamp, long userHandleTyping) {
            this.timeStamp = timeStamp;
            this.userHandleTyping = userHandleTyping;
        }

        @Override
        public void run() {
            Timber.d("Run off notification typing");
            long timeNow = System.currentTimeMillis() / 1000;
            if ((timeNow - timeStamp) > 4) {
                Timber.d("Remove user from the list");

                boolean found = false;
                for (UserTyping user : usersTypingSync) {
                    if (user.getTimeStampTyping() == timeStamp) {
                        if (user.getParticipantTyping().getHandle() == userHandleTyping) {
                            Timber.d("Found user typing in runnable!");
                            usersTypingSync.remove(user);
                            found = true;
                            break;
                        }
                    }
                }

                if (!found) {
                    Timber.d("Not found user typing in runnable!");
                }

                updateUserTypingFromNotification();
            }
        }
    }

    public void updateUserTypingFromNotification() {
        Timber.d("updateUserTypingFromNotification");

        int size = usersTypingSync.size();
        Timber.d("Size of typing: %s", size);
        switch (size) {
            case 0: {
                userTypingLayout.setVisibility(View.GONE);
                break;
            }
            case 1: {
                String userTyping = getResources().getQuantityString(R.plurals.user_typing, 1, usersTypingSync.get(0).getParticipantTyping().getFirstName());
                userTyping = toCDATA(userTyping);
                userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                userTyping = userTyping.replace("[/A]", "</font>");

                Spanned result = Html.fromHtml(userTyping, Html.FROM_HTML_MODE_LEGACY);

                userTypingText.setText(result);
                break;
            }
            case 2: {
                String userTyping = getResources().getQuantityString(R.plurals.user_typing, 2, usersTypingSync.get(0).getParticipantTyping().getFirstName() + ", " + usersTypingSync.get(1).getParticipantTyping().getFirstName());
                userTyping = toCDATA(userTyping);
                userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                userTyping = userTyping.replace("[/A]", "</font>");

                Spanned result = Html.fromHtml(userTyping, Html.FROM_HTML_MODE_LEGACY);

                userTypingText.setText(result);
                break;
            }
            default: {
                String names = usersTypingSync.get(0).getParticipantTyping().getFirstName() + ", " + usersTypingSync.get(1).getParticipantTyping().getFirstName();
                String userTyping = String.format(getString(R.string.more_users_typing), toCDATA(names));

                userTyping = userTyping.replace("[A]", "<font color=\'#8d8d94\'>");
                userTyping = userTyping.replace("[/A]", "</font>");

                Spanned result = Html.fromHtml(userTyping, Html.FROM_HTML_MODE_LEGACY);

                userTypingText.setText(result);
                break;
            }
        }
    }

    public void setRichLinkInfo(long msgId, AndroidMegaRichLinkMessage richLinkMessage) {
        Timber.d("setRichLinkInfo");

        int indexToChange = -1;
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while (itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if (!messageToCheck.isUploading()) {
                if (messageToCheck.getMessage().getMsgId() == msgId) {
                    indexToChange = itr.nextIndex();
                    Timber.d("Found index to change: %s", indexToChange);
                    break;
                }
            }
        }

        if (indexToChange != -1) {

            AndroidMegaChatMessage androidMsg = messages.get(indexToChange);

            androidMsg.setRichLinkMessage(richLinkMessage);

            try {
                if (adapter != null) {
                    adapter.notifyItemChanged(indexToChange + 1);
                }
            } catch (IllegalStateException e) {
                Timber.e(e, "IllegalStateException: do not update adapter");
            }

        } else {
            Timber.e("Error, rich link message not found!!");
        }
    }

    public void setRichLinkImage(long msgId) {
        Timber.d("setRichLinkImage");

        int indexToChange = -1;
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while (itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if (!messageToCheck.isUploading()) {
                if (messageToCheck.getMessage().getMsgId() == msgId) {
                    indexToChange = itr.nextIndex();
                    Timber.d("Found index to change: %s", indexToChange);
                    break;
                }
            }
        }

        if (indexToChange != -1) {

            if (adapter != null) {
                adapter.notifyItemChanged(indexToChange + 1);
            }
        } else {
            Timber.e("Error, rich link message not found!!");
        }
    }

    private String extractMegaLink(String urlIn) {
        try {
            Matcher m = Patterns.WEB_URL.matcher(urlIn);
            while (m.find()) {
                String url = Util.decodeURL(m.group());

                if (Util.matchRegexs(url, Constants.FILE_LINK_REGEXS)) {
                    return url;
                }
                if (Util.matchRegexs(url, Constants.FOLDER_LINK_REGEXS)) {
                    return url;
                }
                if (Util.matchRegexs(url, Constants.CHAT_LINK_REGEXS)) {
                    return url;
                }
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        return null;
    }

    public int checkMegaLink(MegaChatMessage msg) {

        //Check if it is a MEGA link
        if (msg.getType() != MegaChatMessage.TYPE_NORMAL || msg.getContent() == null) return -1;

        String link = extractMegaLink(msg.getContent());

        if (Util.matchRegexs(link, Constants.CHAT_LINK_REGEXS)) {
            ChatLinkInfoListener listener = new ChatLinkInfoListener(this, msg.getMsgId(), megaApi);
            megaChatApi.checkChatLink(link, listener);

            return MEGA_CHAT_LINK;
        }

        if (link == null || megaApi == null || megaApi.getRootNode() == null) return -1;

        Timber.d("The link was found");

        ChatLinkInfoListener listener = null;
        if (Util.matchRegexs(link, Constants.FILE_LINK_REGEXS)) {
            Timber.d("isFileLink");
            internalComposite.add(getPublicNodeUseCase.get(link)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((richLink, throwable) -> {
                        if (throwable == null) {
                            setRichLinkInfo(msg.getMsgId(), richLink);
                        }
                    }));

            return MEGA_FILE_LINK;
        } else {
            Timber.d("isFolderLink");
            internalComposite.add(legacyGetPublicLinkInformationUseCase.get(link, this)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((richLink, throwable) -> {
                        if (throwable == null) {
                            setRichLinkInfo(msg.getMsgId(), richLink);
                        }
                    }));

            return MEGA_FOLDER_LINK;
        }
    }

    @Override
    public void onMessageLoaded(MegaChatApiJava api, MegaChatMessage msg) {

        if (msg != null) {
            Timber.d("STATUS: %s", msg.getStatus());
            Timber.d("TEMP ID: %s", msg.getTempId());
            Timber.d("FINAL ID: %s", msg.getMsgId());
            Timber.d("TIMESTAMP: %s", msg.getTimestamp());
            Timber.d("TYPE: %s", msg.getType());

            if (messages != null) {
                Timber.d("Messages size: %s", messages.size());
            }

            if (msg.isDeleted()) {
                Timber.d("DELETED MESSAGE!!!!");
                numberToLoad--;
                return;
            }

            if (msg.isEdited()) {
                Timber.d("EDITED MESSAGE!!!!");
            }

            if (msg.getType() == MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT) {
                Timber.d("TYPE_REVOKE_NODE_ATTACHMENT MESSAGE!!!!");
                numberToLoad--;
                return;
            }

            checkMegaLink(msg);

            if (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT) {
                Timber.d("TYPE_NODE_ATTACHMENT MESSAGE!!!!");
            }

            if (msg.getStatus() == MegaChatMessage.STATUS_SERVER_REJECTED) {
                Timber.d("STATUS_SERVER_REJECTED %s", msg.getStatus());
            }

            if (msg.getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL) {

                Timber.d("STATUS_SENDING_MANUAL: Getting messages not sent!!!: %s", msg.getStatus());
                AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);

                if (msg.isEdited()) {
                    Timber.d("MESSAGE EDITED");

                    if (!noMoreNoSentMessages) {
                        Timber.d("NOT noMoreNoSentMessages");
                        addInBufferSending(androidMsg);
                    } else {
                        Timber.d("Try to recover the initial msg");
                        if (msg.getMsgId() != -1) {
                            MegaChatMessage notEdited = megaChatApi.getMessage(idChat, msg.getMsgId());
                            Timber.d("Content not edited");
                            AndroidMegaChatMessage androidMsgNotEdited = new AndroidMegaChatMessage(notEdited);
                            int returnValue = modifyMessageReceived(androidMsgNotEdited, false);
                            if (returnValue != -1) {
                                Timber.d("Message %d modified!", returnValue);
                            }
                        }

                        appendMessageAnotherMS(androidMsg);
                    }
                } else {
                    Timber.d("NOT MESSAGE EDITED");
                    int resultModify = -1;
                    if (msg.getUserHandle() == megaChatApi.getMyUserHandle()) {
                        if (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT) {
                            Timber.d("Modify my message and node attachment");

                            long idMsg = dbH.findPendingMessageByIdTempKarere(msg.getTempId());
                            Timber.d("The id of my pending message is: %s", idMsg);
                            if (idMsg != -1) {
                                resultModify = modifyAttachmentReceived(androidMsg, idMsg);
                                dbH.removePendingMessageById(idMsg);
                                if (resultModify == -1) {
                                    Timber.d("Node attachment message not in list -> resultModify -1");
                                } else {
                                    Timber.d("Modify attachment");
                                    numberToLoad--;
                                    return;
                                }
                            }
                        }
                    }

                    int returnValue = modifyMessageReceived(androidMsg, true);
                    if (returnValue != -1) {
                        Timber.d("Message %d modified!", returnValue);
                        numberToLoad--;
                        return;
                    }
                    addInBufferSending(androidMsg);
                    if (!noMoreNoSentMessages) {
                        Timber.d("NOT noMoreNoSentMessages");
                    }
                }
            } else if (msg.getStatus() == MegaChatMessage.STATUS_SENDING) {
                Timber.d("SENDING: Getting messages not sent !!!-------------------------------------------------: %s", msg.getStatus());
                AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);
                int returnValue = modifyMessageReceived(androidMsg, true);
                if (returnValue != -1) {
                    Timber.d("Message %d modified!", returnValue);
                    numberToLoad--;
                    return;
                }
                addInBufferSending(androidMsg);
                if (!noMoreNoSentMessages) {
                    Timber.d("NOT noMoreNoSentMessages");
                }
            } else {
                if (!noMoreNoSentMessages) {
                    Timber.d("First message with NORMAL status");
                    noMoreNoSentMessages = true;
                    if (!bufferSending.isEmpty()) {
                        bufferMessages.addAll(bufferSending);
                        bufferSending.clear();
                    }
                }

                AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);

                if (lastIdMsgSeen != -1) {
                    if (lastIdMsgSeen == msg.getMsgId()) {
                        Timber.d("Last message seen received!");
                        lastSeenReceived = true;
                        positionToScroll = 0;
                        Timber.d("positionToScroll: %s", positionToScroll);
                    }
                } else {
                    Timber.d("lastMessageSeen is -1");
                    lastSeenReceived = true;
                }

                if (positionToScroll >= 0) {
                    positionToScroll++;
                    Timber.d("positionToScroll:increase: %s", positionToScroll);
                }
                bufferMessages.add(androidMsg);
                Timber.d("Size of buffer: %s", bufferMessages.size());
                Timber.d("Size of messages: %s", messages.size());
            }
        } else {
            Timber.d("NULLmsg:REACH FINAL HISTORY:stateHistory %s", stateHistory);
            if (!bufferSending.isEmpty()) {
                bufferMessages.addAll(bufferSending);
                bufferSending.clear();
            }

            if (stateHistory == MegaChatApi.SOURCE_ERROR) {
                Timber.d("SOURCE_ERROR: wait to CHAT ONLINE connection");
                retryHistory = true;
            } else if (thereAreNotMoreMessages()) {
                Timber.d("SOURCE_NONE: there are no more messages");
                fullHistoryReceivedOnLoad();
            } else if (bufferMessages.size() == NUMBER_MESSAGES_TO_LOAD) {
                allMessagesRequestedAreLoaded();
            } else {
                long pendingMessagesCount = numberToLoad - bufferMessages.size();
                if (pendingMessagesCount > 0) {
                    Timber.d("Fewer messages received (%d) than asked (%d): ask for the rest of messages (%d)", bufferMessages.size(), NUMBER_MESSAGES_TO_LOAD, pendingMessagesCount);
                    askForMoreMessages(pendingMessagesCount);

                    if (thereAreNotMoreMessages()) {
                        Timber.d("SOURCE_NONE: there are no more messages");
                        fullHistoryReceivedOnLoad();
                    }
                } else {
                    allMessagesRequestedAreLoaded();
                }
            }
        }
        Timber.d("END onMessageLoaded - messages.size=%s", messages.size());
    }

    private void allMessagesRequestedAreLoaded() {
        Timber.d("All the messages asked are loaded");
        long messagesLoadedCount = bufferMessages.size() + messages.size();
        fullHistoryReceivedOnLoad();

        if (messagesLoadedCount < Math.abs(generalUnreadCount) && messagesLoadedCount < MAX_NUMBER_MESSAGES_TO_LOAD_NOT_SEEN) {
            askForMoreMessages();
        }
    }

    public boolean thereAreNotMoreMessages() {
        return stateHistory == MegaChatApi.SOURCE_NONE;
    }

    /**
     * Initiates fetching 32 messages more of the current ChatRoom.
     */
    private void askForMoreMessages() {
        askForMoreMessages(NUMBER_MESSAGES_TO_LOAD);
    }

    /**
     * Initiates fetching some messages more of the current ChatRoom.
     *
     * @param messagesCount number of messages to be fetched
     */
    private void askForMoreMessages(long messagesCount) {
        isLoadingHistory = true;
        numberToLoad = messagesCount;
        stateHistory = megaChatApi.loadMessages(idChat, (int) numberToLoad);
        getMoreHistory = false;
    }

    /**
     * Updates the loaded messages in the adapter when all the messages have been received.
     */
    public void fullHistoryReceivedOnLoad() {
        Timber.d("fullHistoryReceivedOnLoad");

        isLoadingHistory = false;
        isOpeningChat = false;

        if (!bufferMessages.isEmpty()) {
            Timber.d("Buffer size: %s", bufferMessages.size());
            loadBufferMessages();
            checkLastSeenId();

            if (lastSeenReceived && positionToScroll > 0 && positionToScroll < messages.size()) {
                Timber.d("Last message seen received");
                //Find last message
                updateLocalLastSeenId();
                scrollToMessage(isTurn ? -1 : lastIdMsgSeen);
            }

            setLastMessageSeen();
        }

        Timber.d("getMoreHistoryTRUE");
        getMoreHistory = true;

        //Load pending messages
        if (!pendingMessagesLoaded) {
            pendingMessagesLoaded = true;
            viewModel.loadPendingMessages();

            if (positionToScroll <= 0) {
                mLayoutManager.scrollToPosition(messages.size());
            }
        }

        chatRelativeLayout.setVisibility(View.VISIBLE);
        emptyLayout.setVisibility(View.GONE);
    }

    @Override
    public void onMessageReceived(MegaChatApiJava api, MegaChatMessage msg) {
        Timber.d("CHAT CONNECTION STATE: %s", api.getChatConnectionState(idChat));
        Timber.d("STATUS: %s", msg.getStatus());
        Timber.d("TEMP ID: %s", msg.getTempId());
        Timber.d("FINAL ID: %s", msg.getMsgId());
        Timber.d("TIMESTAMP: %s", msg.getTimestamp());
        Timber.d("TYPE: %s", msg.getType());

        if (msg.getType() == MegaChatMessage.TYPE_REVOKE_NODE_ATTACHMENT) {
            Timber.d("TYPE_REVOKE_NODE_ATTACHMENT MESSAGE!!!!");
            return;
        }

        if (msg.getStatus() == MegaChatMessage.STATUS_SERVER_REJECTED) {
            Timber.d("STATUS_SERVER_REJECTED: %s", msg.getStatus());
        }

        if (!msg.isManagementMessage() || msg.getType() == MegaChatMessage.TYPE_CALL_ENDED) {
            if (positionNewMessagesLayout != -1) {
                Timber.d("Layout unread messages shown: %s", generalUnreadCount);
                if (generalUnreadCount < 0) {
                    generalUnreadCount--;
                } else {
                    generalUnreadCount++;
                }

                if (adapter != null) {
                    adapter.notifyItemChanged(positionNewMessagesLayout);
                }
            }
        } else {
            int messageType = msg.getType();
            Timber.d("Message type: %s", messageType);

            switch (messageType) {
                case MegaChatMessage.TYPE_ALTER_PARTICIPANTS: {
                    if (msg.getUserHandle() == myUserHandle) {
                        Timber.d("me alter participant");
                        hideNewMessagesLayout();
                    }
                    break;
                }
                case MegaChatMessage.TYPE_PRIV_CHANGE: {
                    if (msg.getUserHandle() == myUserHandle) {
                        Timber.d("I change a privilege");
                        hideNewMessagesLayout();
                    }
                    break;
                }
                case MegaChatMessage.TYPE_CHAT_TITLE: {
                    if (msg.getUserHandle() == myUserHandle) {
                        Timber.d("I change the title");
                        hideNewMessagesLayout();
                    }
                    break;
                }
            }
        }

        if (!msgsReceived.contains(msg.getMsgId())) {
            msgsReceived.add(msg.getMsgId());
        }

        if (setAsRead) {
            markAsSeen(msg);
        }

        if (msg.getType() == MegaChatMessage.TYPE_CHAT_TITLE) {
            String newTitle = msg.getContent();
            if (newTitle != null) {
                setTitle(newTitle);
            }
        } else if (msg.getType() == MegaChatMessage.TYPE_TRUNCATE) {
            invalidateOptionsMenu();
        }

        AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);
        appendMessagePosition(androidMsg);

        if (mLayoutManager.findLastCompletelyVisibleItemPosition() == messages.size() - 1) {
            mLayoutManager.scrollToPosition(messages.size());
        } else {
            if (emojiKeyboard != null) {
                if ((emojiKeyboard.getLetterKeyboardShown() || emojiKeyboard.getEmojiKeyboardShown()) && (messages.size() == 1)) {
                    mLayoutManager.scrollToPosition(messages.size());
                }
            }

            showScrollToLastMsgButton();
        }

        checkMegaLink(msg);
    }

    public void sendToDownload(MegaNodeList nodelist) {
        Timber.d("sendToDownload");
        nodeSaver.downloadVoiceClip(nodelist);
    }

    /**
     * Upon a node is tapped, if it cannot be previewed in-app,
     * then download it first, this download will be marked as "download by tap".
     * Since it's down
     *
     * @param node Node to be downloaded.
     */
    public Unit saveNodeByTap(MegaNode node) {
        PermissionUtils.checkNotificationsPermission(this);
        nodeSaver.saveNodes(Collections.singletonList(node), true, false, false, true, true);
        return null;
    }

    @Override
    public void onReactionUpdate(MegaChatApiJava api, long msgid, String reaction, int count) {
        MegaChatMessage message = api.getMessage(idChat, msgid);
        if (adapter == null || message == null) {
            Timber.w("Message not found");
            return;
        }

        adapter.checkReactionUpdated(idChat, message, reaction, count);

        if (bottomSheetDialogFragment != null && bottomSheetDialogFragment.isAdded() && bottomSheetDialogFragment instanceof InfoReactionsBottomSheet) {
            ((InfoReactionsBottomSheet) bottomSheetDialogFragment).changeInReactionReceived(msgid, idChat, reaction, count);
        }
    }

    @Override
    public void onMessageUpdate(MegaChatApiJava api, MegaChatMessage msg) {
        Timber.d("msgID %s", msg.getMsgId());
        if (msg.isDeleted()) {
            if (adapter != null) {
                adapter.stopPlaying(msg.getMsgId());
            }

            deleteMessage(msg, false);

            if (msgsReceived.contains(msg.getMsgId())) {
                msgsReceived.remove(msg.getMsgId());
                if (unreadMsgsLayout.getVisibility() == View.VISIBLE) {
                    showScrollToLastMsgButton();
                }
            }

            return;
        }

        AndroidMegaChatMessage androidMsg = new AndroidMegaChatMessage(msg);

        if (msg.hasChanged(MegaChatMessage.CHANGE_TYPE_ACCESS)) {
            Timber.d("Change access of the message. One attachment revoked, modify message");

            if (modifyMessageReceived(androidMsg, false) == INVALID_VALUE) {
                int firstIndexShown = messages.get(0).getMessage().getMsgIndex();
                Timber.d("Modify result is -1. The first index is: " + firstIndexShown
                        + " the index of the updated message: " + msg.getMsgIndex());

                if (msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT
                        && (firstIndexShown <= msg.getMsgIndex()
                        || messages.size() < NUMBER_MESSAGES_BEFORE_LOAD)) {
                    Timber.d("Node attachment message not in list -> append");
                    AndroidMegaChatMessage msgToAppend = new AndroidMegaChatMessage(msg);
                    reinsertNodeAttachmentNoRevoked(msgToAppend);
                }
            }

            return;
        }

        if (msg.hasChanged(MegaChatMessage.CHANGE_TYPE_CONTENT)) {
            Timber.d("Change content of the message");

            if (msg.getType() == MegaChatMessage.TYPE_TRUNCATE) {
                clearHistory(androidMsg);
            } else {
                disableMultiselection();

                checkMegaLink(msg);

                if (msg.getContainsMeta() != null && msg.getContainsMeta().getType() == MegaChatContainsMeta.CONTAINS_META_GEOLOCATION) {
                    Timber.d("CONTAINS_META_GEOLOCATION");
                }

                Timber.d("resultModify: %s", modifyMessageReceived(androidMsg, false));
            }

            return;
        }

        if (msg.hasChanged(MegaChatMessage.CHANGE_TYPE_STATUS)) {
            int statusMsg = msg.getStatus();
            Timber.d("Status change: %d, Temporal ID: %d, Final ID: %d", statusMsg, msg.getTempId(), msg.getMsgId());

            if (msg.getUserHandle() == megaChatApi.getMyUserHandle()
                    && ((msg.getType() == MegaChatMessage.TYPE_NODE_ATTACHMENT)
                    || (msg.getType() == MegaChatMessage.TYPE_VOICE_CLIP))) {
                long idMsg = dbH.findPendingMessageByIdTempKarere(msg.getTempId());
                Timber.d("Modify my message and node attachment. The id of my pending message is: %s", idMsg);

                if (idMsg != INVALID_VALUE) {
                    if (modifyAttachmentReceived(androidMsg, idMsg) == INVALID_VALUE) {
                        Timber.w("Node attachment message not in list -> resultModify -1");
                    } else {
                        dbH.removePendingMessageById(idMsg);
                    }

                    if (MegaApplication.getChatManagement().isMsgToBeDelete(idMsg)) {
                        Timber.d("Message to be deleted");
                        MegaApplication.getChatManagement().removeMsgToDelete(idMsg);
                        chatC.deleteMessage(msg, idChat);
                    }
                    return;
                }
            }

            if (msg.getStatus() == MegaChatMessage.STATUS_SEEN) {
                Timber.d("STATUS_SEEN");
            } else if (msg.getStatus() == MegaChatMessage.STATUS_SERVER_RECEIVED) {
                Timber.d("STATUS_SERVER_RECEIVED");

                if (msg.getType() == MegaChatMessage.TYPE_NORMAL
                        && msg.getUserHandle() == megaChatApi.getMyUserHandle()) {
                    checkMegaLink(msg);
                }

                Timber.d("resultModify: %s", modifyMessageReceived(androidMsg, true));
                return;
            } else if (msg.getStatus() == MegaChatMessage.STATUS_SERVER_REJECTED) {
                Timber.d("STATUS_SERVER_REJECTED: %s", msg.getStatus());
                deleteMessage(msg, true);
            } else {
                Timber.d("Status: %d, Timestamp: %d, resultModify: %d", msg.getStatus(), msg.getTimestamp(), modifyMessageReceived(androidMsg, false));
                return;
            }
        }

        if (msg.hasChanged(MegaChatMessage.CHANGE_TYPE_TIMESTAMP)) {
            Timber.d("Timestamp change. ResultModify: %s", modifyMessageReceived(androidMsg, true));
        }
    }

    /**
     * Method that controls the update of the chat history depending on the retention time.
     *
     * @param msgId Message ID from which the messages are to be deleted.
     */
    private void updateHistoryByRetentionTime(long msgId) {
        if (messages != null && !messages.isEmpty()) {
            for (AndroidMegaChatMessage message : messages) {
                if (message != null && message.getMessage() != null &&
                        message.getMessage().getMsgId() == msgId) {

                    int position = messages.indexOf(message);

                    if (position < messages.size() - 1) {
                        List<AndroidMegaChatMessage> messagesCopy = new ArrayList<>(messages);
                        messages.clear();

                        for (int i = position + 1; i < messagesCopy.size(); i++) {
                            messages.add(messagesCopy.get(i));
                        }
                    } else {
                        messages.clear();
                    }

                    updateMessages();
                    break;
                }
            }
        }
    }

    @Override
    public void onHistoryTruncatedByRetentionTime(MegaChatApiJava api, MegaChatMessage msg) {
        if (msg == null) {
            return;
        }

        updateHistoryByRetentionTime(msg.getMsgId());
    }

    private void disableMultiselection() {
        if (adapter == null || !adapter.isMultipleSelect())
            return;

        finishMultiselectionMode();
    }

    @Override
    public void onHistoryReloaded(MegaChatApiJava api, MegaChatRoom chat) {
        Timber.d("onHistoryReloaded");
        cleanBuffers();
        invalidateOptionsMenu();
        Timber.d("Load new history");

        long unread = chatRoom.getUnreadCount();
        generalUnreadCount = unread;
        lastSeenReceived = unread == 0;

        if (unread == 0) {
            lastIdMsgSeen = MEGACHAT_INVALID_HANDLE;
            Timber.d("loadMessages unread is 0");
        } else {
            lastIdMsgSeen = megaChatApi.getLastMessageSeenId(idChat);
            if (lastIdMsgSeen != -1) {
                Timber.d("Id of last message seen: %s", lastIdMsgSeen);
            } else {
                Timber.e("Error the last message seen shouldn't be NULL");
            }
        }
    }

    public void deleteMessage(MegaChatMessage msg, boolean rejected) {
        int indexToChange = -1;

        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while (itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();
            Timber.d("Index: %s", itr.nextIndex());

            if (!messageToCheck.isUploading()) {
                if (rejected) {
                    if (messageToCheck.getMessage().getTempId() == msg.getTempId()) {
                        indexToChange = itr.nextIndex();
                        break;
                    }
                } else {
                    if (messageToCheck.getMessage().getMsgId() == msg.getMsgId()
                            || (msg.getTempId() != -1
                            && messageToCheck.getMessage().getTempId() == msg.getTempId())) {
                        indexToChange = itr.nextIndex();
                        break;
                    }
                }
            }
        }

        if (indexToChange != -1) {
            messages.remove(indexToChange);
            Timber.d("Removed index: %d positionNewMessagesLayout: %d messages size: %d", indexToChange, positionNewMessagesLayout, messages.size());
            if (positionNewMessagesLayout <= indexToChange) {
                if (generalUnreadCount == 1 || generalUnreadCount == -1) {
                    Timber.d("Reset generalUnread:Position where new messages layout is show: %s", positionNewMessagesLayout);
                    generalUnreadCount = 0;
                    lastIdMsgSeen = -1;
                } else {
                    Timber.d("Decrease generalUnread:Position where new messages layout is show: %s", positionNewMessagesLayout);
                    generalUnreadCount--;
                }
                adapter.notifyItemChanged(positionNewMessagesLayout);
            }

            if (!messages.isEmpty()) {
                //Update infoToShow of the next message also
                if (indexToChange == 0) {
                    messages.get(indexToChange).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                    //Check if there is more messages and update the following one
                    if (messages.size() > 1) {
                        adjustInfoToShow(indexToChange + 1);
                        setShowAvatar(indexToChange + 1);
                    }
                } else {
                    //Not first element
                    if (indexToChange == messages.size()) {
                        Timber.d("The last message removed, do not check more messages");
                        setShowAvatar(indexToChange - 1);
                    } else {
                        adjustInfoToShow(indexToChange);
                        setShowAvatar(indexToChange);
                        setShowAvatar(indexToChange - 1);
                    }
                }
            }
            adapter.removeMessage(indexToChange + 1, messages);
            disableMultiselection();
        } else {
            Timber.w("index to change not found");
        }
    }

    public int modifyAttachmentReceived(AndroidMegaChatMessage msg, long idPendMsg) {
        Timber.d("ID: %d, tempID: %d, Status: %d", msg.getMessage().getMsgId(), msg.getMessage().getTempId(), msg.getMessage().getStatus());
        int indexToChange = -1;
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while (itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if (messageToCheck.getPendingMessage() != null) {
                Timber.d("Pending ID: %d, other: %d", messageToCheck.getPendingMessage().getId(), idPendMsg);

                if (messageToCheck.getPendingMessage().getId() == idPendMsg) {
                    indexToChange = itr.nextIndex();
                    Timber.d("Found index to change: %s", indexToChange);
                    break;
                }
            }
        }

        if (indexToChange != -1) {

            Timber.d("INDEX change, need to reorder");
            messages.remove(indexToChange);
            Timber.d("Removed index: %s", indexToChange);
            Timber.d("Messages size: %s", messages.size());
            adapter.removeMessage(indexToChange + 1, messages);

            int scrollToP = appendMessagePosition(msg);
            if (scrollToP != -1) {
                if (msg.getMessage().getStatus() == MegaChatMessage.STATUS_SERVER_RECEIVED) {
                    Timber.d("Need to scroll to position: %s", indexToChange);
                    final int indexToScroll = scrollToP + 1;
                    mLayoutManager.scrollToPositionWithOffset(indexToScroll, scaleHeightPx(20, getOutMetrics()));

                }
            }
        } else {
            Timber.e("Error, id pending message message not found!!");
        }
        Timber.d("Index modified: %s", indexToChange);
        return indexToChange;
    }

    /**
     * Checks on the provided list if the message to update exists.
     * If so, returns its index on list.
     *
     * @param msg         The updated AndroidMegaChatMessage.
     * @param checkTempId True if has to check the temp id instead of final id.
     * @param itr         ListIterator containing the list of messages to check.
     * @return The index to change if successful, INVALID_POSITION otherwise.
     */
    private int getIndexToUpdate(AndroidMegaChatMessage msg, boolean checkTempId, ListIterator<AndroidMegaChatMessage> itr) {
        // Iterate in reverse.
        while (itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if (!messageToCheck.isUploading()) {
                Timber.d("Checking with Msg ID: %d and Msg TEMP ID: %d", messageToCheck.getMessage().getMsgId(), messageToCheck.getMessage().getTempId());

                if (checkTempId && msg.getMessage().getTempId() != MEGACHAT_INVALID_HANDLE
                        && msg.getMessage().getTempId() == messageToCheck.getMessage().getTempId()) {
                    Timber.d("Modify received message with idTemp");
                    return itr.nextIndex();
                } else if (msg.getMessage().getMsgId() != MEGACHAT_INVALID_HANDLE
                        && msg.getMessage().getMsgId() == messageToCheck.getMessage().getMsgId()) {
                    Timber.d("modifyMessageReceived");
                    return itr.nextIndex();
                }
            } else {
                Timber.d("This message is uploading");
            }
        }

        return INVALID_POSITION;
    }

    /**
     * Modifies a message on UI (messages list and adapter), on bufferMessages list
     * or on bufferSending list, if it has been already loaded.
     *
     * @param msg         The updated AndroidMegaChatMessage.
     * @param checkTempId True if has to check the temp id instead of final id.
     * @return The index to change if successful, INVALID_POSITION otherwise.
     */
    public int modifyMessageReceived(AndroidMegaChatMessage msg, boolean checkTempId) {
        Timber.d("Msg ID: %dMsg TEMP ID: %dMsg status: %d", msg.getMessage().getMsgId(), msg.getMessage().getTempId(), msg.getMessage().getStatus());

        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());
        int indexToChange = getIndexToUpdate(msg, checkTempId, itr);


        if (indexToChange == INVALID_POSITION) {
            itr = bufferMessages.listIterator(bufferMessages.size());
            indexToChange = getIndexToUpdate(msg, checkTempId, itr);

            if (indexToChange != INVALID_POSITION) {
                bufferMessages.set(indexToChange, msg);
                return indexToChange;
            }
        }

        if (indexToChange == INVALID_POSITION) {
            itr = bufferSending.listIterator(bufferSending.size());
            indexToChange = getIndexToUpdate(msg, checkTempId, itr);

            if (indexToChange != INVALID_POSITION) {
                bufferSending.set(indexToChange, msg);
                return indexToChange;
            }
        }

        Timber.d("Index to change = %s", indexToChange);
        if (indexToChange == INVALID_POSITION) {
            return indexToChange;
        }

        AndroidMegaChatMessage messageToUpdate = messages.get(indexToChange);
        if (isItSameMsg(messageToUpdate.getMessage(), msg.getMessage())) {
            Timber.d("The internal index not change");

            if (msg.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL) {
                Timber.d("Modified a MANUAl SENDING msg");
                //Check the message to change is not the last one
                int lastI = messages.size() - 1;
                if (indexToChange < lastI) {
                    //Check if there is already any MANUAL_SENDING in the queue
                    AndroidMegaChatMessage previousMessage = messages.get(lastI);
                    if (previousMessage.isUploading()) {
                        Timber.d("Previous message is uploading");
                    } else {
                        if (previousMessage.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL) {
                            Timber.d("More MANUAL SENDING in queue");
                            Timber.d("Removed index: %s", indexToChange);
                            messages.remove(indexToChange);
                            appendMessageAnotherMS(msg);
                            adapter.notifyDataSetChanged();
                            return indexToChange;
                        }
                    }
                }
            }

            Timber.d("Modified message keep going");
            messages.set(indexToChange, msg);

            //Update infoToShow also
            if (indexToChange == 0) {
                messages.get(indexToChange).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                messages.get(indexToChange).setShowAvatar(true);
            } else {
                //Not first element
                adjustInfoToShow(indexToChange);
                setShowAvatar(indexToChange);

                //Create adapter
                if (adapter == null) {
                    adapter = new MegaChatAdapter(this, chatRoom, messages,
                            messagesPlaying, removedMessages, listView,
                            getAvatarUseCase, getNodeUseCase, viewModel, megaApi, megaChatApi, dbH);

                    adapter.setHasStableIds(true);
                    listView.setAdapter(adapter);
                } else {
                    adapter.modifyMessage(messages, indexToChange + 1);
                }
            }
        } else {
            Timber.d("INDEX change, need to reorder");
            messages.remove(indexToChange);
            Timber.d("Removed index: %s", indexToChange);
            Timber.d("Messages size: %s", messages.size());
            adapter.removeMessage(indexToChange + 1, messages);
            int scrollToP = appendMessagePosition(msg);
            if (scrollToP != -1) {
                if (msg.getMessage().getStatus() == MegaChatMessage.STATUS_SERVER_RECEIVED) {
                    mLayoutManager.scrollToPosition(scrollToP + 1);
                }
            }
            Timber.d("Messages size: %s", messages.size());
        }

        return indexToChange;
    }

    public void modifyLocationReceived(AndroidMegaChatMessage editedMsg, boolean hasTempId) {
        Timber.d("Edited Msg ID: %d, Old Msg ID: %d", editedMsg.getMessage().getMsgId(), messageToEdit.getMsgId());
        Timber.d("Edited Msg TEMP ID: %d, Old Msg TEMP ID: %d", editedMsg.getMessage().getTempId(), messageToEdit.getTempId());
        Timber.d("Edited Msg status: %d, Old Msg status: %d", editedMsg.getMessage().getStatus(), messageToEdit.getStatus());
        int indexToChange = -1;
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        boolean editedMsgHasTempId = editedMsg.getMessage().getTempId() != -1;

        // Iterate in reverse.
        while (itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();
            Timber.d("Index: %s", itr.nextIndex());

            if (!messageToCheck.isUploading()) {
                Timber.d("Checking with Msg ID: %d and Msg TEMP ID: %d", messageToCheck.getMessage().getMsgId(), messageToCheck.getMessage().getTempId());
                if (hasTempId) {
                    if (editedMsgHasTempId && messageToCheck.getMessage().getTempId() == editedMsg.getMessage().getTempId()) {
                        indexToChange = itr.nextIndex();
                        break;
                    } else if (!editedMsgHasTempId && messageToCheck.getMessage().getTempId() == editedMsg.getMessage().getMsgId()) {
                        indexToChange = itr.nextIndex();
                        break;
                    }
                } else {
                    if (editedMsgHasTempId && messageToCheck.getMessage().getMsgId() == editedMsg.getMessage().getTempId()) {
                        indexToChange = itr.nextIndex();
                        break;
                    } else if (!editedMsgHasTempId && messageToCheck.getMessage().getMsgId() == editedMsg.getMessage().getMsgId()) {
                        indexToChange = itr.nextIndex();
                        break;
                    }
                }
            } else {
                Timber.d("This message is uploading");
            }
        }

        Timber.d("Index to change = %s", indexToChange);
        if (indexToChange != -1) {

            AndroidMegaChatMessage messageToUpdate = messages.get(indexToChange);
            if (messageToUpdate.getMessage().getMsgIndex() == editedMsg.getMessage().getMsgIndex()) {
                Timber.d("The internal index not change");

                if (editedMsg.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL) {
                    Timber.d("Modified a MANUAl SENDING msg");
                    //Check the message to change is not the last one
                    int lastI = messages.size() - 1;
                    if (indexToChange < lastI) {
                        //Check if there is already any MANUAL_SENDING in the queue
                        AndroidMegaChatMessage previousMessage = messages.get(lastI);
                        if (previousMessage.isUploading()) {
                            Timber.d("Previous message is uploading");
                        } else if (previousMessage.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL) {
                            Timber.d("More MANUAL SENDING in queue");
                            Timber.d("Removed index: %s", indexToChange);
                            messages.remove(indexToChange);
                            appendMessageAnotherMS(editedMsg);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }

                Timber.d("Modified message keep going");
                messages.set(indexToChange, editedMsg);

                //Update infoToShow also
                if (indexToChange == 0) {
                    messages.get(indexToChange).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                    messages.get(indexToChange).setShowAvatar(true);
                } else {
                    //Not first element
                    adjustInfoToShow(indexToChange);
                    setShowAvatar(indexToChange);

                    //Create adapter
                    if (adapter == null) {
                        createAdapter();
                    } else {
                        adapter.modifyMessage(messages, indexToChange + 1);
                    }
                }
            } else {
                Timber.d("INDEX change, need to reorder");
                messages.remove(indexToChange);
                Timber.d("Removed index: %s", indexToChange);
                Timber.d("Messages size: %s", messages.size());
                adapter.removeMessage(indexToChange + 1, messages);
                int scrollToP = appendMessagePosition(editedMsg);
                if (scrollToP != -1 && editedMsg.getMessage().getStatus() == MegaChatMessage.STATUS_SERVER_RECEIVED) {
                    mLayoutManager.scrollToPosition(scrollToP + 1);
                }
                Timber.d("Messages size: %s", messages.size());
            }
        } else {
            Timber.e("Error, id temp message not found!! indexToChange == -1");
        }
    }

    public void loadBufferMessages() {
        Timber.d("loadBufferMessages");
        ListIterator<AndroidMegaChatMessage> itr = bufferMessages.listIterator();
        while (itr.hasNext()) {
            int currentIndex = itr.nextIndex();
            AndroidMegaChatMessage messageToShow = itr.next();
            loadMessage(messageToShow, currentIndex);
        }

        //Create adapter
        if (adapter == null) {
            createAdapter();
        } else {
            adapter.loadPreviousMessages(messages, bufferMessages.size());

            Timber.d("addMessage: %s", messages.size());
            updateActionModeTitle();
            reDoTheSelectionAfterRotation();
            recoveredSelectedPositions = null;
        }

        Timber.d("AFTER updateMessagesLoaded: %d messages in list", messages.size());

        bufferMessages.clear();
    }

    public void clearHistory(AndroidMegaChatMessage androidMsg) {
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        // Iterate in reverse.
        while (itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if (messageToCheck.isUploading()) {
                // Remove pending uploading messages.
                megaApi.cancelTransferByTag(messageToCheck.getPendingMessage().getTransferTag());
            } else {
                break;
            }
        }

        Timber.d("Clear all messages");
        messages.clear();
        messages.add(androidMsg);
        androidMsg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
        updateMessages();
    }

    /**
     * Method to control the visibility of the select option.
     */
    private void checkSelectOption() {
        if (selectMenuItem == null)
            return;

        boolean selectableMessages = false;
        if ((messages != null && chatRoom != null && !viewModel.isJoiningOrLeaving() && messages.size() > 0)) {
            for (AndroidMegaChatMessage msg : messages) {
                if (msg == null || msg.getMessage() == null) {
                    continue;
                }

                switch (msg.getMessage().getType()) {
                    case MegaChatMessage.TYPE_CONTAINS_META:
                    case MegaChatMessage.TYPE_NORMAL:
                    case MegaChatMessage.TYPE_NODE_ATTACHMENT:
                    case MegaChatMessage.TYPE_VOICE_CLIP:
                    case MegaChatMessage.TYPE_CONTACT_ATTACHMENT:
                        selectableMessages = true;
                        break;
                }
            }
        }
        selectMenuItem.setVisible(selectableMessages);
    }


    private void updateMessages() {
        checkSelectOption();
        adapter.setMessages(messages);
    }

    private void onPendingMessageLoaded(PendingMessage pendingMessage) {
        if (pendingMessage.getState() == PendingMessageState.COMPRESSING.getValue()) {
            if (isServiceRunning(ChatUploadService.class)) {
                startService(new Intent(this, ChatUploadService.class)
                        .setAction(ACTION_CHECK_COMPRESSING_MESSAGE)
                        .putExtra(CHAT_ID, pendingMessage.getChatId())
                        .putExtra(INTENT_EXTRA_PENDING_MESSAGE_ID, pendingMessage.getId())
                        .putExtra(INTENT_EXTRA_KEY_FILE_NAME, pendingMessage.getName()));
            } else {
                retryPendingMessage(pendingMessage);
                return;
            }
        }

        appendMessagePosition(new AndroidMegaChatMessage(pendingMessage, true));
    }

    public void loadMessage(AndroidMegaChatMessage messageToShow, int currentIndex) {
        messageToShow.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
        messages.add(0, messageToShow);

        if (messages.size() > 1) {
            adjustInfoToShow(1);
        }

        setShowAvatar(0);

    }

    public void appendMessageAnotherMS(AndroidMegaChatMessage msg) {
        Timber.d("appendMessageAnotherMS");
        messages.add(msg);
        int lastIndex = messages.size() - 1;

        if (lastIndex == 0) {
            messages.get(lastIndex).setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
        } else {
            adjustInfoToShow(lastIndex);
        }

        //Create adapter
        if (adapter == null) {
            Timber.d("Create adapter");
            createAdapter();
        } else {

            if (lastIndex == 0) {
                Timber.d("Arrives the first message of the chat");
                updateMessages();
            } else {
                adapter.addMessage(messages, lastIndex + 1);
            }
        }
    }

    public int reinsertNodeAttachmentNoRevoked(AndroidMegaChatMessage msg) {
        Timber.d("reinsertNodeAttachmentNoRevoked");
        int lastIndex = messages.size() - 1;
        Timber.d("Last index: %s", lastIndex);
        if (messages.size() == -1) {
            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
            messages.add(msg);
        } else {
            Timber.d("Finding where to append the message");
            while (messages.get(lastIndex).getMessage().getMsgIndex() > msg.getMessage().getMsgIndex()) {
                Timber.d("Last index: %s", lastIndex);
                lastIndex--;
                if (lastIndex == -1) {
                    break;
                }
            }
            Timber.d("Last index: %s", lastIndex);
            lastIndex++;
            Timber.d("Append in position: %s", lastIndex);
            messages.add(lastIndex, msg);
            adjustInfoToShow(lastIndex);
            int nextIndex = lastIndex + 1;
            if (nextIndex <= messages.size() - 1) {
                adjustInfoToShow(nextIndex);
            }
            int previousIndex = lastIndex - 1;
            if (previousIndex >= 0) {
                adjustInfoToShow(previousIndex);
            }
        }

        //Create adapter
        if (adapter == null) {
            Timber.d("Create adapter");
            createAdapter();
        } else {
            if (lastIndex < 0) {
                Timber.d("Arrives the first message of the chat");
                updateMessages();
            } else {
                adapter.addMessage(messages, lastIndex + 1);
            }
        }
        return lastIndex;
    }

    public int appendMessagePosition(AndroidMegaChatMessage msg) {
        Timber.d("appendMessagePosition: %d messages", messages.size());
        int lastIndex = messages.size() - 1;
        if (messages.size() == 0) {
            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
            msg.setShowAvatar(true);
            messages.add(msg);
        } else {
            Timber.d("Finding where to append the message");

            if (msg.isUploading()) {
                lastIndex++;
                Timber.d("The message is uploading add to index: %dwith state: %d", lastIndex, msg.getPendingMessage().getState());
            } else {
                Timber.d("Status of message: %s", msg.getMessage().getStatus());
                if (lastIndex >= 0) {
                    while (messages.get(lastIndex).isUploading()) {
                        Timber.d("One less index is uploading");
                        lastIndex--;
                        if (lastIndex == -1) {
                            break;
                        }
                    }
                }
                if (lastIndex >= 0) {
                    while (messages.get(lastIndex).getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL) {
                        Timber.d("One less index is MANUAL SENDING");
                        lastIndex--;
                        if (lastIndex == -1) {
                            break;
                        }
                    }
                }
                if (lastIndex >= 0) {
                    if (msg.getMessage().getStatus() == MegaChatMessage.STATUS_SERVER_RECEIVED || msg.getMessage().getStatus() == MegaChatMessage.STATUS_NOT_SEEN) {
                        while (messages.get(lastIndex).getMessage().getStatus() == MegaChatMessage.STATUS_SENDING) {
                            Timber.d("One less index");
                            lastIndex--;
                            if (lastIndex == -1) {
                                break;
                            }
                        }
                    }
                }

                lastIndex++;
                Timber.d("Append in position: %s", lastIndex);
            }
            if (lastIndex >= 0) {
                messages.add(lastIndex, msg);
                adjustInfoToShow(lastIndex);
                msg.setShowAvatar(true);
                if (!messages.get(lastIndex).isUploading()) {
                    int nextIndex = lastIndex + 1;
                    if (nextIndex < messages.size()) {
                        if (messages.get(nextIndex) != null) {
                            if (messages.get(nextIndex).isUploading()) {
                                adjustInfoToShow(nextIndex);
                            }
                        }
                    }
                }
                if (lastIndex > 0) {
                    setShowAvatar(lastIndex - 1);
                }
            }
        }

        //Create adapter
        if (adapter == null) {
            Timber.d("Create adapter");
            createAdapter();
        } else {
            Timber.d("Update adapter with last index: %s", lastIndex);
            if (lastIndex < 0) {
                Timber.d("Arrives the first message of the chat");
                updateMessages();
            } else {
                adapter.addMessage(messages, lastIndex + 1);
                adapter.notifyItemChanged(lastIndex);
            }
        }
        return lastIndex;
    }

    public int adjustInfoToShow(int index) {
        Timber.d("Index: %s", index);

        AndroidMegaChatMessage msg = messages.get(index);

        long userHandleToCompare = -1;
        long previousUserHandleToCompare = -1;

        if (msg.isUploading()) {
            userHandleToCompare = myUserHandle;
        } else {

            if ((msg.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (msg.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                userHandleToCompare = msg.getMessage().getHandleOfAction();
            } else {
                userHandleToCompare = msg.getMessage().getUserHandle();
            }
        }

        if (index == 0) {
            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
        } else {
            AndroidMegaChatMessage previousMessage = messages.get(index - 1);

            if (previousMessage.isUploading()) {

                Timber.d("The previous message is uploading");
                if (msg.isUploading()) {
                    Timber.d("The message is also uploading");
                    if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage.getPendingMessage().getUploadTimestamp()) == 0) {
                        //Same date
                        if (compareTime(msg.getPendingMessage().getUploadTimestamp(), previousMessage.getPendingMessage().getUploadTimestamp()) == 0) {
                            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);
                        } else {
                            //Different minute
                            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                        }
                    } else {
                        //Different date
                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                    }
                } else {
                    if (compareDate(msg.getMessage().getTimestamp(), previousMessage.getPendingMessage().getUploadTimestamp()) == 0) {
                        //Same date
                        if (compareTime(msg.getMessage().getTimestamp(), previousMessage.getPendingMessage().getUploadTimestamp()) == 0) {
                            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);
                        } else {
                            //Different minute
                            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                        }
                    } else {
                        //Different date
                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                    }
                }
            } else {
                Timber.d("The previous message is NOT uploading");

                if (userHandleToCompare == myUserHandle) {
                    Timber.d("MY message!!");

                    if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                        previousUserHandleToCompare = previousMessage.getMessage().getHandleOfAction();
                    } else {
                        previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();
                    }

                    if (previousUserHandleToCompare == myUserHandle) {
                        Timber.d("Last message and previous is mine");
                        //The last two messages are mine
                        if (msg.isUploading()) {
                            Timber.d("The msg to append is uploading");
                            if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);
                                } else {
                                    //Different minute
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        } else {
                            if (compareDate(msg, previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg, previousMessage) == 0) {
                                    if ((msg.getMessage().isManagementMessage())) {
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                    } else {
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);
                                    }
                                } else {
                                    //Different minute
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }

                    } else {
                        //The last message is mine, the previous not
                        Timber.d("Last message is mine, NOT previous");
                        if (msg.isUploading()) {
                            Timber.d("The msg to append is uploading");
                            if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        } else {
                            if (compareDate(msg, previousMessage) == 0) {
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                    }

                } else {
                    Timber.d("NOT MY message!! - CONTACT");

                    if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                        previousUserHandleToCompare = previousMessage.getMessage().getHandleOfAction();
                    } else {
                        previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();
                    }

                    if (previousUserHandleToCompare == userHandleToCompare) {
                        //The last message is also a contact's message
                        if (msg.isUploading()) {
                            if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                    Timber.d("Add with show nothing - same userHandle");
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);

                                } else {
                                    //Different minute
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        } else {

                            if (compareDate(msg, previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg, previousMessage) == 0) {
                                    if ((msg.getMessage().isManagementMessage())) {
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                    } else {
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);
                                    }
                                } else {
                                    //Different minute
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                }
                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }

                    } else {
                        //The last message is from contact, the previous not
                        Timber.d("Different user handle");
                        if (msg.isUploading()) {
                            if (compareDate(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                //Same date
                                if (compareTime(msg.getPendingMessage().getUploadTimestamp(), previousMessage) == 0) {
                                    if (previousUserHandleToCompare == myUserHandle) {
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                    } else {
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_NOTHING);
                                    }

                                } else {
                                    //Different minute
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                }

                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        } else {
                            if (compareDate(msg, previousMessage) == 0) {
                                if (compareTime(msg, previousMessage) == 0) {
                                    if (previousUserHandleToCompare == myUserHandle) {
                                        msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                    } else {
                                        if ((msg.getMessage().isManagementMessage())) {
                                            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                        } else {
                                            msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                        }
                                    }

                                } else {
                                    //Different minute
                                    msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_TIME);
                                }

                            } else {
                                //Different date
                                msg.setInfoToShow(AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL);
                            }
                        }
                    }
                }

            }
        }
        return msg.getInfoToShow();
    }

    public void setShowAvatar(int index) {
        Timber.d("Index: %s", index);

        AndroidMegaChatMessage msg = messages.get(index);

        long userHandleToCompare = -1;
        long previousUserHandleToCompare = -1;

        if (msg.isUploading()) {
            msg.setShowAvatar(false);
            return;
        }

        if (userHandleToCompare == myUserHandle) {
            Timber.d("MY message!!");
        } else {
            Timber.d("Contact message");
            if ((msg.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (msg.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                userHandleToCompare = msg.getMessage().getHandleOfAction();
            } else {
                userHandleToCompare = msg.getMessage().getUserHandle();
            }
            Timber.d("userHandleTocompare: %s", userHandleToCompare);
            AndroidMegaChatMessage previousMessage = null;
            if (messages.size() - 1 > index) {
                previousMessage = messages.get(index + 1);
                if (previousMessage == null) {
                    msg.setShowAvatar(true);
                    Timber.w("Previous message is null");
                    return;
                }
                if (previousMessage.isUploading()) {
                    msg.setShowAvatar(true);
                    Timber.d("Previous is uploading");
                    return;
                }

                if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS)) {
                    previousUserHandleToCompare = previousMessage.getMessage().getHandleOfAction();
                } else {
                    previousUserHandleToCompare = previousMessage.getMessage().getUserHandle();
                }

                Timber.d("previousUserHandleToCompare: %s", previousUserHandleToCompare);

                if ((previousMessage.getMessage().getType() == MegaChatMessage.TYPE_CALL_ENDED) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_CALL_STARTED) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_PRIV_CHANGE) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_ALTER_PARTICIPANTS) || (previousMessage.getMessage().getType() == MegaChatMessage.TYPE_CHAT_TITLE)) {
                    msg.setShowAvatar(true);
                    Timber.d("Set: %s", true);
                } else {
                    if (previousUserHandleToCompare == userHandleToCompare) {
                        msg.setShowAvatar(false);
                        Timber.d("Set: %s", false);
                    } else {
                        msg.setShowAvatar(true);
                        Timber.d("Set: %s", true);
                    }
                }
            } else {
                Timber.w("No previous message");
                msg.setShowAvatar(true);
            }
        }
    }

    public void showMsgNotSentPanel(AndroidMegaChatMessage message, int position) {
        Timber.d("Position: %s", position);

        selectedPosition = position;

        if (message == null || isBottomSheetDialogShown(bottomSheetDialogFragment)) return;

        selectedMessageId = message.getMessage().getRowId();
        Timber.d("Temporal id of MS message: %s", message.getMessage().getTempId());
        bottomSheetDialogFragment = new MessageNotSentBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showUploadingAttachmentBottomSheet(AndroidMegaChatMessage message, int position) {
        if (message == null || message.getPendingMessage() == null
                || message.getPendingMessage().getState() == PendingMessageState.COMPRESSING.getValue()
                || isBottomSheetDialogShown(bottomSheetDialogFragment)) {
            return;
        }

        selectedPosition = position;
        selectedMessageId = message.getPendingMessage().getId();

        bottomSheetDialogFragment = new PendingMessageBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void showReactionBottomSheet(AndroidMegaChatMessage message, int position, String reaction) {
        if (message == null || message.getMessage() == null)
            return;

        selectedPosition = position;
        hideBottomSheet();
        selectedMessageId = message.getMessage().getMsgId();

        if (reaction == null) {
            bottomSheetDialogFragment = new ReactionsBottomSheet();
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        } else if (chatRoom != null && !chatRoom.isPreview() && chatRoom.getOwnPrivilege() != MegaChatRoom.PRIV_RM) {
            bottomSheetDialogFragment = new InfoReactionsBottomSheet(this, reaction);
            bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
        }
    }

    public void hideBottomSheet() {
        if (isBottomSheetDialogShown(bottomSheetDialogFragment)) {
            bottomSheetDialogFragment.dismissAllowingStateLoss();
        }
    }

    /**
     * Method to show the bottom panel
     */
    public void showChatRoomToolbarByPanel() {
        if (isBottomSheetDialogShown(bottomSheetDialogFragment)) {
            return;
        }

        if (emojiKeyboard != null) {
            emojiKeyboard.hideBothKeyboard(this);
        }

        bottomSheetDialogFragment = new ChatRoomToolbarBottomSheetDialogFragment();
        bottomSheetDialogFragment.show(getSupportFragmentManager(),
                bottomSheetDialogFragment.getTag());
    }

    private void showGeneralChatMessageBottomSheet(AndroidMegaChatMessage message, int position) {
        selectedPosition = position;

        if (message == null || isBottomSheetDialogShown(bottomSheetDialogFragment))
            return;

        if (emojiKeyboard != null) {
            emojiKeyboard.hideBothKeyboard(this);
        }

        selectedMessageId = message.getMessage().getMsgId();

        if (MegaApplication.getInstance().getMegaChatApi().getMessage(idChat, selectedMessageId) == null) {
            if (!viewModel.isConnected()) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_server_connection_problem), MEGACHAT_INVALID_HANDLE);
            }
            return;
        }
        bottomSheetDialogFragment = new GeneralChatMessageBottomSheet();
        bottomSheetDialogFragment.show(getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }

    public void removeMsgNotSent() {
        Timber.d("Selected position: %s", selectedPosition);
        messages.remove(selectedPosition);
        adapter.removeMessage(selectedPosition, messages);
    }

    public void updatingRemovedMessage(MegaChatMessage message) {
        for (int i = 0; i < messages.size(); i++) {
            MegaChatMessage messageToCompare = messages.get(i).getMessage();
            if (messageToCompare != null) {
                if ((message.getMsgId() != MEGACHAT_INVALID_HANDLE && message.getMsgId() == messageToCompare.getMsgId()) ||
                        (message.getTempId() != MEGACHAT_INVALID_HANDLE && message.getTempId() == messageToCompare.getTempId())) {
                    RemovedMessage msg = new RemovedMessage(messageToCompare.getTempId(), messageToCompare.getMsgId());
                    removedMessages.add(msg);
                    adapter.notifyItemChanged(i + 1);
                    break;
                }
            }
        }
    }

    /**
     * Removes a pending message from UI and DB if exists.
     *
     * @param id The identifier of the pending message.
     */
    public void removePendingMsg(long id) {
        removePendingMsg(dbH.findPendingMessageById(id));
    }

    /**
     * Removes a pending message from UI and DB if exists.
     *
     * @param pMsg The pending message.
     */
    public void removePendingMsg(PendingMessage pMsg) {
        if (pMsg == null || pMsg.getState() == PendingMessageState.SENT.getValue()) {
            showSnackbar(SNACKBAR_TYPE, getString(R.string.error_message_already_sent), MEGACHAT_INVALID_HANDLE);
            return;
        }

        if ((pMsg.getState() == PendingMessageState.UPLOADING.getValue() || pMsg.getState() == PendingMessageState.ATTACHING.getValue())
                && pMsg.getTransferTag() != INVALID_ID) {
            MegaApplication.getChatManagement().setPendingMessageToBeCancelled(pMsg.getTransferTag(), pMsg.getId());
            megaApi.cancelTransferByTag(pMsg.getTransferTag(), this);
            return;
        }

        removePendingMessageFromDbHAndUI(pMsg.getId());
    }

    /**
     * Gets a pending message position from its id.
     *
     * @param pendingMsgId Identifier of the pending message.
     * @return The position of the pending message if exist, INVALID_POSITION otherwise.
     */
    public int findPendingMessagePosition(long pendingMsgId) {
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        while (itr.hasPrevious()) {
            MegaChatMessage message = itr.previous().getMessage();
            if (message != null && pendingMsgId == message.getTempId()) {
                return itr.nextIndex();
            }
        }

        return INVALID_POSITION;
    }

    /**
     * Updates the UI of a pending message.
     *
     * @param pendingMsg The pending message to update.
     */
    private void updatePendingMessage(PendingMessage pendingMsg) {
        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

        while (itr.hasPrevious()) {
            AndroidMegaChatMessage messageToCheck = itr.previous();

            if (messageToCheck.getPendingMessage() != null
                    && messageToCheck.getPendingMessage().getId() == pendingMsg.getId()) {
                int indexToChange = itr.nextIndex();
                Timber.d("Found index to update: %s", indexToChange);

                messages.set(indexToChange, new AndroidMegaChatMessage(pendingMsg,
                        pendingMsg.getState() >= PendingMessageState.PREPARING.getValue()
                                && pendingMsg.getState() <= PendingMessageState.COMPRESSING.getValue()));

                adapter.modifyMessage(messages, indexToChange + 1);
                break;
            }
        }
    }

    public void showSnackbar(int type, String s, long idChat, String emailUser) {
        showSnackbar(type, fragmentContainer, null, s, idChat, emailUser);
    }

    public void removeProgressDialog() {
        if (statusDialog != null) {
            try {
                statusDialog.dismiss();
            } catch (Exception ex) {
            }
        }
    }

    public void startConversation(long handle) {
        Timber.d("Handle: %s", handle);
        MegaChatRoom chat = megaChatApi.getChatRoomByUser(handle);
        MegaChatPeerList peers = MegaChatPeerList.createInstance();
        if (chat == null) {
            Timber.d("No chat, create it!");
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
            megaChatApi.createChat(false, peers, this);
        } else {
            Timber.d("There is already a chat, open it!");
            Intent intentOpenChat = new Intent(this, ChatActivity.class);
            intentOpenChat.setAction(ACTION_CHAT_SHOW_MESSAGES);
            intentOpenChat.putExtra(CHAT_ID, chat.getChatId());
            this.startActivity(intentOpenChat);
        }
    }

    public void startGroupConversation(ArrayList<Long> userHandles) {
        Timber.d("startGroupConversation");

        MegaChatPeerList peers = MegaChatPeerList.createInstance();

        for (int i = 0; i < userHandles.size(); i++) {
            long handle = userHandles.get(i);
            peers.addPeer(handle, MegaChatPeerList.PRIV_STANDARD);
        }
        megaChatApi.createChat(true, peers, this);
    }

    public void setMessages(ArrayList<AndroidMegaChatMessage> messages) {
        if (dialog != null) {
            dialog.dismiss();
        }
        this.messages = messages;
        //Create adapter
        if (adapter == null) {
            createAdapter();
        } else {
            updateMessages();
        }
    }

    private void createAdapter() {
        //Create adapter
        adapter = new MegaChatAdapter(this, chatRoom, messages, messagesPlaying,
                removedMessages, listView, getAvatarUseCase, getNodeUseCase,
                viewModel, megaApi, megaChatApi, dbH);

        adapter.setHasStableIds(true);
        listView.setLayoutManager(mLayoutManager);
        listView.setAdapter(adapter);
        updateMessages();
    }

    public void updateReactionAdapter(MegaChatMessage msg, String reaction, int count) {
        adapter.checkReactionUpdated(idChat, msg, reaction, count);
    }

    @Override
    public void onRequestStart(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaChatApiJava api, MegaChatRequest request) {

    }

    @Override
    public void onRequestFinish(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        Timber.d("onRequestFinish: %s %d", request.getRequestString(), request.getType());

        if (request.getType() == MegaChatRequest.TYPE_REMOVE_FROM_CHATROOM) {
            Timber.d("Remove participant: %d my user: %d", request.getUserHandle(), megaChatApi.getMyUserHandle());

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                Timber.d("Participant removed OK");
                invalidateOptionsMenu();
            } else {
                Timber.e("ERROR WHEN TYPE_REMOVE_FROM_CHATROOM %s", e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getTranslatedErrorString(e), MEGACHAT_INVALID_HANDLE);
            }
        } else if (request.getType() == MegaChatRequest.TYPE_ATTACH_NODE_MESSAGE) {
            removeProgressDialog();

            disableMultiselection();

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                Timber.d("File sent correctly");
                MegaNodeList nodeList = request.getMegaNodeList();

                for (int i = 0; i < nodeList.size(); i++) {
                    Timber.d("Node handle: %s", nodeList.get(i).getHandle());
                }
                AndroidMegaChatMessage androidMsgSent = new AndroidMegaChatMessage(request.getMegaChatMessage());
                sendMessageToUI(androidMsgSent);

            } else {
                Timber.e("File NOT sent: %s___%s", e.getErrorCode(), e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_attaching_node_from_cloud), -1);
            }

        } else if (request.getType() == MegaChatRequest.TYPE_REVOKE_NODE_MESSAGE) {
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                Timber.d("Node revoked correctly, msg id: %s", request.getMegaChatMessage().getMsgId());
            } else {
                Timber.e("NOT revoked correctly");
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_revoking_node), -1);
            }

        } else if (request.getType() == MegaChatRequest.TYPE_CREATE_CHATROOM) {
            Timber.d("Create chat request finish!!!");
            if (e.getErrorCode() == MegaChatError.ERROR_OK) {

                Timber.d("Open new chat");
                Intent intent = new Intent(this, ChatActivity.class);
                intent.setAction(ACTION_CHAT_SHOW_MESSAGES);
                intent.putExtra(CHAT_ID, request.getChatHandle());
                this.startActivity(intent);
                finish();
            } else {
                Timber.e("ERROR WHEN CREATING CHAT %s", e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.create_chat_error), -1);
            }

        } else if (request.getType() == MegaChatRequest.TYPE_AUTOJOIN_PUBLIC_CHAT) {
            viewModel.setIsJoiningOrLeaving(false, null);

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                if (request.getUserHandle() != MEGACHAT_INVALID_HANDLE) {
                    //Rejoin option
                    initializeInputText();
                    isOpeningChat = true;
                    loadHistory();
                } else {
                    //Join
                    setChatSubtitle();
                    setPreviewersView();
                }

                supportInvalidateOptionsMenu();
            } else {
                MegaChatRoom chatRoom = megaChatApi.getChatRoom(idChat);
                if (chatRoom != null && chatRoom.getOwnPrivilege() >= MegaChatRoom.PRIV_RO) {
                    Timber.w("I'm already a participant");
                    return;
                }

                Timber.e("ERROR WHEN JOINING CHAT %s___%s", e.getErrorCode(), e.getErrorString());
                showSnackbar(SNACKBAR_TYPE, getString(R.string.error_general_nodes), MEGACHAT_INVALID_HANDLE);
            }
        } else if (request.getType() == MegaChatRequest.TYPE_LAST_GREEN) {
            Timber.d("TYPE_LAST_GREEN requested");

        } else if (request.getType() == MegaChatRequest.TYPE_ARCHIVE_CHATROOM) {

            long chatHandle = request.getChatHandle();
            chatRoom = megaChatApi.getChatRoom(chatHandle);
            String chatTitle = getTitleChat(chatRoom);

            if (chatTitle == null) {
                chatTitle = "";
            } else if (!chatTitle.isEmpty() && chatTitle.length() > 60) {
                chatTitle = chatTitle.substring(0, 59) + "...";
            }

            if (!chatTitle.isEmpty() && chatRoom.isGroup() && !chatRoom.hasCustomTitle()) {
                chatTitle = "\"" + chatTitle + "\"";
            }

            supportInvalidateOptionsMenu();
            setChatSubtitle();

            if (!chatRoom.isArchived()) {
                requestLastGreen(INITIAL_PRESENCE_STATUS);
            }

            if (e.getErrorCode() == MegaChatError.ERROR_OK) {

                if (request.getFlag()) {
                    Timber.d("Chat archived");
                    viewModel.launchBroadcastChatArchived(chatTitle);
                    closeChat(true);
                    finish();
                } else {
                    Timber.d("Chat unarchived");
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.success_unarchive_chat, chatTitle), -1);
                }

            } else {
                if (request.getFlag()) {
                    Timber.e("ERROR WHEN ARCHIVING CHAT %s", e.getErrorString());
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_archive_chat, chatTitle), -1);
                } else {
                    Timber.e("ERROR WHEN UNARCHIVING CHAT %s", e.getErrorString());
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.error_unarchive_chat, chatTitle), -1);
                }
            }
        } else if (request.getType() == MegaChatRequest.TYPE_CHAT_LINK_HANDLE) {
            if (request.getFlag() && request.getNumRetry() == 0) {
                Timber.d("Removing chat link");
                if (e.getErrorCode() == MegaChatError.ERROR_OK) {
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.chat_link_deleted), -1);
                } else {
                    if (e.getErrorCode() == MegaChatError.ERROR_ARGS) {
                        Timber.e("The chatroom isn't grupal or public");
                    } else if (e.getErrorCode() == MegaChatError.ERROR_NOENT) {
                        Timber.e("The chatroom doesn't exist or the chatid is invalid");
                    } else if (e.getErrorCode() == MegaChatError.ERROR_ACCESS) {
                        Timber.e("The chatroom doesn't have a topic or the caller isn't an operator");
                    } else {
                        Timber.e("Error TYPE_CHAT_LINK_HANDLE %s", e.getErrorCode());
                    }
                    showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error) + ": " + e.getErrorString(), -1);
                }
            }
        }
    }

    @Override
    public void onRequestTemporaryError(MegaChatApiJava api, MegaChatRequest request, MegaChatError e) {
        Timber.w("onRequestTemporaryError");
    }

    @Override
    protected void onStop() {
        super.onStop();
        viewModel.setChatInitialised(false);
    }

    private void cleanBuffers() {
        if (!bufferMessages.isEmpty()) {
            bufferMessages.clear();
        }
        if (!messages.isEmpty()) {
            messages.clear();
            if (adapter != null) {
                adapter.setMessages(messages);
            }
        }
        if (!removedMessages.isEmpty()) {
            removedMessages.clear();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!viewModel.isChatInitialised() && megaChatApi.getInitState() != INIT_ANONYMOUS) {
            cleanBuffers();

            if (!initChat()) {
                return;
            }
            loadHistory();
        }
    }

    @Override
    protected void onDestroy() {
        internalComposite.clear();
        destroySpeakerAudioManger();
        cleanBuffers();
        if (handlerEmojiKeyboard != null) {
            handlerEmojiKeyboard.removeCallbacksAndMessages(null);
        }
        if (handlerKeyboard != null) {
            handlerKeyboard.removeCallbacksAndMessages(null);
        }

        if (megaChatApi != null && idChat != -1) {
            megaChatApi.closeChatRoom(idChat, this);
            MegaApplication.setClosedChat(true);

            if (chatRoom != null && chatRoom.isPreview()) {
                megaChatApi.closeChatPreview(idChat);
            }
        }

        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }

        if (handlerReceive != null) {
            handlerReceive.removeCallbacksAndMessages(null);
        }
        if (handlerSend != null) {
            handlerSend.removeCallbacksAndMessages(null);
        }

        hideCallBar(null);
        destroyAudioRecorderElements();
        if (adapter != null) {
            adapter.stopAllReproductionsInProgress();
            adapter.destroyVoiceElemnts();
        }

        unregisterReceiver(historyTruncatedByRetentionTimeReceiver);
        unregisterReceiver(voiceclipDownloadedReceiver);
        unregisterReceiver(userNameReceiver);
        unregisterReceiver(closeChatReceiver);
        unregisterReceiver(chatUploadStartedReceiver);
        unregisterReceiver(errorCopyingNodesReceiver);
        unregisterReceiver(retryPendingMessageReceiver);

        if (megaApi != null) {
            megaApi.removeRequestListener(this);
        }

        AudioPlayerService.resumeAudioPlayerIfNotInCall(this);

        nodeSaver.destroy();

        super.onDestroy();
    }

    /**
     * Method for saving in the database if there was something written in the input text or if a message was being edited
     */
    private void saveInputText() {
        try {
            if (textChat != null) {
                String written = textChat.getText().toString();
                if (written != null) {
                    if (dbH == null) {
                        dbH = MegaApplication.getInstance().getDbH();
                    }
                    if (dbH != null) {
                        ChatItemPreferences prefs = dbH.findChatPreferencesByHandle(Long.toString(idChat));
                        String editedMessageId = editingMessage && messageToEdit != null ? Long.toString(messageToEdit.getMsgId()) : "";
                        if (prefs != null) {
                            prefs.setEditedMsgId(editedMessageId);
                            prefs.setWrittenText(written);
                            dbH.setWrittenTextItem(Long.toString(idChat), written, editedMessageId);
                        } else {
                            prefs = new ChatItemPreferences(Long.toString(idChat), written, editedMessageId);
                            dbH.setChatItemPreferences(prefs);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Timber.e(e, "Written message not stored on DB");
        }
    }

    public void closeChat(boolean shouldLogout) {
        if (megaChatApi == null || chatRoom == null || idChat == MEGACHAT_INVALID_HANDLE) {
            return;
        }

        saveInputText();
        shouldLogout = chatC.isInAnonymousMode() && shouldLogout;

        megaChatApi.closeChatRoom(idChat, this);
        if (chatRoom.isPreview()) {
            megaChatApi.closeChatPreview(idChat);
        }

        MegaApplication.setClosedChat(true);
        composite.clear();

        if (shouldLogout) {
            megaChatApi.logout();
        }

        chatRoom = null;
        idChat = MEGACHAT_INVALID_HANDLE;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        hideKeyboard();
        if (intent != null) {
            if (intent.getAction() != null) {
                if (intent.getAction().equals(ACTION_UPDATE_ATTACHMENT)) {
                    Timber.d("Intent to update an attachment with error");

                    long idPendMsg = intent.getLongExtra("ID_MSG", -1);
                    if (idPendMsg != -1) {
                        int indexToChange = -1;
                        ListIterator<AndroidMegaChatMessage> itr = messages.listIterator(messages.size());

                        // Iterate in reverse.
                        while (itr.hasPrevious()) {
                            AndroidMegaChatMessage messageToCheck = itr.previous();

                            if (messageToCheck.isUploading()) {
                                if (messageToCheck.getPendingMessage().getId() == idPendMsg) {
                                    indexToChange = itr.nextIndex();
                                    Timber.d("Found index to change: %s", indexToChange);
                                    break;
                                }
                            }
                        }

                        if (indexToChange != -1) {
                            Timber.d("Index modified: %s", indexToChange);

                            PendingMessage pendingMsg = null;
                            if (idPendMsg != -1) {
                                pendingMsg = dbH.findPendingMessageById(idPendMsg);

                                if (pendingMsg != null) {
                                    messages.get(indexToChange).setPendingMessage(pendingMsg);
                                    adapter.modifyMessage(messages, indexToChange + 1);
                                }
                            }
                        } else {
                            Timber.e("Error, id pending message message not found!!");
                        }
                    } else {
                        Timber.e("Error. The idPendMsg is -1");
                    }

                    int isOverquota = intent.getIntExtra("IS_OVERQUOTA", 0);
                    if (isOverquota == 1) {
                        showOverquotaAlert(false);
                    } else if (isOverquota == 2) {
                        showOverquotaAlert(true);
                    }

                    return;
                } else {
                    long newidChat = intent.getLongExtra(CHAT_ID, MEGACHAT_INVALID_HANDLE);
                    if (intent.getAction().equals(ACTION_CHAT_SHOW_MESSAGES) || intent.getAction().equals(ACTION_OPEN_CHAT_LINK) || idChat != newidChat) {
                        cleanBuffers();
                        pendingMessagesLoaded = false;
                    }
                    if (messagesPlaying != null && !messagesPlaying.isEmpty()) {
                        for (MessageVoiceClip m : messagesPlaying) {
                            m.getMediaPlayer().release();
                            m.setMediaPlayer(null);
                        }
                        messagesPlaying.clear();
                    }

                    closeChat(false);
                    MegaApplication.setOpenChatId(-1);
                    initAfterIntent(intent, null);
                }

            }
        }
        super.onNewIntent(intent);
        setIntent(intent);
    }

    public MegaChatRoom getChatRoom() {
        return chatRoom;
    }

    public void setChatRoom(MegaChatRoom chatRoom) {
        this.chatRoom = chatRoom;
    }

    public void revoke() {
        Timber.d("revoke");
        megaChatApi.revokeAttachmentMessage(idChat, selectedMessageId);
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {

    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {
        removeProgressDialog();
        if (request.getType() == MegaRequest.TYPE_INVITE_CONTACT) {
            Timber.d("MegaRequest.TYPE_INVITE_CONTACT finished: %s", request.getNumber());

            if (request.getNumber() == MegaContactRequest.INVITE_ACTION_REMIND) {
                showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_invitation_resent), -1);
            } else {
                if (e.getErrorCode() == MegaError.API_OK) {
                    Timber.d("OK INVITE CONTACT: %s", request.getEmail());
                    if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD) {
                        showSnackbar(DISMISS_ACTION_SNACKBAR, getString(R.string.contact_invited), MEGACHAT_INVALID_HANDLE);
                    }
                } else {
                    if (e.getErrorCode() == MegaError.API_EEXIST) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.context_contact_already_invited, request.getEmail()), -1);
                    } else if (request.getNumber() == MegaContactRequest.INVITE_ACTION_ADD && e.getErrorCode() == MegaError.API_EARGS) {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.error_own_email_as_contact), -1);
                    } else {
                        showSnackbar(SNACKBAR_TYPE, getString(R.string.general_error), -1);
                    }
                    Timber.e("ERROR: %s___%s", e.getErrorCode(), e.getErrorString());
                }
            }
        } else if (request.getType() == MegaRequest.TYPE_CANCEL_TRANSFER) {
            int tag = request.getTransferTag();
            ChatManagement chatManagement = MegaApplication.getChatManagement();
            long pMsgId = chatManagement.getPendingMsgIdToBeCancelled(tag);
            chatManagement.removePendingMsgToBeCancelled(tag);

            if (e.getErrorCode() != MegaError.API_OK) {
                chatManagement.addMsgToBeDelete(pMsgId);
            } else {
                removePendingMessageFromDbHAndUI(pMsgId);
            }
        } else if (request.getType() == MegaRequest.TYPE_SET_ATTR_USER) {
            if (request.getParamType() == MegaApiJava.USER_ATTR_GEOLOCATION) {
                if (e.getErrorCode() == MegaError.API_OK) {
                    Timber.d("Attribute USER_ATTR_GEOLOCATION enabled");
                    MegaApplication.setEnabledGeoLocation(true);
                    getLocationPermission();
                } else {
                    Timber.d("Attribute USER_ATTR_GEOLOCATION disabled");
                    MegaApplication.setEnabledGeoLocation(false);
                }
            }
        }
    }

    /**
     * Method for removing a penging message from the database and UI
     *
     * @param pendingId Pending message ID
     */
    private void removePendingMessageFromDbHAndUI(long pendingId) {
        try {
            dbH.removePendingMessageById(pendingId);
            int positionToRemove = selectedPosition == INVALID_POSITION
                    ? findPendingMessagePosition(pendingId)
                    : selectedPosition;

            messages.remove(positionToRemove);
            adapter.removeMessage(positionToRemove, messages);
        } catch (IndexOutOfBoundsException exception) {
            Timber.e(exception, "EXCEPTION");
        }
    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

    }

    protected MegaChatAdapter getAdapter() {
        return adapter;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(CHAT_ID, idChat);
        outState.putLong("selectedMessageId", selectedMessageId);
        outState.putInt("selectedPosition", selectedPosition);
        outState.putLong(LAST_MESSAGE_SEEN, lastIdMsgSeen);
        outState.putLong(GENERAL_UNREAD_COUNT, generalUnreadCount);
        outState.putString("mOutputFilePath", mOutputFilePath);
        outState.putBoolean("isShareLinkDialogDismissed", isShareLinkDialogDismissed);
        nodeSaver.saveState(outState);

        if (adapter == null)
            return;


        RotatableAdapter currentAdapter = getAdapter();
        if (currentAdapter != null & adapter.isMultipleSelect()) {
            ArrayList<Integer> selectedPositions = (ArrayList<Integer>) (currentAdapter.getSelectedItems());
            outState.putIntegerArrayList(SELECTED_ITEMS, selectedPositions);
        }

        if (currentMessagePlaying != null) {
            outState.putBoolean(PLAYING, true);
            outState.putLong(ID_VOICE_CLIP_PLAYING, currentMessagePlaying.getIdMessage());
            outState.putLong(MESSAGE_HANDLE_PLAYING, currentMessagePlaying.getMessageHandle());
            outState.putLong(USER_HANDLE_PLAYING, currentMessagePlaying.getUserHandle());
            outState.putInt(PROGRESS_PLAYING, currentMessagePlaying.getProgress());
        } else {
            outState.putBoolean(PLAYING, false);
        }

        outState.putBoolean("isLocationDialogShown", isLocationDialogShown);
        outState.putBoolean(OPENING_AND_JOINING_ACTION, openingAndJoining);
        outState.putBoolean(ERROR_REACTION_DIALOG, errorReactionsDialogIsShown);
        outState.putLong(TYPE_ERROR_REACTION, typeErrorReaction);
        outState.putBoolean(END_CALL_FOR_ALL_DIALOG, AlertDialogUtil.isAlertDialogShown(endCallForAllDialog));
        isOnlyMeInCallDialogShown = AlertDialogUtil.isAlertDialogShown(dialogOnlyMeInCall);
        outState.putBoolean(ONLY_ME_IN_CALL_DIALOG, isOnlyMeInCallDialogShown);
        hideDialogCall();
    }

    /**
     * Handle processed upload intent.
     *
     * @param infos List<ShareInfo> containing all the upload info.
     */
    private void onIntentProcessed(List<ShareInfo> infos) {
        Timber.d("onIntentProcessed");
        if (infos == null) {
            statusDialog.dismiss();
            showSnackbar(SNACKBAR_TYPE, getString(R.string.upload_can_not_open), -1);
        } else {
            PermissionUtils.checkNotificationsPermission(this);
            Timber.d("Launch chat upload with files %s", infos.size());
            for (ShareInfo info : infos) {
                Intent intent = new Intent(this, ChatUploadService.class);

                PendingMessage pMsgSingle = createAttachmentPendingMessage(idChat,
                        info.getFileAbsolutePath(), info.getTitle(), false);

                long idMessage = pMsgSingle.getId();

                if (idMessage != -1) {
                    intent.putExtra(ChatUploadService.EXTRA_ID_PEND_MSG, idMessage);

                    Timber.d("Size of the file: %s", info.getSize());

                    AndroidMegaChatMessage newNodeAttachmentMsg = new AndroidMegaChatMessage(pMsgSingle, true);
                    sendMessageToUI(newNodeAttachmentMsg);

                    intent.putExtra(ChatUploadService.EXTRA_CHAT_ID, idChat);

                    checkIfServiceCanStart(intent);
                } else {
                    Timber.e("Error when adding pending msg to the database");
                }

                removeProgressDialog();
            }
        }
    }

    /**
     * If the chat id received is valid, opens a chat after forward messages.
     * If no, only disables select mode and shows a Snackbar if the text received is not null neither empty.
     *
     * @param chatHandle Chat id.
     * @param text       Text to show as Snackbar if needed, null or empty otherwise.
     */
    public void openChatAfterForward(long chatHandle, String text) {
        removeProgressDialog();

        if (chatHandle == idChat || chatHandle == MEGACHAT_INVALID_HANDLE) {
            disableMultiselection();

            if (text != null) {
                showSnackbar(SNACKBAR_TYPE, text, MEGACHAT_INVALID_HANDLE);
            }

            return;
        }

        Intent intentOpenChat = new Intent(this, ManagerActivity.class);
        intentOpenChat.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intentOpenChat.setAction(ACTION_CHAT_NOTIFICATION_MESSAGE);
        intentOpenChat.putExtra(CHAT_ID, chatHandle);
        intentOpenChat.putExtra(SHOW_SNACKBAR, text);
        closeChat(true);
        startActivity(intentOpenChat);
        finish();
    }

    public void markAsSeen(MegaChatMessage msg) {
        Timber.d("markAsSeen");
        if (activityVisible) {
            if (msg.getStatus() != MegaChatMessage.STATUS_SEEN) {
                Timber.d("Mark message: %d as seen", msg.getMsgId());
                megaChatApi.setMessageSeen(chatRoom.getChatId(), msg.getMsgId());
            }
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        setKeyboardVisibilityListener();

        if (idChat != -1 && chatRoom != null) {
            setNodeAttachmentVisible();

            passcodeManagement.setShowPasscodeScreen(true);
            MegaApplication.setOpenChatId(idChat);
            supportInvalidateOptionsMenu();

            int chatConnection = megaChatApi.getChatConnectionState(idChat);
            Timber.d("Chat connection (%d) is: %d", idChat, chatConnection);
            if (chatConnection == MegaChatApi.CHAT_CONNECTION_ONLINE) {
                setAsRead = true;
                if (!chatRoom.isGroup()) {
                    requestLastGreen(INITIAL_PRESENCE_STATUS);
                }
            } else {
                setAsRead = false;
            }
            setChatSubtitle();
            if (emojiKeyboard != null) {
                emojiKeyboard.hideBothKeyboard(this);
            }

            try {
                ChatAdvancedNotificationBuilder notificationBuilder;
                notificationBuilder = ChatAdvancedNotificationBuilder.newInstance(this);
                notificationBuilder.removeAllChatNotifications();

                notificationManager.cancel((int) idChat);
            } catch (Exception e) {
                Timber.e(e, "Exception NotificationManager - remove all notifications");
            }
            //Update last seen position if different and there is unread messages
            //If the chat is being opened do not update, onLoad will do that

            if (!isOpeningChat) {
                Timber.d("Chat is NOT loading history");
                if (lastSeenReceived == true && messages != null) {

                    long unreadCount = chatRoom.getUnreadCount();
                    if (unreadCount != 0) {
                        lastIdMsgSeen = megaChatApi.getLastMessageSeenId(idChat);

                        //Find last message
                        int positionLastMessage = -1;
                        for (int i = messages.size() - 1; i >= 0; i--) {
                            AndroidMegaChatMessage androidMessage = messages.get(i);

                            if (!androidMessage.isUploading()) {
                                MegaChatMessage msg = androidMessage.getMessage();
                                if (msg.getMsgId() == lastIdMsgSeen) {
                                    positionLastMessage = i;
                                    break;
                                }
                            }
                        }

                        if (positionLastMessage == -1) {
                            scrollToMessage(-1);

                        } else {
                            //Check if it has no my messages after

                            if (positionLastMessage >= messages.size() - 1) {
                                Timber.d("Nothing after, do not increment position");
                            } else {
                                positionLastMessage = positionLastMessage + 1;
                            }

                            AndroidMegaChatMessage message = messages.get(positionLastMessage);
                            Timber.d("Position lastMessage found: %d messages.size: %d", positionLastMessage, messages.size());

                            while (message.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()) {
                                lastIdMsgSeen = message.getMessage().getMsgId();
                                positionLastMessage = positionLastMessage + 1;
                                message = messages.get(positionLastMessage);
                            }

                            generalUnreadCount = unreadCount;
                            scrollToMessage(lastIdMsgSeen);
                        }
                    } else {
                        if (generalUnreadCount != 0) {
                            scrollToMessage(-1);
                        }
                    }
                }
                setLastMessageSeen();
            } else {
                Timber.d("openingChat:doNotUpdateLastMessageSeen");
            }

            activityVisible = true;
            if (aB != null && aB.getTitle() != null) {
                setTitle(titleToolbar.getText());
            }
            updateActionModeTitle();

            if (isOnlyMeInCallDialogShown) {
                showOnlyMeInTheCallDialog();
            } else {
                hideDialogCall();
            }
        }
    }

    public void scrollToMessage(long lastId) {
        if (messages == null || messages.isEmpty())
            return;

        for (int i = messages.size() - 1; i >= 0; i--) {
            AndroidMegaChatMessage androidMessage = messages.get(i);

            if (!androidMessage.isUploading()) {
                MegaChatMessage msg = androidMessage.getMessage();
                if (msg.getMsgId() == lastId) {
                    Timber.d("Scroll to position: %s", i);
                    mLayoutManager.scrollToPositionWithOffset(i + 1, scaleHeightPx(30, getOutMetrics()));
                    break;
                }
            }
        }

    }

    public void setLastMessageSeen() {
        Timber.d("setLastMessageSeen");

        if (messages != null) {
            if (!messages.isEmpty()) {
                AndroidMegaChatMessage lastMessage = messages.get(messages.size() - 1);
                int index = messages.size() - 1;
                if ((lastMessage != null) && (lastMessage.getMessage() != null)) {
                    if (!lastMessage.isUploading()) {
                        while (lastMessage.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()) {
                            index--;
                            if (index == -1) {
                                break;
                            }
                            lastMessage = messages.get(index);
                        }

                        if (lastMessage.getMessage() != null && activityLifecycleHandler.isActivityVisible()) {
                            boolean resultMarkAsSeen = megaChatApi.setMessageSeen(idChat, lastMessage.getMessage().getMsgId());
                            Timber.d("Result setMessageSeen: %s", resultMarkAsSeen);
                        }

                    } else {
                        while (lastMessage.isUploading() == true) {
                            index--;
                            if (index == -1) {
                                break;
                            }
                            lastMessage = messages.get(index);
                        }
                        if ((lastMessage != null) && (lastMessage.getMessage() != null)) {

                            while (lastMessage.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()) {
                                index--;
                                if (index == -1) {
                                    break;
                                }
                                lastMessage = messages.get(index);
                            }

                            if (lastMessage.getMessage() != null && activityLifecycleHandler.isActivityVisible()) {
                                boolean resultMarkAsSeen = megaChatApi.setMessageSeen(idChat, lastMessage.getMessage().getMsgId());
                                Timber.d("Result setMessageSeen: %s", resultMarkAsSeen);
                            }
                        }
                    }
                } else {
                    Timber.e("lastMessageNUll");
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (adapter != null) {
            currentMessagePlaying = adapter.getVoiceClipPlaying();
        }

        if (rtcAudioManager != null)
            rtcAudioManager.unregisterProximitySensor();

        destroyAudioRecorderElements();
        if (adapter != null) {
            adapter.pausePlaybackInProgress();
        }
        hideKeyboard();
        activityVisible = false;
        MegaApplication.setOpenChatId(-1);
    }

    private void onChatConnectionStateUpdate(long chatid, int newState) {
        Timber.d("Chat ID: %d. New State: %d", chatid, newState);

        if (idChat == chatid) {
            if (newState == MegaChatApi.CHAT_CONNECTION_ONLINE) {
                Timber.d("Chat is now ONLINE");
                setAsRead = true;
                setLastMessageSeen();

                if (stateHistory == MegaChatApi.SOURCE_ERROR && retryHistory) {
                    Timber.w("SOURCE_ERROR:call to load history again");
                    retryHistory = false;
                    loadHistory();
                }

            } else {
                setAsRead = false;
            }

            updateCallBanner();
            setChatSubtitle();
            supportInvalidateOptionsMenu();
        }
    }

    private void onChatPresenceLastGreen(long userhandle, int lastGreen) {
        Timber.d("userhandle: %d, lastGreen: %d", userhandle, lastGreen);

        if (chatRoom == null) {
            return;
        }

        if (!chatRoom.isGroup() && userhandle == chatRoom.getPeerHandle(0)) {
            Timber.d("Update last green");

            int state = megaChatApi.getUserOnlineStatus(chatRoom.getPeerHandle(0));

            if (state != MegaChatApi.STATUS_ONLINE && state != MegaChatApi.STATUS_BUSY && state != MegaChatApi.STATUS_INVALID) {
                String formattedDate = lastGreenDate(this, lastGreen);

                setLastGreen(formattedDate);

                Timber.d("Date last green: %s", formattedDate);
            }
        }
    }

    public void uploadPictureOrVoiceClip(String path) {
        if (path == null) return;
        File file;
        if (path.startsWith("content:")) {
            file = getFileFromContentUri(this, Uri.parse(path));
        } else if (isVoiceClip(path)) {
            file = buildVoiceClipFile(outputFileName);
            if (!isFileAvailable(file)) return;
        } else {
            file = new File(path);
            if (!MimeTypeList.typeForName(file.getAbsolutePath()).isImage()) return;
        }

        Intent intent = new Intent(this, ChatUploadService.class);
        PendingMessage pMsgSingle = new PendingMessage();
        pMsgSingle.setChatId(idChat);
        if (isVoiceClip(file.getAbsolutePath())) {
            pMsgSingle.setType(TYPE_VOICE_CLIP);
            intent.putExtra(EXTRA_TRANSFER_TYPE, APP_DATA_VOICE_CLIP);
        }

        long timestamp = System.currentTimeMillis() / 1000;
        pMsgSingle.setUploadTimestamp(timestamp);
        String fingerprint = megaApi.getFingerprint(file.getAbsolutePath());
        pMsgSingle.setFilePath(file.getAbsolutePath());
        pMsgSingle.setName(file.getName());
        pMsgSingle.setFingerprint(fingerprint);
        long idMessage = dbH.addPendingMessage(pMsgSingle);
        pMsgSingle.setId(idMessage);

        if (idMessage == -1) return;

        Timber.d("idMessage = %s", idMessage);
        intent.putExtra(ChatUploadService.EXTRA_ID_PEND_MSG, idMessage);
        if (!isLoadingHistory) {
            Timber.d("sendMessageToUI");
            AndroidMegaChatMessage newNodeAttachmentMsg = new AndroidMegaChatMessage(pMsgSingle, true);
            sendMessageToUI(newNodeAttachmentMsg);
        } else {
            pendingMessagesLoaded = false;
        }
        intent.putExtra(ChatUploadService.EXTRA_CHAT_ID, idChat);

        checkIfServiceCanStart(intent);
    }


    private void showOverquotaAlert(boolean prewarning) {
        Timber.d("prewarning: %s", prewarning);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.overquota_alert_title));

        if (prewarning) {
            builder.setMessage(getString(R.string.pre_overquota_alert_text));
        } else {
            builder.setMessage(getString(R.string.overquota_alert_text));
        }

        if (chatAlertDialog == null) {

            builder.setPositiveButton(getString(R.string.my_account_upgrade_pro), new android.content.DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    navigateToUpgradeAccount();
                }
            });
            builder.setNegativeButton(getString(R.string.general_cancel), new android.content.DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    chatAlertDialog = null;
                }
            });

            chatAlertDialog = builder.create();
            chatAlertDialog.setCanceledOnTouchOutside(false);
        }

        chatAlertDialog.show();
    }

    private void showCallInProgressLayout(String text, boolean shouldChronoShown, MegaChatCall call) {
        if (callInProgressText != null) {
            callInProgressText.setText(text);
        }

        if (shouldChronoShown) {
            startChronometers(call);
        } else {
            stopChronometers(call);
        }

        chatIdBanner = call.getChatid();

        if (!chatRoom.isArchived() && chatRoom.isActive() && callInProgressLayout != null &&
                callInProgressLayout.getVisibility() != View.VISIBLE &&
                startOrJoinMeetingBanner.getVisibility() != View.VISIBLE) {
            callInProgressLayout.setAlpha(1);
            callInProgressLayout.setVisibility(View.VISIBLE);
            callInProgressLayout.setOnClickListener(this);
        }
    }

    /**
     * Method to start the chronometer related to the current call
     *
     * @param call The current call in progress.
     */
    private void startChronometers(MegaChatCall call) {
        if (callInProgressChrono == null) {
            return;
        }

        activateChrono(true, callInProgressChrono, call, true);
        callInProgressChrono.setOnChronometerTickListener(chronometer -> {
            if (subtitleChronoCall == null) {
                return;
            }

            subtitleChronoCall.setVisibility(View.VISIBLE);
            subtitleChronoCall.setText(chronometer.getText());
        });
    }

    /**
     * Method to stop the chronometer related to the current call
     *
     * @param call The current call in progress.
     */
    private void stopChronometers(MegaChatCall call) {
        if (callInProgressChrono != null) {
            activateChrono(false, callInProgressChrono, call);
            callInProgressChrono.setOnChronometerTickListener(null);
        }

        if (subtitleChronoCall != null) {
            subtitleChronoCall.setVisibility(View.GONE);
        }
    }

    /**
     * Method for hiding the current call bar.
     *
     * @param call The call.
     */
    private void hideCallBar(MegaChatCall call) {
        invalidateOptionsMenu();
        stopChronometers(call);

        if (callInProgressLayout != null) {
            callInProgressLayout.setVisibility(View.GONE);
            callInProgressLayout.setOnClickListener(null);
            subtitleCall.setVisibility(View.GONE);
            setSubtitleVisibility();
        }
        if (returnCallOnHoldButton != null) {
            returnCallOnHoldButton.setVisibility(View.GONE);
        }
    }

    /**
     * Method to get another call on hold.
     *
     * @param currentChatId Call id.
     * @return The another call.
     */
    private MegaChatCall getAnotherOnHoldCall(long currentChatId) {
        return getAnotherOnHoldOrActiveCall(currentChatId, false);
    }

    /**
     * Method to get another call in progress.
     *
     * @param currentChatId Call id.
     * @return The another call.
     */
    private MegaChatCall getAnotherActiveCall(long currentChatId) {
        return getAnotherOnHoldOrActiveCall(currentChatId, true);
    }

    /**
     * Method to get another call in progress or on hold.
     *
     * @param currentChatId Call id.
     * @param isActiveCall  True if wants to get a call in progress,
     *                      false if wants to get a call on hold.
     * @return The another call.
     */
    private MegaChatCall getAnotherOnHoldOrActiveCall(long currentChatId, boolean isActiveCall) {
        ArrayList<Long> chatsIDsWithCallActive = getCallsParticipating();
        if (chatsIDsWithCallActive != null && !chatsIDsWithCallActive.isEmpty()) {
            for (Long anotherChatId : chatsIDsWithCallActive) {
                if (anotherChatId != currentChatId && megaChatApi.getChatCall(anotherChatId) != null &&
                        ((isActiveCall && !megaChatApi.getChatCall(anotherChatId).isOnHold()) ||
                                (!isActiveCall && megaChatApi.getChatCall(anotherChatId).isOnHold()))) {
                    return megaChatApi.getChatCall(anotherChatId);
                }
            }
        }
        return null;
    }

    /**
     * Method for updating the banner that indicates the current call in this chat.
     */
    private void updateCallBanner() {
        if (chatRoom == null || chatRoom.isPreview() || !chatRoom.isActive() ||
                megaChatApi.getNumCalls() <= 0 || !isStatusConnected(this, idChat)) {
            /*No calls*/
            subtitleCall.setVisibility(View.GONE);
            setSubtitleVisibility();
            MegaChatCall call = megaChatApi.getChatCall(idChat);
            hideCallBar(call);
            return;
        }

        MegaChatCall anotherActiveCall = getAnotherActiveCall(idChat);
        MegaChatCall anotherOnHoldCall = getAnotherOnHoldCall(idChat);
        MegaChatCall callInThisChat = megaChatApi.getChatCall(chatRoom.getChatId());

        if (callInThisChat == null || !isStatusConnected(this, idChat)) {
            //No call in this chatRoom
            if (anotherActiveCall != null || anotherOnHoldCall != null) {
                updateCallInProgressLayout(anotherActiveCall != null ? anotherActiveCall : anotherOnHoldCall,
                        getString(R.string.call_in_progress_layout));
                returnCallOnHoldButton.setVisibility(View.GONE);
            } else {
                hideCallBar(null);
            }
            return;
        }

        //Call in this chatRoom
        int callStatus = callInThisChat.getStatus();
        Timber.d("The call status in this chatRoom is %s", callStatusToString(callStatus));

        // Check call on hold button
        switch (callStatus) {
            case MegaChatCall.CALL_STATUS_DESTROYED:
                subtitleCall.setVisibility(View.GONE);
                setSubtitleVisibility();
                if (anotherActiveCall != null || anotherOnHoldCall != null) {
                    updateCallInProgressLayout(anotherActiveCall != null ? anotherActiveCall : anotherOnHoldCall,
                            getString(anotherActiveCall != null ? R.string.call_in_progress_layout : R.string.call_on_hold));
                    returnCallOnHoldButton.setVisibility(View.GONE);
                } else {
                    hideCallBar(megaChatApi.getChatCall(idChat));
                }
                break;

            case MegaChatCall.CALL_STATUS_IN_PROGRESS:
                if (callInThisChat.isOnHold() || isSessionOnHold(callInThisChat.getChatid())) {
                    if (anotherActiveCall != null || anotherOnHoldCall != null) {
                        updateCallInProgressLayout(anotherActiveCall != null ? anotherActiveCall : anotherOnHoldCall,
                                getString(R.string.call_in_progress_layout));
                        returnCallOnHoldButtonText.setText(getResources().getString(R.string.call_on_hold));
                        returnCallOnHoldButtonIcon.setImageResource(R.drawable.ic_transfers_pause);
                        returnCallOnHoldButton.setVisibility(View.VISIBLE);
                    } else {
                        updateCallInProgressLayout(callInThisChat, getString(R.string.call_in_progress_layout));
                        returnCallOnHoldButton.setVisibility(View.GONE);
                    }
                    break;
                }
        }

        returnCallOnHoldButton.setVisibility(View.GONE);

        Timber.d("Call Status in this chatRoom: %s", callStatusToString(callStatus));
        switch (callStatus) {
            case MegaChatCall.CALL_STATUS_TERMINATING_USER_PARTICIPATION:
            case MegaChatCall.CALL_STATUS_USER_NO_PRESENT:
                if (chatRoom == null)
                    break;

                if (anotherActiveCall == null && anotherOnHoldCall == null) {
                    String textLayout = getString((callInThisChat.isRinging() || !megaApi.isChatNotifiable(idChat)) ?
                            R.string.call_in_progress_layout :
                            R.string.join_call_layout);

                    if (chatRoom.isGroup()) {
                        if (callInThisChat.getNumParticipants() == 0) {
                            hideCallBar(callInThisChat);
                            break;
                        }

                        long callerHandle = callInThisChat.getCaller();
                        String callerFullName = chatC.getParticipantFullName(callerHandle);
                        if (callerHandle != MEGACHAT_INVALID_HANDLE && !isTextEmpty(callerFullName)) {
                            textLayout = getString(chatRoom.isMeeting() ?
                                    R.string.join_meeting_layout_in_group_call :
                                    R.string.join_call_layout_in_group_call, callerFullName);
                        } else {
                            textLayout = getString(R.string.join_call_layout);
                        }
                    } else if (callInThisChat.getNumParticipants() > 1) {
                        hideCallBar(callInThisChat);
                        break;
                    }

                    tapToReturnLayout(callInThisChat, textLayout);
                } else {
                    updateCallInProgressLayout(anotherActiveCall != null ? anotherActiveCall : anotherOnHoldCall,
                            getString(R.string.call_in_progress_layout));

                    returnCallOnHoldButton.setVisibility(View.VISIBLE);
                    returnCallOnHoldButtonIcon.setImageResource(R.drawable.ic_call_chat);
                    returnCallOnHoldButtonText.setText(getResources().getString(chatRoom.isGroup() ?
                            R.string.title_join_call :
                            R.string.title_join_one_to_one_call));
                }
                break;

            case MegaChatCall.CALL_STATUS_IN_PROGRESS:
                callInProgressLayout.setBackgroundColor(ColorUtils.getThemeColor(this, com.google.android.material.R.attr.colorSecondary));
                updateCallInProgressLayout(callInThisChat, getString(R.string.call_in_progress_layout));
                break;
        }
    }

    private void tapToReturnLayout(MegaChatCall call, String text) {
        callInProgressLayout.setBackgroundColor(ColorUtils.getThemeColor(this, com.google.android.material.R.attr.colorSecondary));
        showCallInProgressLayout(text, false, call);
        callInProgressLayout.setOnClickListener(this);
    }

    private void updateCallInProgressLayout(MegaChatCall call, String text) {
        if (call == null)
            return;

        showCallInProgressLayout(text, true, call);

        callInProgressLayout.setOnClickListener(this);
        if (chatRoom != null && chatRoom.isGroup() && megaChatApi.getChatCall(chatRoom.getChatId()) != null) {
            subtitleCall.setVisibility(View.VISIBLE);
            individualSubtitleToobar.setVisibility(View.GONE);
            setGroupalSubtitleToolbarVisibility(false);
        }

        invalidateOptionsMenu();
    }

    public void goToEnd() {
        Timber.d("goToEnd()");
        int infoToShow = -1;
        if (!messages.isEmpty()) {
            int index = messages.size() - 1;

            AndroidMegaChatMessage msg = messages.get(index);

            while (!msg.isUploading() && msg.getMessage().getStatus() == MegaChatMessage.STATUS_SENDING_MANUAL) {
                index--;
                if (index == -1) {
                    break;
                }
                msg = messages.get(index);
            }

            if (index == (messages.size() - 1)) {
                //Scroll to end
                mLayoutManager.scrollToPositionWithOffset(index + 1, scaleHeightPx(20, getOutMetrics()));
            } else {
                index++;
                infoToShow = adjustInfoToShow(index);
                if (infoToShow == AndroidMegaChatMessage.CHAT_ADAPTER_SHOW_ALL) {
                    mLayoutManager.scrollToPositionWithOffset(index, scaleHeightPx(50, getOutMetrics()));
                } else {
                    mLayoutManager.scrollToPositionWithOffset(index, scaleHeightPx(20, getOutMetrics()));
                }
            }
        }
    }

    /**
     * Method to hide the button, to scroll to the last message, in a chatroom.
     */
    public void hideScrollToLastMsgButton() {
        msgsReceived.clear();
        if (unreadMsgsLayout.getVisibility() != View.VISIBLE)
            return;

        unreadMsgsLayout.animate()
                .alpha(0.0f)
                .setDuration(500)
                .withEndAction(() -> {
                    unreadMsgsLayout.setVisibility(View.GONE);
                    unreadMsgsLayout.setAlpha(1.0f);
                })
                .start();
    }

    /**
     * Method of showing the button to scroll to the last message in a chat room.
     */
    private void showScrollToLastMsgButton() {
        if (isInputTextExpanded) {
            return;
        }

        unreadBadgeImage.setVisibility(View.VISIBLE);
        unreadBadgeLayout.setVisibility(View.VISIBLE);
        unreadMsgsLayout.setVisibility(View.VISIBLE);

        if (msgsReceived != null && msgsReceived.size() > 0) {
            int numOfNewMessages = msgsReceived.size();
            numOfNewMessages = numOfNewMessages - 99;
            if (numOfNewMessages > 0) {
                unreadBadgeText.setText("+" + (msgsReceived.size() - numOfNewMessages));
            } else {
                unreadBadgeText.setText(msgsReceived.size() + "");
            }
        } else {
            unreadBadgeLayout.setVisibility(View.GONE);
        }

    }

    public MegaApiAndroid getLocalMegaApiFolder() {

        PackageManager m = getPackageManager();
        String s = getPackageName();
        PackageInfo p;
        String path = null;
        try {
            p = m.getPackageInfo(s, 0);
            path = p.applicationInfo.dataDir + "/";
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        MegaApiAndroid megaApiFolder = new MegaApiAndroid(MegaApplication.APP_KEY, BuildConfig.USER_AGENT, path);

        megaApiFolder.setDownloadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);
        megaApiFolder.setUploadMethod(MegaApiJava.TRANSFER_METHOD_AUTO_ALTERNATIVE);

        return megaApiFolder;
    }

    public File createImageFile() {
        Timber.d("createImageFile");
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "picture" + timeStamp + "_";
        File storageDir = getExternalFilesDir(null);
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        return new File(storageDir, imageFileName + ".jpg");
    }

    /**
     * Method to control whether or not the expand text view icon should be displayed.
     *
     * @param linesCount number of lines written
     */
    private void controlExpandableInputText(int linesCount) {
        expandCollapseInputTextLayout.setVisibility(linesCount >= MIN_LINES_TO_EXPAND_INPUT_TEXT ? View.VISIBLE : View.GONE);
    }

    /**
     * Method for collapsing the input text.
     */
    private void collapseInputText() {
        isInputTextExpanded = false;
        checkExpandOrCollapseInputText();
    }

    /**
     * Method to control whether to expand or collapse the input text.
     */
    private void checkExpandOrCollapseInputText() {
        if (!isInputTextExpanded) {
            expandCollapseInputTextLayout.setPadding(0, dp2px(editMsgLayout.getVisibility() == View.VISIBLE ?
                    58 : 18, getOutMetrics()), 0, 0);

            ColorUtils.changeStatusBarColor(this, R.color.white_transparent);
            writingContainerLayout.setBackgroundResource(android.R.color.transparent);
            inputTextContainer.setBackground(null);
            writeMsgLayout.setBackground(ContextCompat.getDrawable(this, R.drawable.background_write_layout));

            tB.setVisibility(View.VISIBLE);
            expandCollapseInputTextIcon.setImageResource(R.drawable.ic_expand_text_input);

            writingContainerLayout.getLayoutParams().height = WRAP_CONTENT;
            inputTextContainer.getLayoutParams().height = WRAP_CONTENT;

            inputTextLayout.getLayoutParams().height = WRAP_CONTENT;
            ConstraintLayout constraintLayout = findViewById(R.id.input_text_container);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(R.id.input_text_layout, ConstraintSet.START, R.id.input_text_container, ConstraintSet.START, 0);
            constraintSet.connect(R.id.input_text_layout, ConstraintSet.END, R.id.input_text_container, ConstraintSet.END, 0);
            constraintSet.connect(R.id.input_text_layout, ConstraintSet.BOTTOM, R.id.input_text_container, ConstraintSet.BOTTOM, 0);
            constraintSet.applyTo(constraintLayout);

            writeMsgAndExpandLayout.getLayoutParams().height = WRAP_CONTENT;
            writeMsgLayout.getLayoutParams().height = WRAP_CONTENT;
            editMsgLinearLayout.getLayoutParams().height = WRAP_CONTENT;
            textChat.getLayoutParams().height = WRAP_CONTENT;

            textChat.setPadding(0, dp2px(10, getOutMetrics()), 0, dp2px(8, getOutMetrics()));
        } else {
            expandCollapseInputTextLayout.setPadding(0, dp2px(editMsgLayout.getVisibility() == View.VISIBLE ?
                    71 : 18, getOutMetrics()), 0, 0);

            ColorUtils.changeStatusBarColor(this, R.color.dark_grey_alpha_050);
            writingContainerLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.dark_grey_alpha_050));
            inputTextContainer.setBackground(ContextCompat.getDrawable(this, R.drawable.background_expanded_write_layout));

            writeMsgLayout.setBackground(null);

            tB.setVisibility(View.GONE);
            expandCollapseInputTextIcon.setImageResource(R.drawable.ic_collapse_text_input);

            writingContainerLayout.getLayoutParams().height = MATCH_PARENT;
            inputTextContainer.getLayoutParams().height = MATCH_PARENT;

            inputTextLayout.getLayoutParams().height = 0;
            ConstraintLayout constraintLayout = findViewById(R.id.input_text_container);
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(constraintLayout);
            constraintSet.connect(R.id.input_text_layout, ConstraintSet.TOP, R.id.input_text_container, ConstraintSet.TOP, 0);
            constraintSet.connect(R.id.input_text_layout, ConstraintSet.START, R.id.input_text_container, ConstraintSet.START, 0);
            constraintSet.connect(R.id.input_text_layout, ConstraintSet.END, R.id.input_text_container, ConstraintSet.END, 0);
            constraintSet.connect(R.id.input_text_layout, ConstraintSet.BOTTOM, R.id.input_text_container, ConstraintSet.BOTTOM, 0);
            constraintSet.applyTo(constraintLayout);

            writeMsgAndExpandLayout.getLayoutParams().height = MATCH_PARENT;
            writeMsgLayout.getLayoutParams().height = MATCH_PARENT;
            editMsgLinearLayout.getLayoutParams().height = MATCH_PARENT;
            textChat.getLayoutParams().height = MATCH_PARENT;

            textChat.setPadding(0, dp2px(10, getOutMetrics()), 0, dp2px(40, getOutMetrics()));
        }

        writingContainerLayout.requestLayout();
        inputTextContainer.requestLayout();
        inputTextLayout.requestLayout();

        writeMsgAndExpandLayout.requestLayout();
        writeMsgLayout.requestLayout();
        editMsgLinearLayout.requestLayout();
        textChat.requestLayout();

        setSizeInputText(isTextEmpty(textChat.getText().toString()));
    }

    /**
     * Manages the result after pick an image with camera.
     */
    private void onCaptureImageResult() {
        if (mOutputFilePath == null) {
            Timber.d("mOutputFilePath is null");
            return;
        }

        File f = new File(mOutputFilePath);

        File publicFile = FileUtil.copyFileToDCIM(f);
        //Remove mOutputFilePath
        if (f.exists()) {
            if (f.isDirectory()) {
                if (f.list() != null && f.list().length <= 0) {
                    f.delete();
                }
            } else {
                f.delete();
            }
        }

        Uri finalUri = Uri.fromFile(publicFile);
        galleryAddPic(finalUri);
        PermissionUtils.checkNotificationsPermission(this);
        uploadPictureOrVoiceClip(publicFile.getPath());
    }

    private void galleryAddPic(Uri contentUri) {
        if (contentUri != null) {
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri);
            mediaScanIntent.setPackage(getApplicationContext().getPackageName());
            sendBroadcast(mediaScanIntent);
        }
    }

    public void hideKeyboard() {
        if (emojiKeyboard == null)
            return;

        emojiKeyboard.hideBothKeyboard(this);
    }

    public void showConfirmationConnect() {
        Timber.d("showConfirmationConnect");

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        navigateToLogin(true);
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        Timber.d("BUTTON_NEGATIVE");
                        break;
                }
            }
        };

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        try {
            builder.setMessage(R.string.confirmation_to_reconnect).setPositiveButton(R.string.general_ok, dialogClickListener)
                    .setNegativeButton(R.string.general_cancel, dialogClickListener).show().setCanceledOnTouchOutside(false);
        } catch (Exception e) {
        }
    }

    public int getDeviceDensity() {
        int screen = 0;
        switch (getResources().getDisplayMetrics().densityDpi) {
            case DisplayMetrics.DENSITY_LOW:
                screen = 1;
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                screen = 1;
                break;
            case DisplayMetrics.DENSITY_HIGH:
                screen = 1;
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                screen = 0;
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                screen = 0;
                break;
            case DisplayMetrics.DENSITY_XXXHIGH:
                screen = 0;
                break;
            default:
                screen = 0;
        }
        return screen;
    }

    public void setNodeAttachmentVisible() {
        Timber.d("setNodeAttachmentVisible");
        if (adapter != null && holder_imageDrag != null && position_imageDrag != -1) {
            adapter.setNodeAttachmentVisibility(true, holder_imageDrag, position_imageDrag);
            holder_imageDrag = null;
            position_imageDrag = -1;
        }
    }

    private void addInBufferSending(AndroidMegaChatMessage androidMsg) {
        if (bufferSending.isEmpty()) {
            bufferSending.add(0, androidMsg);
        } else {
            boolean isContained = false;
            for (int i = 0; i < bufferSending.size(); i++) {
                if ((bufferSending.get(i).getMessage().getMsgId() == androidMsg.getMessage().getMsgId()) && (bufferSending.get(i).getMessage().getTempId() == androidMsg.getMessage().getTempId())) {
                    isContained = true;
                    break;
                }
            }
            if (!isContained) {
                bufferSending.add(0, androidMsg);
            }
        }
    }

    private void createSpeakerAudioManger() {
        rtcAudioManager = rtcAudioManagerGateway.getAudioManager();

        if (rtcAudioManager == null) {
            speakerWasActivated = true;
            rtcAudioManager = AppRTCAudioManager.create(this, speakerWasActivated, AUDIO_MANAGER_PLAY_VOICE_CLIP);
        } else {
            activateSpeaker();
        }

        rtcAudioManager.setProximitySensorListener(isNear -> {
            if (!speakerWasActivated && !isNear) {
                adapter.pausePlaybackInProgress();
            } else if (speakerWasActivated && isNear) {
                adapter.refreshVoiceClipPlayback();
                speakerWasActivated = false;
            }
        });
    }

    public void startProximitySensor() {
        Timber.d("Starting proximity sensor");
        createSpeakerAudioManger();
        rtcAudioManager.startProximitySensor();
    }

    private void activateSpeaker() {
        if (!speakerWasActivated) {
            speakerWasActivated = true;
        }

        if (rtcAudioManager != null) {
            MegaChatCall call = getCallInProgress();
            if (call != null) {
                if (!MegaApplication.getChatManagement().getSpeakerStatus(call.getChatid())) {
                    MegaApplication.getChatManagement().setSpeakerStatus(call.getChatid(), true);
                    rtcAudioManagerGateway.updateSpeakerStatus(true, AUDIO_MANAGER_CALL_IN_PROGRESS);
                }
            } else {
                rtcAudioManager.updateSpeakerStatus(true, AUDIO_MANAGER_PLAY_VOICE_CLIP);
            }
        }
    }

    public void stopProximitySensor() {
        if (rtcAudioManager == null || participatingInACall()) return;

        activateSpeaker();
        rtcAudioManager.unregisterProximitySensor();
        destroySpeakerAudioManger();
    }

    private void destroySpeakerAudioManger() {
        if (rtcAudioManager == null) return;
        try {
            rtcAudioManager.stop();
            rtcAudioManager = null;
        } catch (Exception e) {
            Timber.e(e, "Exception stopping speaker audio manager");
        }
    }

    public void setShareLinkDialogDismissed(boolean dismissed) {
        isShareLinkDialogDismissed = dismissed;
    }

    private void checkIfServiceCanStart(Intent intent) {
        preservedIntents.add(intent);
        if (!isAskingForMyChatFiles) {
            checkIfIsNeededToAskForMyChatFilesFolder();
        }
    }

    private void checkIfIsNeededToAskForMyChatFilesFolder() {
        if (existsMyChatFilesFolder()) {
            setMyChatFilesFolder(getMyChatFilesFolder());
            if (isForwardingFromNC()) {
                handleStoredData();
            } else {
                proceedWithAction();
            }
        } else {
            isAskingForMyChatFiles = true;
            megaApi.getMyChatFilesFolder(new GetAttrUserListener(this));
        }
    }

    public void startUploadService() {
        if (!isWaitingForMoreFiles && !preservedIntents.isEmpty()) {
            PermissionUtils.checkNotificationsPermission(this);
            for (Intent intent : preservedIntents) {
                intent.putExtra(ChatUploadService.EXTRA_PARENT_NODE, myChatFilesFolder.serialize());
                startService(intent);
            }
            preservedIntents.clear();
        }
    }

    public void setMyChatFilesFolder(MegaNode myChatFilesFolder) {
        isAskingForMyChatFiles = false;
        this.myChatFilesFolder = myChatFilesFolder;
    }

    public boolean isForwardingFromNC() {
        return isForwardingFromNC;
    }

    public void setIsWaitingForMoreFiles(boolean isWaitingForMoreFiles) {
        this.isWaitingForMoreFiles = isWaitingForMoreFiles;
    }

    public long getCurrentChatid() {
        return idChat;
    }

    Runnable updateVisualizer = new Runnable() {
        @Override
        public void run() {
            if (recordView.isRecordingNow() && recordingLayout.getVisibility() == View.VISIBLE) {
                updateAmplitudeVisualizer(myAudioRecorder.getMaxAmplitude());
                handlerVisualizer.postDelayed(this, REPEAT_INTERVAL);
            }
        }
    };

    private void updateAmplitudeVisualizer(int newAmplitude) {
        if (currentAmplitude != -1 && getRangeAmplitude(currentAmplitude) == getRangeAmplitude(newAmplitude))
            return;
        currentAmplitude = newAmplitude;
        needToUpdateVisualizer(currentAmplitude);
    }

    private int getRangeAmplitude(int value) {
        if (value < MIN_FIRST_AMPLITUDE) return NOT_SOUND;
        if (value >= MIN_FIRST_AMPLITUDE && value < MIN_SECOND_AMPLITUDE) return FIRST_RANGE;
        if (value >= MIN_SECOND_AMPLITUDE && value < MIN_THIRD_AMPLITUDE) return SECOND_RANGE;
        if (value >= MIN_THIRD_AMPLITUDE && value < MIN_FOURTH_AMPLITUDE) return THIRD_RANGE;
        if (value >= MIN_FOURTH_AMPLITUDE && value < MIN_FIFTH_AMPLITUDE) return FOURTH_RANGE;
        if (value >= MIN_FIFTH_AMPLITUDE && value < MIN_SIXTH_AMPLITUDE) return FIFTH_RANGE;
        return SIXTH_RANGE;
    }

    private void changeColor(RelativeLayout bar, boolean isLow) {
        Drawable background;
        if (isLow) {
            background = ContextCompat.getDrawable(this, R.drawable.recording_low);
        } else {
            background = ContextCompat.getDrawable(this, R.drawable.recording_high);
        }
        if (bar.getBackground() == background) return;
        bar.setBackground(background);
    }

    private void needToUpdateVisualizer(int currentAmplitude) {
        int resultRange = getRangeAmplitude(currentAmplitude);

        if (resultRange == NOT_SOUND) {
            initRecordingItems(IS_LOW);
            return;
        }
        if (resultRange == SIXTH_RANGE) {
            initRecordingItems(IS_HIGH);
            return;
        }
        changeColor(firstBar, IS_HIGH);
        changeColor(sixthBar, IS_LOW);

        if (resultRange > FIRST_RANGE) {
            changeColor(secondBar, IS_HIGH);
            if (resultRange > SECOND_RANGE) {
                changeColor(thirdBar, IS_HIGH);
                if (resultRange > THIRD_RANGE) {
                    changeColor(fourthBar, IS_HIGH);
                    if (resultRange > FOURTH_RANGE) {
                        changeColor(fifthBar, IS_HIGH);
                    } else {
                        changeColor(fifthBar, IS_LOW);
                    }
                } else {
                    changeColor(fourthBar, IS_LOW);
                    changeColor(fifthBar, IS_LOW);
                }
            } else {
                changeColor(thirdBar, IS_LOW);
                changeColor(fourthBar, IS_LOW);
                changeColor(fifthBar, IS_LOW);
            }
        } else {
            changeColor(secondBar, IS_LOW);
            changeColor(thirdBar, IS_LOW);
            changeColor(fourthBar, IS_LOW);
            changeColor(fifthBar, IS_LOW);
        }
    }

    public long getLastIdMsgSeen() {
        return lastIdMsgSeen;
    }

    public long getGeneralUnreadCount() {
        return generalUnreadCount;
    }

    public void setPositionNewMessagesLayout(int positionNewMessagesLayout) {
        this.positionNewMessagesLayout = positionNewMessagesLayout;
    }

    /**
     * Checks if it is already joining or leaving the chat to set the right UI.
     */
    private void checkIfIsAlreadyJoiningOrLeaving() {
        if (MegaApplication.getChatManagement().isAlreadyJoining(idChat)) {
            viewModel.setIsJoiningOrLeaving(true, R.string.joining_label);
        } else if (MegaApplication.getChatManagement().isAlreadyLeaving(idChat)) {
            viewModel.setIsJoiningOrLeaving(true, R.string.leaving_label);
        }
    }

    /**
     * Sets the joining or leaving state depending on the action received.
     *
     * @param actionId String ID which indicates if the state to set is the joining or leaving.
     */
    private void setJoiningOrLeaving(@StringRes int actionId) {
        viewModel.setIsJoiningOrLeaving(true, actionId);
    }

    /**
     * Sets the joining or leaving UI depending on the action received.
     *
     * @param actionId String ID which indicates if the UI to set is the joining or leaving state.
     */
    private void setJoiningOrLeavingView(@StringRes int actionId) {
        joiningLeavingText.setText(getString(actionId));
        setBottomLayout(SHOW_JOINING_OR_LEFTING_LAYOUT);
        invalidateOptionsMenu();
    }

    /**
     * Checks if the chat is already joining.
     *
     * @return True if the chat is already joining, false otherwise.
     */
    private boolean isAlreadyJoining(long id) {
        return MegaApplication.getChatManagement().isAlreadyJoining(id);
    }

    /**
     * Add joining chat ID
     *
     * @param id The joining chat ID
     */
    private void addJoiningChatId(long id) {
        MegaApplication.getChatManagement().addJoiningChatId(id);
    }

    public void setLastIdMsgSeen(long lastIdMsgSeen) {
        this.lastIdMsgSeen = lastIdMsgSeen;
    }

    /**
     * Gets the visible positions on adapter and updates the uploading messages between them, if any.
     */
    public void updatePausedUploadingMessages() {
        if (mLayoutManager == null || adapter == null) {
            return;
        }

        adapter.updatePausedUploadingMessages(mLayoutManager.findFirstVisibleItemPosition(),
                mLayoutManager.findLastVisibleItemPosition());
    }

    /*
     * Gets the position of an attachment message if it is visible and exists.
     *
     * @param handle The handle of the attachment.
     * @return The position of the message if it is visible and exists, INVALID_POSITION otherwise.
     */
    public int getPositionOfAttachmentMessageIfVisible(long handle) {
        if (mLayoutManager == null || adapter == null) {
            return INVALID_POSITION;
        }

        int firstVisiblePosition = mLayoutManager.findFirstVisibleItemPosition();
        if (firstVisiblePosition == INVALID_POSITION || firstVisiblePosition == 0) {
            firstVisiblePosition = 1;
        }

        int lastVisiblePosition = mLayoutManager.findLastVisibleItemPosition();
        if (lastVisiblePosition == INVALID_POSITION) {
            lastVisiblePosition = adapter.getItemCount() - 1;
        }

        for (int i = lastVisiblePosition; i >= firstVisiblePosition; i--) {
            AndroidMegaChatMessage msg = adapter.getMessageAtAdapterPosition(i);
            MegaChatMessage chatMessage = msg.getMessage();
            if (chatMessage != null
                    && chatMessage.getMegaNodeList() != null
                    && chatMessage.getMegaNodeList().get(0) != null
                    && chatMessage.getMegaNodeList().get(0).getHandle() == handle) {
                return i;
            }
        }

        return INVALID_POSITION;
    }

    /**
     * Method to display a dialog to show the error related with the chat reactions.
     *
     * @param typeError Type of Error.
     */
    public void createLimitReactionsAlertDialog(long typeError) {
        MaterialAlertDialogBuilder dialogBuilder = new MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_Mega_MaterialAlertDialog);
        dialogBuilder.setMessage(typeError == REACTION_ERROR_TYPE_USER
                        ? getString(R.string.limit_reaction_per_user, MAX_REACTIONS_PER_USER)
                        : getString(R.string.limit_reaction_per_message, MAX_REACTIONS_PER_MESSAGE))
                .setOnDismissListener(dialog -> {
                    errorReactionsDialogIsShown = false;
                    typeErrorReaction = REACTION_ERROR_DEFAULT_VALUE;
                })
                .setPositiveButton(getString(R.string.general_ok),
                        (dialog, which) -> {
                            dialog.dismiss();
                        });

        errorReactionsDialog = dialogBuilder.create();
        errorReactionsDialog.show();
        errorReactionsDialogIsShown = true;
        typeErrorReaction = typeError;
    }

    @Override
    public void showSnackbar(int type, String content, long chatId) {
        showSnackbar(type, fragmentContainer, content, chatId);
    }

    @Override
    public void onSendSuccess(@NotNull AndroidMegaChatMessage message) {
        sendMessageToUI(message);
    }

    /**
     * Method for correctly updating the id of the last read message.
     */
    private void checkLastSeenId() {
        if (lastIdMsgSeen == INVALID_LAST_SEEN_ID && messages != null && !messages.isEmpty() && messages.get(0) != null && messages.get(0).getMessage() != null) {
            lastIdMsgSeen = messages.get(0).getMessage().getMsgId();
            updateLocalLastSeenId();
        }
    }

    /**
     * Method to find the appropriate position of unread messages. Taking into account the last received message that is read and the messages sent by me.
     */
    private void updateLocalLastSeenId() {
        int positionLastMessage = -1;
        for (int i = messages.size() - 1; i >= 0; i--) {
            AndroidMegaChatMessage androidMessage = messages.get(i);
            if (androidMessage != null && !androidMessage.isUploading()) {
                MegaChatMessage msg = androidMessage.getMessage();
                if (msg != null && msg.getMsgId() == lastIdMsgSeen) {
                    positionLastMessage = i;
                    break;
                }
            }
        }

        positionLastMessage = positionLastMessage + 1;
        if (positionLastMessage >= messages.size())
            return;

        AndroidMegaChatMessage message = messages.get(positionLastMessage);

        while (message.getMessage().getUserHandle() == megaChatApi.getMyUserHandle()) {
            lastIdMsgSeen = message.getMessage().getMsgId();
            positionLastMessage = positionLastMessage + 1;
            if (positionLastMessage < messages.size()) {
                message = messages.get(positionLastMessage);
            } else {
                break;
            }
        }
    }

    /**
     * Receive changes to OnChatListItemUpdate, OnChatOnlineStatusUpdate, OnChatConnectionStateUpdate and OnChatPresenceLastGreen and make the necessary changes
     */
    private void checkChatChanges() {
        Disposable chatSubscription = getChatChangesUseCase.get()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe((next) -> {
                    if (next instanceof GetChatChangesUseCase.Result.OnChatOnlineStatusUpdate) {
                        int status = ((GetChatChangesUseCase.Result.OnChatOnlineStatusUpdate) next).component2();
                        setChatSubtitle();
                        requestLastGreen(status);
                    }

                    if (next instanceof GetChatChangesUseCase.Result.OnChatConnectionStateUpdate) {
                        long chatId = ((GetChatChangesUseCase.Result.OnChatConnectionStateUpdate) next).component1();
                        int newState = ((GetChatChangesUseCase.Result.OnChatConnectionStateUpdate) next).component2();
                        onChatConnectionStateUpdate(chatId, newState);
                    }

                    if (next instanceof GetChatChangesUseCase.Result.OnChatPresenceLastGreen) {
                        long userHandle = ((GetChatChangesUseCase.Result.OnChatPresenceLastGreen) next).component1();
                        int lastGreen = ((GetChatChangesUseCase.Result.OnChatPresenceLastGreen) next).component2();
                        onChatPresenceLastGreen(userHandle, lastGreen);
                    }

                }, Timber::e);

        composite.add(chatSubscription);
    }
}
