package org.jboss.eap.qa.footest;

import org.jboss.shrinkwrap.resolver.api.ResolutionException;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.PackagingType;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.logging.LogManager;

public class WildFlyDependenciesResolverMain {

    public static void main(String[] args) {
        System.out.println(new Date());

        //disable logging / warning
        LogManager.getLogManager().reset();

        String wildflyCoreVersion = "5.0.0.Final";
        String wildflyVersion = "13.0.0.Final";

        List<MavenCoordinate> coordinates = Maven.resolver().resolve(
                        "org.wildfly:wildfly-feature-pack:pom" + ":" + wildflyVersion,
                        "org.wildfly:wildfly-servlet-feature-pack:pom" + ":" + wildflyVersion,
                        "org.wildfly.core:wildfly-core-galleon-pack:pom" + ":" + wildflyCoreVersion
                )
                .withTransitivity().asList(MavenCoordinate.class);

        System.out.println("Size: " + coordinates.size());

        for (MavenCoordinate artifact : coordinates) {
            if (artifact.getType().equals(PackagingType.JAR)) {
                try {
                    File sources = Maven.resolver().resolve(artifact.getGroupId() + ":" + artifact.getArtifactId()
                            + ":" + artifact.getType().toString() + ":sources:" + artifact.getVersion())
                            .withoutTransitivity().asSingleFile();
                    System.out.println(artifact + " : " + sources.getAbsolutePath());
                } catch (ResolutionException ex) {
                    System.out.println("NO SOURCES for " + artifact);
                }
            } else {
                System.out.println("SKIP on " + artifact);
            }
        }

        System.out.println(new Date());
    }

}
