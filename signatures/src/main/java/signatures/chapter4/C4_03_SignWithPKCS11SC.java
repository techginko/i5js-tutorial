/*
 * This class is part of the white paper entitled
 * "Digital Signatures for PDF documents"
 * written by Bruno Lowagie
 * 
 * For more info, go to: http://itextpdf.com/sales
 */
package signatures.chapter4;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import sun.security.pkcs11.SunPKCS11;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.log.LoggerFactory;
import com.itextpdf.text.log.SysoLogger;
import com.itextpdf.text.pdf.security.CertificateUtil;
import com.itextpdf.text.pdf.security.CrlClient;
import com.itextpdf.text.pdf.security.CrlClientOnline;
import com.itextpdf.text.pdf.security.DigestAlgorithms;
import com.itextpdf.text.pdf.security.MakeSignature.CryptoStandard;
import com.itextpdf.text.pdf.security.OcspClient;
import com.itextpdf.text.pdf.security.OcspClientBouncyCastle;
import com.itextpdf.text.pdf.security.TSAClient;
import com.itextpdf.text.pdf.security.TSAClientBouncyCastle;

public class C4_03_SignWithPKCS11SC extends C4_02_SignWithPKCS11USB {
	public static final String SRC = "src/main/resources/hello.pdf";
	public static final String DEST = "results/chapter4/hello_smartcard_%s.pdf";
	public static final String DLL = "c:/windows/system32/beidpkcs11.dll";

	public static void main(String[] args) throws IOException, GeneralSecurityException, DocumentException {

		LoggerFactory.getInstance().setLogger(new SysoLogger());
		
		Properties properties = new Properties();
		properties.load(new FileInputStream("c:/home/blowagie/key.properties"));

		String config = "name=beid\n" +
				"library=" + DLL + "\n" +
				"slotListIndex = " + getSlotsWithTokens(DLL)[0];
		ByteArrayInputStream bais = new ByteArrayInputStream(config.getBytes());
		Provider providerPKCS11 = new SunPKCS11(bais);
        Security.addProvider(providerPKCS11);
        System.out.println(providerPKCS11.getName());
		BouncyCastleProvider providerBC = new BouncyCastleProvider();
		Security.addProvider(providerBC);
        KeyStore ks = KeyStore.getInstance("PKCS11");
		ks.load(null, null);
		Enumeration<String> aliases = ks.aliases();
		while (aliases.hasMoreElements()) {
			System.out.println(aliases.nextElement());
		}
		smartcardsign(providerPKCS11.getName(), ks, "Authentication");
		smartcardsign(providerPKCS11.getName(), ks, "Signature");
	}
        
	public static void smartcardsign(String provider, KeyStore ks, String alias) throws GeneralSecurityException, IOException, DocumentException {
        PrivateKey pk = (PrivateKey)ks.getKey(alias, null);
        Certificate[] chain = ks.getCertificateChain(alias);
        OcspClient ocspClient = new OcspClientBouncyCastle();
        TSAClient tsaClient = null;
        for (int i = 0; i < chain.length; i++) {
        	X509Certificate cert = (X509Certificate)chain[i];
        	String tsaUrl = CertificateUtil.getTSAURL(cert);
        	if (tsaUrl != null) {
        		System.out.println(tsaUrl);
        		tsaClient = new TSAClientBouncyCastle(tsaUrl);
        		break;
        	}
        }
        List<CrlClient> crlList = new ArrayList<CrlClient>();
        crlList.add(new CrlClientOnline(chain));
        C4_03_SignWithPKCS11SC app = new C4_03_SignWithPKCS11SC();
		app.sign(SRC, String.format(DEST, alias), chain, pk, DigestAlgorithms.SHA256, provider, CryptoStandard.CMS,
				"Test", "Ghent", crlList, ocspClient, tsaClient, 0);
	}
}
