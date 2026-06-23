package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PdfReportService {
    public byte[] create(String title, List<String> lines) {
        List<String> objects = new ArrayList<>();
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 12 Tf\n50 790 Td\n14 TL\n")
                .append("(").append(escape(title)).append(") Tj\nT*\n");
        int count = 0;
        for (String line : lines) {
            if (count++ >= 48) {
                content.append("(Resultats limites aux 48 premieres lignes.) Tj\nT*\n");
                break;
            }
            content.append("(").append(escape(line)).append(") Tj\nT*\n");
        }
        content.append("ET\n");

        objects.add("<< /Type /Catalog /Pages 2 0 R >>");
        objects.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objects.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>");
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        objects.add("<< /Length " + content.toString().getBytes(StandardCharsets.ISO_8859_1).length + " >>\nstream\n"
                + content + "endstream");

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
            pdf.append(i + 1).append(" 0 obj\n").append(objects.get(i)).append("\nendobj\n");
        }
        int xref = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n")
                .append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n")
                .append("startxref\n").append(xref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace('\n', ' ')
                .replace('\r', ' ');
    }
}
