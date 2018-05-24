package greglib.server;

/**
 * Simple way to test a TomcatServer.
 *
 * Created by greg on 5/23/18.
 */
public class EchoService implements Service {

    public String post(String content) {
        return content;
    }
}
