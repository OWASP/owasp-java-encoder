package org.owasp.encoder.testing.jakarta_test.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import org.owasp.encoder.testing.jakarta_test.dto.Item;
import org.owasp.encoder.testing.jakarta_test.service.ItemService;
import org.springframework.stereotype.Service;

/**
 *
 * @author jeremy
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Override
    public Collection<Item> getItems() {
        Collection<Item> items = new ArrayList<>();
        items.add(new Item(1, "menu", "blob"));
        items.add(new Item(2, "top<script>alert(1)</script>", "fancy <script>alert(1)</script>"));
        return items;
    }

    @Override
    public Item addItem(Item item) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
