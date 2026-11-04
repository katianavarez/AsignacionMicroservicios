package com.mycompany.msproductos.servicios;

import com.mycompany.msproductos.entidades.Producto;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

/**
 *
 * @author katia
 */
@Path("/productos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductoResource {

    @GET
    public List<Producto> getAll() {
        return Producto.listAll();
    }

    @GET
    @Path("/{id}")
    public Producto getProduct(@PathParam("id") Long id) {
        Producto producto = Producto.findById(id);
        if (producto == null) {
            throw new NotFoundException();
        }
        return producto;
    }

    @POST
    @Transactional
    public Response create(Producto producto) {
        producto.persist();
        return Response.status(Response.Status.CREATED).entity(producto).build();
    }
}
