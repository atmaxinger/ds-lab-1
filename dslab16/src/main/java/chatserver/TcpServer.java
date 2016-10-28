package chatserver;

import chatserver.DTOs.User;
import chatserver.Service.ChatService;
import com.sun.corba.se.impl.orbutil.closure.Future;

import javax.swing.text.StyledEditorKit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class TcpServer implements Runnable {
    private ServerSocket serverSocket;
    private Chatserver chatserver;
    private ChatService chatService;

    // This list contains clients sockets that weren't closed by the TcpClientHandler
    // They could however be closed by the ChatService
    private List<Socket> currentlyOpenClientSockets = new LinkedList<>();

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
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String request = "";

                while(!socket.isClosed() && (request = reader.readLine()) != null && !Thread.interrupted()) {
                    String[] parts = request.split(" ");

                    if(user == null) {
                        if(request.startsWith("!login")) {

                            if(parts.length != 3) {
                                // TODO: Error handling
                                chatService.SendWrongNumberOfArgumentsError(socket);
                            }
                            else {
                                String username = parts[1];
                                String pass = parts[2];

                                user = new User(username, socket);

                                boolean success = chatService.LoginUser(user, pass);
                                if (!success) {
                                    // TODO: Error handling
                                    user = null;
                                }
                            }
                        }
                        else {
                            // TODO: What to do if user isnt logged in?
                            chatService.SendNotLoggedInError(socket);
                        }
                    }
                    else {
                        if(request.startsWith("!login")) {
                            chatService.SendAlreadyLoggedInError(user.getUserSocket());
                        }
                        else if (request.startsWith("!logout")) {
                            boolean success = chatService.LogoutUser(user);

                            if(!success) {
                                // TODO: Error handling
                            }
                            else {
                                user = null;
                            }
                        }
                        else if(request.startsWith("!register")) {
                            if(parts.length != 2) {
                                // TODO: error handling
                                chatService.SendWrongNumberOfArgumentsError(socket);
                            }
                            else {
                                chatService.RegisterPrivateAddress(user, parts[1]);
                            }
                        }
                        else if(request.startsWith("!lookup")) {
                            if(parts.length != 2) {
                                // TODO: error handling
                                chatService.SendWrongNumberOfArgumentsError(socket);
                            }
                            else {
                                chatService.LookupPrivateAddress(user, parts[1]);
                            }
                        }
                        else if(request.startsWith("!list")) {
                            chatService.SendAllOnlineUsers(user);
                        }
                        // Test message
                        else if(request.startsWith("!send")) {
                            if(parts.length == 1) {
                                chatService.SendWrongNumberOfArgumentsError(socket);
                            }
                            else {
                                chatService.SendMessageToAllOtherUsers(user, request.substring("!send ".length()));
                            }
                        }
                        else {
                            chatService.SendUnknownCommandError(socket);
                        }
                    }
                }
            }
            catch (SocketException se) {
                // This probably means that we shut down
            }
            catch (IOException e) {
                e.printStackTrace();
            }


            if(!socket.isClosed()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            currentlyOpenClientSockets.remove(socket);
        }
    }

    public TcpServer(ServerSocket serverSocket, Chatserver chatserver) {
        this.serverSocket = serverSocket;
        this.chatserver = chatserver;

        this.chatService = ChatService.getInstance();
    }

    @Override
    public void run() {
        ExecutorService threadPool = Executors.newFixedThreadPool(100);

        while(true) {
            try {
                Socket socket = serverSocket.accept();

                currentlyOpenClientSockets.add(socket);
                TcpClientHandler tch = new TcpClientHandler(socket);
                threadPool.submit(tch);
            }
            catch (SocketException se)
            {
                // This probably means that we shut down
                threadPool.shutdownNow();

                // Go through the list of sockets not closed by the client handler
                // close them if they're still open
                for(Socket s : currentlyOpenClientSockets) {
                    if(!s.isClosed()) {
                        try {
                            s.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
