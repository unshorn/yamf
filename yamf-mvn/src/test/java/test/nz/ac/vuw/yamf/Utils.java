package test.nz.ac.vuw.yamf;

import java.io.File;

public class Utils {

    static File getResourceAsFile(String name) {
        ClassLoader classLoader = Utils.class.getClassLoader();
        return new File(classLoader.getResource(name).getFile());
    }
}
