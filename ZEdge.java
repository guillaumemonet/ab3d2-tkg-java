public class ZEdge {
    private Vec2L position; // Position field

    public Vec2L getPosition() {
        return position;
    }

    public void setPosition(Vec2L position) {
        this.position = position;
    }

    @Override
    public String toString() {
        return "ZEdge{position=" + position + '}';
    }
}