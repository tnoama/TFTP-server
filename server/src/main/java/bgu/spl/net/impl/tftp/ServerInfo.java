package bgu.spl.net.impl.tftp;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ServerInfo {
    public ConcurrentHashMap<Integer, String> id_logins;
    public ConcurrentLinkedDeque<String> file_names;

    public ServerInfo() {
        this.id_logins = new ConcurrentHashMap<Integer, String>();
        this.file_names = new ConcurrentLinkedDeque<String>();
    }
}
