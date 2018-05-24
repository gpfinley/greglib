package greglib.server;

/**
 * Created by greg on 5/23/18.
 */
//public class Service {
//
//    /**
//     * For extending classes, define this in the constructor.
//     */
//    final protected Function<String, String> postFunction;
//
//    /**
//     * Constructor takes a string argument and uses reflection to find the class and nab its postFunction.
//     * @param clazz
//     */
//    public Service(String clazz) {
//        Service subService;
//        try {
//            subService = (Service) Class.forName(clazz).newInstance();
//        } catch (ReflectiveOperationException e) {
//            throw new RuntimeException(e);
//        }
//        this.postFunction = subService.postFunction;
//    }
//
//    /**
//     *
//     * @param content
//     * @return
//     */
//    public final String post(String content) {
//        return postFunction.apply(content);
//    }
//
//}

public interface Service {
    String post(String content);
}
