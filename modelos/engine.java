package modelos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class engine {
        /* ---------------------------
    MODELOS JSON / DOM
    --------------------------- */
    public static class Persona {
        public String nombre;
        public String arcano;
        public int nivel;
        public List<FusionEntry> posiblesFusiones;
        public List<GeneratedByEntry> generadoPor;
        public List<specialEntry> fusionEspecial;
        public Map<String,Object> requisitoFusion;
        public Map<String,Object> estadisticas;

        public Persona() {}
        @Override public String toString() { return nombre + " [" + arcano + " Lv:" + nivel + "]"; }
    }

    public static class FusionEntry {
        public List<String> con;   // Pareja de fusión
        public String resultado; // Persona obtenida
    }

    public static class GeneratedByEntry {
        public List<String> de; // Lista de personas
    }

    public static class specialEntry {
        public List<String> de; // Lista de personas
    }

    public static class requiresEntry {
        public String socialLink; 
    }

    public static class NPC {
        public String nombre;
        public int nivelActual; // 0..10
        public String desbloquea; // Persona desbloqueada al nivel 10
        public NPC() {}
    }

    /* ---------------------------
    REGISTRY: Lista enlazada + índice auxiliar
    --------------------------- */
    public static class Registro {
        public static class Node {
            Persona dato;
            Node next;
            Node(Persona p) { dato = p; next = null; }
        }
        private Node cabeza;
        private int tamaño;
        private final Map<String, Node> indicePorNombre = new HashMap<>(); // índice auxiliar name -> node

        public Registro() { cabeza = null; tamaño = 0; }

        // Construye la lista enlazada desde colección (solo al inicio)
        public void buildFrom(Collection<Persona> personas) {
            Node cola = null;
            for (Persona p : personas) {
                Node n = new Node(p);
                if (cabeza == null) { cabeza = n; cola = n; }
                else { cola.next = n; cola = n; }
                indicePorNombre.put(p.nombre.toLowerCase(), n);
                tamaño++;
            }
        }

        public Persona buscarPorNombre(String name) {
            Node n = indicePorNombre.get(name.toLowerCase());
            return n == null ? null : n.dato;
        }

        public Node nodoPorNombre(String name) { return indicePorNombre.get(name.toLowerCase()); }

        public List<Persona> toList() {
            List<Persona> out = new ArrayList<>();
            Node cur = cabeza;
            while (cur != null) { out.add(cur.dato); cur = cur.next; }
            return out;
        }

        public int size() { return tamaño; }

        // Imprime el registro completo (orden de carga)
        public void mostrarRegistro() {
            Node cur = cabeza;
            while (cur != null) {
                System.out.println(cur.dato);
                cur = cur.next;
            }
        }
    }

    /* ---------------------------
    indicePorNivel: TreeMap<Integer, List<Persona>>
    --------------------------- */
    public static class indicePorNivel {
        private final TreeMap<Integer, List<Persona>> indice = new TreeMap<>();

        public void buildFrom(Collection<Persona> personas) {
            indice.clear();
            for (Persona p : personas) {
                indice.computeIfAbsent(p.nivel, k -> new ArrayList<>()).add(p);
            }
        }

        // Consulta por rango inclusive
        public List<Persona> busquedaPorRango(int min, int max) {
            List<Persona> out = new ArrayList<>();
            NavigableMap<Integer, List<Persona>> sub = indice.subMap(min, true, max, true);
            for (List<Persona> list : sub.values()) out.addAll(list);
            return out;
        }

        // Obtener top N por nivel descendente
        public List<Persona> topN(int n) {
            List<Persona> out = new ArrayList<>();
            for (Integer lvl : indice.descendingKeySet()) {
                for (Persona p : indice.get(lvl)) {
                    out.add(p);
                    if (out.size() >= n) return out;
                }
            }
            return out;
        }
    }

    /* ---------------------------
    SocialGraph: NPCs y desbloqueos
    --------------------------- */
    public static class grafoSocialLinks {
        private final Map<String, NPC> npcs = new HashMap<>();

        public void contruirDe(List<NPC> npcList) {
            npcs.clear();
            for (NPC n : npcList) npcs.put(n.nombre, n);
        }

        public NPC getNPC(String name) { return npcs.get(name); }

        // Incrementa nivel; si llega a 10 devuelve el desbloqueo, si no devuelve lista vacía
        public List<String> aumentarRango(String nombreNPC, int delta) {
            NPC npc = npcs.get(nombreNPC);
            if (npc == null) return Collections.emptyList();
            npc.nivelActual = Math.min(10, npc.nivelActual + delta);
            if (npc.nivelActual >= 10) return new ArrayList<>(Arrays.asList(npc.desbloquea));
            return Collections.emptyList();
        }

        public Collection<NPC> allNPCs() { return npcs.values(); }
    }

    /* ---------------------------
    FusionIndex: HashMaps en memoria
    --------------------------- */
    public static class indiceFusiones {
        private final Map<String, String> indicePar = new HashMap<>();

        private String keyNames(List<String> names) {
            List<String> s = new ArrayList<>(names);
            s.sort(String.CASE_INSENSITIVE_ORDER);
            return String.join("|", s);
        }

        public void construirDe(Collection<Persona> personas) {
        indicePar.clear();
            for (Persona p : personas) {
                if (p.posiblesFusiones == null) continue;
                for (FusionEntry fe : p.posiblesFusiones) {
                    List<String> nombres = new ArrayList<>();
                    nombres.add(p.nombre);
                    nombres.addAll(fe.con);
                    String k = keyNames(nombres);
                    indicePar.put(k, fe.resultado);
                }
            }
        }

        // Obtener resultados entre dos personas
        public List<String> resultadoFusion(engine.Persona a, engine.Persona b) {
            List<String> res = new ArrayList<>();
            String pairKey = keyNames(Arrays.asList(a.nombre, b.nombre));
            if (indicePar.containsKey(pairKey)) res.add(indicePar.get(pairKey));
            return res;
        }
    }

    /* ---------------------------
    TEAM: arreglo fijo con liberar persona
    --------------------------- */
    public static class Equipo {
        private final Persona[] miembros;
        private final int capacidad;

        public Equipo(int capacidad) {
            this.capacidad = capacidad;
            this.miembros = new Persona[capacidad];
        }

        // Añadir persona por referencia (si hay espacio)
        public boolean agregarPersona(Persona p) {
            for (int i = 0; i < capacidad; i++) {
                if (miembros[i] == null) { miembros[i] = p; return true; }
            }
            return false;
        }

        // Liberar (deshacerse) de una persona por nombre
        public boolean liberarPersona(String name) {
            for (int i = 0; i < capacidad; i++) {
                if (miembros[i] != null && miembros[i].nombre.equalsIgnoreCase(name)) {
                    miembros[i] = null;
                    return true;
                }
            }
            return false;
        }

        // Remover por referencia (usado en las fusiones)
        public boolean removerPersona(Persona p) {
            for (int i = 0; i < capacidad; i++) {
                if (miembros[i] == p) { miembros[i] = null; return true; }
            }
            return false;
        }

        public Persona[] getMiembros() { return miembros; }

        public int espaciosOcupados() {
            int c = 0;
            for (Persona p : miembros) if (p != null) c++;
            return c;
        }

        public boolean tieneEspacio() { return espaciosOcupados() < capacidad; }

        // Imprimir equipo
        public void mostrarEquipo() {
            System.out.println("Equipo (cap " + capacidad + "):");
            for (int i = 0; i < capacidad; i++) {
                System.out.println(" [" + i + "] " + (miembros[i] == null ? "<vacío>" : miembros[i]));
            }
        }
    }
}
