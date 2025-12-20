import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TelegramService {
    // 1단계에서 받은 토큰
    private static final String TOKEN = "8430847609:AAFgz3xGocv50RcKQDwdI3JrnyVyrXqEGbg";
    // 2단계에서 받은 숫자 ID
    private static final String CHAT_ID = "8584125048";

    public static void sendMessage(String text) {
        // 서버 성능에 영향을 주지 않도록 별도 스레드(비동기)로 발송
        new Thread(() -> {
            try {
                String urlString = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                // 전송할 데이터 (JSON 형식)
                String json = "{\"chat_id\":\"" + CHAT_ID + "\", \"text\":\"" + text + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                if (conn.getResponseCode() == 200) {
                    System.out.println("📢 텔레그램 메시지 발송 성공!");
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}