-- ============================================
-- Migration: V1__CREATE_TABLES.sql
-- Database: orderdb (Docker container: postgres-order)
-- ============================================

-- Función para auto-actualizar updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Tabla de órdenes
CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_status CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT chk_total_positive CHECK (total_amount >= 0)
);

-- Trigger para actualizar updated_at
CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Tabla de items de orden
CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price NUMERIC(10, 2) NOT NULL,
    subtotal NUMERIC(10, 2) NOT NULL,
    
    CONSTRAINT fk_order FOREIGN KEY (order_id) 
        REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT chk_quantity_positive CHECK (quantity > 0),
    CONSTRAINT chk_unit_price_positive CHECK (unit_price >= 0),
    CONSTRAINT chk_subtotal_positive CHECK (subtotal >= 0)
);

-- Comentarios
COMMENT ON TABLE orders IS 'Órdenes de compra del sistema - DB en Docker';
COMMENT ON TABLE order_items IS 'Items de las órdenes - DB en Docker';
COMMENT ON COLUMN orders.user_id IS 'Usuario que creó la orden (ref. lógica a userdb.users.id en otro contenedor)';
COMMENT ON COLUMN order_items.product_id IS 'Producto (ref. lógica a productdb.products.id en otro contenedor)';
