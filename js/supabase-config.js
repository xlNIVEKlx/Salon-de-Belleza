// ============================================================
// SUPABASE CLIENT CONFIGURATION
// ============================================================
// INSTRUCCIONES DE CONFIGURACIÓN:
// 1. Ve a https://supabase.com y crea un proyecto gratuito.
// 2. En el Dashboard → Settings → API, copia:
//    - "Project URL"     →  pega en SUPABASE_URL
//    - "anon public key" →  pega en SUPABASE_ANON_KEY
// 3. Ejecuta el archivo sql/supabase_schema.sql en el
//    SQL Editor de tu proyecto Supabase.
// ============================================================

const SUPABASE_URL = 'https://abtiwgajogkemhjwrgcs.supabase.co';
const SUPABASE_ANON_KEY = 'sb_publishable_0MTFeXm9ewITUR8LUviYJA_Ijlzdixp';

// Cliente global de Supabase (usado por todos los módulos)
const db = supabase.createClient(SUPABASE_URL, SUPABASE_ANON_KEY);

// ============================================================
// UTILIDADES COMPARTIDAS (UI)
// Definidas aquí para que estén disponibles antes que cualquier
// otro módulo JS que las necesite.
// ============================================================

const UI = {
    /**
     * Muestra una alerta flotante animada.
     * @param {string} message  Texto a mostrar
     * @param {'success'|'error'|'warning'} type  Tipo de alerta
     */
    showAlert(message, type = 'success') {
        const container = document.getElementById('alert-container');
        const el = document.createElement('div');
        el.className = `alert alert-${type}`;

        const icons = {
            success: 'fa-check-circle',
            error: 'fa-circle-exclamation',
            warning: 'fa-triangle-exclamation'
        };

        el.innerHTML = `
            <i class="fa-solid ${icons[type] || icons.success}"></i>
            <p>${message}</p>
        `;

        container.appendChild(el);

        // Doble rAF para asegurar que la transición se anime
        requestAnimationFrame(() => {
            requestAnimationFrame(() => el.classList.add('show'));
        });

        setTimeout(() => {
            el.classList.remove('show');
            setTimeout(() => el.remove(), 350);
        }, 4000);
    },

    /**
     * Formatea un número como moneda colombiana (COP).
     * @param {number} val  Valor numérico
     * @returns {string}    Ej: "$ 85.000"
     */
    formatCurrency(val) {
        return new Intl.NumberFormat('es-CO', {
            style: 'currency',
            currency: 'COP',
            maximumFractionDigits: 0
        }).format(val || 0);
    }
};
