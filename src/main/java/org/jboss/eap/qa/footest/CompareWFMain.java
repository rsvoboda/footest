package org.jboss.eap.qa.footest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public class CompareWFMain {

    private static final String directoryOneDefault = "/Users/rsvoboda/TESTING/720CD13.SP01/jboss-eap-7.2";
    private static final String directoryTwoDefault = "/Users/rsvoboda/TESTING/720Beta.CR1/jboss-eap-7.2";


    public static void main(String... args) throws Exception {

        String directoryOne = directoryOneDefault;
        String directoryTwo = directoryTwoDefault;

        if (args.length == 2) {
            directoryOne = args[0];
            directoryTwo = args[1];
        }

        Set<String> directoryOneClasses = classesInJarsUnderPath(Paths.get(directoryOne));
        Set<String> directoryTwoClasses = classesInJarsUnderPath(Paths.get(directoryTwo));

        System.out.println("directoryOne: " + directoryOne);
        System.out.println("directoryTwo: " + directoryTwo);
        System.out.println();
        System.out.println("directoryOneClasses size: " + directoryOneClasses.size());
        System.out.println("directoryTwoClasses size: " + directoryTwoClasses.size());

        Set<String> directoryOneClassesExtraClasses = new TreeSet<>(directoryOneClasses);
        Set<String> directoryTwoClassesExtraClasses = new TreeSet<>(directoryTwoClasses);
        directoryTwoClassesExtraClasses.removeAll(directoryOneClasses);
        directoryOneClassesExtraClasses.removeAll(directoryTwoClasses);

        System.out.println();
        System.out.println("directoryOneExtraClasses size: " + directoryOneClassesExtraClasses.size());
        directoryOneClassesExtraClasses.stream().forEach(System.out::println);

        System.out.println();
        System.out.println("directoryTwoExtraClasses size: " + directoryTwoClassesExtraClasses.size());
        directoryTwoClassesExtraClasses.stream().forEach(System.out::println);

        System.out.println();
        System.out.println("directoryOneExtraClasses summary:");
        packagesSummary(directoryOneClassesExtraClasses, 3);

        System.out.println();
        System.out.println("directoryTwoClassesExtraClasses summary:");
        packagesSummary(directoryTwoClassesExtraClasses, 3);
    }

    private static void packagesSummary(Set<String> classes, int packageLevel) {
        Map<String, Integer> packages = new TreeMap<>();
        classes.stream()
                .filter(entry -> entry.contains("/"))
                .map(entry -> extractPackageFromZipEntry(entry, packageLevel))
                .forEach(pkg -> {
                    if (packages.containsKey(pkg)) {
                        packages.put(pkg, packages.get(pkg) + 1);
                    } else {
                        packages.put(pkg, 1);
                    }
                });
        packages.entrySet()
                .forEach(System.out::println);
    }

    private static String extractPackageFromZipEntry(String entry, int packageLevel) {

        String[] entrySplit = entry
                .substring(0, entry.lastIndexOf("/"))
                .split("\\/", packageLevel + 1);

        return String.join(
                ".", Arrays.copyOfRange(entrySplit, 0, packageLevel))
                .replaceAll(".null", "");
    }

    private static Set<String> classesInJarsUnderPath(Path rootDirPath) throws IOException {
        Set<String> allClasses = new HashSet<>(5000);
        jarsAsZipFiles(rootDirPath)
                .forEach(zip -> {
                            Set<String> classes = zip.stream()
                                    .filter(entry -> entry.getName().endsWith(".class"))
                                    .map(entry -> entry.getName())
                                    .collect(Collectors.toSet());
                            allClasses.addAll(classes);
                        }

                );
        return allClasses;
    }

    private static Stream<ZipFile> jarsAsZipFiles(Path rootDirPath) throws IOException {
        return Files.walk(rootDirPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".jar"))
                .map(path -> {
                    try {
                        return new ZipFile(path.toFile());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }
}
