package org.jboss.eap.qa.footest;

import org.unix4j.Unix4j;
import org.unix4j.line.Line;
import org.unix4j.unix.Grep;

import java.io.File;
import java.util.List;

public class GrepSourcesMain {

    private static final String sourcesDestination = "sources";

    public static void main(String[] args) throws Exception {

        List<String> sources = Unix4j.find(sourcesDestination, "*.java").toStringList();
        for(String path: sources){
            List<Line> lines = Unix4j.grep(Grep.Options.n, "java\\.version", new File(path)).toLineList();
            if (lines.size() > 0) {
                System.out.println(lines.size() + " - " + path);
                for (Line line : lines) {
                    System.out.println("\t" + line.getContent());
                }
            }
        }

    }
}
