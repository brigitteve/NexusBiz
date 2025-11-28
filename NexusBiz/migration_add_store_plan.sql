-- =====================================================
-- MIGRACIÓN: Agregar campo plan a bodegas
-- Fecha: 2025-11-28
-- Descripción: 
--   Agregar el campo plan a la tabla bodegas para diferenciar
--   entre plan FREE (2 ofertas) y PRO (ilimitadas)
-- =====================================================

-- Crear tipo ENUM para el plan
CREATE TYPE store_plan AS ENUM ('FREE', 'PRO');

-- Agregar columna plan a bodegas
ALTER TABLE bodegas 
ADD COLUMN IF NOT EXISTS plan store_plan NOT NULL DEFAULT 'FREE';

-- Agregar columna store_plan a productos (denormalizado para ordenamiento rápido)
ALTER TABLE productos 
ADD COLUMN IF NOT EXISTS store_plan store_plan;

-- Actualizar productos existentes con el plan de su bodega
UPDATE productos p
SET store_plan = b.plan
FROM bodegas b
WHERE p.store_id = b.id
AND p.store_plan IS NULL;

-- Establecer default para productos sin plan
UPDATE productos
SET store_plan = 'FREE'
WHERE store_plan IS NULL;

-- Comentarios de documentación
COMMENT ON COLUMN bodegas.plan IS 'Plan de la bodega: FREE (2 ofertas activas) o PRO (ilimitadas con mayor visibilidad)';
COMMENT ON COLUMN productos.store_plan IS 'Plan de la bodega denormalizado (para ordenamiento rápido: PRO primero)';

-- =====================================================
-- FIN DE LA MIGRACIÓN
-- =====================================================
-- 
-- INSTRUCCIONES:
-- 1. Ejecuta este SQL en el SQL Editor de Supabase
-- 2. Después de ejecutar, las bodegas tendrán plan FREE por defecto
-- 3. Para actualizar una bodega a PRO:
--    UPDATE bodegas SET plan = 'PRO' WHERE id = 'bodega_id';
-- 4. Las ofertas PRO aparecerán primero en las listas y con cards doradas
-- =====================================================

