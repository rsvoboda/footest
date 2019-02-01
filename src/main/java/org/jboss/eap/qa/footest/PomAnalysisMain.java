package org.jboss.eap.qa.footest;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;

public class PomAnalysisMain {

    private static final String FILE_URL = "https://raw.githubusercontent.com/wildfly/wildfly/master/pom.xml";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static void main(String... args) {
        try {
            Model newModel = pomToModel("/Users/rsvoboda/git/wildfly/pom.xml");
//            Model oldModel = pomToModel("/Users/rsvoboda/tmp/wildfly/pom.xml");

            Path tempFile = Files.createTempFile("PomAnalysis-wildfly-", ".tmp");
            InputStream in = new URL(FILE_URL).openStream();
            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
            Model oldModel = pomToModel(tempFile.toString());
            Files.delete(tempFile);

            Properties newProperties = newModel.getProperties();
            Properties oldProperties = oldModel.getProperties();
            StringBuilder sb = new StringBuilder();

            // removed / added properties
            for (String key : newProperties.stringPropertyNames()) {
                if (! oldProperties.containsKey(key)) {
                    sb.append("Added property: ");
                    sb.append(key);
                    sb.append(LINE_SEPARATOR);
                }
            }
            for (String key : oldProperties.stringPropertyNames()) {
                if (! newProperties.containsKey(key)) {
                    sb.append("Removed property: ");
                    sb.append(key);
                    sb.append(LINE_SEPARATOR);
                }
            }
            // changed properties
            for (Map.Entry<Object, Object> entry : newProperties.entrySet()) {
                Object oldValue = oldProperties.get(entry.getKey());
                if (oldValue != null && !entry.getValue().equals(oldValue)) {
                    sb.append("Changed property: ");
                    sb.append(String.format("%s from %s to %s\n", entry.getKey(), oldValue, entry.getValue()));
                    sb.append(LINE_SEPARATOR);
                }
            }

            if (sb.length() > 0) {
                System.out.println(sb.toString());
            } else {
                System.out.println("No changes");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Model pomToModel(String pathToPom) throws Exception {
        try (BufferedReader in = new BufferedReader(new FileReader(pathToPom))) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(in);
            return model;
        }
    }
}
