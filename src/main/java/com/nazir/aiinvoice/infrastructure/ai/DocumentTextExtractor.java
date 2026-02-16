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
                log.info("event=txt_extraction path={}", filePath);
                return Files.readString(file.toPath());
            }
            if (lowerPath.endsWith(".pdf")) {
                return extractPdfText(file);
            }
            if (lowerPath.endsWith(".docx")) {
                return extractDocxText(file);
            }
        } catch (IOException e) {
            log.error("Failed to extract text from file: {}", filePath, e);
            throw new RuntimeException("Text extraction failed", e);
        }
        log.warn("event=unsupported_file_type path={}", filePath);
        return null;
    }

    private String extractPdfText(File file) throws IOException {
        try {
            try (PDDocument document = Loader.loadPDF(file)) {
                PDFTextStripper stripper = new PDFTextStripper();
                String text = stripper.getText(document);
                log.info("event=pdf_extracted length={}", text.length());
                return text;
            }
        } catch (IOException e) {
             log.warn("Standard PDF loading failed: {}", e.getMessage());
             try (org.apache.pdfbox.io.RandomAccessReadBufferedFile randomAccessFile = new org.apache.pdfbox.io.RandomAccessReadBufferedFile(file)) {
                 try (PDDocument document = Loader.loadPDF(randomAccessFile)) {
                     PDFTextStripper stripper = new PDFTextStripper();
                     return stripper.getText(document);
                 }
             } catch (IOException ex) {
                 log.error("Legacy PDF loading also failed: {}", ex.getMessage());
                 try {
                     byte[] bytes = Files.readAllBytes(file.toPath());
                     try (PDDocument document = Loader.loadPDF(bytes)) {
                         PDFTextStripper stripper = new PDFTextStripper();
                         return stripper.getText(document);
                     }
                 } catch (Exception exc) {
                     log.error("Byte array loading failed too: {}", exc.getMessage());
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
            log.info("event=docx_extracted length={}", text.length());
            return text;
        }
    }
}
