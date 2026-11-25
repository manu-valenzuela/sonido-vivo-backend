package cl.duoc.ventas.repository;

import cl.duoc.ventas.model.Carrito;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {

    @EntityGraph(attributePaths = {"items"})
    Optional<Carrito> findWithItemsByUsuarioEmail(String usuarioEmail);

    /**
     * Crea el carrito del usuario solo si no existe. Si dos solicitudes llegan a la vez,
     * MySQL inserta una y la otra se ignora, en vez de fallar por la restricción UNIQUE de usuario_email.
     */
    @Modifying
    @Query(value = "INSERT IGNORE INTO carritos (usuario_email) VALUES (:email)", nativeQuery = true)
    void crearSiNoExiste(@Param("email") String email);

    /**
     * Lectura con bloqueo (SELECT ... FOR UPDATE): siempre ve la última versión confirmada,
     * incluido un carrito recién creado por otra solicitud simultánea.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT c FROM Carrito c WHERE c.usuarioEmail = :email")
    Optional<Carrito> buscarParaActualizar(@Param("email") String email);
}
