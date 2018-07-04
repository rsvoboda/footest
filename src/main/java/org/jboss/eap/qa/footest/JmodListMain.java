package org.jboss.eap.qa.footest;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class JmodListMain {

    private static final String jmodCommandPath = System.getProperties().get("java.home")
            + File.separator + "bin"
            + File.separator + "jmod";

    public static void main(String[] args) throws Exception {

        Files.walk(Paths.get(System.getProperties().get("java.home") + File.separator + "jmods"))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".jmod"))
                .sorted(Comparator.comparing(path -> path.toAbsolutePath()))
                .forEach(path -> {
                    System.out.println(path);
                    System.out.println("----- module details -----");
                    invokeJmod("describe", path);
                    System.out.println("----- names of all the entries -----");
                    invokeJmod("list", path);
                    System.out.println("-----");
                    System.out.println("   ");
                });
    }

    private static void invokeJmod(String command, Path jmodPath) {  //list, describe
        try {
            Process p = new ProcessBuilder(jmodCommandPath, command, jmodPath.toString()).start();
            p.waitFor(10, TimeUnit.SECONDS);

            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}