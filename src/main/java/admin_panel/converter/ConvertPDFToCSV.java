package admin_panel.converter;

import com.giaybac.traprange.PDFTableExtractor;
import com.giaybac.traprange.entity.Table;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Rectangle;
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
        File txtFile = convertPDFtoCSV(cutPdfFile);
    }

    private File convertPDFtoCSV(File inputFile) {
        String newFileName = replaceSuffix(inputFile.getName(), ".txt");
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

                    // Оберезаем лечую часть PDF
                    bBox.setLowerLeftX(x1 + 60f);
                    bBox.setUpperRightX(x2 / 2f);
                    bBox.setLowerLeftY(y1 + 10f);
                    bBox.setUpperRightY(y2 - 40f);
                    page.setCropBox(bBox);
                    outDoc.importPage(page);

                    // Оберезаем правую часть PDF
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

    private File cutPdfPage_iText(File f) {
        String newFileName = "cut_" + f.getName();
        String newPath = f.getAbsoluteFile().getParent() + File.separator + newFileName;
        File outFile = new File(newPath);

        try {
            PdfReader reader = new PdfReader(new FileInputStream(f));
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(outFile));

            int n = reader.getNumberOfPages();
            PdfDictionary page;
            PdfArray media;
            for (int p = 1; p <= n; p++) {
                page = reader.getPageN(p);
                media = page.getAsArray(PdfName.CROPBOX);
                if (media == null) {
                    media = page.getAsArray(PdfName.MEDIABOX);
                }
                float llx = media.getAsNumber(0).floatValue() + 60f;
                float lly = media.getAsNumber(1).floatValue() + 10f;
                float w = media.getAsNumber(2).floatValue() / 2f;
                float h = media.getAsNumber(3).floatValue();
                String command = String.format(
                        "\nq %.2f %.2f %.2f %.2f re W n\nq\n",
                        llx, lly, w, h);
                stamper.getUnderContent(p).setLiteral(command);
                stamper.getOverContent(p).setLiteral("\nQ\nQ\n");
            }
            stamper.close();
            reader.close();
        } catch (IOException | DocumentException e) {
            e.printStackTrace();
        }
        return outFile;
    }

//    private File splitIntoHalfPages(File f)
//    {
//        String newFileName = "split_" + f.getName();
//        String newPath = f.getAbsoluteFile().getParent()+File.separator+newFileName;
//        File outFile = new File(newPath);
//        try (OutputStream targetStream = new FileOutputStream(outFile))
//        {
//            final PdfReader reader = new PdfReader(new FileInputStream(f));
//            Document document = new Document();
//            PdfCopy copy = new PdfCopy(document, targetStream);
//            document.open();
//            PdfArray leftBox;
//            PdfArray rightBox;
//            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
//                PdfDictionary pageN = reader.getPageN(page);
//                Rectangle cropBox = reader.getCropBox(page);
//                leftBox = new PdfArray(new float[]{
//                        cropBox.getLeft() + (cropBox.getRight() * 0.07f),
//                        cropBox.getBottom() - (cropBox.getBottom() * 0.05f),
//                        (cropBox.getLeft() + cropBox.getRight()) / 2.0f,
//                        cropBox.getTop(cropBox.getTop() * 0.065f)
//                });
//                rightBox = new PdfArray(new float[]{
//                        ((cropBox.getLeft() + cropBox.getRight()) / 2.0f) + ((cropBox.getLeft() + cropBox.getRight()) * 0.05f),
//                        cropBox.getBottom() - (cropBox.getBottom() * 0.05f),
//                        cropBox.getRight() - (cropBox.getRight() * 0.022f),
//                        cropBox.getTop(cropBox.getTop() * 0.065f)
//                });
//
//                PdfImportedPage importedPage = copy.getImportedPage(reader, page);
//                pageN.put(PdfName.CROPBOX, leftBox);
//                copy.addPage(importedPage);
//                pageN.put(PdfName.CROPBOX, rightBox);
//                copy.addPage(importedPage);
//            }
//            document.close();
//        } catch (IOException | DocumentException e) {
//            LOG.error(e.getMessage());
//        }
//        LOG.debug("Split page to file "+outFile.getName());
//        return outFile;
//    }

//    @SneakyThrows
//    private void manipulatePdf(File f) {
//        String newFileName = "split_" + f.getName();
//        String newPath = f.getAbsoluteFile().getParent()+File.separator+newFileName;
//        File outFile = new File(newPath);
//        PdfReader reader = new PdfReader(new FileInputStream(f));
//        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(outFile));
//        int n = reader.getNumberOfPages();
//        PdfDictionary page;
//        PdfArray media;
//        for (int p = 1; p <= n; p++) {
//            page = reader.getPageN(p);
//            media = page.getAsArray(PdfName.CROPBOX);
//            if (media == null) {
//                media = page.getAsArray(PdfName.MEDIABOX);
//            }
//            float llx = media.getAsNumber(0).floatValue() + 200;
//            float lly = media.getAsNumber(1).floatValue() + 200;
//            float w = media.getAsNumber(2).floatValue() - media.getAsNumber(0).floatValue() - 400;
//            float h = media.getAsNumber(3).floatValue() - media.getAsNumber(1).floatValue() - 400;
//            String command = String.format(
//                    "\nq %.2f %.2f %.2f %.2f re W n\nq\n",
//                    llx, lly, w, h);
//            stamper.getUnderContent(p).setLiteral(command);
//            stamper.getOverContent(p).setLiteral("\nQ\nQ\n");
//        }
//        stamper.close();
//        reader.close();
//    }

    private void setWidthAndHeightPdfPage(File f) {
        float width = 8.5f * 72;
        float height = 11f * 72;
        float tolerance = 1f;

        PdfReader reader = null;
        try {
            reader = new PdfReader(new FileInputStream(f));

            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                Rectangle cropBox = reader.getCropBox(i);
                float widthToAdd = width - cropBox.getWidth();
                float heightToAdd = height - cropBox.getHeight();
                if (Math.abs(widthToAdd) > tolerance || Math.abs(heightToAdd) > tolerance) {
                    float[] newBoxValues = new float[]{
                            cropBox.getLeft() - widthToAdd / 2,
                            cropBox.getBottom() - heightToAdd / 2,
                            cropBox.getRight() + widthToAdd / 2,
                            cropBox.getTop() + heightToAdd / 2
                    };
                    PdfArray newBox = new PdfArray(newBoxValues);

                    PdfDictionary pageDict = reader.getPageN(i);
                    pageDict.put(PdfName.CROPBOX, newBox);
                    pageDict.put(PdfName.MEDIABOX, newBox);
                }
            }

            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(f));
            stamper.close();
        } catch (IOException | DocumentException e) {
            LOG.error(e.getMessage());
        }
    }

    // Конвертирование через сервис: https://pdftables.com
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
