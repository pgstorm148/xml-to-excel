package com.actimize.plugins.xml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.actimize.actone.api.AlertApiClient;
import com.actimize.actone.model.Alert;
import com.actimize.rcm.client.RcmApiClient;

/**
 * ActOne/RCM XML to Excel Extractor Plugin
 * This plugin extracts XML data from Actimize alerts and converts it to Excel format
 */
@Component
@RestController
@RequestMapping("/api/plugins/xml-extractor")
public class XmlToExcelExtractorPlugin {
    
    private final AlertApiClient alertApiClient;
    private final RcmApiClient rcmApiClient;
    
    public XmlToExcelExtractorPlugin(AlertApiClient alertApiClient, RcmApiClient rcmApiClient) {
        this.alertApiClient = alertApiClient;
        this.rcmApiClient = rcmApiClient;
    }
    
    /**
     * Endpoint to extract XML data from an alert and convert it to Excel
     * @param alertId The ID of the alert to extract data from
     * @return The path to the generated Excel file
     */
    @GetMapping("/extract/{alertId}")
    public Map<String, String> extractXmlToExcel(@PathVariable String alertId) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Fetch alert data using the alert ID
            Alert alert = alertApiClient.getAlertById(alertId);
            if (alert == null) {
                response.put("status", "error");
                response.put("message", "Alert not found");
                return response;
            }
            
            // Extract XML content from the alert
            String xmlContent = fetchAlertXmlContent(alertId, alert);
            if (xmlContent == null || xmlContent.isEmpty()) {
                response.put("status", "error");
                response.put("message", "No XML content found for this alert");
                return response;
            }
            
            // Convert XML to Excel
            String excelFilePath = convertXmlToExcel(alertId, xmlContent);
            
