import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
public class DatabaseRepository {

    private static final Properties prop = new Properties();
    static {
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            prop.load(fis);
        } catch (IOException e) {
            System.err.println("설정 파일을 찾을 수 없습니다: db.properties");
            e.printStackTrace();
        }
    }

    private static final String URL = prop.getProperty("db.url");
    private static final String USER = prop.getProperty("db.user");
    private static final String PW = prop.getProperty("db.password");

    public void save(double cpu, double memory) {
        String sql = "INSERT INTO system_metrics (cpu_usage, memory_usage) VALUES (?, ?)";
        try (Connection conn = DriverManager.getConnection(URL, USER, PW);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, cpu);
            pstmt.setDouble(2, memory);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Metric> findAll(int limit) {
       List<Metric> metrics = new ArrayList<>();
       String sql = "SELECT * FROM system_metrics ORDER BY recorded_at DESC LIMIT " + limit;
       try( Connection conn = DriverManager.getConnection(URL,USER,PW);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)){

           while(rs.next()) {
               metrics.add(new Metric(
                    rs.getInt("id"),
                    rs.getDouble("cpu_usage"),
                    rs.getDouble("memory_usage"),
                    rs.getTimestamp("recorded_at").toString()
               ));
           }

       }catch(SQLException e){
           e.printStackTrace();
       }

       return metrics;
    }

    public double getTodayMaxCpu(){
        String sql = "SELECT MAX(cpu_usage) FROM system_metrics WHERE DATE(record_at) = CURDATE()";
        try(Connection conn = DriverManager.getConnection(URL,USER,PW);
        PreparedStatement pstmt = conn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery()){
            if(rs.next())
                return rs.getDouble(1);
        }catch(SQLException e){
            e.printStackTrace();
        }
        return 0.0;
    }
}