package com.mycompany.msordenes.servicios;

import com.mycompany.msordenes.entidades.Orden;
import com.mycompany.msordenes.entidades.Producto;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 *
 * @author katia
 */
@Path("/ordenes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrdenResource {
    @Inject
    @RestClient
    ProductoServiceClient productClient;

    @GET
    public List<Orden> getAll() {
        return Orden.listAll();
    }

    @GET
    @Path("/{id}")
    public Orden getOrden(@PathParam("id") Long id) {
        Orden orden = Orden.findById(id);
        if (orden == null) {
            throw new NotFoundException();
        }
        return orden;
    }

    @POST
    @Transactional
    public Response createOrder(List<Long> productIds) {
        if (productIds == null || productIds.size() != 3) {
            return Response.status(Response.Status.BAD_REQUEST)
                           .entity("La orden debe tener 3 productos")
                           .build();
        }

        double total = 0;
        for (Long id : productIds) {
            try {
                Producto product = productClient.getById(id);
                total += product.price;
            } catch (WebApplicationException e) {
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("No existe el producto " + id)
                               .build();
            }
        }

        Orden orden = new Orden();
        orden.productos = productIds;
        orden.total = total;
        orden.persist();

        return Response.status(Response.Status.CREATED).entity(orden).build();
    }
}
