package com.dji.importsdkdemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.hushiyu1995.yolo_v3.Yolo;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import dji.common.camera.SettingsDefinitions;
import dji.common.error.DJICameraError;
import dji.common.error.DJIError;
import dji.common.gimbal.Rotation;
import dji.common.gimbal.RotationMode;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.base.BaseProduct;
import dji.sdk.gimbal.Gimbal;
import dji.sdk.media.DownloadListener;
import dji.sdk.media.FetchMediaTask;
import dji.sdk.media.FetchMediaTaskContent;
import dji.sdk.media.FetchMediaTaskScheduler;
import dji.sdk.media.MediaFile;
import dji.sdk.media.MediaManager;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class YoloMediaActivity extends Activity implements View.OnClickListener{
    private  static final String TAG= YoloMediaActivity.class.getName();
    private Button mBackBtn, mDeleteBtn,mReloadBtn, mDownloadBtn,mStatusBtn;
    private Button mPlayBtn,mResumeBtn,mPauseBtn,mStopBtn,mMoveToBtn;
    private Button mGimbalRestbtn,mGimbaUpbtn,mGimbalDownbtn,mGimbalStopbtn;
    private RecyclerView listView;
    private FileListAdapter mListAdapter;
    private SlidingDrawer mPushDrawerSd;
    private ImageView mDisplayImageView;
    private TextView mPushTv;
    private List<MediaFile> mediaFileList = new ArrayList<MediaFile>();
    private MediaManager mMediaManager;
    private MediaManager.FileListState currentFileListState = MediaManager.FileListState.UNKNOWN;
    private ProgressDialog mLoadingDialog;
    private ProgressDialog mDownloadDialog;
    //yun tai
    private Timer timer;
    private GimbalRotateTimerTask gimbalRotationTimerTask;
    private Gimbal gimbal = null;
    private int currentGimbalId = 0;


    private FetchMediaTaskScheduler scheduler;//对下载任务的调度
    private int lastClickViewIndex = -1;
    private MediaManager.VideoPlaybackStateListener updatedVideoPlaybackStateListener =
            new MediaManager.VideoPlaybackStateListener() {
                @Override
                public void onUpdate(MediaManager.VideoPlaybackState videoPlaybackState) {
                    updateStatusTextView(videoPlaybackState);
                }
            };
    private View lastClickView;
    private MediaManager.FileListStateListener updateFileListStateListener = new MediaManager.FileListStateListener() {
        @Override
        public void onFileListStateChange(MediaManager.FileListState fileListState) {
            currentFileListState = fileListState;
        }
    };
    File destDir = new File(Environment.getExternalStorageDirectory().getPath() + "/DjiYoloDownload/");
    private int currentProgress = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_midea_yolo);
        initUI();
    }
    @Override
    protected void onResume() {
        super.onResume();
        initMediaManager();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        lastClickView=null;
        if (mMediaManager != null) {
            mMediaManager.stop(null);
            mMediaManager.removeFileListStateCallback(this.updateFileListStateListener);
            mMediaManager.removeMediaUpdatedVideoPlaybackStateListener(updatedVideoPlaybackStateListener);
            mMediaManager.exitMediaDownloading();
            if (scheduler!=null) {
                scheduler.removeAllTasks();
            }
        }
        FPVDemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.SHOOT_PHOTO, new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError djiError) {
                if (djiError !=null){
                    setResultToToast("设置相机模式失败"+djiError.getDescription());
                }
            }
        });
        if(mediaFileList!=null){
            mediaFileList.clear();
        }
        super.onDestroy();
    }

    private void setResultToToast(final String s) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(YoloMediaActivity.this, s, Toast.LENGTH_SHORT).show();
            }
        });

    }


    private void initUI() {
        // init recyclerView
        listView = findViewById(R.id.filelistView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(YoloMediaActivity.this, RecyclerView.VERTICAL ,false);
        listView.setLayoutManager(layoutManager);
        mPushDrawerSd = findViewById(R.id.pointing_drawer_sd);
        mPushTv = findViewById(R.id.pointing_push_tv);
        mBackBtn = findViewById(R.id.back_btn);
        mDeleteBtn = (Button) findViewById(R.id.delete_btn);
        mDownloadBtn = (Button) findViewById(R.id.download_btn);
        mReloadBtn = (Button) findViewById(R.id.reload_btn);
        mStatusBtn = (Button) findViewById(R.id.status_btn);
        mPlayBtn = (Button) findViewById(R.id.play_btn);
        mResumeBtn = (Button) findViewById(R.id.resume_btn);
        mPauseBtn = (Button) findViewById(R.id.pause_btn);
        mStopBtn = (Button) findViewById(R.id.stop_btn);
        mMoveToBtn = (Button) findViewById(R.id.moveTo_btn);
        mGimbalDownbtn = findViewById(R.id.gimbal_down_btn);
        mGimbalRestbtn = findViewById(R.id.gimbal_reset_btn);
        mGimbalStopbtn = findViewById(R.id.gimbal_stop_btn);
        mGimbaUpbtn = findViewById(R.id.gimbal_up_btn);

        mDisplayImageView = (ImageView) findViewById(R.id.imageView);
        mDisplayImageView.setVisibility(View.VISIBLE);
        mBackBtn.setOnClickListener(this);
        mDeleteBtn.setOnClickListener(this);
        mDownloadBtn.setOnClickListener(this);
        mReloadBtn.setOnClickListener(this);
        mDownloadBtn.setOnClickListener(this);
        mStatusBtn.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
        mResumeBtn.setOnClickListener(this);
        mPauseBtn.setOnClickListener(this);
        mStopBtn.setOnClickListener(this);
        mMoveToBtn.setOnClickListener(this);

        mGimbaUpbtn.setOnClickListener(this);
        mGimbalStopbtn.setOnClickListener(this);
        mGimbalRestbtn.setOnClickListener(this);
        mGimbalDownbtn.setOnClickListener(this);

        //Init FileListAdapter
        mListAdapter = new FileListAdapter();
        listView.setAdapter(mListAdapter);


//Init Loading Dialog
        mLoadingDialog = new ProgressDialog(YoloMediaActivity.this);
        mLoadingDialog.setMessage("请稍等");
        mLoadingDialog.setCanceledOnTouchOutside(false);
        mLoadingDialog.setCancelable(false);
        // init download dialog
        mDownloadDialog = new ProgressDialog(YoloMediaActivity.this);
        mDownloadDialog.setTitle("下载文件");
        mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mDownloadDialog.setIcon(android.R.drawable.ic_dialog_info);
        mDownloadDialog.setCanceledOnTouchOutside(false);
        mDownloadDialog.setCancelable(true);
        mDownloadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                @Override
                                                public void onCancel(DialogInterface dialog) {
                                                    if(mMediaManager!=null){
                                                        mMediaManager.exitMediaDownloading();
                                                    }

                                                }
                                            }

        );
    }
    private void showProgressDialog() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (mLoadingDialog != null) {
                    mLoadingDialog.show();
                }
            }
        });
    }

    private void hideProgressDialog() {
        runOnUiThread(new Runnable() {
            public void run() {
                if (null != mLoadingDialog && mLoadingDialog.isShowing()) {
                    mLoadingDialog.dismiss();
                }
            }
        });
    }
    //隐藏和展示下载框
    private void ShowDownloadProgressDialog() {
        if (mDownloadDialog != null) {
            runOnUiThread(new Runnable() {
                public void run() {
                    mDownloadDialog.incrementProgressBy(-mDownloadDialog.getProgress());
                    mDownloadDialog.show();
                }
            });
        }
    }

    private void HideDownloadProgressDialog() {

        if (null != mDownloadDialog && mDownloadDialog.isShowing()) {
            runOnUiThread(new Runnable() {
                public void run() {
                    mDownloadDialog.dismiss();
                }
            });
        }
    }
    private void downloadFileByIndex(final int index){
        if ((mediaFileList.get(index).getMediaType()==MediaFile.MediaType.PANORAMA)
        || (mediaFileList.get(index).getMediaType()==MediaFile.MediaType.SHALLOW_FOCUS)){
            setResultToToast("全景低像素");
            return;
        }
        mediaFileList.get(index).fetchFileData(destDir, null, new DownloadListener<String>() {
            @Override
            public void onStart() {
                currentProgress = -1;
                ShowDownloadProgressDialog();
            }

            @Override
            public void onRateUpdate(long total, long current, long persize) {
                int tmpProgress = (int) (1.0 * current / total * 100);
                if (tmpProgress != currentProgress) {
                    mDownloadDialog.setProgress(tmpProgress);
                    currentProgress = tmpProgress;
                }

            }

            @Override
            public void onProgress(long l, long l1) {

            }

            @Override
            public void onSuccess(String filePath) {
                HideDownloadProgressDialog();
                setResultToToast("已下载至" + ":" + filePath);
                currentProgress = -1;
            }

            @Override
            public void onFailure(DJIError djiError) {
                HideDownloadProgressDialog();
                setResultToToast("下载文件失败" + djiError.getDescription());
                currentProgress = -1;
            }
        });
    }
    private void deleteFileByIndex(final int index) {
        ArrayList<MediaFile> fileToDelete = new ArrayList<MediaFile>();
        if (mediaFileList.size() > index) {
            fileToDelete.add(mediaFileList.get(index));
            mMediaManager.deleteFiles(fileToDelete, new CommonCallbacks.CompletionCallbackWithTwoParam<List<MediaFile>, DJICameraError>() {
                @Override
                public void onSuccess(List<MediaFile> x, DJICameraError y) {
                    DJILog.e(TAG, "Delete file success");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            MediaFile file = mediaFileList.remove(index);

                            //Reset select view
                            lastClickViewIndex = -1;
                            lastClickView = null;

                            //Update recyclerView
                            mListAdapter.notifyItemRemoved(index);
                        }
                    });
                }

                @Override
                public void onFailure(DJIError error) {
                    setResultToToast("Delete file failed");
                }
            });
        }
    }
    //初始化媒体管理器
    private void initMediaManager(){
        if (FPVDemoApplication.getmProductInstance() == null
        ){
            mediaFileList.clear();
            mListAdapter.notifyDataSetChanged();
            setResultToToast("初始化相机失败");
            return;
        }else{
            if(FPVDemoApplication.getCameraInstance()!=null&& FPVDemoApplication.getCameraInstance().isMediaDownloadModeSupported()){
                mMediaManager=FPVDemoApplication.getCameraInstance().getMediaManager();
                if (null!=mMediaManager){
                    mMediaManager.addUpdateFileListStateListener(this.updateFileListStateListener);
                    mMediaManager.addMediaUpdatedVideoPlaybackStateListener(this.updatedVideoPlaybackStateListener);
                    FPVDemoApplication.getCameraInstance().setMode(SettingsDefinitions.CameraMode.MEDIA_DOWNLOAD, new CommonCallbacks.CompletionCallback() {
                        @Override
                        public void onResult(DJIError djiError) {
                            if (djiError==null){
                                setResultToToast("download 相机模式成功");
                                showProgressDialog();
                                getFileList();
                            } else{
                                setResultToToast("download 模式失败"+djiError.getDescription());
                            }
                            scheduler= mMediaManager.getScheduler();
                        }
                    });
                }
            }else if (null != FPVDemoApplication.getCameraInstance()&&!FPVDemoApplication.getCameraInstance().isMediaDownloadModeSupported()){
                setResultToToast("你购买的产品不支持下载");
            }
        }
        return;
    }
    //获取无人机上的文件
    private void getFileList() {
        mMediaManager = FPVDemoApplication.getCameraInstance().getMediaManager();
        //setResultToToast("getfilelistzhengzaidiaoyong");
        if (mMediaManager != null) {
            if ((currentFileListState == MediaManager.FileListState.SYNCING) || (currentFileListState == MediaManager.FileListState.DELETING)) {
                setResultToToast("媒体管理器正在繁忙");
            } else {
                //refreshFile 无论在sd卡还是在本机上都可以
                mMediaManager.refreshFileListOfStorageLocation(SettingsDefinitions.StorageLocation.INTERNAL_STORAGE, new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError djiError) {
                        if (null == djiError) {
                            hideProgressDialog();
                            if (currentFileListState != MediaManager.FileListState.INCOMPLETE) {
                                mediaFileList.clear();
                                lastClickViewIndex = -1;
                                lastClickView = null;
                            }
                            //getSDCard从sd卡中读取数据，getSDCardFileListSnapshot、、、
                            mediaFileList = mMediaManager.getInternalStorageFileListSnapshot();
                            Collections.sort(mediaFileList, new Comparator<MediaFile>() {
                                @Override
                                public int compare(MediaFile lhs, MediaFile rhs) {
                                    if (lhs.getTimeCreated() < rhs.getTimeCreated()) {
                                        return 1;
                                    } else if (lhs.getTimeCreated() > rhs.getTimeCreated()) {
                                        return -1;
                                    }
                                    return 0;
                                }
                            });
                            scheduler.resume(new CommonCallbacks.CompletionCallback() {
                                @Override
                                public void onResult(DJIError error) {
                                    if (error == null) {
                                        getThumbnails();
                                    }
                                }
                            });
                        } else {
                            hideProgressDialog();
                            setResultToToast("获取文件列表:" + djiError.getDescription());
                        }
                    }

                });
            }
        }
    }
    //得到缩略图
    private void getThumbnailByIndex(final int index) {
        FetchMediaTask task = new FetchMediaTask(mediaFileList.get(index), FetchMediaTaskContent.THUMBNAIL, taskCallback);
        scheduler.moveTaskToEnd(task);
    }

    private void getThumbnails() {
        if (mediaFileList.size() <= 0) {
            setResultToToast("没有文件信息下载缩略图");
            return;
        }
        for (int i = 0; i < mediaFileList.size(); i++) {
            getThumbnailByIndex(i);
        }
    }
    private FetchMediaTask.Callback taskCallback = new FetchMediaTask.Callback() {
        @Override
        public void onUpdate(MediaFile file, FetchMediaTaskContent option, DJIError error) {
            if (null == error) {
                if (option == FetchMediaTaskContent.PREVIEW) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mListAdapter.notifyDataSetChanged();
                        }
                    });
                }
                if (option == FetchMediaTaskContent.THUMBNAIL) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            mListAdapter.notifyDataSetChanged();
                        }
                    });
                }
            } else {
                DJILog.e(TAG, "获取媒体任务失败" + error.getDescription());
            }
        }
    };
    //在getFileList()方法中，获取最新的mMediaManager对象并检查它是否为空。然后检查currentFileListState
    // 变量的值。如果状态既不同步也不删除，那么调用MediaManager的refreshFileListOfStorageLocation()方法来刷新
    // SD卡上的文件列表。在onResult()回调方法中，如果没有错误，那么检查currentFileListState值是否不等于MediaMan
    // ager.FileListState。不完整并重新设置mediaFileList列表、lastClickViewIndex和lastClickView变量。
    // 调用MediaManager的getSDCardFileListSnapshot()方法来获取当前文件列表并将其存储在mediaFileList变量中。
    // 根据创建的时间对mediaFileList中的媒体文件进行排序。
    // 然后调用FetchMediaTaskScheduler的resume()方法来恢复调度器，并
    // 调用onResult()回调方法中的getThumbnails()方法。如果出现错误，
    // 调用hideProgressDialog()方法来隐藏进度对话框。接下来，创
    // 建getThumbnailByIndex()方法来为FetchMediaTaskContent初始化FetchMediaTask任务。
    // 然后将任务移动到FetchMediaTaskScheduler的末尾。
    // 然后，创建getThumbnails()方法来遍历mediaFileList中的文件，
    // 并调用getThumbnailByIndex()方法来初始化FetchMediaTask任务。
    // 最后，初始化taskCallback变量并实现onUpdate()回调方法。如果没有错误，
    // 检查选项变量的值。如果该值等于FetchMediaTaskContent中的任何一个。
    // 预览或FetchMediaTaskContent。
    // 在UI线程中调用FileListAdapter的notifyDataSetChanged()方法来更新listView。
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back_btn: {
                Intent intent = new Intent(YoloMediaActivity.this,MainActivity.class);
                startActivity(intent);
                this.finish();
                break;
            }
            case R.id.delete_btn:{
                deleteFileByIndex(lastClickViewIndex);

                break;
            }
            case R.id.reload_btn: {
                getFileList();
                break;
            }
            case R.id.download_btn: {
                downloadFileByIndex(lastClickViewIndex);
                break;
            }
            case R.id.status_btn: {
                if (mPushDrawerSd.isOpened()) {
                    mPushDrawerSd.animateClose();
                } else {
                    mPushDrawerSd.animateOpen();
                }
                break;
            }
            case R.id.play_btn: {
                playVideo();
                break;
            }
            case R.id.resume_btn: {
                mMediaManager.resume(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (null != error) {
                            setResultToToast("恢复视频失败" + error.getDescription());
                        } else {
                            DJILog.e(TAG, "恢复视频成功");
                        }
                    }
                });
                break;
            }
            case R.id.pause_btn: {
                mMediaManager.pause(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (null != error) {
                            setResultToToast("Pause Video Failed" + error.getDescription());
                        } else {
                            DJILog.e(TAG, "Pause Video Success");
                        }
                    }
                });
                break;
            }
            case R.id.stop_btn: {
                mMediaManager.stop(new CommonCallbacks.CompletionCallback() {
                    @Override
                    public void onResult(DJIError error) {
                        if (null != error) {
                            setResultToToast("Stop Video Failed" + error.getDescription());
                        } else {
                            DJILog.e(TAG, "Stop Video Success");
                        }
                    }
                });
                break;
            }
            case R.id.moveTo_btn: {
                moveToPosition();
                break;
            }
            case R.id.gimbal_reset_btn:{
                Gimbal gimbal = getGimbalInstance();
                if (gimbal != null) {
                    gimbal.reset(null);
                    showToast("重置成功");
                } else {
                   showToast("The gimbal is disconnected.");
                }
                break;
            }
            case R.id.gimbal_down_btn:{
                handleDownBtnClick();
                break;
            }
            case R.id.gimbal_stop_btn:{
                handleStopBtnClick();
                break;
            }
            case R.id.gimbal_up_btn:{
                handleUpBtnClick();
                break;
            }

            default:
                break;
        }

    }
    //初始化
    private class ItemHolder extends RecyclerView.ViewHolder{
        ImageView thumbnail_img;
        TextView file_name;
        TextView file_type;
        TextView file_size;
        TextView file_time;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            this.file_name = (TextView) itemView.findViewById(R.id.filename);
            this.thumbnail_img = itemView.findViewById(R.id.filethumbnail);
            this.file_type = (TextView) itemView.findViewById(R.id.filetype);
            this.file_size = (TextView) itemView.findViewById(R.id.fileSize);
            this.file_time = (TextView) itemView.findViewById(R.id.filetime);

        }
    }

    private class FileListAdapter extends RecyclerView.Adapter<ItemHolder> {
        @Override
        public int getItemCount() {
            if (mediaFileList != null) {
                return mediaFileList.size();
            }
            return 0;
        }

        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.media_info_item, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(ItemHolder mItemHolder, final int index) {

            final MediaFile mediaFile = mediaFileList.get(index);
            if (mediaFile != null) {
                if (mediaFile.getMediaType() != MediaFile.MediaType.MOV && mediaFile.getMediaType() != MediaFile.MediaType.MP4) {
                    mItemHolder.file_time.setVisibility(View.GONE);
                } else {
                    mItemHolder.file_time.setVisibility(View.VISIBLE);
                    mItemHolder.file_time.setText(mediaFile.getDurationInSeconds() + " s");
                }
                mItemHolder.file_name.setText(mediaFile.getFileName());
                mItemHolder.file_type.setText(mediaFile.getMediaType().name());
                mItemHolder.file_size.setText(mediaFile.getFileSize() + " Bytes");
                mItemHolder.thumbnail_img.setImageBitmap(mediaFile.getThumbnail());
                mItemHolder.thumbnail_img.setOnClickListener(ImgOnClickListener);
                mItemHolder.thumbnail_img.setTag(mediaFile);
                mItemHolder.itemView.setTag(index);

                if (lastClickViewIndex == index) {
                    mItemHolder.itemView.setSelected(true);
                } else {
                    mItemHolder.itemView.setSelected(false);
                }
                mItemHolder.itemView.setOnClickListener(itemViewOnClickListener);

            }
        }
    }
    private View.OnClickListener itemViewOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            lastClickViewIndex = (int) (v.getTag());

            if (lastClickView != null && lastClickView != v) {
                lastClickView.setSelected(false);
            }
            v.setSelected(true);
            lastClickView = v;
        }
    };
    private View.OnClickListener ImgOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            MediaFile selectedMedia = (MediaFile) v.getTag();
            if (selectedMedia != null && mMediaManager != null) {
                addMediaTask(selectedMedia);
            }
        }
    };
    private void addMediaTask(final MediaFile mediaFile) {
        final FetchMediaTaskScheduler scheduler = mMediaManager.getScheduler();
        final FetchMediaTask task =
                new FetchMediaTask(mediaFile, FetchMediaTaskContent.PREVIEW, new FetchMediaTask.Callback() {
                    @Override
                    public void onUpdate(final MediaFile mediaFile, FetchMediaTaskContent fetchMediaTaskContent, DJIError error) {
                        if (null == error) {
                            if (mediaFile.getPreview() != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final Bitmap previewBitmap = mediaFile.getPreview();
                                        mDisplayImageView.setVisibility(View.VISIBLE);
                                        mDisplayImageView.setImageBitmap(previewBitmap);
                                    }
                                });
                            } else {
                                setResultToToast("null bitmap!");
                            }
                        } else {
                            setResultToToast("获取预览图像失败: " + error.getDescription());
                        }
                    }
                });

        scheduler.resume(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    scheduler.moveTaskToNext(task);
                } else {
                    setResultToToast("恢复调度程序失败: " + error.getDescription());
                }
            }
        });
    }
    private void updateStatusTextView(MediaManager.VideoPlaybackState videoPlaybackState) {
        final StringBuffer pushInfo = new StringBuffer();

        addLineToSB(pushInfo, "Video Playback State", null);
        if (videoPlaybackState != null) {
            if (videoPlaybackState.getPlayingMediaFile() != null) {
                addLineToSB(pushInfo, "media index", videoPlaybackState.getPlayingMediaFile().getIndex());
                addLineToSB(pushInfo, "media size", videoPlaybackState.getPlayingMediaFile().getFileSize());
                addLineToSB(pushInfo,
                        "media duration",
                        videoPlaybackState.getPlayingMediaFile().getDurationInSeconds());
                addLineToSB(pushInfo, "media created date", videoPlaybackState.getPlayingMediaFile().getDateCreated());
                addLineToSB(pushInfo,
                        "media orientation",
                        videoPlaybackState.getPlayingMediaFile().getVideoOrientation());
            } else {
                addLineToSB(pushInfo, "media index", "None");
            }
            addLineToSB(pushInfo, "media current position", videoPlaybackState.getPlayingPosition());
            addLineToSB(pushInfo, "media current status", videoPlaybackState.getPlaybackStatus());
            addLineToSB(pushInfo, "media cached percentage", videoPlaybackState.getCachedPercentage());
            addLineToSB(pushInfo, "media cached position", videoPlaybackState.getCachedPosition());
            pushInfo.append("\n");
            setResultToText(pushInfo.toString());
        }
    }

    private void addLineToSB(StringBuffer sb, String name, Object value) {
        if (sb == null) return;
        sb.
                append((name == null || "".equals(name)) ? "" : name + ": ").
                append(value == null ? "" : value + "").
                append("\n");
    }

    private void setResultToText(final String string) {
        if (mPushTv == null) {
            setResultToToast("推送信息流还没有初始化...");
        }
        YoloMediaActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPushTv.setText(string);
            }
        });
    }

    private void playVideo() {
        mDisplayImageView.setVisibility(View.INVISIBLE);
        MediaFile selectedMediaFile = mediaFileList.get(lastClickViewIndex);
        if ((selectedMediaFile.getMediaType() == MediaFile.MediaType.MOV) || (selectedMediaFile.getMediaType() == MediaFile.MediaType.MP4)) {
            mMediaManager.playVideoMediaFile(selectedMediaFile, new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (null != error) {
                        setResultToToast("Play Video Failed" + error.getDescription());
                    } else {
                        DJILog.e(TAG, "Play Video Success");
                    }
                }
            });
        }
    }

    private void moveToPosition(){

        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.prompt_input_position, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setView(promptsView);
        final EditText userInput = (EditText) promptsView.findViewById(R.id.editTextDialogUserInput);
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                String ms = userInput.getText().toString();
                mMediaManager.moveToPosition(Integer.parseInt(ms),
                        new CommonCallbacks.CompletionCallback() {
                            @Override
                            public void onResult(DJIError error) {
                                if (null != error) {
                                    setResultToToast("Move to video position failed" + error.getDescription());
                                } else {
                                    DJILog.e(TAG, "Move to video position successfully.");
                                }
                            }
                        });
            }
        })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();

    }
    //初始化云台
    private static class GimbalRotateTimerTask extends TimerTask {
        float pitchValue;

        GimbalRotateTimerTask(float pitchValue) {
            super();
            this.pitchValue = pitchValue;
        }

        @Override
        public void run() {
            if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
                FPVDemoApplication.getmProductInstance().getGimbal().
                        rotate(new Rotation.Builder().pitch(pitchValue)
                                .mode(RotationMode.SPEED)
                                .yaw(Rotation.NO_ROTATION)
                                .roll(Rotation.NO_ROTATION)
                                .time(0)
                                .build(), new CommonCallbacks.CompletionCallback() {

                            @Override
                            public void onResult(DJIError error) {
                                if(error == null){

                                }
                            }
                        });
            }
        }
    }

    //上升
    protected void handleUpBtnClick() {
        if (timer == null) {
            timer = new Timer();
            showToast("云台正在上升");
            gimbalRotationTimerTask = new GimbalRotateTimerTask(10);
            timer.schedule(gimbalRotationTimerTask, 0, 100);
        }
    }

    //停止
    protected void handleStopBtnClick() {
        if (timer != null) {
            if(gimbalRotationTimerTask != null) {
                gimbalRotationTimerTask.cancel();
            }
            timer.cancel();
            timer.purge();
            gimbalRotationTimerTask = null;
            timer = null;
        }

        if (ModuleVerificationUtil.isGimbalModuleAvailable()) {
            FPVDemoApplication.getmProductInstance().getGimbal().
                    rotate(null, new CommonCallbacks.CompletionCallback() {

                        @Override
                        public void onResult(DJIError error) {
                            if (error ==null){
                                showToast("已停止");
                            }

                        }
                    });
        }
    }
    //下降
    protected void handleDownBtnClick() {
        if (timer == null) {
            timer = new Timer();
            showToast("云台正在下降");
            gimbalRotationTimerTask = new GimbalRotateTimerTask(-10);
            timer.schedule(gimbalRotationTimerTask, 0, 100);
        }
    }
    private Gimbal getGimbalInstance() {
        if (gimbal == null) {
            initGimbal();
        }
        return gimbal;
    }

    private void initGimbal() {
        if (DJISDKManager.getInstance() != null) {
            BaseProduct product = DJISDKManager.getInstance().getProduct();
            if (product != null) {
                if (product instanceof Aircraft) {
                    gimbal = ((Aircraft) product).getGimbals().get(currentGimbalId);
                } else {
                    gimbal = product.getGimbal();
                }
            }
        }
    }
    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(YoloMediaActivity.this,msg,Toast.LENGTH_LONG).show();
            }
        });
    }
}
