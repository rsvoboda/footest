package org.jboss.eap.qa.footest;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class JdepsJdkInternalsMain {

    private static final String jdepsCommandPath = System.getProperties().get("java.home")
            + File.separator + "bin"
            + File.separator + "jdeps";

    private static final String directoryDefault = "/Users/rsvoboda/git/wildfly/dist/target/wildfly-14.0.0.Beta1-SNAPSHOT/modules";

    public static void main(String[] args) throws Exception {

        String directory = directoryDefault;
        if (args.length == 1 ) {
            directory = args[0];
        }

        Files.walk(Paths.get(directory))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".jar"))
                .sorted(Comparator.comparing(path -> path.toAbsolutePath()))
                .forEach(JdepsJdkInternalsMain::invokeJdeps);
    }

    private static void invokeJdeps(Path path) {
        try {
            Process p = new ProcessBuilder(jdepsCommandPath, "--jdk-internals", path.toString()).start();
            p.waitFor(5, TimeUnit.SECONDS);

            if (p.exitValue() == 2) { // Error: foo.jar is a multi-release jar file but --multi-release option is not set
                p = new ProcessBuilder(jdepsCommandPath, "--jdk-internals", "--multi-release", "base", path.toString()).start();
                p.waitFor(5, TimeUnit.SECONDS);
            }

            if (p.exitValue() != 0 || (p.exitValue() == 0 && p.getInputStream().available() > 0)) {
                System.out.println(path);
                System.out.println("----- details -----");

                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }

                System.out.println("   ");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}