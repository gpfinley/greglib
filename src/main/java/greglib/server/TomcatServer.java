package greglib.server;

import greglib.config.ChainConfig;
import greglib.config.Config;
import greglib.config.ConfigurableApp;
import greglib.config.RunApp;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;

import javax.servlet.ServletException;
import java.io.File;

/**
 * A generic and simple embedded Tomcat server, suitable for Java-based microservices.
 *
 * Created by greg on 5/23/18.
 */
public class TomcatServer extends ConfigurableApp {

    @Override
    public String getIniSection() {
        return "tomcat_server";
    }

    @Config(name = "port", doc = "Port to run server on")
    private int port = 5000;

    @ChainConfig
    protected Servlet servlet;

    public void run() {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(port);

        try {
            Context ctx = tomcat.addWebapp("/", new File(".").getAbsolutePath());
            Tomcat.addServlet(ctx, "Embedded", servlet);

            tomcat.start();
        } catch(ServletException | LifecycleException e) {
            throw new RuntimeException(e);
        }
        tomcat.getServer().await();
    }

    public static void main(String[] args) {
        RunApp.main(TomcatServer.class, args);
    }
}
