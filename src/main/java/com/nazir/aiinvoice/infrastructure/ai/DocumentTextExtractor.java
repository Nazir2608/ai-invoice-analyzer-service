package com.nazir.aiinvoice.infrastructure.ai;

import com.nazir.aiinvoice.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;

@Component
@Slf4j
public class DocumentTextExtractor {

    public String extractText(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new ResourceNotFoundException("File not found: " + filePath);
        }
        String lowerPath = filePath.toLowerCase();
        try {
            if (lowerPath.endsWith(".txt")) {
                log.info("event=text_extraction_txt path={}", filePath);
                return Files.readString(file.toPath());
            }
            if (lowerPath.endsWith(".pdf")) {
                return extractPdfText(file);
            }
            if (lowerPath.endsWith(".docx")) {
                return extractDocxText(file);
            }
        } catch (IOException e) {
            log.error("event=text_extraction_failed path={}", filePath, e);
            throw new IllegalStateException("Text extraction failed", e);
        }
        log.warn("event=unsupported_file_type path={}", filePath);
        return null;
    }

    private String extractPdfText(File file) throws IOException {
        try {
            try (PDDocument document = Loader.loadPDF(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                log.info("event=text_extraction_pdf length={}", text.length());
                return text;
            }
        } catch (IOException e) {
             log.warn("event=pdf_standard_load_failed message={}", e.getMessage());
             try (org.apache.pdfbox.io.RandomAccessReadBufferedFile randomAccessFile = new org.apache.pdfbox.io.RandomAccessReadBufferedFile(file)) {
                 try (PDDocument document = Loader.loadPDF(randomAccessFile)) {
                     PDFTextStripper stripper = new PDFTextStripper();
                     String text = stripper.getText(document);
                     log.info("event=text_extraction_pdf_random_access length={}", text.length());
                     return text;
                 }
             } catch (IOException ex) {
                 log.error("event=pdf_random_access_load_failed message={}", ex.getMessage());
                 try {
                     byte[] bytes = Files.readAllBytes(file.toPath());
                     try (PDDocument document = Loader.loadPDF(bytes)) {
                         PDFTextStripper stripper = new PDFTextStripper();
                         String text = stripper.getText(document);
                         log.info("event=text_extraction_pdf_bytes length={}", text.length());
                         return text;
                     }
                 } catch (Exception exc) {
                     log.error("event=pdf_bytes_load_failed message={}", exc.getMessage());
                     throw ex;
                 }
             }
        }
    }

    private String extractDocxText(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String text = extractor.getText();
            log.info("event=text_extraction_docx length={}", text.length());
            return text;
        }
    }
}
