package org.iplantc.de.admin.desktop.client.toolAdmin.events;

import org.iplantc.de.client.models.tool.Tool;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * Created by jstroot on 12/10/15.
 */
public class AddToolSelectedEvent extends GwtEvent<AddToolSelectedEvent.AddToolSelectedEventHandler> {
    public static interface AddToolSelectedEventHandler extends EventHandler {
        void onAddToolSelected(AddToolSelectedEvent event);
    }

    public interface HasAddToolSelectedEventHandlers {
        HandlerRegistration addAddToolSelectedEventHandler(AddToolSelectedEventHandler handler);
    }

    public static Type<AddToolSelectedEventHandler> TYPE = new Type<AddToolSelectedEventHandler>();
    private final Tool tool;

    public AddToolSelectedEvent(final Tool tool) {
        this.tool = tool;
    }

    public Type<AddToolSelectedEventHandler> getAssociatedType() {
        return TYPE;
    }

    public Tool getTool() {
        return tool;
    }

    protected void dispatch(AddToolSelectedEventHandler handler) {
        handler.onAddToolSelected(this);
    }
}
