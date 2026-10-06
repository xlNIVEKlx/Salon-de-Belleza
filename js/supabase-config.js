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
    },

    /**
     * Muestra un modal de confirmación con el diseño de la app.
     * Retorna una Promise<boolean> que resuelve a true si se confirma, o false si se cancela.
     */
    confirm({ title = '¿Estás segura?', message = '¿Deseas continuar con esta acción?', itemName = '', confirmText = 'Eliminar', confirmClass = 'bg-red-600 hover:bg-red-700' } = {}) {
        return new Promise((resolve) => {
            const modal = document.getElementById('modal-confirm');
            const titleEl = document.getElementById('modal-confirm-title');
            const msgEl = document.getElementById('modal-confirm-message');
            const itemEl = document.getElementById('modal-confirm-item');
            const btnCancel = document.getElementById('modal-confirm-cancel');
            const btnOk = document.getElementById('modal-confirm-ok');

            if (!modal) {
                resolve(window.confirm(message));
                return;
            }

            titleEl.textContent = title;
            msgEl.textContent = message;

            if (itemName) {
                itemEl.textContent = `"${itemName}"`;
                itemEl.classList.remove('hidden');
            } else {
                itemEl.classList.add('hidden');
            }

            btnOk.textContent = confirmText;
            btnOk.className = `w-full px-4 py-2.5 text-white ${confirmClass} rounded-xl font-medium text-sm shadow transition`;

            modal.classList.remove('hidden');

            const cleanup = () => {
                modal.classList.add('hidden');
                btnCancel.removeEventListener('click', onCancel);
                btnOk.removeEventListener('click', onOk);
                modal.removeEventListener('click', onBackdrop);
                document.removeEventListener('keydown', onKey);
            };

            const onCancel = () => {
                cleanup();
                resolve(false);
            };

            const onOk = () => {
                cleanup();
                resolve(true);
            };

            const onBackdrop = (e) => {
                if (e.target === modal) {
                    onCancel();
                }
            };

            const onKey = (e) => {
                if (e.key === 'Escape') {
                    onCancel();
                }
            };

            btnCancel.addEventListener('click', onCancel);
            btnOk.addEventListener('click', onOk);
            modal.addEventListener('click', onBackdrop);
            document.addEventListener('keydown', onKey);
        });
    }
};