            response.put("status", "success");
            response.put("filePath", excelFilePath);
            response.put("fileName", "Alert_" + alertId + "_Data.xlsx");
            
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error processing alert: " + e.getMessage());
        }
        
        return response;
    }
    
    /**
     * Helper method to fetch XML content from an alert
     * @param alertId The ID of the alert
     * @param alert The Alert object
     * @return The XML content as a string
     */
    private String fetchAlertXmlContent(String alertId, Alert alert) {
        // Check if this is an RCM alert
        if (alert.getSource().equalsIgnoreCase("RCM")) {
            return rcmApiClient.getAlertXmlData(alertId);
        } 
        // If it's an ActOne alert
        else {
            return alertApiClient.getAlertXmlData(alertId);
        }
    }
    
    /**
     * Converts XML content to an Excel file
     * @param alertId The ID of the alert
     * @param xmlContent The XML content to convert
     * @return The path to the generated Excel file
     * @throws Exception If an error occurs during conversion
     */
    private String convertXmlToExcel(String alertId, String xmlContent) throws Exception {
        // Parse the XML content
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(xmlContent)));
        
        // Create a new workbook
        XSSFWorkbook workbook = new XSSFWorkbook();
        
        // Extract alert data
        extractAlertData(document, workbook);
        
        // Extract transaction data
        extractTransactionData(document, workbook);
        
        // Extract entity data
        extractEntityData(document, workbook);
        
        // Create directory if it doesn't exist
        String directoryPath = System.getProperty("java.io.tmpdir") + "/xml-extracts/";
        Files.createDirectories(Paths.get(directoryPath));
        
        // Write the workbook to a file
        String filePath = directoryPath + "Alert_" + alertId + "_Data.xlsx";
        FileOutputStream outputStream = new FileOutputStream(filePath);
        workbook.write(outputStream);
        workbook.close();
        outputStream.close();
        
        return filePath;
    }
    
    /**
     * Extracts alert data from the XML document
     * @param document The XML document
     * @param workbook The Excel workbook
     */
    private void extractAlertData(Document document, XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet("Alert Details");
        
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] headers = {"Field", "Value"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }
        
        // Extract alert info
        Element alertElement = (Element) document.getElementsByTagName("Alert").item(0);
        if (alertElement == null) {
            alertElement = (Element) document.getElementsByTagName("alert").item(0);
        }
        
        if (alertElement != null) {
            List<String[]> alertData = new ArrayList<>();
            
            // Extract all child elements of the alert
            NodeList alertChildren = alertElement.getChildNodes();
            for (int i = 0; i < alertChildren.getLength(); i++) {
                Node node = alertChildren.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element element = (Element) node;
                    String fieldName = element.getNodeName();
                    String fieldValue = element.getTextContent();
                    
                    alertData.add(new String[]{fieldName, fieldValue});
                }
            }
            
            // Write data to sheet
            int rowNum = 1;
            for (String[] data : alertData) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(data[0]);
                row.createCell(1).setCellValue(data[1]);
            }
        }
        
        // Auto-size columns
        for (int i = 0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    
    /**
     * Extracts transaction data from the XML document
     * @param document The XML document
     * @param workbook The Excel workbook
     */
    private void extractTransactionData(Document document, XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet("Transactions");
        
        // Find transactions
        NodeList transactionNodes = document.getElementsByTagName("Transaction");
        if (transactionNodes.getLength() == 0) {
            transactionNodes = document.getElementsByTagName("transaction");
        }
        
        if (transactionNodes.getLength() > 0) {
            // First, determine all possible fields across transactions
            List<String> allFields = new ArrayList<>();
            for (int i = 0; i < transactionNodes.getLength(); i++) {
                Element transactionElement = (Element) transactionNodes.item(i);
                NodeList children = transactionElement.getChildNodes();
                
                for (int j = 0; j < children.getLength(); j++) {
                    Node node = children.item(j);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        String fieldName = node.getNodeName();
                        if (!allFields.contains(fieldName)) {
                            allFields.add(fieldName);
                        }
                    }
                }
            }
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < allFields.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(allFields.get(i));
            }
            
            // Extract transaction data
            int rowNum = 1;
            for (int i = 0; i < transactionNodes.getLength(); i++) {
                Element transactionElement = (Element) transactionNodes.item(i);
                Row row = sheet.createRow(rowNum++);
                
                // For each field, find its value in this transaction
                for (int j = 0; j < allFields.size(); j++) {
                    String fieldName = allFields.get(j);
                    NodeList fieldNodes = transactionElement.getElementsByTagName(fieldName);
                    
                    if (fieldNodes.getLength() > 0) {
                        String fieldValue = fieldNodes.item(0).getTextContent();
                        row.createCell(j).setCellValue(fieldValue);
                    }
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < allFields.size(); i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }
    
    /**
     * Extracts entity data from the XML document
     * @param document The XML document
     * @param workbook The Excel workbook
     */
    private void extractEntityData(Document document, XSSFWorkbook workbook) {
        XSSFSheet sheet = workbook.createSheet("Entities");
        
        // Find entities (could be Customer, Entity, or other specific entity types)
        NodeList entityNodes = document.getElementsByTagName("Entity");
        if (entityNodes.getLength() == 0) {
            entityNodes = document.getElementsByTagName("Customer");
        }
        if (entityNodes.getLength() == 0) {
            entityNodes = document.getElementsByTagName("entity");
        }
        if (entityNodes.getLength() == 0) {
            entityNodes = document.getElementsByTagName("customer");
        }
        
        if (entityNodes.getLength() > 0) {
            // First, determine all possible fields across entities
            List<String> allFields = new ArrayList<>();
            for (int i = 0; i < entityNodes.getLength(); i++) {
                Element entityElement = (Element) entityNodes.item(i);
                NodeList children = entityElement.getChildNodes();
                
                for (int j = 0; j < children.getLength(); j++) {
                    Node node = children.item(j);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        String fieldName = node.getNodeName();
                        if (!allFields.contains(fieldName)) {
                            allFields.add(fieldName);
                        }
                    }
                }
            }
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < allFields.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(allFields.get(i));
            }
            
            // Extract entity data
            int rowNum = 1;
            for (int i = 0; i < entityNodes.getLength(); i++) {
                Element entityElement = (Element) entityNodes.item(i);
                Row row = sheet.createRow(rowNum++);
                
                // For each field, find its value in this entity
                for (int j = 0; j < allFields.size(); j++) {
                    String fieldName = allFields.get(j);
                    NodeList fieldNodes = entityElement.getElementsByTagName(fieldName);
                    
                    if (fieldNodes.getLength() > 0) {
                        String fieldValue = fieldNodes.item(0).getTextContent();
                        row.createCell(j).setCellValue(fieldValue);
                    }
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < allFields.size(); i++) {
                sheet.autoSizeColumn(i);
            }
        }
    }
}
