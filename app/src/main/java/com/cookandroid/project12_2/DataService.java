package com.cookandroid.project12_2;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.IBinder;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;

public class DataService extends Service {
    private myDBHelper dbHelper;
    private SQLiteDatabase database;
    private Socket socket;
    private BufferedReader inpstream;
    private String response = "NotYet";
    private final String ip = "192.168.100.22";
    private final int port = 8080;


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

//    서비스 생성
    @Override
    public void onCreate() {
        android.util.Log.i("데이터 처리 서비스 생성", "onCreate()");
        super.onCreate();
//        가능한 DB 획득, DB 헬퍼 생성
        dbHelper = new myDBHelper(this);
        database = dbHelper.getWritableDatabase();
    }

//    서비스 작동 시작
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        android.util.Log.i("데이터 처리 서비스 시작", "onStartCommand()");
//        소켓통신을 위한 스레드 생성 및 동작
        ConnectThread thread = new ConnectThread();
        thread.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        android.util.Log.i("데이터 처리 서비스 종료", "onDestory()");
//        소켓, 스트림, DB 일괄 종료, 서비스 종료
        try {
            if (socket != null) {
                socket.close();
            }
            if (inpstream != null) {
                inpstream.close();
            }
            if (database != null) {
                database.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        super.onDestroy();
    }

//    소켓 통신용 스레드
    class ConnectThread extends Thread {
        public void run() {
            super.run();
            try {
//                DB에서 사용할 속성 선언
                String exer_datetime, exer_name;
                int exer_duration, num_sets, num_reps;

//                소켓 생성
                InetAddress serverAddress = InetAddress.getByName(ip);
//                연결 시작
                socket = new Socket(serverAddress, port);
                while (!response.equals("quit")) {
//                    응답 스트림 생성
                    inpstream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    response = (String) inpstream.readLine();
//                    응답 검사
                    if (response.isEmpty())
                        continue;

                    if (response.equals("quit"))
                        onDestroy();
//                    정상 메시지이면 전달 받은 메시지를 토크나이저로 분할하여 저장하고
                    if (!response.equals("NotYet")) {
                        StringTokenizer tokenizer = new StringTokenizer(response);
                        exer_datetime = (String) tokenizer.nextToken();
                        exer_duration = (int) Integer.parseInt(tokenizer.nextToken());
                        exer_name = (String) tokenizer.nextToken();
                        num_sets = (int) Integer.parseInt(tokenizer.nextToken());
                        num_reps = (int) Integer.parseInt(tokenizer.nextToken());
//                        ContentValues 객체에 넣어 DB에 insert함
                        ContentValues values = new ContentValues();
                        values.put("exer_date", exer_datetime);
                        values.put("exer_duration", exer_duration);
                        values.put("exer_name", exer_name);
                        values.put("num_sets", num_sets);
                        values.put("num_reps", num_reps);
                        database.insert("exerciseTBL", null, values);
//                        애플리케이션이 종료될 때까지 무한히 반복 (상시로 통신을 유지하기 위함)
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class myDBHelper extends SQLiteOpenHelper{
        public myDBHelper(Context context) {
            super(context, "exerciseDB", null, 1);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
//            DB 생성. 운동시간(텍스트), 운동지속시간(정수), 운동이름(텍스트), 세트수(정수), 세트당렙수(정수)
            db.execSQL("CREATE TABLE exerciseTBL (" +
                    "exer_datetime TEXT PRIMARY KEY," +
                    "exer_duration INTEGER," +
                    "exer_name TEXT," +
                    "num_sets INTEGER," +
                    "num_reps INTEGER);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS exerciseTBL");
            onCreate(db);
        }
    }
}