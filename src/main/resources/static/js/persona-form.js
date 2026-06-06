// ── Utilidad: aplicar estado visual ──────────────────────────────────────
function setFieldState(input, hint, isValid, message) {
    if (isValid) {
        input.classList.add('valid');
        input.classList.remove('invalid');
        hint.textContent = message;
        hint.className = 'field-hint ok';
    } else {
        input.classList.add('invalid');
        input.classList.remove('valid');
        hint.textContent = message;
        hint.className = 'field-hint error';
    }
}

function clearFieldState(input, hint) {
    input.classList.remove('valid', 'invalid');
    hint.textContent = '';
    hint.className = 'field-hint';
}

// ── DNI ───────────────────────────────────────────────────────────────────
const dniInput = document.getElementById('dni');
const dniHint  = document.getElementById('dni-hint');

dniInput.addEventListener('input', () => {
    dniInput.value = dniInput.value.replace(/\D/g, '');
    const len = dniInput.value.length;
    if (len === 0) {
        clearFieldState(dniInput, dniHint);
    } else if (len < 7) {
        setFieldState(dniInput, dniHint, false, `Faltan ${7 - len} dígito(s) como mínimo`);
    } else {
        setFieldState(dniInput, dniHint, true, '✓ DNI válido');
    }
});

// ── EMAIL ─────────────────────────────────────────────────────────────────
const emailInput = document.getElementById('email');
const emailHint  = document.getElementById('email-hint');
const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

emailInput.addEventListener('input', () => {
    const val = emailInput.value.trim();
    if (val.length === 0) { clearFieldState(emailInput, emailHint); return; }
    if (!val.includes('@')) {
        setFieldState(emailInput, emailHint, false, 'Falta el símbolo @');
    } else if (!emailRegex.test(val)) {
        setFieldState(emailInput, emailHint, false, 'Formato inválido — ej: usuario@mail.com');
    } else {
        setFieldState(emailInput, emailHint, true, '✓ Email válido');
    }
});

// ── CONTRASEÑA ────────────────────────────────────────────────────────────
const pwInput = document.getElementById('password');
const pwHint  = document.getElementById('pw-hint');

pwInput.addEventListener('input', () => {
    const val = pwInput.value;
    if (val.length === 0) { clearFieldState(pwInput, pwHint); return; }
    const errors = [];
    if (val.length < 8)      errors.push('mín. 8 carac.');
    if (!/[a-z]/.test(val))  errors.push('1 minúsc.');
    if (!/[A-Z]/.test(val))  errors.push('1 mayúsc.');
    if (errors.length > 0) {
        setFieldState(pwInput, pwHint, false, 'Falta: ' + errors.join(' · '));
    } else {
        setFieldState(pwInput, pwHint, true, '✓ Contraseña válida');
    }
});

// ── TELÉFONO ──────────────────────────────────────────────────────────────
const telInput = document.getElementById('telefono');
const telHint  = document.getElementById('tel-hint');

telInput.addEventListener('keydown', (e) => {
    const allowed = ['Backspace','Delete','ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Tab','Home','End'];
    if (allowed.includes(e.key)) return;
    if (e.ctrlKey || e.metaKey) return;
    if (!/^\d$/.test(e.key)) e.preventDefault();
});

telInput.addEventListener('input', () => {
    let digits = telInput.value.replace(/\D/g, '').slice(0, 13);
    let formatted = '';
    if (digits.length > 0) formatted  = '+' + digits.slice(0, 2);
    if (digits.length > 2) formatted += ' '  + digits.slice(2, 3);
    if (digits.length > 3) formatted += ' '  + digits.slice(3, 6);
    if (digits.length > 6) formatted += ' '  + digits.slice(6, 9);
    if (digits.length > 9) formatted += ' '  + digits.slice(9, 13);
    telInput.value = formatted;

    if (digits.length === 0) {
        clearFieldState(telInput, telHint);
    } else if (digits.length < 13) {
        setFieldState(telInput, telHint, false, `Faltan ${13 - digits.length} dígito(s)`);
    } else {
        setFieldState(telInput, telHint, true, '✓ Teléfono válido');
    }
});

// ── DEPARTAMENTO (visible solo si DOCENTE está seleccionado) ──────────────
const rolDocenteCheckbox  = document.getElementById('rolDocente');
const docenteFields       = document.getElementById('docente-fields');
const departamentoInput   = document.getElementById('departamento');

function toggleDocenteFields() {
    if (rolDocenteCheckbox.checked) {
        docenteFields.style.display = 'block';
    } else {
        docenteFields.style.display = 'none';
        departamentoInput.value = '';
    }
}

rolDocenteCheckbox.addEventListener('change', toggleDocenteFields);

// ── VALIDACIÓN DE ROLES AL SUBMIT ─────────────────────────────────────────
const personaForm = document.getElementById('personaForm');
const rolesHint   = document.getElementById('roles-hint');

personaForm.addEventListener('submit', (e) => {
    const checks = document.querySelectorAll('input[name="roles"]:checked');
    if (checks.length === 0) {
        e.preventDefault();
        rolesHint.classList.add('visible');
    } else {
        rolesHint.classList.remove('visible');
    }
});

document.querySelectorAll('input[name="roles"]').forEach(cb => {
    cb.addEventListener('change', () => {
        const checks = document.querySelectorAll('input[name="roles"]:checked');
        if (checks.length > 0) rolesHint.classList.remove('visible');
    });
});
