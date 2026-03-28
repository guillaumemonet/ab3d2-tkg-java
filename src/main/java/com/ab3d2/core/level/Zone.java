public class Zone {
    private int zoneId;
    private float floor;
    private float roof;
    private int brightness;
    private List<Edge> edges;

    public Zone(int zoneId, float floor, float roof, int brightness, List<Edge> edges) {
        this.zoneId = zoneId;
        this.floor = floor;
        this.roof = roof;
        this.brightness = brightness;
        this.edges = edges;
    }

    public int getZoneId() {
        return zoneId;
    }

    public float getFloor() {
        return floor;
    }

    public float getRoof() {
        return roof;
    }

    public int getBrightness() {
        return brightness;
    }

    public List<Edge> getEdges() {
        return edges;
    }
}