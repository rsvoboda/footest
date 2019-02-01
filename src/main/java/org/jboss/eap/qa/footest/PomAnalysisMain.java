package org.jboss.eap.qa.footest;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;
import java.util.Properties;

public class PomAnalysisMain {

    public static void main(String... args) {
        try {
            Model newModel = pomToModel("/Users/rsvoboda/git/wildfly/pom.xml");
            Model oldModel = pomToModel("/Users/rsvoboda/tmp/wildfly/pom.xml");

            Properties newProperties = newModel.getProperties();
            Properties oldProperties = oldModel.getProperties();

            // removed / added properties
            for (String key : newProperties.stringPropertyNames()) {
                if (! oldProperties.containsKey(key)) {
                    System.out.println("Added property: " + key);
                }
            }
            for (String key : oldProperties.stringPropertyNames()) {
                if (! newProperties.containsKey(key)) {
                    System.out.println("Removed property: " + key);
                }
            }
            // changed properties
            for (Map.Entry<Object, Object> entry : newProperties.entrySet()) {
                Object oldValue = oldProperties.get(entry.getKey());
                if (oldValue != null && !entry.getValue().equals(oldValue)) {
                    System.out.printf("Changed property: %s from %s to %s\n", entry.getKey(), oldValue, entry.getValue());
                }
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
