package org.iplantc.de.admin.desktop.client.toolAdmin.view;

import org.iplantc.de.admin.desktop.client.toolAdmin.ToolAdminView;
import org.iplantc.de.admin.desktop.client.toolAdmin.events.AddToolSelectedEvent;
import org.iplantc.de.admin.desktop.client.toolAdmin.events.DeleteToolSelectedEvent;
import org.iplantc.de.admin.desktop.client.toolAdmin.events.SaveToolSelectedEvent;
import org.iplantc.de.admin.desktop.client.toolAdmin.events.ToolSelectedEvent;
import org.iplantc.de.admin.desktop.client.toolAdmin.model.ToolProperties;
import org.iplantc.de.admin.desktop.client.toolAdmin.view.dialogs.ToolAdminDetailsDialog;
import org.iplantc.de.client.models.tool.Tool;
import org.iplantc.de.commons.client.ErrorHandler;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.inject.client.AsyncProvider;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import com.sencha.gxt.core.client.Style;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.data.shared.Store;
import com.sencha.gxt.widget.core.client.Composite;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.event.SelectEvent;
import com.sencha.gxt.widget.core.client.grid.ColumnConfig;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;

import java.util.List;

/**
 * @author aramsey
 */
public class ToolAdminViewImpl extends Composite implements ToolAdminView,
                                                            SelectionHandler<Tool>,
                                                            SaveToolSelectedEvent.SaveToolSelectedEventHandler {


    interface ToolAdminViewImplUiBinder extends UiBinder<Widget, ToolAdminViewImpl> {
    }

    private final class NameFilter implements Store.StoreFilter<Tool> {

        private String filterText;

        @Override
        public boolean select(Store<Tool> store, Tool parent, Tool item) {
            if (Strings.nullToEmpty(filterText).isEmpty()) {
                return false;
            }
            return item.getName().toLowerCase().contains(filterText.toLowerCase());
        }

        public void setQuery(String query) {
            this.filterText = query;
        }
    }

    private static ToolAdminViewImplUiBinder uiBinder = GWT.create(ToolAdminViewImplUiBinder.class);

    @UiField
    TextButton addButton;
    @UiField
    Grid<Tool> grid;
    @UiField(provided = true)
    ListStore<Tool> listStore;
    @UiField(provided = true)
    ToolAdminViewAppearance appearance;

    @Inject
    AsyncProvider<ToolAdminDetailsDialog> toolDetailsDlgProvider;

    private final ToolProperties toolProps;
    private final NameFilter nameFilter;

    @Inject
    ToolAdminViewImpl(final ToolAdminViewAppearance appearance,
                      ToolProperties toolProps,
                      @Assisted ListStore<Tool> listStore) {
        this.appearance = appearance;
        this.toolProps = toolProps;
        this.listStore = listStore;
        initWidget(uiBinder.createAndBindUi(this));
        nameFilter = new NameFilter();
        grid.getSelectionModel().addSelectionHandler(this);
        grid.getSelectionModel().setSelectionMode(Style.SelectionMode.SINGLE);
    }

    //<editor-fold desc="Handler Registrations">
    @Override public HandlerRegistration addAddToolSelectedEventHandler(AddToolSelectedEvent.AddToolSelectedEventHandler handler) {
        return addHandler(handler, AddToolSelectedEvent.TYPE);
    }

    @Override public HandlerRegistration addDeleteToolSelectedEventHandler(DeleteToolSelectedEvent.DeleteToolSelectedEventHandler handler) {
        return addHandler(handler, DeleteToolSelectedEvent.TYPE);
    }

    @Override public HandlerRegistration addSaveToolSelectedEventHandler(SaveToolSelectedEvent.SaveToolSelectedEventHandler handler) {
        return addHandler(handler, SaveToolSelectedEvent.TYPE);
    }

    @Override public HandlerRegistration addToolSelectedEventHandler(ToolSelectedEvent.ToolSelectedEventHandler handler) {
        return addHandler(handler, ToolSelectedEvent.TYPE);
    }
    //</editor-fold>

    //<editor-fold desc="Event Handlers">
    @Override public void onSaveToolSelected(SaveToolSelectedEvent event) {
        grid.getSelectionModel().deselect(grid.getSelectionModel().getSelectedItem());
    }
    //</editor-fold>

    @UiFactory
    ColumnModel<Tool> createColumnModel() {
        List<ColumnConfig<Tool, ?>> list = Lists.newArrayList();
        ColumnConfig<Tool, String> nameCol = new ColumnConfig<>(toolProps.name(),
                                                                appearance.nameColumnWidth(),
                                                                appearance.nameColumnLabel());
        ColumnConfig<Tool, String> descriptionCol = new ColumnConfig<>(toolProps.description(),
                                                                       appearance.descriptionColumnWidth(),
                                                                       appearance.descriptionColumnLabel());
        ColumnConfig<Tool, String> locationCol = new ColumnConfig<>(toolProps.location(),
                                                                    appearance.locationColumnInfoWidth(),
                                                                    appearance.locationColumnInfoLabel());
        ColumnConfig<Tool, String> typeCol = new ColumnConfig<>(toolProps.type(),
                                                                appearance.typeColumnInfoWidth(),
                                                                appearance.typeColumnInfoLabel());
        ColumnConfig<Tool, String> attributionCol = new ColumnConfig<>(toolProps.attribution(),
                                                                       appearance.attributionColumnWidth(),
                                                                       appearance.attributionColumnLabel());
        ColumnConfig<Tool, String> versionCol = new ColumnConfig<>(toolProps.version(),
                                                                   appearance.versionColumnInfoWidth(),
                                                                   appearance.versionColumnInfoLabel());

        list.add(nameCol);
        list.add(descriptionCol);
        list.add(locationCol);
        list.add(typeCol);
        list.add(attributionCol);
        list.add(versionCol);
        return new ColumnModel<>(list);
    }

    @Override
    public void editToolDetails(final Tool tool) {
        toolDetailsDlgProvider.get(new AsyncCallback<ToolAdminDetailsDialog>() {

            @Override public void onFailure(Throwable caught) {
                ErrorHandler.post(caught);
            }

            @Override public void onSuccess(final ToolAdminDetailsDialog result) {
                result.show(tool);
                result.addSaveToolSelectedEventHandler(new SaveToolSelectedEvent.SaveToolSelectedEventHandler() {
                    @Override public void onSaveToolSelected(SaveToolSelectedEvent event) {
                        fireEvent(event);
                        result.hide();
                        grid.getSelectionModel().deselect(grid.getSelectionModel().getSelectedItem());
                    }
                });
            }
        });
    }

    @UiHandler("addButton")
    void addButtonClicked(SelectEvent event) {
        toolDetailsDlgProvider.get(new AsyncCallback<ToolAdminDetailsDialog>() {

            @Override public void onFailure(Throwable caught) {
                ErrorHandler.post(caught);
            }

            @Override public void onSuccess(final ToolAdminDetailsDialog result) {
                result.show();
                result.addSaveToolSelectedEventHandler(new SaveToolSelectedEvent.SaveToolSelectedEventHandler() {
                    @Override public void onSaveToolSelected(SaveToolSelectedEvent event) {
                        AddToolSelectedEvent toolSelectedEvent = new AddToolSelectedEvent(event.getTool());
                        fireEvent(toolSelectedEvent);
                        result.hide();
                    }
                });
            }
        });
    }

    @Override
    public void onSelection(SelectionEvent event) {
        ToolSelectedEvent toolSelectedEvent = new ToolSelectedEvent(grid.getSelectionModel().getSelectedItem());
        fireEvent(toolSelectedEvent);
    }

    @UiHandler("filterField")
    void onFilterValueChanged(ValueChangeEvent<String> event) {
        listStore.removeFilters();
        final String query = Strings.nullToEmpty(event.getValue());
        if (query.isEmpty()) {
            return;
        }
        nameFilter.setQuery(query);
        listStore.addFilter(nameFilter);
    }

}
