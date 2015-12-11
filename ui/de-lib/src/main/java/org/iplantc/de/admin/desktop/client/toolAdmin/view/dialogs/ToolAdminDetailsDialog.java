package org.iplantc.de.admin.desktop.client.toolAdmin.view.dialogs;

import org.iplantc.de.admin.desktop.client.toolAdmin.ToolAdminView;
import org.iplantc.de.admin.desktop.client.toolAdmin.events.DeleteToolSelectedEvent;
import org.iplantc.de.admin.desktop.client.toolAdmin.events.SaveToolSelectedEvent;
import org.iplantc.de.admin.desktop.client.toolAdmin.view.ToolAdminDetailsView;
import org.iplantc.de.client.models.IsHideable;
import org.iplantc.de.client.models.tool.Tool;
import org.iplantc.de.client.models.tool.ToolAutoBeanFactory;
import org.iplantc.de.client.models.tool.ToolContainer;
import org.iplantc.de.client.models.tool.ToolDevice;
import org.iplantc.de.client.models.tool.ToolVolume;
import org.iplantc.de.client.models.tool.ToolVolumesFrom;
import org.iplantc.de.commons.client.views.dialogs.IPlantDialog;

import com.google.common.collect.Lists;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.inject.Inject;

import com.sencha.gxt.core.client.dom.ScrollSupport;
import com.sencha.gxt.widget.core.client.Dialog;
import com.sencha.gxt.widget.core.client.box.AlertMessageBox;
import com.sencha.gxt.widget.core.client.box.ConfirmMessageBox;
import com.sencha.gxt.widget.core.client.button.TextButton;
import com.sencha.gxt.widget.core.client.container.FlowLayoutContainer;
import com.sencha.gxt.widget.core.client.event.DialogHideEvent;
import com.sencha.gxt.widget.core.client.event.SelectEvent;

/**
 * Created by jstroot on 12/10/15.
 */
