package com.mycompany.msgateway.servicios;

import com.mycompany.msgateway.clientes.OrdenesClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Punto de entrada del gateway para ordenes:
 * /api/ordenes/** -> http://localhost:8082/ordenes/**
 */
@Path("/api/ordenes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrdenGatewayResource {

    private static final String SERVICIO = "ordenes";

    @Inject
    @RestClient
    OrdenesClient ordenes;

    @GET
    public Response getAll() {
        return Reenvio.reenviar(SERVICIO, () -> ordenes.getAll());
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        return Reenvio.reenviar(SERVICIO, () -> ordenes.getById(id));
    }

    @POST
    public Response create(String productIds) {
        return Reenvio.reenviar(SERVICIO, () -> ordenes.create(productIds));
    }
}
