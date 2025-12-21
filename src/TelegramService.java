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