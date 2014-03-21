package com.cyou.cpush.apns.conf;

public abstract class Gateway {
	private static final String PRODUCTION_HOST = "gateway.push.apple.com";
	private static final int PRODUCTION_PORT = 2195;

	private static final String DEVELOPMENT_HOST = "gateway.sandbox.push.apple.com";
	private static final int DEVELOPMENT_PORT = 2195;

	private static Gateway DEVELOPMENT = new Gateway() {

		@Override
		public int port() {
			return DEVELOPMENT_PORT;
		}

		@Override
		public String host() {
			return DEVELOPMENT_HOST;
		}
	};
	private static Gateway PRODUCTION = new Gateway() {

		@Override
		public int port() {
			return PRODUCTION_PORT;
		}

		@Override
		public String host() {
			return PRODUCTION_HOST;
		}
	};

	public static Gateway get(boolean production) {
		if (production) {
			return PRODUCTION;
		} else {
			return DEVELOPMENT;
		}
	}

	public abstract String host();

	public abstract int port();
}
