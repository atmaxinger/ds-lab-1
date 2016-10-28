package client;

import java.io.*;
import java.net.*;

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
	BufferedReader serverReader = null;
	PrintWriter serverWriter = null;

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

		// create a reader to retrieve messages send by the server
		serverReader = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream()));
		// create a writer to send messages to the server
		serverWriter = new PrintWriter(tcpSocket.getOutputStream(), true);

		shell = new Shell(componentName, userRequestStream, userResponseStream);
		shell.register(this);

		// TODO
	}

	private final String ERR_WRONG_NUMBER_OF_ARGUMENTS = "Wrong number of arguments.";

	@Override
	public void run() {
		boolean finished = false;

		BufferedReader reader = new BufferedReader(new InputStreamReader(userRequestStream));
		PrintWriter writer = new PrintWriter(new OutputStreamWriter(userResponseStream));

		try {

			while(!finished) {
				writer.print(">");
				writer.flush();

				String request = reader.readLine().trim();
				String parts[] = request.split(" ");

				if(request.startsWith("!login ")) {
					if(parts.length != 3) {
						writer.println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
						writer.flush();
					}
					else {
						String ret = login(parts[1], parts[2]);
						writer.println(ret);
					}
				}
				else if(request.startsWith("!logout ")) {
					String ret = logout();
					writer.println(ret);
				} else if (request.startsWith("!send ")) {
					if(parts.length == 1) {
						writer.println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
					}
					else {
						send(request.substring("!send ".length()));
					}
				} else if(request.startsWith("!list")) {
					if(parts.length != 1) {
						writer.println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
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
					if(parts.length != 2) {
						writer.println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
					}
					else {
						String ret = lookup(parts[1]);
						writer.println(ret);
					}
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

	@Override
	@Command
	public String login(String username, String password) throws IOException {
		serverWriter.println("!login " + username + " " + password);
		return serverReader.readLine();
	}

	@Override
	public String logout() throws IOException {
		serverWriter.println("!logout");
		return serverReader.readLine();
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

		return new String(packet.getData());
	}

	@Override
	public String msg(String username, String message) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String lookup(String username) throws IOException {
		serverWriter.println("!lookup " + username);
		return serverReader.readLine();
	}

	@Override
	public String register(String privateAddress) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String lastMsg() throws IOException {
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
