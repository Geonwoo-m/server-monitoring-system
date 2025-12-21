import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Collections;

public class DatabaseRepository {
    private static final Properties prop = new Properties();
    static {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            prop.load(fis);
        } catch (IOException e) {
            System.err.println("설정 파일을 찾을 수 없습니다: config.properties");
            e.printStackTrace();
        }
    }

    private static final String URL = prop.getProperty("db.url");
    private static final String USER = prop.getProperty("db.user");
    private static final String PW = prop.getProperty("db.password");

    public long getLastDataSecondsAgo() {
        String sql = "SELECT TIMESTAMPDIFF(SECOND, MAX(recorded_at), NOW()) as diff FROM system_metrics";
        try (Connection conn = DriverManager.getConnection(URL, USER, PW);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                long diff = rs.getLong("diff");
                return rs.wasNull() ? 999 : diff;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return 999;
    }

    public List<MetricSummary> getMetricsSummary(int rangeCount, double groupUnit) {
        List<MetricSummary> list = new ArrayList<>();
        // groupUnit(초) 단위로 데이터를 평균 내어 묶습니다.
        String sql = "SELECT " +
                "  DATE_FORMAT(FROM_UNIXTIME(FLOOR(UNIX_TIMESTAMP(recorded_at) / ?) * ?), '%H:%i:%s') as time_grp, " +
                "  AVG(cpu_usage) as avg_cpu, " +
                "  AVG(memory_usage) as avg_mem " +
                "FROM system_metrics " +
                "WHERE recorded_at >= NOW() - INTERVAL ? SECOND " +
                "GROUP BY time_grp ORDER BY time_grp DESC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PW);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, groupUnit);
            pstmt.setDouble(2, groupUnit);
            pstmt.setInt(3, (int)(rangeCount * groupUnit) + 10); // 여유있게 조회
            pstmt.setInt(4, rangeCount);

            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(new MetricSummary(
                        rs.getString("time_grp"),
                        rs.getDouble("avg_cpu"),
                        rs.getDouble("avg_mem")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        Collections.reverse(list); // 시간순 정렬
        return list;
    }

    public void saveMetric(double cpu, double mem, String agentName) {
        String sql = "INSERT INTO system_metrics (cpu_usage, memory_usage, agent_name) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PW);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, cpu);
            pstmt.setDouble(2, mem);
            pstmt.setString(3, agentName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ DB 저장 오류: " + e.getMessage());
        }
    }

    public static class MetricSummary {
        public String time;
        public double avgCpu, avgMem;
        public MetricSummary(String t, double ac, double am) {
            this.time = t; this.avgCpu = ac; this.avgMem = am;
        }
    }
}