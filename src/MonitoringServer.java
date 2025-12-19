import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class MonitoringServer {
    private static final DatabaseRepository repository = new DatabaseRepository();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // [API] 데이터 수신 (파이썬으로부터)
        server.createContext("/api/metrics", (exchange) -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);


                JSONObject jsonPayload = new JSONObject(body);

                String agentName = jsonPayload.getString("agent_name");
                double cpu = jsonPayload.getDouble("cpu");
                double memory = jsonPayload.getDouble("memory");

                // [여기 수정 3] 중복 호출 삭제 및 변수명 통일 (agentName, cpu, memory)
                repository.save(agentName, cpu, memory);

                repository.save(agentName, cpu, memory); // 이름도 함께 저장!

                repository.save(agentName,cpu, memory); // DB 저장 호출
                exchange.sendResponseHeaders(200, 0);
                exchange.close();
            }
        });

        // [VIEW] 웹 대시보드 표시
        server.createContext("/view", (exchange) -> {
            String response = MonitoringView.render(repository.findAll(10)); // 뷰 렌더링 호출

            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes(StandardCharsets.UTF_8));
            os.close();
        });

        server.start();
        System.out.println("🚀 [INFO] 서버가 시작되었습니다: http://localhost:8080/view");
    }
}