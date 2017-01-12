package thanatos.volley;

import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import thanatos.volley.controller.DataCallBack;
import thanatos.volley.controller.DateMemeryCache;
import thanatos.volley.controller.HttpConn;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private Volley volley;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        volley=Volley.getInstance(this,new DateMemeryCache());
        Map<String,String> header=new HashMap<>();
        header.put("token","");
        for (int i = 0; i < 100; i++) {
                volley.setDataCache(true).getResult("https://www.baidu.com", HttpConn.Method.GET,
                        null,null ).dataCallBack
                        (new DataCallBack() {
                            @Override
                            public void success(String text) {
                                Log.w(TAG, "success: "+text );
                            }
                            @Override
                            public void error(String code) {
                                Log.w(TAG, "error: "+code );
                            }
                });
            }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        volley.onDestory();

    }
}
