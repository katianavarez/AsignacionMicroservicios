package com.mycompany.msgateway.clientes;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Cliente REST hacia el microservicio de ordenes (puerto 8082).
 * Devuelve la respuesta completa (codigo + cuerpo) para que el gateway
 * la reenvie sin modificarla.
 */
@RegisterRestClient(configKey = "ordenes")
@Path("/ordenes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface OrdenesClient {

    @GET
    Response getAll();

    @GET
    @Path("/{id}")
    Response getById(@PathParam("id") Long id);

    @POST
    Response create(String productIds);
}
