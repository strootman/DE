package org.iplantc.de.admin.desktop.client.toolAdmin.view.subviews;

import org.iplantc.de.admin.desktop.client.toolAdmin.ToolAdminView;
import org.iplantc.de.client.models.tool.ToolImplementation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.Editor;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import com.sencha.gxt.widget.core.client.Composite;
import com.sencha.gxt.widget.core.client.form.FieldLabel;
import com.sencha.gxt.widget.core.client.form.TextField;


public class ToolImplementationEditor extends Composite implements Editor<ToolImplementation> {

    interface ToolImplementationEditorBinder extends UiBinder<Widget, ToolImplementationEditor> {
    }

    private static ToolImplementationEditorBinder uiBinder =
            GWT.create(ToolImplementationEditorBinder.class);

    @Ignore
    @UiField FieldLabel implementorLabel, implementorEmailLabel;
    @UiField TextField implementorEditor;
    @UiField TextField implementorEmailEditor;
    @UiField(provided = true) ToolTestDataEditor testEditor;
    @UiField (provided = true)
    ToolAdminView.ToolAdminViewAppearance appearance;


    @Inject
    ToolImplementationEditor(final ToolAdminView.ToolAdminViewAppearance appearance,
                             final ToolTestDataEditor testEditor) {
        this.appearance = appearance;
        this.testEditor = testEditor;
        initWidget(uiBinder.createAndBindUi(this));

        implementorLabel.setHTML(appearance.toolImplementationImplementorLabel());
        implementorEmailLabel.setHTML(appearance.toolImplementationImplementorEmailLabel());
    }

    public boolean isValid(){
        return implementorEditor.isValid() && implementorEmailEditor.isValid();
    }

}
