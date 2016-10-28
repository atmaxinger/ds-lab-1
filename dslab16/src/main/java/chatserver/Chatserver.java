package chatserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import chatserver.Service.ChatService;
import util.Config;

public class Chatserver implements IChatserverCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	private ServerSocket serverSocket;
	private DatagramSocket udpSocket;


	/**
	 * @param componentName
	 *            the name of the component - represented in the prompt
	 * @param config
	 *            the configuration to use
	 * @param userRequestStream
	 *            the input stream to read user input from
	 * @param userResponseStream
	 *            the output stream to write the console output to
	 */
	public Chatserver(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) throws IOException {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		int tcpPort = config.getInt("tcp.port");
		int udpPort = config.getInt("udp.port");

		serverSocket = new ServerSocket(tcpPort);
		udpSocket = new DatagramSocket(udpPort);
	}

	private HashMap<String, String> getRegisteredUsers()
	{
		HashMap<String, String> registeredUsers = new HashMap<>();

		Config userConfig = new Config("user");
		Set<String> keys = userConfig.listKeys();

		for(String key : keys)
		{
			String username = key.subSequence(0, key.lastIndexOf('.')).toString();
			String password = userConfig.getString(key);

			registeredUsers.put(username, password);
		}

		return registeredUsers;
	}

	@Override
	public void run() {
		ChatService.getInstance().setRegisteredUsers(getRegisteredUsers());

		TcpServer tcpServer = new TcpServer(serverSocket, this);
		UdpServer udpServer = new UdpServer(udpSocket);

		Thread tcp = new Thread(tcpServer);
		tcp.start();

		Thread udp = new Thread(udpServer);
		udp.start();


		// TODO
	}

	@Override
	public String users() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String exit() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Chatserver}
	 *            component
	 */
	public static void main(String[] args) throws InterruptedException, IOException {
		Chatserver chatserver = new Chatserver(args[0],
				new Config("chatserver"), System.in, System.out);
		// TODO: start the chatserver

		Thread t = new Thread(chatserver);
		t.start();
		t.join();
	}
}
