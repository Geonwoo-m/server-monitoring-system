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
        //가장 최근 데이터 시간과 현재 시간의 차이를 구하는 쿼리
        String sql = "SELECT TIMESTAMPDIFF(SECOND, MAX(recorded_at), NOW()) as diff FROM system_metrics";
        try (Connection conn = DriverManager.getConnection(URL, USER, PW);
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                long diff = rs.getLong("diff");
                return rs.wasNull() ? 999 : diff; // 데이터가 없으면 999를 주고 있으면 diff 줌
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
             //DB 접속
        try (Connection conn = DriverManager.getConnection(URL, USER, PW);
             //위에서 작성한 sql문을 pstmt 객체에 담아 실행 준비
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // 위에 sql문에 ? 부분에 데이터를 삽입
            pstmt.setDouble(1, groupUnit);
            pstmt.setDouble(2, groupUnit);
            pstmt.setInt(3, (int)(rangeCount * groupUnit) + 10); // 여유있게 조회
            pstmt.setInt(4, rangeCount);
            //sql문을 실행시킨후 결과값을 rs에 저장 ( 이때 rs 객체 안에는 데이터가 그냥 들어있는것이 아닌 엑셀 시트처럼 들어있음)
            ResultSet rs = pstmt.executeQuery();
                   //다음 줄에 데이터가 있으면 true 반환
            while (rs.next()) {
                //rs에서 읽은 데이터를 MetricSummary 객체에 담고, 그 객체를 리스트에 추가함
                list.add(new MetricSummary(
                        rs.getString("time_grp"),
                        rs.getDouble("avg_cpu"),
                        rs.getDouble("avg_mem")
                ));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        //위에 sql문에서  ORDER BY time_grp DESC를 써서 최신순으로 가져왔으므로, 리스트 순서가 최신 -> 과거 순
        //그래프는 과거 -> 최신 순으로 그려야 하므로 리스트를 통째로 뒤집어줌
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