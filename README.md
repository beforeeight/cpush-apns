cpush-apns
==========

High-performance, Asynchronous, Netty based, Provider of Apple Push Notification Service.
It's easy to use.

All notifications are sent with this Interface.
public interface ApnsConnection {
	public Future<Void> push(Notification notification);

	public Future<Iterable<ErrorPacket>> push(Notification... notifications);
}

All you need to do is create a new instance of ApnsConnection.
You can new a DefaultApnsConnection instance with a Credentials, like this:
DefaultCredentials credential = new DefaultCredentials(false);
credential.setCertification(...); // argument is p12 authtication on the forms of an byte array
credential.setPassword("...."); // p12 password
ApnsConnection connection = new DefaultApnsConnection(credential);

Now, you can send notification to any IOS device.
