package com.ds.browser.task;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ds.browser.activity.DownloadActivity;
import com.ds.browser.util.BrowserDBHelper;
import com.ds.browser.util.DownloadHelper;
import com.ds.browser.widget.ClickableToast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;



@SuppressLint("StaticFieldLeak")
public class DownloaderTask extends AsyncTask<String, Integer, String> {
    private Context context;
    private WeakReference<Context> contextReference;
    private String downloadUrl;
    private boolean isPause;   //暂停下载
    private String fileName;
    private File filePath;
    private int fileSize;   //文件总大小
    private long time = System.currentTimeMillis();   //下载任务创建时间
    private int progress;    //当前下载的进度
    private int downloadLength;      //已下载的长度
    private String cookie;

    public DownloaderTask(Context context, String fileName, File filePath, int fileSize, int downloadLength) {
        contextReference=new WeakReference<>(context);
        this.context=context.getApplicationContext();
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.downloadLength = downloadLength;
    }
    public DownloaderTask(Context context, String fileName, File filePath, int fileSize, int downloadLength,String cookie) {
        contextReference=new WeakReference<>(context);
        this.context=context.getApplicationContext();
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.downloadLength = downloadLength;
        this.cookie = cookie;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setPause(boolean pause) {
        isPause = pause;
    }

    public int getFileSize() {
        return fileSize;
    }

    public int getProgress() {
        return progress;
    }

    public String getFilePath() {
        return filePath.getAbsolutePath();
    }

    public long getTime() {
        return time;
    }

    @Override
    protected String doInBackground(String... params) {
        downloadUrl = params[0];
        int current_write = downloadLength;
        HttpURLConnection connection = null;
        RandomAccessFile raf = null;
        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(8000);

            if (!TextUtils.isEmpty(this.cookie)){
                connection.setRequestProperty("Cookie", this.cookie);
            }

            connection.setRequestProperty("Connection", "Keep-Alive");// 维持长连接
            connection.setRequestProperty("Charset", "UTF-8");// 设置请求头
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setDoOutput(true);// 使用 URL 连接进行输出
            connection.setDoInput(true);// 使用 URL 连接进行输入
            connection.setUseCaches(false);// 忽略缓存
            connection.setConnectTimeout(8 * 1000);
            //connection.setRequestMethod("GET");
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Accept-Encoding", "identity");
            /*
            String path = filePath.getAbsolutePath();

            if (connection.getResponseCode() == 200) {
                InputStream inputStream = connection.getInputStream();
                OutputStream os = new FileOutputStream(path);
                byte[] buffer = new byte[1024 * 4];
                int length;
                while ((length = inputStream.read(buffer)) != -1) {
                    if (!filePath.exists()) return "file_error";
                    if (isCancelled()) break;

                    os.write(buffer, 0, length);

                    current_write += length;
                    publishProgress(current_write);
                }
            }
             */
            //fileSize = connection.getContentLength();
            connection.setRequestProperty("Range", "bytes=" + downloadLength + "-" + fileSize);
            raf = new RandomAccessFile(filePath, "rwd");
            raf.seek(downloadLength);
            if (connection.getResponseCode() == 200) {
                InputStream input = connection.getInputStream();
                byte[] buffer = new byte[1024 * 4];
                int len;
                while ((len = input.read(buffer)) != -1) {
                    if (!filePath.exists()) return "file_error";
                    if (isCancelled()) break;
                    raf.write(buffer, 0, len);
                    current_write += len;
                    publishProgress(current_write);
                }
            }


            return "success";
        } catch (IOException e) {
            e.printStackTrace();
            return "fail";
        } finally {
            if (connection != null)
                connection.disconnect();
            try {
                if (raf != null)
                    raf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (isPause) {
            downloadLength = progress;
            Log.d("download", "onCancelled更新进度:  " + progress);
            BrowserDBHelper.getBrowserDBHelper(context).updateDownloadTable(downloadUrl, filePath.getAbsolutePath(), fileName, fileSize, progress, time);
        } else {
            Log.d("download", "取消下载:  " + fileName);
            Toast.makeText(context, "下载取消", Toast.LENGTH_SHORT).show();
        }
        DownloadHelper.downloadList.remove(this);
    }

    @Override
    protected void onPreExecute() {
        // TODO Auto-generated method stub
        super.onPreExecute();
        BrowserDBHelper.getBrowserDBHelper(context).deleteTableItem(BrowserDBHelper.DTB_NAME, "where downloadUrl='" + downloadUrl + "'");
        ClickableToast.makeClickText(context, "开始下载", "查看", Toast.LENGTH_SHORT, new ClickableToast.OnToastClickListener() {
            @Override
            public void onToastClick() {
                if (contextReference.get()!=null) {
                    Context c = contextReference.get();
                    c.startActivity(new Intent(c, DownloadActivity.class));
                }
            }
        }).show();
    }

    @Override
    protected void onCancelled(String s) {
        super.onCancelled(s);
    }

    /*
           每隔一秒刷新一次
         */
    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        progress = values[0];
    }

    @Override
    protected void onPostExecute(final String result) {
        // TODO Auto-generated method stub
        super.onPostExecute(result);
        switch (result) {
            case "success":
                BrowserDBHelper.getBrowserDBHelper(context).deleteTableItem(BrowserDBHelper.DTB_NAME, "where downloadUrl='" + downloadUrl + "'");
                // 添加到媒体库
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath.getAbsolutePath());
                values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                break;
            case "file_error":
                BrowserDBHelper.getBrowserDBHelper(context).updateDownloadTable(downloadUrl, filePath.getAbsolutePath(), fileName, fileSize, progress, time);
                Toast.makeText(context, "文件被篡改,请重新下载！", Toast.LENGTH_SHORT).show();
                break;
            case "fail":
                if (progress==0)progress=downloadLength;
                Log.d("download","下载失败更新进度："+progress+"  downloadLength:"+downloadLength);
                BrowserDBHelper.getBrowserDBHelper(context).updateDownloadTable(downloadUrl, filePath.getAbsolutePath(), fileName, fileSize, progress, time);
                Toast.makeText(context, "下载错误,请重试！", Toast.LENGTH_SHORT).show();
                break;

        }
        Intent intent = new Intent();
        intent.setAction("download_progress_refresh");
        intent.putExtra("finish_download", true);
        context.sendBroadcast(intent);
        DownloadHelper.downloadList.remove(this);

    }


}
