// Lógica para mostrar/ocultar contraseña
const pwShowHide = document.querySelectorAll(".pw_hide");

pwShowHide.forEach((icon) => {
  icon.addEventListener("click", () => {
    let getPwInput = icon.parentElement.querySelector("input");
    if (getPwInput.type === "password") {
      getPwInput.type = "text";
      icon.classList.replace("uil-eye-slash", "uil-eye");
    } else {
      getPwInput.type = "password";
      icon.classList.replace("uil-eye", "uil-eye-slash");
    }
  });
});

// Manejo del login con AJAX para asegurar que las cookies se guarden
document.addEventListener("DOMContentLoaded", function() {
  const loginForm = document.querySelector(".login_form form");
  if (loginForm) {
    loginForm.addEventListener("submit", async function(e) {
      e.preventDefault();
      
      const identifier = this.querySelector("input[name='identifier']").value;
      const password = this.querySelector("input[name='password']").value;
      
      try {
        // Enviar credenciales CON cookies (credentials: 'include')
        const response = await fetch("/login", {
          method: "POST",
          headers: {
            "Content-Type": "application/x-www-form-urlencoded",
          },
          credentials: "include", // IMPORTANTE: Incluye/guarda cookies
          body: new URLSearchParams({
            identifier: identifier,
            password: password
          })
        });
        
        // Si es redirect (3xx), fetch no lo sigue automáticamente
        // pero la cookie YA fue guardada en el paso anterior
        if (response.redirected) {
          // El servidor hizo redirect, ir a la nueva URL
          window.location.href = response.url;
        } else if (response.ok) {
          // Si no hay redirect (respuesta 200), esperar un poco y recargar
          setTimeout(() => {
            window.location.href = "/";
          }, 500);
        } else {
          // Error en autenticación
          const text = await response.text();
          console.error("Error en login:", text);
          // El servidor ya hace redirect con error, confiar en eso
        }
      } catch (error) {
        console.error("Error en AJAX login:", error);
        // Fallback: enviar form tradicionalmente
        this.submit();
      }
    });
  }
});