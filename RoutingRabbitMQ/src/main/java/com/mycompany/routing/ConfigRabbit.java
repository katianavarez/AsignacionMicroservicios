package com.mycompany.routing;

import com.rabbitmq.client.ConnectionFactory;

/**
 * Configuración compartida por el emisor y los receptores.
 *
 * El host se puede cambiar sin recompilar:
 *   - variable de entorno RABBITMQ_HOST, o
 *   - propiedad de sistema -Drabbitmq.host=192.168.1.50
 */
public final class ConfigRabbit {

    /** Exchange de tipo DIRECT: entrega cada mensaje a las colas cuyo binding key == routing key. */
    public static final String EXCHANGE = "direct_logs";

    /** Severidades que usa el sistema como routing keys. */
    public static final String[] SEVERIDADES = {"info", "warning", "error"};

    private ConfigRabbit() {
    }

    public static ConnectionFactory crearFabrica() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(valor("rabbitmq.host", "RABBITMQ_HOST", "localhost"));
        factory.setPort(Integer.parseInt(valor("rabbitmq.port", "RABBITMQ_PORT", "5672")));
        factory.setUsername(valor("rabbitmq.user", "RABBITMQ_USER", "guest"));
        factory.setPassword(valor("rabbitmq.password", "RABBITMQ_PASSWORD", "guest"));
        return factory;
    }

    private static String valor(String propiedad, String variableEntorno, String porDefecto) {
        String v = System.getProperty(propiedad);
        if (v == null || v.isBlank()) {
            v = System.getenv(variableEntorno);
        }
        return (v == null || v.isBlank()) ? porDefecto : v;
    }

    // ---------- Colores para la consola (se desactivan con la variable de entorno NO_COLOR) ----------

    private static final boolean COLOR = System.getenv("NO_COLOR") == null;

    public static String color(String severidad, String texto) {
        if (!COLOR) {
            return texto;
        }
        String codigo = switch (severidad) {
            case "info" -> "\u001B[32m";     // verde
            case "warning" -> "\u001B[33m";  // amarillo
            case "error" -> "\u001B[31m";    // rojo
            default -> "\u001B[90m";         // gris
        };
        return codigo + texto + "\u001B[0m";
    }

    public static String negritas(String texto) {
        return COLOR ? "\u001B[1m" + texto + "\u001B[0m" : texto;
    }
}
