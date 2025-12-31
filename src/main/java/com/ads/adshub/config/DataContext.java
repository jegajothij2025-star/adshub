package com.ads.adshub.config;

public class DataContext {

	private static final ThreadLocal<Long> appIdHolder = new ThreadLocal<>();
	private static final ThreadLocal<Integer> userIdHolder = new ThreadLocal<>();
	private static final ThreadLocal<String> correlationIdHolder = new ThreadLocal<>();

	public static void setCorrelationId(String correlationId) {
		correlationIdHolder.set(correlationId);
	}

	public static String getCorrelationId() {
		return correlationIdHolder.get();
	}

	public static void setAppId(Long appId) {
		appIdHolder.set(appId);
	}

	public static Long getAppId() {
		return appIdHolder.get();
	}

	public static void setUserId(Integer userId) {
		userIdHolder.set(userId);
	}

	public static Integer getUserId() {
		return userIdHolder.get();
	}

	public static void clear() {
		appIdHolder.remove();
		userIdHolder.remove();
		correlationIdHolder.remove();
	}
}
