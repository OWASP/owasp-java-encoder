package org.owasp.encoder.testing.jakarta_test.controller;

import org.owasp.encoder.testing.jakarta_test.service.ItemService;
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

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/viewItems")
    public String viewItems(Model model) {
        model.addAttribute("items", itemService.getItems());
        return "view-items";
    }
}
