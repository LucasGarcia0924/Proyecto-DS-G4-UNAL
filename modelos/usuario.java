package modelos;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

import modelos.engine.*;
import vista.consola;
import interfaz.interfaz;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Clase que administra la información del usuario activo.
 */
public class usuario {
    private final Scanner escaner;
    private final consola consola;
    public User usuarioActivo;

    public usuario() {
        this.escaner = new Scanner(System.in);
        this.consola = new consola();
        this.usuarioActivo = null;
    }

    public static class User {
        public String nombreUsuario;
        public String contraseña; // base64
        public String preguntaHash; // base64
        public String respuestaHash; // base64
        public String salt;         // base64
        public List<String> equipo = new ArrayList<>(); // nombres
        public Set<String> owned = new HashSet<>();   // nombres
        public Map<String,Integer> socialLinks = new HashMap<>();
        public String lastModified;
        public User() {}
}

    public static class PasswordUtil {
        private static final int SALT_BYTES = 16;
        private static final int HASH_BYTES = 32;
        private static final int ITERATIONS = 100_000;

        public static String generateSaltBase64() {
            byte[] salt = new byte[SALT_BYTES];
            new SecureRandom().nextBytes(salt);
            return Base64.getEncoder().encodeToString(salt);
        }

        public static String hashPassword(String password, String saltBase64) throws Exception {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, HASH_BYTES * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        }

        public static boolean verifyPassword(String password, String saltBase64, String expectedHashBase64) throws Exception {
            String computed = hashPassword(password, saltBase64);
            return MessageDigest.isEqual(Base64.getDecoder().decode(computed), Base64.getDecoder().decode(expectedHashBase64));
        }
    }

    public class managerUsuario {
        private final Path usersDir = Paths.get("data/users");
        private final ObjectMapper M = new ObjectMapper();
        private final ConcurrentHashMap<String, User> users = new ConcurrentHashMap<>();

        public managerUsuario() throws IOException {
            if (!Files.exists(usersDir)) Files.createDirectories(usersDir);
            loadAllUsers();
        }

        private void loadAllUsers() throws IOException {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(usersDir, "*.json")) {
                for (Path p : ds) {
                    User u = M.readValue(p.toFile(), User.class);
                    users.put(u.nombreUsuario.toLowerCase(), u);
                }
            }
        }

        public boolean usernameExists(String username) { return users.containsKey(username.toLowerCase()); }

        public User authenticate(String username, String password) throws Exception {
            User u = users.get(username.toLowerCase());
            if (u == null) return null;
            if (PasswordUtil.verifyPassword(password, u.salt, u.contraseña)) return u;
            return null;
        }

        public User recobrarContraseña() throws Exception {
            System.out.print("Ingresa tu nombre de usuario: ");
            String username = escaner.nextLine();

            User u = getUser(username);
            if (u == null) {
                System.out.println("Usuario no encontrado.");
                return null;
            }

            System.out.println("Pregunta de seguridad: " + u.preguntaHash);
            System.out.print("Ingresa tu respuesta: ");
            String respuesta = escaner.nextLine();

            if (PasswordUtil.verifyPassword(respuesta, u.salt, u.respuestaHash)) {
                System.out.print("Ingresa tu nueva contraseña: ");
                String nuevaContraseña = escaner.nextLine();
                String nuevaHash = PasswordUtil.hashPassword(nuevaContraseña, u.salt);
                u.contraseña = nuevaHash;
                saveUser(u);
                return u;
            }

            return null;
        }

