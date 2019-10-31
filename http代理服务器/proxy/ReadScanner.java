package proxy;

import java.util.Scanner;

public class ReadScanner {

  private String host;
  private int port = 80;
  private String type;
  private String head;
  
  public ReadScanner(Scanner scanner) {
    // TODO Auto-generated constructor stub
    StringBuilder headString = new StringBuilder();
    String line;
    try {
      while (scanner.hasNextLine()) {
        line = scanner.nextLine();
        headString.append(line + "\r\n");
        if (line.length() == 0) {
          break;
        }
        String[] temp = line.split(" ");
        if (temp[0].contains("Host")) {
          String[] hostTemp = temp[1].split(":");
          host = hostTemp[0];
          if (hostTemp.length > 1) {
            port = Integer.parseInt(hostTemp[1]);
          }
        }
      }
    } catch (Exception e) {
      // TODO: handle exception
      e.printStackTrace();
    }
    head = headString.toString();
    if (headString.length() > 0) {      
      type = head.substring(0, head.indexOf(" "));
    }
  }

  public String getHead() {
    return head;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getType() {
    return type;
  }
}
