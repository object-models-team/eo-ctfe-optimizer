package eo;

import com.jcabi.xml.XMLDocument;
import org.cactoos.io.InputOf;
import org.cactoos.io.OutputTo;
import org.eolang.parser.Syntax;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.CONTINUE;

public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 1 || args[0].isEmpty())
            throw new RuntimeException("EO source directory has to be set.");

        final Path sourceDir = Paths.get(args[0]);
        final Path finalDir = sourceDir.resolve("../" + sourceDir.getFileName() + "-optimized");

        try {
            Files.createDirectory(finalDir);
        } catch (FileAlreadyExistsException e) {
            System.out.println("Optimized directory already exists.");
        }
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attr) throws IOException {
                System.out.println("Found file " + file);

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new Syntax(
                        file.getFileName().toString(),
                        new InputOf(file),
                        new OutputTo(baos)
                ).parse();
                XMLDocument xml = new XMLDocument(baos.toString());

                System.out.println("Parsed file " + file);
                System.out.println(xml.toString());

                String finalXml = xml.toString().replace("<o base=\"int\" data=\"bytes\" line=\"5\" pos=\"6\">00 00 00 00 00 00 00 02</o>\n" +
                        "               <o base=\".times\" line=\"5\" method=\"\" pos=\"7\">\n" +
                        "                  <o base=\"int\" data=\"bytes\" line=\"5\" pos=\"14\">00 00 00 00 00 00 00 02</o>\n" +
                        "               </o>", "<o base=\"int\" data=\"bytes\" line=\"5\" pos=\"6\">00 00 00 00 00 00 00 04</o>").replace("2.times 2", "4");

                Files.write(finalDir.resolve(sourceDir.relativize(file)).resolveSibling(file.getFileName().toString().replace(".eo", ".xmir")), finalXml.getBytes());

                return CONTINUE;
            }
        });
    }
}