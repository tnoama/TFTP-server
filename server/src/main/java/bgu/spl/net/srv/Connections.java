package bgu.spl.net.srv;

import java.util.concurrent.ConcurrentHashMap;
import bgu.spl.net.impl.tftp.ServerInfo;
import java.io.IOException;

public interface Connections<T> {

    // ConcurrentHashMap<Integer, ConnectionHandler<T>> getConnectionsMap();

    void connect(int connectionId, ConnectionHandler<T> handler, ServerInfo serverInfo);

    boolean send(int connectionId, T msg);

    void disconnect(int connectionId);

    // void addToQueue(T msg);
}
