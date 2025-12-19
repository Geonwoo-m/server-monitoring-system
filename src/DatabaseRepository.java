import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseRepository {
    private static final String URL = "jdbc:mysql://localhost:3306/monitoring_db";
    private static final String USER = "root";
    private static final String PW = "1234";

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
}