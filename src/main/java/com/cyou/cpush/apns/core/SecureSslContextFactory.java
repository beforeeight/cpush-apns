package com.cyou.cpush.apns.core;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.cyou.cpush.apns.conf.Credentials;

public class SecureSslContextFactory {
	private static final String PROTOCOL = "TLS";
	private static final ConcurrentHashMap<Credentials, SSLContext> CLIENT_CONTEXT = new ConcurrentHashMap<Credentials, SSLContext>();

	public static SSLContext getSSLContext(Credentials conf) {
		SSLContext clientContext = CLIENT_CONTEXT.get(conf);
		if (clientContext == null) {
			try {
				String algorithm = Security
						.getProperty("ssl.KeyManagerFactory.algorithm");
				if (algorithm == null) {
					algorithm = "SunX509";
				}

				KeyStore keyStore = KeyStore.getInstance("PKCS12");
				keyStore.load(new ByteArrayInputStream(conf.getCertification()), conf
						.getPassword().toCharArray());

				KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
				kmf.init(keyStore, conf.getPassword().toCharArray());

				clientContext = SSLContext.getInstance(PROTOCOL);
				clientContext.init(kmf.getKeyManagers(),
						new TrustManager[] { new X509TrustManager() {
							@Override
							public X509Certificate[] getAcceptedIssuers() {
								return null;
							}

							@Override
							public void checkServerTrusted(X509Certificate[] chain,
									String authType) throws CertificateException {
							}

							@Override
							public void checkClientTrusted(X509Certificate[] chain,
									String authType) throws CertificateException {
								throw new CertificateException("Client is not trusted.");
							}
						} }, null);
				CLIENT_CONTEXT.putIfAbsent(conf, clientContext);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return clientContext;
	}
}
