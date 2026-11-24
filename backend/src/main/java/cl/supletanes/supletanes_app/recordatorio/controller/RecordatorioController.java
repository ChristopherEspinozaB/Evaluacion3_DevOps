package cl.supletanes.supletanes_app.recordatorio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.supletanes.supletanes_app.recordatorio.dto.RecordatorioRequest;
import cl.supletanes.supletanes_app.recordatorio.entity.Recordatorio;
import cl.supletanes.supletanes_app.recordatorio.service.RecordatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/api/v1/recordatorios")
public class RecordatorioController {

    private final RecordatorioService recordatorioService;

    public RecordatorioController(RecordatorioService recordatorioService) {
        this.recordatorioService = recordatorioService;
    }

    @GetMapping
    public ResponseEntity<List<Recordatorio>> obtenerTodos() {
        return ResponseEntity.ok(recordatorioService.obtenerTodosLosRecordatorios());
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<List<Recordatorio>> obtenerPorUsuario(@PathVariable String idUsuario) {
        return ResponseEntity.ok(recordatorioService.obtenerRecordatoriosPorUsuario(idUsuario));
    }

    @Operation(summary = "Crear un recordatorio")
    @ApiResponse(responseCode = "201", description = "Recordatorio creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @PostMapping
    public ResponseEntity<Recordatorio> crear(@RequestBody RecordatorioRequest request) {
        Recordatorio recordatorio = mapToEntity(request);
        Recordatorio nuevoRecordatorio = recordatorioService.crearRecordatorio(recordatorio);
        return new ResponseEntity<>(nuevoRecordatorio, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recordatorio> actualizar(
            @PathVariable Long id,
            @RequestBody RecordatorioRequest request) {
        Recordatorio recordatorio = mapToEntity(request);
        return recordatorioService.actualizarRecordatorio(id, recordatorio)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (recordatorioService.obtenerRecordatorioPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        recordatorioService.eliminarRecordatorio(id);
        return ResponseEntity.noContent().build();
    }

    private Recordatorio mapToEntity(RecordatorioRequest request) {
        Recordatorio recordatorio = new Recordatorio();
        recordatorio.setIdUsuario(request.getIdUsuario());
        recordatorio.setMensaje(request.getMensaje());
        recordatorio.setFechaHora(request.getFechaHora());
        return recordatorio;
    }
}