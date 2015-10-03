package com.xd.multidownload;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    EditText getPath;
    TextView info;
    static int threadCount = 3;
    static int finishedThread = 0;
    int currentProgress;
    private ProgressBar mProgressBar;

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    info.setText((long) mProgressBar.getProgress() * 100 / mProgressBar.getMax() + "%");
                    break;
                case 1:
                    info.setText("100%");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getPath = (EditText) findViewById(R.id.et_get_path);
        mProgressBar = (ProgressBar) findViewById(R.id.progress);
        info = (TextView) findViewById(R.id.progress_info);

    }


    public void startDown(View v) {

        Thread thread = new Thread() {


            @Override
            public void run() {
                try {
                    String path = getPath.getText().toString();
                    URL url = new URL(path);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.connect();
                    if (connection.getResponseCode() == 200) {
                        //获取文件长度信息
                        long length = connection.getContentLength();
                        Log.d("D", "文件大小：" + length);
                        mProgressBar.setMax((int) length);
                        int size = (int) (length / threadCount);
                        Log.d("D", "分割后每段文件大小：" + size);
                        File file = new File(getFilesDir(), getNameFromString(path));
                        RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                        raf.setLength(length);
                        raf.close();
                        //                        Log.d("D", "随机文件大小:" + raf.length());
                        Log.d("D", "成功创建随机文件");
                        int startIndex;
                        int stopIndex;
                        for (int i = 0; i < threadCount; i++) {
                            startIndex = i * size;
                            stopIndex = (i + 1) * size - 1;
                            if (i == threadCount - 1) {
                                stopIndex = (int) (length - 1);
                            }
                            Log.d("D", "线程" + i + "的开始节点：" + startIndex);
                            Log.d("D", "线程" + i + "的结束节点：" + stopIndex);
                            new DownThread(startIndex, stopIndex, i).start();


                        }

                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();

    }

    public void pauseDown(View v) {
        Toast.makeText(this,"开发者。。。",Toast.LENGTH_SHORT);
    }

    public class DownThread extends Thread {
        int startIndex;
        int stopIndex;
        int threadId;

        public DownThread(int startIndex, int stopIndex, int threadId) {
            this.startIndex = startIndex;
            this.stopIndex = stopIndex;
            this.threadId = threadId;
        }

        @Override
        public void run() {

            try {
                String path = getPath.getText().toString();

                File file = new File(getFilesDir(), getNameFromString(path));
                File progessFile = new File(getFilesDir(), threadId + "ThreadProgress");
                if (progessFile.exists()) {
                    FileInputStream fis = new FileInputStream(progessFile);
                    BufferedReader bf = new BufferedReader(new InputStreamReader(fis));
                    int lastProgress = Integer.parseInt(bf.readLine());
                    startIndex += lastProgress;

                    currentProgress += lastProgress;
                    mProgressBar.setProgress(currentProgress);
                    mHandler.sendEmptyMessage(0);
                    fis.close();
                }

                URL url = new URL(path);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                Log.d("D", "创建连接");
                connection.setRequestProperty("Range", "bytes=" + startIndex + "-" + stopIndex);
                connection.connect();
                if (connection.getResponseCode() == 206) {
                    Log.d("D", "连接成功");

                    InputStream inputStream = connection.getInputStream();
                    RandomAccessFile raf = new RandomAccessFile(file, "rwd");
                    raf.seek(startIndex);
                    int len = 0;
                    byte[] b = new byte[1024];
                    int total = 0;
                    while ((len = inputStream.read(b)) != -1) {
                        raf.write(b, 0, len);
                        total += len;
                        //                        Log.d("D", "已下载：" + total);

                        currentProgress += len;
                        mProgressBar.setProgress(currentProgress);
                        mHandler.sendEmptyMessage(0);

                        RandomAccessFile randomAccessFile = new RandomAccessFile(progessFile, "rwd");
                        //                        Log.d("D", "创建临时文件成功：" + threadId);
                        randomAccessFile.write((total + "").getBytes());
                        randomAccessFile.close();
                    }
                    raf.close();
                    Log.d("D", "线程" + threadId + "已下载：---------------->" + total);
                    synchronized (path) {
                        finishedThread++;
                        if (finishedThread == threadCount) {
                            for (int i = 0; i < threadCount; i++) {
                                File f = new File(getFilesDir(), i + "ThreadProgress");
                                f.delete();
                                Log.d("D", "删除临时文件成功：" + i);
                                Log.d("D", "下载彻底结束");
mHandler.sendEmptyMessage(1);

                            }
                            finishedThread = 0;
                        }

                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getNameFromString(String s) {
        return s.substring(s.lastIndexOf("/") + 1);
    }
}
