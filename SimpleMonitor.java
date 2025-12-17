import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SimpleMonitor {
    public static void main(String[] args) {
        try {
            while (true) {
                System.out.println("=== CPU 사용량 ===");
                executeCommand("top -bn1 | grep 'Cpu(s)'");

                System.out.println("=== 메모리 사용량 ===");
                executeCommand("free -h");

                // 5초 대기
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[] { "bash", "-c", command });
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
