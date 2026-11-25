package cl.duoc.reportes.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VentaExternalDto {
    private Long id;
    private String estado;
    private String tipoDespacho;
    @JsonAlias({"total"})
    private Integer montoTotal;

    @JsonAlias({"fechaCreacion", "fecha"})
    private LocalDateTime fechaVenta;
}
