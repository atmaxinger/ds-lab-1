package client;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by Maximilian on 30.10.2016.
 */
public class PrivateMessageSender implements Runnable {
    private PrintWriter userWriter = null;
    private String receiverUsername = null;
    private InetAddress receiverAddress = null;
    private int receiverPort;
    private String message = null;

    public PrivateMessageSender(PrintWriter userWriter, String receiverUsername, InetAddress receiverAddress, int receiverPort, String message) {
        this.userWriter = userWriter;
        this.receiverUsername = receiverUsername;
        this.receiverAddress = receiverAddress;
        this.receiverPort = receiverPort;
        this.message = message;
    }

    @Override
    public void run() {
        try {
            Socket socket = new Socket(receiverAddress, receiverPort);

            PrintWriter tcpWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
            BufferedReader tcpReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            tcpWriter.println(message);
            tcpWriter.flush();

            String resp = tcpReader.readLine();
            if (resp.startsWith("!ack")) {
                synchronized (userWriter) {
                    userWriter.println("MSG: " + receiverUsername + " replied with !ack.");
                    userWriter.flush();
                }
            }

            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (SocketException se) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
