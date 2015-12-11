package org.iplantc.de.admin.desktop.client.toolAdmin.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * @author jstroot
 * @author aramsey
 */
public class DeleteToolSelectedEvent extends GwtEvent<DeleteToolSelectedEvent.DeleteToolSelectedEventHandler> {
    public static interface DeleteToolSelectedEventHandler extends EventHandler {
        void onDeleteToolSelected(DeleteToolSelectedEvent event);
    }

    public interface HasDeleteToolSelectedEventHandlers {
        HandlerRegistration addDeleteToolSelectedEventHandler(DeleteToolSelectedEventHandler handler);
    }

    public static Type<DeleteToolSelectedEventHandler> TYPE = new Type<>();
    private final String toolId;

    public DeleteToolSelectedEvent(final String toolId) {
        this.toolId = toolId;
    }

    public Type<DeleteToolSelectedEventHandler> getAssociatedType() {
        return TYPE;
    }

    public String getToolId() {
        return toolId;
    }

    protected void dispatch(DeleteToolSelectedEventHandler handler) {
        handler.onDeleteToolSelected(this);
    }
}
