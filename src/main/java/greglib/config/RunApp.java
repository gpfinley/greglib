package greglib.config;

import java.util.Arrays;

/**
 * Generic way to run a configurable apps.
 * Preferred style:
 * java -jar JARNAME CLASSNAME [configfile.ini] -option value1 value2 -otheroption value ...
 * Alternatively:
 * java -jar JARNAME CLASSNAME [configfile.ini] [option=value otheroption=value ...]
 *
 * If no initial ini file is provided, a new Ini object is created and is only fed with the given arguments.
 * The ini filename can be a resource bundled with the jar, though a local file with the same name will take precedence.
 *
 * Created by greg on 9/6/17.
 */
public class RunApp {

    private static String defaultAppPackage = "";

    public static void setDefaultAppPackage(String location) {
        defaultAppPackage = location;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: 'java -jar EMRAI.jar CLASSNAME PARAMETERS...'");
            System.out.println("       'java -jar EMRAI.jar CLASSNAME help' for a list of configurable parameters");
            System.exit(1);
        }
        String className = args[0];
        Class<? extends ConfigurableApp> clazz;
        try {
            Class<?> tempClass = Class.forName(className);
            clazz = tempClass.asSubclass(ConfigurableApp.class);
        } catch (ClassNotFoundException e) {
            try {
                String newName = defaultAppPackage + "." + className;
                Class<?> tempClass = Class.forName(newName);
                clazz = tempClass.asSubclass(ConfigurableApp.class);
            } catch (ClassNotFoundException e2) {
                throw new RuntimeException(e);
            }
        } catch (ClassCastException e) {
            System.out.println(className + " does not implement ConfigurableApp and cannot be run in this way");
            throw new RuntimeException(e);
        }
        runConfigurableApp(clazz, Arrays.copyOfRange(args, 1, args.length));
    }

    /**
     * Might be useful for either legacy or convenience: use main() method in a ConfigurableApp to call this directly.
     * @param clazz a class that extends ConfigurableApp
     * @param args the normal command-line arguments following that class name
     */
    public static void runConfigurableApp(Class<? extends ConfigurableApp> clazz, String[] args) {
        ConfigurableApp.getInstance(clazz, args).run();
    }

    /**
     * Simple shortcut for classes that want their own main() that does something similar to this class's main.
     * @param appClass
     * @param args
     */
    public static void main(Class<? extends ConfigurableApp> appClass, String[] args) {
        String[] newArgs = new String[args.length+1];
        newArgs[0] = appClass.getName();
        for (int i=0; i<args.length; i++) {
            newArgs[i+1] = args[i];
        }
        main(newArgs);
    }

}
