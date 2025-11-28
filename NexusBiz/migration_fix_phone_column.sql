-- =====================================================
-- MIGRACIÓN: Hacer columna phone nullable en usuarios
-- Fecha: 2025-11-28
-- Descripción: 
--   Hacer que la columna phone sea opcional (nullable) en la tabla usuarios
--   Esto permite registrar usuarios sin teléfono y evita conflictos con la restricción UNIQUE
-- =====================================================

-- Hacer la columna phone nullable
ALTER TABLE usuarios 
ALTER COLUMN phone DROP NOT NULL;

-- IMPORTANTE: En PostgreSQL, múltiples valores NULL no violan la restricción UNIQUE
-- Por lo tanto, podemos tener múltiples usuarios con phone = NULL sin problemas

-- Limpiar valores vacíos existentes (convertirlos a NULL)
UPDATE usuarios 
SET phone = NULL 
WHERE phone = '';

-- Comentario de documentación
COMMENT ON COLUMN usuarios.phone IS 'Teléfono del usuario (opcional, puede ser NULL). No se usa en el registro actual. Múltiples valores NULL están permitidos por la restricción UNIQUE.';

-- =====================================================
-- FIN DE LA MIGRACIÓN
-- =====================================================
-- 
-- INSTRUCCIONES:
-- 1. Ejecuta este SQL en el SQL Editor de Supabase
-- 2. Después de ejecutar, el registro de usuarios funcionará sin requerir teléfono
-- 3. Alternativamente, puedes mantener phone como NOT NULL y el código
--    ya está configurado para enviar un string vacío ""
-- =====================================================

