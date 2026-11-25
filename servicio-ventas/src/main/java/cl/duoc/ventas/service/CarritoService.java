package cl.duoc.ventas.service;

import cl.duoc.ventas.client.ProductosClient;
import cl.duoc.ventas.dto.ActualizarCantidadCarritoRequest;
import cl.duoc.ventas.dto.AgregarItemCarritoRequest;
import cl.duoc.ventas.dto.CarritoResponse;
import cl.duoc.ventas.dto.ItemCarritoResponse;
import cl.duoc.ventas.dto.external.ProductoExternalResponse;
import cl.duoc.ventas.exception.BusinessException;
import cl.duoc.ventas.exception.ResourceNotFoundException;
import cl.duoc.ventas.model.Carrito;
import cl.duoc.ventas.model.DetalleCarrito;
import cl.duoc.ventas.repository.CarritoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final ProductosClient productosClient;

    @Transactional
    public CarritoResponse obtenerCarrito(String email) {
        Carrito carrito = obtenerOCrearEntidad(email);
        return toResponse(carrito);
    }

    @Transactional
    public CarritoResponse agregarItem(String email, AgregarItemCarritoRequest request) {
        String idNormalizado = request.getProductoId().trim().toUpperCase();
        ProductoExternalResponse producto = productosClient.obtenerProducto(idNormalizado);
        Carrito carrito = obtenerOCrearEntidad(email);

        Optional<DetalleCarrito> existenteOpt = carrito.getItems().stream()
                .filter(i -> i.getProductoId().equalsIgnoreCase(idNormalizado))
                .findFirst();

        int cantidadFinal = request.getCantidad() + existenteOpt.map(DetalleCarrito::getCantidad).orElse(0);

        if (producto.getStock() < cantidadFinal) {
            throw new BusinessException("Stock insuficiente para '" + producto.getNombre()
                    + "'. Disponible: " + producto.getStock() + ", solicitado en carrito: " + cantidadFinal);
        }

        if (existenteOpt.isPresent()) {
            DetalleCarrito existente = existenteOpt.get();
            existente.setCantidad(cantidadFinal);
            existente.setNombreProducto(producto.getNombre());
            existente.setPrecioUnitario(producto.getPrecio());
            existente.setImagen(producto.getImagen());
        } else {
            DetalleCarrito nuevo = DetalleCarrito.builder()
                    .productoId(producto.getId())
                    .nombreProducto(producto.getNombre())
                    .precioUnitario(producto.getPrecio())
                    .imagen(producto.getImagen())
                    .cantidad(cantidadFinal)
                    .build();
            carrito.agregarItem(nuevo);
        }

        return toResponse(carritoRepository.save(carrito));
    }

    @Transactional
    public CarritoResponse actualizarCantidad(String email, String productoId, ActualizarCantidadCarritoRequest request) {
        String idNormalizado = productoId.trim().toUpperCase();
        ProductoExternalResponse producto = productosClient.obtenerProducto(idNormalizado);
        Carrito carrito = obtenerOCrearEntidad(email);

        DetalleCarrito item = carrito.getItems().stream()
                .filter(i -> i.getProductoId().equalsIgnoreCase(idNormalizado))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("El producto con ID " + idNormalizado + " no está en tu carrito"));

        if (producto.getStock() < request.getCantidad()) {
            throw new BusinessException("Stock insuficiente para '" + producto.getNombre()
                    + "'. Disponible: " + producto.getStock() + ", solicitado: " + request.getCantidad());
        }

        item.setCantidad(request.getCantidad());
        item.setNombreProducto(producto.getNombre());
        item.setPrecioUnitario(producto.getPrecio());
        item.setImagen(producto.getImagen());

        return toResponse(carritoRepository.save(carrito));
    }

    @Transactional
    public CarritoResponse eliminarItem(String email, String productoId) {
        String idNormalizado = productoId.trim().toUpperCase();
        Carrito carrito = obtenerOCrearEntidad(email);
        boolean eliminado = carrito.getItems().removeIf(i -> i.getProductoId().equalsIgnoreCase(idNormalizado));

        if (!eliminado) {
            throw new ResourceNotFoundException("El producto con ID " + idNormalizado + " no está en tu carrito");
        }

        return toResponse(carritoRepository.save(carrito));
    }

    @Transactional
    public CarritoResponse vaciarCarrito(String email) {
        Carrito carrito = obtenerOCrearEntidad(email);
        carrito.limpiarItems();
        return toResponse(carritoRepository.save(carrito));
    }

    public Carrito obtenerOCrearEntidad(String email) {
        return carritoRepository.findWithItemsByUsuarioEmail(email)
                .orElseGet(() -> {
                    // Al iniciar sesión el front pide el carrito y, si el cliente agrega un producto de inmediato,
                    // dos solicitudes pueden intentar crearlo a la vez: INSERT IGNORE evita el error de clave duplicada.
                    carritoRepository.crearSiNoExiste(email);
                    return carritoRepository.buscarParaActualizar(email)
                            .orElseThrow(() -> new IllegalStateException("No se pudo crear el carrito de " + email));
                });
    }

    private CarritoResponse toResponse(Carrito carrito) {
        List<ItemCarritoResponse> itemsDto = carrito.getItems().stream()
                .map(i -> ItemCarritoResponse.builder()
                        .id(i.getId())
                        .productoId(i.getProductoId())
                        .nombreProducto(i.getNombreProducto())
                        .precioUnitario(i.getPrecioUnitario())
                        .imagen(i.getImagen())
                        .cantidad(i.getCantidad())
                        .subtotal(i.getPrecioUnitario() * i.getCantidad())
                        .build())
                .toList();

        int totalItems = itemsDto.stream().mapToInt(ItemCarritoResponse::getCantidad).sum();
        int totalMonto = itemsDto.stream().mapToInt(ItemCarritoResponse::getSubtotal).sum();

        return CarritoResponse.builder()
                .id(carrito.getId())
                .usuarioEmail(carrito.getUsuarioEmail())
                .fechaActualizacion(carrito.getFechaActualizacion())
                .totalItems(totalItems)
                .total(totalMonto)
                .items(itemsDto)
                .build();
    }
}