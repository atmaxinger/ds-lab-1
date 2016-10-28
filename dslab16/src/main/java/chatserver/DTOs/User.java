package chatserver.DTOs;

import java.net.Socket;
import java.util.Objects;

public class User {
    private String _username = "";
    private Socket userSocket;

    public User(String _username, Socket userSocket) {
        this._username = _username;
        this.userSocket = userSocket;
    }

    public String getUserName() {
        return _username;
    }

    public void setUserName(String username) {
        this._username = username;
    }

    public Socket getUserSocket() {
        return userSocket;
    }

    public void setUserSocket(Socket userSocket) {
        this.userSocket = userSocket;
    }
}
