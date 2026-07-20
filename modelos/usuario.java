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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Clase que administra la información del usuario activo.
 */
public class usuario {
    public usuario() {
        // Constructor
    }

    public class User {
        public String nombreUsuario;
        public String contraseña; // base64
        public String preguntaHash; // base64
        public String respuestaHash; // base64
        public String salt;         // base64
        public List<String> team = new ArrayList<>(); // nombres
        public Set<String> owned = new HashSet<>();   // nombres
        public Map<String,Integer> socialLinks = new HashMap<>();
        public String lastModified;
        public User() {}
}

public class PasswordUtil {
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

    public synchronized void saveUser(User u) throws IOException {
        u.lastModified = Instant.now().toString();
        Path out = usersDir.resolve(u.nombreUsuario + ".json");
        M.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), u);
    }

    public User getUser(String username) { return users.get(username.toLowerCase()); }
}
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