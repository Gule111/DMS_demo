package com.dms.service;

import com.dms.common.QiniuUtil;
import com.dms.entity.GeneratedDocument;
import com.dms.mapper.GeneratedDocumentMapper;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
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
     * 生成标准机动车驾驶证申请表 PDF，并上传至七牛云
     */
    public GeneratedDocument generateEnrollmentForm(Long studentId) {
        try {
            // 1. 查询学员数据
            Map<String, Object> studentInfo = documentMapper.getStudentInfoForPdf(studentId);
            if (studentInfo == null) {
                throw new RuntimeException("学员不存在");
            }

            // 2. 在内存中生成 PDF 流
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // 读取 Windows 内置的黑体以支持中文渲染
            BaseFont bfChinese = BaseFont.createFont("C:/Windows/Fonts/simhei.ttf", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
            Font titleFont = new Font(bfChinese, 18, Font.BOLD);
            Font normalFont = new Font(bfChinese, 12, Font.NORMAL);
            Font smallItalicFont = new Font(bfChinese, 10, Font.ITALIC);
            Font subtitleFont = new Font(bfChinese, 14, Font.BOLD);

            Paragraph title = new Paragraph("机动车驾驶证申请表", titleFont);
            title.setAlignment(Paragraph.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("申请人姓名: " + studentInfo.get("real_name"), normalFont));
            document.add(new Paragraph("身份证号码: " + studentInfo.get("id_card"), normalFont));
            document.add(new Paragraph("联系电话: " + studentInfo.get("phone"), normalFont));
            document.add(new Paragraph("申请准驾车型: " + studentInfo.get("license_type"), normalFont));
            document.add(new Paragraph("\n"));
            
            document.add(new Paragraph("【申告事项】", subtitleFont));
            document.add(new Paragraph("    本人承诺以上填写的信息及提交的材料均真实有效。如有虚假，本人愿承担相关的法律责任及后果。", normalFont));
            document.add(new Paragraph("\n\n\n"));
            
            Paragraph signature = new Paragraph("申请人签字：(已通过系统实名认证代签)", smallItalicFont);
            signature.setAlignment(Paragraph.ALIGN_RIGHT);
            document.add(signature);
            
            document.close();

            // 3. 将内存里的 PDF 流上传到七牛云 generated/enrollment_forms 目录下
            ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
            String fileName = "enrollment_form_stu" + studentId + ".pdf";
            String fileUrl = qiniuUtil.uploadFile(in, "generated/enrollment_forms/", fileName);

            // 4. 将生成的记录保存到数据库
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
}
