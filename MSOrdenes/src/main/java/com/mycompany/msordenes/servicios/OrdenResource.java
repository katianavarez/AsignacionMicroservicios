package com.mycompany.msordenes.servicios;

import com.mycompany.msordenes.entidades.Orden;
import com.mycompany.msordenes.entidades.Producto;
import com.mycompany.msordenes.mensajeria.PublicadorLogs;
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

    /** Productor del paradigma Routing (publica en el exchange direct "direct_logs"). */
    @Inject
    PublicadorLogs logs;

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
            int recibidos = productIds == null ? 0 : productIds.size();
            // ROUTING KEY "warning": error del cliente, la orden no se crea
            logs.warning("Orden rechazada: se recibieron " + recibidos + " productos (deben ser 3)");
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
                logs.warning("Orden rechazada: no existe el producto " + id);
                return Response.status(Response.Status.NOT_FOUND)
                               .entity("No existe el producto " + id)
                               .build();
            } catch (RuntimeException e) {
                // MSProductos apagado o inaccesible -> ROUTING KEY "error"
                logs.error("MSProductos no responde: no se pudo crear la orden " + productIds);
                return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                               .entity("El servicio de productos no está disponible")
                               .build();
            }
        }

        Orden orden = new Orden();
        orden.productos = productIds;
        orden.total = total;
        orden.persist();

        // ROUTING KEY "info": todo salió bien
        logs.info(String.format("Orden #%d creada con productos %s, total $%.2f", orden.id, productIds, total));

        return Response.status(Response.Status.CREATED).entity(orden).build();
    }
}
