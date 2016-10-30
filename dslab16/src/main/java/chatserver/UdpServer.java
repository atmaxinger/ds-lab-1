package chatserver;

import chatserver.Service.ChatService;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        ExecutorService threadPool = Executors.newCachedThreadPool();

        byte[] buffer;
        DatagramPacket packet;

        try {
            while (true) {
                buffer = new byte[1024];
                // create a datagram packet of specified length (buffer.length)
                /*
				 * Keep in mind that: in UDP, packet delivery is not
				 * guaranteed,and the order of the delivery/processing is not
				 * guaranteed
				 */
                packet = new DatagramPacket(buffer, buffer.length);

                // / wait for incoming packets from client
                socket.receive(packet);

                UdpClientHandler uch = new UdpClientHandler(packet);
                threadPool.submit(uch);
            }
        } catch (SocketException se) {
            threadPool.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class UdpClientHandler implements Runnable {
        private DatagramPacket packet;

        public UdpClientHandler(DatagramPacket packet) {
            this.packet = packet;
        }

        @Override
        public void run() {
            String request = new String(packet.getData());

            if (request.startsWith("!list")) {
                // get the address of the sender (client) from the received
                // packet
                InetAddress address = packet.getAddress();
                // get the port of the sender from the received packet
                int port = packet.getPort();

                try {
                    ChatService.getInstance().SendOnlineUsersOverUdp(socket, address, port);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
