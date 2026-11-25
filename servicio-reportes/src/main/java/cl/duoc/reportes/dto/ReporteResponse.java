package cl.duoc.reportes.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ReporteResponse {
    private Long id;
    private String titulo;
    private LocalDateTime fechaEmision;
    private String emitidoPor;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    // Métricas de Usuarios
    private Integer totalUsuarios;
    private Map<String, Integer> usuariosPorRol;

    // Métricas de Ventas
    private Integer cantidadVentasValidas;
    private Integer cantidadVentasAnuladas;
    private Integer cantidadVentasRechazadas;
    private Long montoTotalVentas;
    private Integer ventasDespachoCantidad;
    private Long ventasDespachoMonto;
    private Integer ventasRetiroCantidad;
    private Long ventasRetiroMonto;

    // Métricas de Inventario
    private Integer totalProductosCatalogo;
    private Integer totalUnidadesStock;
    private Long valorTotalInventario;
    private Integer cantidadBajoStock;
    private Integer cantidadSinStock;
    private List<ProductoStockAlertDto> productosBajoStock;
    private List<ProductoStockAlertDto> productosSinStock;
}