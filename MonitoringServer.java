import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class MonitoringServer {
    // 파이썬이 보내준 최신 데이터를 저장할 변수
    private static String lastData = "아직 데이터가 들어오지 않았습니다.";

    public static void main(String[] args) throws Exception {
        // 1. 8080 포트로 서버 시작
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // 2. 파이썬 에이전트가 데이터를 보내는 통로 (POST 방식)
        server.createContext("/api/metrics", (exchange) -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                lastData = body; // 받은 데이터를 변수에 저장
                System.out.println("리눅스에서 받은 데이터: " + body);

                exchange.sendResponseHeaders(200, 0);
                exchange.close();
            }
        });

        // 3. 내가 크롬에서 확인할 통로 (GET 방식)
        server.createContext("/view", (exchange) -> {
            // 크롬에 보여줄 HTML 화면
            String response = "<html><body>" +
                    "<h1>Server Monitoring System</h1>" +
                    "<p><b>Latest Data from Fedora:</b> " + lastData + "</p>" +
                    "<script>setTimeout(function(){location.reload();}, 2000);</script>" + // 2초마다 자동 새로고침
                    "</body></html>";

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        });

        server.setExecutor(null);
        server.start();
        System.out.println("자바 서버 시작됨! 크롬에서 http://localhost:8080/view 접속 가능");
    }
}