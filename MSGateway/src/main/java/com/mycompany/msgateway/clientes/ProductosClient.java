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
 * Cliente REST hacia el microservicio de productos (puerto 8081).
 * Devuelve la respuesta completa (codigo + cuerpo) para que el gateway
 * la reenvie sin modificarla.
 */
@RegisterRestClient(configKey = "productos")
@Path("/productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ProductosClient {

    @GET
    Response getAll();

    @GET
    @Path("/{id}")
    Response getById(@PathParam("id") Long id);

    @POST
    Response create(String producto);
}
