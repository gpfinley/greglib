package greglib.util;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Make a singleton out of anything!
 *
 * Created by greg on 6/8/18.
 */
public final class SingletonOf {

    private final static Map<Class, Object> singletonObjects = new HashMap<>();

    public static <T> T get(Class<? extends T> clazz, Object... constructorParams) {
        if (!singletonObjects.containsKey(clazz)) {
//            List<Class> classes = Arrays.stream(constructorParams).map(Object::getClass).collect(Collectors.toList());
//            Class[] classesArray = classes.toArray(new Class[classes.size()]);
            T t = null;
            for (Constructor constructor : clazz.getConstructors()) {
                try {
                    t = (T) constructor.newInstance(constructorParams);
                } catch (InstantiationException e) {
                    // do nothing; try the next constructor
                } catch (ReflectiveOperationException e2) {
                    throw new RuntimeException(e2);
                }
            }
            if (t == null) {
                throw new RuntimeException("No appropriate constructor found");
            }
            singletonObjects.put(clazz, t);
//            try {
//                T t = clazz.getConstructor(classesArray).newInstance(constructorParams);
//            } catch (ReflectiveOperationException e) {
//                throw new RuntimeException(e);
//            }
        }
        return (T) singletonObjects.get(clazz);
    }
}
