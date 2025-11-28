-- =====================================================
-- ESQUEMA SQL COMPLETO PARA NEXUSBIZ - SUPABASE
-- Basado en análisis del código Kotlin real del proyecto
-- =====================================================

-- =====================================================
-- 1. TIPOS ENUMERADOS (ENUMS)
-- =====================================================

-- Enum para tipo de usuario (usado en User.kt y AuthViewModel.kt)
CREATE TYPE user_type AS ENUM ('CONSUMER', 'STORE_OWNER');

-- Enum para estado de grupo (usado en Group.kt y GroupRepository.kt)
-- Máquina de estados: ACTIVE → PICKUP → VALIDATED/COMPLETED
-- ACTIVE puede expirar → EXPIRED
CREATE TYPE group_status AS ENUM ('ACTIVE', 'PICKUP', 'VALIDATED', 'COMPLETED', 'EXPIRED');

-- Enum para estado de reserva/participante
CREATE TYPE reservation_status AS ENUM ('RESERVED', 'VALIDATED', 'CANCELLED');

-- =====================================================
-- 2. TABLA: usuarios (users)
-- Usada en: AuthRepository.kt, AuthViewModel.kt, HomeScreen.kt, ProfileScreen.kt
-- =====================================================
CREATE TABLE usuarios (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    alias TEXT NOT NULL UNIQUE, -- Alias usado para login (LoginScreen.kt)
    password_hash TEXT NOT NULL, -- Hash SHA-256 de la contraseña
    fecha_nacimiento DATE NOT NULL, -- Fecha de nacimiento del usuario
    district TEXT NOT NULL, -- Distrito del usuario
    email TEXT,
    avatar TEXT, -- URL de imagen de perfil
    points INTEGER DEFAULT 0, -- Sistema de puntos
    badges TEXT[] DEFAULT '{}', -- Lista de badges (User.kt)
    streak INTEGER DEFAULT 0, -- Racha de compras
    completed_groups INTEGER DEFAULT 0, -- Contador usado en GroupCompletedConsumerScreen.kt
    total_savings NUMERIC(10, 2) DEFAULT 0.00, -- Ahorros totales
    user_type user_type NOT NULL DEFAULT 'CONSUMER', -- CONSUMER o STORE_OWNER
    latitude DOUBLE PRECISION, -- Latitud opcional
    longitude DOUBLE PRECISION, -- Longitud opcional
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices para usuarios
CREATE INDEX idx_usuarios_alias ON usuarios(alias);
CREATE INDEX idx_usuarios_user_type ON usuarios(user_type);
CREATE INDEX idx_usuarios_district ON usuarios(district);

-- =====================================================
-- 3. TABLA: bodegas (stores)
-- Usada en: Store.kt, StoreRepository.kt, StoreDashboardScreen.kt, StoreProfileScreen.kt
-- =====================================================
CREATE TABLE bodegas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL, -- Nombre de la bodega
    address TEXT NOT NULL, -- Dirección completa
    district TEXT NOT NULL, -- Distrito (usado en filtros)
    latitude DOUBLE PRECISION, -- Coordenada GPS
    longitude DOUBLE PRECISION, -- Coordenada GPS
    phone TEXT NOT NULL, -- Teléfono de contacto
    image_url TEXT, -- URL de imagen de la bodega
    has_stock BOOLEAN DEFAULT true, -- Si tiene stock disponible
    owner_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE, -- Relación con usuario propietario
    rating NUMERIC(3, 2) DEFAULT 0.00, -- Calificación (0.00 a 5.00)
    total_sales INTEGER DEFAULT 0, -- Total de ventas
    ruc TEXT, -- RUC de la bodega (BodegaValidateRucScreen.kt)
    commercial_name TEXT, -- Nombre comercial (BodegaRegistrationScreens.kt)
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices para bodegas
CREATE INDEX idx_bodegas_owner_id ON bodegas(owner_id);
CREATE INDEX idx_bodegas_district ON bodegas(district);
CREATE INDEX idx_bodegas_has_stock ON bodegas(has_stock);
CREATE INDEX idx_bodegas_location ON bodegas USING GIST (point(longitude, latitude)); -- Para búsquedas geográficas

-- =====================================================
-- 4. TABLA: categorias (categories)
-- Usada en: Category.kt, ProductRepository.kt, HomeScreen.kt
-- =====================================================
CREATE TABLE categorias (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL UNIQUE, -- Nombre de la categoría (ej: "Alimentos", "Limpieza")
    icon TEXT, -- Icono de la categoría
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- =====================================================
-- 5. TABLA: productos (products) - Ofertas publicadas
-- Usada en: Product.kt, ProductRepository.kt, PublishProductScreen.kt, ProductDetailScreen.kt
-- =====================================================
CREATE TABLE productos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL, -- Nombre del producto
    description TEXT, -- Descripción del producto
    image_url TEXT, -- URL de imagen
    category_id UUID REFERENCES categorias(id) ON DELETE SET NULL, -- Relación con categoría
    category_name TEXT, -- Nombre de categoría denormalizado (para búsquedas rápidas)
    normal_price NUMERIC(10, 2) NOT NULL CHECK (normal_price > 0), -- Precio normal
    group_price NUMERIC(10, 2) NOT NULL CHECK (group_price > 0 AND group_price < normal_price), -- Precio grupal (debe ser menor)
    min_group_size INTEGER NOT NULL DEFAULT 3 CHECK (min_group_size >= 1), -- Tamaño mínimo del grupo
    max_group_size INTEGER NOT NULL DEFAULT 10 CHECK (max_group_size >= min_group_size), -- Tamaño máximo
    store_id UUID NOT NULL REFERENCES bodegas(id) ON DELETE CASCADE, -- Bodega que publica
    store_name TEXT NOT NULL, -- Nombre de bodega denormalizado
    district TEXT NOT NULL, -- Distrito denormalizado (para filtros)
    is_active BOOLEAN DEFAULT true, -- Si la oferta está activa
    duration_hours INTEGER DEFAULT 24, -- Duración de la oferta en horas (PublishProductScreen.kt)
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índices para productos
CREATE INDEX idx_productos_store_id ON productos(store_id);
CREATE INDEX idx_productos_category_id ON productos(category_id);
CREATE INDEX idx_productos_district ON productos(district);
CREATE INDEX idx_productos_is_active ON productos(is_active);
CREATE INDEX idx_productos_category_name ON productos(category_name); -- Para búsquedas por categoría

-- =====================================================
-- 6. TABLA: grupos (groups) - Grupos de compra colectiva
-- Usada en: Group.kt, GroupRepository.kt, MyGroupsScreen.kt, GroupDetailScreen.kt
-- =====================================================
CREATE TABLE grupos (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id UUID NOT NULL REFERENCES productos(id) ON DELETE CASCADE, -- Producto asociado
    product_name TEXT NOT NULL, -- Nombre denormalizado
    product_image TEXT, -- Imagen denormalizada
    creator_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE, -- Usuario que crea el grupo
    creator_alias TEXT NOT NULL, -- Alias denormalizado
    current_size INTEGER DEFAULT 1 CHECK (current_size >= 0), -- Participantes actuales
    target_size INTEGER NOT NULL CHECK (target_size >= 1), -- Meta de participantes
    status group_status NOT NULL DEFAULT 'ACTIVE', -- Estado del grupo
    expires_at TIMESTAMPTZ NOT NULL, -- Fecha de expiración
    created_at TIMESTAMPTZ DEFAULT NOW(),
    store_id UUID NOT NULL REFERENCES bodegas(id) ON DELETE CASCADE, -- Bodega donde se retira
    store_name TEXT NOT NULL, -- Nombre denormalizado
    qr_code TEXT UNIQUE, -- QR único generado cuando se completa la meta (GroupRepository.kt línea 133)
    validated_at TIMESTAMPTZ, -- Fecha de validación (cuando se escanea QR)
    normal_price NUMERIC(10, 2) NOT NULL, -- Precio normal denormalizado
    group_price NUMERIC(10, 2) NOT NULL -- Precio grupal denormalizado
);

-- Índices para grupos
CREATE INDEX idx_grupos_product_id ON grupos(product_id);
CREATE INDEX idx_grupos_creator_id ON grupos(creator_id);
CREATE INDEX idx_grupos_store_id ON grupos(store_id);
CREATE INDEX idx_grupos_status ON grupos(status);
CREATE INDEX idx_grupos_expires_at ON grupos(expires_at);
CREATE INDEX idx_grupos_qr_code ON grupos(qr_code) WHERE qr_code IS NOT NULL;
CREATE INDEX idx_grupos_active_product ON grupos(product_id, status) WHERE status = 'ACTIVE'; -- Para validar grupos activos únicos por producto

-- =====================================================
-- 7. TABLA: participantes (participants) - Usuarios en grupos
-- Usada en: Participant.kt, Group.kt, GroupRepository.createReservation()
-- =====================================================
CREATE TABLE participantes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES grupos(id) ON DELETE CASCADE, -- Grupo al que pertenece
    user_id UUID NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE, -- Usuario participante
    alias TEXT NOT NULL, -- Alias del usuario al momento de unirse
    avatar TEXT, -- Avatar del usuario
    reserved_units INTEGER NOT NULL DEFAULT 1 CHECK (reserved_units >= 1), -- Unidades reservadas por el usuario
    joined_at TIMESTAMPTZ DEFAULT NOW(), -- Fecha de unión
    is_validated BOOLEAN DEFAULT false, -- Si ya retiró su producto (ScanQRScreen.kt)
    validated_at TIMESTAMPTZ, -- Fecha de validación/retiro
    status reservation_status NOT NULL DEFAULT 'RESERVED', -- Estado de la reserva
    UNIQUE(group_id, user_id) -- Un usuario no puede unirse dos veces al mismo grupo
);

-- Índices para participantes
CREATE INDEX idx_participantes_group_id ON participantes(group_id);
CREATE INDEX idx_participantes_user_id ON participantes(user_id);
CREATE INDEX idx_participantes_is_validated ON participantes(is_validated);

-- =====================================================
-- 8. TABLA: codigos_verificacion (verification_codes)
-- ELIMINADA: Ya no se usa verificación por SMS
-- =====================================================

-- =====================================================
-- 9. TRIGGERS Y FUNCIONES AUTOMÁTICAS
-- =====================================================

-- Función para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Triggers para updated_at
CREATE TRIGGER update_usuarios_updated_at BEFORE UPDATE ON usuarios
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_bodegas_updated_at BEFORE UPDATE ON bodegas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_productos_updated_at BEFORE UPDATE ON productos
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Función para actualizar current_size cuando se agrega/elimina participante
CREATE OR REPLACE FUNCTION update_group_current_size()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' OR TG_OP = 'UPDATE' THEN
        UPDATE grupos 
        SET current_size = (
            SELECT COALESCE(SUM(reserved_units), 0) 
            FROM participantes 
            WHERE group_id = NEW.group_id
            AND status != 'CANCELLED'
        )
        WHERE id = NEW.group_id;
        
        -- Si se alcanza la meta, cambiar estado a PICKUP y generar QR
        UPDATE grupos
        SET status = 'PICKUP',
            qr_code = 'QR_' || id::text || '_' || EXTRACT(EPOCH FROM NOW())::bigint
        WHERE id = NEW.group_id
        AND current_size >= target_size
        AND status = 'ACTIVE'
        AND qr_code IS NULL;
        
        RETURN NEW;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE grupos 
        SET current_size = (
            SELECT COALESCE(SUM(reserved_units), 0) 
            FROM participantes 
            WHERE group_id = OLD.group_id
            AND status != 'CANCELLED'
        )
        WHERE id = OLD.group_id;
        RETURN OLD;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger para actualizar current_size
CREATE TRIGGER update_group_size_on_participant
    AFTER INSERT OR DELETE OR UPDATE OF reserved_units, status ON participantes
    FOR EACH ROW EXECUTE FUNCTION update_group_current_size();

-- Función para expirar grupos automáticamente
CREATE OR REPLACE FUNCTION expire_active_groups()
RETURNS void AS $$
BEGIN
    UPDATE grupos
    SET status = 'EXPIRED'
    WHERE status = 'ACTIVE'
    AND expires_at < NOW();
END;
$$ LANGUAGE plpgsql;

-- Función para validar cuando todas las reservas están validadas
CREATE OR REPLACE FUNCTION check_group_completion()
RETURNS TRIGGER AS $$
DECLARE
    total_participants INTEGER;
    validated_participants INTEGER;
BEGIN
    -- Contar participantes totales y validados
    SELECT 
        COUNT(*) FILTER (WHERE status != 'CANCELLED'),
        COUNT(*) FILTER (WHERE status = 'VALIDATED')
    INTO total_participants, validated_participants
    FROM participantes
    WHERE group_id = COALESCE(NEW.group_id, OLD.group_id);
    
    -- Si todos están validados, cambiar estado a VALIDATED
    IF validated_participants = total_participants AND total_participants > 0 THEN
        UPDATE grupos
        SET status = 'VALIDATED',
            validated_at = NOW()
        WHERE id = COALESCE(NEW.group_id, OLD.group_id)
        AND status = 'PICKUP';
    END IF;
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Trigger para verificar completitud cuando se valida un participante
CREATE TRIGGER check_group_completion_on_validation
    AFTER UPDATE OF is_validated, status ON participantes
    FOR EACH ROW
    WHEN (NEW.status = 'VALIDATED' AND OLD.status != 'VALIDATED')
    EXECUTE FUNCTION check_group_completion();

-- Función para actualizar estadísticas del usuario
CREATE OR REPLACE FUNCTION update_user_stats()
RETURNS TRIGGER AS $$
BEGIN
    -- Actualizar completed_groups cuando un grupo pasa a VALIDATED
    IF NEW.status = 'VALIDATED' AND OLD.status != 'VALIDATED' THEN
        UPDATE usuarios
        SET completed_groups = completed_groups + 1
        WHERE id IN (
            SELECT user_id FROM participantes WHERE group_id = NEW.id
        );
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger para actualizar estadísticas
CREATE TRIGGER update_user_stats_on_group_validation
    AFTER UPDATE OF status ON grupos
    FOR EACH ROW
    WHEN (NEW.status = 'VALIDATED' AND OLD.status != 'VALIDATED')
    EXECUTE FUNCTION update_user_stats();

-- Función para prevenir grupos activos duplicados por producto
CREATE OR REPLACE FUNCTION prevent_duplicate_active_groups()
RETURNS TRIGGER AS $$
BEGIN
    -- Verificar si ya existe un grupo activo para este producto
    IF EXISTS (
        SELECT 1 FROM grupos
        WHERE product_id = NEW.product_id
        AND status = 'ACTIVE'
        AND expires_at > NOW()
        AND id != NEW.id
    ) THEN
        RAISE EXCEPTION 'Ya existe una oferta activa para este producto';
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger para prevenir grupos duplicados
CREATE TRIGGER prevent_duplicate_active_groups_trigger
    BEFORE INSERT OR UPDATE ON grupos
    FOR EACH ROW
    WHEN (NEW.status = 'ACTIVE')
    EXECUTE FUNCTION prevent_duplicate_active_groups();

-- =====================================================
-- 10. VISTAS ÚTILES
-- =====================================================

-- Vista para grupos con información completa (usada en MyGroupsScreen.kt)
CREATE OR REPLACE VIEW grupos_completos AS
SELECT 
    g.*,
    p.name as product_full_name,
    p.description as product_description,
    COUNT(DISTINCT part.user_id) FILTER (WHERE part.status != 'CANCELLED') as participant_count,
    COUNT(DISTINCT part.user_id) FILTER (WHERE part.status = 'VALIDATED') as validated_count
FROM grupos g
JOIN productos p ON g.product_id = p.id
LEFT JOIN participantes part ON g.id = part.group_id
GROUP BY g.id, p.id;

-- Vista para productos con información de bodega (usada en HomeScreen.kt, ProductDetailScreen.kt)
CREATE OR REPLACE VIEW productos_con_bodega AS
SELECT 
    p.*,
    b.name as store_full_name,
    b.address as store_address,
    b.phone as store_phone,
    b.latitude as store_latitude,
    b.longitude as store_longitude,
    b.rating as store_rating
FROM productos p
JOIN bodegas b ON p.store_id = b.id;

-- =====================================================
-- 11. COMENTARIOS DE DOCUMENTACIÓN
-- =====================================================

COMMENT ON TABLE usuarios IS 'Usuarios del sistema (consumidores y bodegueros). Usada en AuthRepository.kt, HomeScreen.kt, ProfileScreen.kt. Autenticación basada en alias y contraseña';
COMMENT ON TABLE bodegas IS 'Bodegas registradas. Usada en StoreRepository.kt, StoreDashboardScreen.kt, StoreProfileScreen.kt';
COMMENT ON TABLE productos IS 'Productos/ofertas publicadas por bodegas. Usada en ProductRepository.kt, PublishProductScreen.kt, ProductDetailScreen.kt';
COMMENT ON TABLE grupos IS 'Grupos de compra colectiva. Usada en GroupRepository.kt, MyGroupsScreen.kt, GroupDetailScreen.kt. Estados: ACTIVE → PICKUP → VALIDATED';
COMMENT ON TABLE participantes IS 'Participantes en grupos. Usada en GroupRepository.createReservation(), GroupDetailScreen.kt';
COMMENT ON TABLE categorias IS 'Categorías de productos. Usada en ProductRepository.getCategories(), HomeScreen.kt';

COMMENT ON COLUMN grupos.status IS 'Máquina de estados: ACTIVE (reserva) → PICKUP (meta cumplida) → VALIDATED (todos retirados) → COMPLETED. ACTIVE puede expirar → EXPIRED';
COMMENT ON COLUMN grupos.qr_code IS 'QR único generado cuando current_size >= target_size. Usado en PickupQRScreen.kt y ScanQRScreen.kt';
COMMENT ON COLUMN participantes.is_validated IS 'Indica si el participante ya retiró su producto escaneando el QR. Usado en ScanQRScreen.kt';

-- =====================================================
-- 12. POLÍTICAS RLS (Row Level Security) - OPCIONAL
-- =====================================================

-- Habilitar RLS (descomentar si se usa)
-- ALTER TABLE usuarios ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE bodegas ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE productos ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE grupos ENABLE ROW LEVEL SECURITY;
-- ALTER TABLE participantes ENABLE ROW LEVEL SECURITY;

-- Ejemplo de política: usuarios solo pueden ver sus propios datos
-- CREATE POLICY "Users can view own data" ON usuarios
--     FOR SELECT USING (auth.uid() = id);

-- =====================================================
-- FIN DEL ESQUEMA
-- =====================================================
