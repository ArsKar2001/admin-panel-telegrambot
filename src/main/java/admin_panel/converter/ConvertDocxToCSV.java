package admin_panel.converter;

import lombok.Getter;
import lombok.var;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConvertDocxToCSV {
    private static final Logger LOG = Logger.getLogger(ConvertDocxToCSV.class);
    private final File inputFile;
    private final List<List<String>> pages = new ArrayList<>();
    @Getter
    public List<List<String>> splitPages = new ArrayList<>();
    private String leftGroupName;
    private String rightGroupName;

    public ConvertDocxToCSV(File inputFile) {
        this.inputFile = inputFile;
    }

    public void convert() {
        LOG.info("[STARTED] Conversion process.");
        File txtFile = parserDocxInText_ApachePOI(inputFile);
        onPages(txtFile);
        checkingPages();
        /*....*/
    }

    private File parserDocxInText_ApachePOI(File f) {
        String newFileName = "parser_"+inputFile.getName();
        String newPath = f.getAbsoluteFile().getParent()+File.separator+ getOutputFile(newFileName, "txt");
        File outputFile = new File(newPath);

        try (FileInputStream stream = new FileInputStream(f)) {
            XWPFDocument docxFile = new XWPFDocument(OPCPackage.open(stream));
            XWPFWordExtractor extractor = new XWPFWordExtractor(docxFile);
            try (FileWriter writer = new FileWriter(outputFile)) {
                String text = extractor.getText();
                writer.write(text);
            }
            LOG.debug("Считали данные из "+f.getName()+" в "+outputFile.getName()+"...");
        } catch (IOException | InvalidFormatException e) {
            LOG.error(e.getMessage());
        }
        return outputFile;
    }

    private void onPages(File txtFile) {
        List<String> page = new ArrayList<>();
        try (Scanner scanner = new Scanner(txtFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if(!"".equals(line)) {
                    page.add(line);
                } else {
                    pages.add(page);
                    page = new ArrayList<>();
                }
            }
        } catch (FileNotFoundException e) {
            LOG.error(e);
        }
        correctPages();
    }

    private void correctPages() {
        pages.removeIf(pg -> pg.size() < 10);
        for (var page : pages) {
            leftGroupName = "";
            rightGroupName = "";
            page.remove(1);
            splitPage(page);
        }
    }

    private void splitPage(List<String> page) {
        List<String> numbers = new ArrayList<>(Arrays.asList(
                "I", "I-II", "I-III", "I-IV", "I-V", "I-VI",
                "II", "II-III", "II-IV", "II-V", "II-VI",
                "III", "III-IV", "III-V", "III-VI",
                "IV", "IV-V", "IV-VI",
                "V", "V-VI",
                "VI"
        ));
        List<String> days = new ArrayList<>(Arrays.asList(
                "Понедельник",
                "Вторник",
                "Среда",
                "Четверг",
                "Пятница"
        ));
        List<String> leftPage = new ArrayList<>();
        List<String> rightPage = new ArrayList<>();
        StringBuilder leftStringBuilder;
        StringBuilder rightStringBuilder;
        for (var line : page) {
            leftStringBuilder = new StringBuilder();
            rightStringBuilder = new StringBuilder();
            String[] splitLine = line.split("\t");

            try {
                if (page.get(0).equals(line)) {
                    leftGroupName = splitLine[0].split("\\s")[1];
                    rightGroupName = splitLine[1].split("\\s")[1];
                    continue;
                }
                leftStringBuilder.append(leftGroupName).append(";").append(splitLine[1]);
                for (int i = 2; i < splitLine.length; i++) {
                    if (days.contains(splitLine[i])) continue;
                    if (numbers.contains(splitLine[i])) {
                        rightStringBuilder.append(rightGroupName).append(";");
                        for (int j = i; j < splitLine.length; j++)
                            rightStringBuilder.append(splitLine[j]).append(";");
                        break;
                    }
                    leftStringBuilder.append(";").append(splitLine[i]);
                }
            } catch (Exception e) {
                LOG.warn(e.getMessage()+" "+line);
            }
            leftPage.add(leftStringBuilder.toString().trim());
            rightPage.add(rightStringBuilder.toString().trim());
        }
        splitPages.add(leftPage);
        splitPages.add(rightPage);
    }

    private String correct(String line) {
        Pattern pattern = Pattern.compile("\t.+?\t", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        while (matcher.find())
            line.replace(line.substring(matcher.start(), matcher.end()), ";");
        return line;
    }

    private void checkingPages() {
        var pgs = splitPages;
        for (var page : pgs) {
            page.removeIf(line -> line.equals("") || line.length() < 5);
        }
        pgs.removeIf(pg -> pg.size() == 0);
        splitPages = pgs;
    }

    private static String getOutputFile(String fileName, String suffix) {
        if (fileName.length() >= 5 && fileName.toLowerCase().endsWith(".pdf")) {
            return fileName.substring(0, fileName.length() - 4) + "." + suffix;
        } else {
            return fileName + "." + suffix;
        }
    }
}
