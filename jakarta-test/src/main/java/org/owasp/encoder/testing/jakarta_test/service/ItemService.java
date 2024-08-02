package org.owasp.encoder.testing.jakarta_test.service;

import java.util.Collection;
import org.owasp.encoder.testing.jakarta_test.dto.Item;

/**
 *
 * @author jeremy
 */
public interface ItemService {
    Collection<Item> getItems();

    Item addItem(Item item);
}
