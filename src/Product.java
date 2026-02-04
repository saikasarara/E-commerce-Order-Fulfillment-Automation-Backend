/** Product.java – contributed by Adiba (Product model, part of inventory management) */
public class Product {
    public String productId;
    public String category;
    public String brand;
    public String name;
    public int price;
    public int stock;

    public Product(String productId, String category, String brand, String name, int price, int stock) {
        this.productId = productId;
        this.category = category;
        this.brand = brand;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }
}



