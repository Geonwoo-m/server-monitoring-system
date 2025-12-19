import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;
import java.net.InetSocketAddress;

public class SimpleHttpServer {

    public static void main(String[] args) throws Exception {

        // 1. 8080 포트로 HTTP 서버 생성
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // 2. /status 경로 등록
        server.createContext("/status", new StatusHandler());

        // 3. 서버 시작
        server.setExecutor(null);
        server.start();

        System.out.println("HTTP 서버 시작됨: http://localhost:8080/status");
    }

    static class StatusHandler implements HttpHandler {

        @Override
        public void handle(HttpExchange exchange) {

            try {
                // CPU, 메모리 정보 가져오기
                OperatingSystemMXBean os =
                        (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

                double cpuLoad = os.getSystemCpuLoad() * 100;
                long totalMem = os.getTotalPhysicalMemorySize();
                long freeMem = os.getFreePhysicalMemorySize();
                long usedMem = totalMem - freeMem;

                // JSON 형태로 응답
                String response = String.format(
                        "{\"cpu\": %.2f, \"memory\": %.2f}",
                        cpuLoad,
                        (usedMem * 100.0 / totalMem)
                );

                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.getBytes().length);

                OutputStream osOut = exchange.getResponseBody();
                osOut.write(response.getBytes());
                osOut.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
