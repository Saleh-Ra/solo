package il.cshaifasweng.OCSFMediatorExample.client;

import il.cshaifasweng.OCSFMediatorExample.entities.MenuItem;
import java.util.List;

public interface MenuUpdateListener {
    void onMenuUpdate(List<MenuItem> menuItems);
}
