package com.cyou.cpush.apns.conf;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class DefaultCredentials implements Credentials {
	private Gateway gateway;
	private byte[] certification;
	private String password;

	public DefaultCredentials(boolean production) {
		this(null, null, production);
	}

	public DefaultCredentials(byte[] certification, String password,
			boolean production) {
		this.certification = certification;
		this.password = password;
		gateway = Gateway.get(production);
	}

	@Override
	public byte[] getCertification() {
		return certification;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public Gateway getGateway() {
		return gateway;
	}

	public void setCertification(byte[] certification) {
		this.certification = certification;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setGateway(Gateway gateway) {
		this.gateway = gateway;
	}

	@Override
	public int hashCode() {
		HashCodeBuilder builder = new HashCodeBuilder();
		return builder.append(this.getCertification()).append(this.getPassword())
				.append(this.getGateway()).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (Credentials.class.isInstance(obj)) {
			Credentials target = (Credentials) obj;
			EqualsBuilder builder = new EqualsBuilder();
			return builder.append(this.getCertification(), target.getCertification())
					.append(this.getPassword(), target.getPassword())
					.append(this.getGateway(), target.getGateway()).isEquals();
		} else {
			return false;
		}
	}
}
