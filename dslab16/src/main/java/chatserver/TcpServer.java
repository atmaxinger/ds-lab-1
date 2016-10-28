package chatserver;

import chatserver.DTOs.User;
import chatserver.Service.ChatService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class TcpServer implements Runnable {
    private ServerSocket serverSocket;
    private Chatserver chatserver;
    private ChatService chatService;

    private class TcpClientHandler implements Runnable {
        private Socket socket;

        public TcpClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            System.out.println("[INFO] CLIENT CONNECTED: " + socket.getInetAddress().getHostAddress());


        }
    }

    public TcpServer(ServerSocket serverSocket, Chatserver chatserver) {
        this.serverSocket = serverSocket;
        this.chatserver = chatserver;

        this.chatService = ChatService.getInstance();
    }



    @Override
    public void run() {
        while(true) {
            try {
                Socket socket = serverSocket.accept();

                TcpClientHandler tch = new TcpClientHandler(socket);
                Thread t = new Thread(tch);
                t.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
