package interfaz;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import modelos.usuario;
import modelos.usuario.PasswordUtil;
import vista.consola;

/**
 * Clase orquestadora principal del asistente de fusiones.
 * Gestiona el flujo completo de la interfaz con la que interactua el usuario.
 */
public class interfaz {
    private final modelos.usuario usuario;
    private final vista.consola consola;
    private boolean finPrograma;
    private final Scanner escaner;

    public interfaz() {
        this.consola = new consola();
        this.finPrograma = false;
        this.usuario = new usuario();
        this.escaner = new Scanner(System.in);
    }

    /**
     * Inicia el juego.
     */
    public void iniciar() {
        
        consola.mostrarBienvenida();

        try {
            usuario.seleccionarUsuario();
        } catch (Exception e) {
            System.out.println("Error al seleccionar usuario: " + e.getMessage());
        }
        
        // 2. Mostrar menú principal
        try {
            while (!finPrograma) {
            consola.mostrarMenuPrincipal();
            String opcion = System.console().readLine();
            switch (opcion) {
                // Lógica para llamar a las funciones que se encargan de cada opción del menú
                case "1":
                    verEquipo();
                case "2":
                    verRegistro();
                case "3":
                    verSocialLinks();
                case "4":
                    buscarPersona();
                case "5":
                    verFusionesEspeciales();
                case "6":
                    cambiarUsuario();
                case "7":
                    finPrograma = true;
                    System.out.println("Éxitos en el juego. ¡Hasta la próxima!");
                    break;
                default:
                    System.out.println("Opción no válida. Por favor, seleccione una opción del 1 al 7.");
            }
        }
        } catch (Exception e) {
            System.out.println("Error durante la ejecución del programa: " + e.getMessage());
        }
    }
    public synchronized void crearUsuario() throws Exception {
        usuario.managerUsuario mU = usuario.new managerUsuario();

        System.out.print("Ingresa tu nombre de usuario: ");
        String nombreUsuario = escaner.nextLine();
        System.out.print("Ingresa contraseña: ");
        String contraseña = escaner.nextLine();
        System.out.print("Ingresa pregunta de seguridad: ");
        String pregunta = escaner.nextLine();
        System.out.print("Ingresa respuesta: ");
        String respuesta = escaner.nextLine();

        if (mU.usernameExists(nombreUsuario)) throw new IllegalArgumentException("Usuario ya existe");
        String salt = PasswordUtil.generateSaltBase64();
        String hash = PasswordUtil.hashPassword(contraseña, salt);
        String respuestaHash = PasswordUtil.hashPassword(respuesta, salt);

        usuario.User tu = usuario.new User();
        tu.nombreUsuario = nombreUsuario;
        tu.salt = salt;
        tu.contraseña = hash;
        tu.preguntaHash = pregunta;
        tu.respuestaHash = respuestaHash;
        tu.equipo = new ArrayList<>(Arrays.asList("Orpheus"));
        tu.owned = new HashSet<>();
        tu.socialLinks = new HashMap<>();
        for (String npc : Arrays.asList("Yukari","Junpei","Mitsuru","Akihiko","Fuuka","Koromaru","Ken","Shinjiro")) {
            tu.socialLinks.put(npc, 0);
        }
        tu.lastModified = Instant.now().toString();

        mU.saveUser(tu);
        System.out.println("Usuario creado exitosamente.");
    }
    public void verEquipo() throws Exception {
        // Implementar la lógica para ver y administrar el equipo del usuario
        usuario.managerUsuario mU = usuario.new managerUsuario();
        usuario.User tu = mU.getUsuarioActivo();

        if (tu == null) {
            System.out.println("No hay usuario activo. Por favor, inicia sesión o crea un usuario.");
        }
        else {
            System.out.println("Usuario activo: " + tu.nombreUsuario);
            System.out.println("Equipo actual: ");
            
        }
    }
    public void verRegistro() {
        // Implementar la lógica para ver y administrar el registro del usuario
    }
    public void verSocialLinks() {
        // Implementar la lógica para ver y administrar los social links del usuario
    }
    public void buscarPersona() {
        // Implementar la lógica para buscar una persona
    }
    public void verFusionesEspeciales() {
        // Implementar la lógica para ver y realizar las fusiones especiales
    }
    public void cambiarUsuario() {
        // Implementar la lógica para cambiar de usuario
        boolean bandera = false;
        boolean flag = true;
        while (flag == true) {
            System.out.print("¿Desea cambiar de usuario? (s/n): ");
            String opcion = escaner.nextLine();
            switch (opcion.toLowerCase()) {
                case "s" : {
                    bandera = true;
                    flag = false;
                    break;
                }
                case "n" : {
                // Se rompe el bucle de selección pero no se continua el proceso de cambio de usuario
                    flag = false;    
                    break;
                }
                default:
                     System.out.println("Opción no válida. Por favor, seleccione 's' o 'n'.");
                break;     
            }
            // Se rompe el bucle de selección y se continua el proceso de cambio de usuario
                    }
        if (bandera == true){
            try {
                usuario.seleccionarUsuario();
            } 
            catch (Exception e) {
                System.out.println("Error al escoger usuario: " + e.getMessage());
            }
        }
    }
}