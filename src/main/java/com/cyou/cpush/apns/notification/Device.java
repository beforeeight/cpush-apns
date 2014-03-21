package com.cyou.cpush.apns.notification;

public class Device {
	private String token;

	public Device(String token) {
		super();
		this.token = token;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

}
