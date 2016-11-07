package client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Queue;

public class PublicListenerThread implements Runnable {
    private Queue<String> queue;
    private Socket socket;
    private PrintWriter userWriter;

    private BufferedReader serverReader = null;
    private String lastMsg = "";


    public PublicListenerThread(Queue<String> queue, Socket socket, PrintWriter userWriter) throws IOException {
        this.queue = queue;
        this.socket = socket;
        this.userWriter = userWriter;

        // create a reader to retrieve messages send by the server
        serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }


    public String getLastMsg() {
        synchronized (lastMsg) {

            if(lastMsg == null || lastMsg.isEmpty()) {
                return "No message received!";
            }

            return lastMsg;
        }
    }

    @Override
    public void run() {
        String response = "";
        try {
            while (!socket.isClosed() && (response = serverReader.readLine()) != null) {
                if (response.startsWith("!response")) {
                    synchronized (queue) {
                        queue.add(response);
                        queue.notify();
                    }
                } else {
                    synchronized (lastMsg) {
                        lastMsg = response;
                    }
                    synchronized (userWriter) {
                        userWriter.println(response);
                        userWriter.flush();
                    }
                }
            }
        } catch (SocketException se) {
            // This probably means that we have exited.
        } catch (IOException e) {
            e.printStackTrace();
        }

        synchronized (queue) {
            queue.notify();
        }

        if(!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
