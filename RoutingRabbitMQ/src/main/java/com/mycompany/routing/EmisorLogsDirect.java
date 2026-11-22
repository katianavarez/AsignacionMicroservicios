package com.mycompany.routing;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * PRODUCTOR manual del paradigma Routing (para el demo en vivo).
 *
 * Modo interactivo (sin argumentos): escribe "severidad mensaje", p. ej.
 *     error Se cayó la base de datos
 * Modo de un solo envío:
 *     EmisorLogsDirect error Se cayó la base de datos
 */
public class EmisorLogsDirect {

    /** Se pone en true cuando el broker nos devuelve un mensaje que no coincidió con ningún binding. */
    private static final AtomicBoolean DESCARTADO = new AtomicBoolean(false);

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = ConfigRabbit.crearFabrica();
        try (Connection connection = factory.newConnection("emisor-manual");
             Channel channel = connection.createChannel()) {

            // EXCHANGE de tipo DIRECT (mismo nombre que usan MSOrdenes y los receptores)
            channel.exchangeDeclare(ConfigRabbit.EXCHANGE, BuiltinExchangeType.DIRECT);

            // Solo para el demo: con mandatory=true el broker nos "regresa" los mensajes
            // que no coinciden con ningún binding; en el tutorial simplemente se pierden.
            channel.addReturnListener(r -> DESCARTADO.set(true));
            channel.confirmSelect();

            if (argv.length > 0) {
                String severidad = argv[0];
                String mensaje = argv.length > 1
                        ? String.join(" ", Arrays.copyOfRange(argv, 1, argv.length))
                        : "Mensaje de prueba";
                publicar(channel, severidad, mensaje);
                return;
            }

            System.out.println(ConfigRabbit.negritas("Emisor conectado a " + factory.getHost()
                    + " - exchange '" + ConfigRabbit.EXCHANGE + "' (direct)"));
            System.out.println("Escribe: <routing key> <mensaje>    (salir = terminar)");
            System.out.println("  ej.  info Orden #10 creada");
            System.out.println("       warning Orden rechazada: faltan productos");
            System.out.println("       error MSProductos no responde");
            System.out.println("       debug nadie escucha esta key\n");

            Scanner in = new Scanner(System.in, StandardCharsets.UTF_8);
            while (true) {
                System.out.print("> ");
                if (!in.hasNextLine()) {
                    break;
                }
                String linea = in.nextLine().trim();
                if (linea.isEmpty()) {
                    continue;
                }
                if (linea.equalsIgnoreCase("salir")) {
                    break;
                }
                String[] partes = linea.split("\\s+", 2);
                String mensaje = partes.length > 1 ? partes[1] : "Mensaje de prueba";
                publicar(channel, partes[0], mensaje);
            }
        }
    }

    private static void publicar(Channel channel, String severidad, String mensaje) throws Exception {
        DESCARTADO.set(false);

        // ROUTING KEY = severidad. El exchange direct la compara contra las binding keys.
        channel.basicPublish(ConfigRabbit.EXCHANGE, severidad, true, null,
                mensaje.getBytes(StandardCharsets.UTF_8));
        channel.waitForConfirmsOrDie(5000);

        if (DESCARTADO.get()) {
            System.out.println(ConfigRabbit.color("otro", " [!] Ninguna cola tiene binding '" + severidad
                    + "' -> el exchange descartó el mensaje"));
        } else {
            System.out.println(" [x] Enviado " + ConfigRabbit.color(severidad, "'" + severidad + "'") + ": '" + mensaje + "'");
        }
    }
}
