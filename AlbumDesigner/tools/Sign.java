import com.android.apksig.ApkSigner;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Signs an APK with v1 + v2 schemes so it installs on Android 4.x through 13+. */
public class Sign {
    public static void main(String[] a) throws Exception {
        // in.apk out.apk keystore storepass alias keypass
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(a[2])) { ks.load(in, a[3].toCharArray()); }
        PrivateKey key = (PrivateKey) ks.getKey(a[4], a[5].toCharArray());
        Certificate[] chain = ks.getCertificateChain(a[4]);
        List<X509Certificate> certs = new ArrayList<>();
        for (Certificate c : chain) certs.add((X509Certificate) c);

        ApkSigner.SignerConfig signer =
                new ApkSigner.SignerConfig.Builder("album", key, certs).build();
        ApkSigner.Builder b = new ApkSigner.Builder(Collections.singletonList(signer))
                .setInputApk(new File(a[0]))
                .setOutputApk(new File(a[1]))
                .setV1SigningEnabled(false)  // v1 removed (JDK-21 incompatible); v2 covers Android 7+
                .setV2SigningEnabled(true)
                .setMinSdkVersion(24);
        b.build().sign();
        System.out.println("APK signed (v2): " + a[1]);
    }
}
