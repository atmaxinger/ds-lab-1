package chatserver;

import java.net.DatagramSocket;

/**
 * Created by Maximilian on 28.10.2016.
 */
public class UdpServer implements Runnable {
    private DatagramSocket socket;

    public UdpServer(DatagramSocket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

    }
}
