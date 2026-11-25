package com.vitairos.report;

import java.time.OffsetDateTime;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@Repository
class Reports {
    record Entry(String id, String user, OffsetDateTime createdAt, String status, int attempts,
                 String supervisorNote, String type, String process, String area, String shift, String notes,
                 Double latitude, Double longitude) {}
    record Event(OffsetDateTime time, String text) {}
    record FileInfo(String id, String name, double sizeMb, String kind) {}
    record Detail(Entry report, List<FileInfo> attachments, List<Event> timeline, Weather.Snapshot weather) {}

    private final JdbcTemplate db;
    Reports(JdbcTemplate db) { this.db = db; }

    private Entry map(ResultSet rs, int row) throws SQLException {
        return new Entry(rs.getString("id"), rs.getString("author"), rs.getObject("created_at", OffsetDateTime.class),
                rs.getString("status"), rs.getInt("attempts"), rs.getString("supervisor_note"), rs.getString("type"),
                rs.getString("process"), rs.getString("area"), rs.getString("shift"), rs.getString("notes"),
                rs.getObject("latitude", Double.class), rs.getObject("longitude", Double.class));
    }

    List<Entry> list(Access.Profile user) {
        String order = " ORDER BY created_at DESC, id";
        if (user.supervisor()) return db.query("SELECT * FROM reports" + order, this::map);
        return db.query("SELECT * FROM reports WHERE owner=?" + order, this::map, user.email());
    }

    Entry access(String id, Access.Profile user, boolean ownerOnly) {
        boolean scoped = ownerOnly || !user.supervisor();
        String sql = "SELECT * FROM reports WHERE id=?" + (scoped ? " AND owner=?" : "") + " FOR UPDATE";
        Object[] args = scoped ? new Object[]{id, user.email()} : new Object[]{id};
        return db.query(sql, this::map, args).stream().findFirst().orElseThrow(() -> new ResponseStatusException(NOT_FOUND));
    }

    void editable(Entry entry) {
        if (!List.of("BORRADOR","REQUIERE_CORRECCION").contains(entry.status()))
            throw new ResponseStatusException(CONFLICT, "El reporte ya fue enviado");
    }
    void event(String id, String text) {
        db.update("INSERT INTO events(report_id,message) VALUES (?,?)", id, text);
    }
    Detail detail(Entry entry, Weather.Snapshot weather) {
        var files = db.query("SELECT id,name,size_bytes,media_type FROM attachments WHERE report_id=? ORDER BY id",
                (r,n) -> new FileInfo(r.getString(1),r.getString(2),r.getLong(3)/1048576.0,
                        "application/pdf".equals(r.getString(4)) ? "PDF" : "IMAGEN"), entry.id());
        var timeline = db.query("SELECT occurred_at,message FROM events WHERE report_id=? ORDER BY seq",
                (r,n) -> new Event(r.getObject(1, OffsetDateTime.class),r.getString(2)), entry.id());
        return new Detail(entry,files,timeline,weather);
    }
}

@RestController
@RequestMapping("/api/v1/reports")
@Transactional
class ReportController {
    private final Reports reports;
    private final JdbcTemplate db;
    private final Weather weather;
    ReportController(Reports reports, JdbcTemplate db, Weather weather) {
        this.reports = reports;
        this.db = db;
        this.weather = weather;
    }

    @GetMapping
    List<Reports.Entry> list(@RequestAttribute Access.Profile user) { return reports.list(user); }

    @GetMapping("/{id}")
    Reports.Detail detail(@PathVariable String id, @RequestAttribute Access.Profile user) {
        var entry = reports.access(id,user,false);
        return reports.detail(entry, weather.saved(id));
    }

    @PutMapping("/{id}")
    Reports.Detail save(@PathVariable String id, @RequestAttribute Access.Profile user, @RequestBody Catalog.Form form) {
        checkId(id);
        try {
            db.update("""
                INSERT INTO reports(id,owner,author,type,process,area,shift,notes,latitude,longitude,status)
                VALUES (?,?,?,?,?,?,?,?,?,?,'BORRADOR')
                """,id,user.email(),user.name(),form.type(),form.process(),form.area(),form.shift(),form.notes(),form.latitude(),form.longitude());
            reports.event(id, "Borrador creado");
            weather.capture(id, form);
        } catch (DuplicateKeyException existing) {
            var entry = reports.access(id,user,true);
            reports.editable(entry);
            var previous = new Catalog.Form(entry.type(),entry.process(),entry.area(),entry.shift(),entry.notes(),entry.latitude(),entry.longitude());
            if (!previous.equals(form)) {
                db.update("UPDATE reports SET type=?,process=?,area=?,shift=?,notes=?,latitude=?,longitude=? WHERE id=?",
                        form.type(),form.process(),form.area(),form.shift(),form.notes(),form.latitude(),form.longitude(),id);
                reports.event(id, "Reporte editado");
            }
        }
        return detail(id,user);
    }

    static void checkId(String id) {
        if (!id.matches("[A-Za-z0-9_-]{1,80}")) throw new ResponseStatusException(BAD_REQUEST, "Identificador inválido");
    }

    @PutMapping("/{id}/submission")
    Reports.Detail submit(@PathVariable String id, @RequestAttribute Access.Profile user) {
        var entry = reports.access(id,user,true);
        if (!entry.status().equals("SINCRONIZADO")) {
            reports.editable(entry);
            if (db.queryForObject("SELECT COUNT(*) FROM attachments WHERE report_id=?",Integer.class,id)==0)
                throw new ResponseStatusException(CONFLICT,"Agregue al menos un adjunto antes de enviar");
            db.update("UPDATE reports SET status='SINCRONIZADO', attempts=attempts+1, supervisor_note=NULL WHERE id=?",id);
            reports.event(id,"Sincronizado correctamente");
        }
        return detail(id,user);
    }

    record Correction(String note) {}
    @PutMapping("/{id}/correction")
    Reports.Detail correct(@PathVariable String id, @RequestAttribute Access.Profile user, @RequestBody Correction correction) {
        if (!user.supervisor()) throw new ResponseStatusException(FORBIDDEN);
        var entry = reports.access(id,user,false);
        String note = correction.note();
        if (note == null || note.isBlank() || note.length()>300) throw new ResponseStatusException(BAD_REQUEST,"Indique la corrección (1 a 300 caracteres)");
        if (entry.status().equals("REQUIERE_CORRECCION") && note.equals(entry.supervisorNote())) return detail(id,user);
        if (!entry.status().equals("SINCRONIZADO")) throw new ResponseStatusException(CONFLICT);
        db.update("UPDATE reports SET status='REQUIERE_CORRECCION', supervisor_note=? WHERE id=?",note,id);
        reports.event(id,"Corrección solicitada por " + user.name());
        return detail(id,user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(NO_CONTENT)
    void delete(@PathVariable String id, @RequestAttribute Access.Profile user) {
        if (!reports.access(id,user,true).status().equals("BORRADOR")) throw new ResponseStatusException(CONFLICT);
        db.update("DELETE FROM reports WHERE id=?",id);
    }
}
