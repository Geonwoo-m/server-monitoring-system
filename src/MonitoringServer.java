import com.sun.net.httpserver.HttpServer;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.stream.Collectors;

public class MonitoringServer {
    private static final DatabaseRepository repository = new DatabaseRepository();

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // [기능 1] 연결 상태 감시 스레드 (연결됨/끊김 알림)
        new Thread(() -> {
            boolean isConnected = false;
            while (true) {
                try {
                    Thread.sleep(5000); // 5초마다 체크
                    long lastSeen = repository.getLastDataSecondsAgo();

                    // 데이터가 15초 이내에 들어왔는데, 이전에 끊김 상태였다면? -> 연결됨!
                    if (lastSeen < 15 && !isConnected) {
                        isConnected = true;
                        TelegramService.sendMessage("✅ [알림] 에이전트가 서버에 연결되었습니다.");
                    }
                    // 데이터가 30초 이상 안 들어오는데, 이전에 연결 상태였다면? -> 끊김!
                    else if (lastSeen >= 30 && isConnected) {
                        isConnected = false;
                        TelegramService.sendMessage("🚨 [경고] 에이전트 연결이 끊어졌습니다! (데이터 수집 중단)");
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        }).start();

        // API: 실시간 데이터 저장 및 CPU 임계치 알림
        server.createContext("/api/metrics", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
                    String body = reader.lines().collect(Collectors.joining());
                    JSONObject json = new JSONObject(body);

                    double cpu = json.getDouble("cpu");
                    double mem = json.getDouble("mem");

                    String agentName = json.optString("agent_name", "Unknown-Agent");

                    // DB 저장
                    repository.saveMetric(cpu, mem, agentName);

                    // CPU가 80% 이상일 때 알림 (조건은 원하는 대로 수정 가능)
                    if (cpu >= 80.0) {
                        String message = String.format("⚠️ [과부하] CPU 사용량 급증!\n- CPU: %.1f%%\n- MEM: %.1f%%", cpu, mem);
                        TelegramService.sendMessage(message);
                    }

                    exchange.sendResponseHeaders(200, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                    exchange.sendResponseHeaders(400, 0);
                }
            }
            exchange.getResponseBody().close();
        });

        // API: 히스토리 데이터 조회
        server.createContext("/api/history", exchange -> {
            try {
                String query = exchange.getRequestURI().getQuery();
                int range = 15;
                double unit = 1.0;

                if (query != null) {
                    for (String param : query.split("&")) {
                        String[] pair = param.split("=");
                        if (pair.length == 2) {
                            if (pair[0].equals("range")) range = Integer.parseInt(pair[1]);
                            if (pair[0].equals("unit")) unit = Double.parseDouble(pair[1]);
                        }
                    }
                }

                var historyList = repository.getMetricsSummary(range, unit);
                JSONArray jsonArray = new JSONArray();
                for (var s : historyList) {
                    JSONObject obj = new JSONObject();
                    obj.put("time", s.time);
                    obj.put("avgCpu", s.avgCpu);
                    obj.put("avgMem", s.avgMem);
                    jsonArray.put(obj);
                }

                JSONObject responseObj = new JSONObject();
                responseObj.put("history", jsonArray);
                responseObj.put("lastSeen", repository.getLastDataSecondsAgo());

                byte[] response = responseObj.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } catch (Exception e) { e.printStackTrace(); }
            exchange.getResponseBody().close();
        });

        // 웹 페이지 서빙
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.equals("/index.html")) path = "/monitoring.html";
            File file = new File("resources" + path);
            if (file.exists()) {
                byte[] response = Files.readAllBytes(file.toPath());
                if (path.endsWith(".html")) exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } else { exchange.sendResponseHeaders(404, 0); }
            exchange.getResponseBody().close();
        });

        server.start();
        System.out.println("🚀 모니터링 서버 시작: http://localhost:8080");
    }
}