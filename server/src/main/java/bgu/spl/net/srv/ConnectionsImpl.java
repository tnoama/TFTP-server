package bgu.spl.net.srv;

import bgu.spl.net.srv.BaseServer;
import bgu.spl.net.impl.tftp.ServerInfo;
import java.util.concurrent.ConcurrentHashMap;
import bgu.spl.net.impl.tftp.TftpProtocol;

public class ConnectionsImpl<T> implements Connections<T> {

    private ConcurrentHashMap<Integer, ConnectionHandler<T>> connectionsMap;

    public ConnectionsImpl() {
        this.connectionsMap = new ConcurrentHashMap<>();
    }

    // public ConcurrentHashMap<Integer, ConnectionHandler<T>> getConnectionsMap() {
    // return connectionsMap;
    // }

    @Override
    public void connect(int connectionId, ConnectionHandler<T> handler, ServerInfo serverInfo) {
        if (!connectionsMap.containsKey(connectionId)) {
            connectionsMap.put(connectionId, handler);
            serverInfo.id_logins.put(connectionId, "");
        }
    }

    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = connectionsMap.get(connectionId);
        if (handler != null) {
            handler.send(msg);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void disconnect(int connectionId) {
        if (connectionsMap.containsKey(connectionId)) {
            connectionsMap.remove(connectionId);
        }
    }
}
