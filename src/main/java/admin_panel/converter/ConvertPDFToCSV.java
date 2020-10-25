package admin_panel.converter;

import com.giaybac.traprange.PDFTableExtractor;
import com.giaybac.traprange.entity.Table;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.*;
import java.util.List;

public class ConvertPDFToCSV {
    private static final Logger LOG = Logger.getLogger(ConvertPDFToCSV.class);

    @Setter
    @Getter
    private String pathToPDFFile;
    @Setter
    @Getter
    private String pathToCSVFile;
    @Setter
    @Getter
    private String string;
    @Getter
    private File inputFile;

    public ConvertPDFToCSV(String pathToPDFFile, String pathToCSVFile) {
        this.pathToPDFFile = pathToPDFFile;
        this.pathToCSVFile = pathToCSVFile;
    }

    public ConvertPDFToCSV(File inputFile) {
        this.inputFile = inputFile;
    }

    public void convert() {
        LOG.info("[STARTED] Conversion process.");
        File cutPdfFile = cutPdfPages_pdfbox(inputFile);
        File txtFile = convertPDFtoCSV_traprange(cutPdfFile);
        /*....*/
    }

    /**
     * Конвертирование PDF в CSV через API traprange: https://github.com/thoqbk/traprange
     * @param inputFile файл PDF
     * @return
     */
    private File convertPDFtoCSV_traprange(File inputFile) {
        String newFileName = replaceSuffix(inputFile.getName(), ".csv");
        newFileName = "convert_" + newFileName;
        String newPath = inputFile.getAbsoluteFile().getParent()+File.separator+newFileName;
        File outFile = new File(newPath);

        PDFTableExtractor extractor = (new PDFTableExtractor())
                .setSource(inputFile)
                .exceptLine(new int[]{0, 1});
        List<Table> tables = extractor.extract();
        try (Writer writer = new FileWriter(outFile)) {
            for (Table table : tables) {
                writer.write(table.toString());
                writer.write("\n");
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        LOG.debug("Convert the "+ inputFile.getName()+" to "+outFile.getName());
        return outFile;
    }

    /**
     * Конвертирование PDF в CSV через API pdfBox. https://pdfbox.apache.org/
     * @param f
     * @return
     */
    private File convert_pdfbox(File f) {
        String newFileName = replaceSuffix(f.getName(), ".txt");
        String newPath = f.getAbsoluteFile().getParent()+File.separator+"convert_"+newFileName;
        File outFile = new File(newPath);
        try {
            PDFParser pdfParser = new PDFParser(new RandomAccessFile(f, "r"));
            pdfParser.parse();
            COSDocument cosDoc = pdfParser.getDocument();
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDDocument pdDoc = new PDDocument(cosDoc);
            pdfStripper.writeText(pdDoc, new FileWriter(outFile));
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        LOG.debug("Convert the "+ f.getName()+" to "+outFile.getName());
        return outFile;
    }

    private static String replaceSuffix(String fileName, String suffix) {
        int index = fileName.indexOf('.');
        if (index != -1) {
            int lastIndex = index;
            while (index != -1) {
                index = fileName.indexOf('.', lastIndex + 1);
                if (index != -1) lastIndex = index;
            }
            return fileName.substring(0, lastIndex) + suffix;
        } else {
            return fileName + "suffix";
        }
    }

    /**
     * Обрезка PDF страницы через API pdfBox (не работает). https://pdfbox.apache.org/
     * @param f
     * @return
     */
    private File cutPdfPages_pdfbox(File f) {
        String newFileName = "cut_" + f.getName();
        String newPath = f.getAbsoluteFile().getParent() + File.separator + newFileName;
        File outFile = new File(newPath);

        try (PDDocument inputDoc = PDDocument.load(f)) {
            try (PDDocument outDoc = new PDDocument()) {
                for (PDPage page : inputDoc.getPages()) {
                    PDRectangle bBox = page.getBBox();
                    float x1 = bBox.getLowerLeftX();
                    float x2 = bBox.getUpperRightX();
                    float y1 = bBox.getLowerLeftY();
                    float y2 = bBox.getUpperRightY();

                    // Оберезаем лечую часть страницы PDF
                    bBox.setLowerLeftX(x1 + 60f);
                    bBox.setUpperRightX(x2 / 2f);
                    bBox.setLowerLeftY(y1 + 10f);
                    bBox.setUpperRightY(y2 - 40f);
                    page.setCropBox(bBox);
                    outDoc.importPage(page);

                    // Оберезаем правую часть страницы PDF
                    bBox.setLowerLeftX(x2 / 2f + 40f);
                    bBox.setUpperRightX(x2 - 10f);
                    bBox.setLowerLeftY(y1 + 10f);
                    bBox.setUpperRightY(y2 - 40f);
                    page.setCropBox(bBox);
                    outDoc.importPage(page);
                }
                outDoc.save(outFile);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        LOG.debug("split pages to " + f.getName()+"; new "+outFile.getName());
        return outFile;
    }

    // Конвертирование PDF через сервис: https://pdftables.com
    /*private File getConvertFile(File inputFile, String format, String api_key) {

        LOG.info("[STARTED] ConvertPDFToCSV https://pdftables.com. File: "+pathToPDFFile+", format to ."+format);
        RequestConfig requestConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build();

        if (!inputFile.canRead()) {
            LOG.error("Can't read input PDF inputFile: \"" + pathToPDFFile + "\"");
        }

        try (CloseableHttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build()) {
            HttpPost httpPost = new HttpPost("https://pdftables.com/api?format=" + format + "&key=" + api_key);
            FileBody fileBody = new FileBody(inputFile);

            HttpEntity httpEntity = MultipartEntityBuilder.create().addPart("f", fileBody).build();
            httpPost.setEntity(httpEntity);

            LOG.debug("Sending request");

            try (CloseableHttpResponse httpResponse = httpClient.execute(httpPost)) {
                if (httpResponse.getStatusLine().getStatusCode() != 200) {
                    LOG.debug(httpResponse.getStatusLine());
                }
                HttpEntity entity = httpResponse.getEntity();
                if (entity != null) {
                    final File outputFile = getNewOutFile(inputFile, "", ".csv");
                    FileUtils.copyToFile(entity.getContent(), outputFile);

                    return outputFile;
                } else {
                    LOG.error("File missing from response");
                }
            }

        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }*/
}
