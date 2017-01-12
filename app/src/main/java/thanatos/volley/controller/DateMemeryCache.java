package thanatos.volley.controller;

import android.util.Log;
import android.util.LruCache;

/**
 * Created on 2017/1/12.
 * 作者：by thanatos
 * 作用：
 */

public class DateMemeryCache implements Cache<String> {

    private LruCache<String,String > mLruCache;

    public DateMemeryCache() {
        int mCacheSize = (int) (Runtime.getRuntime().maxMemory() / 8);
        mLruCache=new LruCache<>(mCacheSize);
    }

    @Override
    public String getCache(String url) {
        return mLruCache.get(url);
    }

    @Override
    public void pubCache(String url, String res) {
        if (mLruCache!=null){
            mLruCache.put(url, res);
        }else {
            throw new NullPointerException("please init Cache");
        }
    }
}
