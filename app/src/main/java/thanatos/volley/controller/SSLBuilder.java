package thanatos.volley.controller;

import android.content.Context;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created on 2017/1/12.
 * 作者：by thanatos
 * 作用：
 */

public class SSLBuilder {

    private WeakReference<Context> reference;

    private static SSLBuilder mSSLBuilder;

    private SSLBuilder(Context context) {
        reference=new WeakReference<>(context);
    }

    public static SSLBuilder getInstance(Context context){
        if (mSSLBuilder==null){
            synchronized (SSLBuilder.class){
                if (mSSLBuilder==null){
                    mSSLBuilder=new SSLBuilder(context);
                }
            }
        }
        return mSSLBuilder;
    }

    private KeyStore getKeyStore(int certResourceID){
        InputStream inputStream = reference.get().getResources().openRawResource(
                certResourceID);
        try {
            //以 x.509 读取证书
            CertificateFactory cerFactory = CertificateFactory.getInstance("X.509");
            Certificate cer = cerFactory.generateCertificate(inputStream);
            //创建一个证书库，并将证书导入证书库
            String keyStoreType=KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", cer);
            return keyStore;
        } catch (Exception e){
            return null;
        }
    }

    private TrustManagerFactory getTrustManager(KeyStore ks){
        String tmfAlgorithm=TrustManagerFactory.getDefaultAlgorithm();
        try {
            TrustManagerFactory trustManagerFactory=TrustManagerFactory.getInstance(tmfAlgorithm);
            trustManagerFactory.init(ks);
            return trustManagerFactory;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return null;
        }
    }

    public SSLSocketFactory getSSlSocketFactory(int cerId){
        try {
            SSLContext sSLContext=SSLContext.getInstance("TLS");
            sSLContext.init(null,getTrustManager(getKeyStore(cerId)).getTrustManagers(),null);
            return sSLContext.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (KeyManagementException e) {
            e.printStackTrace();
            return null;
        }
    }


    public void onDestory(){
        if (reference!=null){
            reference.clear();
            reference=null;
        }
    }
}
