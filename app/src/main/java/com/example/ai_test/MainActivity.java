package com.example.ai_test;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity {
    private Button choose_photo;
    private Button take_photo;
    private Button test;
    private ImageView imageview;
    private String filePath;


    private TextToSpeech textToSpeech = null; //TTS
    private String curLang;
    private String result;
    private TextView textView;
    private ListView listView;
    private ProgressBar progressBar;
    private ProgressDialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        choose_photo = (Button) findViewById(R.id.choose_photo);
        take_photo = (Button) findViewById(R.id.take_photo);
       // test = (Button) findViewById(R.id.test);
        imageview = (ImageView) findViewById(R.id.imageview);
       // textView = (TextView) findViewById(R.id.re);
        listView= (ListView) findViewById(R.id.listview);
        //progressBar = (ProgressBar)findViewById(R.id.progressBarNormal);
//        dialog = new ProgressDialog(MainActivity.this);
//        dialog.setTitle("加载");
//        dialog.setMessage("正在加载......");
//        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
//        dialog.setIndeterminate(false);
//        dialog.setCancelable(true);
        textToSpeech = new TextToSpeech(MainActivity.this, new TTSListener()); //给定一个TTS语音
        choose_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //从相册获取图片
                Intent intent = new Intent(Intent.ACTION_PICK, null);
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, 2);

            }
        });
        take_photo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                startActivityForResult(intent, 1);

            }
        });
    }
    public void testClick(View view) {

        Gson gson = new Gson();
        RecResult recresult = gson.fromJson(result,RecResult.class);
        Log.i("lzw","re_ok");
        List<RecResult.ResultBean> list = recresult.getResult();
        List<Map<String,String>> listmap = new ArrayList<Map<String,String>>();
        String str = "";
        for(int i = 0;i<list.size();i++) {
            double score = list.get(i).getScore();
            String name = list.get(i).getName();
            Map<String,String> map = new HashMap<String,String>();
            double s = score;
            String n = name;
            map.put("n","名称: "+n);
            map.put("s","相似度: "+s);
            if(i==0){
                str = "本次识别的结果是"+name+"相似率为"+s;
            }
            listmap.add(map);
            SimpleAdapter adapter = new SimpleAdapter(this,listmap,R.layout.item,new String[]{"n","s"},new int[]{R.id.name,R.id.score});
            listView.setAdapter(adapter);//绑定listView
            Log.i("lzw", score + "");
            Log.i("lzw", name);
        }
        textToSpeech.speak(str, TextToSpeech.QUEUE_FLUSH, null);
        //textView.setText(result);
       // textToSpeech.speak(result, TextToSpeech.QUEUE_FLUSH, null);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            Bitmap bmp = saveCameraImage(data);
            Log.i("lzw", filePath);
            new Thread(new Runnable(){
                @Override
                public void run() {
                    result = plant(filePath);
                }
            }).start();
            try {
                Thread.sleep(1300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            imageview.setImageBitmap(bmp);
        }
        if (requestCode == 2) {
            if (data != null) {
                // 得到图片的全路径
                Uri uri = data.getData();
                filePath = RealPathFromUriUtils.getRealPathFromUri(this, data.getData());
                Log.i("lzw", filePath);
                new Thread(new Runnable(){
                    @Override
                    public void run() {

                        result = plant(filePath);
                    }
                }).start();
                try {
                    Thread.sleep(1300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                imageview.setImageURI(uri);
//                progressBar.incrementProgressBy(10);
//                progressBar.incrementSecondaryProgressBy(10);
            }
        }
    }

    private String plant(String filePath) {
        // 请求url
        String url = "https://aip.baidubce.com/rest/2.0/image-classify/v1/animal";
        try {
            // 本地文件路径
            byte[] imgData = FileUtil.readFileByBytes(filePath);
            String imgStr = Base64Util.encode(imgData);
            String imgParam = URLEncoder.encode(imgStr, "UTF-8");
            String param = "image=" + imgParam;
            // 注意这里仅为了简化编码每一次请求都去获取access_token，线上环境access_token有过期时间， 客户端可自行缓存，过期后重新获取。
            String accessToken = Token.getAuth();

            String result = HttpUtil.post(url, accessToken, param);
            Log.i("lzw", result);
            System.out.println(result);

            return result;
        } catch (Exception e) {
            e.printStackTrace();
            Log.i("lzw", "获取不到");
            return "图片规格不符合";
        }
        //return null;
    }

    private int SetLanguage(String lang) {
        int results = textToSpeech.setLanguage(Locale.CHINESE);
        return results;
    }


    private class TTSListener implements TextToSpeech.OnInitListener {
        @Override
        public void onInit(int i) {
            if (i == TextToSpeech.SUCCESS) {
                int result = SetLanguage(curLang);
            }
        }
    }

    private Bitmap saveCameraImage(Intent data) {
        // 检查sd card是否存在

        //
        Bitmap bmp = (Bitmap) data.getExtras().get("data");// 解析返回的图片成bitmap
        // 保存文件为图片命名啊
        FileOutputStream fos = null;
        File file = new File("/storage/emulated/0/FishImage/");
        file.mkdirs();// 创建文件夹
        filePath = "/storage/emulated/0/FishImage/" + "fish.jpg";// 保存路径

        try {// 写入SD card
            fos = new FileOutputStream(filePath);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                fos.flush();
                fos.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }// 显示图片
        return bmp;
    }

    private void parseJSONWithJSONObject(String jsonData) {
        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            Log.i("lzw", "jsss");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String score = jsonObject.getString("score");
                String name = jsonObject.getString("name");
                Log.i("lzw", "score : " + score);
                Log.i("lzw", "name : " + name);
            }
        } catch (Exception e) {
            Log.i("lzw", "json_err");
            e.printStackTrace();
        }
    }
}