        public synchronized void saveUser(User u) throws IOException {
            u.lastModified = Instant.now().toString();
            Path out = usersDir.resolve(u.nombreUsuario + ".json");
            M.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), u);
        }

        public User getUser(String username) { return users.get(username.toLowerCase()); }

        public User getUsuarioActivo() { return usuarioActivo; }

        public void setUsuarioActivo(User usuarioActivo) {usuario.this.usuarioActivo = usuarioActivo; }

        public void cargarEquipo() {
            // Lógica para cargar el equipo del usuario
        }
        public void cargarRegistro() {
            // Lógica para cargar el registro del usuario
        }
        public void cargarSL() {
            // Lógica para cargar los SL del usuario
        }
    }

    class UserView {
        public final User usuario;
        public final engine.Registro registro;
        public final engine.indiceFusiones indiceF;
        public final engine.grafoSocialLinks grafoS;
        public final engine.indicePorNivel indiceN;
        public final engine.Equipo equipo;

        public UserView(User user, engine.Registro registro, engine.indiceFusiones fusionIndex, engine.grafoSocialLinks socialGraph, engine.indicePorNivel levelIndex, int teamCapacity) {
            this.usuario = user;
            this.registro = registro;
            this.indiceF = fusionIndex;
            this.grafoS = socialGraph;
            this.indiceN = levelIndex;
            this.equipo = new engine().new Equipo(teamCapacity);
            resolveTeamFromUser();
        }

        // Resuelve nombres del usuario a referencias Persona y llena Team si hay espacio
        private void resolveTeamFromUser() {
            equipo.mostrarEquipo(); 
            for (String name : usuario.equipo) {
                Persona p = registro.buscarPorNombre(name);
                if (p != null) equipo.agregarPersona(p);
            }
        }

        // 1) Liberar persona del equipo
        public boolean releaseFromTeam(String nombrePersona) throws IOException {
            boolean removed = equipo.liberarPersona(nombrePersona);
            if (removed) {
                usuario.equipo.removeIf(n -> n.equalsIgnoreCase(nombrePersona));
                // persistir cambios mínimos
                return true;
            }
            return false;
        }

        // 2) Marcar automáticamente personas NO registradas previamente (owned false) -> owned true
        public List<String> autoRegisterUnlocked(List<String> personaNames) throws IOException {
            List<String> newly = new ArrayList<>();
            for (String nombre : personaNames) {
                if (!usuario.owned.contains(nombre)) {
                    usuario.owned.add(nombre);
                    newly.add(nombre);
                }
            }
            return newly;
        }

        // 3) Añadir persona ya registrada al equipo si hay espacio
        public boolean addRegisteredToTeam(String personaName) throws IOException {
            if (!usuario.owned.contains(personaName)) return false; // no está registrada
            if (!equipo.tieneEspacio()) return false;
            Persona p = registro.buscarPorNombre(personaName);
            if (p == null) return false;
            boolean ok = equipo.agregarPersona(p);
            if (ok) {
                usuario.equipo.add(personaName);
                return true;
            }
            return false;
        }

        // 4) Imprimir registro (con owned flag del usuario)
        public void printRegistryWithOwned() {
            System.out.println("Registro (owned marcado):");
            for (Persona p : registro.toList()) {
                System.out.println((usuario.owned.contains(p.nombre) ? "[X] " : "[ ] ") + p);
            }
        }

        // 5+6) Fusionar dos personas del equipo: si resultado no registrado, registrar; remover los dos y añadir resultado
        public boolean fuseAndReplace(String nameA, String nameB, managerUsuario um) throws IOException {
            Persona a = registro.buscarPorNombre(nameA);
            Persona b = registro.buscarPorNombre(nameB);
            if (a == null || b == null) return false;
            // comprobar que ambos están en el equipo (por referencia)
            boolean inTeamA = false, inTeamB = false;
            for (Persona p : equipo.getMiembros()) {
                if (p == a) inTeamA = true;
                if (p == b) inTeamB = true;
            }
            if (!inTeamA || !inTeamB) return false;

            List<String> results = indiceF.resultadoFusion(a, b);
            if (results.isEmpty()) return false; // no hay fusión conocida

            String resultName = results.get(0); // tomar el primero (puedes mostrar opciones en UI)
            Persona resultPersona = registro.buscarPorNombre(resultName);
            if (resultPersona == null) {
                // Si el resultado no existe en el registro maestro, no podemos crear uno nuevo aquí.
                return false;
            }

            // 5) Si usuario no tiene registrado el resultado, marcarlo automáticamente
            if (!usuario.owned.contains(resultName)) {
                usuario.owned.add(resultName);
            }

            // 6) Remover las dos personas del equipo y añadir el resultado (si hay espacio)
            equipo.removerPersona(a);
            equipo.removerPersona(b);
            // también actualizar user.team (nombres)
            usuario.equipo.removeIf(n -> n.equalsIgnoreCase(a.nombre) || n.equalsIgnoreCase(b.nombre));
            boolean added = equipo.agregarPersona(resultPersona);
            if (added) usuario.equipo.add(resultName);
            // persistir cambios del usuario
            um.saveUser(usuario);
            return true;
        }

        // Incrementar social link y auto registrar desbloqueos
        public List<String> increaseSocialLinkAndHandleUnlock(String npcName, int delta, managerUsuario um) throws IOException {
            List<String> unlocked = grafoS.aumentarRango(npcName, delta);
            List<String> newly = autoRegisterUnlocked(unlocked);
            um.saveUser(usuario);
            return newly;
        }
    }
    public void seleccionarUsuario() throws Exception {
        consola.menuUsuarios();

        boolean flag = true;
        while (flag == true) {
            String opcion = escaner.nextLine();
            switch (opcion) {
                case "1" : {
                    iniciarSesion();
                    flag = false;
                    break;
                }
                case "2" : {
                    interfaz interfaz = new interfaz();
                    interfaz.crearUsuario();
                    flag = false;    
                    break;
                }
                case "3" : {
                // Se rompe el bucle de selección pero no se continua el proceso de cambio de usuario
                    flag = false;    
                    break;
                }
                default:
                     System.out.println("Opción no válida. Por favor, seleccione '1' o '2'.");
                break;     
            }
            // Se rompe el bucle de selección y se continua el proceso de cambio de usuario
                    }
    }
    public void iniciarSesion() throws Exception {
        managerUsuario mU = new managerUsuario();
        System.out.print("Ingresa tu nombre de usuario: ");
        String nombreUsuario = escaner.nextLine();
        System.out.print("Ingresa tu contraseña: ");
        String contraseña = escaner.nextLine();

        User user = mU.authenticate(nombreUsuario, contraseña);
        if (user != null) {
            System.out.println("Inicio de sesión exitoso. ¡Bienvenido, " + user.nombreUsuario + "!");
            // Aquí puedes continuar con la lógica del programa después de un inicio de sesión exitoso
            mU.setUsuarioActivo(user);

        } else {
            for (int i = 0; i < 3; i++){
                System.out.println("Nombre de usuario o contraseña incorrectos. Intenta nuevamente.");
                System.out.print("Ingresa tu nombre de usuario: ");
                nombreUsuario = escaner.nextLine();
                System.out.print("Ingresa tu contraseña: ");
                contraseña = escaner.nextLine();
                user = mU.authenticate(nombreUsuario, contraseña);
                if (user != null) {
                    System.out.println("Inicio de sesión exitoso. ¡Bienvenido, " + user.nombreUsuario + "!");
                    mU.setUsuarioActivo(user);
                    return; // Salir del método después de un inicio de sesión exitoso
                }
            }
            System.out.println("Has excedido el número máximo de intentos.");
            System.out.println("¿Deseas recuperar tu contraseña? (s/n): ");
            String opcion = escaner.nextLine();
            while (!opcion.equalsIgnoreCase("s") && !opcion.equalsIgnoreCase("n")) {
                System.out.println("Opción no válida. Por favor, selecciona 's' o 'n'.");
                System.out.print("¿Deseas recuperar tu contraseña? (s/n): ");
                opcion = escaner.nextLine();
                if (opcion.equalsIgnoreCase("s")) {
                    user = mU.recobrarContraseña();
                    if (user != null) {
                            iniciarSesion(); // Volver a iniciar sesión después de actualizar la contraseña
                            return; // Salir del método después de actualizar la contraseña
                        } else {
                            System.out.println("Respuesta incorrecta. No se pudo recuperar la contraseña.");
                        }
                }
                else if (opcion.equalsIgnoreCase("n")) {
                    System.out.println("Regresando al menú principal.");
                }
            }
            System.out.println("Nombre de usuario o contraseña incorrectos. Intenta nuevamente.");
            seleccionarUsuario(); // Volver a la selección de usuario
        }
    }
}