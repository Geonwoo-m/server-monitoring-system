import java.util.ArrayList;
import java.util.List;

public class MonitoringView {
    public static String render(List<Metric> history) {
        // 데이터 가공
        StringBuilder labels = new StringBuilder();
        StringBuilder cpuData = new StringBuilder();
        StringBuilder memData = new StringBuilder();

        for (int i = history.size() - 1; i >= 0; i--) {
            Metric m = history.get(i);
            labels.append("'").append(m.getTimestamp().toString().substring(11, 19)).append("',"); // 시간만 추출
            cpuData.append(m.getCpu()).append(",");
            memData.append(m.getMemory()).append(",");
        }

        StringBuilder sb = new StringBuilder();

        double cpuSum = 0;
        for(Metric m : history){
            cpuSum+=m.getCpu();
        }

        double cpuAvg = history.isEmpty() ? 0 : cpuSum/history.size();

        String statusText, statusColor;
        if (cpuAvg >= 70) { statusText = "위험 🔴"; statusColor = "#dc3545"; }
        else if (cpuAvg >= 40) { statusText = "주의 🟡"; statusColor = "#ffc107"; }
        else { statusText = "정상 🟢"; statusColor = "#28a745"; }

        sb.append("<html><head> <meta charset='UTF-8'>");
        sb.append("<script src='https://cdn.jsdelivr.net/npm/chart.js'></script>");
        sb.append("<style>");
        sb.append("body { font-family: 'Segoe UI', sans-serif; margin: 40px; background-color: #f8f9fa; }");
        sb.append("table { width: 100%; border-collapse: collapse; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
        sb.append("th, td { padding: 15px; border-bottom: 1px solid #eee; text-align: center; }");
        sb.append("th { background-color: #007bff; color: white; }");
        sb.append("tr:hover { background-color: #f1f1f1; }");
        sb.append(".alert { color: red; font-weight: bold; }");
        sb.append("</style></head><body>");

        sb.append("<h2>📊 실시간 서버 리소스 모니터링 (구조화 버전)</h2>");

        sb.append("<div class= 'status-card'>");
        sb.append("  <h3>현재 서버 상태: <span style='color:").append(statusColor).append(";'>").append(statusText).append("</span></h3>");
        sb.append("  <p>최근 10회 평균 CPU 사용률: <strong>").append(String.format("%.1f%%", cpuAvg)).append("</strong></p>");
        sb.append("</div>");

        sb.append("<div style='width: 100%; margin-bottom: 30px; background: white; padding: 20px; border-radius: 10px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);'>");
        sb.append("  <canvas id='myChart' height='100'></canvas>");
        sb.append("</div>");

        sb.append("<table><tr><th>ID</th><th>서버 이름</th><th>CPU 사용률</th><th>메모리 사용률</th><th>기록 시간</th></tr>");



        for (Metric m : history) {
            // CPU가 80% 넘으면 빨간색으로 표시하는 기능 슬쩍 추가
            String cpuClass = m.getCpu() >= 80 ? "class='alert'" : "";
            sb.append("<tr>")
                    .append("<td>").append(m.getId()).append("</td>")
                    .append("<td><strong>").append(m.getAgentName()).append("</strong></td>")
                    .append("<td ").append(cpuClass).append(">").append(String.format("%.1f%%", m.getCpu())).append("</td>")
                    .append("<td>").append(String.format("%.1f%%", m.getMemory())).append("</td>")
                    .append("<td>").append(m.getTimestamp()).append("</td>")
                    .append("</tr>");
        }

        sb.append("</table>");
        sb.append("<script>");
        sb.append("const ctx = document.getElementById('myChart').getContext('2d');");
        sb.append("new Chart(ctx, {")
                .append("  type: 'line',")
                .append("  data: {")
                .append("    labels: [").append(labels).append("],")
                .append("    datasets: [{")
                .append("      label: 'CPU Usage (%)',")
                .append("      data: [").append(cpuData).append("],")
                .append("      borderColor: 'rgb(255, 99, 132)',")
                .append("      tension: 0.3")
                .append("    }, {")
                .append("      label: 'Memory Usage (%)',")
                .append("      data: [").append(memData).append("],")
                .append("      borderColor: 'rgb(54, 162, 235)',")
                .append("      tension: 0.3")
                .append("    }]")
                .append("  },")
                .append("  options: { responsive: true, scales: { y: { min: 0, max: 100 } } }")
                .append("});");
        sb.append("</script>");
        sb.append("<script>setTimeout(() => location.reload(), 2000);</script>");
        sb.append("</body></html>");

        return sb.toString();
    }
}