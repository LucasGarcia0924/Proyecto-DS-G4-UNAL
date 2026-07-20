package vista;


/**
 * Clase que imprime en consola la interfaz con la que interactua el usuario.
 */
public class consola {

    public consola() {
    }

    /**
     * Muestra el mensaje de bienvenida.
     */
    public void mostrarBienvenida() {
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║           Bienvenido al Asistente de Fusiones        ║");
        System.out.println("║      Enfoncado en Persona 3 R  - Estructuras de Datos ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");
    }

    public void mostrarMenuPrincipal() {
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║               Selecciona una opción válida            ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║                  1. Ver tu Equipo actual             ║");
        System.out.println("║                  2. Ver tu Registro                 ║");
        System.out.println("║                  3. Ver tus Social Links            ║");
        System.out.println("║                  4. Buscar una Persona              ║");
        System.out.println("║                  5. Ver Fusiones Especiales         ║");
        System.out.println("║                  6. Cambiar de Usuario              ║");
        System.out.println("║                  7. Salir                           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝\n");
    }

    public void mostrarMensaje(String mensaje) {
        System.out.println(mensaje);
    } 
}