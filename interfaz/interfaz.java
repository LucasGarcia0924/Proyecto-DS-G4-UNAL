package interfaz;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            String opcion = escaner.nextLine();
            switch (opcion) {
                // Lógica para llamar a las funciones que se encargan de cada opción del menú
                case "1":
                    verEquipo();
                        break;
                case "2":
                    verRegistro();
                        break;
                case "3":
                    verSocialLinks();
                        break;
                case "4":
                    buscarPersona();
                        break;
                case "5":
                    verFusionesEspeciales();
                        break;
                case "6":
                    cambiarUsuario();
                        break;
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

        usuario.User tu = new modelos.usuario.User();;
        tu.nombreUsuario = nombreUsuario;
        tu.salt = salt;
        tu.contraseña = hash;
        tu.preguntaHash = pregunta;
        tu.respuestaHash = respuestaHash;
        tu.equipo = new ArrayList<>(Arrays.asList("Alice"));
        tu.owned = new HashSet<>();
        tu.registerOwned("Alice");
        tu.socialLinks = new HashMap<>();
        for (String npc : Arrays.asList("Yukari","Junpei","Mitsuru","Akihiko","Fuuka","Koromaru","Ken","Shinjiro")) {
            tu.socialLinks.put(npc, 0);
        }
        tu.lastModified = Instant.now().toString();

        mU.saveUser(tu);
        System.out.println("Usuario creado exitosamente.");
    }
    public void verEquipo() throws Exception {
        usuario.managerUsuario mU = usuario.new managerUsuario();
        usuario.User tu = mU.getUsuarioActivo();

        if (tu == null) {
            System.out.println("No hay usuario activo. Por favor, inicia sesión o crea un usuario.");
            return;
        }

        Path personasDir = Paths.get("Data/personas");
        ObjectMapper mapper = new ObjectMapper();
        List<modelos.engine.Persona> personas = new ArrayList<>();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(personasDir, "*.json")) {
            for (Path p : ds) {
                modelos.engine.Persona persona = mapper.readValue(p.toFile(), modelos.engine.Persona.class);
                personas.add(persona);
            }
        }

        modelos.engine.Registro registro = new modelos.engine.Registro();
        registro.buildFrom(personas);

        modelos.engine.indiceFusiones indiceF = new modelos.engine.indiceFusiones();
        modelos.engine.grafoSocialLinks socialGraph = new modelos.engine.grafoSocialLinks();
        modelos.engine.indicePorNivel levelIndex = new modelos.engine.indicePorNivel();

        usuario.UserView userView = new usuario().new UserView(tu, registro, indiceF, socialGraph, levelIndex, 8);

        boolean submenu = true;
        while (submenu) {
            consola.mostrarMenuEquipo();
            System.out.print("Opción: ");
            String opcion = escaner.nextLine();

            switch (opcion) {
                case "1":
                    System.out.println("Usuario activo: " + tu.nombreUsuario);
                    userView.mostrarEquipoDetallado();
                    break;
                case "2":
                    System.out.print("Ingresa el nombre de la persona que quieres agregar al equipo: ");
                    String nombre = escaner.nextLine();
                    if (userView.addToTeam(nombre, mU)) {
                        System.out.println("Persona añadida al equipo.");
                    } else {
                        System.out.println(" No se pudo agregar.");
                    }
                    break;
                case "3":
                    System.out.print("Ingresa el nombre de la persona a liberar: ");
                    String liberar = escaner.nextLine();
                    if (userView.releaseFromTeam(liberar, mU)) {
                        System.out.println("Persona liberada del equipo.");
                    } else {
                        System.out.println("No se encontró esa persona en el equipo.");
                    }
                    break;
                case "4":
                    submenu = false;
                    break;
                default:
                    System.out.println("Opción no válida.");
            }
        }
    }

    public void verRegistro() throws Exception {
        usuario.managerUsuario mU = usuario.new managerUsuario();
        usuario.User tu = mU.getUsuarioActivo();

        if (tu == null) {
            System.out.println("No hay usuario activo. Por favor, inicia sesión o crea un usuario.");
            return;
        }

        Path personasDir = Paths.get("Data/personas");
        ObjectMapper mapper = new ObjectMapper();
        List<modelos.engine.Persona> personas = new ArrayList<>();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(personasDir, "*.json")) {
            for (Path p : ds) {
                modelos.engine.Persona persona = mapper.readValue(p.toFile(), modelos.engine.Persona.class);
                personas.add(persona);
            }
        }

        modelos.engine.Registro registro = new modelos.engine.Registro();
        registro.buildFrom(personas);

        modelos.engine.indiceFusiones indiceF = new modelos.engine.indiceFusiones();
        modelos.engine.grafoSocialLinks socialGraph = new modelos.engine.grafoSocialLinks();
        modelos.engine.indicePorNivel levelIndex = new modelos.engine.indicePorNivel();

        usuario.UserView userView = new usuario().new UserView(tu, registro, indiceF, socialGraph, levelIndex, 12);

        boolean submenu = true;
        while (submenu) {
            consola.mostrarMenuRegistro();
            System.out.print("Opción: ");
            String opcion = escaner.nextLine();

            switch (opcion) {
                case "1":
                    userView.printRegistryWithOwned();
                    break;
                case "2":
                    System.out.print("Nombre: ");
                    String nombre = escaner.nextLine();

                    modelos.engine.Persona nueva = registro.buscarPorNombre(nombre);
                    if (nueva == null) {
                        System.out.println("No se encontró una persona con ese nombre en el registro maestro.");
                        break;
                    }

                    tu.registerOwned(nombre);
                    mU.saveUser(tu);
                    System.out.println("Persona registrada en el usuario. Arcano: " + nueva.arcano + ", Nivel: " + nueva.nivel);
                    break;
                case "3":
                    System.out.print("Ingresa la persona que quieres mover del registro al equipo: ");
                    String agregar = escaner.nextLine();
                    if (!userView.usuario.hasOwned(agregar)) {
                        System.out.println("La Persona no está registrada y no se puede agregar.");
                    }
                    else if (userView.addToTeam(agregar, mU)) {
                        System.out.println("Persona añadida al equipo correctamente.");
                    }
                    else {
                        System.out.println("No se pudo agregar, revisa el nombre que ingresaste.");
                    }
                    break;
                case "4":
                    submenu = false;
                    break;
                default:
                    System.out.println("Opción no válida.");
            }
        }
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