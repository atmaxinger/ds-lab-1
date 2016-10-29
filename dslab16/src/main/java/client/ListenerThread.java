package client;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ListenerThread implements Runnable {
    Queue<String> queue;
    Socket socket;
    PrintWriter out;

    BufferedReader serverReader = null;
    String lastMsg = "";


    public ListenerThread(Queue<String> queue, Socket socket, PrintWriter out) throws IOException {
        this.queue = queue;
        this.socket = socket;
        this.out = out;

        // create a reader to retrieve messages send by the server
        serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }


    public String getLastMsg() {
        synchronized (lastMsg) {
            return lastMsg;
        }
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
                    synchronized (lastMsg) {
                        lastMsg = response;
                    }
                    synchronized (out) {
                        out.println(response);
                        out.flush();
                    }
                }
            }
        } catch (SocketException se) {
            // This probably means that we have exited.
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