public class ToolAdminDetailsDialog extends IPlantDialog implements IsHideable,
                                                                    SaveToolSelectedEvent.HasSaveToolSelectedEventHandlers,
                                                                    DeleteToolSelectedEvent.HasDeleteToolSelectedEventHandlers {


    private static class CancelSelectHandler implements SelectEvent.SelectHandler {
        private final IsHideable isHideable;

        public CancelSelectHandler(final IsHideable isHideable) {
            this.isHideable = isHideable;
        }

        @Override public void onSelect(SelectEvent event) {
            isHideable.hide();
        }
    }

    private static class DeleteSelectHandler implements SelectEvent.SelectHandler {
        private final IsHideable isHideable;
        private final HasHandlers hasHandlers;
        private final ToolAdminDetailsView view;

        public DeleteSelectHandler(final IsHideable isHideable,
                                   final HasHandlers hasHandlers,
                                   final ToolAdminDetailsView view) {
            this.isHideable = isHideable;
            this.hasHandlers = hasHandlers;
            this.view = view;
        }

        @Override public void onSelect(SelectEvent event) {
            final Tool tool = view.getTool();
            final ConfirmMessageBox deleteMsgBox =
                new ConfirmMessageBox("Confirm", "Delete " + tool.getName() + "?");
            deleteMsgBox.addDialogHideHandler(new DialogHideEvent.DialogHideHandler() {
                @Override
                public void onDialogHide(DialogHideEvent event) {
                    if (event.getHideButton().equals(Dialog.PredefinedButton.OK)
                            || event.getHideButton().equals(Dialog.PredefinedButton.YES)) {
                        DeleteToolSelectedEvent deleteToolSelectedEvent = new DeleteToolSelectedEvent(tool.getId());
                        hasHandlers.fireEvent(deleteToolSelectedEvent);
                        deleteMsgBox.hide();
                        isHideable.hide();
                    }
                }
            });
            deleteMsgBox.show();
        }
    }

    private static class OkSelectHandler implements SelectEvent.SelectHandler {
        private final HasHandlers hasHandlers;
        private final IsHideable hideable;
        private final ToolAdminView.ToolAdminViewAppearance appearance;
        private final ToolAdminDetailsView view;

        public OkSelectHandler(final HasHandlers hasHandlers,
                               final IsHideable hideable,
                               final ToolAdminView.ToolAdminViewAppearance appearance,
                               final ToolAdminDetailsView view) {
            this.hasHandlers = hasHandlers;
            this.hideable = hideable;
            this.appearance = appearance;
            this.view = view;
        }

        @Override public void onSelect(SelectEvent event) {
            if(view.isValid()){
                SaveToolSelectedEvent saveToolSelectedEvent = new SaveToolSelectedEvent(view.getTool());
                hasHandlers.fireEvent(saveToolSelectedEvent);
                hideable.hide();
            } else {
                AlertMessageBox alertMsgBox = new AlertMessageBox("Warning", appearance.completeRequiredFieldsError());
                alertMsgBox.show();
            }
        }
    }

    private final ToolAdminDetailsView view;
    private final ToolAdminView.ToolAdminViewAppearance appearance;
    @Inject ToolAutoBeanFactory factory;

    @Inject
    ToolAdminDetailsDialog(final ToolAdminDetailsView view,
                           final ToolAdminView.ToolAdminViewAppearance appearance) {
        this.view = view;
        this.appearance = appearance;

        setHideOnButtonClick(false);
        setHeadingText(appearance.dialogWindowName());
        setResizable(true);
        setPixelSize(1000, 500);

        setPredefinedButtons(PredefinedButton.OK, PredefinedButton.CANCEL, PredefinedButton.CLOSE);
        getOkButton().setText(appearance.dialogWindowUpdateBtnText());
        getDeleteButton().setText(appearance.dialogWindowDeleteBtnText());

        addOkButtonSelectHandler(new OkSelectHandler(this, this, appearance, view));
        addCancelButtonSelectHandler(new CancelSelectHandler(this));
        getButton(PredefinedButton.CLOSE).addSelectHandler(new DeleteSelectHandler(this, this, view));

        FlowLayoutContainer flc = new FlowLayoutContainer();
        flc.getScrollSupport().setScrollMode(ScrollSupport.ScrollMode.AUTO);
        flc.add(view);
        add(flc);
    }

    //<editor-fold desc="Handler Registrations">
    @Override public HandlerRegistration addDeleteToolSelectedEventHandler(DeleteToolSelectedEvent.DeleteToolSelectedEventHandler handler) {
        return addHandler(handler, DeleteToolSelectedEvent.TYPE);
    }

    @Override public HandlerRegistration addSaveToolSelectedEventHandler(SaveToolSelectedEvent.SaveToolSelectedEventHandler handler) {
        return addHandler(handler, SaveToolSelectedEvent.TYPE);
    }
    //</editor-fold>

    /**
     * To edit existing tool.
     * @param tool the tool to be displayed/edited.
     */
    public void show(final Tool tool){
        view.edit(tool);
        super.show();
    }

    TextButton getDeleteButton() {
        return getButton(PredefinedButton.CLOSE);
    }

    /**
     * To create new tool
     */
    @Override public void show() {
        // Hide the delete button.
        final TextButton deleteButton = getDeleteButton();
        deleteButton.disable();
        deleteButton.setVisible(false);

        final ToolContainer toolContainer = factory.getContainer().as();
        toolContainer.setMemoryLimit(appearance.containerMemoryLimitDefaultValue());
        toolContainer.setCpuShares(appearance.containerCPUSharesDefaultValue());
        toolContainer.setDeviceList(Lists.<ToolDevice>newArrayList());
        toolContainer.setContainerVolumes(Lists.<ToolVolume>newArrayList());
        toolContainer.setContainerVolumesFrom(Lists.<ToolVolumesFrom>newArrayList());
        toolContainer.setImage(factory.getImage().as());
        final Tool tool = factory.getTool().as();
        tool.setType(appearance.toolImportTypeDefaultValue());
        tool.setContainer(toolContainer);

        show(tool);
    }
}