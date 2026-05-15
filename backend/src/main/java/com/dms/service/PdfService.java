package com.dms.service;

import com.dms.common.QiniuUtil;
import com.dms.entity.GeneratedDocument;
import com.dms.mapper.GeneratedDocumentMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;

@Service
public class PdfService {

    private final GeneratedDocumentMapper documentMapper;
    private final QiniuUtil qiniuUtil;

    public PdfService(GeneratedDocumentMapper documentMapper, QiniuUtil qiniuUtil) {
        this.documentMapper = documentMapper;
        this.qiniuUtil = qiniuUtil;
    }

    /**
     * 异步生成所有报名相关的 3 份 PDF 文档
     */
    public void generateAllPdfsAsync(Long studentId) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                System.out.println("[PdfService] 开始为学员 " + studentId + " 异步生成档案PDF...");
                Map<String, Object> studentInfo = documentMapper.getStudentInfoForPdf(studentId);
                if (studentInfo == null) return;

                generateEnrollmentForm(studentId);
                generateHealthCertForm(studentId, studentInfo);
                generateExamTicket(studentId, studentInfo);
                System.out.println("[PdfService] 学员 " + studentId + " 档案生成完毕！");
            } catch (Exception e) {
                System.err.println("[PdfService Error] 异步生成 PDF 失败: " + e.getMessage());
            }
        });
    }

    /**
     * 创建居中的单元格
     */
    private PdfPCell createCenterCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        return cell;
    }

    /**
     * 生成标准机动车驾驶证申请表 PDF
     */
    public GeneratedDocument generateEnrollmentForm(Long studentId) {
        try {
            Map<String, Object> studentInfo = documentMapper.getStudentInfoForPdf(studentId);
            if (studentInfo == null) throw new RuntimeException("学员不存在");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont bfChinese = BaseFont.createFont("C:/Windows/Fonts/simhei.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bfChinese, 18, Font.BOLD);
            Font normalFont = new Font(bfChinese, 12, Font.NORMAL);

            Paragraph title = new Paragraph("机动车驾驶证申请表", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            // Row 1
            table.addCell(createCenterCell("姓名", normalFont));
            table.addCell(createCenterCell(studentInfo.get("real_name").toString(), normalFont));
            table.addCell(createCenterCell("性别", normalFont));
            table.addCell(createCenterCell("", normalFont));

            // Row 2
            table.addCell(createCenterCell("身份证号码", normalFont));
            PdfPCell idCell = createCenterCell(studentInfo.get("id_card").toString(), normalFont);
            idCell.setColspan(3);
            table.addCell(idCell);

            // Row 3
            table.addCell(createCenterCell("联系电话", normalFont));
            table.addCell(createCenterCell(studentInfo.get("phone").toString(), normalFont));
            table.addCell(createCenterCell("申请车型", normalFont));
            table.addCell(createCenterCell(studentInfo.get("license_type").toString(), normalFont));

            // Row 4
            PdfPCell declareCell = new PdfPCell();
            declareCell.setColspan(4);
            declareCell.setPadding(10f);
            declareCell.addElement(new Paragraph("【申告事项】", new Font(bfChinese, 14, Font.BOLD)));
            declareCell.addElement(new Paragraph("    本人承诺以上填写的信息及提交的材料均真实有效。如有虚假，本人愿承担相关的法律责任及后果。", normalFont));
            declareCell.addElement(new Paragraph("\n\n申请人签字：(已实名认证自动代签)           日期：____年__月__日", normalFont));
            table.addCell(declareCell);

            document.add(table);
            document.close();

            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            String fileName = "enrollment_form_stu" + studentId + ".pdf";
            String fileUrl = qiniuUtil.uploadFile(in, "generated/enrollment_forms/", fileName);

            GeneratedDocument doc = new GeneratedDocument();
            doc.setStudentId(studentId);
            doc.setDocType("EnrollmentForm");
            doc.setFileUrl(fileUrl);
            documentMapper.insert(doc);
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("生成报名表 PDF 失败: " + e.getMessage());
        }
    }

    /**
     * 生成体检合格证明 PDF
     */
    public GeneratedDocument generateHealthCertForm(Long studentId, Map<String, Object> studentInfo) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont bfChinese = BaseFont.createFont("C:/Windows/Fonts/simhei.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bfChinese, 18, Font.BOLD);
            Font normalFont = new Font(bfChinese, 12, Font.NORMAL);

            Paragraph title = new Paragraph("机动车驾驶人身体条件证明", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);

            table.addCell(createCenterCell("姓名", normalFont));
            table.addCell(createCenterCell(studentInfo.get("real_name").toString(), normalFont));
            table.addCell(createCenterCell("身份证号", normalFont));
            table.addCell(createCenterCell(studentInfo.get("id_card").toString(), normalFont));

            PdfPCell headerCell = createCenterCell("体检项目与结论", new Font(bfChinese, 14, Font.BOLD));
            headerCell.setColspan(4);
            table.addCell(headerCell);

            String[] items = {"身高", "视力", "辨色力", "听力", "上肢", "下肢", "躯干、颈部"};
            for (String item : items) {
                table.addCell(createCenterCell(item, normalFont));
                PdfPCell resCell = createCenterCell("合格", normalFont);
                resCell.setColspan(3);
                table.addCell(resCell);
            }

            PdfPCell signCell = new PdfPCell();
            signCell.setColspan(4);
            signCell.setPadding(10f);
            signCell.addElement(new Paragraph("审核结果: 经 AI 视觉模型审核及数据比对，该申请人符合所申请准驾车型的身体条件。", normalFont));
            signCell.addElement(new Paragraph("\n\n医疗机构盖章：(系统审核专章)           日期：____年__月__日", normalFont));
            table.addCell(signCell);

            document.add(table);
            document.close();

            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            String fileName = "health_cert_stu" + studentId + ".pdf";
            String fileUrl = qiniuUtil.uploadFile(in, "generated/health_certs/", fileName);

            GeneratedDocument doc = new GeneratedDocument();
            doc.setStudentId(studentId);
            doc.setDocType("HealthCert");
            doc.setFileUrl(fileUrl);
            documentMapper.insert(doc);
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("生成体检表 PDF 失败: " + e.getMessage());
        }
    }

    /**
     * 生成通用准考证 PDF
     */
    public GeneratedDocument generateExamTicket(Long studentId, Map<String, Object> studentInfo) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            BaseFont bfChinese = BaseFont.createFont("C:/Windows/Fonts/simhei.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bfChinese, 22, Font.BOLD);
            Font normalFont = new Font(bfChinese, 14, Font.NORMAL);

            Paragraph title = new Paragraph("机动车驾驶人考试通用准考证", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n\n"));

            PdfPTable table = new PdfPTable(2);
            table.setWidthPercentage(80);

            table.addCell(createCenterCell("考生姓名", normalFont));
            table.addCell(createCenterCell(studentInfo.get("real_name").toString(), normalFont));

            table.addCell(createCenterCell("身份证号", normalFont));
            table.addCell(createCenterCell(studentInfo.get("id_card").toString(), normalFont));

            table.addCell(createCenterCell("准考车型", normalFont));
            table.addCell(createCenterCell(studentInfo.get("license_type").toString(), normalFont));

            table.addCell(createCenterCell("发证机关", normalFont));
            table.addCell(createCenterCell("公安局交警支队车管所(代发)", normalFont));

            PdfPCell noticeCell = new PdfPCell();
            noticeCell.setColspan(2);
            noticeCell.setPadding(15f);
            noticeCell.addElement(new Paragraph("【注意事项】", new Font(bfChinese, 16, Font.BOLD)));
            noticeCell.addElement(new Paragraph("1. 考生须凭本准考证及有效身份证件原件进入考场。", normalFont));
            noticeCell.addElement(new Paragraph("2. 请提前 30 分钟到达指定考场，迟到 15 分钟取消本次考试资格。", normalFont));
            noticeCell.addElement(new Paragraph("3. 严禁携带通讯工具进入考场，违者按作弊处理。", normalFont));
            table.addCell(noticeCell);

            document.add(table);
            document.close();

            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            String fileName = "exam_ticket_stu" + studentId + ".pdf";
            String fileUrl = qiniuUtil.uploadFile(in, "generated/exam_tickets/", fileName);

            GeneratedDocument doc = new GeneratedDocument();
            doc.setStudentId(studentId);
            doc.setDocType("ExamTicket");
            doc.setFileUrl(fileUrl);
            documentMapper.insert(doc);
            return doc;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("生成准考证 PDF 失败: " + e.getMessage());
        }
    }
}
