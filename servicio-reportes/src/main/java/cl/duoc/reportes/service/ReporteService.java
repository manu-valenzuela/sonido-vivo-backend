package cl.duoc.reportes.service;

import cl.duoc.reportes.dto.GenerarReporteRequest;
import cl.duoc.reportes.dto.ProductoStockAlertDto;
import cl.duoc.reportes.dto.ReporteResponse;
import cl.duoc.reportes.dto.SnapshotDetalleDto;
import cl.duoc.reportes.dto.external.ProductoExternalDto;
import cl.duoc.reportes.dto.external.UsuarioExternalDto;
import cl.duoc.reportes.dto.external.VentaExternalDto;
import cl.duoc.reportes.exception.BusinessException;
import cl.duoc.reportes.exception.ResourceNotFoundException;
import cl.duoc.reportes.model.Reporte;
import cl.duoc.reportes.repository.ReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.services.usuarios.url}")
    private String usuariosServiceUrl;

    @Value("${app.services.productos.url}")
    private String productosServiceUrl;

    @Value("${app.services.ventas.url}")
    private String ventasServiceUrl;

    @Transactional
    public ReporteResponse generarReporte(GenerarReporteRequest request, String emitidoPor) {
        LocalDate fechaInicio = (request != null) ? request.getFechaInicio() : null;
        LocalDate fechaFin = (request != null) ? request.getFechaFin() : null;

        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            throw new BusinessException("La fecha de inicio no puede ser posterior a la fecha de término");
        }

        // 1. Obtener registros desde microservicios fuente
        List<UsuarioExternalDto> usuarios = obtenerUsuarios();
        List<ProductoExternalDto> productos = obtenerProductos();
        List<VentaExternalDto> ventas = obtenerVentas();

        // 2. Cuantificar usuarios y agrupar por rol
        int totalUsuarios = usuarios.size();
        Map<String, Integer> usuariosPorRol = new LinkedHashMap<>();
        usuariosPorRol.put("ADMINISTRADOR", 0);
        usuariosPorRol.put("TRABAJADOR", 0);
        usuariosPorRol.put("CLIENTE", 0);

        for (UsuarioExternalDto usuario : usuarios) {
            String rol = (usuario.getRol() != null && !usuario.getRol().isBlank())
                    ? usuario.getRol().trim().toUpperCase()
                    : "SIN_ROL";
            usuariosPorRol.put(rol, usuariosPorRol.getOrDefault(rol, 0) + 1);
        }

        // 3. Cuantificar ventas válidas vs anuladas y separar por tipo de despacho
        int cantidadVentasValidas = 0;
        int cantidadVentasAnuladas = 0;
        int cantidadVentasRechazadas = 0;
        long montoTotalVentas = 0L;
        int ventasDespachoCantidad = 0;
        long ventasDespachoMonto = 0L;
        int ventasRetiroCantidad = 0;
        long ventasRetiroMonto = 0L;

        for (VentaExternalDto venta : ventas) {
            if (!estaDentroDeRango(venta, fechaInicio, fechaFin)) {
                continue;
            }

            if ("ANULADA".equalsIgnoreCase(venta.getEstado())) {
                cantidadVentasAnuladas++;
                continue;
            }
            if ("RECHAZADA".equalsIgnoreCase(venta.getEstado())) {
                cantidadVentasRechazadas++;
                continue;
            }

            long monto = (venta.getMontoTotal() != null) ? venta.getMontoTotal() : 0L;
            cantidadVentasValidas++;
            montoTotalVentas += monto;

            if ("DOMICILIO".equalsIgnoreCase(venta.getTipoDespacho())) {
                ventasDespachoCantidad++;
                ventasDespachoMonto += monto;
            } else {
                ventasRetiroCantidad++;
                ventasRetiroMonto += monto;
            }
        }

        // 4. Auditar inventario: stock total, valorización y alertas de reposición
        int totalProductosCatalogo = productos.size();
        int totalUnidadesStock = 0;
        long valorTotalInventario = 0L;
        List<ProductoStockAlertDto> productosBajoStock = new ArrayList<>();
        List<ProductoStockAlertDto> productosSinStock = new ArrayList<>();

        for (ProductoExternalDto producto : productos) {
            int stock = (producto.getStock() != null) ? producto.getStock() : 0;
            int precio = (producto.getPrecio() != null) ? producto.getPrecio() : 0;

            totalUnidadesStock += stock;
            valorTotalInventario += (long) precio * stock;

            if (stock == 0) {
                productosSinStock.add(ProductoStockAlertDto.builder()
                        .id(producto.getId())
                        .nombre(producto.getNombre())
                        .precio(precio)
                        .stock(stock)
                        .build());
            } else if (stock <= 3) {
                productosBajoStock.add(ProductoStockAlertDto.builder()
                        .id(producto.getId())
                        .nombre(producto.getNombre())
                        .precio(precio)
                        .stock(stock)
                        .build());
            }
        }

        // 5. Serializar instantánea histórica en JSON
        SnapshotDetalleDto snapshot = SnapshotDetalleDto.builder()
                .usuariosPorRol(usuariosPorRol)
                .productosBajoStock(productosBajoStock)
                .productosSinStock(productosSinStock)
                .build();

        String snapshotJson = objectMapper.writeValueAsString(snapshot);

        String titulo = (request != null && request.getTitulo() != null && !request.getTitulo().isBlank())
                ? request.getTitulo().trim()
                : "Reporte Consolidado Sonido Vivo - " + LocalDate.now();

        Reporte reporte = Reporte.builder()
                .titulo(titulo)
                .emitidoPor(emitidoPor)
                .fechaInicio(fechaInicio)
                .fechaFin(fechaFin)
                .totalUsuarios(totalUsuarios)
                .cantidadVentasValidas(cantidadVentasValidas)
                .cantidadVentasAnuladas(cantidadVentasAnuladas)
                .cantidadVentasRechazadas(cantidadVentasRechazadas)
                .montoTotalVentas(montoTotalVentas)
                .ventasDespachoCantidad(ventasDespachoCantidad)
                .ventasDespachoMonto(ventasDespachoMonto)
                .ventasRetiroCantidad(ventasRetiroCantidad)
                .ventasRetiroMonto(ventasRetiroMonto)
                .totalProductosCatalogo(totalProductosCatalogo)
                .totalUnidadesStock(totalUnidadesStock)
                .valorTotalInventario(valorTotalInventario)
                .cantidadBajoStock(productosBajoStock.size())
                .cantidadSinStock(productosSinStock.size())
                .detalleSnapshot(snapshotJson)
                .build();

        return toResponse(reporteRepository.save(reporte));
    }

    @Transactional(readOnly = true)
    public List<ReporteResponse> listarReportes() {
        return reporteRepository.findAllByOrderByFechaEmisionDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReporteResponse obtenerPorId(Long id) {
        Reporte reporte = reporteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reporte no encontrado con ID: " + id));
        return toResponse(reporte);
    }

    @Transactional
    public void eliminarReporte(Long id) {
        if (!reporteRepository.existsById(id)) {
            throw new ResourceNotFoundException("Reporte no encontrado con ID: " + id);
        }
        reporteRepository.deleteById(id);
    }

    private List<UsuarioExternalDto> obtenerUsuarios() {
        List<UsuarioExternalDto> lista = restClient.get()
                .uri(usuariosServiceUrl + "/api/usuarios")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return lista != null ? lista : Collections.emptyList();
    }

    private List<ProductoExternalDto> obtenerProductos() {
        List<ProductoExternalDto> lista = restClient.get()
                .uri(productosServiceUrl + "/api/productos")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return lista != null ? lista : Collections.emptyList();
    }

    private List<VentaExternalDto> obtenerVentas() {
        List<VentaExternalDto> lista = restClient.get()
                .uri(ventasServiceUrl + "/api/ventas")
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        return lista != null ? lista : Collections.emptyList();
    }

    private boolean estaDentroDeRango(VentaExternalDto venta, LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaInicio == null && fechaFin == null) {
            return true;
        }
        if (venta.getFechaVenta() == null) {
            return false;
        }
        LocalDate fechaOrden = venta.getFechaVenta().toLocalDate();
        if (fechaInicio != null && fechaOrden.isBefore(fechaInicio)) {
            return false;
        }
        if (fechaFin != null && fechaOrden.isAfter(fechaFin)) {
            return false;
        }
        return true;
    }

    private ReporteResponse toResponse(Reporte reporte) {
        SnapshotDetalleDto snapshot = objectMapper.readValue(
                reporte.getDetalleSnapshot(),
                SnapshotDetalleDto.class
        );

        return ReporteResponse.builder()
                .id(reporte.getId())
                .titulo(reporte.getTitulo())
                .fechaEmision(reporte.getFechaEmision())
                .emitidoPor(reporte.getEmitidoPor())
                .fechaInicio(reporte.getFechaInicio())
                .fechaFin(reporte.getFechaFin())
                .totalUsuarios(reporte.getTotalUsuarios())
                .usuariosPorRol(snapshot.getUsuariosPorRol())
                .cantidadVentasValidas(reporte.getCantidadVentasValidas())
                .cantidadVentasAnuladas(reporte.getCantidadVentasAnuladas())
                .cantidadVentasRechazadas(reporte.getCantidadVentasRechazadas())
                .montoTotalVentas(reporte.getMontoTotalVentas())
                .ventasDespachoCantidad(reporte.getVentasDespachoCantidad())
                .ventasDespachoMonto(reporte.getVentasDespachoMonto())
                .ventasRetiroCantidad(reporte.getVentasRetiroCantidad())
                .ventasRetiroMonto(reporte.getVentasRetiroMonto())
                .totalProductosCatalogo(reporte.getTotalProductosCatalogo())
                .totalUnidadesStock(reporte.getTotalUnidadesStock())
                .valorTotalInventario(reporte.getValorTotalInventario())
                .cantidadBajoStock(reporte.getCantidadBajoStock())
                .cantidadSinStock(reporte.getCantidadSinStock())
                .productosBajoStock(snapshot.getProductosBajoStock())
                .productosSinStock(snapshot.getProductosSinStock())
                .build();
    }
}
