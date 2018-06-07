package org.jboss.eap.qa.footest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public class CompareWFMain {

    private static final String directoryOneDefault = "/Users/rsvoboda/TESTING/wildfly-13.0.0.Beta1";
    private static final String directoryTwoDefault = "/Users/rsvoboda/TESTING/wildfly-13.0.0.Final";


    public static void main(String... args) throws Exception {

        String directoryOne = directoryOneDefault;
        String directoryTwo = directoryTwoDefault;

        if (args.length == 2 ) {
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
        System.out.println("directoryOneClassesExtraClasses size: " + directoryOneClassesExtraClasses.size());
        directoryOneClassesExtraClasses.stream().forEach(System.out::println);

        System.out.println();
        System.out.println("directoryTwoClassesExtraClasses size: " + directoryTwoClassesExtraClasses.size());
        directoryTwoClassesExtraClasses.stream().forEach(System.out::println);



    }


    private static Set<String> classesInJarsUnderPath (Path rootDirPath) throws IOException {
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
