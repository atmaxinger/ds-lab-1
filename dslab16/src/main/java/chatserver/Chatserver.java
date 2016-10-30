package chatserver;

import chatserver.Service.ChatService;
import util.Config;

import java.io.*;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Set;

public class Chatserver implements IChatserverCli, Runnable {

    private String componentName;
    private Config config;
    private InputStream userRequestStream;
    private PrintStream userResponseStream;

    private ServerSocket serverSocket;
    private DatagramSocket udpSocket;


    /**
     * @param componentName      the name of the component - represented in the prompt
     * @param config             the configuration to use
     * @param userRequestStream  the input stream to read user input from
     * @param userResponseStream the output stream to write the console output to
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

    /**
     * @param args the first argument is the name of the {@link Chatserver}
     *             component
     */
    public static void main(String[] args) throws InterruptedException, IOException {
        Chatserver chatserver = new Chatserver(args[0],
                new Config("chatserver"), System.in, System.out);

        chatserver.run();
    }

    private HashMap<String, String> getRegisteredUsers() {
        HashMap<String, String> registeredUsers = new HashMap<>();

        Config userConfig = new Config("user");
        Set<String> keys = userConfig.listKeys();

        for (String key : keys) {
            String username = key.subSequence(0, key.lastIndexOf('.')).toString();
            String password = userConfig.getString(key);

            registeredUsers.put(username, password);
        }

        return registeredUsers;
    }

    @Override
    public void run() {
        ChatService.getInstance().setRegisteredUsers(getRegisteredUsers());

        TcpServer tcpServer = new TcpServer(serverSocket);
        UdpServer udpServer = new UdpServer(udpSocket);

        Thread tcp = new Thread(tcpServer);
        tcp.start();

        Thread udp = new Thread(udpServer);
        udp.start();

        String userCommand = "";

        BufferedReader userReader = new BufferedReader(new InputStreamReader(userRequestStream));
        PrintWriter userWriter = new PrintWriter(new OutputStreamWriter(userResponseStream));

        try {
            while (!(userCommand = userReader.readLine().trim()).startsWith("!exit")) {
                if (userCommand.startsWith("!users")) {
                    userWriter.println(users());
                } else {
                    userWriter.println("Unknown command");
                }

                userWriter.flush();
            }

            exit();

            serverSocket.close();
            udpSocket.close();

            tcp.join();
            udp.join();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // TODO
    }

    @Override
    public String users() throws IOException {
        String msg = ChatService.getInstance().GetAllUsers();
        return msg;
    }

    @Override
    public String exit() throws IOException {
        ChatService.getInstance().CloseAllClientSockets();
        return null;
    }
}
