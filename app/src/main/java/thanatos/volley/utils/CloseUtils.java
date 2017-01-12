package thanatos.volley.utils;

import java.io.Closeable;
import java.io.IOException;

/**
 * Created on 2017/1/12.
 * 作者：by thanatos
 * 作用：关闭所有的流
 */

public class CloseUtils  {

    private CloseUtils(){}

    public static void close(Closeable closeable){
        if (closeable!=null){
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
