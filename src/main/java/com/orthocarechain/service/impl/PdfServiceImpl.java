package com.orthocarechain.service.impl;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.orthocarechain.dto.response.ReportResponse;
import com.orthocarechain.dto.response.PrescriptionResponse;
import com.orthocarechain.dto.response.MedicalImageResponse;
import com.orthocarechain.enums.SeverityLevel;
import com.orthocarechain.exception.ResourceNotFoundException;
import com.orthocarechain.exception.UnauthorizedAccessException;
import com.orthocarechain.repository.PatientRepository;
import com.orthocarechain.repository.DoctorRepository;
import com.orthocarechain.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfServiceImpl {

    private final ReportServiceImpl reportService;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;

    // ── Brand Colors ──────────────────────────────────────────────────────────
    private static final BaseColor BRAND_TEAL    = new BaseColor(15,  188, 173);
    private static final BaseColor BRAND_DARK    = new BaseColor(13,  20,  36);
    private static final BaseColor BRAND_NAVY    = new BaseColor(26,  39,  68);
    private static final BaseColor LABEL_BG      = new BaseColor(232, 250, 249);
    private static final BaseColor TABLE_ALT     = new BaseColor(248, 253, 253);
    private static final BaseColor RX_BG         = new BaseColor(236, 253, 252);
    private static final BaseColor RX_BORDER     = new BaseColor(15,  188, 173);
    private static final BaseColor IMG_HEADER_BG = new BaseColor(15,  188, 173);
    private static final BaseColor FOOTER_COLOR  = new BaseColor(15,  188, 173);
    private static final BaseColor DIVIDER_COLOR = new BaseColor(15,  188, 173);
    private static final BaseColor TEXT_DARK     = new BaseColor(15,  30,  50);
    private static final BaseColor TEXT_MID      = new BaseColor(55,  65,  81);

    // ── Severity Colors ───────────────────────────────────────────────────────
    private static final BaseColor SEV_CRITICAL  = new BaseColor(220, 38,  38);
    private static final BaseColor SEV_SEVERE    = new BaseColor(234, 88,  12);
    private static final BaseColor SEV_MODERATE  = new BaseColor(202, 138, 4);
    private static final BaseColor SEV_MILD      = new BaseColor(5,   150, 105);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font TITLE_FONT      = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD,   BaseColor.WHITE);
    private static final Font SUBTITLE_FONT   = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, new BaseColor(164, 232, 228));
    private static final Font REPORT_ID_FONT  = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD,   BaseColor.WHITE);
    private static final Font REPORT_DT_FONT  = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, new BaseColor(164, 232, 228));
    private static final Font SECTION_FONT    = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.WHITE);
    private static final Font BODY_FONT       = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, TEXT_DARK);
    private static final Font LABEL_FONT      = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(10, 80, 75));
    private static final Font SMALL_FONT      = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, new BaseColor(120, 120, 120));
    private static final Font SEV_BADGE_FONT  = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.WHITE);
    private static final Font RX_TITLE_FONT   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   new BaseColor(10, 80, 75));
    private static final Font RX_BODY_FONT    = new Font(Font.FontFamily.HELVETICA,  9, Font.NORMAL, TEXT_MID);
    private static final Font IMG_HEAD_FONT   = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD,   BaseColor.WHITE);
    private static final Font FOOTER_FONT     = new Font(Font.FontFamily.HELVETICA,  8, Font.ITALIC, FOOTER_COLOR);

    // row counter for alternating table rows
    private int rowCounter = 0;

    // ─────────────────────────────────────────────────────────────────────────
    public byte[] generateReportPdf(Long reportId) {
        UserDetailsImpl currentUser = getCurrentUser();
        ReportResponse report = reportService.getReportById(reportId);

        if ("ROLE_PATIENT".equals(currentUser.getRole())) {
            var patient = patientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found."));
            if (!report.getPatientId().equals(patient.getId())) {
                throw new UnauthorizedAccessException("You can only download your own reports.");
            }
        }

        try {
            Document document = new Document(PageSize.A4, 50, 50, 60, 60);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, baos);

            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    addPageFooter(writer, document);
                }
            });

            document.open();
            addReportContent(document, report);
            document.close();

            log.info("PDF generated for Report ID: {}", reportId);
            return baos.toByteArray();

        } catch (DocumentException e) {
            log.error("Error generating PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF report.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void addReportContent(Document document, ReportResponse report) throws DocumentException {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMMM yyyy");

        // ══ HEADER BANNER ════════════════════════════════════════════════════
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3, 1.2f});
        header.setSpacingAfter(20);

        // Left cell — dark navy with logo text
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setBackgroundColor(BRAND_DARK);
        leftCell.setPadding(18);
        leftCell.setPaddingLeft(22);

        Paragraph logoText = new Paragraph("OrthoCareChain", TITLE_FONT);
        Paragraph tagline  = new Paragraph("Orthopedic Healthcare Management System", SUBTITLE_FONT);
        tagline.setSpacingBefore(3);
        leftCell.addElement(logoText);
        leftCell.addElement(tagline);
        header.addCell(leftCell);

        // Right cell — teal with report id + date
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setBackgroundColor(BRAND_TEAL);
        rightCell.setPadding(18);
        rightCell.setPaddingRight(22);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        rightCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph reportIdPara = new Paragraph("Report #" + report.getId(), REPORT_ID_FONT);
        reportIdPara.setAlignment(Element.ALIGN_RIGHT);
        rightCell.addElement(reportIdPara);

        if (report.getVisitDate() != null) {
            Paragraph datePara = new Paragraph(report.getVisitDate().format(fmt), REPORT_DT_FONT);
            datePara.setAlignment(Element.ALIGN_RIGHT);
            datePara.setSpacingBefore(4);
            rightCell.addElement(datePara);
        }
        header.addCell(rightCell);
        document.add(header);

        // ══ VISIT INFORMATION ════════════════════════════════════════════════
        addSectionHeader(document, "Visit Information");
        rowCounter = 0;
        PdfPTable visitTable = new PdfPTable(2);
        visitTable.setWidthPercentage(100);
        visitTable.setSpacingAfter(6);
        addTableRow(visitTable, "Visit Date",    report.getVisitDate()     != null ? report.getVisitDate().format(fmt)     : "N/A");
        addTableRow(visitTable, "Next Visit",    report.getNextVisitDate() != null ? report.getNextVisitDate().format(fmt) : "Not scheduled");
        addTableRow(visitTable, "Patient",       report.getPatientName());
        addTableRow(visitTable, "Doctor",        "Dr. " + report.getDoctorName());
        addTableRow(visitTable, "Specialization",report.getDoctorSpecialization());
        document.add(visitTable);

        // ══ DIAGNOSIS ════════════════════════════════════════════════════════
        addSectionHeader(document, "Diagnosis & Assessment");
        rowCounter = 0;
        PdfPTable diagTable = new PdfPTable(2);
        diagTable.setWidthPercentage(100);
        diagTable.setSpacingAfter(6);
        addTableRow(diagTable, "Diagnosis", report.getDiagnosis());
        document.add(diagTable);

        // Severity colored badge
        addSeverityRow(document, report.getSeverityLevel());

        if (notBlank(report.getDiagnosisDetails())) {
            addSubHeader(document, "Details:");
            addBodyBlock(document, report.getDiagnosisDetails());
        }
        if (notBlank(report.getTreatmentPlan())) {
            addSubHeader(document, "Treatment Plan:");
            addBodyBlock(document, report.getTreatmentPlan());
        }
        if (notBlank(report.getDoctorNotes())) {
            addSubHeader(document, "Doctor's Notes:");
            addBodyBlock(document, report.getDoctorNotes());
        }

        // ══ PRESCRIPTIONS ════════════════════════════════════════════════════
        if (report.getPrescriptions() != null && !report.getPrescriptions().isEmpty()) {
            addSectionHeader(document, "Prescriptions  (" + report.getPrescriptions().size() + ")");

            for (int i = 0; i < report.getPrescriptions().size(); i++) {
                PrescriptionResponse rx = report.getPrescriptions().get(i);
                addPrescriptionCard(document, rx, i + 1, fmt);
            }
        }

        // ══ MEDICAL IMAGES ═══════════════════════════════════════════════════
        if (report.getImages() != null && !report.getImages().isEmpty()) {
            addSectionHeader(document, "Medical Images  (" + report.getImages().size() + ")");

            PdfPTable imgTable = new PdfPTable(3);
            imgTable.setWidthPercentage(100);
            imgTable.setWidths(new float[]{1.2f, 1.8f, 3f});
            imgTable.setSpacingAfter(8);
            addImgHeaderCell(imgTable, "Type");
            addImgHeaderCell(imgTable, "Body Part");
            addImgHeaderCell(imgTable, "Description");

            boolean alt = false;
            for (MedicalImageResponse img : report.getImages()) {
                BaseColor bg = alt ? TABLE_ALT : BaseColor.WHITE;
                addImgDataCell(imgTable, img.getImageType()   != null ? img.getImageType().name() : "N/A", bg);
                addImgDataCell(imgTable, img.getBodyPart()    != null ? img.getBodyPart()          : "—",  bg);
                addImgDataCell(imgTable, img.getDescription() != null ? img.getDescription()       : "—",  bg);
                alt = !alt;
            }
            document.add(imgTable);
        }

        // ══ FOLLOW-UP ════════════════════════════════════════════════════════
        if (notBlank(report.getFollowUpInstructions())) {
            addSectionHeader(document, "Follow-Up Instructions");
            addBodyBlock(document, report.getFollowUpInstructions());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Teal filled section header bar */
    private void addSectionHeader(Document document, String text) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingBefore(16);
        t.setSpacingAfter(4);

        PdfPCell cell = new PdfPCell(new Phrase("  " + text, SECTION_FONT));
        cell.setBackgroundColor(BRAND_TEAL);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(9);
        cell.setPaddingLeft(14);
        t.addCell(cell);
        document.add(t);
    }

    /** Bold label line for inline sub-sections */
    private void addSubHeader(Document document, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, LABEL_FONT);
        p.setSpacingBefore(10);
        p.setSpacingAfter(4);
        document.add(p);
    }

    /** Indented body text block */
    private void addBodyBlock(Document document, String text) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        t.setSpacingAfter(6);

        PdfPCell cell = new PdfPCell(new Phrase(text, BODY_FONT));
        cell.setBorder(Rectangle.LEFT);
        cell.setBorderColor(BRAND_TEAL);
        cell.setBorderWidthLeft(3);
        cell.setBackgroundColor(new BaseColor(245, 253, 252));
        cell.setPadding(10);
        cell.setPaddingLeft(14);
        t.addCell(cell);
        document.add(t);
    }

    /** Alternating teal-tint / white table rows */
    private void addTableRow(PdfPTable table, String label, String value) {
        boolean even = (rowCounter++ % 2 == 0);
        BaseColor bg = even ? LABEL_BG : BaseColor.WHITE;

        PdfPCell lc = new PdfPCell(new Phrase(label, LABEL_FONT));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setBackgroundColor(bg);
        lc.setPadding(9);
        lc.setPaddingLeft(14);

        PdfPCell vc = new PdfPCell(new Phrase(value != null ? value : "N/A", BODY_FONT));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setBackgroundColor(bg);
        vc.setPadding(9);

        table.addCell(lc);
        table.addCell(vc);
    }

    /** Colored severity badge row */
    private void addSeverityRow(Document document, SeverityLevel level) throws DocumentException {
        BaseColor sevColor = getSeverityColor(level);
        String    sevText  = level != null ? level.name() : "N/A";

        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setSpacingAfter(6);

        PdfPCell lc = new PdfPCell(new Phrase("Severity", LABEL_FONT));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setBackgroundColor(LABEL_BG);
        lc.setPadding(9);
        lc.setPaddingLeft(14);

        // Badge inside the value cell
        PdfPCell vc = new PdfPCell();
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setBackgroundColor(LABEL_BG);
        vc.setPadding(6);

        PdfPTable badge = new PdfPTable(1);
        badge.setTotalWidth(90);
        badge.setLockedWidth(true);
        PdfPCell bc = new PdfPCell(new Phrase("  " + sevText + "  ", SEV_BADGE_FONT));
        bc.setBackgroundColor(sevColor);
        bc.setBorder(Rectangle.NO_BORDER);
        bc.setPadding(5);
        bc.setHorizontalAlignment(Element.ALIGN_CENTER);
        badge.addCell(bc);
        vc.addElement(badge);

        t.addCell(lc);
        t.addCell(vc);
        document.add(t);
    }

    /** Prescription card with left teal accent border */
    private void addPrescriptionCard(Document document, PrescriptionResponse rx, int num,
                                     DateTimeFormatter fmt) throws DocumentException {
        PdfPTable card = new PdfPTable(1);
        card.setWidthPercentage(100);
        card.setSpacingBefore(10);
        card.setSpacingAfter(4);

        // Card header strip
        PdfPCell headCell = new PdfPCell(new Phrase("  Prescription " + num + ":  " + rx.getDrugName(), RX_TITLE_FONT));
        headCell.setBackgroundColor(RX_BG);
        headCell.setBorder(Rectangle.LEFT | Rectangle.TOP | Rectangle.RIGHT);
        headCell.setBorderColor(RX_BORDER);
        headCell.setBorderWidthLeft(4);
        headCell.setBorderWidthTop(1);
        headCell.setBorderWidthRight(1);
        headCell.setPadding(10);
        card.addCell(headCell);

        // Details inner table
        PdfPTable inner = new PdfPTable(4);
        inner.setWidths(new float[]{1.2f, 1.8f, 1.2f, 1.8f});
        inner.setWidthPercentage(100);

        addRxPair(inner, "Drug",         rx.getDrugName());
        addRxPair(inner, "Dosage",       rx.getDosage());
        addRxPair(inner, "Frequency",    rx.getFrequency());
        addRxPair(inner, "Duration",     rx.getDuration());

        if (rx.getInstructions() != null && !rx.getInstructions().isBlank()) {
            addRxPair(inner, "Instructions", rx.getInstructions());
            addRxPair(inner, "", "");   // fill empty column
        }
        if (rx.getMedicationStartDate() != null)
            addRxPair(inner, "Start Date", rx.getMedicationStartDate().format(fmt));
        if (rx.getMedicationEndDate() != null)
            addRxPair(inner, "End Date", rx.getMedicationEndDate().format(fmt));

        PdfPCell innerCell = new PdfPCell(inner);
        innerCell.setBorder(Rectangle.LEFT | Rectangle.BOTTOM | Rectangle.RIGHT);
        innerCell.setBorderColor(RX_BORDER);
        innerCell.setBorderWidthLeft(4);
        innerCell.setBorderWidthBottom(1);
        innerCell.setBorderWidthRight(1);
        innerCell.setPadding(0);
        card.addCell(innerCell);

        document.add(card);
    }

    private void addRxPair(PdfPTable table, String label, String value) {
        PdfPCell lc = new PdfPCell(new Phrase(label, new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, new BaseColor(10, 80, 75))));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setBackgroundColor(RX_BG);
        lc.setPadding(7);
        lc.setPaddingLeft(12);

        PdfPCell vc = new PdfPCell(new Phrase(value != null ? value : "—", RX_BODY_FONT));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setBackgroundColor(BaseColor.WHITE);
        vc.setPadding(7);

        table.addCell(lc);
        table.addCell(vc);
    }

    /** Image table header cell */
    private void addImgHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, IMG_HEAD_FONT));
        cell.setBackgroundColor(IMG_HEADER_BG);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(10);
        cell.setPaddingLeft(12);
        table.addCell(cell);
    }

    /** Image table data cell */
    private void addImgDataCell(PdfPTable table, String text, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY_FONT));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(new BaseColor(220, 245, 244));
        cell.setPadding(8);
        cell.setPaddingLeft(12);
        table.addCell(cell);
    }

    /** Map severity enum to its color */
    private BaseColor getSeverityColor(SeverityLevel level) {
        if (level == null) return new BaseColor(150, 150, 150);
        return switch (level) {
            case CRITICAL -> SEV_CRITICAL;
            case SEVERE   -> SEV_SEVERE;
            case MODERATE -> SEV_MODERATE;
            case MILD     -> SEV_MILD;
        };
    }

    /** Page footer with teal italic text */
    private void addPageFooter(PdfWriter writer, Document document) {
        PdfContentByte cb = writer.getDirectContent();
        cb.saveState();

        // Footer line
        cb.setColorStroke(DIVIDER_COLOR);
        cb.setLineWidth(0.6f);
        cb.moveTo(document.leftMargin(), document.bottom() - 4);
        cb.lineTo(document.right(),      document.bottom() - 4);
        cb.stroke();

        Phrase footer = new Phrase(
                "OrthoCareChain  ·  Confidential Medical Record  ·  Page " + writer.getPageNumber(),
                FOOTER_FONT
        );
        ColumnText.showTextAligned(
                cb, Element.ALIGN_CENTER, footer,
                (document.right() - document.left()) / 2 + document.leftMargin(),
                document.bottom() - 16, 0
        );
        cb.restoreState();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private UserDetailsImpl getCurrentUser() {
        return (UserDetailsImpl) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }
}