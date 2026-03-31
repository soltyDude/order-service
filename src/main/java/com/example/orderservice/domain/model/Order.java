package com.example.orderservice.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address", columnDefinition = "jsonb", nullable = false)
    private ShippingAddress shippingAddress;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "reservation_id")
    private String reservationId;

    @Column(name = "cancel_reason")
    private String cancelReason;

    @Version
    private Integer version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    // ==================== Business Methods ====================

    /**
     * Создаёт заказ. Фабричный метод вместо конструктора.
     * Инкапсулирует правила создания: начальный статус, вычисление суммы, timestamps.
     */
    public static Order create(UUID userId,
                               ShippingAddress shippingAddress,
                               String paymentMethod,
                               List<OrderItemData> itemsData) {
        var now = Instant.now();

        var order = Order.builder()
                .userId(userId)
                .status(OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .paymentMethod(paymentMethod)
                .totalAmount(BigDecimal.ZERO)
                .createdAt(now)
                .updatedAt(now)
                .items(new ArrayList<>())
                .build();

        itemsData.forEach(data -> order.addItem(data.productId(), data.quantity(), data.price()));
        order.calculateTotalAmount();

        return order;
    }

    /**
     * Данные для создания OrderItem. Record — потому что это просто контейнер данных,
     * не entity и не DTO. Живёт внутри domain layer.
     */
    public record OrderItemData(UUID productId, int quantity, BigDecimal price) {}

    /**
     * Переводит заказ в новый статус. Если переход невалидный — бросает исключение.
     * Единственный способ изменить статус. Прямого сеттера нет.
     */
    public void transitionTo(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new InvalidOrderStateException(this.id, this.status, newStatus);
        }
        this.status = newStatus;
        this.updatedAt = Instant.now();
    }

    /**
     * Отменяет заказ. Бизнес-метод: проверяет можно ли отменить, устанавливает причину.
     * Возвращает информацию о необходимых компенсациях.
     */
    public CancelResult cancel(String reason) {
        if (!this.status.isCancellable()) {
            throw new OrderNotCancellableException(this.id, this.status);
        }

        boolean refundNeeded = isPaymentDone();
        boolean releaseNeeded = isInventoryReserved();

        transitionTo(OrderStatus.CANCELLED);
        this.cancelReason = reason;

        return new CancelResult(refundNeeded, releaseNeeded);
    }

    public record CancelResult(boolean refundNeeded, boolean releaseNeeded) {}

    public void assignPaymentId(String paymentId) {
        this.paymentId = paymentId;
        this.updatedAt = Instant.now();
    }

    public void assignReservationId(String reservationId) {
        this.reservationId = reservationId;
        this.updatedAt = Instant.now();
    }

    // ==================== Private Helpers ====================

    private void addItem(UUID productId, int quantity, BigDecimal price) {
        var item = OrderItem.builder()
                .order(this)
                .productId(productId)
                .quantity(quantity)
                .price(price)
                .build();
        item.calculateSubtotal();
        this.items.add(item);
    }

    private void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean isPaymentDone() {
        return status == OrderStatus.PAYMENT_CONFIRMED
                || status == OrderStatus.INVENTORY_RESERVING
                || status == OrderStatus.CONFIRMED
                || status == OrderStatus.SHIPPED;
    }

    private boolean isInventoryReserved() {
        return status == OrderStatus.CONFIRMED
                || status == OrderStatus.SHIPPED;
    }
}