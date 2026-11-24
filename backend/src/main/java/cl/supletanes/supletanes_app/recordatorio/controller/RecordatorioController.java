package cl.supletanes.supletanes_app.recordatorio.controller;

import cl.supletanes.supletanes_app.recordatorio.dto.RecordatorioRequest;
import cl.supletanes.supletanes_app.recordatorio.entity.Recordatorio;
import cl.supletanes.supletanes_app.recordatorio.service.RecordatorioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recordatorios")
public class RecordatorioController {

    private final RecordatorioService recordatorioService;

    public RecordatorioController(RecordatorioService recordatorioService) {
        this.recordatorioService = recordatorioService;
    }

    @Operation(summary = "Crear un recordatorio")
    @ApiResponse(responseCode = "201", description = "Recordatorio creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @PostMapping
    public ResponseEntity<Recordatorio> crearRecordatorio(@RequestBody RecordatorioRequest request) {
        Recordatorio recordatorio = new Recordatorio();
        recordatorio.setIdUsuario(request.getIdUsuario());
        recordatorio.setMensaje(request.getMensaje());
        recordatorio.setFechaHora(request.getFechaHora());
        Recordatorio nuevoRecordatorio = recordatorioService.crearRecordatorio(recordatorio);
        return new ResponseEntity<>(nuevoRecordatorio, HttpStatus.CREATED);
    }
}