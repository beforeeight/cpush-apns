package com.cyou.cpush.apns.util;

import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

public class JSONUtil {
	private static JsonParser parser = new JsonParser();

	private static Gson gson = new GsonBuilder().create();

	public static JsonElement toJSON(Object o) {
		return parser.parse(gson.toJson(o));
	}

	public static <T> T fromJson(JsonElement json, Class<T> clazz) {
		return gson.fromJson(json, clazz);
	}

	public static <T> T fromJson(String json, Class<T> clazz) {
		return gson.fromJson(json, clazz);
	}

	public static String toJSONString(List<String> l) {
		String s = gson.toJson(l, new TypeToken<List<String>>() {
		}.getType());
		return s;
	}

	public static String toJSONBoolean(List<Boolean> l) {
		String s = gson.toJson(l, new TypeToken<List<Boolean>>() {
		}.getType());
		return s;
	}

	public static String toJSONInteger(List<Integer> l) {
		String s = gson.toJson(l, new TypeToken<List<Integer>>() {
		}.getType());
		return s;
	}

	public static String toJSONString(String[] l) {
		String[] a = l;
		if (a == null) {
			a = new String[] {};
		}
		String s = gson.toJson(a, new TypeToken<String[]>() {
		}.getType());
		return s;
	}

	public static JsonArray arrayFromString(String s) {
		if (isBlank(s))
			return null;
		try {
			JsonElement e = parser.parse(s);
			if (e.isJsonArray()) {
				return (JsonArray) e;
			} else {
				return null;
			}
		} catch (JsonParseException e1) {
			return null;
		}
	}

	public static JsonObject objectFromString(String s) {
		if (isBlank(s))
			return null;
		try {
			JsonElement e = parser.parse(s);
			if (e.isJsonObject()) {
				return (JsonObject) e;
			} else {
				return null;
			}
		} catch (JsonParseException e1) {
			return null;
		}
	}

	public static void apply(JsonObject src, JsonObject dest) {
		apply(src, dest, false);
	}

	public static void apply(JsonObject src, JsonObject dest, boolean appendArray) {
		for (Entry<String, JsonElement> entry : src.entrySet()) {
			if (dest.has(entry.getKey())) {
				if (src.get(entry.getKey()).isJsonObject()
						&& dest.get(entry.getKey()).isJsonObject()) {
					apply((JsonObject) src.get(entry.getKey()),
							(JsonObject) dest.get(entry.getKey()), appendArray);
				} else if (src.get(entry.getKey()).isJsonArray()
						&& dest.get(entry.getKey()).isJsonArray() && appendArray) {
					JsonArray srcArray = src.get(entry.getKey()).getAsJsonArray();
					JsonArray destArray = dest.get(entry.getKey()).getAsJsonArray();
					Set<String> array = new HashSet<String>();
					for (JsonElement e : destArray) {
						if (e.isJsonPrimitive()) {
							array.add(e.getAsString());
						}
					}
					for (JsonElement e : srcArray) {
						if (e.isJsonPrimitive() && !array.contains(e.getAsString())) {
							destArray.add(e);
						}
					}
				} else {
					dest.add(entry.getKey(), entry.getValue());
				}
			} else {
				dest.add(entry.getKey(), entry.getValue());
			}
		}
	}

	public static void main(String[] args) {
		JsonObject src = new JsonObject();
		src.addProperty("a", "aaa");
		src.addProperty("b", "bbb");
		JsonObject secSrc1 = new JsonObject();
		secSrc1.addProperty("sec1-a", "src-sec1-a");
		secSrc1.addProperty("sec1-b", "src-sec1-b");
		JsonObject secSrc2 = new JsonObject();
		secSrc2.addProperty("sec2-a", "src-sec2-a");
		secSrc2.addProperty("sec2-b", "src-sec2-b");
		src.add("sec1", secSrc1);
		src.add("sec2", secSrc2);

		JsonObject dest = new JsonObject();
		dest.addProperty("a", "src");
		dest.addProperty("c", "ccc");
		JsonObject destSrc1 = new JsonObject();
		destSrc1.addProperty("sec1-a", "dest-sec1-a");
		destSrc1.addProperty("sec1-c", "dest-sec1-c");
		dest.add("sec1", destSrc1);

		apply(src, dest);
		System.out.println(dest);
	}

	private static boolean isBlank(String s) {
		if (s == null || "".equals(s.trim())) {
			return true;
		} else {
			return false;
		}
	}
}
