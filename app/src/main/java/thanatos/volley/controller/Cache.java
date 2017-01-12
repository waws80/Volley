package thanatos.volley.controller;

/**
 * Created on 2017/1/12.
 * 作者：by thanatos
 * 作用：缓存的控制器
 */

public interface Cache<R> {
    R getCache(String url);
    void pubCache(String url,R res);
}
