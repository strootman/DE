package org.iplantc.de.admin.desktop.client.toolAdmin.view.subviews;

import org.iplantc.de.admin.desktop.client.toolAdmin.ToolAdminView;
import org.iplantc.de.admin.desktop.client.toolAdmin.model.AppProperties;
import org.iplantc.de.client.models.apps.App;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;

import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.widget.core.client.Composite;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;

import java.util.ArrayList;
import java.util.List;

/**
 * @author aramsey
 */
public class ToolPublicAppListWindow extends Composite {

    interface ToolPublicAppListUiBinder extends UiBinder<Widget, ToolPublicAppListWindow> {

    }

    private static final org.iplantc.de.admin.desktop.client.toolAdmin.model.AppProperties AppProperties = GWT.create(AppProperties.class);
    private static ToolPublicAppListUiBinder uiBinder = GWT.create(ToolPublicAppListUiBinder.class);
    @UiField Grid<App> grid;
    @UiField ListStore<App> listStore;
    @UiField(provided = true)
    ToolAdminView.ToolAdminViewAppearance appearance =
            GWT.create(ToolAdminView.ToolAdminViewAppearance.class);

    @UiFactory
    ListStore<App> createListStore() {
        return new ListStore<>(AppProperties.id());
    }

    @UiFactory
    ColumnModel<App> createColumnModel() {
        ColumnConfig<App, String> name = new ColumnConfig<>(AppProperties.name(),
                                                            appearance.publicAppNameColumnWidth(),
                                                            appearance.publicAppNameLabel());
        ColumnConfig<App, String> integratorName = new ColumnConfig<>(AppProperties.integratorName(),
                                                                      appearance.publicAppIntegratorColumnWidth(),
                                                                      appearance.publicAppIntegratorLabel());
        ColumnConfig<App, String> integratorEmail = new ColumnConfig<>(AppProperties.integratorEmail(),
                                                                       appearance.publicAppIntegratorEmailColumnWidth(),
                                                                       appearance.publicAppIntegratorEmailLabel());
        ColumnConfig<App, Boolean> isDisabled = new ColumnConfig<>(AppProperties.isDisabled(),
                                                                   appearance.publicAppDisabledColumnWidth(),
                                                                   appearance.publicAppDisabledLabel());

        List<ColumnConfig<App, ?>> columns = new ArrayList<>();
        columns.add(name);
        columns.add(integratorName);
        columns.add(integratorEmail);
        columns.add(isDisabled);
        return new ColumnModel<>(columns);
    }

    public ToolPublicAppListWindow() {
        initWidget(uiBinder.createAndBindUi(this));
        grid.setAllowTextSelection(true);
    }

    public void addApps(List<App> apps) {
        listStore.addAll(apps);
    }

}
