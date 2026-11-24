package cl.supletanes.supletanes_app.recordatorio.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RecordatorioRequest {
    private String idUsuario;
    private String mensaje;
    private LocalDateTime fechaHora;
}