package greglib.config;

import org.ini4j.Ini;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Apps meant to be configured with Ini fields or CLAs should extend this class.
 * They will need to override getIniSection and should be instantiated using getInstance.
 * They will need a no-argument constructor.
 * Apps will NOT need a main() method, although if they do have one, it is best to call RunApp.runConfigurableApp;
 * it is best to use RunApp with the first argument being the full class name and other arguments being INI files
 *      or parameters (as defined in IniAndArgumentParser.parseIniAndArgs).
 *
 * Created by greg on 9/6/17.
 */
public abstract class ConfigurableApp {

    protected Logger LOGGER;

    // Should return the name of the ini config section to use
    protected abstract String getIniSection();

    /**
     * The application's work should be done here.
     * Will be called after the constructor and setting all configurable fields.
     */
    public abstract void run();

    /**
     * Override this method in implementing apps if there is apps-specific logic in processing arguments
     * before creating an Ini from them.
     * @return an Ini configuration object as specified from these arguments
     */
    protected Ini parseIni(Class clazz, String section, String... args) throws ConfigException {
        return IniAndArgumentParser.parseIniAndArgs(clazz, section, args);
    }

    protected static <T extends ConfigurableApp> T getInstance(Class<T> clazz, Ini ini) {
        T t;
        try {
            t = clazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Class has no default constructor (should not happen)");
        }
        t.LOGGER = Logger.getLogger(t.getClass().getName());
        t.LOGGER.info(ini.toString());
        try {
            fillFieldValues(t, ini, t.getIniSection());
        } catch (ConfigException e) {
            System.out.println(getUsageSummary(clazz));
            throw new RuntimeException(e);
        }
        return t;
    }

    /**
     * Get all declared fields of this class and its superclasses.
     * @param clazz
     * @return a flat list of fields
     */
    private static List<Field> getThisAndSuperclassFields(Class clazz) {
        List<Field> thisAndSuperclassFields = new ArrayList<>();
        thisAndSuperclassFields.addAll(Arrays.asList(clazz.getDeclaredFields()));
        Class superClazz = clazz.getSuperclass();
        while (superClazz != null) {
            // Don't add duplicate fields from superclasses (i.e., effectively allow @Config annotations to override)
            Set<String> subclassFieldNames = thisAndSuperclassFields.stream()
                    .map(Field::getName)
                    .collect(Collectors.toSet());
            Arrays.stream(superClazz.getDeclaredFields())
                    .filter(f -> !subclassFieldNames.contains(f.getName()))
                    .forEach(thisAndSuperclassFields::add);
//            thisAndSuperclassFields.addAll(Arrays.asList(superClazz.getDeclaredFields()));
            superClazz = superClazz.getSuperclass();
        }
        return thisAndSuperclassFields;
    }

