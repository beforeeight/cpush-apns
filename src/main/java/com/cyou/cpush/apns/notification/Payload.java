package com.cyou.cpush.apns.notification;

import java.util.HashMap;
import java.util.Map;

import com.cyou.cpush.apns.util.JSONUtil;
import com.google.gson.JsonObject;

public class Payload {
	private static final String DEFAULT_SOUND = "default";
	private static final int DEFAULT_BADGE = 0;

	private String alert;
	private int badge;
	private String sound;
	private Map<String, Object> customData;

	public Payload(String alert) {
		this(DEFAULT_SOUND, DEFAULT_BADGE, alert);
	}

	public Payload(String sound, int badge, String alert) {
		this.sound = sound;
		this.badge = badge;
		this.alert = alert;
	}

	public String getSound() {
		return sound;
	}

	public void setSound(String sound) {
		this.sound = sound;
	}

	public int getBadge() {
		return badge;
	}

	public void setBadge(int badge) {
		this.badge = badge;
	}

	public String getAlert() {
		return alert;
	}

	public void setAlert(String alert) {
		this.alert = alert;
	}

	public void addCustomData(String key, String value) {
		addCustomDataObject(key, value);
	}

	public void addCustomData(String key, Number value) {
		addCustomDataObject(key, value);
	}

	public void addCustomDataObject(String key, Object value) {
		if (customData == null) {
			customData = new HashMap<String, Object>();
		}
		if (key != null && value != null) {
			customData.put(key, value);
		}
	}

	public String toJson() {
		JsonObject j;
		if (customData == null) {
			j = new JsonObject();
		} else {
			j = (JsonObject) JSONUtil.toJSON(customData);
		}
		JsonObject aps = new JsonObject();
		aps.addProperty("alert", this.getAlert());
		aps.addProperty("badge", this.getBadge());
		aps.addProperty("sound", this.getSound());
		j.add("aps", aps);
		return j.toString();
	}
}
