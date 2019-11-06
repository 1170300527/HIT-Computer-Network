package sr;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;

import static sr.PublicMethod.*;

public class Server {

    private static int port = 7070;

    public static void main(String[] args) throws IOException {
        File sendFile = new File("file//serverSend.pdf");
        File recvFile = new File("file//serverRecv.pdf");
        FileOutputStream fileOutputStream = new FileOutputStream(recvFile);
        System.out.println("receive clientSend file as serverRecv");
        DatagramSocket socket = new DatagramSocket(port);
        socket.setSoTimeout(1000);
        recvByteMethod(fileOutputStream, socket);
        byte[] sendBytes = getByteArray(sendFile).toByteArray();
        System.out.println("send server send file");
        send(socket, sendBytes);
        System.out.println("send succeed");
    }
}
