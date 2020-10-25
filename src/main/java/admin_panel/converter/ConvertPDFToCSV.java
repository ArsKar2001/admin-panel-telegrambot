package admin_panel.converter;

import com.giaybac.traprange.PDFTableExtractor;
import com.giaybac.traprange.entity.Table;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
     * Конвертирование PDF в CSV через API pdfBox. https://pdfbox.apache.org/
     * @param f
     * @return
     */
    private File convert_pdfbox(File f) {
        String newFileName = "convert_"+inputFile.getName();
        File outputFile = new File(getOutputFileName(newFileName, "csv"));
        try {
            PDFParser pdfParser = new PDFParser(new RandomAccessFile(f, "r"));
            pdfParser.parse();
            COSDocument cosDoc = pdfParser.getDocument();
            PDFTextStripper pdfStripper = new PDFTextStripper();
            PDDocument pdDoc = new PDDocument(cosDoc);
            pdfStripper.writeText(pdDoc, new FileWriter(outputFile));
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        LOG.debug("Convert the "+ f.getName()+" to "+ outputFile.getName());
        return outputFile;
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
        String newFileName = "convert_"+inputFile.getName();
        File outputFile = new File(getOutputFileName(newFileName, "pdf"));

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
                outDoc.save(outputFile);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        LOG.debug("split pages to " + f.getName()+"; new "+ outputFile.getName());
        return outputFile;
    }

    /**
     * Конвертирование PDF в CSV через API traprange: https://github.com/thoqbk/traprange
     * @param inputFile файл PDF
     * @return
     */
    private File convertPDFtoCSV_traprange(File inputFile) {
        PDFTableExtractor extractor = (new PDFTableExtractor())
                .setSource(inputFile)
                .exceptLine(new int[]{0, 1});
        String newFileName = "convert_"+inputFile.getName();
        File outputFile = new File(getOutputFileName(newFileName, "csv"));

        List<Table> tables = extractor.extract();
        try (Writer writer = new FileWriter(outputFile)) {
            for (Table table : tables) {
                writer.write(table.toString());
                writer.write("\n");
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        LOG.debug("Convert the "+ inputFile.getName()+" to "+ outputFile.getName());
        return outputFile;
    }


    /**
     * Конвертирование PDF через сервис: https://pdftables.com
     * Работает хорошо, но платный :(
     * @param inputFile PDF файл
     * @param format в какой формат конвертируем
     * @param api_key
     * @return
     */
    private File getConvertFile(File inputFile, String format, String api_key) {
        String newFileName = "convert_"+inputFile.getName();

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
                    final File outputFile = new File(getOutputFileName(newFileName, format));
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
    }

    private static String getOutputFileName(String pdfFilename, String suffix) {
        if (pdfFilename.length() >= 5 && pdfFilename.toLowerCase().endsWith(".pdf")) {
            return pdfFilename.substring(0, pdfFilename.length() - 4) + "." + suffix;
        } else {
            return pdfFilename + "." + suffix;
        }
    }
}
