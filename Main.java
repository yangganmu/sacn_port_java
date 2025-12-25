import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.LongAdder;

public class Main {
    private static final int SOCKET_TIMEOUT = 1500;
    private static final int PORT_COUNT = 65536;
    private static final int BATCH_COUNT = 2000;
    private static final Semaphore SEMAPHORE = new Semaphore(BATCH_COUNT);
    private static final LongAdder PROCESSED_COUNT = new LongAdder();
    private static final ArrayList<Integer> OPENED_PORTS = new ArrayList<>(20);

    public static void main(String[] args) {
        final String ipStr;
        if (args.length != 0) {
            ipStr = args[0];
        } else {
            System.out.print("要扫描的IP地址: ");
            ipStr = new Scanner(System.in).nextLine();
        }
        final InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(ipStr);
        } catch (Exception _) {
            System.out.println("找不到该IP地址： " + ipStr);
            System.exit(1);
            return;
        }
        final Thread printThread = Thread.startVirtualThread(Main::printProgress);
        for (int i = 0; i < PORT_COUNT; ++i) {
            try {
                SEMAPHORE.acquire();
            } catch (InterruptedException e) {
                PROCESSED_COUNT.add(1);
                continue;
            }
            final int port = i;
            Thread.startVirtualThread(() -> {
                scanPort(inetAddress, port);
                SEMAPHORE.release();
                PROCESSED_COUNT.add(1);
            });
        }
        try {
            printThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static void printProgress() {
        long processed = 0;
        while (processed < PORT_COUNT) {
            processed = PROCESSED_COUNT.sum();
            System.out.printf("进度:%d/%d (%.2f%c) | 开放端口: %d\r", processed, PORT_COUNT, ((double) processed) / PORT_COUNT * 100, '%', OPENED_PORTS.size());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("进度:" + processed + "/" + PORT_COUNT + " (100.00%) | 开放的端口个数:" + OPENED_PORTS.size());
        System.out.println("开放的端口:" + OPENED_PORTS);
    }

    private static void scanPort(InetAddress inetAddress, int port) {
        try (Socket socket = new Socket()) {
            socket.setReuseAddress(true);
            socket.setTcpNoDelay(true);
            socket.connect(new InetSocketAddress(inetAddress, port), SOCKET_TIMEOUT);
            if (socket.isConnected()) {
                synchronized (OPENED_PORTS) {
                    OPENED_PORTS.add(port);
                }
            }
        } catch (Exception _) {
        }
    }
}
