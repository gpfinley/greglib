package greglib.config;

import org.ini4j.Ini;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Apps meant to be configured with Ini fields or CLAs should extend this class.
 * They will need to override getIniSection and should be instantiated using getConfiguredInstance.
 * They will need a no-argument constructor.
 * Apps will NOT need a main() method, although if they do have one, it is best to call RunApp.runConfigurableApp;
 * it is best to use RunApp with the first argument being the full class name and other arguments being INI files
 *      or parameters (as defined in IniAndArgumentParser.parseIniAndArgs).
 *
 * Created by greg on 9/6/17.
 */
public abstract class ConfigurableApp {

    protected Logger LOGGER;

    private static final int indentWidth = 4;
    private static final String tab = new String(new char[indentWidth]).replace("\0", " ");

    // Should return the name of the ini config section to use
    protected abstract String getIniSection();

    /**
     * String prefix for any arguments to configure this object (almost always empty)
     */
    protected String argumentPrefix = "";

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

    protected static <T extends ConfigurableApp> T getConfiguredInstance(Class<T> clazz, Ini ini, String prefix) {
        return configure(getNewInstance(clazz), ini, prefix);
    }

    protected static <T extends ConfigurableApp> T getConfiguredInstance(Class<T> clazz, String... args) {
        for (String arg : args) {
            if (arg.equals("help") || arg.equals("-h") || arg.equals("--help") || arg.equals("-help")) {
                System.out.println(getUsageSummary(clazz));
                System.exit(-1);
            }
        }
        T t = getNewInstance(clazz);
        try {
            Ini ini = t.parseIni(clazz, t.getIniSection(), args);
            return configure(t, ini, "");
        } catch (ConfigException e) {
            System.out.println(getUsageSummary(clazz));
            throw new RuntimeException(e);
        }
    }

    /**
     * Find a constructor and call it to create a new instance
     * @param clazz any ConfigurableApp implementation
     * @param <T> any ConfigurableApp implementation
     * @return a new, unconfigured instance of the app
     */
    private static <T extends ConfigurableApp> T getNewInstance(Class<T> clazz) {
        try {
            // private constructors are okay! Setting an empty constructor private could be useful
            //      if a class isn't appropriate for empty constructor in non-configured use
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(clazz.getName() + ": Class has no default constructor (should not happen)");
        }
    }

