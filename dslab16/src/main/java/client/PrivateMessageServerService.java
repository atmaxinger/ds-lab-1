package client;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by Maximilian on 29.10.2016.
 */
public class PrivateMessageServerService implements Runnable {
    private ServerSocket serverSocket;
    private PrintWriter userWriter;

    public PrivateMessageServerService(ServerSocket serverSocket, PrintWriter userWriter) {
        this.serverSocket = serverSocket;
        this.userWriter = userWriter;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();

                BufferedReader tcpReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter tcpWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

                String message = tcpReader.readLine();

                tcpWriter.println("!ack");
                tcpWriter.flush();

                socket.close();

                synchronized (userWriter) {
                    userWriter.println("PRIVATE MESSAGE: " + message);
                    userWriter.flush();
                }
            }
        } catch (SocketException e) {
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
