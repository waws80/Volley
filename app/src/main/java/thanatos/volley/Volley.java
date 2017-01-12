package thanatos.volley;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.json.JSONObject;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import javax.net.ssl.SSLSocketFactory;

import thanatos.volley.controller.Cache;
import thanatos.volley.controller.DataCallBack;
import thanatos.volley.controller.HttpConn;
import thanatos.volley.controller.SSLBuilder;
import thanatos.volley.controller.ThreadPool;
import thanatos.volley.utils.Utils;

/**
 * Created on 2017/1/12.
 * 作者：by thanatos
 * 作用：获取网络数据
 */

public class Volley {

    private static Volley mVolley;

    private ExecutorService mThreadPool;
    private SSLBuilder mSSLBuilder;

    private Cache<String> mCache;
    private WeakReference<Context> contextWeakReference;
    private DataCallBack mDataCallBack;

    private UIHandler mUIHandler;
    private QueueHandler mQueueHandler;

    private Handler dataCallBackHandler;

    private LinkedList<Runnable> mTaskQueue;

    private HandlerThread mQueueHandlerLooper;

    private static final Semaphore mQueueHandlerSemaphore=new Semaphore(0);

    private Semaphore mTaskQueueSizeSemaphore;

    private boolean isDataCache=false;

    private int cerId=Integer.MAX_VALUE;




    private Volley(Context context, int cerId, Cache<String> cache){
        contextWeakReference=new WeakReference<>(context);
        mThreadPool=ThreadPool.getInstance();
        mTaskQueueSizeSemaphore=new Semaphore(ThreadPool.getThreadPoolSize());
        mSSLBuilder=SSLBuilder.getInstance(contextWeakReference.get());
        mQueueHandlerLooper=new HandlerThread("thread-looper");
        mTaskQueue=new LinkedList<>();
        mCache=cache;
        this.cerId=cerId;
        intitCallBackHandler();
        doInBackground();
    }

    private Volley(Context context, int cerId, Cache<String> cache,int threadSize){
        contextWeakReference=new WeakReference<>(context);
        mThreadPool=ThreadPool.getInstance(threadSize);
        mTaskQueueSizeSemaphore=new Semaphore(threadSize);
        mSSLBuilder=SSLBuilder.getInstance(contextWeakReference.get());
        mQueueHandlerLooper=new HandlerThread("thread-looper");
        mTaskQueue=new LinkedList<>();
        mCache=cache;
        this.cerId=cerId;
        intitCallBackHandler();
        doInBackground();
    }

    private Volley(Context context, Cache<String> cache){
        contextWeakReference=new WeakReference<>(context);
        mThreadPool=ThreadPool.getInstance();
        mTaskQueueSizeSemaphore=new Semaphore(ThreadPool.getThreadPoolSize());
        mQueueHandlerLooper=new HandlerThread("thread-looper");
        mTaskQueue=new LinkedList<>();
        this.mCache=cache;
        intitCallBackHandler();
        doInBackground();
    }

    /**
     * 初始化接收错误消息器
     */
    private void intitCallBackHandler() {
        dataCallBackHandler=new DataCallBackHandler();
    }

    /**
     * 后台轮询
     */
    private void doInBackground() {
        mQueueHandlerLooper.start();
        Thread queueThread = new Thread() {
            @Override
            public void run() {
                if (mQueueHandler == null) {
                    mQueueHandler = new QueueHandler(mQueueHandlerLooper.getLooper());
                    mQueueHandlerSemaphore.release();
                }
            }
        };
        queueThread.start();

    }


    public static  Volley getInstance(Context context,int cerId,Cache<String> cache){
        if (mVolley==null){
            synchronized (Volley.class){
                if (mVolley==null){
                    mVolley=new Volley(context,cerId,cache);
                }
            }
        }
        return mVolley;
    }

    public static Volley getInstance(Context context,Cache<String> cache){
        if (mVolley==null){
            synchronized (Volley.class){
                if (mVolley==null){
                    mVolley=new Volley(context,cache);
                }
            }
        }
        return mVolley;
    }

    public static Volley getInstance(Context context,int cerId,Cache<String> cache,int threadSize){
        if (mVolley==null){
            synchronized (Volley.class){
                if (mVolley==null){
                    mVolley=new Volley(context,cerId,cache,threadSize);
                }
            }
        }
        return mVolley;
    }

    /**
     * 设置是否使用缓存
     * @param cache 缓存对象
     * @return  是否使用缓存
     */
    public Volley setDataCache(boolean cache){
        isDataCache=cache;
        return this;
    }

