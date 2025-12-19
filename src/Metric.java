public class Metric {
    private int id;
    private String agentName; // 추가 : 어떤 서버인지 식별하기 위함
    private double cpu;
    private double memory;
    private String timestamp;

    public Metric(int id, String agentName,double cpu, double memory, String timestamp) {
        this.id = id;
        this.agentName= agentName;
        this.cpu = cpu;
        this.memory = memory;
        this.timestamp = timestamp;
    }

    public int getId() { return id; }
    public double getCpu() { return cpu; }
    public double getMemory() { return memory; }
    public String getTimestamp() { return timestamp; }
    public String getAgentName(){ return agentName; }
}