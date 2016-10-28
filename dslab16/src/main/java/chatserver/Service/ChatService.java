package chatserver.Service;

import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.util.*;

import chatserver.DTOs.User;


public class ChatService {
    private static ChatService instance = new ChatService();
    private ChatService() {
        userList = new LinkedList<>();
        registeredUsers = new HashMap<>();
    }

    public final String ERR_WRONG_USERNAME_PASSWORD = "Wrong username or password.";
    public final String ERR_ALREADY_LOGGED_IN = "Already logged in.";
    public final String ERR_NOT_LOGGED_IN = "Not logged int.";

    public final String INF_SUCCESS_LOGIN = "Successfully logged in.";
    public final String INF_SUCCESS_LOGOUT = "Successfully logged out.";

    public static ChatService getInstance() {
        return instance;
    }

    private List<User> userList;
    private HashMap<String, String> registeredUsers;

    public void setRegisteredUsers(HashMap<String, String> registeredUsers) {
        this.registeredUsers = registeredUsers;
    }

    public boolean LoginUser(User user, String password) throws IOException {
        User oldUser = null;

        synchronized (this) {
            if(!registeredUsers.containsKey(user.getUserName()))  {
                SendViaTcp(user.getUserSocket(), ERR_WRONG_USERNAME_PASSWORD);
                return false;
            }

            if(!registeredUsers.get(user.getUserName()).equals(password)) {
                SendViaTcp(user.getUserSocket(), ERR_WRONG_USERNAME_PASSWORD);
                return false;
            }

            for (User u : userList) {
                if(u.getUserName().equals(user.getUserName())) {
                    oldUser = u;
                    break;
                }
            }

            if(oldUser != null) {
                /*userList.remove(oldUser);
                oldUser.getUserSocket().close();*/

                SendViaTcp(user.getUserSocket(), ERR_ALREADY_LOGGED_IN);
                return false;
            }

             userList.add(user);
        }

        SendViaTcp(user.getUserSocket(), INF_SUCCESS_LOGIN);

        return true;
    }

    public boolean LogoutUser(User user) throws IOException {
        User oldUser = null;

        synchronized (this) {
            for (User u : userList) {
                if(u.getUserName().equals(user.getUserName())) {
                    oldUser = u;
                    break;
                }
            }

            if(oldUser != null) {
                userList.remove(oldUser);

                SendViaTcp(user.getUserSocket(), INF_SUCCESS_LOGOUT);

                oldUser.getUserSocket().close();
            }
            else {
                SendViaTcp(user.getUserSocket(), ERR_NOT_LOGGED_IN);

                return false;
            }
        }

        return true;
    }

    public void SendNotLoggedInError(Socket socket) {
        SendViaTcp(socket, ERR_NOT_LOGGED_IN);
    }

    private void SendViaTcp(Socket socket, String toSend)
    {
        try {
            PrintStream ps = new PrintStream(socket.getOutputStream());
            ps.println(toSend);
            ps.flush();
        } catch (IOException e) {
            synchronized (this) {
                System.out.println("[ERROR] SendViaTcp: IOException");
                e.printStackTrace();
            }
        }
    }

    private void SendMessageViaTcpSocket(Socket socket, User sender, String message) {
        SendViaTcp(socket, sender.getUserName() + ": " + message);
    }

    public void SendMessageToUser(String usernameReceiver, User sender, String message) {
        User user = null;
        synchronized (this) {
            for (User u : userList) {
                if(u.getUserName().equals(usernameReceiver)) {
                    user = u;
                    break;
                }
            }
        }

        if(user != null) {
            SendMessageViaTcpSocket(user.getUserSocket(), sender, message);
        }
        else {
            synchronized (this) {
                System.out.println("[ERROR] SendMessageToUser: No user found (" + usernameReceiver + ")");
            }
        }
    }

    public void SendMessageToAllOtherUsers(User sender, String message) {
        synchronized (this) {
            for (User user : userList) {
                if(user != sender) {
                    SendMessageViaTcpSocket(user.getUserSocket(), sender, message);
                }
            }
        }
    }

    private List<User> getAllOnlineUsers() {
        List<User> onlineUsers = new LinkedList<>();

        synchronized (this) {
            for (User u : userList) {
                if(!u.getUserSocket().isClosed()) {
                    onlineUsers.add(u);
                }
            }
        }

        return onlineUsers;
    }

    private String getAllOnlineUsersFormatted() {
        String msg = "Online users:\n";

        List<User> onlineUsers = getAllOnlineUsers();
        Collections.sort(onlineUsers, new Comparator<User>() {
            @Override
            public int compare(User o1, User o2) {
                return o1.getUserName().compareTo(o2.getUserName());
            }
        });

        for (User u : onlineUsers) {
            msg += "* " + u.getUserName() + "\n";
        }

        return msg;
    }

    private String getAllUsers()
    {
        String message = "";
        int i=1;

        synchronized (this) {
            for(String username : registeredUsers.keySet()) {
                message += Integer.toString(i) + ". " + username;
                boolean found = false;

                for (User u : userList) {
                    if(u.getUserName().equals(username)) {
                        found = true;
                        break;
                    }
                }

                if(found) {
                    message += " online";
                }
                else {
                    message += " offline";
                }

                message += "\n";
                i++;
            }
        }

        return message;
    }

    public void SendAllOnlineUsers(User receiver) {
        String msg = getAllOnlineUsersFormatted();

        SendViaTcp(receiver.getUserSocket(), msg);
    }

    public void SendAllUsers(User receiver) {
        SendViaTcp(receiver.getUserSocket(), getAllUsers());
    }

    public void CloseAllClientSockets() throws IOException {
        synchronized (this) {
            for (User u : userList) {
                if(!u.getUserSocket().isClosed()) {
                    u.getUserSocket().close();
                }
            }

            userList.clear();
        }
    }
}
