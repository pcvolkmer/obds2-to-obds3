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
                Option.builder("i").argName("input").hasArg().desc("Input file").converter(File::new).build());
        options.addOption(
                Option.builder("o").argName("output").hasArg().desc("Output file").converter(File::new).build());
        options.addOption(
                Option.builder("v").desc("Show exceptions and stack traces").build());

        final var parsedCliArgs = DefaultParser.builder().build().parse(options, args);

        if (parsedCliArgs.hasOption("h") || !parsedCliArgs.hasOption("input") || !parsedCliArgs.hasOption("output")) {
            new HelpFormatter().printHelp("obds2toobds3 --input <input file> --output <output file>", options);
        } else {
            try {
                var input = Paths.get(parsedCliArgs.getOptionValue("i"));
                var output = Paths.get(parsedCliArgs.getOptionValue("o"));

                var mapper = new ObdsMapper();
                var bomInputStream = BOMInputStream.builder().setInputStream(Files.newInputStream(input)).get();

                var inputObj = mapper.readValue(IOUtils.toString(bomInputStream, StandardCharsets.UTF_8),
                        ADTGEKID.class);
                var mappedString = mapper.writeMappedXmlString(inputObj);

                Files.writeString(output, mappedString);
            } catch (Exception e) {
                System.err.println("Konvertierung fehlgeschlagen");
                if (parsedCliArgs.hasOption("v")) {
                    throw e;
                }
            }

        }

    }
}
