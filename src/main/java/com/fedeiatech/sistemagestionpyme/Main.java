package com.fedeiatech.sistemagestionpyme; 

import com.fedeiatech.sistemagestionpyme.dao.ItemDAO;
import com.fedeiatech.sistemagestionpyme.model.ItemVenta;
import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        try {
            ItemDAO dao = new ItemDAO();
            
            // 1. Crear producto (Fíjate en los .0 para los precios y stock)
            System.out.println("Intentando guardar producto...");
            ItemVenta producto = new ItemVenta(
                0,                  // id (autoincremental, ponemos 0)
                "P001",             // codigo
                "Coca Cola",        // nombre
                "Bebida 500ml",     // descripcion
                100.0,              // precioCosto
                150.0,              // precioVenta
                50.0,               // stock
                false               // esServicio
            );
            dao.guardar(producto);
            
            // 2. Crear servicio
            System.out.println("Intentando guardar servicio...");
            ItemVenta servicio = new ItemVenta(
                0, 
                "S001", 
                "Mano de Obra", 
                "Reparación PC", 
                0.0, 
                2000.0, 
                -1.0, 
                true
            );
            dao.guardar(servicio);
            
            System.out.println("--- Datos guardados exitosamente ---");
            
            // 3. Listar
            System.out.println("Listando base de datos:");
            for (ItemVenta item : dao.listarTodos()) {
                System.out.println("ID: " + item.getId() + " | " + item.getNombre() + " | Precio: " + item.getPrecioVenta());
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("Error en la base de datos: " + e.getMessage());
        }
    }
}