import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class TelegramService {
    private static final Properties prop =  new Properties();

    static{

        try(FileInputStream fis = new FileInputStream("config.properties")){
                prop.load(fis);
        }catch(IOException e){
            System.out.println("설정 파일을 찾을 수 없습니다: config.properties");
            e.printStackTrace();
        }
    }
    private static final String TOKEN = prop.getProperty("telegram.token");
    private static final String CHAT_ID = prop.getProperty("telegram.chat_id");

    public static void sendMessage(String text) {

        if (TOKEN == null || CHAT_ID == null) return;

        new Thread(() -> {
            try {
                String urlString = "https://api.telegram.org/bot" + TOKEN + "/sendMessage";
                // 문자열로 된 주소를 네트워크 통신이 가능한 URL 객체로 생성 (주소 형식 검증 포함)
                URL url = new URL(urlString);
                // 해당 URL을 기반으로 실제 HTTP 통신을 위한 연결 객체(HttpURLConnection)를 생성
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                // 서버로 데이터를 출력(전송)할 수 있는 상태로 설정 (기본값은 false임)
                conn.setDoOutput(true);
                // HTTP 헤더 설정
                conn.setRequestProperty("Content-Type", "application/json");

                // 전송할 데이터 (JSON 형식)
                String json = "{\"chat_id\":\"" + CHAT_ID + "\", \"text\":\"" + text + "\"}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = json.getBytes(StandardCharsets.UTF_8);
                    // 스트림을 통해 실제 바이트 데이터 전송
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