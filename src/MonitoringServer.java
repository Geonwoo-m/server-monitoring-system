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
            try {        //getRequestURI(): URI 주소를 모두 가져옴    getQuery(): ?이후의 값을 가져옴
                String query = exchange.getRequestURI().getQuery();
                int range = 15;
                double unit = 1.0;

                if (query != null) {
                    //만약 range=15&unit=1이라면
                    // 1. split("&")은 ["range=15", "unit=1"] 이라는 '임시배열'을 생성
                    // 2. [첫 번째 루프] param은 임시배열의 인덱스 0인 값을 가져옴
                    // 5. [두 번째 루프] "range=15"의 값이 "unit=1"로 덮어씌워짐
                    for (String param : query.split("&")) {
                        // 3. pair은 = 기준으로 데이터를 나눔 / ex) "range=15" -> pair[0] = "range" pair[1] = "15"
                        String[] pair = param.split("=");
                        // 4. 아래 if문으로 검사하여 range와 unit값 변경
                        if (pair.length == 2) {
                            if (pair[0].equals("range")) range = Integer.parseInt(pair[1]);
                            if (pair[0].equals("unit")) unit = Double.parseDouble(pair[1]);
                        }
                    }
                }
                // DB에서 요약된 데이터 리스트를 가져옴
                var historyList = repository.getMetricsSummary(range, unit);
                JSONArray jsonArray = new JSONArray();
                //리스트에 있는 개수만큼 반복
                for (var s : historyList) {
                    // 개별 데이터를 담기 위해 Map처럼 키:값 구조를 가진 JSON 객체 생성
                    JSONObject obj = new JSONObject();
                    // 각 항목에 이름을 붙여서 데이터를 채움
                    obj.put("time", s.time);
                    obj.put("avgCpu", s.avgCpu);
                    obj.put("avgMem", s.avgMem);
                    // 배열 형태로 데이터가 쌓임
                    jsonArray.put(obj);
                }

                JSONObject responseObj = new JSONObject();
                //위에서 정리한 jsonArray 배열을 history라는 이름으로 저장함
                responseObj.put("history", jsonArray);
                //지금으로부터 몇 초 전의 데이터인지 lastSeen에 저장
                responseObj.put("lastSeen", repository.getLastDataSecondsAgo());
                // toString()으로 문자열로 바꾸고, 네트워크 전송이 가능한 바이트로 변환하여 response 배열에 저장
                byte[] response = responseObj.toString().getBytes(StandardCharsets.UTF_8);
                //네트워크 패킷의 헤더 부분에 데이터 형식이 JSON임을 명시
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                // 헤더에 상태코드 200과 데이터 크기를 실어서 보냄 ** 여기서 200은 응답상태 성공을 뜻함
                exchange.sendResponseHeaders(200, response.length);
                //실제 데이터인 Payload를 전송.
                exchange.getResponseBody().write(response);
            } catch (Exception e) { e.printStackTrace(); }
            //데이터 전송 끝 , Stream을 닫고 자원을 반납
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