package com.mycompany.routing;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * CONSUMIDOR del paradigma Routing.
 *
 * Uso: ReceptorLogsDirect <nombre> <severidad> [<severidad> ...] [--archivo=ruta.log]
 *
 * Ejemplos:
 *   monitor   info warning error      -> ve todo
 *   auditoria warning error --archivo=auditoria.log
 *   alertas   error                   -> solo errores
 */
public class ReceptorLogsDirect {

    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static void main(String[] argv) throws Exception {
        String nombre = null;
        String archivo = null;
        List<String> severidades = new ArrayList<>();
        for (String arg : argv) {
            if (arg.startsWith("--archivo=")) {
                archivo = arg.substring("--archivo=".length());
            } else if (nombre == null) {
                nombre = arg;
            } else {
                severidades.add(arg);
            }
        }
        if (nombre == null || severidades.isEmpty()) {
            System.err.println("Uso: ReceptorLogsDirect <nombre> <severidad> [<severidad> ...] [--archivo=ruta.log]");
            System.err.println("Ej.: ReceptorLogsDirect alertas error");
            System.exit(1);
        }

        ConnectionFactory factory = ConfigRabbit.crearFabrica();
        Connection connection = factory.newConnection("receptor-" + nombre);
        Channel channel = connection.createChannel();

        // 1) EXCHANGE: se declara igual que en el emisor. Es idempotente: si ya existe no pasa nada,
        //    así no importa quién arranque primero.
        channel.exchangeDeclare(ConfigRabbit.EXCHANGE, BuiltinExchangeType.DIRECT);

        // 2) COLA propia de este consumidor.
        //    durable=false, exclusive=true, autoDelete=true -> vive solo mientras este receptor esté conectado.
        String queueName = "cola_" + nombre;
        channel.queueDeclare(queueName, false, true, true, null);

        // 3) BINDINGS: un binding por cada severidad que nos interesa.
        //    El tercer parámetro es la BINDING KEY.
        System.out.println(ConfigRabbit.negritas("Receptor '" + nombre + "' conectado a " + factory.getHost()));
        for (String severidad : severidades) {
            channel.queueBind(queueName, ConfigRabbit.EXCHANGE, severidad);
            System.out.println("  binding: " + ConfigRabbit.EXCHANGE + " --[" + ConfigRabbit.color(severidad, severidad)
                    + "]--> " + queueName);
        }

        PrintWriter log = archivo == null ? null : new PrintWriter(new FileWriter(archivo, StandardCharsets.UTF_8, true), true);
        if (log != null) {
            System.out.println("  también se guarda en: " + archivo);
        }
        System.out.println(" [*] Esperando mensajes. Para salir presiona CTRL+C\n");

        // 4) CONSUMIR: el callback se ejecuta por cada mensaje que llega a NUESTRA cola.
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String mensaje = new String(delivery.getBody(), StandardCharsets.UTF_8);
            String routingKey = delivery.getEnvelope().getRoutingKey();
            String hora = LocalTime.now().format(HORA);

            String etiqueta = String.format("%-9s", "[" + routingKey.toUpperCase() + "]");
            System.out.println(" [" + hora + "] " + ConfigRabbit.color(routingKey, etiqueta) + " " + mensaje);
            if (log != null) {
                log.println(hora + " " + etiqueta + " " + mensaje);
            }
        };
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (connection.isOpen()) {
                    connection.close();
                }
            } catch (IOException ignored) {
                // la cola es auto-delete: se borra sola al desconectarse
            }
            if (log != null) {
                log.close();
            }
        }));
    }
}
