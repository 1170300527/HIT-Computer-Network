package sr;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PublicMethod {

    private static int Length = 1024;

    private static int sendWindows = 16;

    private static int recvWindows = 16;

    private static double sendLoss = 0.1;

    private static double recvLoss = 0.05;

    private static InetAddress sendAddress;

    private static int sendPort;

    public static ByteArrayOutputStream getByteArray(File file) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(file);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = fileInputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result;
    }

    public static void send(DatagramSocket socket, byte[] sendBytes, InetAddress address, int targetPort) throws IOException {
        int number = sendBytes.length / Length;
        int last = sendBytes.length % Length;
        Map<Integer, Integer> ackMap = new ConcurrentHashMap<>(); //一定要用线程安全的，多线程调用，hashmap线程不安全
        int base = 0;
        int next = 0;
        byte[] bytes;
        RecvACK recvACK = new RecvACK(socket, ackMap);
        recvACK.start(); //开始接收ack
        while (next <= number || ackMap.size() != 0) {
            int oldBase = base;
            int oldSize = ackMap.size();
            for (int i = 0; i < oldSize; i++) { //滑动窗口
                Integer now = (oldBase + i) % 256;
                if (ackMap.get(now) == null)
                    System.out.println("出现了null: " + now);
                if (ackMap.get(now) == -1)
                    break;
                base = (base + 1) % 256;
                ackMap.remove(now);
            }
//            System.out.println(ackMap);
            int canSend = sendWindows - ackMap.size();
            int ackNumber = (base + ackMap.size()) % 256; //最大长度
            for (int i = 0; i < canSend; i++) {
                if (next > number)
                    break;
                int sequence = (ackNumber + i) % 256;
                int sendLength = next < number ? Length : last;
                int end = next < number ? 1 : 0;
                bytes = new byte[sendLength + 4];
                bytes[0] = (byte) ((sendLength >> 8) & 0xff);
                bytes[1] = (byte) (sendLength & 0xff);
                bytes[2] = (byte) sequence;
                bytes[sendLength + 3] = (byte) end;
                for (int j = 3; j < sendLength + 3; j++) {
                    bytes[j] = sendBytes[next * Length + j - 3];
                }
                DatagramPacket datagramPacket = new DatagramPacket(bytes, sendLength + 4, address, targetPort);
                if (Math.random() > sendLoss) {
                    System.out.println("发送了： " + sequence);
                    socket.send(datagramPacket);
                }
                next++;
                ackMap.put(sequence, -1);
                new TimeOut(ackMap, socket, datagramPacket, sequence).start();
            }
        }
        return;
    }

    public static byte[] receive(DatagramSocket socket) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] recvBytes = new byte[1028];
        int length;
        int base = 0;
        Map<Integer, byte[]> cache = new HashMap<>();
        int end = 1;
        while (true) {
            DatagramPacket packet = new DatagramPacket(recvBytes, recvBytes.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
                System.out.println("接收完成");
                break;
            }
            length = recvBytes[0] & 0xff;
            length = length << 8 | recvBytes[1] & 0xff;
            int sequence = recvBytes[2] & 0xff;
            InetAddress address = packet.getAddress();
            int port = packet.getPort();
            sendAddress = address;
            sendPort = port;
            byte[] recvAck = new byte[2];
            recvAck[0] = recvBytes[2];
            recvAck[1] = recvBytes[length + 3];
            byte[] newArray = Arrays.copyOfRange(recvBytes, 3, 3 + length);
            int old1 = (base - recvWindows + 256) % 256;
            int old2 = (base - 1 + 256) % 256;
            int new1 = base;
            int new2 = (base + recvWindows - 1) % 256;
            if (((old1 > old2) && (sequence >= old1 || sequence <= old2)) || (sequence >= old1 && sequence <= old2)) {
                DatagramPacket ackPocket = new DatagramPacket(recvAck, recvAck.length, address, port);  //发送ACK
                socket.send(ackPocket);
                System.out.println("收到了：" + sequence);
                continue;
            }
            if (((new1 > new2) && (sequence >= new1 || sequence <= new2)) || (sequence >= new1 && sequence <= new2)) {
                if (sequence == base) { //连续的滑动接收窗口
                    byteArrayOutputStream.write(newArray, 0, newArray.length);
                    base = (base + 1) % 256;
                    while (cache.get(base) != null) {
                        byteArrayOutputStream.write(cache.get(base), 0, cache.get(base).length);
                        cache.remove(base); //不remove也行，下次到这个序列号就更新了
                        base = (base + 1) % 256;
                    }
                } else {
                    cache.put(sequence, newArray); //不连续的加入缓存
                }
                DatagramPacket ackPocket = new DatagramPacket(recvAck, recvAck.length, address, port);  //发送ACK
                if (Math.random() > recvLoss)
                    socket.send(ackPocket);
                System.out.println("收到了：" + sequence);
            }
            if ((recvBytes[length + 3] & 0xff) == 0)
                end = 0;
//            if (end == 0 && cache.size() == 0) { //当收到中止字符0并且没有缓存时退出(全接收但发送的ack丢失此时关闭无法接收重发返回ack)
//                System.out.println("stop");
//                break;
//            }
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static void send(DatagramSocket socket, byte[] sendBytes) throws IOException { //server端重载send方法，其中address和port已获得
        send(socket, sendBytes, sendAddress, sendPort);
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
