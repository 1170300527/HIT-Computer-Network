package proxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {

  public Server() {
    // TODO Auto-generated constructor stub
  }

  public static void main(String[] args) throws IOException {
    
    ServerSocket server;
    Socket socket = null;
    try {
      server = new ServerSocket(8080);
      int i = 0;
      while (true) {
        socket = server.accept();
        i++;
//        System.out.println("启动第 " + i + " 个线程");
        new SocketThread(socket).start();;
      }
    } catch (Exception e) {
      // TODO: handle exception
      if (socket != null) {
        socket.close();
      }
      e.printStackTrace();
    }
  }
}
