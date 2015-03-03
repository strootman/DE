package org.iplantc.de.admin.desktop.client.apps.views.cells;

import org.iplantc.de.apps.client.views.cells.AppHyperlinkCell;
import org.iplantc.de.client.models.apps.App;

import com.google.gwt.cell.client.Cell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;

/**
 * @author jstroot
 */
public class AppNameCell extends AppHyperlinkCell {

    public interface AppNameCellAppearance extends AppHyperlinkCellAppearance {

        String editApp();

        void render(SafeHtmlBuilder sb, App value);
    }

    private AppNameCellAppearance appearance = GWT.create(AppNameCellAppearance.class);

    @Override
    public void render(Cell.Context context, App value, SafeHtmlBuilder sb) {
        if (value == null) {
            return;
        }
        appearance.render(sb, value);
    }
}
