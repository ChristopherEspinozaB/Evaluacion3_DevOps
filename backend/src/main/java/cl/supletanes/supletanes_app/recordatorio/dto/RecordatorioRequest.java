package cl.supletanes.supletanes_app.recordatorio.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class RecordatorioRequest {
    private String idUsuario;
    private String mensaje;
    private LocalDateTime fechaHora;
}