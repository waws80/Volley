package thanatos.volley.controller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created on 2017/1/12.
 * 作者：by thanatos
 * 作用：
 */

public class ThreadPool {

    private  ExecutorService mThreadPoolService;

    private static ThreadPool mThreadPool;

    //线程的最大数量等于CPU的数量
    private static final int DEFAULT_THREADSIZE=Runtime.getRuntime().availableProcessors();

    private static int mThreadPoolSize;

    private ThreadPool(int threadPoolSize){
        mThreadPoolService= Executors.newFixedThreadPool(threadPoolSize);
        mThreadPoolSize=threadPoolSize;
    }

    public static ExecutorService getInstance(){
        if (mThreadPool==null){
            synchronized (ThreadPool.class){
                if (mThreadPool==null){
                    mThreadPool=new ThreadPool(DEFAULT_THREADSIZE);
                }
            }
        }
        return mThreadPool.mThreadPoolService;
    }

    public static ExecutorService getInstance(int threadPoolSize){
        if (mThreadPool==null){
            synchronized (ThreadPool.class){
                if (mThreadPool==null){
                    mThreadPool=new ThreadPool(threadPoolSize);
                }
            }
        }
        return mThreadPool.mThreadPoolService;
    }

    public static int getThreadPoolSize(){
        return mThreadPoolSize;
    }
}
