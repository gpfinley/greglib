package greglib.config;

import org.ini4j.Ini;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Some configuration reader methods.
 *
 * Created by greg on 5/17/17.
 */
public final class IniAndArgumentParser {

    private static final Logger LOGGER = Logger.getLogger(IniAndArgumentParser.class.getName());

    /**
     * Override fields of an INI configuration using arguments specified as Strings.
     * If no value is specified, will assume boolean true.
     * @param ini the Ini object to modify
     * @param args command-line arguments in the format "--[sectionName:]fieldName=fieldVal" or "-[-][sec:]field val1 val2"
     * @param sectionName default section name when not overridden in individual options
     */
    public static void overrideArguments(Ini ini, String sectionName, String... args) {
        boolean iniStyle = true;
        // assume "INI style" if there are no arguments that start with '-'
        for (String arg : args) {
            if (arg.startsWith("-")) {
                iniStyle = false;
                break;
            }
        }
        if (iniStyle) {
            overrideArgumentsIniStyle(ini, sectionName, args);
        } else {
            overrideArgumentsUnixStyle(ini, sectionName, args);
        }
    }

    /**
     * Override the parameters using "UNIX style":
     * parameter names are specified with a '-' or '--' prefix,
     * the parameter and its value are separated by a space,
     * and additional values for one parameter can also be separated by spaces.
     * This style is preferred over "INI style."
     * @param ini
     * @param sectionName
     * @param args
     */
    private static void overrideArgumentsUnixStyle(Ini ini, String sectionName, String... args) {
        if (args.length == 0) return;
        String flag = args[0];
        while (flag.charAt(0) == '-') flag = flag.substring(1,flag.length());
        List<String> values = new ArrayList<>();
        for (int i=1; i<args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if (values.size() == 0) {
                    values.add("true");
                }
                overrideParameter(ini, flag, String.join(";", values), sectionName);
                values = new ArrayList<>();
                flag = arg;
                while (flag.charAt(0) == '-') flag = flag.substring(1,flag.length());
            } else {
                values.add(arg);
            }
        }
        // join arguments using a semicolon internally because that's what classes expect from an INI file
        if (values.size() == 0) {
            values.add("true");
        }
        overrideParameter(ini, flag, String.join(";", values), sectionName);
    }

    /**
     * Override parameters using "INI style":
     * parameter names do not have to be specified by '-',
     * and multiple values are separated by escaped semicolons.
     * Parameters are separated by spaces; no space can appear within a single parameter valuation.
     * @param ini
     * @param sectionName
     * @param args
     */
    private static void overrideArgumentsIniStyle(Ini ini, String sectionName, String... args) {
        for (String arg : args) {
            // Make starting with '-' or '--' optional
            int flagStart = 0;
            if (arg.startsWith("--")) {
                flagStart = 2;
            } else if(arg.startsWith("-")) {
                flagStart = 1;
            }
            // default to boolean true if no value is provided
            if (!arg.contains("=")) {
                arg += "=true";
            }
            int eq = arg.indexOf('=');
            overrideParameter(ini, arg.substring(flagStart, eq), arg.substring(eq + 1), sectionName);
        }
    }

    /**
     * Override a single parameter.
     * Allows for overriding parameters of different INI sections than the default by using syntax SECTION:PARAMETER.
     * @param ini
     * @param flag
     * @param value
     * @param thisSection
     */
    private static void overrideParameter(Ini ini, String flag, String value, String thisSection) {
        int colon = flag.indexOf(':');
        if (colon >= 0) {
            thisSection = flag.substring(0, colon);
            flag = flag.substring(colon + 1, flag.length());
        }
        if (!ini.containsKey(thisSection)) {
            ini.add(thisSection);
        }
        ini.get(thisSection).put(flag, value);
    }

    /**
     * Read an INI file. Used for apps configuration; included Class is used to see if there is an INI file in resources.
     * @param clazz Class to search the resource path if no matching INI file in the filesystem path
     * @param iniFilename path or name of INI file
     * @return a parsed Ini object
     */
    public static Ini readIni(Class clazz, String iniFilename) {
        Ini ini;
        try {
            if (new File(iniFilename).exists()) {
                ini = new Ini(new FileReader(iniFilename));
            } else {
                ini = new Ini(clazz.getResourceAsStream(iniFilename));
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("ERROR: INI file cannot be read: "+iniFilename);
        }
        return ini;
    }

    /**
     * Simple utility method. Can be useful when an Ini is desired, although this shouldn't be used for apps config.
     * @param iniFilename
     * @return
     */
    public static Ini readIni(String iniFilename) {
        Ini ini;
        try {
            ini = new Ini(new FileReader(iniFilename));
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("ERROR: INI file cannot be read: "+iniFilename);
        }
        return ini;
    }

    /**
     * Parse any number of Inis from the command line.
     * Arguments starting with "--" are overrides of parameters in the previously loaded Ini file;
     *      all other arguments should be paths to .INI files in the filesystem.
     * @param sectionName the section to override parameters (leave null if specifying section manually in overrides)
     * @param args usually, the same args from `public static void main(String[] args)`
     * @return a list of Ini objects (assumed one Ini per run of an application)
     */
    @Deprecated
    public static List<Ini> parseInisAndArgs(String sectionName, String... args) {
        List<Ini> iniList = new ArrayList<>();
        Ini lastIni = null;
        for (String arg : args) {
            if (arg.toLowerCase().endsWith(".ini")) {
                if (lastIni != null) {
                    iniList.add(lastIni);
                }
                lastIni = readIni(arg);
            } else if(arg.startsWith("--")) {
                if (lastIni == null) {
                    lastIni = new Ini();
                }
                overrideArguments(lastIni, sectionName, arg);
            } else {
                LOGGER.warning("Command line option \"" + arg + "\" does not conform to expected format.");
            }
        }
        if (lastIni == null) {
            lastIni = new Ini();
        }
        iniList.add(lastIni);
        return iniList;
    }

    public static Ini parseIniAndArgs(Class clazz, String sectionName, String... args) {
        if (args.length == 0) {
            return new Ini();
        }
        Ini ini;
        int start=0;
        if (args[0].toLowerCase().endsWith(".ini")) {
            ini = readIni(clazz, args[0]);
            start++;
        } else {
            ini = new Ini();
        }
        overrideArguments(ini, sectionName, Arrays.copyOfRange(args, start, args.length));
        return ini;
    }
}
