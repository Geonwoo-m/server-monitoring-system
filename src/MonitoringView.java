import java.util.List;

public class MonitoringView {
    public static String render(List<Metric> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><head><meta charset='UTF-8'>");
        sb.append("<style>");
        sb.append("body { font-family: 'Segoe UI', sans-serif; margin: 40px; background-color: #f8f9fa; }");
        sb.append("table { width: 100%; border-collapse: collapse; background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }");
        sb.append("th, td { padding: 15px; border-bottom: 1px solid #eee; text-align: center; }");
        sb.append("th { background-color: #007bff; color: white; }");
        sb.append("tr:hover { background-color: #f1f1f1; }");
        sb.append(".alert { color: red; font-weight: bold; }");
        sb.append("</style></head><body>");

        sb.append("<h2>📊 실시간 서버 리소스 모니터링 (구조화 버전)</h2>");
        sb.append("<table><tr><th>ID</th><th>CPU 사용률</th><th>메모리 사용률</th><th>기록 시간</th></tr>");

        for (Metric m : history) {
            // CPU가 80% 넘으면 빨간색으로 표시하는 기능 슬쩍 추가
            String cpuClass = m.getCpu() >= 80 ? "class='alert'" : "";
            sb.append("<tr>")
                    .append("<td>").append(m.getId()).append("</td>")
                    .append("<td ").append(cpuClass).append(">").append(String.format("%.1f%%", m.getCpu())).append("</td>")
                    .append("<td>").append(String.format("%.1f%%", m.getMemory())).append("</td>")
                    .append("<td>").append(m.getTimestamp()).append("</td>")
                    .append("</tr>");
        }

        sb.append("</table>");
        sb.append("<script>setTimeout(() => location.reload(), 2000);</script>");
        sb.append("</body></html>");

        return sb.toString();
    }
}