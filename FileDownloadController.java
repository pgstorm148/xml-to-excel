package com.actimize.plugins.xml;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/plugins/xml-extractor")
public class FileDownloadController {
    
    /**
     * Endpoint to download the generated Excel file
     * @param filePath The path to the file to download
     * @param fileName The name to give the downloaded file
     * @return The file as a downloadable resource
     * @throws IOException If an error occurs during file handling
     */
    @GetMapping("/download/{filePath:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filePath, 
                                                @RequestParam(defaultValue = "output.xlsx") String fileName) throws IOException {
        
        Path path = Paths.get(filePath);
        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));
        
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(resource.contentLength())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
