package com.vitairos.report;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
class Catalog {
    record Options(List<String> reportTypes, List<String> processes, List<String> areas, List<String> shifts) {}
    static final Options OPTIONS = new Options(
        List.of("Reporte de turno v1", "Reporte de mantención v1", "Reporte de incidente v1", "Checklist de inspección v2"),
        List.of("Chancado", "Molienda", "Flotación", "Transporte de mineral", "Mantención mecánica"),
        List.of("Faena Norte", "Faena Sur", "Planta", "Taller"),
        List.of("Turno A (08–20)", "Turno B (20–08)", "Día administrativo"));

    @GetMapping("/api/v1/catalog")
    Options options() { return OPTIONS; }

    record Form(String type, String process, String area, String shift, String notes, Double latitude, Double longitude) {
        Form {
            notes = notes == null ? "" : notes;
            if (type == null || !OPTIONS.reportTypes.contains(type) || process == null || !OPTIONS.processes.contains(process)
                    || area == null || !OPTIONS.areas.contains(area) || shift == null || !OPTIONS.shifts.contains(shift))
                throw new ResponseStatusException(BAD_REQUEST, "Seleccione valores del catálogo");
            if (notes.length() > 300 || (type.contains("incidente") && notes.isBlank()))
                throw new ResponseStatusException(BAD_REQUEST, "Observaciones inválidas");
            if ((latitude == null) != (longitude == null) || latitude != null &&
                    (!Double.isFinite(latitude) || !Double.isFinite(longitude) || Math.abs(latitude)>90 || Math.abs(longitude)>180))
                throw new ResponseStatusException(BAD_REQUEST, "Coordenadas inválidas");
        }
    }
}
