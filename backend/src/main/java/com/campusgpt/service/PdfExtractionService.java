package com.campusgpt.service;

import com.campusgpt.exception.ApiException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfExtractionService {
  public record PageText(int pageNumber, String text) {}

  public List<PageText> extract(File file) {
    try (PDDocument pdf = Loader.loadPDF(file)) {
      if (pdf.isEncrypted()) throw new ApiException(HttpStatus.BAD_REQUEST, "Encrypted PDFs are not supported.");
      List<PageText> pages = new ArrayList<>();
      PDFTextStripper stripper = new PDFTextStripper();
      for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
        stripper.setStartPage(i);
        stripper.setEndPage(i);
        String text = stripper.getText(pdf).trim();
        if (!text.isBlank()) pages.add(new PageText(i, text));
      }
      if (pages.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "No extractable text found. OCR is not supported yet.");
      return pages;
    } catch (ApiException e) {
      throw e;
    } catch (Exception e) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "PDF could not be read: " + e.getMessage());
    }
  }
}
