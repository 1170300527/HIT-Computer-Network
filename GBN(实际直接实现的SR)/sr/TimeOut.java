package sr;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Map;

public class TimeOut extends Thread {

    private Map<Integer, Integer> ackMap;

    private DatagramSocket socket;

    private DatagramPacket packet;

    private int sequence;

    public TimeOut(Map<Integer, Integer> ackMap, DatagramSocket socket, DatagramPacket packet, int sequence) {
        this.ackMap = ackMap;
        this.socket = socket;
        this.packet = packet;
        this.sequence = sequence;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (ackMap.get(sequence) == null || ackMap.get(sequence) == 1)
            return;
        if (ackMap.get(sequence) == -1) { //收到的都remove了
            try {
                System.err.println("重新发送了：" + sequence);
                socket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
            new TimeOut(ackMap, socket, packet, sequence).start();
        }
    }
}
