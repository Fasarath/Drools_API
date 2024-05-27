package org.example.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/drl-files")
public class DrlFileController {

    private final Path fileStorageLocation;

    public DrlFileController(@Value("${file.upload-dir}") String uploadDir) throws IOException {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.fileStorageLocation);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Path targetLocation = fileStorageLocation.resolve(file.getOriginalFilename());
            if (Files.exists(targetLocation)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("File already exists: " + file.getOriginalFilename());
            }
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok("File uploaded successfully: " + file.getOriginalFilename());
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("Could not upload the file: " + file.getOriginalFilename());
        }
    }

    @PostMapping("/upload-multiple")
    public ResponseEntity<String> uploadMultipleFiles(@RequestParam("files") MultipartFile[] files) {
        try {
            for (MultipartFile file : files) {
                Path targetLocation = fileStorageLocation.resolve(file.getOriginalFilename());
                if (Files.exists(targetLocation)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("File already exists: " + file.getOriginalFilename());
                }
                Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }
            return ResponseEntity.ok("Files uploaded successfully");
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("Could not upload the files");
        }
    }

    @GetMapping
    public ResponseEntity<List<String>> listFiles() {
        try (Stream<Path> files = Files.list(fileStorageLocation)) {
            List<String> filenames = files.map(Path::getFileName)
                                          .map(Path::toString)
                                          .collect(Collectors.toList());
            return ResponseEntity.ok(filenames);
        } catch (IOException ex) {
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> readFile(@PathVariable String filename) {
        try {
            Path filePath = fileStorageLocation.resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                     .body(new ByteArrayResource("There is no file in this name".getBytes()));
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (MalformedURLException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/{filename:.+}")
    public ResponseEntity<String> updateFile(@PathVariable String filename, @RequestBody String newContent) {
        try {
            Path filePath = fileStorageLocation.resolve(filename).normalize();
            Files.write(filePath, newContent.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            return ResponseEntity.ok("File updated successfully: " + filename);
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("Could not update the file: " + filename);
        }
    }

    @DeleteMapping("/{filename:.+}")
    public ResponseEntity<String> deleteFile(@PathVariable String filename) {
        try {
            Path filePath = fileStorageLocation.resolve(filename).normalize();
            Files.delete(filePath);
            return ResponseEntity.ok("File deleted successfully: " + filename);
        } catch (IOException ex) {
            return ResponseEntity.status(500).body("Could not delete the file: " + filename);
        }
    }
}
