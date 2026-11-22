package com.mycompany.msordenes.mensajeria;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * PRODUCTOR del paradigma Routing dentro de MSOrdenes.
 *
 * Cada evento de una orden se publica en el exchange DIRECT "direct_logs" usando su
 * severidad como ROUTING KEY: "info", "warning" o "error".
 * El exchange solo lo entrega a las colas que hicieron binding con esa misma key.
 *
 * Si RabbitMQ no está disponible, las órdenes se siguen creando normalmente
 * (solo se registra una advertencia en la consola).
 *
 * @author Equipo 3
 */
@ApplicationScoped
public class PublicadorLogs {

    public static final String EXCHANGE = "direct_logs";

    private static final Logger LOG = Logger.getLogger(PublicadorLogs.class);

    @ConfigProperty(name = "rabbitmq.host", defaultValue = "localhost")
    String host;

    @ConfigProperty(name = "rabbitmq.port", defaultValue = "5672")
    int port;

    @ConfigProperty(name = "rabbitmq.username", defaultValue = "guest")
    String username;

    @ConfigProperty(name = "rabbitmq.password", defaultValue = "guest")
    String password;

    private Connection connection;
    private Channel channel;

    void alIniciar(@Observes StartupEvent ev) {
        conectar();
    }

    void alDetener(@Observes ShutdownEvent ev) {
        cerrarConexion();
    }

    private void cerrarConexion() {
        try {
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (Exception ignored) {
            // nada que hacer
        }
        connection = null;
        channel = null;
    }

    public void info(String mensaje) {
        publicar("info", mensaje);
    }

    public void warning(String mensaje) {
        publicar("warning", mensaje);
    }

    public void error(String mensaje) {
        publicar("error", mensaje);
    }

    /**
     * Publica un mensaje en el exchange direct.
     *
     * @param severidad routing key del mensaje (info, warning, error)
     * @param mensaje   cuerpo del mensaje
     */
    public synchronized void publicar(String severidad, String mensaje) {
        if (!conectar()) {
            LOG.warnf("RabbitMQ no disponible; no se publicó [%s] %s", severidad, mensaje);
            return;
        }
        try {
            // basicPublish(exchange, ROUTING KEY, propiedades, cuerpo)
            channel.basicPublish(EXCHANGE, severidad, null, mensaje.getBytes(StandardCharsets.UTF_8));
            LOG.infof(" [x] Publicado en '%s' con routing key '%s': %s", EXCHANGE, severidad, mensaje);
        } catch (Exception e) {
            LOG.warnf("No se pudo publicar en RabbitMQ: %s", e.getMessage());
        }
    }

    /** Abre la conexión (o la reutiliza) y declara el exchange. Regresa false si RabbitMQ no responde. */
    private synchronized boolean conectar() {
        if (channel != null && channel.isOpen()) {
            return true;
        }
        cerrarConexion();
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(host);
            factory.setPort(port);
            factory.setUsername(username);
            factory.setPassword(password);
            factory.setConnectionTimeout(2000);
            // la reconexión la hacemos nosotros en el siguiente publicar()
            factory.setAutomaticRecoveryEnabled(false);

            connection = factory.newConnection("MSOrdenes");
            channel = connection.createChannel();

            // EXCHANGE de tipo DIRECT: compara routing key == binding key
            channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.DIRECT);
            LOG.infof("Conectado a RabbitMQ en %s:%d (exchange '%s', tipo direct)", host, port, EXCHANGE);
            return true;
        } catch (Exception e) {
            LOG.warnf("No se pudo conectar a RabbitMQ en %s:%d (%s)", host, port, e.getMessage());
            cerrarConexion();
            return false;
        }
    }
}
