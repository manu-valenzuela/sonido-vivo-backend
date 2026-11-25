package cl.duoc.reportes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductoStockAlertDto {
    private String id;
    private String nombre;
    private Integer precio;
    private Integer stock;
}