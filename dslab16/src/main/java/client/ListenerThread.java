package client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ListenerThread implements Runnable {
    Queue<String> queue;
    Socket socket;
    PrintWriter out;

    BufferedReader serverReader = null;


    public ListenerThread(Queue<String> queue, Socket socket, PrintWriter out) throws IOException {
        this.queue = queue;
        this.socket = socket;
        this.out = out;

        // create a reader to retrieve messages send by the server
        serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }


    @Override
    public void run() {
        String response = "";
        try {
            while (!socket.isClosed() && (response = serverReader.readLine()) != null)
            {
                if(response.startsWith("!response")) {
                    queue.add(response);
                }
                else {
                    synchronized (out) {
                        out.println(response);
                        out.flush();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
