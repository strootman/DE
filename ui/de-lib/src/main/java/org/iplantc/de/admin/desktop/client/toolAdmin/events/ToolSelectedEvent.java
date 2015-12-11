package org.iplantc.de.admin.desktop.client.toolAdmin.events;

import org.iplantc.de.client.models.tool.Tool;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * @author jstroot
 * @author aramsey
 */
public class ToolSelectedEvent extends GwtEvent<ToolSelectedEvent.ToolSelectedEventHandler> {
    public static interface ToolSelectedEventHandler extends EventHandler {
        void onToolSelected(ToolSelectedEvent event);
    }

    public interface HasToolSelectedEventHandlers {
        HandlerRegistration addToolSelectedEventHandler(ToolSelectedEventHandler handler);
    }

    public static Type<ToolSelectedEventHandler> TYPE = new Type<ToolSelectedEventHandler>();
    private final Tool tool;

    public ToolSelectedEvent(final Tool tool) {
        this.tool = tool;
    }

    public Type<ToolSelectedEventHandler> getAssociatedType() {
        return TYPE;
    }

    public Tool getTool() {
        return tool;
    }

    protected void dispatch(ToolSelectedEventHandler handler) {
        handler.onToolSelected(this);
    }
}
