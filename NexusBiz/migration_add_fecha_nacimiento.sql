-- =====================================================
-- MIGRACIÓN: Agregar columna fecha_nacimiento a usuarios
-- Fecha: 2025-11-28
-- Descripción: 
--   Agregar la columna fecha_nacimiento que falta en la tabla usuarios
--   Esta columna es necesaria para el registro de usuarios
-- =====================================================

-- Agregar columna fecha_nacimiento si no existe
ALTER TABLE usuarios 
ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE;

-- Si la columna ya existe pero es NULL, hacerla NOT NULL (opcional)
-- Descomentar solo si quieres hacerla obligatoria después de migrar datos existentes
-- ALTER TABLE usuarios 
-- ALTER COLUMN fecha_nacimiento SET NOT NULL;

-- Comentario de documentación
COMMENT ON COLUMN usuarios.fecha_nacimiento IS 'Fecha de nacimiento del usuario. Usado para validar edad mínima de 18 años en el registro.';

-- =====================================================
-- FIN DE LA MIGRACIÓN
-- =====================================================
-- 
-- INSTRUCCIONES:
-- 1. Ejecuta este SQL en el SQL Editor de Supabase
-- 2. Después de ejecutar, el registro de usuarios debería funcionar correctamente
-- 3. Si tienes usuarios existentes sin fecha_nacimiento, considera actualizarlos
--    o hacer la columna nullable temporalmente
-- =====================================================

