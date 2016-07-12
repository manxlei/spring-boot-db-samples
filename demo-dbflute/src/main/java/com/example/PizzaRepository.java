package com.example;

import java.util.List;
import java.util.stream.Collectors;

import org.dbflute.cbean.result.ListResultBean;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.dbflute.exbhv.PizzaBhv;
import com.example.dbflute.exbhv.PizzaToppingsBhv;
import com.example.dbflute.exentity.Pizza;
import com.example.dbflute.exentity.PizzaToppings;

@Repository
public class PizzaRepository {
    private final PizzaBhv pizzaBhv;
    private final PizzaToppingsBhv pizzaToppingsBhv;

    public PizzaRepository(PizzaBhv pizzaBhv, PizzaToppingsBhv pizzaToppingsBhv) {
        this.pizzaBhv = pizzaBhv;
        this.pizzaToppingsBhv = pizzaToppingsBhv;
    }

    public List<com.example.model.Pizza> findOrderByIdAsc() {
        ListResultBean<Pizza> pizzaList = pizzaBhv.selectList(cb -> {
            cb.setupSelect_Base();
            cb.query().addOrderBy_Id_Asc();
        });
        pizzaBhv.loadPizzaToppings(pizzaList, toppingsCB -> {
            toppingsCB.setupSelect_Topping();
        });
        return pizzaList.mappingList(pizza -> { // mapping entity to model
            com.example.model.Pizza modelPizza = new com.example.model.Pizza();
            modelPizza.setId(pizza.getId());
            modelPizza.setName(pizza.getName());
            modelPizza.setPrice(pizza.getPrice());
            pizza.getBase().alwaysPresent(base -> { // BASE_ID is not-null
                com.example.model.Base modelBase = new com.example.model.Base(base.getId());
                modelBase.setName(base.getName());
                modelPizza.setBase(modelBase);
            });
            List<com.example.model.Topping> modelToppingList = pizza.getPizzaToppingsList().stream().map(toppings -> {
                com.example.model.Topping modelTopping = new com.example.model.Topping(toppings.getToppingsId());
                modelTopping.setName(toppings.getTopping().get().getName()); // TOPPINGS_ID is not-null
                return modelTopping;
            }).collect(Collectors.toList());
            modelPizza.setToppings(modelToppingList);
            return modelPizza;
        });
    }

    @Transactional
    public com.example.model.Pizza save(com.example.model.Pizza modelPizza) {
        Pizza pizza = new Pizza();
        pizza.setName(modelPizza.getName());
        pizza.setPrice(modelPizza.getPrice());
        pizza.setBaseId(modelPizza.getBase().getId());
        pizzaBhv.insert(pizza);
        Long pizzaId = pizza.getId(); // generated by insert
        List<PizzaToppings> toppingsList = modelPizza.getToppings().stream().map(modelTopping -> {
            PizzaToppings toppings = new PizzaToppings();
            toppings.setPizzaId(pizzaId);
            toppings.setToppingsId(modelTopping.getId());
            return toppings;
        }).collect(Collectors.toList());
        pizzaToppingsBhv.batchInsert(toppingsList);
        return modelPizza;
    }
}