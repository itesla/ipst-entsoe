/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.cim;

import org.apache.commons.cli.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class Main {

    private static final Options OPTIONS = new Options();

    static {
        OPTIONS.addOption(Option.builder()
                .longOpt("cim-zip-path")
                .desc("CIM zip file or directory")
                .hasArg()
                .argName("PATH")
                .required()
                .build());
        OPTIONS.addOption(Option.builder()
                .longOpt("output-dir")
                .desc("directory to write anonymized zip file")
                .hasArg()
                .argName("DIR")
                .required()
                .build());
        OPTIONS.addOption(Option.builder()
                .longOpt("dic-file")
                .desc("ID dictionary file")
                .hasArg()
                .argName("FILE")
                .required()
                .build());
    }

    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("cim-anonymizer", OPTIONS);
    }

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine line = parser.parse(OPTIONS, args);
            Path cimZipPath = Paths.get(line.getOptionValue("cim-zip-path"));
            Path outputDir = Paths.get(line.getOptionValue("output-dir"));
            Path dicFile = Paths.get(line.getOptionValue("dic-file"));

            CimAnomymizer anomymizer = new CimAnomymizer();
            CimAnomymizer.Logger logger = new CimAnomymizer.Logger() {
                @Override
                public void logAnonymizingFile(Path file) {
                    System.out.println("Anonymizing " + file);
                }

                @Override
                public void logSkipped(Set<String> skipped) {
                    System.err.println("Skipped " + skipped);
                }
            };

            if (Files.isDirectory(cimZipPath)) {
                try (Stream<Path> stream = Files.list(cimZipPath).filter(cimZipFile -> cimZipFile.getFileName().toString().endsWith(".zip"))) {
                    stream.forEach(cimZipFile -> anomymizer.anonymizeZip(cimZipFile, outputDir, dicFile, logger));
                }
            } else {
                anomymizer.anonymizeZip(cimZipPath, outputDir, dicFile, logger);
            }
        } catch (ParseException e) {
            printHelp();
            System.exit(-1);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-2);
        }
    }
}
