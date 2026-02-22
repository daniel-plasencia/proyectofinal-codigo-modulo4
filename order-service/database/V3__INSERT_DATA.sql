-- ============================================
-- Migration: V3__INSERT_DATA.sql
-- Database: orderdb
-- ============================================

-- Datos de prueba para órdenes
INSERT INTO orders (order_number, user_id, status, total_amount) VALUES
('ORD-2025-001', 1, 'CONFIRMED', 1489.97),
('ORD-2025-002', 2, 'PENDING', 799.98),
('ORD-2025-003', 1, 'SHIPPED', 99.99);

-- Datos de prueba para items de orden
INSERT INTO order_items (order_id, product_id, quantity, unit_price, subtotal) VALUES
(1, 1, 1, 1299.99, 1299.99),  -- Laptop Dell XPS 15
(1, 2, 1, 99.99, 99.99),      -- Mouse Logitech MX Master 3
(1, 3, 1, 89.99, 89.99),      -- Teclado Mecánico Keychron K8
(2, 4, 1, 449.99, 449.99),    -- Monitor LG UltraWide 34"
(2, 5, 1, 349.99, 349.99),    -- Auriculares Sony WH-1000XM5
(3, 2, 1, 99.99, 99.99);      -- Mouse Logitech MX Master 3
