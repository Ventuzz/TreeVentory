package com.inventario.sucursales.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

// # Entidad JPA que representa una sucursal física en el territorio nacional
@Entity
@Table(name = "branches")
public class Branch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El codigo de la sucursal es obligatorio")
    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @NotBlank(message = "El nombre de la sucursal es obligatorio")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String state;

    @Column(length = 200)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    public Branch() {
    }

    public Branch(Long id, String code, String name, String city, String state, String address, String phone, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.city = city;
        this.state = state;
        this.address = address;
        this.phone = phone;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
