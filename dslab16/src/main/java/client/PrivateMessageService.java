package client;

import com.sun.corba.se.spi.activation.Server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by Maximilian on 29.10.2016.
 */
public class PrivateMessageService implements Runnable {

    ServerSocket serverSocket;
    PrintWriter out;

    public PrivateMessageService(ServerSocket serverSocket, PrintWriter out) {
        this.serverSocket = serverSocket;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            Socket socket = serverSocket.accept();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));

            String message = reader.readLine();

            writer.println("!ack");
            writer.flush();

            socket.close();

            synchronized (out) {
                out.println("PRIVATE MESSAGE: " + message);
            }

            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
