package cl.duoc.reportes.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductoExternalDto {
    private String id;
    private String nombre;
    private Integer precio;
    private Integer stock;
}