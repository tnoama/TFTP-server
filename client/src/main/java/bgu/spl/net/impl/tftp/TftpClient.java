package bgu.spl.net.impl.tftp;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.io.*;

public class TftpClient {
    private String PATH = "client";
    private byte[][] dataToSend;
    private byte[] dataToSave;
    private String lastCommand;
    private String nameOfFileToSave;
    private String nameOfFileToSend;
    private boolean shouldTerminate = false;

    public static void main(String[] args) {
        String host = "127.0.0.1"; // Local host
        int port = 7777; // Port number
        TftpClient client = new TftpClient();

        try (Socket socket = new Socket(host, port);
                BufferedReader keyboardReader = new BufferedReader(new InputStreamReader(System.in));
                OutputStream out = socket.getOutputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to server.");

            // Start a thread to listen for server responses
            Thread listeningThread = new Thread(() -> {
                try {
                    InputStream inputStream = socket.getInputStream();
                    byte[] responseBuffer = new byte[1024]; // Adjust the buffer size as needed
                    int bytesRead;
                    while ((bytesRead = inputStream.read(responseBuffer)) != -1 && !client.shouldTerminate) {
                        byte[] responseBytes = Arrays.copyOf(responseBuffer, bytesRead);
                        byte[] ansToServer = client.process(responseBytes);
                        if (ansToServer != null) {
                            byte[] size = new byte[2];
                            size[0] = ansToServer[2];
                            size[1] = ansToServer[3];
                            out.write(ansToServer);
                        }
                        // System.out.println("Server response: " + Arrays.toString(responseBytes));
                        // new String(responseBytes,
                        // StandardCharsets.UTF_8));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            listeningThread.start(); // Start listening thread

            // Start a thread to read input from the keyboard
            Thread keyboardThread = new Thread(() -> {
                try {
                    while (!client.shouldTerminate) {
                        System.out.println("Enter message");
                        String userInput = keyboardReader.readLine();

                        String[] parts = userInput.split(" ", 2);
                        String firstPart = parts[0];
                        String secondPart = "";
                        if (parts.length > 1) {
                            secondPart = parts[1];
                        }
                        client.lastCommand = firstPart;

                        byte[] messageBytes = secondPart.getBytes();
                        byte[] packet = client.convertToPacket(messageBytes, firstPart, secondPart);
                        if (packet == null) {
                            System.out.println("unknown command, please try again");
                        } else {
                            out.write(packet);
                            // out.write('\n'); // Write a newline character to indicate the end of the
                            // message
                            out.flush();
                        }
                        if (client.lastCommand.equals("DISC")) {
                            client.shouldTerminate = true;
                        }
                    }
                    // socket.close();
                    // out.close();
                    // keyboardReader.close();
                    // System.exit(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            keyboardThread.start(); // Start keyboard input thread

            // Wait for the keyboard thread to terminate
            keyboardThread.join();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public byte[] read(String filename) throws IOException {
        Path filePath = Paths.get(PATH, filename);

        File file = filePath.toFile();

        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filename);
        }

        return Files.readAllBytes(filePath);
    }

    public byte[][] prepareDataToSend(byte[] data) {
        short numOfPackets = (short) (data.length / 512);
        short sizeOfLastPacket = (short) (data.length % 512);
        byte[][] ans = new byte[numOfPackets + 1][];
        int index = 0;
        if (numOfPackets > 0) {
            for (short i = 0; i < numOfPackets; i++) {
                byte[] packet = new byte[518];
                packet[0] = 0;
                packet[1] = 3;
                Byte[] size = shortToByte((short) (512));
                packet[2] = size[0];
                packet[3] = size[1];
                Byte[] block = shortToByte((short) (i + 1));
                packet[4] = block[0];
                packet[5] = block[1];
                for (int j = 6; j < packet.length; j++) {
                    packet[j] = data[index];
                    index++;
                }
                // index = 0;
                ans[i] = packet;
            }

        }
        byte[] packet = new byte[sizeOfLastPacket + 6];
        packet[0] = 0;
        packet[1] = 3;
        Byte[] size = shortToByte((short) (sizeOfLastPacket));
        packet[2] = size[0];
        packet[3] = size[1];
        Byte[] block = shortToByte((short) (numOfPackets + 1));
        packet[4] = block[0];
        packet[5] = block[1];
        for (int i = 6; i < packet.length; i++) {
            packet[i] = data[index];
            index++;
        }
        if (numOfPackets > 0) {
            ans[numOfPackets] = packet;
        } else {
            ans[0] = packet;
        }
        return ans;
    }

    public Byte[] shortToByte(short a) {
        Byte[] a_bytes = new Byte[] { (byte) (a >> 8), (byte) (a & 0xff) };
        return a_bytes;
    }

    public byte[] process(byte[] message) {
        int opcode = message[1];
        ///////////// BCAST
        if (opcode == 9) {
            String msg = "BCAST ";
            if (message[2] == 0) {
                msg = msg + "del ";
            } else {
                msg = msg + "add ";
            }
            message = Arrays.copyOfRange(message, 3, message.length - 1);
            msg = msg + byteToString(message);
            System.out.println(msg);
            return null;

        } ///////////// ERROR
        else if (opcode == 5) {
            String msg = "ERROR ";
            byte[] errCode = Arrays.copyOfRange(message, 2, 4);
            short code = byteToShort(errCode);
            msg = msg + code;
            message = Arrays.copyOfRange(message, 3, message.length - 1);
            msg = msg + " " + byteToString(message);
            System.out.println(msg);
            return null;
        } ///////////// ACK
        else if (opcode == 4) {
            short blockNum = byteToShort(Arrays.copyOfRange(message, 2, message.length));
            String toPrint = "ACK " + blockNum;
            System.out.println(toPrint);
            if (lastCommand != null && lastCommand.equals("WRQ") && blockNum < dataToSend.length) {
                return dataToSend[blockNum];
            }
            if (lastCommand != null && lastCommand.equals("WRQ") && blockNum == dataToSend.length) {
                System.out.println("WRQ " + nameOfFileToSend + " completed");
            }

        } ///////////// DATA
        else if (opcode == 3) {
            if (!lastCommand.equals("DIRQ")) {
                byte[] blockByte = new byte[2];
                blockByte[0] = message[4];
                blockByte[1] = message[5];
                short block = byteToShort(blockByte);
                System.out.println("ACK " + block);
                byte[] toSave = prepareDataToSave(message);
                if (dataToSave != null) {
                    dataToSave = mergeArrays(dataToSave, toSave);
                } else {
                    dataToSave = toSave;
                }
                if (message.length < 518) {
                    write(dataToSave, dataToSave.length, nameOfFileToSave);
                    System.out.println("RRQ " + nameOfFileToSave + " completed");
                    dataToSave = null;
                }
                return ACK(block);
            } else {
                List<String> names = new ArrayList<>();

                int start = 6;
                int end = 6;
                for (int i = 6; i < message.length; i++) {
                    if (message[i] == 0) {
                        end = i;
                        names.add(byteToString(Arrays.copyOfRange(message, start, end)));
                        start = i + 1;
                    }

                }
                for (String name : names) {
                    System.out.println(name);
                }
            }

        }
        return null;
    }

    public byte[] ACK(int blockNum) {
        byte[] a = new byte[4];
        a[0] = 0;
        a[1] = 4;
        Byte[] blocks = shortToByte((short) blockNum);
        a[2] = blocks[0];
        a[3] = blocks[1];
        return a;
    }

    public byte[] mergeArrays(byte[] array1, byte[] array2) {
        byte[] mergedArray = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, mergedArray, 0, array1.length);
        System.arraycopy(array2, 0, mergedArray, array1.length, array2.length);
        return mergedArray;
    }

    public byte[] prepareDataToSave(byte[] data) {
        byte[] newData = new byte[data.length - 6];
        for (int i = 0; i < newData.length; i++) {
            newData[i] = data[i + 6];
        }
        return newData;
    }

    public byte[] deleteOpcode(byte[] message) {
        return Arrays.copyOfRange(message, 2, message.length);
    }

    public String byteToString(byte[] message) {
        return new String(message, StandardCharsets.UTF_8).substring(0, message.length);
    }

    public short byteToShort(byte[] b) {
        short b_short = (short) (((short) b[0] & 0xFF) << 8 | (short) (b[1] & 0xFF));
        return b_short;
    }

    private boolean write(byte[] data, int length, String name) {
        try {

            File file = new File(PATH, name);

            // Write the received bytes to the file
            try (FileOutputStream out = new FileOutputStream(file, true)) {
                out.write(data, 0, length);
                dataToSave = null;
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public byte[] convertToPacket(byte[] message, String opcode, String fileName) {
        if (opcode.equals("LOGRQ")) {
            byte[] ans = new byte[message.length + 3];
            ans[0] = 0;
            ans[1] = 7;
            ans[ans.length - 1] = 0;
            int index = 2;
            for (byte b : message) {
                ans[index] = b;
                index++;
            }
            return ans;
        } else if (opcode.equals("DELRQ")) {
            byte[] ans = new byte[message.length + 3];
            ans[0] = 0;
            ans[1] = 8;
            ans[ans.length - 1] = 0;
            int index = 2;
            for (byte b : message) {
                ans[index] = b;
                index++;
            }
            return ans;
        } else if (opcode.equals("RRQ")) {

            if (getAllFileNames(PATH).contains(fileName)) {
                System.out.println("file already exists");
                return null;

            }
            byte[] ans = new byte[message.length + 3];
            ans[0] = 0;
            ans[1] = 1;
            ans[ans.length - 1] = 0;
            int index = 2;
            for (byte b : message) {
                ans[index] = b;
                index++;
            }
            this.nameOfFileToSave = fileName;
            return ans;
        } else if (opcode.equals("WRQ")) { // need to use the read function to send the data
            byte[] ans = new byte[message.length + 3];
            ans[0] = 0;
            ans[1] = 2;
            ans[ans.length - 1] = 0;
            int index = 2;
            for (byte b : message) {
                ans[index] = b;
                index++;
            }
            try {
                byte[] fileContent = read(fileName); // Read the contents of the file
                dataToSend = prepareDataToSend(fileContent); // Prepare data to be sent
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.nameOfFileToSend = fileName;
            return ans;
        } else if (opcode.equals("DIRQ")) {
            byte[] ans = new byte[2];
            ans[0] = 0;
            ans[1] = 6;
            return ans;
        } else if (opcode.equals("DISC")) {
            byte[] ans = new byte[2];
            ans[0] = 0;
            ans[1] = 10;
            return ans;
        } else {
            return null;
        }

    }

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