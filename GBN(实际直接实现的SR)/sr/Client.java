package sr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;

import static sr.PublicMethod.*;


public class Client {

    private static String host = "localhost";

    private static int ownPort = 8080;

    private static int targetPort = 7070;

    public static void main(String[] args) throws IOException {
        File sendFile = new File("file//clientSend.pdf");
        File recvFile = new File("file//clientRecv.pdf");
        FileOutputStream fileOutputStream = new FileOutputStream(recvFile);
        byte[] sendBytes = getByteArray(sendFile).toByteArray();
        System.out.println("send clientSend file");
        DatagramSocket socket = new DatagramSocket(ownPort);
        socket.setSoTimeout(1000);
        InetAddress address = InetAddress.getByName(host);
        send(socket, sendBytes, address, targetPort);
        System.out.println("send succeed");
        recvByteMethod(fileOutputStream, socket);
    }

    static void recvByteMethod(FileOutputStream fileOutputStream, DatagramSocket socket) throws IOException {
        byte[] recvBytes;
        while (true) { //使其一直处于接收状态
            recvBytes = receive(socket);
            if (recvBytes.length != 0) {
                System.out.println(recvBytes.length);
                fileOutputStream.write(recvBytes);
                break;
            }
        }
        System.out.println("receive succeed");
    }
}
