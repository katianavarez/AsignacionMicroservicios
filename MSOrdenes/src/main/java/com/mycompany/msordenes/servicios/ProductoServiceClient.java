package com.mycompany.msordenes.servicios;

import com.mycompany.msordenes.entidades.Producto;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 *
 * @author katia
 */
@RegisterRestClient(configKey = "productos")
@Path("/productos")
public interface ProductoServiceClient {
    @GET
    @Path("/{id}")
    Producto getById(@PathParam("id") Long id);
}
