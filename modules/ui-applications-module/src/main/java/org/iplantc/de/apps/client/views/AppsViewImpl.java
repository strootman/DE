package org.iplantc.de.apps.client.views;

import org.iplantc.de.apps.client.AppsToolbarView;
import org.iplantc.de.apps.client.AppsView;
import org.iplantc.de.apps.client.events.AppFavoritedEvent;
import org.iplantc.de.apps.client.events.selection.AppCategorySelectionChangedEvent;
import org.iplantc.de.apps.client.events.selection.AppCommentSelectedEvent;
import org.iplantc.de.apps.client.events.selection.AppFavoriteSelectedEvent;
import org.iplantc.de.apps.client.events.selection.AppInfoSelectedEvent;
import org.iplantc.de.apps.client.events.selection.AppNameSelectedEvent;
import org.iplantc.de.apps.client.events.selection.AppRatingDeselected;
import org.iplantc.de.apps.client.events.selection.AppRatingSelected;
import org.iplantc.de.apps.client.events.selection.AppSelectionChangedEvent;
import org.iplantc.de.apps.client.models.AppCategoryStringValueProvider;
import org.iplantc.de.apps.shared.AppsModule.Ids;
import org.iplantc.de.client.models.DEProperties;
import org.iplantc.de.client.models.HasId;
import org.iplantc.de.client.models.UserInfo;
import org.iplantc.de.client.models.apps.App;
import org.iplantc.de.client.models.apps.AppCategory;
import org.iplantc.de.client.services.AppUserServiceFacade;
import org.iplantc.de.client.util.JsonUtil;
import org.iplantc.de.resources.client.IplantResources;
import org.iplantc.de.resources.client.messages.IplantDisplayStrings;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiTemplate;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import static com.sencha.gxt.core.client.Style.SelectionMode.SINGLE;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.data.shared.TreeStore;
import com.sencha.gxt.data.shared.event.StoreRemoveEvent;
import com.sencha.gxt.widget.core.client.Composite;
import com.sencha.gxt.widget.core.client.ContentPanel;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.container.BorderLayoutContainer;
import com.sencha.gxt.widget.core.client.container.BorderLayoutContainer.BorderLayoutData;
import com.sencha.gxt.widget.core.client.grid.ColumnModel;
import com.sencha.gxt.widget.core.client.grid.Grid;
import com.sencha.gxt.widget.core.client.grid.GridView;
import com.sencha.gxt.widget.core.client.selection.SelectionChangedEvent;
import com.sencha.gxt.widget.core.client.selection.SelectionChangedEvent.SelectionChangedHandler;
import com.sencha.gxt.widget.core.client.tips.QuickTip;
import com.sencha.gxt.widget.core.client.tree.Tree;
import com.sencha.gxt.widget.core.client.tree.Tree.TreeAppearance;
import com.sencha.gxt.widget.core.client.tree.Tree.TreeNode;
import com.sencha.gxt.widget.core.client.tree.TreeStyle;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author jstroot
 */
