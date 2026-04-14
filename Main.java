import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    private static final int SOCKET_TIMEOUT = 1500;
    private static final int PORT_COUNT = 65536;
    private static final int CONCURRENCY = 2048;
    private static final ArrayList<Integer> OPENED_PORTS = new ArrayList<>(16);
    private static final AtomicInteger PROCESSED_COUNT = new AtomicInteger(0);

    public static void main(String[] args) {
        final String ipStr;
        if (args.length != 0) {
            ipStr = args[0].trim();
        } else {
            System.out.print("要扫描的IP地址: ");
            try (Scanner scanner = new Scanner(System.in)) {
                ipStr = scanner.nextLine().trim();
            }
        }
        final InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(ipStr);
        } catch (Exception _) {
            System.out.println("找不到该IP地址： " + ipStr);
            System.exit(1);
            return;
        }
        for (int i = 0; i < CONCURRENCY; i++) {
            Thread.startVirtualThread(() -> {
                int port;
                while (PROCESSED_COUNT.get() < PORT_COUNT) {
                    port = PROCESSED_COUNT.getAndIncrement();
                    if (port > PORT_COUNT) {
                        break;
                    }
                    try (Socket socket = new Socket()) {
                        socket.setSoLinger(true, 0);
                        socket.connect(new InetSocketAddress(inetAddress, port), SOCKET_TIMEOUT);
                        synchronized (OPENED_PORTS) {
                            OPENED_PORTS.add(port);
                        }
                    } catch (Exception _) {

                    }
                }
            });
        }
        int printProcessed = 0;
        while (printProcessed < PORT_COUNT) {
            printProcessed = PROCESSED_COUNT.get();
            System.out.print("\r进度: " + printProcessed + "/" + PORT_COUNT + " (" + ((printProcessed * 100) >> 16) + "%) | 开放端口数: " + OPENED_PORTS.size());
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        OPENED_PORTS.sort(Integer::compareTo);
        System.out.println("\n开放的端口:" + OPENED_PORTS);
    }
}
