package chatserver;

import chatserver.DTOs.User;
import chatserver.Service.ChatService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
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

            User user = null;

            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));

                String request = "";

                while(!socket.isClosed() && (request = reader.readLine()) != null) {
                    if(user == null) {
                        if(request.startsWith("!login")) {
                            String[] parts = request.split(" ");

                            if(parts.length != 3) {
                                // TODO: Error handling
                            }

                            String username = parts[1];
                            String pass = parts[2];

                            user = new User(username, socket);

                            boolean success = chatService.LoginUser(user, pass);
                            if(!success) {
                                // TODO: Error handling
                                user = null;
                            }
                        }
                        else {
                            // TODO: What to do if user isnt logged in?
                            chatService.SendNotLoggedInError(socket);
                        }
                    }
                    else {
                        if (request.startsWith("!logout")) {
                            boolean success = chatService.LogoutUser(user);

                            if(!success) {
                                // TODO: Error handling
                            }
                            else {
                                user = null;
                            }
                        }
                        // Test message
                        else {
                            chatService.SendMessageToAllOtherUsers(user, request);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
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
