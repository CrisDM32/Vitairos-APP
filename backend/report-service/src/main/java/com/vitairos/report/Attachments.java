package com.vitairos.report;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api/v1/reports/{id}/attachments")
@Transactional
class Attachments {
    private static final Map<String,String> TYPES = Map.of("jpg","image/jpeg","jpeg","image/jpeg","png","image/png","pdf","application/pdf");
    private final Reports reports;
    private final JdbcTemplate db;
    Attachments(Reports reports, JdbcTemplate db) { this.reports = reports; this.db = db; }

    @PutMapping(value="/{fileId}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    Reports.FileInfo put(@PathVariable String id, @PathVariable String fileId, @RequestAttribute Access.Profile user,
                         @RequestParam MultipartFile file) throws IOException {
        reports.editable(reports.access(id,user,true));
        ReportController.checkId(fileId);
        String name = file.getOriginalFilename();
        if (name == null || name.length()>200 || name.matches(".*[/\\\\].*") || name.chars().anyMatch(Character::isISOControl))
            throw new ResponseStatusException(BAD_REQUEST,"Nombre de archivo inválido");
        int dot = name.lastIndexOf('.');
        String type = dot < 0 ? null : TYPES.get(name.substring(dot+1).toLowerCase(Locale.ROOT));
        if (type == null || file.isEmpty()) throw new ResponseStatusException(BAD_REQUEST,"Use JPG, PNG o PDF con contenido");
        if (file.getSize()>10485760) throw new ResponseStatusException(PAYLOAD_TOO_LARGE);
        byte[] bytes = file.getBytes();
        byte[] signature = switch (type) {
            case "image/jpeg" -> new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff};
            case "image/png" -> new byte[]{(byte)0x89,80,78,71,13,10,26,10};
            default -> new byte[]{37,80,68,70,45};
        };
        if (!Arrays.equals(signature, Arrays.copyOf(bytes, signature.length)))
            throw new ResponseStatusException(BAD_REQUEST,"El contenido no coincide con el formato");
        int updated = db.update("UPDATE attachments SET name=?,media_type=?,size_bytes=?,content=? WHERE report_id=? AND id=?",
                name,type,file.getSize(),bytes,id,fileId);
        if (updated == 0) {
            if (db.queryForObject("SELECT COUNT(*) FROM attachments WHERE report_id=?",Integer.class,id)>=5)
                throw new ResponseStatusException(CONFLICT,"Máximo cinco adjuntos");
            db.update("INSERT INTO attachments(report_id,id,name,media_type,size_bytes,content) VALUES (?,?,?,?,?,?)",
                    id,fileId,name,type,file.getSize(),bytes);
        }
        return new Reports.FileInfo(fileId,name,file.getSize()/1048576.0,type.equals("application/pdf") ? "PDF" : "IMAGEN");
    }

    @GetMapping("/{fileId}")
    ResponseEntity<byte[]> download(@PathVariable String id, @PathVariable String fileId, @RequestAttribute Access.Profile user) {
        reports.access(id,user,false);
        return db.query("SELECT name,media_type,content FROM attachments WHERE report_id=? AND id=?", (r,n) ->
                ResponseEntity.ok().contentType(MediaType.parseMediaType(r.getString(2)))
                        .header("Content-Disposition",ContentDisposition.attachment().filename(r.getString(1),java.nio.charset.StandardCharsets.UTF_8).build().toString())
                        .body(r.getBytes(3)),id,fileId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    @DeleteMapping("/{fileId}")
    @ResponseStatus(NO_CONTENT)
    void delete(@PathVariable String id, @PathVariable String fileId, @RequestAttribute Access.Profile user) {
        reports.editable(reports.access(id,user,true));
        if (db.update("DELETE FROM attachments WHERE report_id=? AND id=?",id,fileId)==0)
            throw new ResponseStatusException(NOT_FOUND);
    }
}
