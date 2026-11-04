package org.example.classes;

import java.util.List;
import java.util.Objects;

public class Cart {
    private List<Product> items;

    public Cart(List<Product> items) {
        this.items = items;
    }

    public List<Product> getItems() {
        return items;
    }

    public void setItems(List<Product> items) {
        this.items = items;
    }

    // Конструктор, методы добавления и удаления товаров, геттеры и другие методы


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Cart cart)) return false;
        return Objects.equals(items, cart.items);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(items);
    }

    @Override
    public String toString() {
        return "Cart{" +
            "items=" + items +
            '}';
    }
}