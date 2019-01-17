package com.ds.browser.activity;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ds.browser.R;
import com.ds.browser.adapter.DownloadAdapter;
import com.ds.browser.bean.FileDownloadBean;
import com.ds.browser.task.DownloaderTask;
import com.ds.browser.util.AppUtil;
import com.ds.browser.util.BrowserDBHelper;
import com.ds.browser.util.DownloadHelper;
import com.ds.browser.util.FileUtil;
import com.ds.browser.widget.SwipeBackActivity;
import com.ds.browser.widget.TextProgressBar;
import com.tencent.smtt.sdk.MimeTypeMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import butterknife.BindView;
import butterknife.ButterKnife;



public class DownloadActivity extends SwipeBackActivity {
    @BindView(R.id.download_record_bar_theme)
    View downloadRecordBarTheme;
    @BindView(R.id.download_record_back)
    Button downloadRecordBack;
    @BindView(R.id.download_record_list)
    ListView downloadRecordList;
    @BindView(R.id.download_record_storage_size_bar)
    View storageSizeBar;
    @BindView(R.id.storage_size_progress)
    TextProgressBar textProgressBar;
    @BindView(R.id.empty_download_record)
    TextView emptyRecord;
    @BindView(R.id.download_record_select_more_bar)
    View selectMoreBar;
    @BindView(R.id.download_record_confirm_delete)
    Button confirmDelete;
    @BindView(R.id.download_record_cancel_delete)
    Button cancelDelete;

    private DownloadAdapter adapter;
    private List<FileDownloadBean> data = new ArrayList<>();
    private Set<Integer> selectedItemList = new TreeSet<>();
    private int selectedPosition;
    private PopupWindow deleteWindow;
    private Timer timer;
    private File[] files;    //下载目录内的文件
    private int downloadingCount = 0;  //正在下载的文件数
    private Map<String, FileDownloadBean> pauseList = new LinkedHashMap<>();   //暂停任务列表
    private List<String> pauseListRemoveLog = new ArrayList();   //记录从pauseList移除的文件下载地址
    //private boolean flag=false;   //删除标志
    //String cookie;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //startService(new Intent(this, FileService.class));
        setContentView(R.layout.activity_download_record);
        IntentFilter mFilter = new IntentFilter();
        mFilter.addAction("download_progress_refresh");
        registerReceiver(downloadStatus, mFilter);

