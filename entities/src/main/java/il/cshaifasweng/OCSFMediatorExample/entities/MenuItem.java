package il.cshaifasweng.OCSFMediatorExample.entities;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "MenuItem")
public class MenuItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(nullable = false)
    private String name;

    private String ingredients;

    private String preferences;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private String category;

    @ManyToOne(optional = true)
    @JoinColumn(name = "branch_id", nullable = true)
    private Branch branch;

    //private String catagory;

    public MenuItem(String name, String ingredients, String preferences, double price, String category) {
        this.name = name;
        this.ingredients = ingredients;
        this.preferences = preferences;
        this.price = price;
        this.category = category;
    }
    
    public MenuItem(String name, String ingredients, String preferences, double price, String category, Branch branch) {
        this.name = name;
        this.ingredients = ingredients;
        this.preferences = preferences;
        this.price = price;
        this.category = category;
        this.branch = branch;
    }

    public MenuItem() {
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public String getIngredients() {
        return ingredients;
    }

    public void setIngredients(String ingredients) {
        this.ingredients = ingredients;
    }

    public String getPreferences() {
        return preferences;
    }

    public void setPreferences(String preferences) {
        this.preferences = preferences;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /*public String getCatagory() {
        return catagory;
    }

    public void setCatagory(String catagory) {
        this.catagory = catagory;
    }*/
}