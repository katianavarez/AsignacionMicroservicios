package com.mycompany.msgateway.servicios;

import com.mycompany.msgateway.clientes.ProductosClient;
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
 * Punto de entrada del gateway para productos:
 * /api/productos/** -> http://localhost:8081/productos/**
 */
@Path("/api/productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoGatewayResource {

    private static final String SERVICIO = "productos";

    @Inject
    @RestClient
    ProductosClient productos;

    @GET
    public Response getAll() {
        return Reenvio.reenviar(SERVICIO, () -> productos.getAll());
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        return Reenvio.reenviar(SERVICIO, () -> productos.getById(id));
    }

    @POST
    public Response create(String producto) {
        return Reenvio.reenviar(SERVICIO, () -> productos.create(producto));
    }
}
