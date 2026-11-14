package com.mycompany.msgateway.servicios;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import java.util.function.Supplier;

/**
 * Reenvia la peticion al microservicio y copia su respuesta (codigo, tipo y cuerpo)
 * para devolverla al cliente. Si el microservicio no responde, regresa 503.
 */
final class Reenvio {

    private Reenvio() {
    }

    static Response reenviar(String servicio, Supplier<Response> llamada) {
        try (Response respuesta = llamada.get()) {
            return Response.status(respuesta.getStatus())
                           .type(respuesta.getMediaType())
                           .entity(respuesta.hasEntity() ? respuesta.readEntity(String.class) : null)
                           .build();
        } catch (ProcessingException e) {
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                           .entity("El microservicio de " + servicio + " no esta disponible")
                           .build();
        }
    }
}
