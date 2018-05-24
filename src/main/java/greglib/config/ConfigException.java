package greglib.config;

/**
 * Simple checked exception. If caught, might want to print a usage statement.
 *
 * Created by greg on 9/6/17.
 */
public class ConfigException extends Exception {
    public ConfigException() {}
    public ConfigException(String msg) {
        super(msg);
    }
    public ConfigException(Throwable e) {
        super(e);
    }
}
