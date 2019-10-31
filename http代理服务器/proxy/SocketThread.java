package proxy;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SocketThread extends Thread {

  private Socket socket;
  private Socket proxySocket;
  public static Map<String, List<Byte>> cacheMap = new HashMap<String, List<Byte>>();

  public static Map<String, String> lastTimeMap = new HashMap<String, String>();

  private String lastTime;

  private boolean flag = false;

  public SocketThread(Socket socket) {
    // TODO Auto-generated constructor stub
    this.socket = socket;
  }

  @Override
  public void run() {
    // TODO Auto-generated method stub
    try (InputStream inputClient = socket.getInputStream();
        OutputStream outputClient = socket.getOutputStream()) {
      Scanner scanner = new Scanner(inputClient);
      ReadScanner reader = new ReadScanner(scanner);
      String head = reader.getHead();
      String host = reader.getHost();
      String type = reader.getType();
      int port = reader.getPort();
      if (head.contains("google") || head.contains("microsoft")) {
        return;
      }
//      String inetAdress = socket.getInetAddress().toString();
//      if (inetAdress.equals("/127.0.0.1")) {
//        return;
//      }
      if (host.equals("jwes.hit.edu.cn")) {
        return;
      }
      if (host.equals("jwts.hit.edu.cn")) {
        host = "people.com.cn";
        head = "GET http://people.com.cn/ HTTP/1.1\r\n" + "Host: people.com.cn\r\n"
            + "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/70.0.3538.102 Safari/537.36 Edge/18.18362\r\n"
            + "Proxy-Connection: Keep-Alive\r\n\r\n\r\n";
      }
      if (cacheMap.containsKey(host) && lastTimeMap.containsKey(host)) {
        lastTime = lastTimeMap.get(host);
        head = head.trim() + "\r\nIf-Modified-Since: " + lastTimeMap.get(host) + "\r\n\r\n";
        flag = true;
      }
      proxySocket = new Socket(host, port);
      InputStream proxyInput = proxySocket.getInputStream();
      OutputStream proxyOutput = proxySocket.getOutputStream();
      if ("CONNECT".equalsIgnoreCase(type)) {
        port = 443;
        outputClient.write("HTTP/1.1 200 Connection Established\r\n\r\n".getBytes());
        outputClient.flush();
        new ProxyHandleThread(inputClient, proxyOutput).start();
        while (true) {
          outputClient.write(proxyInput.read());
        }
      } else {
        proxyOutput.write(head.getBytes());
        new ProxyHandleThread(inputClient, proxyOutput).start();
        List<Byte> newCache = new ArrayList<Byte>();
        int i = 0;
        while (true) {
//          outputClient.write(proxyInput.read());
          byte b = (byte) proxyInput.read();
          newCache.add(b);
          i++;
          if (i == 1000) {
            byte[] hd = new byte[1000];
            for (int j = 0; j < 1000; j++) {
              hd[j] = (byte) newCache.get(j);
            }
            String hdString = new String(hd);
            if (flag) {
              if (hdString.contains("304 Not Modified")) {
                List<Byte> oldCacheList = new ArrayList<Byte>();
                oldCacheList = cacheMap.get(host);
                byte[] oldCache = new byte[oldCacheList.size()];
                for (int j = 0; j < oldCacheList.size(); j++) {
                  oldCache[j] = (byte) oldCacheList.get(j);
                }
                System.out.println("缓存长度：" + oldCache.length);
                outputClient.write(oldCache);
                outputClient.flush();
                return;
              }
            }
            String[] tmpStrings = hdString.split("\r\n");
            for (String string : tmpStrings) {
              if (string.contains("Last-Modified")) {
                lastTime = string.substring(string.indexOf(" ") + 1);
                lastTimeMap.put(host, lastTime);
                cacheMap.put(host, newCache);
              }
            }
            outputClient.write(hd);
            outputClient.flush();
          }
          if (i > 1000) {
            outputClient.write(b);
          }

        }
      }

    } catch (Exception e) {
      // TODO: handle exception
//      e.printStackTrace();
    } finally {
      try {
        if (socket != null && !socket.isClosed()) {
          socket.close();
        }
        if (proxySocket != null && !proxySocket.isClosed()) {
          proxySocket.close();
        }
      } catch (IOException e) {
//        e.printStackTrace();
      }

    }
  }
}

class ProxyHandleThread extends Thread {

  private InputStream input;
  private OutputStream output;

  public ProxyHandleThread(InputStream input, OutputStream output) {
    this.input = input;
    this.output = output;
  }

  @Override
  public void run() {
    try {
      while (true) {
        output.write(input.read());
      }
    } catch (IOException e) {
//      e.printStackTrace();
    }
  }
}