    /**
     * With an app created and Ini populated, get the final configured app.
     * @param app
     * @param ini
     * @param prefix
     * @return
     */
    private static <T extends ConfigurableApp> T configure(T app, Ini ini, String prefix) {
        app.argumentPrefix = prefix;
        app.LOGGER = Logger.getLogger(app.getClass().getName());
        app.LOGGER.info(ini.toString());
        try {
            fillFieldValues(app, ini, app.getIniSection(), prefix);
        } catch (ConfigException e) {
            System.out.println(getUsageSummary(app.getClass()));
            throw new RuntimeException(e);
        }
        return app;
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
            superClazz = superClazz.getSuperclass();
        }
        return thisAndSuperclassFields;
    }

    /**
     *
     * @param t an instantiated object to apply configuration to
     * @param ini
     * @param iniSection
     * @param <T>
     * @return false if the (chained) object could not be properly configured
     * @throws ConfigException
     */
    private static <T> boolean fillFieldValues(T t, Ini ini, String iniSection, String prefix) throws ConfigException {
        Class clazz = t.getClass();
        for (Field field : getThisAndSuperclassFields(clazz)) {
            for (Annotation annotation : field.getDeclaredAnnotations()) {
                if (annotation instanceof Config) {
                    Config a = (Config) annotation;
                    String prefixedName = prefix + a.name();
                    String value = ini.get(iniSection, prefixedName);
                    if (a.required() && value == null) {
                        if (ConfigurableApp.class.isAssignableFrom(t.getClass())) {
                            throw new ConfigException("Parameter '" + prefixedName + "' is required!");
                        } else {
                            // If this was just a chainable object, set it to null
                            // (this obj will be null on the obj that has it as a member)
                            return false;
                        }
                    }
                    try {
                        field.setAccessible(true);
                        if (value != null) {
                            field.set(t, getObject(field.getType(), value, ini));
                        }
                    } catch (IllegalAccessException e) {
                        throw new ConfigException(e);
                    }
                } else if (annotation instanceof ChainConfig) {
                    // Any ChainConfig-ed annotation must either be a ConfigurableApp or implement Chainable
                    field.setAccessible(true);
                    Class fieldClass = field.getType();
                    String chainedPrefix = prefix + ((ChainConfig) annotation).prefix();
                    try {
                        if (ConfigurableApp.class.isAssignableFrom(fieldClass)) {
                            // If a ConfigurableApp is chained, treat it just like any other.
                            field.set(t, getConfiguredInstance(
                                    (Class<? extends ConfigurableApp>) fieldClass,
                                    ini,
                                    chainedPrefix
                            ));
                        } else {
                            if (!Chainable.class.isAssignableFrom(field.getType())) {
                                throw new UnsupportedOperationException(String.format(
                                        "Bad implementation: @ChainConfig annotation applied to class %s that is not Chainable!",
                                        fieldClass));
                            }
                            Class<? extends Chainable> chainableFieldClass = fieldClass;
                            Chainable chainable = getInstanceOfChainable(chainableFieldClass);
                            // If not, just loop through its Config (or ChainConfig) fields and keep the same iniSection
                            if (!fillFieldValues(chainable, ini, iniSection, chainedPrefix)) {
                                if (((ChainConfig) annotation).required()) {
                                    throw new ConfigException(chainableFieldClass +
                                            " needs to be configured (check its fields)");
                                }
                                field.set(t, null);
                            } else {
                                chainable.initialize();
                                field.set(t, chainable);
                            }
                        }
                    } catch (IllegalAccessException e) {
                        throw new ConfigException(e);
                    }

                }
            }
        }
        return true;
    }

    /**
     * Allows us to override a private constructor for the purposes of configurability
     * @param clazz any class that implements the Chainable interface
     * @return a new instance (uninitialized)
     */
    private static Chainable getInstanceOfChainable(Class<? extends Chainable> clazz) {
        try {
            Constructor<? extends Chainable> constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new UnsupportedOperationException("No default constructor available for Chainable " + clazz.getName());
        }
    }


    private static <T> T getObject(Class<T> clazz, String value, Ini ini) throws ConfigException {
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
                    T t = (T) subClazz.getDeclaredConstructor().newInstance();
                    // Fill in further configurable values if any are provided for this class
                    //      (although they would not have shown up in the help message)
                    if (ConfigurableApp.class.isAssignableFrom(subClazz)) {
                        ConfigurableApp app = (ConfigurableApp) t;
                        fillFieldValues(app, ini, app.getIniSection(), app.argumentPrefix);
                    }
                    return t;
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
        constructorLikes.put(Pattern.class, Pattern::compile);
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
        ConfigurableApp t = getNewInstance(clazz);
        StringBuilder summary = new StringBuilder();
        summary.append(clazz.toString())
                .append("\nSection name for .ini file: ")
                .append(t.getIniSection())
                .append("\n")
                .append("CONFIGURABLE FIELDS:\n");

        addAnnotationHelpToBuilder(summary, clazz, t, 0, "");

        summary.append("\n(* = required)");

        summary.append("\n\nCommand syntax:   java -jar EMRAI.jar " + clazz.getCanonicalName() + " --PARAMETER=VALUE...");

        return summary.toString();
    }

    /**
     * Loop through all Config annotations and add usage summaries to a running StringBuilder.
     * Will recurse through ChainConfig annotations.
     * @param summary a StringBuilder with a summary being built
     * @param clazz a class to loop through fields and annotations
     * @param object
     * @param indent
     * @param prefix
     */
    private static void addAnnotationHelpToBuilder(StringBuilder summary, Class clazz, Object object, int indent, String prefix) {
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
                    thisSummary.append(prefix + a.name())
                            .append(" (")
                            .append(field.getType().getSimpleName())
                            .append("), default = ")
                            .append(defaultVal);
                    if (!a.doc().equals("")) {
                        thisSummary.append("\n")
                                .append(tab)
                                .append(a.doc());
                    }
                    thisSummary.append("\n");
                } else if (annotation instanceof ChainConfig) {
                    field.setAccessible(true);
                    Class fieldClass = field.getType();
                    try {
                        if (ConfigurableApp.class.isAssignableFrom(fieldClass)) {
                            // todo: why not recurse on this?
                            thisSummary.append(fieldClass.toString())
                                    .append(" (see documentation for that class's configurable parameters)\n");
                        } else {
                            if (!Chainable.class.isAssignableFrom(fieldClass)) {
                                throw new UnsupportedOperationException(String.format(
                                        "Bad implementation: @ChainConfig annotation applied to class %s that is not Chainable!",
                                        fieldClass));
                            }
                            Class<? extends Chainable> chainableFieldClass = fieldClass;
                            Object chainedObject = field.get(object);
                            if (chainedObject == null) {
                                chainedObject = getInstanceOfChainable(chainableFieldClass);
                            }
                            thisSummary.append(field.getType().getName()).append(":\n");
                            addAnnotationHelpToBuilder(thisSummary,
                                    fieldClass,
                                    chainedObject,
                                    indent + indentWidth,
                                    prefix + ((ChainConfig) annotation).prefix());
                        }
                    } catch (ReflectiveOperationException e) {
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

    // todo: delete
    protected static <T extends ConfigurableApp> T getInstanceOld(Class<T> clazz, String... args) {
        T t;
        try {
            t = clazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Class has no default constructor (if class overrides empty constructor, be sure it is public)");
        }
        t.LOGGER = Logger.getLogger(t.getClass().getName());
        for (String arg : args) {
            if (arg.equals("help") || arg.equals("-h") || arg.equals("--help") || arg.equals("-help")) {
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
        return getConfiguredInstance(clazz, ini, "");
    }


    // todo: delete
    protected static <T extends ConfigurableApp> T getInstanceOld(Class<T> clazz, Ini ini, String prefix) {
        T t;
        try {
            t = clazz.getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(clazz.getName() + ": Class has no default constructor (should not happen)");
        }
        t.argumentPrefix = prefix;
        t.LOGGER = Logger.getLogger(t.getClass().getName());
        t.LOGGER.info(ini.toString());
        try {
            fillFieldValues(t, ini, t.getIniSection(), prefix);
        } catch (ConfigException e) {
            System.out.println(getUsageSummary(clazz));
            throw new RuntimeException(e);
        }
        return t;
    }

}