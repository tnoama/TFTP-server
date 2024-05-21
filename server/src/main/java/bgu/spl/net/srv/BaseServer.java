package bgu.spl.net.srv;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import bgu.spl.net.impl.tftp.ServerInfo;

import bgu.spl.net.api.BidiMessagingProtocol;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import bgu.spl.net.impl.tftp.TftpEncoderDecoder;
import bgu.spl.net.impl.tftp.TftpProtocol;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.io.File;
import java.io.FileWriter;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public abstract class BaseServer<T> implements Server<T> {

    private final int port;
    private final Supplier<BidiMessagingProtocol<T>> protocolFactory;
    private final Supplier<MessageEncoderDecoder<T>> encdecFactory;
    private ServerSocket sock;
    private int clientId = 0;
    private Connections<T> connections;
    private static final String PATH = "./Flies";
    public ServerInfo ServerInfo;

    public BaseServer(
            int port,
            Supplier<BidiMessagingProtocol<T>> protocolFactory,
            Supplier<MessageEncoderDecoder<T>> encdecFactory) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
        this.sock = null;
        this.connections = new ConnectionsImpl<T>();
        this.ServerInfo = new ServerInfo();
        List<String> fileNames = getAllFileNames(PATH);
        for (String file : fileNames) {
            ServerInfo.file_names.add(file);
        }
    }

    public ServerInfo getServerInfo() {
        return this.ServerInfo;
    }

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
            System.out.println("Server started");

            this.sock = serverSock; // just to be able to close

            while (!Thread.currentThread().isInterrupted()) {
                Socket clientSock = serverSock.accept();
                BidiMessagingProtocol<T> p = protocolFactory.get();
                p.start(clientId, connections, ServerInfo);
                BlockingConnectionHandler<T> handler = new BlockingConnectionHandler<>(
                        clientSock,
                        encdecFactory.get(),
                        p);
                connections.connect(clientId, handler, this.ServerInfo);
                clientId++;
                execute(handler);

            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
        if (sock != null)
            sock.close();
    }

    protected abstract void execute(BlockingConnectionHandler<T> handler);

    public static List<String> getAllFileNames(String folderPath) {
        List<String> fileNames = new ArrayList<>();
        File folder = new File(folderPath);

        // Check if the specified path is a directory
        if (folder.exists() && folder.isDirectory()) {
            // Retrieve all files in the directory
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    // Add file names to the list
                    if (file.isFile()) {
                        fileNames.add(file.getName());
                    }
                }
            }
        } else {
            System.out.println("The specified path is not a directory or does not exist.");
        }
        return fileNames;
    }
}