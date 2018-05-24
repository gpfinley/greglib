package greglib.config;

/**
 * Interface for chainable objects that need to initialize their state.
 * (They can't do this in the constructor because fields are filled in after calling it.)
 *
 * Created by greg on 10/21/17.
 */
public interface Chainable {

    void initialize() throws ConfigException;
}
