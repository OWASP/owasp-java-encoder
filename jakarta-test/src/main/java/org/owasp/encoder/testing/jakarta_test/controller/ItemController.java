package org.owasp.encoder.testing.jakarta_test.controller;

import java.util.List;
import org.owasp.encoder.testing.jakarta_test.dto.Item;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 *
 * @author jeremy
 */
@Controller
@RequestMapping("/item")
public class ItemController {

    private static final List<Item> ITEMS = List.of(
        new Item(1, "menu", "blob"),
        new Item(2, "top<script>alert(1)</script>", "fancy <script>alert(1)</script>"));

    @GetMapping("/viewItems")
    public String viewItems(Model model) {
        model.addAttribute("items", ITEMS);
        return "view-items";
    }
}
