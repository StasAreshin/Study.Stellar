import org.stellar.sdk.Network;
import org.stellar.sdk.Server;

/**
 * Created by Stas on 2018-06-13.
 */
public class Connections {

    public static Server getServer() {
        return getServer(false);
    }

    private static Server getServer(Boolean isProd) {
        Server result = null;
        if (!isProd) {
            Network.useTestNetwork();
            result = new Server(main.HOST_HORIZON);
        } else {
            //TODO: Prod server not implemented yet, because we are only studying
        }
        return result;
    }
}
