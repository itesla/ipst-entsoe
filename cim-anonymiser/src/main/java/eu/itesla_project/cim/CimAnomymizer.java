/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.cim;

import com.google.common.collect.ImmutableSet;
import eu.itesla_project.commons.util.StringAnonymizer;
import javanet.staxutils.helpers.EventWriterDelegate;
import net.java.truevfs.access.TPath;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CimAnomymizer {

    public interface Logger {

        void logAnonymizingFile(Path file);

        void logSkipped(Set<String> skipped);
    }

    public static class DefaultLogger implements Logger {

        public void logAnonymizingFile(Path file) {
        }

        public void logSkipped(Set<String> skipped) {
        }
    }

    private static final Set<String> NAMES_TO_EXCLUDE = ImmutableSet.of("PATL",
                                                                        "TATL");

    private static final Set<String> DESCRIPTIONS_TO_EXCLUDE = ImmutableSet.of();

    private static final String RDF_URI = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    private static final QName RDF_ID = new QName(RDF_URI, "ID");
    private static final QName RDF_RESOURCE = new QName(RDF_URI, "resource");
    private static final QName RDF_ABOUT = new QName(RDF_URI, "about");

    private final XMLInputFactory inputFactory = XMLInputFactory.newFactory();
    private final XMLOutputFactory outputFactory = XMLOutputFactory.newFactory();
    private final XMLEventFactory eventFactory = XMLEventFactory.newInstance();

    private static void readRdfIdValues(Path cimFile, XMLInputFactory inputFactory, Set<String> rdfIdValues) {
        // memoize RDF ID values of the document
        try (Reader reader = Files.newBufferedReader(cimFile, StandardCharsets.UTF_8)) {
            XMLEventReader eventReader = inputFactory.createXMLEventReader(reader);
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();
                if (event.isStartElement()) {
                    StartElement startElement = event.asStartElement();
                    Iterator it = startElement.getAttributes();
                    while (it.hasNext()) {
                        Attribute attribute = (Attribute) it.next();
                        QName name = attribute.getName();
                        if (RDF_ID.equals(name)) {
                            rdfIdValues.add("#" + attribute.getValue());
                        }
                    }
                }
            }
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private static XMLEvent anonymizeCharacters(Characters characters, Set<String> exclude, Set<String> skipped, XMLEventFactory eventFactory,
                                                StringAnonymizer dictionary) {
        if (exclude.contains(characters.getData())) {
            skipped.add(characters.getData());
            return null;
        } else {
            return eventFactory.createCharacters(dictionary.anonymize(characters.getData()));
        }
    }

    private static void anonymizeFile(Path cimFile, Path anonymizedCimFile, XMLInputFactory inputFactory,
                                      XMLOutputFactory outputFactory, XMLEventFactory eventFactory,
                                      StringAnonymizer dictionary, Set<String> rdfIdValues, Set<String> skipped) {
        try (Writer writer = Files.newBufferedWriter(anonymizedCimFile, StandardCharsets.UTF_8);
             Reader reader = Files.newBufferedReader(cimFile, StandardCharsets.UTF_8)) {

            XMLEventReader eventReader = inputFactory.createXMLEventReader(reader);
            XMLEventWriter eventWriter = new EventWriterDelegate(outputFactory.createXMLEventWriter(writer)) {

                private boolean identifiedObjectName = false;
                private boolean identifiedObjectDescription = false;

                @Override
                public void add(XMLEvent event) throws XMLStreamException {

                    XMLEvent newEvent = null;

                    if (event.isStartElement()) {
                        StartElement startElement = event.asStartElement();
                        if (startElement.getName().getLocalPart().equals("IdentifiedObject.name")) {
                            identifiedObjectName = true;
                        } else if (startElement.getName().getLocalPart().equals("IdentifiedObject.description")) {
                            identifiedObjectDescription = true;
                        } else {
                            Iterator it = startElement.getAttributes();
                            if (it.hasNext()) {
                                List<Attribute> newAttributes = new ArrayList<>();
                                while (it.hasNext()) {
                                    Attribute attribute = (Attribute) it.next();
                                    Attribute newAttribute = null;
                                    if (attribute.getName().equals(RDF_ID)) {
                                        newAttribute = eventFactory.createAttribute(attribute.getName(), dictionary.anonymize(attribute.getValue()));
                                    } else if (attribute.getName().equals(RDF_RESOURCE) ||
                                               attribute.getName().equals(RDF_ABOUT)) {
                                        // skip outside graph rdf:ID references
                                        if (rdfIdValues.contains(attribute.getValue())) {
                                            String valueWithoutHashtag = attribute.getValue().substring(1);
                                            newAttribute = eventFactory.createAttribute(attribute.getName(),
                                                                                        "#" + dictionary.anonymize(valueWithoutHashtag));
                                        } else {
                                            skipped.add(attribute.getValue());
                                        }
                                    } else {
                                        throw new AssertionError("Unknown attribute "+  attribute.getName());
                                    }
                                    newAttributes.add(newAttribute != null ? newAttribute : attribute);
                                }
                                newEvent = eventFactory.createStartElement(startElement.getName(),
                                                                           newAttributes.iterator(),
                                                                           startElement.getNamespaces());
                            }
                        }
                    } else if (event.isCharacters()) {
                        Characters characters = event.asCharacters();
                        if (identifiedObjectName ) {
                            identifiedObjectName = false;
                            newEvent = anonymizeCharacters(characters, NAMES_TO_EXCLUDE, skipped, eventFactory, dictionary);
                        } else if (identifiedObjectDescription) {
                            identifiedObjectDescription = false;
                            newEvent = anonymizeCharacters(characters, DESCRIPTIONS_TO_EXCLUDE, skipped, eventFactory, dictionary);
                        }
                    }

                    super.add(newEvent != null ? newEvent : event);
                }
            };
            eventWriter.add(eventReader);
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private static StringAnonymizer loadDic(Path dictionaryFile) {
        StringAnonymizer dictionary = new StringAnonymizer();
        // load previous dictionary
        if (dictionaryFile != null) {
            if (Files.exists(dictionaryFile)) {
                try (BufferedReader reader = Files.newBufferedReader(dictionaryFile, StandardCharsets.UTF_8)) {
                    dictionary.readCsv(reader);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return dictionary;
    }

    private void saveDic(StringAnonymizer dictionary, Path dictionaryFile) {
        // save updated dictionary
        try (BufferedWriter writer = Files.newBufferedWriter(dictionaryFile, StandardCharsets.UTF_8)) {
            dictionary.writeCsv(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void anonymizeZip(Path cimZipFile, Path anonymizedCimFileDir, Path dictionaryFile, Logger logger) {
        Objects.requireNonNull(cimZipFile);
        Objects.requireNonNull(anonymizedCimFileDir);
        Objects.requireNonNull(dictionaryFile);
        Objects.requireNonNull(logger);

        logger.logAnonymizingFile(cimZipFile);

        if (!Files.isDirectory(anonymizedCimFileDir)) {
            throw new RuntimeException(anonymizedCimFileDir + " has to be a directory");
        }

        Path anonymizedCimZipFile = anonymizedCimFileDir.resolve(cimZipFile.getFileName());

        // load dictionary
        StringAnonymizer dictionary = loadDic(dictionaryFile);

        // memoize rdf:ID values, will be used to detect outside graph references
        Set<String> rdfIdValues = new HashSet<>();
        TPath zipFile2 = new TPath(cimZipFile);
        try (Stream<Path> stream = Files.list(zipFile2)) {
            stream.forEach(cimFile -> readRdfIdValues(cimFile, inputFactory, rdfIdValues));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Set<String> skipped = new HashSet<>();

        // anonymize each file of the archive
        Path anonymizedCimZipFile2 = new TPath(anonymizedCimZipFile);
        try (Stream<Path> stream = Files.list(zipFile2)) {
            stream.forEach(cimFile -> {
                Path anonymizedCimFile = anonymizedCimZipFile2.resolve(cimFile.getFileName());
                anonymizeFile(cimFile, anonymizedCimFile, inputFactory, outputFactory, eventFactory, dictionary, rdfIdValues, skipped);
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.logSkipped(skipped);

        saveDic(dictionary, dictionaryFile);
    }
}
