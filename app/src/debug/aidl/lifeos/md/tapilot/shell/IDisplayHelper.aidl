package lifeos.md.tapilot.shell;
import android.view.Surface;
interface IDisplayHelper {
    int identity();
    void watchClient(IBinder client);
    int create(in Surface surface, int width, int height);
    boolean gesture(int displayId, in float[] points, long duration);
    void launch(int displayId, String component);
    void release(int displayId);
    void shutdown();
}