public class AppsViewImpl extends Composite implements AppsView,
                                                       AppInfoSelectedEvent.AppInfoSelectedEventHandler, StoreRemoveEvent.StoreRemoveHandler<App> {
    @UiTemplate("AppsView.ui.xml")
    interface MyUiBinder extends UiBinder<Widget, AppsViewImpl> {
    }
    @UiField protected ColumnModel<App> cm;
    @UiField protected Grid<App> grid;
    protected Presenter presenter;
    @UiField(provided = true) protected Tree<AppCategory, String> tree;
    final DEProperties properties;
    @UiField(provided = true) final AppsToolbarView toolBar;
    @UiField ContentPanel centerPanel;
    @UiField BorderLayoutContainer con;
    @UiField BorderLayoutData eastData;
    @UiField ContentPanel eastPanel;
    @UiField GridView<App> gridView;
    @Inject JsonUtil jsonUtil;
    @UiField(provided = true) ListStore<App> listStore;
    Logger logger = Logger.getLogger("App View");
    @UiField BorderLayoutData northData;
    @UiField(provided = true) TreeStore<AppCategory> treeStore;
    @UiField ContentPanel westPanel;
    private static String WEST_COLLAPSE_BTN_ID = "idCategoryCollapseBtn"; //$NON-NLS-1$
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private final AppUserServiceFacade appUserService;
    private final IplantDisplayStrings displayStrings;
    private final IplantResources resources;
    private final UserInfo userInfo;
    private String FAVORITES;
    private String USER_APPS_GROUP;
    private String WORKSPACE;

    @Inject
    AppsViewImpl(final DEProperties properties,
                 final AppsToolbarView toolBar,
                 final IplantResources resources,
                 final UserInfo userInfo,
                 final IplantDisplayStrings displayStrings,
                 final AppUserServiceFacade appUserService,
                 @Assisted final ListStore<App> listStore,
                 @Assisted final TreeStore<AppCategory> treeStore) {
        this.properties = properties;
        this.toolBar = toolBar;
        this.resources = resources;
        this.userInfo = userInfo;
        this.displayStrings = displayStrings;
        this.appUserService = appUserService;
        this.listStore = listStore;
        this.treeStore = treeStore;
        initWidget(uiBinder.createAndBindUi(this));

        final AppColumnModel acm = (AppColumnModel) cm;
        acm.addAppInfoSelectedEventHandler(this);
        this.listStore.addStoreRemoveHandler(this);

        WORKSPACE = this.properties.getPrivateWorkspace();

        if (this.properties.getPrivateWorkspaceItems() != null) {
            JSONArray items = JSONParser.parseStrict(this.properties.getPrivateWorkspaceItems()).isArray();
            USER_APPS_GROUP = jsonUtil.getRawValueAsString(items.get(0));
            FAVORITES = jsonUtil.getRawValueAsString(items.get(1));
        }

        this.tree.getSelectionModel().setSelectionMode(SINGLE);

        grid.getSelectionModel().addSelectionChangedHandler(new SelectionChangedHandler<App>() {
            @Override
            public void onSelectionChanged(SelectionChangedEvent<App> event) {
                fireEvent(new AppSelectionChangedEvent(event.getSelection()));
            }
        });

        tree.getSelectionModel().addSelectionChangedHandler(new SelectionChangedHandler<AppCategory>() {
            @Override
            public void onSelectionChanged(SelectionChangedEvent<AppCategory> event) {
                fireEvent(new AppCategorySelectionChangedEvent(event.getSelection()));
            }
        });
        setTreeIcons();
        new QuickTip(grid).getToolTipConfig().setTrackMouse(true);
    }

    //<editor-fold desc="Handler Registrations">
    @Override
    public HandlerRegistration addAppCategorySelectedEventHandler(AppCategorySelectionChangedEvent.AppCategorySelectionChangedEventHandler handler) {
        return asWidget().addHandler(handler, AppCategorySelectionChangedEvent.TYPE);
    }

    @Override
    public HandlerRegistration addAppCommentSelectedEventHandlers(AppCommentSelectedEvent.AppCommentSelectedEventHandler handler) {
        final AppColumnModel acm = (AppColumnModel) cm;
        return acm.addAppCommentSelectedEventHandlers(handler);
    }

    @Override
    public HandlerRegistration addAppFavoriteSelectedEventHandlers(AppFavoriteSelectedEvent.AppFavoriteSelectedEventHandler handler) {
        final AppColumnModel acm = (AppColumnModel) cm;
        return acm.addAppFavoriteSelectedEventHandlers(handler);
    }

    @Override
    public HandlerRegistration addAppFavoritedEventHandler(AppFavoritedEvent.AppFavoritedEventHandler eventHandler) {
        return asWidget().addHandler(eventHandler, AppFavoritedEvent.TYPE);
    }

    @Override
    public HandlerRegistration addAppInfoSelectedEventHandler(AppInfoSelectedEvent.AppInfoSelectedEventHandler handler) {
        final AppColumnModel acm = (AppColumnModel) cm;
        return acm.addAppInfoSelectedEventHandler(handler);
    }

    @Override
    public HandlerRegistration addAppNameSelectedEventHandler(AppNameSelectedEvent.AppNameSelectedEventHandler handler) {
        final AppColumnModel acm = (AppColumnModel) cm;
        return acm.addAppNameSelectedEventHandler(handler);
    }

    @Override
    public HandlerRegistration addAppRatingDeselectedHandler(AppRatingDeselected.AppRatingDeselectedHandler handler) {
        final AppColumnModel acm = (AppColumnModel) cm;
        return acm.addAppRatingDeselectedHandler(handler);
    }

    @Override
    public HandlerRegistration addAppRatingSelectedHandler(AppRatingSelected.AppRatingSelectedHandler handler) {
        final AppColumnModel acm = (AppColumnModel) cm;
        return acm.addAppRatingSelectedHandler(handler);
    }

    @Override
    public HandlerRegistration addAppSelectionChangedEventHandler(AppSelectionChangedEvent.AppSelectionChangedEventHandler handler) {
        return asWidget().addHandler(handler, AppSelectionChangedEvent.TYPE);
    }
    //</editor-fold>

    @Override
    public void expandAppCategories() {
        tree.expandAll();
    }

    @Override
    public List<App> getAllSelectedApps() {
        return grid.getSelectionModel().getSelectedItems();
    }

    @Override
    public AppCategory getAppCategoryFromElement(Element el) {
        TreeNode<AppCategory> node = tree.findNode(el);
        if (node != null && tree.getView().isSelectableTarget(node.getModel(), el)) {
            return node.getModel();
        }

        return null;
    }

    @Override
    public App getAppFromElement(Element el) {
        Element row = gridView.findRow(el);
        int index = gridView.findRowIndex(row);
        return listStore.get(index);
    }

    @Override
    public Grid<App> getAppsGrid() {
        return grid;
    }

    @Override
    public App getSelectedApp() {
        return grid.getSelectionModel().getSelectedItem();
    }

    @Override
    public AppCategory getSelectedAppCategory() {
        return tree.getSelectionModel().getSelectedItem();
    }

    @Override
    public AppsToolbarView getToolBar() {
        return toolBar;
    }

    @Override
    public void hideAppMenu() {
        toolBar.hideAppMenu();
    }

    @Override
    public void hideWorkflowMenu() {
        toolBar.hideWorkflowMenu();
    }

    @Override
    public String highlightSearchText(String text) {
        return presenter.highlightSearchText(text);
    }

    @Override
    public void maskCenterPanel(final String loadingMask) {
        centerPanel.mask(loadingMask);
    }

    @Override
    public void maskWestPanel(String loadingMask) {
        westPanel.mask(loadingMask);
    }

    //<editor-fold desc="Event Handlers">
    @Override
    public void onAppFavorited(AppFavoritedEvent event) {
        // Forward event so the App Info window can get it if it is open
        asWidget().fireEvent(event);
    }

    @Override
    public void onAppInfoSelected(AppInfoSelectedEvent event) {
        final App selectedApp = grid.getSelectionModel().getSelectedItem();
        Dialog appInfoWin = new Dialog();
        appInfoWin.setModal(true);
        appInfoWin.setResizable(false);
        appInfoWin.setHeadingText(selectedApp.getName());
        appInfoWin.setPixelSize(450, 300);
        // Get app favorite requests
        final AppInfoView appInfoView = new AppInfoView(selectedApp, this, appUserService);
        appInfoView.addAppFavoriteSelectedEventHandlers(presenter);
        addAppFavoritedEventHandler(appInfoView);
        appInfoWin.add(appInfoView);
        appInfoWin.getButtonBar().clear();
        appInfoWin.show();
    }

    @Override
    public void onRemove(StoreRemoveEvent<App> event) {
        grid.getSelectionModel().deselectAll();
    }
    //</editor-fold>

    //<editor-fold desc="Selection">
    @Override
    public void selectApp(String appId) {
        App app = listStore.findModelWithKey(appId);
        if (app != null) {
            grid.getSelectionModel().select(app, false);
        }
    }

    @Override
    public void selectAppCategory(HasId appCategory) {
        if (appCategory == null) {
            tree.getSelectionModel().deselectAll();
            return;
        }
        AppCategory ag = treeStore.findModelWithKey(appCategory.getId());

        if (ag != null) {
            if (tree.getSelectionModel().isSelected(ag)) {
                /* if category is already selected, then manually fire event since selection
                     * model won't fire selection changed
                     */
                fireEvent(new AppCategorySelectionChangedEvent(Collections.singletonList(ag)));
            } else {
                tree.getSelectionModel().select(ag, false);
                tree.scrollIntoView(ag);
            }
        } else {
            // Try to find app group by name if ID could not locate the
            for (AppCategory appGrp : treeStore.getAll()) {

                if (appGrp.getName().equalsIgnoreCase(appCategory.getId())) {
                    tree.getSelectionModel().select(appGrp, false);
                    tree.scrollIntoView(appGrp);
                    return;
                }
            }
        }

    }

    @Override
    public void selectFirstApp() {
        grid.getSelectionModel().select(0, false);
    }

    @Override
    public void selectFirstAppCategory() {
        AppCategory ag = treeStore.getRootItems().get(0);
        tree.getSelectionModel().select(ag, false);
        tree.scrollIntoView(ag);
    }
    //</editor-fold>

    @Override
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
//        AppColumnModel appColModel = (AppColumnModel)cm;
//        appColModel.addAppInfoSelectedEventHandler(this);
//        appColModel.addAppNameSelectedEventHandler(presenter);
//        appColModel.addAppFavoriteSelectedEventHandlers(this);
//        appColModel.addAppCommentSelectedEventHandlers(presenter);
//        appColModel.addAppRatingDeselectedHandler(presenter);
//        appColModel.addAppRatingSelectedHandler(presenter);
//        addAppCategorySelectedEventHandler(presenter);
        this.toolBar.init(presenter, this);
    }

    @Override
    public void unMaskCenterPanel() {
        centerPanel.unmask();
    }

    @Override
    public void unMaskWestPanel() {
        westPanel.unmask();
    }

    @Override
    public void updateAppListHeading(String headingText) {
        centerPanel.setHeadingText(headingText);
    }

    //<editor-fold desc="UI Factories">
    @UiFactory
    protected ColumnModel<App> createColumnModel() {
        return new AppColumnModel(this, displayStrings);
    }

    @UiFactory
    Tree<AppCategory, String> createTree() {
        return new Tree<>(treeStore, new AppCategoryStringValueProvider());
    }
    //</editor-fold>

    @Override
    protected void onEnsureDebugId(String baseID) {
        super.onEnsureDebugId(baseID);
        toolBar.asWidget().ensureDebugId(baseID + Ids.MENU_BAR);
        tree.ensureDebugId(baseID + Ids.CATEGORIES);
        grid.ensureDebugId(baseID + Ids.APP_GRID);
        ((AppColumnModel) cm).ensureDebugId(baseID);

        westPanel.getHeader().getTool(0).getElement().setId(WEST_COLLAPSE_BTN_ID);
    }

    /**
     * FIXME JDS This needs to be implemented in an {@link TreeAppearance}
     */
    private void setTreeIcons() {
        TreeStyle style = tree.getStyle();
        style.setNodeCloseIcon(resources.category());
        style.setNodeOpenIcon(resources.category_open());
        style.setLeafIcon(resources.subCategory());
    }
}