        ButterKnife.bind(this);
        initData();
        initView();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                showNetSpeed();
            }
        }, 0, 1000);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        //stopService(new Intent(this,FileService.class));
        unregisterReceiver(downloadStatus);
        if (timer != null) {
            timer.cancel();
        }
        for (FileDownloadBean bean : pauseList.values()) {
            BrowserDBHelper.getBrowserDBHelper(this).updateDownloadTable(
                    bean.getDownloadUrl(),
                    bean.getFilePath(),
                    bean.getFileName(),
                    bean.getFileSize(),
                    bean.getDownloadProgress(),
                    bean.getLastModified());
        }
        pauseList.clear();
        for (String url : pauseListRemoveLog) {
            BrowserDBHelper.getBrowserDBHelper(this).deleteTableItem(BrowserDBHelper.DTB_NAME, "where downloadUrl='" + url + "'");
        }
        BrowserDBHelper.getBrowserDBHelper(this).removeMessage();
    }

    @SuppressWarnings("ConstantConditions")
    private void initView() {
        //final String cookie = this.cookie;
        adapter = new DownloadAdapter(this, data);
        downloadRecordList.setAdapter(adapter);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        downloadRecordBarTheme.setBackgroundColor(Color.parseColor(preferences.getString("theme_color", "#474747")));

        downloadRecordBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });


        downloadRecordList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                if (adapter.isCanSelectMore()) {
                    CheckBox itemCheckBox = view.findViewById(R.id.download_record_delete_checkbox);
                    if (itemCheckBox.isChecked()) {
                        itemCheckBox.setChecked(false);

                    } else {
                        itemCheckBox.setChecked(true);
                    }
                } else {
                    final FileDownloadBean fileDownloadBean = data.get(position);
                    if (fileDownloadBean.isFinished()) {
                        String path = data.get(position).getFilePath();
                        File file = new File(path);

                        if (!file.exists()){
                            Toast.makeText(DownloadActivity.this,"文件不存在",Toast.LENGTH_SHORT).show();
                        }else {

                            String authority = "com.ds.matisse.file.provider";
                            // 版本大于7.0获取文件路径uri
                            //Uri uri = Uri.fromFile(file);
                            //获取文件file的MIME类型
                            String type = FileUtil.getMIMEType(file);

                            Intent intent = new Intent();

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                Uri uri = FileProvider.getUriForFile(DownloadActivity.this, authority, file);
                                intent.setDataAndType(uri, type);
                            } else {
                                intent.setDataAndType(Uri.fromFile(file), type);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            }
                            intent.putExtra("file_path", path);
                            //设置intent的Action属性
                            intent.setAction(Intent.ACTION_VIEW);
                            startActivity(intent);

                        }
                    } else {
                        final ImageView downloadStatus = view.findViewById(R.id.download_status);
                        final TextView downloadSpeed = view.findViewById(R.id.download_speed);

                        if (!fileDownloadBean.isDownloading()) {

                            if (DownloadHelper.downloadList.size() == DownloadHelper.downloadLimitCount) {
                                Toast.makeText(DownloadActivity.this, "下载数达最大限制",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            downloadStatus.setImageDrawable(getResources().getDrawable(R.drawable.stop_download));
                            downloadSpeed.setText(fileDownloadBean.getSpeed());
                            fileDownloadBean.setDownloading(true);
                            if (new File(fileDownloadBean.getFilePath()).exists())
                                fileDownloadBean.setDownloadProgress(pauseList.get(fileDownloadBean.getDownloadUrl()).getDownloadProgress());
                            else
                                fileDownloadBean.setDownloadProgress(0);

                            DownloaderTask downloaderTask = new DownloaderTask(
                                    DownloadActivity.this,
                                    fileDownloadBean.getFileName(),
                                    new File(fileDownloadBean.getFilePath()),
                                    fileDownloadBean.getFileSize(),
                                    fileDownloadBean.getDownloadProgress());

                            downloaderTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, fileDownloadBean.getDownloadUrl());
                            DownloadHelper.downloadList.add(downloaderTask);
                            pauseListRemoveLog.add(fileDownloadBean.getDownloadUrl());
                            pauseList.remove(fileDownloadBean.getDownloadUrl());

                        } else {

                            fileDownloadBean.setDownloading(false);
                            downloadStatus.setImageDrawable(getResources().getDrawable(R.drawable.start_download));
                            downloadSpeed.setText("暂停");
                            DownloaderTask downloaderTask = DownloadHelper.getDownloadFile(fileDownloadBean.getFilePath());
                            if (downloaderTask == null) return;
                            Log.d("ewe", "task进度:" + downloaderTask.getProgress() + "bean进度：" + fileDownloadBean.getDownloadProgress());
                            fileDownloadBean.setDownloadUrl(downloaderTask.getDownloadUrl());
                            downloaderTask.setPause(true);
                            downloaderTask.cancel(true);
                            DownloadHelper.downloadList.remove(downloaderTask);
                            pauseList.put(fileDownloadBean.getDownloadUrl(), fileDownloadBean);
                            pauseListRemoveLog.remove(fileDownloadBean.getDownloadUrl());
                        }
                    }
                }

            }
        });
        downloadRecordList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                selectedPosition = position;
                if (!selectMoreBar.isShown()) {
                    int[] positions = new int[2];
                    view.getLocationOnScreen(positions);
                    deleteWindow.showAtLocation(view, Gravity.TOP | Gravity.END,
                            AppUtil.dip2px(DownloadActivity.this, 20),
                            positions[1] + AppUtil.dip2px(DownloadActivity.this, 60));
                }
                return true;
            }
        });
        adapter.setOnCheckChangedListener(new DownloadAdapter.OnCheckChangedListener() {
            @Override
            public void onCheckChanged(int position, boolean checked) {
                if (checked) {
                    selectedItemList.add(position);
                } else {
                    selectedItemList.remove(position);
                }
            }
        });
        emptyRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AppUtil.createDialog(DownloadActivity.this, "删除提示",
                        "清空后需要重新下载文件!", "确定"
                        , new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                BrowserDBHelper.getBrowserDBHelper(DownloadActivity.this).deleteTableItem(BrowserDBHelper.DTB_NAME, null);
                                DownloadHelper.stopAllDownloads();
                                File savePath = new File("/storage/emulated/0/BrowserDownloads");
                                if (savePath.exists()) {
                                    for (File file : savePath.listFiles()) {
                                        file.delete();
                                    }
                                    initData();
                                }
                            }
                        }, null);
            }
        });
        @SuppressLint("InflateParams")
        View contentView = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(R.layout.history_item_delete_window, null);
        Button editButton = contentView.findViewById(R.id.editButton);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.setRestoreCheckBox(false);
                adapter.setCanSelectMore(true);
                adapter.notifyDataSetInvalidated();
                selectMoreBar.setVisibility(View.VISIBLE);
                storageSizeBar.setVisibility(View.INVISIBLE);
                deleteWindow.dismiss();
            }
        });

        Button deleteButton = contentView.findViewById(R.id.deleteButton1);
        deleteButton.setText("删除该文件");
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteWindow.dismiss();
                //flag=true;
                final FileDownloadBean selectedBean = data.get(selectedPosition);
                //检查要删除的文件是否是暂停下载的
                if (pauseList.containsKey(selectedBean.getDownloadUrl())) {
                    BrowserDBHelper.getBrowserDBHelper(DownloadActivity.this).deleteTableItem(BrowserDBHelper.DTB_NAME, "where downloadUrl='" + selectedBean.getDownloadUrl() + "'");
                    pauseList.remove(selectedBean.getDownloadUrl());
                    pauseListRemoveLog.add(selectedBean.getDownloadUrl());
                    new File(selectedBean.getFilePath()).delete();
                    data.remove(selectedPosition);
                    adapter.notifyDataSetChanged();
                    refreshStorageStatus();
                } else {
                    final DownloaderTask task = DownloadHelper.getDownloadFile(selectedBean.getFilePath());
                    Log.d("Record", "任务：" + task);
                    if (task != null) {
                        AppUtil.createDialog(DownloadActivity.this,"删除提示","删除下载中的文件需要重新下载!","仍要删除",new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                task.cancel(true);
                                new File(selectedBean.getFilePath()).delete();
                                data.remove(selectedPosition);
                                adapter.notifyDataSetChanged();
                                refreshStorageStatus();
                                //flag=false;
                            }
                        },null);
                    } else {
                        new File(selectedBean.getFilePath()).delete();
                        data.remove(selectedPosition);
                        adapter.notifyDataSetChanged();
                        refreshStorageStatus();
                    }
                }