    /**
     * 获取结果
     * @param url 请求的url
     * @param method 请求方法
     * @param body 请求体
     * @param header    请求头
     * @return 返回当前类
     */
    public Volley getResult(String url,HttpConn.Method method,JSONObject body,Map<String,String> header){
        if (mUIHandler==null){
            mUIHandler=new UIHandler();
        }
        String cache = mCache.getCache(url);
        if (cache!=null&&!(cache).isEmpty()){
            Log.w("thanatos", "run: find cache in memery" );
            postRes(url,cache);
        }else {
            if (mSSLBuilder!=null){
                addTask(buildTask(url, method,body,header,mSSLBuilder.getSSlSocketFactory(cerId)));
            }else {
                addTask(buildTask(url, method,body,header,null));
            }

        }
        return this;
    }

    /**
     * 添加任务
     * @param runnable 任务
     */
    private synchronized void addTask(Runnable runnable) {
        if (mTaskQueue!=null){
            mTaskQueue.add(runnable);
            if (mQueueHandler==null){
                try {
                    mQueueHandlerSemaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            mQueueHandler.sendEmptyMessage(0x100);
        }
    }

    /**
     * 获取一个任务
     * @return 任务
     */
    private  Runnable getTask(){
        return mTaskQueue.removeFirst();
    }

    /**
     * 创建任务
     * @param url 请求url
     * @param method 请求方法
     * @param body  请求体
     * @param header 请求头
     * @param socketFactory 支持https的证书
     * @return 返回一个线程对象
     */
    private Runnable buildTask(final String url, final HttpConn.Method method, final JSONObject body, final Map<String,String> header
                                    , final SSLSocketFactory socketFactory) {
        return new Runnable() {
            @Override
            public void run() {
                /**
                 * 使用缓存
                 */
                if (isDataCache){
                    String cache = mCache.getCache(url);
                    if (cache!=null){
                        Log.w("thanatos", "run: find cache in memery" );
                        postRes(url,cache);

                    }else {
                        getDate(url,method,body,header,socketFactory,true);
                    }

                }else {//不使用缓存
                    getDate(url,method,body,header,socketFactory,false);

                }
                mTaskQueueSizeSemaphore.release();
            }
        };
    }

    /**
     * 获取数据
     */
    private void getDate(final String url, final HttpConn.Method method, final JSONObject body, final Map<String,String> header
            , final SSLSocketFactory socketFactory,boolean isDataCache){
        if (contextWeakReference==null){
            onDestory();
            return;
        }
        Object[] urls = HttpConn.getInstance().getResult(contextWeakReference.get(), method, url, body, header,
                socketFactory);
        if (urls!=null&&200==(int)urls[0]){
            if (urls[1]!=null){
                String text = Utils.getText((InputStream) urls[1]);
                if (isDataCache){
                    mCache.pubCache(url,  text);
                }
                postRes(url,text);
            }
        }else if (urls!=null){
            Message msg=Message.obtain();
            msg.obj= (int) urls[0];
            dataCallBackHandler.sendMessage(msg);

        }

    }

    /**
     * 获取数据回调接口
     */
    private class UIHandler extends Handler{
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            ResultHolder holder= (ResultHolder) msg.obj;
            if (mDataCallBack!=null)mDataCallBack.success(holder.res);

        }

    }

    /**
     * 刷新数据到主线程并进行回调
     * @param url 获取数据的请求地址
     * @param res 获取到的结果值
     */
    private void postRes(String url,String res){
        Message msg=Message.obtain();
        ResultHolder holder=new ResultHolder();
        holder.url=url;
        holder.res=res;
        msg.obj=holder;
        mUIHandler.sendMessage(msg);
    }

    /**
     * 轮询队列
     */
    private  class QueueHandler extends Handler{
         QueueHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            mThreadPool.execute(getTask());
            try {
                mTaskQueueSizeSemaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * 获取数据出错回调
     */
    private  class DataCallBackHandler extends Handler{

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int code= (int) msg.obj;
            if (Integer.MAX_VALUE==code){
                if (mDataCallBack!=null)mDataCallBack.error("网络出错");
            }else {
                if (mDataCallBack!=null)mDataCallBack.error(code+"");
            }

        }
    }

    /**
     * 数据实体
     */
     private static class  ResultHolder{
        String url;
        String res;
    }

    /**
     * 数据回调
     * @param callBack 回调接口
     */
    public void dataCallBack(DataCallBack callBack){
        mDataCallBack=callBack;
    }


    public void onDestory(){
        if (mSSLBuilder!=null){
            mSSLBuilder.onDestory();
        }
        if (contextWeakReference!=null){
            contextWeakReference.clear();
            contextWeakReference=null;

        }
    }




}
