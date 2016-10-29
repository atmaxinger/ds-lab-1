package client;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import cli.Command;
import cli.Shell;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import util.Config;

public class Client implements IClientCli, Runnable {

	private String componentName;
	private Config config;
	private InputStream userRequestStream;
	private PrintStream userResponseStream;

	Socket tcpSocket = null;
	PrintWriter serverWriter = null;

	BufferedReader reader;
	PrintWriter writer;

	ListenerThread listenerThread;

	ConcurrentLinkedQueue<String> serverResponseQueue = new ConcurrentLinkedQueue<>();

	private Shell shell;

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
	public Client(String componentName, Config config,
			InputStream userRequestStream, PrintStream userResponseStream) throws IOException {
		this.componentName = componentName;
		this.config = config;
		this.userRequestStream = userRequestStream;
		this.userResponseStream = userResponseStream;

		tcpSocket = new Socket(config.getString("chatserver.host"),  config.getInt("chatserver.tcp.port"));


		// create a writer to send messages to the server
		serverWriter = new PrintWriter(tcpSocket.getOutputStream(), true);

		reader = new BufferedReader(new InputStreamReader(userRequestStream));
		writer = new PrintWriter(new OutputStreamWriter(userResponseStream));

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);

		listenerThread = new ListenerThread(serverResponseQueue, tcpSocket, writer);
		(new Thread(listenerThread)).start();

		// TODO
	}

	private final String ERR_WRONG_NUMBER_OF_ARGUMENTS = "Wrong number of arguments.";

	private void print(String s) {
		synchronized (writer) {
			writer.print(s);
			writer.flush();
		}
	}

	private void println(String s) {
		synchronized (writer) {
			writer.println(s);
			writer.flush();
		}
	}

	@Override
	public void run() {
		boolean finished = false;

		try {
			while(!finished) {
				String request = "";

				synchronized (writer) {
					writer.print(">");
					writer.flush();
				}

				request = reader.readLine().trim();

				String parts[] = request.split(" ");

				if(request.startsWith("!login ")) {
					if(parts.length != 3) {
						println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
					}
					else {
						String ret = login(parts[1], parts[2]);
						println(ret);
					}
				}
				else if(request.startsWith("!logout ")) {
					String ret = logout();
					writer.println(ret);
				} else if (request.startsWith("!send ")) {
					if(parts.length == 1) {
						println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
					}
					else {
						send(request.substring("!send ".length()));
					}
				} else if(request.startsWith("!list")) {
					if(parts.length != 1) {
						println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
					}
					else {
						String ret = list();
						writer.println(ret);
					}
				}
				else if(request.startsWith("!msg")) {
					// TODO
					throw new NotImplementedException();
				}
				else if(request.startsWith("!lookup")) {
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if(parts.length != 2) {
						println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
					}
					else {
						String ret = lookup(parts[1]);
						println(ret);
					}
				}
				else if(request.startsWith("!lastMsg")) {
					println(lastMsg());
				}

				writer.flush();
			}
		} catch (SocketException se) {
			// Wsl wurde der Server beendet.
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getFromResponseQueue(String what) {
		String resp = null;
		what = what.trim();
		String search = "!response " + what;

		while(resp == null) {
			for (String s : serverResponseQueue) {
				if (s.startsWith(search)) {
					resp = s;
					break;
				}
			}

			if(resp != null) {
				serverResponseQueue.remove(resp);
			}
			else {
				// TODO: Better waiting
			}
		}

		// cut off the !response !what
		resp = resp.substring( (search + " ").length() );

		return resp;
	}

	@Override
	@Command
	public String login(String username, String password) throws IOException {
		serverWriter.println("!login " + username + " " + password);
		return "LOGIN: " + getFromResponseQueue("!login");
	}

	@Override
	public String logout() throws IOException {
		serverWriter.println("!logout");
		return "LOGOUT: " + getFromResponseQueue("!logout");
	}

	@Override
	public String send(String message) throws IOException {
		serverWriter.println("!send " + message);
		return "";
	}

	@Override
	public String list() throws IOException {
		DatagramSocket udpSocket = new DatagramSocket();

		byte[] buffer = "!list".getBytes();
		DatagramPacket packet;

		packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(config.getString("chatserver.host")), config.getInt("chatserver.udp.port"));
		udpSocket.send(packet);

		buffer = new byte[udpSocket.getReceiveBufferSize()];
		packet = new DatagramPacket(buffer, buffer.length);

		udpSocket.receive(packet);

		return "LIST: " + new String(packet.getData());
	}

	@Override
	public String msg(String username, String message) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lookup(String username) throws IOException {
		serverWriter.println("!lookup " + username);
		return "LOOKUP: " + getFromResponseQueue("!lookup");
	}

	@Override
	public String register(String privateAddress) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String lastMsg() throws IOException {
		return "LASTMSG: " + listenerThread.getLastMsg();
	}

	@Override
	public String exit() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @param args
	 *            the first argument is the name of the {@link Client} component
	 */
	public static void main(String[] args) throws IOException {
		Client client = new Client(args[0], new Config("client"), System.in,
				System.out);

		client.run();
	}

	// --- Commands needed for Lab 2. Please note that you do not have to
	// implement them for the first submission. ---

	@Override
	public String authenticate(String username) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

}
