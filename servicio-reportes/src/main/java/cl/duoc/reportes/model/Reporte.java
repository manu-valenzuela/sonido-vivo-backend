package cl.duoc.reportes.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reportes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private LocalDateTime fechaEmision;

    @Column(name = "emitido_por", nullable = false, length = 120)
    private String emitidoPor;

    @Column(name = "fecha_inicio")
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Column(name = "total_usuarios", nullable = false)
    private Integer totalUsuarios;

    @Column(name = "cantidad_ventas_validas", nullable = false)
    private Integer cantidadVentasValidas;

    @Column(name = "cantidad_ventas_anuladas", nullable = false)
    private Integer cantidadVentasAnuladas;

    @Column(name = "cantidad_ventas_rechazadas", nullable = false)
    private Integer cantidadVentasRechazadas;

    @Column(name = "monto_total_ventas", nullable = false)
    private Long montoTotalVentas;

    @Column(name = "ventas_despacho_cantidad", nullable = false)
    private Integer ventasDespachoCantidad;

    @Column(name = "ventas_despacho_monto", nullable = false)
    private Long ventasDespachoMonto;

    @Column(name = "ventas_retiro_cantidad", nullable = false)
    private Integer ventasRetiroCantidad;

    @Column(name = "ventas_retiro_monto", nullable = false)
    private Long ventasRetiroMonto;

    @Column(name = "total_productos_catalogo", nullable = false)
    private Integer totalProductosCatalogo;

    @Column(name = "total_unidades_stock", nullable = false)
    private Integer totalUnidadesStock;

    @Column(name = "valor_total_inventario", nullable = false)
    private Long valorTotalInventario;

    @Column(name = "cantidad_bajo_stock", nullable = false)
    private Integer cantidadBajoStock;

    @Column(name = "cantidad_sin_stock", nullable = false)
    private Integer cantidadSinStock;

    @Column(name = "detalle_snapshot", nullable = false, columnDefinition = "LONGTEXT")
    private String detalleSnapshot;

    @PrePersist
    protected void onCreate() {
        if (this.fechaEmision == null) {
            this.fechaEmision = LocalDateTime.now();
        }
    }
}