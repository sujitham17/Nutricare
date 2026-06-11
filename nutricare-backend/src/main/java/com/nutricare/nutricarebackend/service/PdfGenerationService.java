package com.nutricare.nutricarebackend.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.nutricare.nutricarebackend.entity.ConsultationPayment;
import com.nutricare.nutricarebackend.entity.SubscriptionTransaction;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
public class PdfGenerationService {

    private static final Color PRIMARY_COLOR = new Color(0, 95, 115); // Dark Teal
    private static final Color SECONDARY_COLOR = new Color(45, 55, 72); // Slate / Dark Gray
    private static final Color LIGHT_BG = new Color(243, 244, 246); // Light Gray
    private static final Color SUCCESS_COLOR = new Color(16, 185, 129); // Green

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final DateTimeFormatter appointmentDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter appointmentTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a");

    public byte[] generateSubscriptionBillPdf(SubscriptionTransaction transaction, String billNumber) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Font setups
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, PRIMARY_COLOR);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, SECONDARY_COLOR);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, SECONDARY_COLOR);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, SECONDARY_COLOR);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);

            // 1. Header Section
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{3f, 2f});

            // Logo and platform name
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(PdfPCell.NO_BORDER);
            leftCell.addElement(new Paragraph("NutriCare", brandFont));
            leftCell.addElement(new Paragraph("Healthy Eating, Healthy Living", FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY)));
            leftCell.addElement(new Paragraph("support@nutricare.com", FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY)));
            headerTable.addCell(leftCell);

            // Receipt label and metadata
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(PdfPCell.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph rightPara = new Paragraph("INVOICE / RECEIPT\n\n", titleFont);
            rightPara.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(rightPara);

            Paragraph numPara = new Paragraph("Invoice No: " + billNumber, boldFont);
            numPara.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(numPara);

            Paragraph datePara = new Paragraph("Date: " + transaction.getCreatedAt().format(formatter), normalFont);
            datePara.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(datePara);

            Paragraph statusPara = new Paragraph("Status: PAID", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, SUCCESS_COLOR));
            statusPara.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(statusPara);
            headerTable.addCell(rightCell);

            document.add(headerTable);
            document.add(new Paragraph("\n"));

            // Horizontal Separator
            PdfPTable separator = new PdfPTable(1);
            separator.setWidthPercentage(100);
            PdfPCell sepCell = new PdfPCell();
            sepCell.setBorder(PdfPCell.BOTTOM);
            sepCell.setBorderColor(Color.LIGHT_GRAY);
            sepCell.setPadding(0);
            separator.addCell(sepCell);
            document.add(separator);
            document.add(new Paragraph("\n"));

            // 2. Billing details
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[]{1f, 1f});

            // Billed To
            PdfPCell billedToCell = new PdfPCell();
            billedToCell.setBorder(PdfPCell.NO_BORDER);
            billedToCell.addElement(new Paragraph("BILLED TO:", boldFont));
            billedToCell.addElement(new Paragraph("Name: " + transaction.getUser().getFullName(), normalFont));
            billedToCell.addElement(new Paragraph("Email: " + transaction.getUser().getEmail(), normalFont));
            billedToCell.addElement(new Paragraph("Role: " + transaction.getUser().getRole().name(), normalFont));
            detailsTable.addCell(billedToCell);

            // Payment Reference
            PdfPCell refCell = new PdfPCell();
            refCell.setBorder(PdfPCell.NO_BORDER);
            refCell.addElement(new Paragraph("PAYMENT INFO:", boldFont));
            refCell.addElement(new Paragraph("Payment Method: Razorpay", normalFont));
            refCell.addElement(new Paragraph("Payment ID: " + transaction.getProviderPaymentId(), normalFont));
            refCell.addElement(new Paragraph("Order ID: " + transaction.getRazorpayOrderId(), normalFont));
            detailsTable.addCell(refCell);

            document.add(detailsTable);
            document.add(new Paragraph("\n\n"));

            // 3. Itemized Table
            PdfPTable itemsTable = new PdfPTable(3);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{4f, 1f, 1.5f});

            // Headers
            addTableHeader(itemsTable, "Description", boldFont);
            addTableHeader(itemsTable, "Qty", boldFont);
            addTableHeader(itemsTable, "Amount (INR)", boldFont);

            // Row for subscription
            String descText = "Platform Subscription - " + transaction.getPlan().getPlanName() + " Plan" +
                    "\nDuration: " + transaction.getPlan().getDurationInDays() + " days (" +
                    transaction.getStartDate() + " to " + transaction.getEndDate() + ")";
            addTableCell(itemsTable, descText, normalFont, Element.ALIGN_LEFT);
            addTableCell(itemsTable, "1", normalFont, Element.ALIGN_CENTER);
            addTableCell(itemsTable, "INR " + formatAmount(transaction.getAmount()), normalFont, Element.ALIGN_RIGHT);

            document.add(itemsTable);

            // 4. Summary Total
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[]{4f, 2.5f});

            PdfPCell spacerCell = new PdfPCell();
            spacerCell.setBorder(PdfPCell.NO_BORDER);
            totalTable.addCell(spacerCell);

            PdfPCell amountCell = new PdfPCell();
            amountCell.setPadding(8);
            amountCell.setBorder(PdfPCell.BOX);
            amountCell.setBackgroundColor(LIGHT_BG);
            Paragraph totPara = new Paragraph("TOTAL PAID: INR " + formatAmount(transaction.getAmount()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_COLOR));
            totPara.setAlignment(Element.ALIGN_RIGHT);
            amountCell.addElement(totPara);
            totalTable.addCell(amountCell);

            document.add(new Paragraph("\n"));
            document.add(totalTable);
            document.add(new Paragraph("\n\n\n"));

            // 5. Footer
            Paragraph footer = new Paragraph("This is a system-generated bill from NutriCare.", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    public byte[] generateConsultationBillPdf(ConsultationPayment payment, String billNumber) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);

        try {
            PdfWriter.getInstance(document, baos);
            document.open();

            // Font setups
            Font brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, PRIMARY_COLOR);
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, SECONDARY_COLOR);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, SECONDARY_COLOR);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, SECONDARY_COLOR);
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Color.GRAY);

            // 1. Header Section
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{3f, 2f});

            // Logo and platform name
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(PdfPCell.NO_BORDER);
            leftCell.addElement(new Paragraph("NutriCare", brandFont));
            leftCell.addElement(new Paragraph("Healthy Eating, Healthy Living", FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY)));
            leftCell.addElement(new Paragraph("support@nutricare.com", FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY)));
            headerTable.addCell(leftCell);

            // Receipt label and metadata
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(PdfPCell.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph rightPara = new Paragraph("INVOICE / RECEIPT\n\n", titleFont);
            rightPara.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(rightPara);

            Paragraph numPara = new Paragraph("Invoice No: " + billNumber, boldFont);
            numPara.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(numPara);

            String paidAtText = payment.getPaidAt() != null
                    ? payment.getPaidAt().format(formatter)
                    : payment.getCreatedAt().format(formatter);
            Paragraph datePara = new Paragraph("Date: " + paidAtText, normalFont);
            datePara.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(datePara);

            Paragraph statusPara = new Paragraph("Status: PAID", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, SUCCESS_COLOR));
            statusPara.setAlignment(Element.ALIGN_RIGHT);
            rightCell.addElement(statusPara);
            headerTable.addCell(rightCell);

            document.add(headerTable);
            document.add(new Paragraph("\n"));

            // Horizontal Separator
            PdfPTable separator = new PdfPTable(1);
            separator.setWidthPercentage(100);
            PdfPCell sepCell = new PdfPCell();
            sepCell.setBorder(PdfPCell.BOTTOM);
            sepCell.setBorderColor(Color.LIGHT_GRAY);
            sepCell.setPadding(0);
            separator.addCell(sepCell);
            document.add(separator);
            document.add(new Paragraph("\n"));

            // 2. Billing details
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setWidths(new float[]{1f, 1f});

            // Billed To
            PdfPCell billedToCell = new PdfPCell();
            billedToCell.setBorder(PdfPCell.NO_BORDER);
            billedToCell.addElement(new Paragraph("BILLED TO:", boldFont));
            billedToCell.addElement(new Paragraph("Name: " + payment.getUser().getFullName(), normalFont));
            billedToCell.addElement(new Paragraph("Email: " + payment.getUser().getEmail(), normalFont));
            billedToCell.addElement(new Paragraph("Role: " + payment.getUser().getRole().name(), normalFont));
            detailsTable.addCell(billedToCell);

            // Payment Reference
            PdfPCell refCell = new PdfPCell();
            refCell.setBorder(PdfPCell.NO_BORDER);
            refCell.addElement(new Paragraph("PAYMENT INFO:", boldFont));
            refCell.addElement(new Paragraph("Payment Method: Razorpay", normalFont));
            refCell.addElement(new Paragraph("Payment ID: " + payment.getProviderPaymentId(), normalFont));
            refCell.addElement(new Paragraph("Order ID: " + payment.getRazorpayOrderId(), normalFont));
            detailsTable.addCell(refCell);

            document.add(detailsTable);
            document.add(new Paragraph("\n\n"));

            // 3. Itemized Table
            PdfPTable itemsTable = new PdfPTable(3);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{4f, 1f, 1.5f});

            // Headers
            addTableHeader(itemsTable, "Description", boldFont);
            addTableHeader(itemsTable, "Qty", boldFont);
            addTableHeader(itemsTable, "Amount (INR)", boldFont);

            // Row for appointment
            String dateFormatted = payment.getAppointment().getAppointmentDate().format(appointmentDateFormatter);
            String timeFormatted = payment.getAppointment().getAppointmentTime().format(appointmentTimeFormatter);

            String descText = "Appointment Consultation Fee\n" +
                    "Dietician: Dr. " + payment.getDietician().getFullName() + "\n" +
                    "User: " + payment.getUser().getFullName() + "\n" +
                    "Appointment Date: " + dateFormatted + "\n" +
                    "Appointment Time: " + timeFormatted;

            addTableCell(itemsTable, descText, normalFont, Element.ALIGN_LEFT);
            addTableCell(itemsTable, "1", normalFont, Element.ALIGN_CENTER);
            addTableCell(itemsTable, "INR " + formatAmount(payment.getAmount()), normalFont, Element.ALIGN_RIGHT);

            document.add(itemsTable);

            // 4. Summary Total
            PdfPTable totalTable = new PdfPTable(2);
            totalTable.setWidthPercentage(100);
            totalTable.setWidths(new float[]{4f, 2.5f});

            PdfPCell spacerCell = new PdfPCell();
            spacerCell.setBorder(PdfPCell.NO_BORDER);
            totalTable.addCell(spacerCell);

            PdfPCell amountCell = new PdfPCell();
            amountCell.setPadding(8);
            amountCell.setBorder(PdfPCell.BOX);
            amountCell.setBackgroundColor(LIGHT_BG);
            Paragraph totPara = new Paragraph("TOTAL PAID: INR " + formatAmount(payment.getAmount()), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY_COLOR));
            totPara.setAlignment(Element.ALIGN_RIGHT);
            amountCell.addElement(totPara);
            totalTable.addCell(amountCell);

            document.add(new Paragraph("\n"));
            document.add(totalTable);
            document.add(new Paragraph("\n\n\n"));

            // 5. Footer
            Paragraph footer = new Paragraph("This is a system-generated bill from NutriCare.", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return baos.toByteArray();
    }

    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(LIGHT_BG);
        cell.setPadding(8);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTableCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(8);
        cell.setHorizontalAlignment(alignment);
        table.addCell(cell);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return amount.setScale(2, BigDecimal.ROUND_HALF_UP).toString();
    }
}
