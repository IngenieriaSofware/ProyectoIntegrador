function loadUsuarios() {
    fetch('/api/admin/usuarios?page=1&pageSize=20')
        .then(r => r.json())
        .then(data => renderUsuarios(Array.isArray(data) ? data : []))
        .catch(() => {
            document.getElementById('usuariosContainer').innerHTML =
                '<div class="empty-state">Error al cargar usuarios</div>';
        });
}

function searchUsuarios() {
    const q = document.getElementById('searchInput').value.trim();
    if (!q) { loadUsuarios(); return; }
    fetch('/api/admin/usuarios/search?q=' + encodeURIComponent(q))
        .then(r => r.json())
        .then(data => renderUsuarios(Array.isArray(data) ? data : []))
        .catch(console.error);
}

function renderUsuarios(usuarios) {
    const c = document.getElementById('usuariosContainer');
    if (!usuarios.length) {
        c.innerHTML = '<div class="empty-state">No hay usuarios para mostrar</div>';
        return;
    }
    c.innerHTML = usuarios.map(u => `
        <div class="usuario-item">
            <div class="usuario-info">
                <div class="usuario-name">${u.nombre} ${u.apellido}</div>
                <div class="usuario-email">${u.email} — DNI: ${u.dni}</div>
                <div class="usuario-roles">${(u.roles||[]).map(r=>`<span class="role-pill">${r}</span>`).join('')}</div>
            </div>
            <div class="usuario-actions">
                <button class="btn-small btn-info"    onclick="verDetalles(${u.id})">Detalles</button>
                <button class="btn-small btn-warning" onclick="asignarRol(${u.id})">Roles</button>
                <button class="btn-small btn-danger"  onclick="deshabilitarUsuario(${u.id})">Ban</button>
            </div>
        </div>`).join('');
}

function verDetalles(id) {
    alert('Función de detalles en desarrollo (ID: ' + id + ')');
}

function asignarRol(id) {
    const rol = prompt('Ingrese el rol a asignar (DOCENTE, ESTUDIANTE, ADMINISTRADOR):');
    if (!rol) return;
    fetch(`/api/admin/usuarios/${id}/role/assign?rol=${encodeURIComponent(rol)}`, { method: 'POST' })
        .then(r => r.json()).then(d => { alert(d.message); loadUsuarios(); })
        .catch(() => alert('Error al asignar rol'));
}

function deshabilitarUsuario(id) {
    const razon = prompt('Ingrese la razón del ban:');
    if (!razon) return;
    fetch(`/api/admin/usuarios/${id}/disable?razon=${encodeURIComponent(razon)}`, { method: 'POST' })
        .then(r => r.json()).then(d => { alert(d.message); loadUsuarios(); })
        .catch(() => alert('Error al deshabilitar usuario'));
}

window.addEventListener('load', loadUsuarios);