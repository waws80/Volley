package thanatos.volley.controller;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import thanatos.volley.utils.CloseUtils;
import thanatos.volley.utils.Utils;

/**
 * Created on 2016/12/26.
 * 作者：by Administrator
 * 作用：用来连接网络
 */

public class HttpConn {

    private static final String TAG = "HttpConn";
    private static HttpConn mHttpConn;


    private HttpConn (){}


    public static HttpConn getInstance(){
        if (mHttpConn==null){
            synchronized (HttpConn.class){
                if (mHttpConn==null){
                    mHttpConn=new HttpConn();
                }
            }
        }
        return mHttpConn;
    }



    /**
     * 请求的方式
     */
    public enum Method{
        GET,
        POST,
        HEAD,
        OPTIONS,
        PUT,
        DELETE,
        TRACE;
    }

    /**
     * Set the method for the URL request, one of:
     * <UL>
     *  <LI>GET
     *  <LI>POST
     *  <LI>HEAD
     *  <LI>OPTIONS
     *  <LI>PUT
     *  <LI>DELETE
     *  <LI>TRACE
     * </UL> are legal, subject to protocol restrictions.  The default
     * method is GET.
     * @param url  request url eg: https://www.baidu.com
     */
    public   Object[]  getResult(Context context,Method method , String url, JSONObject jsonObject, Map<String,
            String> map,SSLSocketFactory sslSocketFactory){
            if (!Utils.isNetworkAvailable(context))
            return new Object[]{Integer.MAX_VALUE,null};
            if (url==null||url.isEmpty())throw new NullPointerException("url is null");
            HttpURLConnection conn=null;
            DataOutputStream outStream=null;
        try {
            if (url.startsWith("https")){

                if (null==sslSocketFactory){
                    conn=setHttp(url);
                }else {
                    conn = setHttps(url,sslSocketFactory);
                }
            }else {
                Log.w(TAG, "getResult: http" );
                conn = setHttp(url);
            }
            if (method==null){
                conn.setRequestMethod(Method.GET.name());
            }else {
                conn.setRequestMethod(method.name());
            }
            conn.setReadTimeout(8000);
            conn.setConnectTimeout(8000);
            //添加请求头
            addHeader(conn,map);

            conn.setDoInput(true);
            if (jsonObject==null||jsonObject.toString().isEmpty()){
                method=Method.GET;
            }else {
                conn.setDoOutput(true);
                outStream = new DataOutputStream(conn.getOutputStream());
                outStream.write(jsonObject.toString().getBytes());
                outStream.flush();
            }
            int responseCode = conn.getResponseCode();
            if (responseCode==200){
                InputStream text = conn.getInputStream();
                return new Object[]{responseCode,text};
            }else {
                if (!Utils.isNetworkAvailable(context)){
                    return new Object[]{Integer.MAX_VALUE,null};
                }else {
                    return new Object[]{responseCode,null};
                }

            }
        }catch (IOException e) {
            e.printStackTrace();
            return null;
        }finally {
            CloseUtils.close(outStream);
        }
    }

    /**
     * conn的https连接
     * 对https加证书的支持
     * @param url 请求url
     * @param sslSocketFactory  证书
     * @return conn
     */
    private HttpsURLConnection setHttps(String url, SSLSocketFactory sslSocketFactory){
        try {
            HttpsURLConnection conn = (HttpsURLConnection)new URL(url).openConnection();
            conn.setSSLSocketFactory(sslSocketFactory);
            return conn;
        }  catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * conn的http连接
     * @param url
     * @return
     */
    private HttpURLConnection setHttp(String url){
        try {
            return (HttpURLConnection)new URL(url).openConnection();
        }  catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 对请求头的数据进行处理
     * @param connection
     * @param map
     */
    private void addHeader(HttpURLConnection connection, Map<String,String> map){
        if (map==null||map.isEmpty())return;
        for (String key : map.keySet()) {
            System.out.println("key= "+ key + " and value= " + map.get(key));
            connection.setRequestProperty( key, map.get(key));
        }


    }




}