    /**
     *
     * @param t
     * @param ini
     * @param iniSection
     * @param <T>
     * @return false if the (chained) object could not be properly configured
     * @throws ConfigException
     */
    private static <T> boolean fillFieldValues(T t, Ini ini, String iniSection) throws ConfigException {
        Class clazz = t.getClass();
        for (Field field : getThisAndSuperclassFields(clazz)) {
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                if (annotation instanceof Config) {
                    Config a = (Config) annotation;
                    String value = ini.get(iniSection, a.name());
                    if (a.required() && value == null) {
                        if (ConfigurableApp.class.isAssignableFrom(t.getClass())) {
                            throw new ConfigException("Parameter " + a.name() + " is required!");
                        } else {
                            // If this was just a chainable object, set it to null
                            // (this obj will be null on the obj that has it as a member)
                            return false;
                        }
                    }
                    try {
                        field.setAccessible(true);
                        if (value != null) {
                            field.set(t, getObject(field.getType(), value));
                        }
                    } catch (IllegalAccessException e) {
                        throw new ConfigException(e);
                    }
                } else if (annotation instanceof ChainConfig) {

                    field.setAccessible(true);
                    try {
                        if (ConfigurableApp.class.isAssignableFrom(field.getType())) {
                            // If a ConfigurableApp is chained, treat it just like any other.
                            field.set(t, getInstance((Class<? extends ConfigurableApp>) field.getType(), ini));
                        } else {
                            Object obj = field.getType().newInstance();
                            // If not, just loop through its Config (or ChainConfig) fields and keep the same iniSection
                            if (!fillFieldValues(obj, ini, iniSection)) {
                                if (((ChainConfig) annotation).required()) {
                                    throw new ConfigException(field.getType() + " needs to be configured (check its fields)");
                                }
                                field.set(t, null);
                            } else {
                                field.set(t, obj);
                                if (Chainable.class.isAssignableFrom(field.getType())) {
                                    ((Chainable) field.get(t)).initialize();
                                }
                            }
                        }
                    } catch (IllegalAccessException | InstantiationException e) {
                        throw new ConfigException(e);
                    }

                }
            }
        }
        return true;
    }

    protected static <T extends ConfigurableApp> T getInstance(Class<T> clazz, String... args) {
        T t;
        try {
            t = clazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Class has no default constructor (if class overrides empty constructor, be sure it is public)");
        }
        t.LOGGER = Logger.getLogger(t.getClass().getName());
        for (String arg : args) {
            if (arg.equals("help") || arg.equals("-h") || arg.equals("--help")) {
                System.out.println(getUsageSummary(clazz));
                System.exit(1);
            }
        }
        Ini ini;
        try {
            ini = t.parseIni(clazz, t.getIniSection(), args);
        } catch (ConfigException e) {
            System.out.println(getUsageSummary(clazz));
            throw new RuntimeException(e);
        }
        return getInstance(clazz, ini);
    }

    private static <T> T getObject(Class<T> clazz, String value) throws ConfigException {
        if (clazz.equals(String.class)) {
            return (T) value;
        }
        // Check to see if we have any trivial transforms from String defined
        if (constructorLikes.containsKey(clazz)) {
            return (T) constructorLikes.get(clazz).apply(value);
        }

        // If not, see if this String is actually an implementation of clazz
        try {
            Class<?> subClazz = Class.forName(value);
            if (clazz.isAssignableFrom(subClazz)) {
                try {
                    return (T) subClazz.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new ConfigException("Cannot instantiate an object of class " + value);
                }
            }
        } catch (ClassNotFoundException e) {
            // do nothing. Try a String-only constructor in this case.
        }

        // Finally, try to call the desired class's constructor with the String argument
        try {
            return clazz.getConstructor(String.class).newInstance(value);
        } catch (ReflectiveOperationException e) {
            throw new ConfigException("Cannot instantiate an object of " + clazz + " with String argument " + value);
        }
    }

    // Known object initialization functions that are "constructor-like"
    private static final Map<Class, Function<String, ?>> constructorLikes = new HashMap<>();
    static {
        constructorLikes.put(Path.class, Paths::get);
        constructorLikes.put(int.class, Integer::parseInt);
        constructorLikes.put(Integer.class, Integer::parseInt);
        constructorLikes.put(boolean.class, Boolean::parseBoolean);
        constructorLikes.put(Boolean.class, Boolean::parseBoolean);
        constructorLikes.put(float.class, Float::parseFloat);
        constructorLikes.put(Float.class, Float::parseFloat);
        constructorLikes.put(double.class, Double::parseDouble);
        constructorLikes.put(Double.class, Double::parseDouble);
        constructorLikes.put(byte.class, Byte::parseByte);
        constructorLikes.put(Byte.class, Byte::parseByte);
        constructorLikes.put(String[].class, s -> s.split(";"));
        constructorLikes.put(Class.class, n -> {
            try {
                return Class.forName(n);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Class name " + n + " does not exist!");
            }
        });
    }

    /**
     * Generate a summary string to print specifying the usage of this apps.
     * @param clazz the Class of the apps extending this class
     * @return a human-readable usage summary
     */
    public static String getUsageSummary(Class<? extends ConfigurableApp> clazz) {
        ConfigurableApp t;
        try {
            t = clazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(clazz + " has no public default constructor");
        }
        StringBuilder summary = new StringBuilder();
        summary.append(clazz.toString())
                .append("\nSection name for .ini file: ")
                .append(t.getIniSection())
                .append("\n")
                .append("CONFIGURABLE FIELDS:\n");

        addAnnotationHelpToBuilder(summary, clazz, t, 0);

        summary.append("\n(* = required)");

        summary.append("\n\nCommand syntax:   java -jar EMRAI.jar " + clazz.getCanonicalName() + " --PARAMETER=VALUE...");

        return summary.toString();
    }

    /**
     * Loop through all Config annotations and add usage summaries to a running StringBuilder.
     * Will recurse through ChainConfig annotations.
     * @param summary a StringBuilder with a summary being built
     * @param clazz a class to loop through fields and annotations
     */
    private static void addAnnotationHelpToBuilder(StringBuilder summary, Class clazz, Object object, int indent) {
        StringBuilder thisSummary = new StringBuilder();
        for (Field field : getThisAndSuperclassFields(clazz)) {
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                if (annotation instanceof Config) {
                    Config a = (Config) annotation;
                    field.setAccessible(true);
                    String defaultVal;
                    try {
                        defaultVal = String.valueOf(field.get(object));
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                    if (a.required()) {
                        thisSummary.append("*");
                    }
                    thisSummary.append(a.name())
                            .append(" (")
                            .append(field.getType().getSimpleName())
                            .append("), default = ")
                            .append(defaultVal);
                    if (!a.doc().equals("")) {
                        thisSummary.append("\n\t")
                                .append(a.doc());
                    }
                    thisSummary.append("\n");
                } else if (annotation instanceof ChainConfig) {
                    // TODO: test for all cases
                    field.setAccessible(true);
                    Class fieldClass = field.getType();
                    try {
                        if (ConfigurableApp.class.isAssignableFrom(fieldClass)) {
                            thisSummary.append(fieldClass.toString())
                                    .append(" (see documentation for that class's configurable parameters)\n");
                        } else {
                            Object chainedObject = field.get(object);
                            if (chainedObject == null) {
                                try {
                                    chainedObject = fieldClass.newInstance();
                                } catch (InstantiationException e) {
                                    thisSummary.append(fieldClass.toString())
                                            .append(" (could not create instance)\n");
                                }
                            }
                            if (chainedObject != null) {
                                thisSummary.append(field.getType().getName()).append(":\n");
                                addAnnotationHelpToBuilder(thisSummary, fieldClass, chainedObject, indent+4);
                            }
                        }
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(
                                "Bad implementation of " + fieldClass + ", tell developer to fix");
                    }
                }
            }
        }

        String[] lines = thisSummary.toString().split("\\n");
        for (String line : lines) {
            for (int i=0; i<indent; i++) {
                summary.append(" ");
            }
            summary.append(line).append("\n");
        }
    }

}