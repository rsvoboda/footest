package org.jboss.eap.qa.footest;

import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AnalyseWFMain {

//    private static final String directoryOneDefault = "/Users/rsvoboda/tmp/wildfly-14.0.0.Beta2/modules/";
    private static final String directoryOneDefault = "/Users/rsvoboda/git/wildfly/dist/target/wildfly-15.0.0.Alpha1-SNAPSHOT/";
//    private static final String directoryOneDefault = "/Users/rsvoboda/TESTING/720CD14.CR1/jboss-eap-7.2/";

    public static void main(String... args) throws Exception {

        String directoryOne = directoryOneDefault;

        if (args.length == 2) {
            directoryOne = args[0];
        }

        header("Large JARs", false);
        largeJars(directoryOne);

        header("JARs With Package Substring 'org.apache'", true);
        jarsWithPackageSubstring(directoryOne, "org.apache");
        header("JARs With Package Substring 'io.netty'", true);
        jarsWithPackageSubstring(directoryOne, "io.netty");
        header("JARs With Package Substring 'io.reactive'", true);
        jarsWithPackageSubstring(directoryOne, "io.reactive");

        header("Possible Shaded JARs", true);
        possibleShadedJars(directoryOne + "modules/");

        header("Suspicious Modules", true);
        suspiciousModules(directoryOne + "modules/");

        header("Duplicate Packages", true);
        duplicatePackages(directoryOne + "modules/");

        header("Duplicate Classes", true);
        duplicateClasses(directoryOne + "modules/");
        footer();
    }

    private static void suspiciousModules(String directoryOne) throws IOException {
        Files.walk(Paths.get(directoryOne))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith("module.xml"))
                .forEach(path -> {
                    try {
                        Set<String> jarFilesPrefixes = Files.list(path.getParent())
                                .filter(p -> Files.isRegularFile(p) && p.getFileName().toString().endsWith(".jar"))
                                .map(jarPath -> jarPath.getFileName().toString().split("-")[0])
                                .collect(Collectors.toSet());
                        if (jarFilesPrefixes.size() > 1) {
                            System.out.println(path.getParent().toString());
                            System.out.println("JAR Files Prefixes: " + jarFilesPrefixes);
                            Files.list(path.getParent()).forEach(p -> System.out.println("  " + p.getFileName()));
                            System.out.println();
                        }
                    } catch (IOException e) { e. printStackTrace(); }
                });
    }

    private static void jarsWithPackageSubstring(String directoryOne, String packageSubstring) throws IOException {
        jarsAsZipFiles(Paths.get(directoryOne))
                .forEach(zip -> {
                    long containsSubstringCount = zip.stream()
                            .filter(entry -> entry.getName().endsWith(".class") && entry.getName().contains("/"))
                            .map(AnalyseWFMain::extractPackageFromZipEntry)
                            .filter(pkg -> pkg.contains(packageSubstring))
                            .count();
                    if ( containsSubstringCount > 0 ) {
                        System.out.println(containsSubstringCount + "\t" + zip.getName());
                    }
                });
    }

    // based on packages prefixes
    private static void possibleShadedJars(String directoryOne) throws IOException {
        jarsAsZipFiles(Paths.get(directoryOne))
                .forEach(zip -> {
                        Map<String, Integer> packages = new TreeMap<>();
                        int packageLevel = 3;

                        zip.stream()
                                .filter(entry -> entry.getName().endsWith(".class") && entry.getName().contains("/"))
                                .map(entry -> extractPackageFromZipEntry(entry.getName(), packageLevel))
                                .forEach(pkg -> {
                                    if (packages.containsKey(pkg)) {
                                        packages.put(pkg, packages.get(pkg) + 1);
                                    } else {
                                        packages.put(pkg, 1);
                                    }
                                });
                        if (packages.size() > 1) {
                            System.out.println(zip.getName());
                            packages.entrySet()
                                    .forEach(System.out::println);
                            System.out.println();
                        }
                });
    }

    private static String extractPackageFromZipEntry(String entry, int packageLevel) {

        String[] entrySplit = entry
                .substring(0, entry.lastIndexOf("/"))
                .split("/", packageLevel + 1);

        return String.join(
                ".", Arrays.copyOfRange(entrySplit, 0, packageLevel))
                .replaceAll(".null", "");
    }

    private static void largeJars(String directoryOne) throws IOException {
        Map<String, Long> jarsSizes = new TreeMap<>();

        Files.walk(Paths.get(directoryOne))
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().endsWith(".jar"))
                .forEach(path -> jarsSizes.put(path.toFile().getAbsolutePath(), path.toFile().length()));

        printValidEntries(directoryOne, jarsSizes, 3 * 1024 * 1024, 1024 * 1024 * 1024);
        System.out.println();
        printValidEntries(directoryOne, jarsSizes, 2 * 1024 * 1024, 3 * 1024 * 1024);
        System.out.println();
        printValidEntries(directoryOne, jarsSizes, 1024 * 1024, 2 * 1024 * 1024);
        System.out.println();
    }

    private static void printValidEntries(String directoryOne, Map<String, Long> jarsSizes, long minSize, long maxSize) {
        jarsSizes.forEach((key, value) -> {
            if (value > minSize && value < maxSize) {
                System.out.println(key.replaceAll(directoryOne, "")
                        + ": " + value + " (" + readableSize(value) + ")");
            }
        });
    }

    private static String readableSize (long size) {
        String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int unitIndex = (int) (Math.log10(size) / 3);
        double unitValue = 1 << (unitIndex * 10);

        return new DecimalFormat("#,##0.#")
                .format(size / unitValue) + " "
                + units[unitIndex];
    }

    private static void duplicateClasses(String directoryOne) throws IOException {
        Map<String, List<String>> classesInJars = new TreeMap<>();

        jarsAsZipFiles(Paths.get(directoryOne))
                .forEach(zip -> zip.stream()
                        .filter(entry -> entry.getName().endsWith(".class") && entry.getName().contains("/"))
                        .forEach(entry -> {
                            if (classesInJars.containsKey(entry.getName())) {
                                classesInJars.get(entry.getName()).add(zip.getName());
                            } else {
                                classesInJars.put(entry.getName(), Lists.newArrayList(zip.getName()));
                            }
                        })
                );
        Set<String> jarFiles = new TreeSet<>();
        classesInJars.forEach((key, value) -> {
            if (value.size() > 1) {
                jarFiles.addAll(value);
                System.out.printf("Class %s Is Present In Multiple JARs\n", key);
                value.forEach(System.out::println);
                System.out.println();
            }
        });

        System.out.println("Summary: Jars With Duplicated Classes:");
        jarFiles.forEach(System.out::println);
    }

    private static void duplicatePackages(String directoryOne) throws IOException {
        Map<String, List<String>> packagesInJars = new TreeMap<>();

        jarsAsZipFiles(Paths.get(directoryOne))
                .forEach(zip -> zip.stream()
                        .filter(entry -> entry.getName().endsWith(".class") && entry.getName().contains("/"))
                        .map(AnalyseWFMain::extractPackageFromZipEntry)
                        .distinct()
                        .forEach(pkg -> {
                            if (packagesInJars.containsKey(pkg)) {
                                packagesInJars.get(pkg).add(zip.getName());
                            } else {
                                packagesInJars.put(pkg, Lists.newArrayList(zip.getName()));
                            }
                        })
                );
        packagesInJars.forEach((key, value) -> {
            if (value.size() > 1) {
                System.out.printf("Package %s Is Present In Multiple JARs: %s", key, value);
                System.out.println();
            }
        });
    }

    private static String extractPackageFromZipEntry (ZipEntry entry) {
        return entry.getName()
                .substring(0, entry.getName().lastIndexOf("/"))
                .replaceAll("/", ".");
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

    private static void header(String title, boolean prependFooter) {
        if (prependFooter) {
            footer();
        }
        System.out.println("*************************************************");
        System.out.println(title);
        System.out.println("*************************************************");
    }
    private static void footer() {
        System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        System.out.println();
    }
}