//                if (flag) {
//                    new File(selectedBean.getFilePath()).delete();
//                    data.remove(selectedPosition);
//                    adapter.notifyDataSetChanged();
//                    refreshStorageStatus();
//                }
            }
        });
        deleteWindow = new PopupWindow(contentView, AppUtil.dip2px(this, 120), ViewGroup.LayoutParams.WRAP_CONTENT);
        deleteWindow.setFocusable(true);
        deleteWindow.setOutsideTouchable(true);
        deleteWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        confirmDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("rer", "选中：" + selectedItemList + "data size：" + data.size());
                for (int i : selectedItemList) {
                    FileDownloadBean selectedBean = data.get(i);
                    if (pauseList.containsKey(selectedBean.getDownloadUrl())) {
                        BrowserDBHelper.getBrowserDBHelper(DownloadActivity.this).deleteTableItem(BrowserDBHelper.DTB_NAME, "where downloadUrl='" + selectedBean.getDownloadUrl() + "'");
                        pauseList.remove(selectedBean.getDownloadUrl());
                        pauseListRemoveLog.add(selectedBean.getDownloadUrl());

                    }
                    new File(selectedBean.getFilePath()).delete();
                    DownloaderTask task = DownloadHelper.getDownloadFile(selectedBean.getFilePath());
                    if (task != null) {
                        task.cancel(true);
                    }
                    pauseListRemoveLog.add(selectedBean.getDownloadUrl());
                    pauseList.remove(selectedBean.getDownloadUrl());
                }
                int removeNum = -1;
                for (int i : selectedItemList) {
                    data.remove((i - (removeNum + 1)));
                    removeNum++;
                }
                adapter.setRestoreCheckBox(true);
                adapter.setCanSelectMore(false);
                adapter.notifyDataSetChanged();
                selectMoreBar.setVisibility(View.INVISIBLE);
                storageSizeBar.setVisibility(View.VISIBLE);
                selectedItemList.clear();
                refreshStorageStatus();
            }
        });
        cancelDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapter.setCanSelectMore(false);
                adapter.setRestoreCheckBox(true);
                adapter.notifyDataSetInvalidated();
                selectMoreBar.setVisibility(View.INVISIBLE);
                storageSizeBar.setVisibility(View.VISIBLE);
            }
        });
    }

    private void initData() {
        downloadingCount = DownloadHelper.downloadList.size();
        data.clear();
        pauseList.clear();
        pauseListRemoveLog.clear();

        BrowserDBHelper.getBrowserDBHelper(this).searchDownloadTable("select * from " + BrowserDBHelper.DTB_NAME + " order by downloadTIME desc",
                new BrowserDBHelper.OnSearchDownloadTableListener() {
                    @Override
                    public void onResult(ArrayList<FileDownloadBean> mDownloadData) {
                        Log.d("download", "数据库：" + mDownloadData.size());
                        for (FileDownloadBean bean : mDownloadData)
                            pauseList.put(bean.getDownloadUrl(), bean);
                        data.addAll(mDownloadData);
                        File savePath = new File("/storage/emulated/0/BrowserDownloads");
                        if (savePath.exists()) {
                            files = savePath.listFiles();
                            if (files != null) {
                                for (File file : files) {
                                    if (file.isDirectory()) continue;
                                    if (data.contains(new FileDownloadBean(file.getName())))
                                        continue;
                                    FileDownloadBean fileInfo = new FileDownloadBean(file.getName());
                                    DownloaderTask task = DownloadHelper.getDownloadFile(file.getAbsolutePath());
                                    if (task != null) {
                                        fileInfo.setDownloading(true);
                                        fileInfo.setFinished(false);
                                        fileInfo.setLastModified(task.getTime());
                                        fileInfo.setDownloadProgress(task.getProgress());
                                        fileInfo.setFileSize(task.getFileSize());
                                        fileInfo.setDownloadUrl(task.getDownloadUrl());
                                    } else {
                                        fileInfo.setDownloading(false);
                                        fileInfo.setFinished(true);
                                        fileInfo.setLastModified(file.lastModified());
                                        fileInfo.setFileSize((int) file.length());
                                    }
                                    fileInfo.setFilePath(file.getAbsolutePath());
                                    data.add(fileInfo);
                                }

                            }
                            Collections.sort(data);
                            if (adapter != null)
                                adapter.notifyDataSetChanged();
                        }
                    }
                });

        refreshStorageStatus();
    }

    @SuppressLint("SetTextI18n")
    private void showNetSpeed() {
        if (downloadRecordList.getChildCount() != 0 && data.size() != 0) {
            downloadingCount = DownloadHelper.downloadList.size();
            int downloadItemInList = 0;
            final int childIndex = downloadRecordList.getFirstVisiblePosition();
            for (int i = downloadRecordList.getFirstVisiblePosition(); i <= downloadRecordList.getLastVisiblePosition(); i++) {
                if (i >= data.size()) return;
                if (data.get(i).isDownloading()) {
                    downloadItemInList++;
                    if (downloadItemInList > downloadingCount) break;
                    final int j = i;

                    DownloaderTask task = DownloadHelper.getDownloadFile(data.get(i).getFilePath());

                    if (task == null) return;
                    final int progress = task.getProgress();
                    if (progress != 0 && progress != data.get(j).getDownloadProgress()) {
                        View view = downloadRecordList.getChildAt(i - childIndex);
                        final ProgressBar progressBar = view.findViewById(R.id.download_progress);
                        final TextView speed = view.findViewById(R.id.download_speed);
                        Log.d("download", "task进度：" + progress + "bean进度：" + data.get(j).getDownloadProgress());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setProgress(progress);
                                String downloadSpeed = Formatter.formatFileSize(DownloadActivity.this, progress - data.get(j).getDownloadProgress()) + "/s";
                                speed.setText(downloadSpeed);
                                data.get(j).setDownloadProgress(progress);
                                data.get(j).setSpeed(downloadSpeed);
                            }
                        });
                    }
                }
            }
        }
    }

    private void refreshStorageStatus() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long blockSize = statFs.getBlockSizeLong();
            long totalBlocks = statFs.getBlockCountLong();
            long availableBlocks = statFs.getAvailableBlocksLong();
            String totalSize = Formatter.formatFileSize(this, blockSize * totalBlocks);
            String availableSize = Formatter.formatFileSize(this, blockSize * availableBlocks);
            textProgressBar.setTextAndProgress("内置存储可用：" + availableSize + "/共：" + totalSize, (int) ((float) availableBlocks / totalBlocks * 100));
        } else {
            textProgressBar.setTextAndProgress("内置存储不可用", 0);
        }
    }

    /*
    每隔一秒发送一次广播
     */
    private BroadcastReceiver downloadStatus = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getBooleanExtra("finish_download", false)) {
                initData();
            }
        }
    };
    private String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null)
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return type;
    }
}
