package dev.pcvolkmer.onco.obds2toobds3;

import org.apache.commons.cli.*;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.io.IOUtils;

import de.basisdatensatz.obds.v2.ADTGEKID;
import dev.pcvolkmer.onko.obds2to3.ObdsMapper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class Application {
    public static void main(String[] args) throws Exception {
        final var options = new Options();
        options.addOption(
                Option.builder("i").longOpt("input").argName("input").hasArg().desc("Input file").converter(File::new).build());
        options.addOption(
                Option.builder("o").longOpt("output").argName("output").hasArg().desc("Output file").converter(File::new).build());
        options.addOption(
                Option.builder().longOpt("ignore-unmappable").desc("Ignore unmappable messages and patients").build());
        options.addOption(
                Option.builder("v").desc("Show errors").build());
        options.addOption(
                Option.builder("vv").desc("Show exceptions and stack traces").build());

        final var parsedCliArgs = DefaultParser.builder().build().parse(options, args);

        if (parsedCliArgs.hasOption("h") || !parsedCliArgs.hasOption("i") || !parsedCliArgs.hasOption("o")) {
            new HelpFormatter().printHelp("java -jar obds2-to-obds3-app.jar --input <input file> --output <output file>", options);
        } else {
            try {
                var input = Paths.get(parsedCliArgs.getOptionValue("i"));
                var output = Paths.get(parsedCliArgs.getOptionValue("o"));

                var mapper = ObdsMapper.builder()
                        .ignoreUnmappable(parsedCliArgs.hasOption("ignore-unmappable"))
                        .build();
                var bomInputStream = BOMInputStream.builder().setInputStream(Files.newInputStream(input)).get();

                var inputObj = mapper.readValue(IOUtils.toString(bomInputStream, StandardCharsets.UTF_8),
                        ADTGEKID.class);
                var mappedString = mapper.writeMappedXmlString(inputObj);

                if (parsedCliArgs.hasOption("v") || parsedCliArgs.hasOption("vv")) {
                    var outputObj = mapper.map(inputObj);
                    var inPatientsCount = inputObj.getMengePatient().getPatient().size();
                    var inMessagesCount = inputObj.getMengePatient().getPatient().stream()
                            .mapToLong(patient -> patient.getMengeMeldung().getMeldung().size()).sum();
                    var outPatientsCount = outputObj.getMengePatient().getPatient().size();
                    var outMessageCount = outputObj.getMengePatient().getPatient().stream()
                            .mapToLong(patient -> patient.getMengeMeldung().getMeldung().size()).sum();

                    System.err.println(String.format("Patienten >  In: %d, Out: %d ", inPatientsCount, outPatientsCount));
                    System.err.println(String.format("Meldungen >  In: %d, Out: %d ", inMessagesCount, outMessageCount));
                }

                Files.writeString(output, mappedString);
            } catch (Exception e) {
                System.err.println("Konvertierung fehlgeschlagen");
                if (parsedCliArgs.hasOption("v")) {
                    System.err.println(e.getLocalizedMessage());
                    System.err.println(e.getCause().getLocalizedMessage());
                } else if (parsedCliArgs.hasOption("vv")) {
                    throw e;
                }
            }

        }

    }
}
