package greglib.server;

import greglib.config.Chainable;
import greglib.config.Config;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.stream.Collectors;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Basic servlet for running service tasks.
 */
@WebServlet(name = "MicroservicesServlet", urlPatterns = { "" }, loadOnStartup = 1)
public class Servlet extends HttpServlet implements Chainable {

    @Config(name = "service", doc = "Name of class extending greglib.server.Service to use", required = true)
    protected Service service;

    // TODO: find a more elegant way to do this than setting a static variable!
    public static Service staticService;

    public Servlet() { }

    public Servlet(Service service) {
        staticService = service;
    }

    @Override
    public void initialize() {
        if (service != null) {
            staticService = service;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException();
        // TODO: implement
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/json");
        String content = request.getReader().lines().collect(Collectors.joining());
        String result = staticService.post(content);
        PrintWriter writer = response.getWriter();
        writer.write(result);
        writer.close();
    }
}
