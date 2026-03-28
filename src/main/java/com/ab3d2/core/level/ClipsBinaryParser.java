import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;

public class ClipsBinaryParser {

    private int frameCount;
    private int frameWidth;
    private int frameHeight;
    private BufferedImage[] frames;

    public ClipsBinaryParser(InputStream inputStream) throws IOException {
        parseBinaryClips(inputStream);
    }

    private void parseBinaryClips(InputStream inputStream) throws IOException {
        byte[] data = inputStream.readAllBytes();
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        frameCount = buffer.getInt(); // Read frame count
        frameWidth = buffer.getInt(); // Read frame width
        frameHeight = buffer.getInt(); // Read frame height

        frames = new BufferedImage[frameCount];
        for (int i = 0; i < frameCount; i++) {
            byte[] imgData = new byte[frameWidth * frameHeight * 4]; // Assuming 4 bytes per pixel
            buffer.get(imgData); // Read image data
            frames[i] = new BufferedImage(frameWidth, frameHeight, BufferedImage.TYPE_INT_ARGB);
            frames[i].getRaster().setDataElements(0, 0, frameWidth, frameHeight, imgData);
        }
    }

    public int getFrameCount() {
        return frameCount;
    }

    public BufferedImage getFrame(int index) {
        return frames[index];
    }
}