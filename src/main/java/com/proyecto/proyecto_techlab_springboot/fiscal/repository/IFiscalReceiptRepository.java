package com.proyecto.proyecto_techlab_springboot.fiscal.repository;

import com.proyecto.proyecto_techlab_springboot.fiscal.model.FiscalReceipt;
import com.proyecto.proyecto_techlab_springboot.fiscal.model.FiscalReceiptType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Repository
public interface IFiscalReceiptRepository extends JpaRepository<FiscalReceipt, UUID> {

    /**
     * Recupera la Factura C original de una venta para asociarla obligatoriamente en la Nota de Crédito C.
     */
    Optional<FiscalReceipt> findBySale_SaleIdAndReceiptType(UUID saleId, FiscalReceiptType receiptType);

    /**
     * Previene dobles emisiones sobre la misma venta.
     */
    boolean existsBySale_SaleIdAndReceiptType(UUID saleId, FiscalReceiptType receiptType);

    /**
     * Recupera el comprobante vinculado a una devolución particular.
     */
    Optional<FiscalReceipt> findBySaleReturn_ReturnId(UUID returnId);

    List<FiscalReceipt> findAllBySale_SaleId(UUID saleId);

    /**
     * Obtiene el último número emitido por punto de venta y tipo de comprobante en la base local (usado en el mock y como salvaguarda).
     */
    @Query("""
        SELECT COALESCE(MAX(fr.receiptNumber), 0)
        FROM FiscalReceipt fr
        WHERE fr.pointOfSale = :pos
          AND fr.receiptType = :type
    """)
    long findMaxReceiptNumber(@Param("pos") Integer pos, @Param("type") FiscalReceiptType type);
}