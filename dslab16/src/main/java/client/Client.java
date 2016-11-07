package client;

import cli.Command;
import cli.Shell;
import util.Config;

import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Client implements IClientCli, Runnable {

    private final String ERR_WRONG_NUMBER_OF_ARGUMENTS = "Wrong number of arguments.";
    private String componentName;
    private Config config;
    private InputStream userRequestStream;
    private PrintStream userResponseStream;
    private BufferedReader userReader;
    private PrintWriter userWriter;
    private String username = "";
    private boolean loggedin = false;
    private Socket tcpSocket = null;
    private PrintWriter serverWriter = null;
    private PublicListenerThread publicListenerThread;
    private ConcurrentLinkedQueue<String> serverResponseQueue = new ConcurrentLinkedQueue<>();
    private ServerSocket privateServerSocket;
    private Shell shell;

    /**
     * @param componentName      the name of the component - represented in the prompt
     * @param config             the configuration to use
     * @param userRequestStream  the input stream to read user input from
     * @param userResponseStream the output stream to write the console output to
     */
    public Client(String componentName, Config config,
                  InputStream userRequestStream, PrintStream userResponseStream) throws IOException {
        this.componentName = componentName;
        this.config = config;
        this.userRequestStream = userRequestStream;
        this.userResponseStream = userResponseStream;

        tcpSocket = new Socket(config.getString("chatserver.host"), config.getInt("chatserver.tcp.port"));


        // create a writer to send messages to the server
        serverWriter = new PrintWriter(tcpSocket.getOutputStream(), true);

        userReader = new BufferedReader(new InputStreamReader(userRequestStream));
        userWriter = new PrintWriter(new OutputStreamWriter(userResponseStream));

        shell = new Shell(componentName, userRequestStream, userResponseStream);
        shell.register(this);

        publicListenerThread = new PublicListenerThread(serverResponseQueue, tcpSocket, userWriter);
        (new Thread(publicListenerThread)).start();

        // TODO
    }

    /**
     * @param args the first argument is the name of the {@link Client} component
     */
    public static void main(String[] args) throws IOException {
        Client client = new Client(args[0], new Config("client"), System.in,
                System.out);

        client.run();
    }

    private void print(String s) {
        synchronized (userWriter) {
            userWriter.print(s);
            userWriter.flush();
        }
    }

    private void println(String s) {
        synchronized (userWriter) {
            userWriter.println(s);
            userWriter.flush();
        }
    }

    @Override
    public void run() {
        boolean finished = false;

        try {
            while (!finished) {
                String request = "";

                print("> ");

                request = userReader.readLine().trim();

                String parts[] = request.split(" ");


                if (request.startsWith("!exit")) {
                    finished = true;
                    exit();
                    return;
                }
                else if (request.startsWith("!list")) {
                    if (parts.length != 1) {
                        println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
                    } else {
                        String ret = list();
                        println(ret);
                    }
                }

                if(!loggedin) {
                    if (request.startsWith("!login ")) {
                        if (parts.length != 3) {
                            println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
                        } else {
                            username = parts[1];
                            String ret = login(parts[1], parts[2]);
                            println(ret);
                        }
                    }
                    else {
                        println("Not logged in.");
                    }
                }
                else {
                    if (request.startsWith("!logout")) {
                        String ret = logout();
                        println(ret);
                    } else if(request.startsWith("!login")) {
                        println("Already logged in.");
                    }
                    else if (request.startsWith("!send ")) {
                        if (parts.length == 1) {
                            println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
                        } else {
                            send(request.substring("!send ".length()));
                        }
                    } else if (request.startsWith("!msg")) {
                        if (parts.length < 3) {
                            println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
                        } else {
                            String msg = "";

                            for (int i = 2; i < parts.length; i++) {
                                msg += parts[i] + " ";
                            }

                            String ret = msg(parts[1], msg);

                            println(ret);
                        }
                    } else if (request.startsWith("!lookup")) {
                        if (parts.length != 2) {
                            println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
                        } else {
                            String ret = lookup(parts[1]);
                            println(ret);
                        }
                    } else if (request.startsWith("!register")) {
                        if (parts.length != 2) {
                            println(ERR_WRONG_NUMBER_OF_ARGUMENTS);
                        } else {
                            String ret = register(parts[1]);
                            println(ret);
                        }
                    } else if (request.startsWith("!lastMsg")) {
                        println(lastMsg());
                    }
                }
            }
        } catch (SocketException se) {
            // Wsl wurde der Server beendet.
        }
        catch (ServerHasBeenClosedException se) {
            println("Server has been closed");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFromResponseQueue(String what) throws ServerHasBeenClosedException {
        String resp = null;
        what = what.trim();
        String search = "!response " + what;

        while (resp == null && !tcpSocket.isClosed()) {
            synchronized (serverResponseQueue) {
                for (String s : serverResponseQueue) {
                    if (s.startsWith(search)) {
                        resp = s;
                        break;
                    }
                }

                if (resp != null) {
                    serverResponseQueue.remove(resp);
                } else {
                    // TODO: Better waiting
                    try {
                        serverResponseQueue.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        if(resp == null) {
            throw new ServerHasBeenClosedException();
        }

        // cut off the !response !what
        resp = resp.substring((search + " ").length());

        return resp;
    }

    @Override
    @Command
    public String login(String username, String password) throws IOException {
        serverWriter.println("!login " + username + " " + password);

        String response = getFromResponseQueue("!login");

        if(response.toLowerCase().contains("success")) {
            loggedin = true;
            this.username = username;
        }


        return "LOGIN: " + response;
    }

    @Override
    public String logout() throws IOException {
        serverWriter.println("!logout");

        String response = getFromResponseQueue("!logout");

        if(response.toLowerCase().contains("success")) {
            loggedin = false;
            this.username = "";
        }

        return "LOGOUT: " + response;
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
        String lu = lookup(username);
        lu = lu.substring(lu.indexOf(' '));

        if (lu.toLowerCase().contains("wrong username")) {
            return "MSG: " + lu;
        }

        try {
            String[] parts = lu.split(":");
            InetAddress address = InetAddress.getByName(parts[0].trim());
            int port = Integer.parseInt(parts[1]);
            if(!checkPort(port)) {
                throw new NumberFormatException();
            }

            PrivateMessageSender pms = new PrivateMessageSender(userWriter, username, address, port, this.username + ": " + message);
            (new Thread(pms)).start();
        }
        catch (NumberFormatException ex) {
            return "MSG: " + username + " has invalid port";
        }
        catch (UnknownHostException ex) {
            return "MSG: " + username + " has invalid address";
        }

        return "MSG: Message sent.";
    }

    @Override
    public String lookup(String username) throws IOException {
        serverWriter.println("!lookup " + username);
        return "LOOKUP: " + getFromResponseQueue("!lookup");
    }

    @Override
    public String register(String privateAddress) throws IOException {
        if (privateServerSocket != null && privateServerSocket.isClosed() == false) {
            return "REGISTER: there is already an open socket";
        }

        try {
            String[] parts = privateAddress.split(":");
            InetAddress address = InetAddress.getByName(parts[0].trim());

            if(parts.length != 2) {
                return "REGISTER: Invalid format (ipaddress:port)";
            }

            int port = Integer.parseInt(parts[1]);
            if(!checkPort(port)) {
                throw new NumberFormatException();
            }

            privateServerSocket = new ServerSocket(port, 100, address);

            PrivateMessageServerService pms = new PrivateMessageServerService(privateServerSocket, userWriter);
            (new Thread(pms)).start();
        }
        catch (NumberFormatException ex) {
            return "REGISTER: Invalid port";
        }
        catch (UnknownHostException ex) {
            return "REGISTER: Invalid address";
        }
        catch (IOException ex) {
            return "REGISTER: Error opening socket";
        }

        serverWriter.println("!register " + privateAddress);
        return "REGISTER: " + getFromResponseQueue("!register");
    }

    @Override
    public String lastMsg() throws IOException {
        return "LASTMSG: " + publicListenerThread.getLastMsg();
    }

    private boolean checkPort(int port) {
        return port >= 1 && port <= 65535;
    }

    @Override
    public String exit() throws IOException {
        tcpSocket.close();

        if (privateServerSocket != null && !privateServerSocket.isClosed()) {
            privateServerSocket.close();
        }

        return null;
    }

    // --- Commands needed for Lab 2. Please note that you do not have to
    // implement them for the first submission. ---

    @Override
    public String authenticate(String username) throws IOException {
        // TODO Auto-generated method stub
        return null;
    }

}
