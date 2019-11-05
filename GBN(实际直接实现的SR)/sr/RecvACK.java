package sr;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;

public class RecvACK extends Thread {

    private DatagramSocket socket;

    private Map<Integer, Integer> ackMap;

    public RecvACK(DatagramSocket socket, Map<Integer, Integer> ackMap) {
        this.socket = socket;
        this.ackMap = ackMap;
    }

    @Override
    public void run() {
        int end = 1;
        while (true) {
            //当收到中止，并且没有等待接收ack的缓存时停止（偶尔能用，按序接到最后一个但此时map.size不是0，之后就阻塞了）找了半天原因
            if (end == 0 && ackMap.size() == 0)
                break;
            byte[] ackByte = new byte[2];
            DatagramPacket packet = new DatagramPacket(ackByte, ackByte.length);
            try {
                socket.receive(packet);
            } catch (IOException e) {
//                e.printStackTrace();
                //1s内接收不到认为停止
                System.out.println("接收完成");
            }
            int ack = 0xff & ackByte[0];
            if ((0xff & ackByte[1]) == 0)
                end = 0;
            if (ackMap.get(ack) != null) {
                ackMap.put(ack, 1);
            }
            System.out.println("收到ack: " + ack);
            if (end == 0 && ackMap.size() == 0) //当收到中止，并且没有等待接收ack的缓存时停止
                break;
        }
    }
}
