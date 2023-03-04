package eo;

import com.jcabi.xml.XMLDocument;
import org.apache.commons.lang3.StringUtils;
import org.cactoos.io.InputOf;
import org.cactoos.io.OutputTo;
import org.eolang.parser.Syntax;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.Integer.parseInt;
import static java.nio.file.FileVisitResult.CONTINUE;

public class Main {
    private static String optimizeXmirListing(String xml, Integer a, String sign, Integer b, Integer result) {
        String[] lines = xml.split("\n");
        List<String> finalLines = new ArrayList<>();

        boolean wasSet = false;

        for (String line : lines) {
            if (wasSet) {
                finalLines.add(line);
                continue;
            }
            int numberOfSpaces = line.indexOf(a.toString());
            if (line.contains(a + sign + " " + b)) {
                finalLines.add(StringUtils.repeat(' ', numberOfSpaces) + result);
                wasSet = true;
            } else {
                finalLines.add(line);
            }
        }
        return StringUtils.join(finalLines, "\n");
    }
    private static String optimizeXmir(String xml) {
        String[] lines = xml.split("\n");
        List<String> finalLines = new ArrayList<>();

        int a = 0;
        String sign = "";
        int b = 0;
        int result = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int numberOfSpaces = line.indexOf("<");
            if (!sign.equals("")) {
                finalLines.add(line);
                continue;
            }
            try {
                Node element = new XMLDocument(line.trim()).node().getFirstChild();
                NamedNodeMap attributes = element.getAttributes();
                boolean isO = element.getNodeName().equals("o");
                boolean isInt = attributes.getNamedItem("base").getNodeValue().equals("int");
                a = parseInt(element.getFirstChild().getNodeValue().replaceAll(" ", ""), 16);
                if (isO && isInt) {
                    String lineP1 = lines[i + 1];
                    Node elementP1 = new XMLDocument(lineP1.trim() + "</o>").node().getFirstChild();
                    NamedNodeMap attributesP1 = elementP1.getAttributes();
                    boolean isOP1 = elementP1.getNodeName().equals("o");
                    boolean isPlusP1 = attributesP1.getNamedItem("base").getNodeValue().equals(".plus");
                    boolean isMinusP1 = attributesP1.getNamedItem("base").getNodeValue().equals(".minus");
                    boolean isTimesP1 = attributesP1.getNamedItem("base").getNodeValue().equals(".times");
                    boolean isDivP1 = attributesP1.getNamedItem("base").getNodeValue().equals(".div");
                    sign = attributesP1.getNamedItem("base").getNodeValue();
                    if (isOP1 && (isPlusP1 || isMinusP1 || isTimesP1 || isDivP1)) {
                        String lineP2 = lines[i + 2];
                        Node elementP2 = new XMLDocument(lineP2.trim()).node().getFirstChild();
                        NamedNodeMap attributesP2 = elementP2.getAttributes();
                        boolean isOP2 = elementP2.getNodeName().equals("o");
                        boolean isIntP2 = attributesP2.getNamedItem("base").getNodeValue().equals("int");
                        b = parseInt(elementP2.getFirstChild().getNodeValue().replaceAll(" ", ""), 16);
                        if (isOP2 && isIntP2) {
                            result = isPlusP1
                                    ? a + b
                                    : isMinusP1
                                    ? a - b
                                    : isTimesP1
                                    ? a * b
                                    : a / b;
                            Node newElement = element.cloneNode(false);
                            String paddedValue = StringUtils.leftPad(Integer.toHexString(result), 16, "0");
                            String paddedSpacedValue = paddedValue.substring(0, 2) +
                                    " " +
                                    paddedValue.substring(2, 4) +
                                    " " +
                                    paddedValue.substring(4, 6) +
                                    " " +
                                    paddedValue.substring(6, 8) +
                                    " " +
                                    paddedValue.substring(8, 10) +
                                    " " +
                                    paddedValue.substring(10, 12) +
                                    " " +
                                    paddedValue.substring(12, 14) +
                                    " " +
                                    paddedValue.substring(14, 16);
                            newElement.setTextContent(paddedSpacedValue);

                            finalLines.add(StringUtils.repeat(' ', numberOfSpaces) + new XMLDocument(newElement).toString().replace("\n", ""));
                            i += 3;
                            continue;
                        }
                    }
                }
            } catch(Exception ignored) {

            }
            finalLines.add(line);
        }
        String intermediateResult = StringUtils.join(finalLines, "\n");
        return Objects.equals(sign, "")
                ? intermediateResult
                : optimizeXmirListing(intermediateResult, a, sign, b, result);
    }
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

                String before = xml.toString();
                String after;
                do {
                    after = optimizeXmir(before);
                    if (before.equals(after)) {
                        break;
                    } else {
                        before = after;
                    }
                } while(true);

                Files.write(finalDir.resolve(sourceDir.relativize(file)).resolveSibling(file.getFileName().toString().replace(".eo", ".xmir")), after.getBytes());

                return CONTINUE;
            }
        });
    }
}